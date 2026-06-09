package com.bank.customer.party.service;

import com.bank.common.persistence.JpaAuditingConfig;
import com.bank.customer.party.domain.DuplicateReviewCase;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.dto.DuplicateReviewResponse;
import com.bank.customer.party.repository.DuplicateReviewCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 중복고객 검토 큐 검증 — 중복고객 검토 화면(/admin/duplicates)의 백엔드(B1).
 *
 * <p>PENDING 케이스만 신규·기존 party 이름과 함께 큐에 노출되는지, 복본/별개 확정 후 큐에서 빠지는지 확인한다.
 */
@DataJpaTest
@Import({JpaAuditingConfig.class, PartyManageService.class})
@ActiveProfiles("test")
@DisplayName("PartyManageService — 중복고객 검토 큐")
class DuplicateReviewServiceTest {

    private static final Pageable FIRST_20 = PageRequest.of(0, 20);

    @Autowired
    TestEntityManager em;

    @Autowired
    PartyManageService service;

    @Autowired
    DuplicateReviewCaseRepository caseRepository;

    private Party party(String name) {
        Party p = Party.builder()
                .partyTypeCode(Party.TYPE_PERSONAL)
                .partyName(name)
                .partyStatusCode(Party.STATUS_ACTIVE)
                .build();
        em.persist(p);
        return p;
    }

    private Long duplicateCase(String newName, String existingName, String matchType, String status) {
        Party np = party(newName);
        Party ep = party(existingName);
        DuplicateReviewCase d = DuplicateReviewCase.builder()
                .newPartyId(np.getPartyId())
                .existingPartyId(ep.getPartyId())
                .matchTypeCode(matchType)
                .reviewStatusCode(status)
                .detectedAt(OffsetDateTime.now())
                .build();
        em.persist(d);
        return d.getDuplicateReviewCaseId();
    }

    @Test
    @DisplayName("PENDING 케이스만 신규·기존 이름과 함께 큐에 노출된다")
    void queue_onlyPending_withBothNames() {
        duplicateCase("김민수(신규)", "김민수(기존)", DuplicateReviewCase.MATCH_NAME_BIRTH,
                DuplicateReviewCase.STATUS_PENDING);
        duplicateCase("이미처리신규", "이미처리기존", DuplicateReviewCase.MATCH_CI,
                DuplicateReviewCase.STATUS_DUPLICATE);
        em.flush();

        Page<DuplicateReviewResponse> result = service.listPendingDuplicates(FIRST_20);

        assertThat(result.getContent()).hasSize(1);
        DuplicateReviewResponse row = result.getContent().get(0);
        assertThat(row.newPartyName()).isEqualTo("김민수(신규)");
        assertThat(row.existingPartyName()).isEqualTo("김민수(기존)");
        assertThat(row.matchTypeCode()).isEqualTo(DuplicateReviewCase.MATCH_NAME_BIRTH);
    }

    @Test
    @DisplayName("복본 확정하면 DUPLICATE로 바뀌고 큐에서 빠지며 검토자가 기록된다")
    void markDuplicate_removesFromQueue() {
        Long caseId = duplicateCase("신규", "기존", DuplicateReviewCase.MATCH_CI,
                DuplicateReviewCase.STATUS_PENDING);
        em.flush();

        service.markDuplicate(caseId, 9001L, "동일인 확인 - 계정 통합");
        em.flush();
        em.clear();

        DuplicateReviewCase reloaded = caseRepository.findById(caseId).orElseThrow();
        assertThat(reloaded.getReviewStatusCode()).isEqualTo(DuplicateReviewCase.STATUS_DUPLICATE);
        assertThat(reloaded.getReviewerEmployeeId()).isEqualTo(9001L);
        assertThat(service.listPendingDuplicates(FIRST_20).getContent()).isEmpty();
    }

    @Test
    @DisplayName("별개 확정하면 DISTINCT로 바뀌고 큐에서 빠진다")
    void markDistinct_removesFromQueue() {
        Long caseId = duplicateCase("신규", "기존", DuplicateReviewCase.MATCH_NAME_BIRTH,
                DuplicateReviewCase.STATUS_PENDING);
        em.flush();

        service.markDistinct(caseId, 9001L, "동명이인 확인");
        em.flush();
        em.clear();

        DuplicateReviewCase reloaded = caseRepository.findById(caseId).orElseThrow();
        assertThat(reloaded.getReviewStatusCode()).isEqualTo(DuplicateReviewCase.STATUS_DISTINCT);
        assertThat(service.listPendingDuplicates(FIRST_20).getContent()).isEmpty();
    }
}

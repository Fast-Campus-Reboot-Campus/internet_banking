package com.bank.customer.party.service;

import com.bank.common.persistence.JpaAuditingConfig;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.SanctionScreeningHit;
import com.bank.customer.party.dto.SanctionHitResponse;
import com.bank.customer.party.repository.SanctionScreeningHitRepository;
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
 * 제재 스크리닝 Hit 검토 큐 검증 — 제재대상 Hit 검토 화면(/admin/screening)의 백엔드(B2).
 *
 * <p>PENDING hit만 일치율 높은 순으로 큐에 노출되는지, 무혐의/제재확정 처리 후 큐에서 빠지는지 확인한다.
 */
@DataJpaTest
@Import({JpaAuditingConfig.class, PartyManageService.class})
@ActiveProfiles("test")
@DisplayName("PartyManageService — 제재 스크리닝 Hit 검토 큐")
class ScreeningHitReviewServiceTest {

    private static final Pageable FIRST_20 = PageRequest.of(0, 20);

    @Autowired
    TestEntityManager em;

    @Autowired
    PartyManageService service;

    @Autowired
    SanctionScreeningHitRepository hitRepository;

    private Long hit(String name, String hitType, int matchRate, String status) {
        Party p = Party.builder()
                .partyTypeCode(Party.TYPE_PERSONAL)
                .partyName(name)
                .partyStatusCode(Party.STATUS_ACTIVE)
                .build();
        em.persist(p);
        SanctionScreeningHit h = SanctionScreeningHit.builder()
                .partyId(p.getPartyId())
                .hitTypeCode(hitType)
                .matchRate(matchRate)
                .screeningStatusCode(status)
                .detectedAt(OffsetDateTime.now())
                .build();
        em.persist(h);
        return h.getSanctionScreeningHitId();
    }

    @Test
    @DisplayName("PENDING hit만 일치율 높은 순으로 큐에 노출된다")
    void queue_onlyPending_sortedByMatchRate() {
        hit("저일치", "EU", 81, SanctionScreeningHit.STATUS_PENDING);
        hit("고일치", "OFAC_SDN", 95, SanctionScreeningHit.STATUS_PENDING);
        hit("이미처리", "UN", 99, SanctionScreeningHit.STATUS_CLEARED);
        em.flush();

        Page<SanctionHitResponse> result = service.listPendingScreeningHits(FIRST_20);

        assertThat(result.getContent()).extracting(SanctionHitResponse::partyName)
                .containsExactly("고일치", "저일치"); // 일치율 내림차순, CLEARED 제외
    }

    @Test
    @DisplayName("무혐의 처리하면 CLEARED로 바뀌고 큐에서 빠지며 검토자가 기록된다")
    void clear_removesFromQueue() {
        Long hitId = hit("동명이인", "KR_PEP", 84, SanctionScreeningHit.STATUS_PENDING);
        em.flush();

        service.clearScreeningHit(hitId, 9001L, "동명이인 확인");
        em.flush();
        em.clear();

        SanctionScreeningHit reloaded = hitRepository.findById(hitId).orElseThrow();
        assertThat(reloaded.getScreeningStatusCode()).isEqualTo(SanctionScreeningHit.STATUS_CLEARED);
        assertThat(reloaded.getReviewerEmployeeId()).isEqualTo(9001L);
        assertThat(reloaded.getReviewedAt()).isNotNull();
        assertThat(service.listPendingScreeningHits(FIRST_20).getContent()).isEmpty();
    }

    @Test
    @DisplayName("제재 확정하면 CONFIRMED로 바뀌고 큐에서 빠진다")
    void confirm_removesFromQueue() {
        Long hitId = hit("제재확정", "OFAC_SDN", 96, SanctionScreeningHit.STATUS_PENDING);
        em.flush();

        service.confirmScreeningHit(hitId, 9001L, "OFAC SDN 일치 확정");
        em.flush();
        em.clear();

        SanctionScreeningHit reloaded = hitRepository.findById(hitId).orElseThrow();
        assertThat(reloaded.getScreeningStatusCode()).isEqualTo(SanctionScreeningHit.STATUS_CONFIRMED);
        assertThat(service.listPendingScreeningHits(FIRST_20).getContent()).isEmpty();
    }
}

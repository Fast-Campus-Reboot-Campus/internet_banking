package com.bank.customer.party.service;

import com.bank.common.persistence.JpaAuditingConfig;
import com.bank.customer.party.domain.Party;
import com.bank.customer.party.domain.PartyRelation;
import com.bank.customer.party.dto.AgentReviewResponse;
import com.bank.customer.party.repository.PartyRelationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 대리인 위임장 검토 큐 검증 — 대리인 검토 화면(/admin/agent)의 백엔드.
 *
 * <p>review_status='PENDING'인 관계만 대리인(toParty) 이름과 함께 큐에 노출되는지, 승인/거절 후
 * 큐에서 빠지는지 확인한다. 서비스가 party 레포들만 의존하므로 @DataJpaTest에 서비스 빈만 추가한다.
 */
@DataJpaTest
@Import({JpaAuditingConfig.class, PartyManageService.class})
@ActiveProfiles("test")
@DisplayName("PartyManageService — 대리인 위임장 검토 큐")
class AgentReviewServiceTest {

    private static final Pageable FIRST_20 = PageRequest.of(0, 20);

    @Autowired
    TestEntityManager em;

    @Autowired
    PartyManageService service;

    @Autowired
    PartyRelationRepository relationRepository;

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private Party party(String name) {
        Party p = Party.builder()
                .partyTypeCode(Party.TYPE_PERSONAL)
                .partyName(name)
                .partyStatusCode(Party.STATUS_ACTIVE)
                .build();
        em.persist(p);
        return p;
    }

    /** owner→agent 위임 관계를 주어진 검토상태로 생성하고 relationId를 반환한다. */
    private Long relation(String ownerName, String agentName, String reviewStatus) {
        Party owner = party(ownerName);
        Party agent = party(agentName);
        PartyRelation r = PartyRelation.builder()
                .fromPartyId(owner.getPartyId())
                .toPartyId(agent.getPartyId())
                .relationTypeCode(PartyRelation.TYPE_GUARANTOR)
                .representationScope("계좌 조회·이체")
                .proofUrl("https://docs/poa.pdf")
                .relationStartDate("20260601")
                .relationReviewStatusCode(reviewStatus)
                .build();
        em.persist(r);
        return r.getRelationId();
    }

    // ── 테스트 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING 관계만 대리인 이름과 함께 큐에 노출된다")
    void queue_onlyPending_withAgentName() {
        relation("위임자A", "대리인A", PartyRelation.REVIEW_PENDING);
        relation("위임자B", "대리인B", PartyRelation.REVIEW_APPROVED);
        relation("위임자C", "대리인C", null); // 시드·자동 관계
        em.flush();

        Page<AgentReviewResponse> result = service.listPendingAgentReviews(FIRST_20);

        assertThat(result.getContent()).hasSize(1);
        AgentReviewResponse row = result.getContent().get(0);
        assertThat(row.agentName()).isEqualTo("대리인A");
        assertThat(row.representationScope()).isEqualTo("계좌 조회·이체");
        assertThat(row.relationReviewStatusCode()).isEqualTo(PartyRelation.REVIEW_PENDING);
    }

    @Test
    @DisplayName("승인하면 APPROVED로 바뀌고 큐에서 빠진다")
    void approve_removesFromQueue() {
        Long relationId = relation("위임자", "대리인", PartyRelation.REVIEW_PENDING);
        em.flush();

        service.approveReview(relationId);
        em.flush();
        em.clear();

        PartyRelation reloaded = relationRepository.findById(relationId).orElseThrow();
        assertThat(reloaded.getRelationReviewStatusCode()).isEqualTo(PartyRelation.REVIEW_APPROVED);
        assertThat(service.listPendingAgentReviews(FIRST_20).getContent()).isEmpty();
    }

    @Test
    @DisplayName("거절하면 REJECTED로 바뀌고 큐에서 빠진다")
    void reject_removesFromQueue() {
        Long relationId = relation("위임자", "대리인", PartyRelation.REVIEW_PENDING);
        em.flush();

        service.rejectReview(relationId);
        em.flush();
        em.clear();

        PartyRelation reloaded = relationRepository.findById(relationId).orElseThrow();
        assertThat(reloaded.getRelationReviewStatusCode()).isEqualTo(PartyRelation.REVIEW_REJECTED);
        assertThat(service.listPendingAgentReviews(FIRST_20).getContent()).isEmpty();
    }
}

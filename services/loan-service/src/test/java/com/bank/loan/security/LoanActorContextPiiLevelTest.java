package com.bank.loan.security;

import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.review.domain.LoanReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoanActorContextPiiLevelTest {

    private static final Long OWNER_ID    = 10L;
    private static final Long REVIEWER_ID = 20L;
    private static final Long APPROVER_ID = 30L;
    private static final Long OTHER_ID    = 99L;
    private static final String BRANCH    = "0001";
    private static final String OTHER_BRANCH = "0002";

    private LoanApplication app;
    private LoanReview review;
    private LoanReview escalatedReview;

    @BeforeEach
    void setUp() {
        app = LoanApplication.builder()
                .applId(1L).applNo("AP-001").customerId(100L).prodId(1L)
                .channelCd("MOBILE").requestedAmount(50_000_000L).requestedPeriodMo(60)
                .repaymentMethodCd("EQUAL_PRINCIPAL").applStatusCd("REVIEWING")
                .appliedAt(OffsetDateTime.now()).branchId(BRANCH)
                .estimatedIncomeAmt(60_000_000L)
                .build();

        review = LoanReview.builder()
                .applId(1L).revTypeCd(LoanReview.TYPE_AUTO)
                .revStatusCd(LoanReview.STATUS_PENDING_APPROVER)
                .ownerId(OWNER_ID).reviewerId(REVIEWER_ID).approverId(APPROVER_ID)
                .build();

        escalatedReview = LoanReview.builder()
                .applId(2L).revTypeCd(LoanReview.TYPE_MANUAL)
                .revStatusCd(LoanReview.STATUS_COMPLETED)
                .ownerId(OWNER_ID).reviewerId(REVIEWER_ID).approverId(APPROVER_ID)
                .escalatedAt(OffsetDateTime.now())
                .build();
    }

    // ── FULL ───────────────────────────────────────────────────────────────

    @Test
    void OPS_FULL() {
        assertThat(actor(OTHER_ID, BRANCH, "ROLE_OPS").piiLevel(app, review))
                .isEqualTo(PiiLevel.FULL);
    }

    @Test
    void INTERNAL_FULL() {
        assertThat(actor(OTHER_ID, null, "ROLE_INTERNAL").piiLevel(app, review))
                .isEqualTo(PiiLevel.FULL);
    }

    @Test
    void ADMIN_FULL() {
        assertThat(actor(OTHER_ID, null, "ROLE_ADMIN").piiLevel(app, review))
                .isEqualTo(PiiLevel.FULL);
    }

    @Test
    void 담당자_ownerId일치_FULL() {
        assertThat(actor(OWNER_ID, BRANCH, "ROLE_TELLER").piiLevel(app, review))
                .isEqualTo(PiiLevel.FULL);
    }

    @Test
    void 심사자_reviewerId일치_FULL() {
        assertThat(actor(REVIEWER_ID, BRANCH, "ROLE_DEPUTY_MANAGER").piiLevel(app, review))
                .isEqualTo(PiiLevel.FULL);
    }

    // ── MASKED ────────────────────────────────────────────────────────────

    @Test
    void 승인자_approverId일치_MASKED() {
        assertThat(actor(APPROVER_ID, BRANCH, "ROLE_BRANCH_MANAGER").piiLevel(app, review))
                .isEqualTo(PiiLevel.MASKED);
    }

    @Test
    void 지점장_같은지점_MASKED() {
        assertThat(actor(OTHER_ID, BRANCH, "ROLE_BRANCH_MANAGER").piiLevel(app, review))
                .isEqualTo(PiiLevel.MASKED);
    }

    @Test
    void 본사담당자_상신건_MASKED() {
        assertThat(actor(OTHER_ID, "HQ", "ROLE_HQ_REVIEWER").piiLevel(app, escalatedReview))
                .isEqualTo(PiiLevel.MASKED);
    }

    @Test
    void 감사_MASKED() {
        assertThat(actor(OTHER_ID, null, "ROLE_COMPLIANCE").piiLevel(app, review))
                .isEqualTo(PiiLevel.MASKED);
    }

    // ── REDACTED ──────────────────────────────────────────────────────────

    @Test
    void 지점장_다른지점_REDACTED() {
        assertThat(actor(OTHER_ID, OTHER_BRANCH, "ROLE_BRANCH_MANAGER").piiLevel(app, review))
                .isEqualTo(PiiLevel.REDACTED);
    }

    @Test
    void 본사담당자_미상신건_REDACTED() {
        assertThat(actor(OTHER_ID, "HQ", "ROLE_HQ_REVIEWER").piiLevel(app, review))
                .isEqualTo(PiiLevel.REDACTED);
    }

    @Test
    void 고객본인_REDACTED() {
        assertThat(actor(100L, null, "ROLE_CUSTOMER").piiLevel(app, review))
                .isEqualTo(PiiLevel.REDACTED);
    }

    // ── PiiLevel 에 따른 DTO 필드 검증 ────────────────────────────────────

    @Test
    void FULL_소득금액노출_범위null() {
        var resp = com.bank.loan.review.dto.LoanReviewResponse.of(review, app, PiiLevel.FULL);
        assertThat(resp.estimatedIncomeAmt()).isEqualTo(60_000_000L);
        assertThat(resp.estimatedIncomeRange()).isNull();
    }

    @Test
    void MASKED_소득금액노출_범위null() {
        var resp = com.bank.loan.review.dto.LoanReviewResponse.of(review, app, PiiLevel.MASKED);
        assertThat(resp.estimatedIncomeAmt()).isEqualTo(60_000_000L);
        assertThat(resp.estimatedIncomeRange()).isNull();
    }

    @Test
    void REDACTED_소득금액null_범위노출() {
        var resp = com.bank.loan.review.dto.LoanReviewResponse.of(review, app, PiiLevel.REDACTED);
        assertThat(resp.estimatedIncomeAmt()).isNull();
        assertThat(resp.estimatedIncomeRange()).isEqualTo("6천만원대");
    }

    @Test
    void REDACTED_소득없는신청_범위null() {
        LoanApplication noIncome = LoanApplication.builder()
                .applId(2L).applNo("AP-002").customerId(100L).prodId(1L)
                .channelCd("MOBILE").requestedAmount(10_000_000L).requestedPeriodMo(12)
                .repaymentMethodCd("EQUAL_PRINCIPAL").applStatusCd("REVIEWING")
                .appliedAt(OffsetDateTime.now()).branchId(BRANCH)
                .build();
        var resp = com.bank.loan.review.dto.LoanReviewResponse.of(review, noIncome, PiiLevel.REDACTED);
        assertThat(resp.estimatedIncomeAmt()).isNull();
        assertThat(resp.estimatedIncomeRange()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private LoanActorContext actor(Long id, String branch, String... roleAuthorities) {
        return new LoanActorContext(id, branch, Set.of(roleAuthorities));
    }
}

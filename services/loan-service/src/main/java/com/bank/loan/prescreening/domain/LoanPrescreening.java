package com.bank.loan.prescreening.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 가심사 결과. ERD STAGE 2 LOAN_PRESCREENING 매핑. appl_id UNIQUE — 신청당 1건.
 *
 * 화면 A-2 한도조회 결과를 본 신청에 1:1 로 연결한다.
 * 본 단계: 외부 가심사 엔진 stub — 클라이언트가 결과(PASS/REJECT) 를 입력으로 전달.
 *
 * 결과(presc_result_cd):
 *   PASS    가심사 통과 → 신청 PRESCREENED 로 전이
 *   REJECT  가심사 거절 → 신청 REJECTED 로 전이 (reject_reason_cd 기록)
 */
@Getter
@Entity
@Table(name = "loan_prescreening")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanPrescreening extends BaseEntity {

    public static final String RESULT_PASS   = "PASS";
    public static final String RESULT_REJECT = "REJECT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "presc_id")
    private Long prescId;

    @Column(name = "appl_id", nullable = false, unique = true)
    private Long applId;

    @Column(name = "presc_result_cd", nullable = false, length = 50)
    private String prescResultCd;

    @Column(name = "estimated_limit_amt")
    private Long estimatedLimitAmt;

    @Column(name = "estimated_rate_bps")
    private Integer estimatedRateBps;

    @Column(name = "estimated_grade", length = 10)
    private String estimatedGrade;

    @Column(name = "estimated_score")
    private Integer estimatedScore;

    @Column(name = "reject_reason_cd", length = 50)
    private String rejectReasonCd;

    @Column(name = "presc_remark", length = 500)
    private String prescRemark;

    @Column(name = "prescreened_at", nullable = false)
    private OffsetDateTime prescreenedAt;

    @Column(name = "presc_engine_version", length = 50)
    private String prescEngineVersion;

    public boolean isPass() {
        return RESULT_PASS.equals(prescResultCd);
    }
}

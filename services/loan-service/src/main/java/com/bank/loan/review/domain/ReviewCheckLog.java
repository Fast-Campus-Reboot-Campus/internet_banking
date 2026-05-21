package com.bank.loan.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 본심사 체크 항목 로그. ERD STAGE 5 REVIEW_CHECK_LOG 매핑. append-only.
 *
 * 본 심사관/시스템이 항목별로 PASS/FAIL/REVIEW/N_A 결과를 남긴다.
 * 스키마상 deleted_at/updated_at/version 이 없어 BaseEntity 를 사용하지 않고
 * created_at/created_by 만 직접 audit 으로 채운다.
 *
 * 표준 항목(check_item_cd):
 *   PRESCREEN_PASS   가심사 PASS 확인
 *   CB_DECISION      신용평가 decision 확인
 *   DSR_CHECK        DSR 산정 결과 확인
 *   LTV_CHECK        LTV 산정 결과 확인 (담보 필수 상품만, 그 외 N_A)
 *   FINAL_DECISION   본심사 최종 결정 기록
 */
@Getter
@Entity
@Table(name = "review_check_log")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewCheckLog {

    // 체크 항목
    public static final String ITEM_PRESCREEN_PASS = "PRESCREEN_PASS";
    public static final String ITEM_CB_DECISION   = "CB_DECISION";
    public static final String ITEM_DSR_CHECK     = "DSR_CHECK";
    public static final String ITEM_LTV_CHECK     = "LTV_CHECK";
    public static final String ITEM_FINAL_DECISION = "FINAL_DECISION";

    // 체크 결과
    public static final String RESULT_PASS   = "PASS";
    public static final String RESULT_FAIL   = "FAIL";
    public static final String RESULT_REVIEW = "REVIEW";
    public static final String RESULT_N_A    = "N_A";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rchk_id")
    private Long rchkId;

    @Column(name = "rev_id", nullable = false)
    private Long revId;

    @Column(name = "check_item_cd", nullable = false, length = 50)
    private String checkItemCd;

    @Column(name = "check_result_cd", nullable = false, length = 50)
    private String checkResultCd;

    @Column(name = "check_remark", length = 500)
    private String checkRemark;

    @Column(name = "checker_id")
    private Long checkerId;

    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;
}

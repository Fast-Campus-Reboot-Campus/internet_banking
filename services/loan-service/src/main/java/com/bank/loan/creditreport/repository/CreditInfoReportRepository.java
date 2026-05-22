package com.bank.loan.creditreport.repository;

import com.bank.loan.creditreport.domain.CreditInfoReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditInfoReportRepository extends JpaRepository<CreditInfoReport, Long> {

    Optional<CreditInfoReport> findByCrptIdAndDeletedAtIsNull(Long crptId);

    List<CreditInfoReport> findByCntrIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long cntrId);

    /**
     * 자동 발화 멱등 키 조회 — (cntrId, dlqId, crptTypeCd, reportReasonCd) 가 동일한 신고가
     * SENT/ACKED 상태로 존재하면 재발화 시 그 row 를 그대로 반환한다.
     */
    Optional<CreditInfoReport>
    findFirstByCntrIdAndDlqIdAndCrptTypeCdAndReportReasonCdAndCrptStatusCdInAndDeletedAtIsNullOrderByCrptIdAsc(
            Long cntrId, Long dlqId, String crptTypeCd, String reportReasonCd, List<String> statuses);
}

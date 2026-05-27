package com.bank.loan.guaranteeinsuranceexpiry.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;
import com.bank.loan.guaranteeinsurance.repository.GuaranteeInsuranceRepository;
import com.bank.loan.guaranteeinsuranceexpiry.dto.GuaranteeInsuranceExpiryRunResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 보증보험 만기 일배치 — ISSUED 중 gins_end_date 가 baseDate 보다 이른 row 를 EXPIRED 전이.
 *
 * gins_end_date / baseDate 모두 YYYYMMDD 8자리 문자열이라 사전식 비교가 곧 날짜 비교.
 * 즉 end_date < baseDate 면 baseDate 시점에 이미 만기 경과 → EXPIRED.
 *
 * 멱등성: EXPIRED 전이된 row 는 다음 호출 대상에서 빠지므로 자연 멱등.
 * 영업일 가드 없음: 만기는 절대시점 기준이라 휴일에도 처리 (applicationexpiry 와 동일).
 *
 * drawdown 검증(LoanExecutionService.validateGuaranteeInsuranceIfApplicable) 은
 * ISSUED 만 활성으로 인정하므로 본 배치가 동작하면 만기된 보증보험은 자동으로
 * "ISSUED 없음" 상태가 되어 drawdown 차단 효과가 발생한다.
 */
@Service
@RequiredArgsConstructor
public class GuaranteeInsuranceExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(GuaranteeInsuranceExpiryBatchService.class);

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "GUARANTEE_INSURANCE";
    private static final String REASON_EXPIRED = "GUARANTEE_INSURANCE_EXPIRED";

    private final GuaranteeInsuranceRepository repository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public GuaranteeInsuranceExpiryRunResponse run(String baseDate) {
        List<GuaranteeInsurance> candidates = repository.findExpirableIssued(baseDate);
        int processed = 0;

        for (GuaranteeInsurance gins : candidates) {
            String before = gins.currentStatus();
            gins.markExpired();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, gins.getGinsId(),
                    before, GuaranteeInsurance.STATUS_EXPIRED,
                    REASON_EXPIRED,
                    "baseDate=" + baseDate + ", endDate=" + gins.getGinsEndDate(),
                    currentActor.currentActorId()
            ));
            processed++;
        }

        log.info("guarantee-insurance-expiry batch: baseDate={} total={} processed={}",
                baseDate, candidates.size(), processed);
        return GuaranteeInsuranceExpiryRunResponse.of(baseDate, candidates.size(), processed);
    }
}

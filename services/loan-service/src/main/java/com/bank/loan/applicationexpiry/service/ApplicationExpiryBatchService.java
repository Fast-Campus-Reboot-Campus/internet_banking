package com.bank.loan.applicationexpiry.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.applicationexpiry.dto.ApplicationExpiryRunResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 승인 만료 일배치 (flows §1.1: APPROVED → EXPIRED).
 *
 * 승인 유효기간: 14일 (가이드 값). 상품별 차등은 후속 — 본 단계는 상수 사용.
 *
 * 기준:
 *   threshold = baseDate.atStartOfDay(systemZone) - 14d
 *   대상      = APPROVED + LoanReview.approvedAt < threshold (즉 14d 가 이미 경과)
 *
 * 멱등성: 한 번 EXPIRED 로 전이되면 다음 호출엔 대상에서 빠지므로 자연 멱등.
 * 영업일 가드 없음: 만료는 절대시점 기준이라 휴일에도 처리.
 */
@Service
@RequiredArgsConstructor
public class ApplicationExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationExpiryBatchService.class);

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_APPLICATION";
    private static final String REASON_APPROVAL_EXPIRED = "APPROVAL_EXPIRED";
    private static final int APPROVAL_VALID_DAYS = 14;
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final LoanApplicationRepository applicationRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public ApplicationExpiryRunResponse run(String baseDate) {
        OffsetDateTime threshold = LocalDate.parse(baseDate, YYYYMMDD)
                .atStartOfDay(ZoneId.systemDefault())
                .minusDays(APPROVAL_VALID_DAYS)
                .toOffsetDateTime();

        List<LoanApplication> candidates = applicationRepository.findExpirableApproved(threshold);
        int processed = 0;

        for (LoanApplication app : candidates) {
            String before = app.currentStatus();
            app.markExpired();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, app.getApplId(),
                    before, LoanApplication.STATUS_EXPIRED,
                    REASON_APPROVAL_EXPIRED,
                    "baseDate=" + baseDate + ", validDays=" + APPROVAL_VALID_DAYS,
                    currentActor.currentActorId()
            ));
            processed++;
        }

        log.info("approval-expiry batch: baseDate={} threshold={} total={} processed={}",
                baseDate, threshold, candidates.size(), processed);
        return ApplicationExpiryRunResponse.of(baseDate, threshold, candidates.size(), processed);
    }
}

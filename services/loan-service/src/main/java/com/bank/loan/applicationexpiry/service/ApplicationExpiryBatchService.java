package com.bank.loan.applicationexpiry.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.application.domain.LoanApplication;
import com.bank.loan.application.repository.LoanApplicationRepository;
import com.bank.loan.applicationexpiry.dto.ApplicationExpiryRunResponse;
import com.bank.loan.applicationexpiry.dto.ExpiryCandidate;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 승인 만료 일배치 (flows §1.1: APPROVED → EXPIRED).
 *
 * 상품별 applicationValidityDays 를 우선 적용하고, 설정이 없으면 시스템 기본 14일 사용.
 *
 *   threshold(per-row) = baseDateStart - candidate.validityDays
 *   대상               = approvedAt < threshold
 *
 * 멱등성: 한 번 EXPIRED 로 전이되면 다음 호출 시 대상에서 제외.
 */
@Service
@RequiredArgsConstructor
public class ApplicationExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationExpiryBatchService.class);

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_APPLICATION";
    private static final String REASON_APPROVAL_EXPIRED = "APPROVAL_EXPIRED";
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final LoanApplicationRepository applicationRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public ApplicationExpiryRunResponse run(String baseDate) {
        OffsetDateTime baseDateStart = LocalDate.parse(baseDate, YYYYMMDD)
                .atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();

        // 상품별 validityDays 포함 APPROVED 후보 전체 조회
        List<ExpiryCandidate> candidates = applicationRepository.findApprovedWithValidityDays();

        // per-row 만료 판정: approvedAt < (baseDateStart - validityDays)
        Map<Long, Integer> validityByApplId = candidates.stream()
                .collect(Collectors.toMap(ExpiryCandidate::applId, ExpiryCandidate::validityDays));

        List<Long> expireIds = candidates.stream()
                .filter(c -> c.approvedAt().isBefore(baseDateStart.minusDays(c.validityDays())))
                .map(ExpiryCandidate::applId)
                .toList();

        List<LoanApplication> apps = expireIds.isEmpty()
                ? List.of()
                : applicationRepository.findAllById(expireIds);

        for (LoanApplication app : apps) {
            String before = app.currentStatus();
            app.markExpired();
            int validDays = validityByApplId.getOrDefault(app.getApplId(), 14);
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_TABLE_CD, app.getApplId(),
                    before, LoanApplication.STATUS_EXPIRED,
                    REASON_APPROVAL_EXPIRED,
                    "baseDate=" + baseDate + ", validDays=" + validDays,
                    currentActor.currentActorId()
            ));
        }

        log.info("approval-expiry batch: baseDate={} candidates={} processed={}",
                baseDate, candidates.size(), apps.size());
        return ApplicationExpiryRunResponse.of(baseDate, baseDateStart, candidates.size(), apps.size());
    }
}

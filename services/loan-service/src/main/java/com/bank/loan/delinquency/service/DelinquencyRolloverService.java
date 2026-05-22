package com.bank.loan.delinquency.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.delinquency.domain.Delinquency;
import com.bank.loan.delinquency.domain.DelinquencyDailySnapshot;
import com.bank.loan.delinquency.dto.DelinquencyRolloverResponse;
import com.bank.loan.delinquency.repository.DelinquencyDailySnapshotRepository;
import com.bank.loan.delinquency.repository.DelinquencyRepository;
import com.bank.loan.notification.event.DelinquencyOpenedEvent;
import com.bank.loan.notification.event.DelinquencyResolvedEvent;
import com.bank.loan.notification.event.DelinquencyStageAdvancedEvent;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 연체 일배치 — flows §2.2 후반·§1.2.
 *
 * 보통 매일 새벽 자동이체 배치(AutoDebitBatchService) 직후 호출된다.
 *
 * 1) due_date < baseDate AND status=DUE 회차 → OVERDUE 전이 (status_history append)
 * 2) 영향받은 계약마다:
 *    - 활성 OVERDUE 회차 집계
 *    - 활성 dlq가 없으면 신규 생성 (dlqStartDate = 가장 오래된 OVERDUE 회차의 due_date+1)
 *    - 있으면 dlq_days·집계금액·stage 갱신
 *    - 일별 스냅샷 append (UNIQUE (dlq_id, snapshot_date) 로 멱등 보장)
 * 3) 활성 dlq 중 OVERDUE 회차가 0 인 계약 → RESOLVED 전이
 *
 * 본 단계: overdue_rate_bps = 시스템 디폴트 300bps (3%/년) 가산. 상품별 차등은 후속.
 *
 * 멱등성: 같은 baseDate 재실행 시
 *   - 이미 OVERDUE 인 회차는 (1) 단계에서 매칭 안 됨 (status=DUE 필터)
 *   - 스냅샷 UNIQUE 제약으로 중복 INSERT 차단
 *   - 집계 갱신은 idempotent (같은 입력 → 같은 결과)
 */
@Service
@RequiredArgsConstructor
public class DelinquencyRolloverService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_SCHEDULE = "REPAYMENT_SCHEDULE";
    private static final String TARGET_DELINQUENCY = "DELINQUENCY";

    private static final String REASON_OVERDUE_ROLLOVER = "OVERDUE_ROLLOVER";
    private static final String REASON_DLQ_OPENED = "DELINQUENCY_OPENED";
    private static final String REASON_DLQ_RESOLVED = "DELINQUENCY_RESOLVED";

    /** 시스템 디폴트 연체가산금리: 3%/년 (300bps). 상품별 차등은 후속 — LoanProduct 정책 추가 시 교체. */
    private static final int DEFAULT_OVERDUE_RATE_BPS = 300;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RepaymentScheduleRepository scheduleRepository;
    private final DelinquencyRepository delinquencyRepository;
    private final DelinquencyDailySnapshotRepository snapshotRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DelinquencyRolloverResponse rollover(String baseDate) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate base = LocalDate.parse(baseDate, DATE);
        Long actorId = currentActor.currentActorId();

        // (1) 새로 OVERDUE 가 된 회차들
        List<RepaymentSchedule> newlyOverdue = scheduleRepository
                .findByRschStatusCdAndDueDateLessThanAndRschVersionCdAndDeletedAtIsNullOrderByCntrIdAscInstallmentNoAsc(
                        RepaymentSchedule.STATUS_DUE, baseDate, RepaymentSchedule.VERSION_INITIAL);

        Set<Long> impactedCntrIds = new LinkedHashSet<>();
        for (RepaymentSchedule sch : newlyOverdue) {
            String before = sch.currentStatus();
            sch.markOverdue();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_SCHEDULE, sch.getRschId(),
                    before, RepaymentSchedule.STATUS_OVERDUE,
                    REASON_OVERDUE_ROLLOVER, "baseDate=" + baseDate, actorId
            ));
            impactedCntrIds.add(sch.getCntrId());
        }

        // (2) 활성 dlq 가 있는 계약도 매일 갱신 대상
        impactedCntrIds.addAll(findActiveDelinquencyCntrIds());

        int activeCount = 0;
        int resolvedCount = 0;
        int snapshotCount = 0;

        for (Long cntrId : impactedCntrIds) {
            ProcessResult r = processContract(cntrId, base, baseDate, now, actorId);
            activeCount += r.active;
            resolvedCount += r.resolved;
            snapshotCount += r.snapshotsCreated;
        }

        return DelinquencyRolloverResponse.of(baseDate, newlyOverdue.size(), activeCount, resolvedCount, snapshotCount);
    }

    private record ProcessResult(int active, int resolved, int snapshotsCreated) {}

    private ProcessResult processContract(Long cntrId, LocalDate base, String baseDate, OffsetDateTime now, Long actorId) {
        List<RepaymentSchedule> overdueSchedules = scheduleRepository
                .findByCntrIdAndRschStatusCdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(
                        cntrId, RepaymentSchedule.STATUS_OVERDUE, RepaymentSchedule.VERSION_INITIAL);

        Optional<Delinquency> existingActive = delinquencyRepository
                .findByCntrIdAndDlqStatusCdAndDeletedAtIsNull(cntrId, Delinquency.STATUS_ACTIVE);

        if (overdueSchedules.isEmpty()) {
            if (existingActive.isPresent()) {
                Delinquency d = existingActive.get();
                String before = d.currentStatus();
                d.markResolved(baseDate, now);
                statusHistoryPublisher.publish(StatusChangeEvent.of(
                        DOMAIN_CD, TARGET_DELINQUENCY, d.getDlqId(),
                        before, Delinquency.STATUS_RESOLVED,
                        REASON_DLQ_RESOLVED, "baseDate=" + baseDate, actorId
                ));
                eventPublisher.publishEvent(new DelinquencyResolvedEvent(cntrId, d.getDlqId(), now));
                return new ProcessResult(0, 1, 0);
            }
            return new ProcessResult(0, 0, 0);
        }

        long principalSum = overdueSchedules.stream().mapToLong(RepaymentSchedule::getScheduledPrincipal).sum();
        long interestSum  = overdueSchedules.stream().mapToLong(RepaymentSchedule::getScheduledInterest).sum();

        // 가장 오래된 OVERDUE 회차의 due_date+1 일을 연체 시작일로 본다
        String oldestDueDate = overdueSchedules.get(0).getDueDate();
        LocalDate dlqStart = LocalDate.parse(oldestDueDate, DATE).plusDays(1);
        int dlqDays = Math.max(0, (int) ChronoUnit.DAYS.between(dlqStart, base) + 1);
        String stage = Delinquency.stageOf(dlqDays);

        Delinquency dlq;
        if (existingActive.isPresent()) {
            dlq = existingActive.get();
            String prevStage = dlq.getDlqStageCd();
            dlq.updateDailyAggregate(dlqDays, principalSum, interestSum, stage);
            if (prevStage != null && !prevStage.equals(stage)) {
                eventPublisher.publishEvent(new DelinquencyStageAdvancedEvent(
                        cntrId, dlq.getDlqId(), prevStage, stage, dlqDays));
            }
        } else {
            dlq = delinquencyRepository.save(Delinquency.builder()
                    .cntrId(cntrId)
                    .dlqStatusCd(Delinquency.STATUS_ACTIVE)
                    .dlqStartDate(dlqStart.format(DATE))
                    .dlqDays(dlqDays)
                    .dlqPrincipalAmt(principalSum)
                    .dlqInterestAmt(interestSum)
                    .dlqTotalAmt(principalSum + interestSum)
                    .overdueRateBps(DEFAULT_OVERDUE_RATE_BPS)
                    .dlqStageCd(stage)
                    .build());
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_DELINQUENCY, dlq.getDlqId(),
                    null, Delinquency.STATUS_ACTIVE,
                    REASON_DLQ_OPENED, "baseDate=" + baseDate, actorId
            ));
            eventPublisher.publishEvent(new DelinquencyOpenedEvent(
                    cntrId, dlq.getDlqId(), dlq.getDlqStartDate(), stage));
        }

        int snapshotsCreated = 0;
        if (!snapshotRepository.existsByDlqIdAndSnapshotDate(dlq.getDlqId(), baseDate)) {
            snapshotRepository.save(DelinquencyDailySnapshot.builder()
                    .dlqId(dlq.getDlqId())
                    .cntrId(cntrId)
                    .snapshotDate(baseDate)
                    .dlqDays(dlqDays)
                    .dlqPrincipalAmt(principalSum)
                    .dlqInterestAmt(interestSum)
                    .dlqTotalAmt(principalSum + interestSum)
                    .overdueRateBps(DEFAULT_OVERDUE_RATE_BPS)
                    .dlqStageCd(stage)
                    .snapshottedAt(now)
                    .build());
            snapshotsCreated = 1;
        }

        return new ProcessResult(1, 0, snapshotsCreated);
    }

    private Set<Long> findActiveDelinquencyCntrIds() {
        Set<Long> ids = new HashSet<>();
        delinquencyRepository.findByDlqStatusCdAndDeletedAtIsNull(Delinquency.STATUS_ACTIVE)
                .forEach(d -> ids.add(d.getCntrId()));
        return ids;
    }
}

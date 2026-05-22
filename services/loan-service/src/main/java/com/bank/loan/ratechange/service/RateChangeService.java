package com.bank.loan.ratechange.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.contract.domain.LoanContract;
import com.bank.loan.contract.repository.LoanContractRepository;
import com.bank.loan.ratechange.domain.RateChangeHistory;
import com.bank.loan.ratechange.dto.CreateRateChangeRequest;
import com.bank.loan.ratechange.dto.RateChangeApplyResponse;
import com.bank.loan.ratechange.dto.RateChangeHistoryListResponse;
import com.bank.loan.ratechange.dto.RateChangeHistoryResponse;
import com.bank.loan.ratechange.repository.RateChangeHistoryRepository;
import com.bank.loan.schedule.domain.RepaymentSchedule;
import com.bank.loan.schedule.repository.RepaymentScheduleRepository;
import com.bank.loan.schedule.service.EqualPaymentCalculator;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 금리 변경 + 스케줄 재생성 서비스 (flows §2.3).
 *
 * 흐름:
 *   1) 계약 조회 + 새 금리 계산 (total = base + spread - preferential, 음수면 0)
 *   2) RATE_CHANGE_HISTORY append (append-only)
 *   3) LoanContract 금리 컬럼 갱신
 *   4) appliedStartDate 이후 영향 받는 활성 회차(DUE/OVERDUE) → SUPERSEDED 전이
 *   5) 영향 받은 회차의 scheduled_principal 합 = 남은 원금 → EqualPaymentCalculator 로 재계산
 *   6) 신규 V_{n+1} 회차 일괄 INSERT (installment_no/due_date 는 원본과 동일)
 *   7) status_history 양쪽 기록 (LOAN_CONTRACT: RATE_CHANGED / REPAYMENT_SCHEDULE: SUPERSEDED 각 row)
 *
 * 모든 회차가 이미 PAID 인 경우 (영향회차 0) 스케줄 재생성을 생략하고
 * 금리 갱신·이력 append 만 수행한다.
 *
 * 본 단계: 원리금균등(EQUAL) 만 지원 — LoanProduct 정책 검증은 LoanExecutionService 가 책임.
 * 변경 후 GET /api/loan-contracts/{cntrId}/repayment-schedules 는 기본으로 최신 버전을,
 * version 쿼리 파라미터를 명시하면 V1 등 과거 버전(SUPERSEDED 포함) 도 조회 가능.
 */
@Service
@RequiredArgsConstructor
public class RateChangeService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_CONTRACT = "LOAN_CONTRACT";
    private static final String TARGET_SCHEDULE = "REPAYMENT_SCHEDULE";
    private static final String REASON_RATE_CHANGED = "RATE_CHANGED";

    private final RateChangeHistoryRepository historyRepository;
    private final LoanContractRepository contractRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional
    public RateChangeApplyResponse apply(Long cntrId, CreateRateChangeRequest req) {
        LoanContract contract = contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));

        int newBase = req.newBaseRateBps();
        int newSpread = req.newSpreadBps() == null ? 0 : req.newSpreadBps();
        int newPref  = req.newPreferentialRateBps() == null ? 0 : req.newPreferentialRateBps();
        int newTotal = req.newTotalRateBps() == null
                ? Math.max(0, newBase + newSpread - newPref)
                : req.newTotalRateBps();
        if (newTotal < 0) {
            throw new BusinessException(LoanErrorCode.LOAN_110, "newTotalRateBps must be >= 0");
        }

        int previousTotal = contract.getTotalRateBps();
        OffsetDateTime now = OffsetDateTime.now();
        Long actorId = currentActor.currentActorId();

        // (2) history append
        RateChangeHistory history = historyRepository.save(RateChangeHistory.builder()
                .cntrId(cntrId)
                .rateChangeReasonCd(req.rateChangeReasonCd())
                .previousRateBps(previousTotal)
                .newRateBps(newTotal)
                .baseRateBps(newBase)
                .spreadBps(newSpread)
                .preferentialRateBps(newPref)
                .appliedStartDate(req.appliedStartDate())
                .changedAt(now)
                .changedBy(actorId)
                .build());

        // (3) contract rate
        contract.updateRate(newBase, newSpread, newPref, newTotal);
        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_CONTRACT, cntrId,
                String.valueOf(previousTotal), String.valueOf(newTotal),
                REASON_RATE_CHANGED,
                "appliedStartDate=" + req.appliedStartDate()
                        + " / reason=" + req.rateChangeReasonCd()
                        + " / rchgId=" + history.getRchgId(),
                actorId
        ));

        // (4) 영향 받는 회차 찾기 (최신 버전의 DUE/OVERDUE 중 due_date >= appliedStartDate)
        String currentVersion = scheduleRepository.findMaxVersion(cntrId);
        if (currentVersion == null || currentVersion.isEmpty()) {
            // 스케줄이 아직 없으면 (drawdown 전) — history 만 남기고 종료
            return RateChangeApplyResponse.of(history, "", 0, 0);
        }

        List<RepaymentSchedule> affected = scheduleRepository
                .findByCntrIdAndRschVersionCdAndDeletedAtIsNullOrderByInstallmentNoAsc(cntrId, currentVersion)
                .stream()
                .filter(s -> (RepaymentSchedule.STATUS_DUE.equals(s.currentStatus())
                              || RepaymentSchedule.STATUS_OVERDUE.equals(s.currentStatus()))
                             && s.getDueDate().compareTo(req.appliedStartDate()) >= 0)
                .sorted(Comparator.comparing(RepaymentSchedule::getInstallmentNo))
                .toList();

        if (affected.isEmpty()) {
            return RateChangeApplyResponse.of(history, currentVersion, 0, 0);
        }

        // (4') SUPERSEDED 전이 + status_history
        long remainingPrincipal = 0L;
        for (RepaymentSchedule s : affected) {
            String before = s.currentStatus();
            s.markSuperseded();
            statusHistoryPublisher.publish(StatusChangeEvent.of(
                    DOMAIN_CD, TARGET_SCHEDULE, s.getRschId(),
                    before, RepaymentSchedule.STATUS_SUPERSEDED,
                    REASON_RATE_CHANGED,
                    "rchgId=" + history.getRchgId(), actorId
            ));
            remainingPrincipal += s.getScheduledPrincipal();
        }

        // (5) 새 PMT 재계산
        String newVersion = nextVersion(currentVersion);
        List<EqualPaymentCalculator.Installment> recalculated = EqualPaymentCalculator.calculate(
                remainingPrincipal, newTotal, affected.size());

        // (6) V_{n+1} 신규 row INSERT — installmentNo·dueDate 는 원본 유지
        List<RepaymentSchedule> toSave = new ArrayList<>(affected.size());
        for (int i = 0; i < affected.size(); i++) {
            RepaymentSchedule src = affected.get(i);
            EqualPaymentCalculator.Installment inst = recalculated.get(i);
            toSave.add(RepaymentSchedule.builder()
                    .cntrId(cntrId)
                    .installmentNo(src.getInstallmentNo())
                    .dueDate(src.getDueDate())
                    .scheduledPrincipal(inst.scheduledPrincipal())
                    .scheduledInterest(inst.scheduledInterest())
                    .scheduledTotal(inst.scheduledTotal())
                    .remainingBalance(inst.remainingBalance())
                    .appliedRateBps(newTotal)
                    .rschStatusCd(RepaymentSchedule.STATUS_DUE)
                    .rschVersionCd(newVersion)
                    .holidayAdjustedYn(src.getHolidayAdjustedYn())
                    .build());
        }
        scheduleRepository.saveAll(toSave);

        return RateChangeApplyResponse.of(history, newVersion, affected.size(), toSave.size());
    }

    @Transactional(readOnly = true)
    public RateChangeHistoryListResponse listHistory(Long cntrId) {
        contractRepository.findByCntrIdAndDeletedAtIsNull(cntrId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_062));
        List<RateChangeHistoryResponse> items = historyRepository
                .findByCntrIdOrderByChangedAtAsc(cntrId)
                .stream()
                .map(RateChangeHistoryResponse::of)
                .toList();
        return RateChangeHistoryListResponse.of(cntrId, items);
    }

    /** V1 → V2, V2 → V3 ... 단순 숫자 증가. 비정상 형식은 그대로 + "_NEXT". */
    private String nextVersion(String current) {
        if (current != null && current.startsWith("V")) {
            try {
                int n = Integer.parseInt(current.substring(1));
                return "V" + (n + 1);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return (current == null ? "V" : current) + "_NEXT";
    }
}

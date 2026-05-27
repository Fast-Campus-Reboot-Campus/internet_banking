package com.bank.loan.maturity.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.loan.maturity.domain.Maturity;
import com.bank.loan.maturity.dto.MaturityRunResponse;
import com.bank.loan.maturity.repository.MaturityRepository;
import com.bank.loan.notification.event.MaturityReachedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 만기 도래 일배치.
 *
 * 대상: matStatusCd = ACTIVE AND currentMaturityDate <= baseDate.
 * 처리: ACTIVE → MATURED 전이 + STATUS_HISTORY 적재 + MaturityReachedEvent 발행.
 *
 * 멱등성: MATURED 전이된 row 는 다음 호출 대상에서 빠지므로 자연 멱등.
 * 영업일 가드 없음: 만기는 절대시점 기준 (applicationexpiry, guaranteeinsuranceexpiry 와 동일).
 *
 * 본 단계 자동 종결(MATURED→CLOSED)·잔액 정산은 본 배치 범위 밖.
 */
@Service
@RequiredArgsConstructor
public class MaturityBatchService {

    private static final Logger log = LoggerFactory.getLogger(MaturityBatchService.class);

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "MATURITY";
    private static final String REASON_MATURED = "MATURITY_REACHED";

    private final MaturityRepository repository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MaturityRunResponse run(String baseDate) {
        List<Maturity> candidates = repository
                .findByMatStatusCdAndCurrentMaturityDateLessThanEqualAndDeletedAtIsNullOrderByCntrIdAsc(
                        Maturity.STATUS_ACTIVE, baseDate);

        Long actorId = currentActor.currentActorId();
        int processed = 0;

        for (Maturity m : candidates) {
            try {
                String before = m.currentStatus();
                m.markMatured();
                statusHistoryPublisher.publish(StatusChangeEvent.of(
                        DOMAIN_CD, TARGET_TABLE_CD, m.getMatId(),
                        before, Maturity.STATUS_MATURED,
                        REASON_MATURED,
                        "baseDate=" + baseDate + " / maturityDate=" + m.getCurrentMaturityDate(),
                        actorId
                ));
                eventPublisher.publishEvent(new MaturityReachedEvent(
                        m.getMatId(), m.getCntrId(),
                        m.getCurrentMaturityDate(), m.getExtensionCount()));
                processed++;
            } catch (RuntimeException e) {
                log.warn("[maturity-batch] cntrId={} baseDate={} 실패: {}",
                        m.getCntrId(), baseDate, e.toString());
            }
        }

        return MaturityRunResponse.of(baseDate, candidates.size(), processed);
    }
}

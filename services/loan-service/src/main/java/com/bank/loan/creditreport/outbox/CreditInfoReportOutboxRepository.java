package com.bank.loan.creditreport.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CreditInfoReportOutboxRepository extends JpaRepository<CreditInfoReportOutbox, Long> {

    /**
     * dispatch 배치 핫패스. 상태=PENDING/FAILED 중 nextAttemptAt <= now 인 row 를
     * 한 페이지(기본 200) 단위로 꺼낸다. findAll 무페이지 금지(AI_GUIDELINES).
     */
    List<CreditInfoReportOutbox>
    findByStatusInAndNextAttemptAtLessThanEqualAndDeletedAtIsNullOrderByNextAttemptAtAsc(
            List<String> statuses, OffsetDateTime cutoff, Pageable pageable);

    Optional<CreditInfoReportOutbox> findByOutboxIdAndDeletedAtIsNull(Long outboxId);

    Optional<CreditInfoReportOutbox> findByCrptIdAndDeletedAtIsNull(Long crptId);
}

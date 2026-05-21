package com.bank.loan.application.repository;

import com.bank.loan.application.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    Optional<LoanApplication> findByApplIdAndDeletedAtIsNull(Long applId);

    /**
     * 승인 유효기간이 지난 APPROVED 신청 목록.
     * LoanReview.approvedAt < threshold 이고 아직 APPROVED 상태인 건 (즉 약정/취소 미진행).
     */
    @Query("""
            select a
              from LoanApplication a, LoanReview r
             where a.applStatusCd = 'APPROVED'
               and a.deletedAt is null
               and r.applId = a.applId
               and r.deletedAt is null
               and r.approvedAt is not null
               and r.approvedAt < :threshold
             order by a.applId asc
            """)
    List<LoanApplication> findExpirableApproved(@Param("threshold") OffsetDateTime threshold);
}

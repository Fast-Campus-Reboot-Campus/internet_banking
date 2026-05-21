package com.bank.loan.accrual.repository;

import com.bank.loan.accrual.domain.InterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, Long> {

    boolean existsByCntrIdAndAccrualDate(Long cntrId, String accrualDate);

    Optional<InterestAccrual> findFirstByCntrIdAndAccrualDateLessThanOrderByAccrualDateDesc(
            Long cntrId, String accrualDate);

    List<InterestAccrual> findByCntrIdAndAccrualDateBetweenOrderByAccrualDateAsc(
            Long cntrId, String from, String to);

    List<InterestAccrual> findByCntrIdOrderByAccrualDateAsc(Long cntrId);

    /**
     * 회차 귀속 기간(fromExclusive, toInclusive] 의 daily_interest_amt 합.
     * YYYYMMDD 문자열 사전식 비교가 곧 날짜 비교.
     * iacc_status_cd = ACCRUED 만 합산 (REVERSED 보정분 제외).
     *
     * 회차 상환 시 실제 발생이자 정산에 사용. accrual 배치가 한 번도 안 돌았으면 0 반환.
     */
    @Query("""
            select coalesce(sum(a.dailyInterestAmt), 0)
              from InterestAccrual a
             where a.cntrId = :cntrId
               and a.accrualDate > :fromExclusive
               and a.accrualDate <= :toInclusive
               and a.iaccStatusCd = 'ACCRUED'
            """)
    long sumDailyInterestInRange(@Param("cntrId") Long cntrId,
                                 @Param("fromExclusive") String fromExclusive,
                                 @Param("toInclusive") String toInclusive);
}

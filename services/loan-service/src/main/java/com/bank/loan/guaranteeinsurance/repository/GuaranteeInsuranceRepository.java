package com.bank.loan.guaranteeinsurance.repository;

import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuaranteeInsuranceRepository extends JpaRepository<GuaranteeInsurance, Long> {

    Optional<GuaranteeInsurance> findByGinsIdAndDeletedAtIsNull(Long ginsId);

    /**
     * 계약의 활성(취소·만기 외) 보증보험 — 중복 발급 차단 및 drawdown 검증에 사용.
     * 본 단계 ISSUED 만 활성 의미.
     */
    Optional<GuaranteeInsurance> findByCntrIdAndGinsStatusCdAndDeletedAtIsNull(Long cntrId, String ginsStatusCd);

    Optional<GuaranteeInsurance> findByGinsPolicyNoAndDeletedAtIsNull(String ginsPolicyNo);

    /** 계약에 어떤 상태든 보증보험 row 가 한 번이라도 등록됐는지 (CANCELED 포함). drawdown 사전조건 분기에 사용. */
    boolean existsByCntrIdAndDeletedAtIsNull(Long cntrId);

    /** 계약에 특정 상태의 보증보험이 있는지. drawdown 시 ISSUED 잔존 검증에 사용. */
    boolean existsByCntrIdAndGinsStatusCdAndDeletedAtIsNull(Long cntrId, String ginsStatusCd);

    /**
     * 만기가 baseDate 보다 이른 ISSUED 보증보험 — 일배치 EXPIRED 전이 대상.
     * gins_end_date / baseDate 는 모두 YYYYMMDD 8자리 문자열이라 사전식 비교가 곧 날짜 비교.
     */
    @Query("""
            select g
              from GuaranteeInsurance g
             where g.ginsStatusCd = 'ISSUED'
               and g.ginsEndDate < :baseDate
               and g.deletedAt is null
             order by g.ginsId asc
            """)
    List<GuaranteeInsurance> findExpirableIssued(@Param("baseDate") String baseDate);
}

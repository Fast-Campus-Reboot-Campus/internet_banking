package com.bank.loan.guaranteeinsurance.repository;

import com.bank.loan.guaranteeinsurance.domain.GuaranteeInsurance;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

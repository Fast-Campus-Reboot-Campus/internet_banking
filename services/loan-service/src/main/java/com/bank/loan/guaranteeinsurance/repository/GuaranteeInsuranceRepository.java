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
}

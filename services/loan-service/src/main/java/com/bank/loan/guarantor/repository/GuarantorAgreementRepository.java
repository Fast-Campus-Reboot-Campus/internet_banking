package com.bank.loan.guarantor.repository;

import com.bank.loan.guarantor.domain.GuarantorAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuarantorAgreementRepository extends JpaRepository<GuarantorAgreement, Long> {

    Optional<GuarantorAgreement> findByGagrIdAndDeletedAtIsNull(Long gagrId);

    List<GuarantorAgreement> findByApplIdAndDeletedAtIsNullOrderByGagrIdAsc(Long applId);

    /**
     * 같은 신청에 같은 보증인(gmstId) 의 활성(취소 외) 약정이 이미 존재하는지.
     * 중복 등록 차단에 사용.
     */
    boolean existsByApplIdAndGmstIdAndGagrStatusCdInAndDeletedAtIsNull(
            Long applId, Long gmstId, java.util.Collection<String> statusCds);

    /**
     * 신청에 특정 상태의 보증 약정이 있는지. 약정 체결 시 미서명(REGISTERED) 잔존 차단에 사용.
     */
    boolean existsByApplIdAndGagrStatusCdAndDeletedAtIsNull(Long applId, String gagrStatusCd);
}

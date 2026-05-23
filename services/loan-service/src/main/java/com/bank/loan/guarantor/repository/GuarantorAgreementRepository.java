package com.bank.loan.guarantor.repository;

import com.bank.loan.guarantor.domain.GuarantorAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 신청에 활성 SIGNED 보증 약정 수를 반환한다.
     * N+1 방지를 위해 단일 COUNT JPQL 쿼리 사용 — SELECT * 금지.
     * GuarantorPolicyValidator 가 minGuarantorCount 충족 여부 판단에 사용.
     */
    @Query("SELECT COUNT(g) FROM GuarantorAgreement g " +
           "WHERE g.applId = :applId AND g.gagrStatusCd = 'SIGNED' AND g.deletedAt IS NULL")
    long countActiveSignedByApplId(@Param("applId") Long applId);
}

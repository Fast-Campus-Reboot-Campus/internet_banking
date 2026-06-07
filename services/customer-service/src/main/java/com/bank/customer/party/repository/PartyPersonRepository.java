package com.bank.customer.party.repository;

import com.bank.customer.party.domain.PartyPerson;
import com.bank.customer.party.dto.MinorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartyPersonRepository extends JpaRepository<PartyPerson, Long> {

    Optional<PartyPerson> findByPartyIdAndDeletedAtIsNull(Long partyId);

    /** 본인확인 CI값으로 기존 등록 여부 조회 (중복 가입 방지). */
    boolean existsByCiValueAndDeletedAtIsNull(String ciValue);

    /** 본인확인 CI값으로 기존 개인 party 조회 — 동일인 재가입 시 party 재사용(역할 추가)용. */
    Optional<PartyPerson> findByCiValueAndDeletedAtIsNull(String ciValue);

    /**
     * 미성년(만 19세 미만) 목록 — 생년월일(YYYYMMDD)이 기준일보다 큰(=더 최근) 개인.
     * 기준일은 서비스에서 (오늘 - 19년)으로 계산한다. birth_date는 CHAR(8)이라 문자열 비교가 곧 날짜 비교.
     * party_id 역순 고정 정렬.
     */
    @Query(value = """
            SELECT new com.bank.customer.party.dto.MinorResponse(
                pp.partyId, p.partyName, pp.birthDate, pp.genderCode, pp.nationalityCode)
            FROM PartyPerson pp JOIN Party p ON p.partyId = pp.partyId
            WHERE pp.birthDate IS NOT NULL
              AND pp.birthDate > :thresholdYmd
              AND pp.deletedAt IS NULL
            ORDER BY pp.partyId DESC
            """,
            countQuery = """
            SELECT COUNT(pp)
            FROM PartyPerson pp
            WHERE pp.birthDate IS NOT NULL
              AND pp.birthDate > :thresholdYmd
              AND pp.deletedAt IS NULL
            """)
    Page<MinorResponse> searchMinors(@Param("thresholdYmd") String thresholdYmd, Pageable pageable);
}

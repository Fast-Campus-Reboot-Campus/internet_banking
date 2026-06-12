package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.Customer;
import com.bank.customer.customer.dto.CustomerSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerIdAndDeletedAtIsNull(Long customerId);

    Optional<Customer> findByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(
            Long partyId, String excludedStatus);

    boolean existsByPartyIdAndCustomerStatusCodeNotAndDeletedAtIsNull(
            Long partyId, String excludedStatus);

    /**
     * 직원용 고객 목록·검색. 이름은 party 도메인에 있어 Party 엔티티 조인으로 가져온다.
     *
     * <p>{@code keyword}(이름·전화 부분일치)·{@code status}·{@code grade}는 모두 선택값으로,
     * null이면 해당 조건을 적용하지 않는다(서비스에서 공백 문자열을 null로 정규화).
     * 개인(PERSONAL) 파티만 대상으로 하며, 현직 직원(party_role 'EMPLOYEE' 이며 ACTIVE)은 제외한다.
     * (직원 구분은 party_type 가 아니라 party_role 로 한다 — party_type 은 개인/법인 축.)
     * 역할이 CLOSED(퇴직)인 전(前) 직원은 일반 고객으로 다시 목록에 노출된다.
     */
    @Query(value = """
            SELECT new com.bank.customer.customer.dto.CustomerSummaryResponse(
                c.customerId, c.partyId, p.partyName, c.phone, c.email,
                c.customerGradeCode, c.customerStatusCode, c.joinedAt, c.lastTransactionAt)
            FROM Customer c JOIN Party p ON c.partyId = p.partyId
            WHERE c.deletedAt IS NULL
              AND p.partyTypeCode = 'PERSONAL'
              AND NOT EXISTS (SELECT pr.partyId FROM PartyRole pr
                              WHERE pr.partyId = p.partyId
                                AND pr.roleTypeCode   = 'EMPLOYEE'
                                AND pr.roleStatusCode = 'ACTIVE')
              AND (:keyword IS NULL OR p.partyName LIKE %:keyword% OR c.phone LIKE %:keyword%)
              AND (:status  IS NULL OR c.customerStatusCode = :status)
              AND (:grade   IS NULL OR c.customerGradeCode  = :grade)
            ORDER BY c.customerId DESC
            """,
            countQuery = """
            SELECT COUNT(c)
            FROM Customer c JOIN Party p ON c.partyId = p.partyId
            WHERE c.deletedAt IS NULL
              AND p.partyTypeCode = 'PERSONAL'
              AND NOT EXISTS (SELECT pr.partyId FROM PartyRole pr
                              WHERE pr.partyId = p.partyId
                                AND pr.roleTypeCode   = 'EMPLOYEE'
                                AND pr.roleStatusCode = 'ACTIVE')
              AND (:keyword IS NULL OR p.partyName LIKE %:keyword% OR c.phone LIKE %:keyword%)
              AND (:status  IS NULL OR c.customerStatusCode = :status)
              AND (:grade   IS NULL OR c.customerGradeCode  = :grade)
            """)
    Page<CustomerSummaryResponse> searchCustomers(
            @Param("keyword") String keyword,
            @Param("status")  String status,
            @Param("grade")   String grade,
            Pageable pageable);

    // ── 가입 통계 집계 (가입 대시보드) ─────────────────────────────────────────

    /** 코드별 건수 한 행. GROUP BY 결과 매핑용 인터페이스 프로젝션. */
    interface CodeCount {
        String getCode();
        long   getCount();
    }

    @Query("""
            SELECT c.customerStatusCode AS code, COUNT(c) AS count
            FROM Customer c
            WHERE c.deletedAt IS NULL
            GROUP BY c.customerStatusCode
            """)
    List<CodeCount> countByStatus();

    @Query("""
            SELECT c.customerGradeCode AS code, COUNT(c) AS count
            FROM Customer c
            WHERE c.deletedAt IS NULL
            GROUP BY c.customerGradeCode
            """)
    List<CodeCount> countByGrade();

    @Query("""
            SELECT c.joinChannelCode AS code, COUNT(c) AS count
            FROM Customer c
            WHERE c.deletedAt IS NULL
            GROUP BY c.joinChannelCode
            """)
    List<CodeCount> countByChannel();

    long countByDeletedAtIsNull();

    long countByJoinedAtGreaterThanEqualAndDeletedAtIsNull(OffsetDateTime since);
}

package com.bank.customer.customer.repository;

import com.bank.customer.customer.domain.CustomerAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 고객 조회 접근 감사로그 리포지토리. 스냅샷 컬럼만 보유하므로 조인 없이 단순 조회한다.
 */
public interface CustomerAccessLogRepository extends JpaRepository<CustomerAccessLog, Long> {

    /**
     * 감사로그 검색. keyword 는 직원명·고객명·행위코드 부분일치, branchCode 는 지점 한정(지점 직원용).
     * 둘 다 null 이면 전체. 최신순 고정 정렬.
     */
    @Query("""
            SELECT al FROM CustomerAccessLog al
            WHERE (:keyword IS NULL
                   OR al.accessorName        LIKE %:keyword%
                   OR al.targetCustomerName  LIKE %:keyword%
                   OR al.accessActionCode    LIKE %:keyword%)
              AND (:branchCode IS NULL OR al.accessorBranchCode = :branchCode)
            ORDER BY al.accessedAt DESC
            """)
    Page<CustomerAccessLog> search(@Param("keyword") String keyword,
                                   @Param("branchCode") String branchCode,
                                   Pageable pageable);
}

package com.bank.customer.code.repository;

import com.bank.customer.code.domain.CustCodeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustCodeMasterRepository
        extends JpaRepository<CustCodeMaster, CustCodeMaster.CustCodeId> {

    /** 그룹 내 전체 코드 (정렬순) */
    List<CustCodeMaster> findByCodeGroupIdOrderBySortOrderAsc(String codeGroupId);

    /** 그룹 내 유효 코드만 (오늘 날짜 기준) */
    @Query("""
            SELECT c FROM CustCodeMaster c
            WHERE c.codeGroupId = :groupId
              AND c.effectiveStartDate <= :today
              AND (c.effectiveEndDate IS NULL OR c.effectiveEndDate >= :today)
            ORDER BY c.sortOrder ASC
            """)
    List<CustCodeMaster> findActiveByGroup(@Param("groupId") String groupId,
                                           @Param("today") String today);
}

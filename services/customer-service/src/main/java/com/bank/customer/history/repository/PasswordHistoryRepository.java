package com.bank.customer.history.repository;

import com.bank.customer.history.domain.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /** 특정 고객의 window 내 비밀번호 변경 횟수 — FDS PASSWORD_CHANGE_FREQ 룰용 */
    @Query("""
            SELECT COUNT(ph)
            FROM PasswordHistory ph
            WHERE ph.customerId = :customerId
              AND ph.createdAt >= :since
            """)
    long countChangesSince(@Param("customerId") Long customerId,
                           @Param("since") OffsetDateTime since);

    /** 최근 N건의 비밀번호 해시 조회 — 재사용 방지 검증용 */
    @Query("""
            SELECT ph.passwordHash
            FROM PasswordHistory ph
            WHERE ph.credentialId = :credentialId
            ORDER BY ph.passwordHistoryId DESC
            LIMIT :limit
            """)
    List<String> findRecentHashes(@Param("credentialId") Long credentialId,
                                  @Param("limit") int limit);
}

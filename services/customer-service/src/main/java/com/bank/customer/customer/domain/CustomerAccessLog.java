package com.bank.customer.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 고객 조회 접근 감사로그. Append-only — "누가(직원) 누구를(고객) 무엇을(행위) 왜(사유) 언제" 조회했는가.
 *
 * <p>직원명·역할·지점·고객명은 조회 시점 스냅샷으로 적재한다(조회는 조인 없이 단순 SELECT).
 * 직원 신원은 게이트웨이가 JWT 에서 주입한 X-Employee-Id(검증된 employee_id)로만 채워진다.
 */
@Getter
@Entity
@Table(name = "customer_access_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustomerAccessLog {

    /** 고객 상세 조회 */
    public static final String ACTION_CUSTOMER_DETAIL = "CUSTOMER_DETAIL";
    /** 연락처 등 민감정보 열람(조회 사유 필요) */
    public static final String ACTION_CONTACT_VIEW    = "CONTACT_VIEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_access_log_id")
    private Long customerAccessLogId;

    @Column(name = "accessor_employee_id", nullable = false)
    private Long accessorEmployeeId;

    @Column(name = "accessor_name", length = 100)
    private String accessorName;

    @Column(name = "accessor_role", length = 40)
    private String accessorRole;

    @Column(name = "accessor_branch_code", length = 10)
    private String accessorBranchCode;

    @Column(name = "target_customer_id", nullable = false)
    private Long targetCustomerId;

    @Column(name = "target_customer_name", length = 100)
    private String targetCustomerName;

    @Column(name = "access_action_code", nullable = false, length = 40)
    private String accessActionCode;

    @Column(name = "access_reason", length = 500)
    private String accessReason;

    @Column(name = "accessed_at", nullable = false)
    private OffsetDateTime accessedAt;

    public static CustomerAccessLog of(Long accessorEmployeeId, String accessorName,
                                       String accessorRole, String accessorBranchCode,
                                       Long targetCustomerId, String targetCustomerName,
                                       String actionCode, String reason, OffsetDateTime accessedAt) {
        return CustomerAccessLog.builder()
                .accessorEmployeeId(accessorEmployeeId)
                .accessorName(accessorName)
                .accessorRole(accessorRole)
                .accessorBranchCode(accessorBranchCode)
                .targetCustomerId(targetCustomerId)
                .targetCustomerName(targetCustomerName)
                .accessActionCode(actionCode)
                .accessReason(reason)
                .accessedAt(accessedAt)
                .build();
    }
}

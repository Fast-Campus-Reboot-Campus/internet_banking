package com.bank.deposit.domain.entity;

import com.bank.deposit.domain.enums.QueryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "staff_query_logs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class StaffQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "customer_id", length = 30, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false)
    private QueryType queryType;

    @Column(name = "target_table", length = 50, nullable = false)
    private String targetTable;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "query_at", nullable = false)
    @Builder.Default
    private OffsetDateTime queryAt = OffsetDateTime.now();

    @Column(name = "result_count", nullable = false)
    @Builder.Default
    private Integer resultCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

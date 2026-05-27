package com.bank.common.audit;

import com.bank.common.persistence.CreatedOnlyBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ERD STATUS_HISTORY 매핑. append-only 이력 테이블.
 *
 * 분산 보관 정책: 각 도메인 DB 가 자체 status_history 테이블을 보유한다.
 * (통합 조회는 추후 read-replica + view 로 구성)
 */
@Getter
@Entity
@Table(name = "status_history",
        indexes = {
                @Index(name = "idx_status_history_target",
                        columnList = "target_domain_cd, target_table_cd, target_id, changed_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StatusHistory extends CreatedOnlyBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sthist_id")
    private Long sthistId;

    @Column(name = "target_domain_cd", length = 30, nullable = false)
    private String targetDomainCd;

    @Column(name = "target_table_cd", length = 50, nullable = false)
    private String targetTableCd;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "before_status_cd", length = 50)
    private String beforeStatusCd;

    @Column(name = "after_status_cd", length = 50, nullable = false)
    private String afterStatusCd;

    @Column(name = "change_reason_cd", length = 50)
    private String changeReasonCd;

    @Column(name = "change_remark", length = 500)
    private String changeRemark;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;
}

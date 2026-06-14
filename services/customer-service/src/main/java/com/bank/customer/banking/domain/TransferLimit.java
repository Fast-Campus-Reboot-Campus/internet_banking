package com.bank.customer.banking.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 고객당 인터넷뱅킹 이체한도(1일/1회). 계좌당 출금한도(deposit-service)와 구분된다.
 * 온라인에서는 감액만 허용(증액은 영업점·본인인증) — 감액 검증은 서비스 레이어에서 수행한다.
 */
@Getter
@Entity
@Table(name = "transfer_limit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TransferLimit extends BaseEntity {

    /** 행이 없는 고객의 기본 이체한도(1일/1회). */
    public static final long DEFAULT_LIMIT = 1_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_limit_id")
    private Long transferLimitId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "daily_limit", nullable = false)
    private Long dailyLimit;

    @Column(name = "once_limit", nullable = false)
    private Long onceLimit;

    public void updateLimits(long dailyLimit, long onceLimit) {
        this.dailyLimit = dailyLimit;
        this.onceLimit  = onceLimit;
    }
}

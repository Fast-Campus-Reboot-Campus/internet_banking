package com.bank.commonaccount.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 공통 계좌 마스터 (common_db.common_account).
 *
 * 여러 서비스가 공유하는 계좌 마스터의 loan-service 측 매핑.
 * 별도 패키지(com.bank.commonaccount)에 두어 기본(loan_db) EMF 스캔 범위와 겹치지 않게 한다.
 * common datasource 전용 EMF/리포지토리({@code commonEntityManagerFactory})에 바인딩된다.
 */
@Getter
@Entity
@Table(name = "common_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CommonAccount {

    public static final String TYPE_GENERAL = "GENERAL";
    public static final String TYPE_VIRTUAL = "VIRTUAL";
    public static final String TYPE_SYSTEM  = "SYSTEM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_no", length = 30, unique = true)
    private String accountNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "account_type_cd", length = 30)
    private String accountTypeCd;

    @Column(name = "account_nickname", length = 100)
    private String accountNickname;

    @Column(name = "bank_cd", length = 10)
    private String bankCd;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "currency_cd", length = 3)
    private String currencyCd;

    @Column(name = "account_status", length = 20)
    private String accountStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}

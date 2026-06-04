package com.bank.customer.banking.domain;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "withdrawal_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WithdrawalAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_account_id")
    private Long withdrawalAccountId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "account_alias", length = 100)
    private String accountAlias;

    @Column(name = "registration_type", nullable = false, length = 20)
    private String registrationType;

    @Column(name = "priority_order", nullable = false)
    private Short priorityOrder;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    public void updatePriorityOrder(Short order) {
        this.priorityOrder = order;
    }
}

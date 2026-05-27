package com.bank.payment.outbound.feign.dto;

/**
 * B-5 POST /api/v1/balances/withdraw/cancel 응답 data.
 * 합의서 v1.0 시트13. deposit이 새 row INSERT (reversal pattern).
 */
public record WithdrawCancelData(
        String cancelTransactionNo,             // 취소 거래 식별자 (새 row)
        String originalDepositTransactionNo,    // 원 출금 거래 식별자
        String accountNo,
        Long amount,
        Long balanceBefore,                     // 취소 직전 잔액
        Long balanceAfter,                      // 취소 후 잔액 (원래대로 복원)
        String canceledAt
) {}

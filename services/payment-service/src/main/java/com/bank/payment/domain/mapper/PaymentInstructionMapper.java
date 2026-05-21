package com.bank.payment.domain.mapper;

import com.bank.payment.domain.PaymentInstruction;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface PaymentInstructionMapper {

    void insert(PaymentInstruction paymentInstruction);

    PaymentInstruction selectById(@Param("paymentInstructionId") String paymentInstructionId);

    int updateStatus(@Param("paymentInstructionId") String paymentInstructionId,
                     @Param("status") String status,
                     @Param("completedAt") LocalDateTime completedAt,
                     @Param("failureCategory") String failureCategory,
                     @Param("version") Integer version);

    void updateReceiverHolderSnap(@Param("paymentInstructionId") String paymentInstructionId,
                                  @Param("receiverHolderNameSnap") String receiverHolderNameSnap,
                                  @Param("holderInquiryAt") LocalDateTime holderInquiryAt);
}

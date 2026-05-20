package com.bank.payment.domain.mapper;

import com.bank.payment.domain.Ledger;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LedgerMapper {

    void insert(Ledger ledger);

    List<Ledger> selectByPaymentId(@Param("paymentInstructionId") String paymentInstructionId);
}

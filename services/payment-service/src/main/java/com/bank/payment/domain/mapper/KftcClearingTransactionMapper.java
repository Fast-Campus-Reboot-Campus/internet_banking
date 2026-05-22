package com.bank.payment.domain.mapper;

import com.bank.payment.domain.KftcClearingTransaction;
import org.apache.ibatis.annotations.Param;

public interface KftcClearingTransactionMapper {

    void insert(KftcClearingTransaction clearingTransaction);

    KftcClearingTransaction selectByClearingNo(@Param("clearingNo") String clearingNo);

    void updateSettled(@Param("piId") String piId,
                       @Param("settledAt") String settledAt,
                       @Param("settlementDate") String settlementDate);
}

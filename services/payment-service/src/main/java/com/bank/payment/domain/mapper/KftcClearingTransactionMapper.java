package com.bank.payment.domain.mapper;

import com.bank.payment.domain.KftcClearingTransaction;
import org.apache.ibatis.annotations.Param;

public interface KftcClearingTransactionMapper {

    void insert(KftcClearingTransaction clearingTransaction);

    KftcClearingTransaction selectByClearingNo(@Param("clearingNo") String clearingNo);

    void updateSettled(@Param("piId") String piId,
                       @Param("settledAt") String settledAt,
                       @Param("settlementDate") String settlementDate);

    /** F2 거절: clearing_status=REJECTED + reject_code/message 박제. settled_at 등 건드리지 않음(P-015). */
    void updateRejected(@Param("piId") String piId,
                        @Param("rejectCode") String rejectCode,
                        @Param("rejectMessage") String rejectMessage);
}

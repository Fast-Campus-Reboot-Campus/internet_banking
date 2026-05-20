package com.bank.payment.domain.mapper;

import com.bank.payment.domain.ExternalCall;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ExternalCallMapper {

    void insert(ExternalCall externalCall);

    int updateResponse(@Param("callId") String callId,
                       @Param("responseStatusCode") Integer responseStatusCode,
                       @Param("responseHeader") String responseHeader,
                       @Param("responseBody") String responseBody,
                       @Param("businessResponseCode") String businessResponseCode,
                       @Param("responseMessage") String responseMessage,
                       @Param("result") String result,
                       @Param("responseTimeMs") Integer responseTimeMs,
                       @Param("respondedAt") LocalDateTime respondedAt);
}

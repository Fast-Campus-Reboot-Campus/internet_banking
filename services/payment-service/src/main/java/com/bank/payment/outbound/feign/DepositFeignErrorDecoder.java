package com.bank.payment.outbound.feign;

import com.bank.payment.common.exception.DepositInboundFailureException;
import com.bank.payment.outbound.feign.dto.DepositErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.io.InputStream;

/**
 * deposit-service 4xx/5xx 응답을 DepositInboundFailureException 으로 변환.
 * deposit 에러 바디: {code: ErrorCode.name(), message, errors, timestamp}
 */
public class DepositFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public DepositFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String depositCode = "DEPOSIT_HTTP_" + response.status();
        String message = "deposit HTTP " + response.status();

        if (response.body() != null) {
            try (InputStream body = response.body().asInputStream()) {
                DepositErrorResponse err = objectMapper.readValue(body, DepositErrorResponse.class);
                if (err.code() != null) depositCode = err.code();
                if (err.message() != null) message = err.message();
            } catch (IOException ignored) {
                // body 파싱 실패 시 HTTP 상태 코드 기반 코드 유지
            }
        }

        return new DepositInboundFailureException(depositCode, message);
    }
}

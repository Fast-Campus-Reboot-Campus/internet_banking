package com.bank.payment.api;

import com.bank.payment.common.exception.PaymentCancelConflictException;
import com.bank.payment.common.exception.PaymentNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 운영자 취소 엔드포인트 전용 예외 매핑. 기존 POST /api/v1/payments 흐름 예외는 건드리지 않음.
 * PaymentNotFoundException(404), PaymentCancelConflictException(409)만 처리.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PaymentNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PaymentCancelConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(PaymentCancelConflictException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }
}

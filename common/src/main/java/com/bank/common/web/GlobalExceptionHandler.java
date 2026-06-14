package com.bank.common.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ErrorCode c = ex.getErrorCode();
        log.warn("BusinessException: {} - {}", c.getCode(), ex.getMessage());
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), ex.getMessage(), ex.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .collect(Collectors.toList());
        Map<String, Object> data = Map.of("errors", errors);
        CommonErrorCode c = CommonErrorCode.COMMON_400;
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), "요청 검증에 실패했습니다.", data));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of(
                        "field", cv.getPropertyPath().toString(),
                        "message", cv.getMessage()))
                .collect(Collectors.toList());
        CommonErrorCode c = CommonErrorCode.COMMON_400;
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), "요청 검증에 실패했습니다.", Map.of("errors", errors)));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        CommonErrorCode c = CommonErrorCode.COMMON_400;
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), "필수 헤더 누락: " + ex.getHeaderName()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        CommonErrorCode c = CommonErrorCode.COMMON_404;
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), c.getMessage()));
    }

    // Boot 3 는 매핑되지 않은 경로를 정적 리소스로 폴백하다 NoResourceFoundException 을 던진다.
    // NoHandlerFoundException 과 동일하게 404 로 응답한다(미처리 시 Exception 폴백으로 500 이 됨).
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        CommonErrorCode c = CommonErrorCode.COMMON_404;
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), c.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        CommonErrorCode c = CommonErrorCode.COMMON_400;
        log.warn("IllegalArgument: {}", ex.getMessage());
        return ResponseEntity.status(c.getStatus())
                .body(ApiResponse.error(c.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFallback(Exception ex) {
        log.error("Unhandled exception", ex);
        CommonErrorCode c = CommonErrorCode.COMMON_500;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}

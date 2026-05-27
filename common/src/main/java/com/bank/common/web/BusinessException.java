package com.bank.common.web;

import lombok.Getter;

/**
 * 도메인 비즈니스 예외. ErrorCode 를 들고 다니며 GlobalExceptionHandler 가 응답 envelope 로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, null, message);
    }

    public BusinessException(ErrorCode errorCode, Object data, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
}

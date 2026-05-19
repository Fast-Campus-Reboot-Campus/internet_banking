package com.bank.common.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    COMMON_400(HttpStatus.BAD_REQUEST,           "잘못된 요청입니다."),
    COMMON_401(HttpStatus.UNAUTHORIZED,          "인증이 필요합니다."),
    COMMON_403(HttpStatus.FORBIDDEN,             "접근 권한이 없습니다."),
    COMMON_404(HttpStatus.NOT_FOUND,             "리소스를 찾을 수 없습니다."),
    COMMON_409(HttpStatus.CONFLICT,              "리소스 충돌입니다."),
    COMMON_422(HttpStatus.UNPROCESSABLE_ENTITY,  "요청을 처리할 수 없습니다."),
    COMMON_500(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    COMMON_INVALID_CODE(HttpStatus.BAD_REQUEST,  "유효하지 않은 코드값입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}

package com.bank.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 의 응답 envelope.
 *   { "code": "OK" | "<DOMAIN>_<NNN>", "message": "...", "data": {...} | null }
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(String code, String message, T data) {

    public static final String OK_CODE = "OK";
    public static final String OK_MESSAGE = "OK";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK_CODE, OK_MESSAGE, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(OK_CODE, OK_MESSAGE, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

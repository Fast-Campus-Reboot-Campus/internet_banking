package com.bank.ai.support;

import com.bank.common.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    INFERENCE_UNAVAILABLE("AI_001", "추론 서버에 연결할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE),
    INFERENCE_FAILED("AI_002", "추론 요청이 실패했습니다", HttpStatus.BAD_GATEWAY),
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;
}

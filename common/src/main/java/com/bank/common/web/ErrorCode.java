package com.bank.common.web;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 ErrorCode enum 이 구현하는 공통 인터페이스.
 * 코드명은 "<DOMAIN>_<NNN>" 규칙. 예: LOAN_001, AUTH_401.
 */
public interface ErrorCode {

    String getCode();

    HttpStatus getStatus();

    String getMessage();
}

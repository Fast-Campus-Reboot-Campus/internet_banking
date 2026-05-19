package com.bank.loan.support;

import com.bank.common.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * LON 도메인 에러코드. "<DOMAIN>_<NNN>" 규칙.
 * 상품(001-009) / 신청(010-029) / 심사(030-049) / 계약(050-069) /
 * 실행(070-079) / 상환(080-099) / 연체(100-109) / 종결(110-119) 구간 사용.
 */
@Getter
@RequiredArgsConstructor
public enum LoanErrorCode implements ErrorCode {

    LOAN_001(HttpStatus.CONFLICT,  "이미 존재하는 상품 코드입니다."),
    LOAN_002(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    LOAN_003(HttpStatus.BAD_REQUEST, "상품 금리/금액/기간 범위가 유효하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}

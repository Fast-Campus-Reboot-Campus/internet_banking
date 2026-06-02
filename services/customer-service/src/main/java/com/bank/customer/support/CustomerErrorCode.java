package com.bank.customer.support;

import com.bank.common.web.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * CUST 도메인 에러코드.
 * 회원(001-009) / 인증(010-019) / 계정(020-029) 구간 사용.
 */
@Getter
@RequiredArgsConstructor
public enum CustomerErrorCode implements ErrorCode {

    // 회원
    CUST_001(HttpStatus.CONFLICT,   "이미 사용 중인 로그인 ID입니다."),
    CUST_002(HttpStatus.NOT_FOUND,  "고객 정보를 찾을 수 없습니다."),
    CUST_003(HttpStatus.CONFLICT,   "이미 가입된 고객입니다."),

    // 인증
    CUST_010(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    CUST_011(HttpStatus.FORBIDDEN,    "계정이 잠겨 있습니다. 비밀번호 5회 오류."),
    CUST_012(HttpStatus.FORBIDDEN,    "탈퇴하거나 비활성화된 계정입니다."),
    CUST_013(HttpStatus.UNAUTHORIZED, "비밀번호가 만료되었습니다."),

    // 설정
    CUST_020(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    // 인증서 로그인 (030-039)
    CUST_030(HttpStatus.NOT_FOUND,     "인증서를 찾을 수 없습니다."),
    CUST_031(HttpStatus.UNAUTHORIZED,  "인증서가 만료되었습니다."),
    CUST_032(HttpStatus.FORBIDDEN,     "폐기된 인증서입니다."),
    CUST_033(HttpStatus.UNAUTHORIZED,  "인증서 PIN이 올바르지 않습니다."),
    CUST_034(HttpStatus.FORBIDDEN,     "인증서가 잠겨 있습니다."),

    // QR 로그인 (040-049)
    CUST_040(HttpStatus.NOT_FOUND,   "QR 토큰을 찾을 수 없습니다."),
    CUST_041(HttpStatus.GONE,        "QR 코드가 만료되었습니다."),
    CUST_042(HttpStatus.CONFLICT,    "이미 처리된 QR 코드입니다."),

    // 출금계좌 관리 (050-059)
    CUST_050(HttpStatus.NOT_FOUND,  "출금계좌를 찾을 수 없습니다."),
    CUST_051(HttpStatus.CONFLICT,   "이미 등록된 출금계좌입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}

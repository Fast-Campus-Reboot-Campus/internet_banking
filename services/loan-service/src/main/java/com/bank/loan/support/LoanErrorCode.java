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
    LOAN_003(HttpStatus.BAD_REQUEST, "상품 금리/금액/기간 범위가 유효하지 않습니다."),
    LOAN_004(HttpStatus.CONFLICT,  "이미 단종된 상품입니다."),
    LOAN_005(HttpStatus.CONFLICT,  "동일 상품/조건의 활성 우대금리 정책이 이미 존재합니다."),

    LOAN_010(HttpStatus.BAD_REQUEST, "판매 중인 상품이 아닙니다."),
    LOAN_011(HttpStatus.BAD_REQUEST, "요청 금액 또는 기간이 상품 범위를 벗어났습니다."),
    LOAN_012(HttpStatus.NOT_FOUND,   "대출 신청을 찾을 수 없습니다."),
    LOAN_013(HttpStatus.CONFLICT,    "현재 상태에서는 신청을 취소할 수 없습니다."),

    LOAN_020(HttpStatus.BAD_REQUEST, "본인확인에 실패했습니다."),
    LOAN_021(HttpStatus.NOT_FOUND,   "본인확인 내역을 찾을 수 없습니다."),

    LOAN_030(HttpStatus.NOT_FOUND,   "신용조회 동의 내역을 찾을 수 없습니다."),
    LOAN_031(HttpStatus.CONFLICT,    "이미 철회된 동의입니다."),

    LOAN_032(HttpStatus.UNPROCESSABLE_ENTITY, "신용평가 사전조건이 충족되지 않았습니다. (가심사 PASS 필요)"),
    LOAN_033(HttpStatus.CONFLICT,             "이미 신용평가가 수행되었습니다. (신청당 1건)"),
    LOAN_034(HttpStatus.NOT_FOUND,            "신용평가 내역을 찾을 수 없습니다."),

    LOAN_035(HttpStatus.UNPROCESSABLE_ENTITY, "DSR 산정 사전조건이 충족되지 않았습니다. (신용평가 완료 필요)"),
    LOAN_036(HttpStatus.CONFLICT,             "이미 DSR 산정이 수행되었습니다. (신청당 1건)"),
    LOAN_037(HttpStatus.NOT_FOUND,            "DSR 산정 내역을 찾을 수 없습니다."),

    LOAN_038(HttpStatus.UNPROCESSABLE_ENTITY, "본심사 사전조건이 충족되지 않았습니다. (PRESCREENED + CB(APPROVE/REVIEW) + DSR PASS 필요)"),
    LOAN_039(HttpStatus.CONFLICT,             "이미 본심사가 수행되었습니다. (신청당 1건)"),

    LOAN_040(HttpStatus.BAD_REQUEST, "서류 업로드에 실패했습니다."),
    LOAN_041(HttpStatus.NOT_FOUND,   "서류를 찾을 수 없습니다."),
    LOAN_042(HttpStatus.NOT_FOUND,            "본심사 내역을 찾을 수 없습니다."),
    LOAN_043(HttpStatus.BAD_REQUEST,          "수동 체크 항목 코드가 유효하지 않습니다. (자동 적재 항목은 직접 추가 불가)"),
    LOAN_044(HttpStatus.UNPROCESSABLE_ENTITY, "본심사 정정 가능 상태가 아닙니다. (신청 APPROVED/REJECTED 필요, 약정 진입 후 불가)"),

    LOAN_045(HttpStatus.NOT_FOUND,            "가심사 내역을 찾을 수 없습니다."),
    LOAN_046(HttpStatus.CONFLICT,             "이미 가심사가 수행되었습니다. (신청당 1건)"),
    LOAN_047(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 가심사를 수행할 수 없습니다. (SUBMITTED 필요)"),

    LOAN_048(HttpStatus.UNPROCESSABLE_ENTITY, "본심사 자동 결정 불가. CB 결정이 REVIEW 인 경우 수동 본심사가 필요합니다."),

    LOAN_050(HttpStatus.NOT_FOUND,   "담보를 찾을 수 없습니다."),
    LOAN_051(HttpStatus.CONFLICT,    "이미 해제된 담보입니다."),

    LOAN_052(HttpStatus.UNPROCESSABLE_ENTITY, "LTV 산정 사전조건이 충족되지 않았습니다. (담보 감정평가 DONE 필요 또는 담보 상태 위반)"),
    LOAN_053(HttpStatus.CONFLICT,             "이미 LTV 산정이 수행되었습니다. (담보당 1건)"),
    LOAN_054(HttpStatus.NOT_FOUND,            "LTV 산정 내역을 찾을 수 없습니다."),

    LOAN_060(HttpStatus.UNPROCESSABLE_ENTITY, "약정 가능한 신청 상태가 아닙니다. (APPROVED 필요)"),
    LOAN_061(HttpStatus.BAD_REQUEST,          "약정 조건이 신청 범위를 벗어났습니다."),
    LOAN_062(HttpStatus.NOT_FOUND,            "대출 계약을 찾을 수 없습니다."),
    LOAN_063(HttpStatus.UNPROCESSABLE_ENTITY, "현재 계약 상태에서는 자금 인출이 불가합니다."),
    LOAN_064(HttpStatus.BAD_REQUEST,          "약정한도를 초과하여 인출할 수 없습니다."),

    LOAN_080(HttpStatus.NOT_FOUND,            "상환계좌를 찾을 수 없습니다."),
    LOAN_081(HttpStatus.CONFLICT,             "이미 등록된 상환계좌입니다. (계약당 1건)"),
    LOAN_082(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 상환계좌를 검증할 수 없습니다. (REGISTERED 필요)"),
    LOAN_083(HttpStatus.UNPROCESSABLE_ENTITY, "상환계좌가 검증되지 않았습니다. (drawdown 사전조건)"),
    LOAN_084(HttpStatus.UNPROCESSABLE_ENTITY, "지원하지 않는 상환방식입니다. (현재 EQUAL 만 지원)"),

    LOAN_090(HttpStatus.NOT_FOUND,            "상환 회차를 찾을 수 없습니다."),
    LOAN_091(HttpStatus.CONFLICT,             "이미 납부되었거나 상환 가능한 상태가 아닌 회차입니다."),
    LOAN_092(HttpStatus.UNPROCESSABLE_ENTITY, "현재 계약 상태에서는 중도상환이 불가합니다. (ACTIVE 필요)"),
    LOAN_093(HttpStatus.BAD_REQUEST,          "중도상환 금액이 유효하지 않습니다. (1 이상)"),
    LOAN_094(HttpStatus.UNPROCESSABLE_ENTITY, "중도상환 금액이 잔여 원금을 초과합니다."),
    LOAN_095(HttpStatus.NOT_FOUND,            "상환 거래를 찾을 수 없습니다."),
    LOAN_096(HttpStatus.UNPROCESSABLE_ENTITY, "역분개 대상 조건을 충족하지 않습니다. (SUCCESS + SCHEDULED 필요)"),
    LOAN_097(HttpStatus.CONFLICT,             "이미 역분개된 상환 거래입니다."),
    LOAN_098(HttpStatus.UNPROCESSABLE_ENTITY, "부분상환 금액이 회차 잔액을 초과합니다."),

    LOAN_100(HttpStatus.NOT_FOUND,            "활성 연체 정보가 없습니다."),

    LOAN_110(HttpStatus.BAD_REQUEST,          "금리 변경 값이 유효하지 않습니다."),

    LOAN_120(HttpStatus.UNPROCESSABLE_ENTITY, "종결 가능한 계약 상태가 아닙니다. (ACTIVE 필요)"),
    LOAN_121(HttpStatus.UNPROCESSABLE_ENTITY, "잔여 원금이 남아있어 정상 종결할 수 없습니다."),
    LOAN_122(HttpStatus.UNPROCESSABLE_ENTITY, "활성 회차(DUE/OVERDUE)가 남아있어 정상 종결할 수 없습니다."),
    LOAN_123(HttpStatus.CONFLICT,             "이미 종결된 계약입니다."),
    LOAN_124(HttpStatus.NOT_FOUND,            "종결 정보가 없습니다."),

    LOAN_130(HttpStatus.NOT_FOUND,            "만기 정보를 찾을 수 없습니다."),
    LOAN_131(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 만기 연장이 불가합니다. (ACTIVE/MATURED 필요)"),

    LOAN_140(HttpStatus.NOT_FOUND,            "증명서를 찾을 수 없습니다."),

    LOAN_150(HttpStatus.NOT_FOUND,            "신용정보 신고 내역을 찾을 수 없습니다."),

    LOAN_160(HttpStatus.NOT_FOUND,            "영업일 캘린더 항목을 찾을 수 없습니다."),
    LOAN_161(HttpStatus.CONFLICT,             "이미 등록된 캘린더 일자입니다."),
    LOAN_162(HttpStatus.BAD_REQUEST,          "캘린더 일자 형식이 유효하지 않습니다. (YYYYMMDD)"),

    LOAN_170(HttpStatus.NOT_FOUND,            "보증 약정을 찾을 수 없습니다."),
    LOAN_171(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 서명할 수 없습니다. (REGISTERED 필요)"),
    LOAN_172(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 취소할 수 없습니다."),
    LOAN_173(HttpStatus.UNPROCESSABLE_ENTITY, "보증 약정 등록 가능한 신청 상태가 아닙니다. (SUBMITTED/PRESCREENED/REVIEWING/APPROVED 필요)"),
    LOAN_174(HttpStatus.CONFLICT,             "동일 신청에 동일 보증인의 활성 약정이 이미 존재합니다."),
    LOAN_175(HttpStatus.UNPROCESSABLE_ENTITY, "미서명(REGISTERED) 보증 약정이 남아있어 약정 체결이 불가합니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}

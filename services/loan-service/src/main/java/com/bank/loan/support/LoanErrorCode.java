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

    LOAN_029(HttpStatus.SERVICE_UNAVAILABLE, "외부 신용평가 엔진 일시 장애로 가심사를 수행할 수 없습니다. 잠시 후 다시 시도해 주세요."),

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
    LOAN_049(HttpStatus.UNPROCESSABLE_ENTITY, "본심사가 권고(PENDING_APPROVAL) 상태가 아닙니다. (확정 불가)"),

    LOAN_050(HttpStatus.NOT_FOUND,   "담보를 찾을 수 없습니다."),
    LOAN_051(HttpStatus.CONFLICT,    "이미 해제된 담보입니다."),

    LOAN_052(HttpStatus.UNPROCESSABLE_ENTITY, "LTV 산정 사전조건이 충족되지 않았습니다. (담보 감정평가 DONE 필요 또는 담보 상태 위반)"),
    LOAN_053(HttpStatus.CONFLICT,             "이미 LTV 산정이 수행되었습니다. (담보당 1건)"),
    LOAN_054(HttpStatus.NOT_FOUND,            "LTV 산정 내역을 찾을 수 없습니다."),

    LOAN_055(HttpStatus.UNPROCESSABLE_ENTITY, "서류 검증이 완료되지 않았습니다. NEEDS_RESUBMIT 또는 HOLD 상태의 서류가 남아있습니다."),

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
    LOAN_099(HttpStatus.UNPROCESSABLE_ENTITY, "EARLY 역분개 사전조건 미충족. (최신 EARLY 만 지원 + V_new 회차 모두 DUE/OVERDUE 필요)"),

    LOAN_100(HttpStatus.NOT_FOUND,            "활성 연체 정보가 없습니다."),

    LOAN_110(HttpStatus.BAD_REQUEST,          "금리 변경 값이 유효하지 않습니다."),

    LOAN_120(HttpStatus.UNPROCESSABLE_ENTITY, "종결 가능한 계약 상태가 아닙니다. (ACTIVE 필요)"),
    LOAN_121(HttpStatus.UNPROCESSABLE_ENTITY, "잔여 원금이 남아있어 정상 종결할 수 없습니다."),
    LOAN_122(HttpStatus.UNPROCESSABLE_ENTITY, "활성 회차(DUE/OVERDUE)가 남아있어 정상 종결할 수 없습니다."),
    LOAN_123(HttpStatus.CONFLICT,             "이미 종결된 계약입니다."),
    LOAN_124(HttpStatus.NOT_FOUND,            "종결 정보가 없습니다."),
    LOAN_125(HttpStatus.UNPROCESSABLE_ENTITY, "대위변제 사전조건이 충족되지 않았습니다. (활성 보증보험 ISSUED 또는 SIGNED 보증인 1명 이상 필요)"),
    LOAN_126(HttpStatus.CONFLICT,             "이미 WRITE_OFF 또는 SUBROGATION 종결된 계약입니다."),

    LOAN_130(HttpStatus.NOT_FOUND,            "만기 정보를 찾을 수 없습니다."),
    LOAN_131(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 만기 연장이 불가합니다. (ACTIVE/MATURED 필요)"),

    LOAN_140(HttpStatus.NOT_FOUND,            "증명서를 찾을 수 없습니다."),

    LOAN_150(HttpStatus.NOT_FOUND,            "신용정보 신고 내역을 찾을 수 없습니다."),
    LOAN_151(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 ACK 처리할 수 없습니다. (SENT 필요)"),
    LOAN_152(HttpStatus.CONFLICT,             "이미 ACKED 된 신고는 재전송할 수 없습니다."),
    LOAN_153(HttpStatus.BAD_REQUEST,          "ACK 페이로드가 유효하지 않습니다."),

    LOAN_160(HttpStatus.NOT_FOUND,            "영업일 캘린더 항목을 찾을 수 없습니다."),
    LOAN_161(HttpStatus.CONFLICT,             "이미 등록된 캘린더 일자입니다."),
    LOAN_162(HttpStatus.BAD_REQUEST,          "캘린더 일자 형식이 유효하지 않습니다. (YYYYMMDD)"),
    LOAN_163(HttpStatus.UNPROCESSABLE_ENTITY, "지정한 범위 안에서 영업일을 찾지 못했습니다."),

    LOAN_170(HttpStatus.NOT_FOUND,            "보증 약정을 찾을 수 없습니다."),
    LOAN_171(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 서명할 수 없습니다. (REGISTERED 필요)"),
    LOAN_172(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 취소할 수 없습니다."),
    LOAN_173(HttpStatus.UNPROCESSABLE_ENTITY, "보증 약정 등록 가능한 신청 상태가 아닙니다. (SUBMITTED/PRESCREENED/REVIEWING/APPROVED 필요)"),
    LOAN_174(HttpStatus.CONFLICT,             "동일 신청에 동일 보증인의 활성 약정이 이미 존재합니다."),
    LOAN_175(HttpStatus.UNPROCESSABLE_ENTITY, "미서명(REGISTERED) 보증 약정이 남아있어 약정 체결이 불가합니다."),
    LOAN_176(HttpStatus.UNPROCESSABLE_ENTITY, "보증 필수 상품의 활성 SIGNED 보증인이 부족합니다. (실행 사전조건 미충족)"),

    LOAN_180(HttpStatus.NOT_FOUND,            "보증보험 정보를 찾을 수 없습니다."),
    LOAN_181(HttpStatus.CONFLICT,             "이미 발급된 활성 보증보험이 존재합니다. (계약당 1건)"),
    LOAN_182(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 보증보험을 취소할 수 없습니다. (ISSUED 필요)"),
    LOAN_183(HttpStatus.UNPROCESSABLE_ENTITY, "보증보험 발급 가능한 계약 상태가 아닙니다. (SIGNED/ACTIVE 필요)"),
    LOAN_184(HttpStatus.UNPROCESSABLE_ENTITY, "보증보험이 등록된 계약은 활성 ISSUED 보증보험이 필요합니다. (drawdown 사전조건)"),
    LOAN_185(HttpStatus.UNPROCESSABLE_ENTITY, "대출실행 출금 요청이 실패했습니다."),
    LOAN_186(HttpStatus.UNPROCESSABLE_ENTITY, "역분개 환급 이체 요청이 실패했습니다."),
    LOAN_187(HttpStatus.UNPROCESSABLE_ENTITY, "온라인 상환 결제 요청이 실패했습니다."),

    LOAN_190(HttpStatus.NOT_FOUND,            "알림 outbox 를 찾을 수 없습니다."),
    LOAN_191(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 재전송할 수 없습니다. (FAILED/DEAD 필요)"),

    // 편향 검증·승인자 단계 (192–199)
    LOAN_192(HttpStatus.UNPROCESSABLE_ENTITY, "본심사가 편향 검증(BIAS_REVIEWING) 상태가 아닙니다."),
    LOAN_193(HttpStatus.UNPROCESSABLE_ENTITY, "편향 검증 리포트가 아직 생성되지 않았습니다. 리포트 생성 후 다시 시도하세요."),
    LOAN_194(HttpStatus.UNPROCESSABLE_ENTITY, "편향 검증 결과가 BLOCKED 입니다. 결정을 정정하거나 상급자 우회 승인을 받으세요."),
    LOAN_195(HttpStatus.UNPROCESSABLE_ENTITY, "본심사가 승인자 대기(PENDING_APPROVER) 상태가 아닙니다."),
    LOAN_196(HttpStatus.UNPROCESSABLE_ENTITY, "승인자와 심사원이 동일합니다. 4-eye 원칙에 따라 다른 사람이 승인해야 합니다."),
    LOAN_197(HttpStatus.BAD_REQUEST,          "결정 변경(override) 시 사유 코드(overrideReasonCd)가 필요합니다."),
    LOAN_198(HttpStatus.BAD_REQUEST,          "OVERRIDE_APPROVED 시 승인 금액·금리·기간이 필요합니다."),
    LOAN_199(HttpStatus.UNPROCESSABLE_ENTITY, "본심사가 편향 검증(BIAS_REVIEWING) 상태가 아니어서 편향 우회 승인이 불가합니다."),

    // 4-eye 원칙 위반 (200)
    LOAN_200(HttpStatus.FORBIDDEN, "심사원 본인이 자신의 편향을 우회 승인할 수 없습니다. 다른 상급자가 승인해야 합니다."),

    // Advisory (201)
    LOAN_201(HttpStatus.UNPROCESSABLE_ENTITY, "CRITICAL Advisory 리포트를 먼저 확인(ACK)해야 합니다."),

    // 접근 제어 (202)
    LOAN_202(HttpStatus.FORBIDDEN, "해당 대출 건에 대한 조회 권한이 없습니다."),

    // 이상거래 상신 (203-204)
    LOAN_203(HttpStatus.CONFLICT, "이미 본사에 상신된 건입니다."),
    LOAN_204(HttpStatus.UNPROCESSABLE_ENTITY, "현재 상태에서는 본사 상신이 불가합니다. 심사 진행 중인 건만 상신할 수 있습니다."),

    // break-glass 긴급 접근 (205-206)
    LOAN_205(HttpStatus.BAD_REQUEST, "break-glass 사유는 10자 이상이어야 합니다."),
    LOAN_206(HttpStatus.NOT_FOUND,   "break-glass 대상 건을 찾을 수 없습니다."),

    // RAG (210-219)
    LOAN_210(HttpStatus.SERVICE_UNAVAILABLE,  "임베딩 API 호출에 실패했습니다. 잠시 후 재시도하세요."),
    LOAN_211(HttpStatus.BAD_GATEWAY,          "임베딩 API 응답이 유효하지 않습니다. (차원 불일치 또는 빈 응답)");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}

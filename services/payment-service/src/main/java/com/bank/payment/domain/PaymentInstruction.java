package com.bank.payment.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제지시 (payment_instruction)
 *
 * 결제 처리의 메인 엔티티. IN/OUT 거래 모두 수용.
 * - V1__create_payment_instruction.sql 정합
 * - 컬럼명세서 v12.2 본문 #1~#36 기반
 * - 상태 전이: DRAFT → AUTHORIZED → SCHEDULED → PROCESSING → CLEARING → ... → COMPLETED/FAILED/CANCELED
 * - 비즈니스 메서드는 Stage 5에서 추가 (Stage 4-A는 데이터 구조만)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentInstruction {

    // 결제지시번호
    private String paymentInstructionId;

    // 연결된멱등키값
    private String idempotencyKey;

    // 송신고객번호
    private String senderUserId;

    // 송신계좌번호
    private String senderAccountId;

    // 인증토큰번호
    private String authTokenId;

    // 원거래참조
    private String originalPaymentId;

    // 거래번호
    private String transactionNo;

    // 송신계좌번호_스냅샷
    private String senderAccountNoSnap;

    // 송신계좌별명_스냅샷
    private String senderAccountAliasSnap;

    // 수신은행코드
    private String receiverBankCode;

    // 수신계좌번호
    private String receiverAccountNo;

    // 수신예금주명_스냅샷
    private String receiverHolderNameSnap;

    // 예금주조회시각
    private LocalDateTime holderInquiryAt;

    // 자행이체여부
    private Boolean isIntraBank;

    // 라우팅망종류
    private String routingNetworkType;

    // 이체금액
    private BigDecimal transferAmount;

    // 수수료
    private BigDecimal feeAmount;

    // 수신통장_송신자표시명
    private String receiverPassbookSenderDisplay;

    // 받는분통장메모
    private String receiverMemo;

    // 내통장메모
    private String senderMemo;

    // 진행상태
    private String status;

    // 실패분류
    private String failureCategory;

    // 채널
    private String channel;

    // 요청시각
    private LocalDateTime requestedAt;

    // 완료시각
    private LocalDateTime completedAt;

    // 영업일자
    private String businessDate;

    // 다음재시도시각
    private LocalDateTime nextRetryAt;

    // 다음타임아웃시각
    private LocalDateTime nextTimeoutAt;

    // 낙관적락버전
    private Integer version;

    // 트리거주체
    private String triggerSource;

    // 예약여부
    private Boolean isScheduled;

    // 예약실행시각
    private LocalDateTime scheduledExecutionAt;

    // 최초등록일시
    private LocalDateTime firstRegisteredAt;

    // 최초등록자식별번호
    private String firstRegistrantId;

    // 최종수정일시
    private LocalDateTime lastModifiedAt;

    // 최종수정자식별번호
    private String lastModifierId;
}

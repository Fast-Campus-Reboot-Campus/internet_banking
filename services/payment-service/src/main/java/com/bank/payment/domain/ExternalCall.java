package com.bank.payment.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 외부호출 (external_call)
 *
 * KFTC/BOK/Feign 호출 스냅샷. 재시도/감사 추적용.
 * - V4__create_external_call.sql 정합
 * - payment_instruction FK (NULL 허용, 예금주조회는 결제지시 생성 전)
 * - compensation_target_call_id, parent_call_id self FK 2개
 * - JSONB 4개(requestHeader/requestBody/responseHeader/responseBody) → String
 * - 비즈니스 메서드는 Stage 5에서 추가
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExternalCall {

    // 외부호출번호
    private String callId;

    // 호출멱등키
    private String callIdempotencyKey;

    // 보상유형
    private String compensationType;

    // 보상대상호출번호
    private String compensationTargetCallId;

    // 결제지시번호
    private String paymentInstructionId;

    // 부모호출번호
    private String parentCallId;

    // 세션ID
    private String sessionId;

    // 고객번호
    private String userId;

    // 호출종류
    private String callType;

    // 대상시스템
    private String targetSystem;

    // 엔드포인트URL
    private String endpointUrl;

    // HTTP메서드
    private String httpMethod;

    // 요청ID
    private String requestId;

    // 요청헤더 (JSONB → String. 직렬화/역직렬화는 Service 또는 typeHandler에서 처리 — Stage 5)
    private String requestHeader;

    // 요청본문 (JSONB → String. 직렬화/역직렬화는 Service 또는 typeHandler에서 처리 — Stage 5)
    private String requestBody;

    // 요청본문해시
    private String requestBodyHash;

    // 응답상태코드
    private Integer responseStatusCode;

    // 응답헤더 (JSONB → String. 직렬화/역직렬화는 Service 또는 typeHandler에서 처리 — Stage 5)
    private String responseHeader;

    // 응답본문 (JSONB → String. 직렬화/역직렬화는 Service 또는 typeHandler에서 처리 — Stage 5)
    private String responseBody;

    // 비즈니스응답코드
    private String businessResponseCode;

    // 응답메시지
    private String responseMessage;

    // 결과
    private String result;

    // 시도번호
    private Integer attemptNo;

    // 요청시각
    private LocalDateTime requestedAt;

    // 응답시각
    private LocalDateTime respondedAt;

    // 응답시간_ms
    private Integer responseTimeMs;

    // 타임아웃설정값
    private Integer timeoutMs;

    // 최초등록일시
    private LocalDateTime firstRegisteredAt;

    // 최초등록자식별번호
    private String firstRegistrantId;

    // 최종수정일시
    private LocalDateTime lastModifiedAt;

    // 최종수정자식별번호
    private String lastModifierId;
}

package com.bank.loan.review.dto;

import jakarta.validation.constraints.Size;

/**
 * 본심사 자동 권고(PENDING_APPROVAL) 결과 사람 확정 요청.
 *
 * 권고된 결정(APPROVED/REJECTED) 그대로 마감하고 신청 상태를 전이한다.
 * 결정·한도·금리·기간을 바꾸려면 본 endpoint 대신 PATCH /review (revise) 사용.
 *
 * 확정한 심사관 식별(reviewerId)은 게이트웨이가 검증해 SecurityContext 에 주입한
 * 인증 토큰(X-User-Id)에서만 가져온다. 요청 바디로는 받지 않는다(위조 방지).
 *
 *   confirmRemark   — 확정 메모 (선택)
 */
public record ConfirmReviewRequest(

        @Size(max = 500) String confirmRemark
) {
}

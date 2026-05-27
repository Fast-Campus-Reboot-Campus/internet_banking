package com.bank.common.persistence;

/**
 * 감사 컬럼 created_by/updated_by/deleted_by 에 기록할 사용자 ID 공급자.
 *
 * 기본 구현은 시스템 사용자(0L) 를 반환한다. JWT 인증 도입 후 SecurityContext 에서
 * sub claim 을 추출하는 빈으로 교체하면 자동으로 적용된다.
 */
@FunctionalInterface
public interface CurrentActorProvider {

    Long SYSTEM = 0L;

    Long currentActorId();
}

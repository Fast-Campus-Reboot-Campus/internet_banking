package com.bank.common.persistence;

import org.springframework.data.jpa.domain.Specification;

/**
 * Soft delete 필터 Specification 헬퍼.
 *
 * 사용 예:
 *   repository.findAll(activeOnly().and(byCustomerId(id)));
 *
 * 도메인 repository 에서 메서드 시그니처에 ...AndDeletedAtIsNull 을 명시하는 방식도 함께 허용한다.
 */
public final class SoftDeleteSpecifications {

    private SoftDeleteSpecifications() {}

    /** deleted_at IS NULL 인 활성 행만 반환. */
    public static <T> Specification<T> activeOnly() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /** 명시적으로 삭제된 행만 조회 (관리자/감사용). */
    public static <T> Specification<T> deletedOnly() {
        return (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
    }
}

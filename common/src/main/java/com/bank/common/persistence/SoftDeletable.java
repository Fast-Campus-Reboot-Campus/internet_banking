package com.bank.common.persistence;

/**
 * Soft delete 가능 엔티티 마커. BaseEntity 가 구현한다.
 *
 * 조회 시 deleted_at IS NULL 필터는 호출자가 명시적으로 적용한다.
 * (@SQLDelete / @Where 미사용 — 의도치 않은 전역 필터 회피)
 */
public interface SoftDeletable {

    void softDelete(Long actorId);

    boolean isDeleted();
}

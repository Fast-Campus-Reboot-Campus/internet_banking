package com.bank.common.code;

/**
 * master-service 가 코드 변경 시 Redis 채널 "code:invalidate" 로 발행하는 이벤트.
 *
 *  - codeCd != null  → 해당 단건 무효화
 *  - groupCd 만      → 해당 그룹 전체 무효화
 *  - 둘 다 null      → 전체 캐시 무효화
 */
public record CodeInvalidationEvent(String groupCd, String codeCd) {

    public static CodeInvalidationEvent all() {
        return new CodeInvalidationEvent(null, null);
    }

    public static CodeInvalidationEvent ofGroup(String groupCd) {
        return new CodeInvalidationEvent(groupCd, null);
    }

    public static CodeInvalidationEvent of(String groupCd, String codeCd) {
        return new CodeInvalidationEvent(groupCd, codeCd);
    }
}

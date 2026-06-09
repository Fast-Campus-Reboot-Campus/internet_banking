package com.bank.customer.identity.port;

/**
 * 본인확인기관(NICE/KCB 등) 연동 추상화.
 *
 * <p>이름·주민등록번호·전화번호로 실명확인을 수행하고 연계정보(CI)와 인적사항을 돌려준다.
 * 현재 구현은 외부망 없는 {@link MockIdentityVerificationProvider}(주민번호 기반 결정적 파생)이며,
 * 실 기관 연동 시 이 인터페이스의 다른 구현으로 교체한다.
 */
public interface IdentityVerificationPort {

    /**
     * 실명확인 결과.
     *
     * @param ci                  연계정보(CI) — 사람 단위 고유 식별자(서비스 무관)
     * @param birthDate           생년월일 YYYYMMDD
     * @param genderCode          M / F
     * @param nationalityTypeCode DOMESTIC / FOREIGN
     */
    record VerifiedIdentity(String ci, String birthDate, String genderCode, String nationalityTypeCode) {}

    /** 이름·주민번호·전화번호로 본인확인. 형식 오류 시 BusinessException 을 던진다. */
    VerifiedIdentity resolve(String name, String rrn, String phoneNumber);
}

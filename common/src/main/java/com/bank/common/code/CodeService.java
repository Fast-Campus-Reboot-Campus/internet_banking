package com.bank.common.code;

import java.util.List;
import java.util.Optional;

/**
 * 코드 조회 추상화. 도메인 서비스는 이 인터페이스에만 의존한다.
 *
 * 구현체:
 *  - master-service 측: 자체 DB(CODE_MASTER) 직접 조회
 *  - 다른 서비스 측: master-service HTTP 호출 + Redis 캐시 (추후 추가)
 *
 * 빈이 등록되지 않은 환경(테스트 등) 에서는 ValidCodeValidator 가 검증을 통과시켜
 * 컴포넌트 부재가 곧장 검증 실패로 이어지지 않게 한다.
 */
public interface CodeService {

    Optional<CodeDto> find(String groupCd, String codeCd);

    List<CodeDto> findGroup(String groupCd);

    default boolean exists(String groupCd, String codeCd) {
        return find(groupCd, codeCd).filter(c -> "Y".equalsIgnoreCase(c.activeYn())).isPresent();
    }
}

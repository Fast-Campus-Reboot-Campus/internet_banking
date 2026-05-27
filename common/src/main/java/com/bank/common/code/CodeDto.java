package com.bank.common.code;

/**
 * 다른 서비스가 코드를 조회/캐싱할 때 사용하는 DTO.
 * 실제 엔티티(CodeMaster) 는 master-service 에만 존재한다.
 */
public record CodeDto(
        Long codeId,
        String codeGroupCd,
        String codeCd,
        String codeName,
        String codeDesc,
        Integer sortNo,
        String activeYn
) {
}

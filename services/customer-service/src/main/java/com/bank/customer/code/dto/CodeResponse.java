package com.bank.customer.code.dto;

import com.bank.customer.code.domain.CustCodeMaster;

public record CodeResponse(
        String codeGroupId,
        String codeValue,
        String codeName,
        String description,
        Integer sortOrder
) {
    public static CodeResponse from(CustCodeMaster c) {
        return new CodeResponse(c.getCodeGroupId(), c.getCodeValue(),
                c.getCodeName(), c.getDescription(), c.getSortOrder());
    }
}

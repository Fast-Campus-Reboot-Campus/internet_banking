package com.bank.customer.party.dto;

import com.bank.customer.party.domain.PartyRole;

public record PartyRoleResponse(
        Long   roleId,
        Long   partyId,
        String roleTypeCode,
        String roleStatusCode,
        String roleStartDate,
        String roleEndDate,
        String roleEndReasonCode
) {
    public static PartyRoleResponse from(PartyRole r) {
        return new PartyRoleResponse(
                r.getRoleId(), r.getPartyId(), r.getRoleTypeCode(),
                r.getRoleStatusCode(), r.getRoleStartDate(),
                r.getRoleEndDate(), r.getRoleEndReasonCode());
    }
}

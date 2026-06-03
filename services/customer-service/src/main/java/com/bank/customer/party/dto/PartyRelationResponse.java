package com.bank.customer.party.dto;

import com.bank.customer.party.domain.PartyRelation;

public record PartyRelationResponse(
        Long   relationId,
        Long   fromPartyId,
        Long   toPartyId,
        String relationTypeCode,
        String relationDetailCode,
        Integer equityRatioBps,
        String representationScope,
        String relationStartDate,
        String relationEndDate
) {
    public static PartyRelationResponse from(PartyRelation r) {
        return new PartyRelationResponse(
                r.getRelationId(), r.getFromPartyId(), r.getToPartyId(),
                r.getRelationTypeCode(), r.getRelationDetailCode(),
                r.getEquityRatioBps(), r.getRepresentationScope(),
                r.getRelationStartDate(), r.getRelationEndDate());
    }
}

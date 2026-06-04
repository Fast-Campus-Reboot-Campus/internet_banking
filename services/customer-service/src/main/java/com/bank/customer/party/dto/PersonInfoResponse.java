package com.bank.customer.party.dto;

import com.bank.customer.party.domain.PartyPerson;

public record PersonInfoResponse(
        String  birthDate,
        String  genderCode,
        String  nationalityTypeCode,
        String  nationalityCode,
        String  maritalStatusCode,
        Integer dependentCount,
        String  occupationCode,
        String  occupationName,
        String  workplaceName,
        Long    annualIncomeAmount,
        String  incomeProofCode,
        boolean isPep,
        String  pepTypeCode
) {
    public static PersonInfoResponse from(PartyPerson p) {
        return new PersonInfoResponse(
                p.getBirthDate(), p.getGenderCode(),
                p.getNationalityTypeCode(), p.getNationalityCode(),
                p.getMaritalStatusCode(), p.getDependentCount(),
                p.getOccupationCode(), p.getOccupationName(),
                p.getWorkplaceName(), p.getAnnualIncomeAmount(),
                p.getIncomeProofCode(), p.isPep(), p.getPepTypeCode());
    }
}

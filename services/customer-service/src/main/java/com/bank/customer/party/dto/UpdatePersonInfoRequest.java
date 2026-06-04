package com.bank.customer.party.dto;

public record UpdatePersonInfoRequest(
        String  occupationCode,
        String  occupationName,
        String  workplaceName,
        Long    annualIncomeAmount,
        String  incomeProofCode,
        String  maritalStatusCode
) {}

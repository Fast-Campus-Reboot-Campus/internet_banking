package com.bank.loan.collateral.dto;

import java.util.List;

public record CollateralListResponse(
        List<CollateralResponse> items,
        int totalCount
) {
    public static CollateralListResponse of(List<CollateralResponse> items) {
        return new CollateralListResponse(items, items.size());
    }
}

package com.bank.loan.product.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record LoanProductListResponse(
        List<LoanProductListItem> items,
        long totalCount,
        int page,
        int size
) {
    public static LoanProductListResponse of(Page<LoanProductListItem> p) {
        return new LoanProductListResponse(
                p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}

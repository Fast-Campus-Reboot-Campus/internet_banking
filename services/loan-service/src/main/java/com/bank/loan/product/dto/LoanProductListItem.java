package com.bank.loan.product.dto;

import com.bank.loan.product.domain.LoanProduct;

/**
 * 목록 화면용 슬림 DTO. 단건 조회는 LoanProductResponse 사용.
 */
public record LoanProductListItem(
        Long prodId,
        String prodCd,
        String prodName,
        String loanTypeCd,
        Integer baseRateBps,
        Long minAmount,
        Long maxAmount,
        Integer minPeriodMo,
        Integer maxPeriodMo,
        String prodStatusCd
) {
    public static LoanProductListItem of(LoanProduct p) {
        return new LoanProductListItem(
                p.getProdId(), p.getProdCd(), p.getProdName(),
                p.getLoanTypeCd(), p.getBaseRateBps(),
                p.getMinAmount(), p.getMaxAmount(),
                p.getMinPeriodMo(), p.getMaxPeriodMo(),
                p.getProdStatusCd()
        );
    }
}

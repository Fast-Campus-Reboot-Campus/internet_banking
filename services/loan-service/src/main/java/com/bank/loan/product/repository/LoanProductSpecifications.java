package com.bank.loan.product.repository;

import com.bank.common.persistence.SoftDeleteSpecifications;
import com.bank.loan.product.domain.LoanProduct;
import org.springframework.data.jpa.domain.Specification;

public final class LoanProductSpecifications {

    private LoanProductSpecifications() {}

    public static Specification<LoanProduct> activeOnly() {
        return SoftDeleteSpecifications.activeOnly();
    }

    public static Specification<LoanProduct> hasLoanType(String loanTypeCd) {
        if (loanTypeCd == null || loanTypeCd.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("loanTypeCd"), loanTypeCd);
    }

    public static Specification<LoanProduct> hasStatus(String prodStatusCd) {
        if (prodStatusCd == null || prodStatusCd.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("prodStatusCd"), prodStatusCd);
    }
}

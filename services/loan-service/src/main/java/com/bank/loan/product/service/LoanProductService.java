package com.bank.loan.product.service;

import com.bank.common.web.BusinessException;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.dto.CreateLoanProductRequest;
import com.bank.loan.product.dto.LoanProductResponse;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private static final String DEFAULT_STATUS_CD = "DRAFT";
    private static final String DEFAULT_NO = "N";

    private final LoanProductRepository repository;

    @Transactional(readOnly = true)
    public LoanProductResponse get(Long prodId) {
        LoanProduct product = repository.findByProdIdAndDeletedAtIsNull(prodId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));
        return LoanProductResponse.of(product);
    }

    @Transactional
    public LoanProductResponse create(CreateLoanProductRequest req) {
        validateRanges(req);

        if (repository.existsByProdCdAndDeletedAtIsNull(req.prodCd())) {
            throw new BusinessException(LoanErrorCode.LOAN_001);
        }

        LoanProduct saved = repository.save(LoanProduct.builder()
                .productId(req.productId())
                .prodCd(req.prodCd())
                .prodName(req.prodName())
                .loanTypeCd(req.loanTypeCd())
                .targetCustomerCd(req.targetCustomerCd())
                .repaymentMethodCd(req.repaymentMethodCd())
                .rateTypeCd(req.rateTypeCd())
                .baseRateBps(req.baseRateBps())
                .minRateBps(req.minRateBps())
                .maxRateBps(req.maxRateBps())
                .minAmount(req.minAmount())
                .maxAmount(req.maxAmount())
                .minPeriodMo(req.minPeriodMo())
                .maxPeriodMo(req.maxPeriodMo())
                .collateralRequiredYn(nvl(req.collateralRequiredYn()))
                .guarantorRequiredYn(nvl(req.guarantorRequiredYn()))
                .saleStartDate(req.saleStartDate())
                .saleEndDate(req.saleEndDate())
                .prodStatusCd(DEFAULT_STATUS_CD)
                .prodTermsUrl(req.prodTermsUrl())
                .prodTermsHash(req.prodTermsHash())
                .build());

        return LoanProductResponse.of(saved);
    }

    private void validateRanges(CreateLoanProductRequest req) {
        if (req.minAmount() > req.maxAmount()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minAmount > maxAmount");
        }
        if (req.minPeriodMo() > req.maxPeriodMo()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minPeriodMo > maxPeriodMo");
        }
        if (req.minRateBps() != null && req.maxRateBps() != null && req.minRateBps() > req.maxRateBps()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minRateBps > maxRateBps");
        }
    }

    private String nvl(String yn) {
        return yn == null ? DEFAULT_NO : yn;
    }
}

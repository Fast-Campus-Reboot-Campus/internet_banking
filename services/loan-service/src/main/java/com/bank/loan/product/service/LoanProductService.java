package com.bank.loan.product.service;

import com.bank.common.audit.StatusChangeEvent;
import com.bank.common.audit.StatusHistoryPublisher;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.BusinessException;
import com.bank.loan.product.domain.LoanProduct;
import com.bank.loan.product.dto.CreateLoanProductRequest;
import com.bank.loan.product.dto.DiscontinueLoanProductRequest;
import com.bank.loan.product.dto.LoanProductListItem;
import com.bank.loan.product.dto.LoanProductListResponse;
import com.bank.loan.product.dto.LoanProductResponse;
import com.bank.loan.product.dto.UpdateLoanProductRequest;
import com.bank.loan.product.repository.LoanProductRepository;
import com.bank.loan.product.repository.LoanProductSpecifications;
import com.bank.loan.support.LoanErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private static final String DOMAIN_CD = "LOAN";
    private static final String TARGET_TABLE_CD = "LOAN_PRODUCT";
    private static final String DEFAULT_NO = "N";

    private final LoanProductRepository repository;
    private final StatusHistoryPublisher statusHistoryPublisher;
    private final CurrentActorProvider currentActor;

    @Transactional(readOnly = true)
    public LoanProductListResponse list(String loanTypeCd, String prodStatusCd, Pageable pageable) {
        Specification<LoanProduct> spec = Specification
                .where(LoanProductSpecifications.activeOnly())
                .and(LoanProductSpecifications.hasLoanType(loanTypeCd))
                .and(LoanProductSpecifications.hasStatus(prodStatusCd));
        return LoanProductListResponse.of(repository.findAll(spec, pageable).map(LoanProductListItem::of));
    }

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
                .prodStatusCd(LoanProduct.STATUS_DRAFT)
                .prodTermsUrl(req.prodTermsUrl())
                .prodTermsHash(req.prodTermsHash())
                .build());

        return LoanProductResponse.of(saved);
    }

    @Transactional
    public LoanProductResponse update(Long prodId, UpdateLoanProductRequest req) {
        LoanProduct product = repository.findByProdIdAndDeletedAtIsNull(prodId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));

        product.update(
                req.prodName(),
                req.loanTypeCd(), req.targetCustomerCd(),
                req.repaymentMethodCd(), req.rateTypeCd(),
                req.baseRateBps(), req.minRateBps(), req.maxRateBps(),
                req.minAmount(), req.maxAmount(),
                req.minPeriodMo(), req.maxPeriodMo(),
                req.collateralRequiredYn(), req.guarantorRequiredYn(),
                req.saleStartDate(), req.saleEndDate(),
                req.prodTermsUrl(), req.prodTermsHash(),
                req.prodStatusCd()
        );

        validateRanges(product);
        return LoanProductResponse.of(product);
    }

    @Transactional
    public LoanProductResponse discontinue(Long prodId, DiscontinueLoanProductRequest req) {
        LoanProduct product = repository.findByProdIdAndDeletedAtIsNull(prodId)
                .orElseThrow(() -> new BusinessException(LoanErrorCode.LOAN_002));

        if (product.isDiscontinued()) {
            throw new BusinessException(LoanErrorCode.LOAN_004);
        }

        String before = product.currentStatus();
        product.discontinue(req.saleEndDate());

        statusHistoryPublisher.publish(StatusChangeEvent.of(
                DOMAIN_CD, TARGET_TABLE_CD, product.getProdId(),
                before, LoanProduct.STATUS_DISCONTINUED,
                req.reasonCd(), req.reasonRemark(),
                currentActor.currentActorId()
        ));

        return LoanProductResponse.of(product);
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

    private void validateRanges(LoanProduct p) {
        if (p.getMinAmount() > p.getMaxAmount()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minAmount > maxAmount");
        }
        if (p.getMinPeriodMo() > p.getMaxPeriodMo()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minPeriodMo > maxPeriodMo");
        }
        if (p.getMinRateBps() != null && p.getMaxRateBps() != null && p.getMinRateBps() > p.getMaxRateBps()) {
            throw new BusinessException(LoanErrorCode.LOAN_003, "minRateBps > maxRateBps");
        }
    }

    private String nvl(String yn) {
        return yn == null ? DEFAULT_NO : yn;
    }
}

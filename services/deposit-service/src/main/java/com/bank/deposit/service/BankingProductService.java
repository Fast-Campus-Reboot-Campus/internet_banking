package com.bank.deposit.service;

import com.bank.deposit.domain.entity.BankingProduct;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.exception.ErrorCode;
import com.bank.deposit.repository.BankingProductRepository;
import com.bank.deposit.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankingProductService {

    private final BankingProductRepository bankingProductRepository;
    private final ProductRepository productRepository;

    public BankingProduct findByProductId(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return bankingProductRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "수신상품 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public BankingProduct create(Long productId, String bankingProductType, BigDecimal minJoinAmount,
                                 BigDecimal maxJoinAmount, Integer minContractPeriodMonth,
                                 Integer maxContractPeriodMonth, Boolean autoTransferAvailableYn,
                                 Boolean autoRenewalAvailableYn) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (bankingProductRepository.existsByProductId(productId)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 수신상품 정보가 존재합니다.");
        }
        return bankingProductRepository.save(BankingProduct.builder()
                .productId(productId)
                .bankingProductType(bankingProductType)
                .minJoinAmount(minJoinAmount)
                .maxJoinAmount(maxJoinAmount)
                .minContractPeriodMonth(minContractPeriodMonth)
                .maxContractPeriodMonth(maxContractPeriodMonth)
                .autoTransferAvailableYn(autoTransferAvailableYn != null && autoTransferAvailableYn)
                .autoRenewalAvailableYn(autoRenewalAvailableYn != null && autoRenewalAvailableYn)
                .build());
    }

    @Transactional
    public BankingProduct update(Long productId, String bankingProductType, BigDecimal minJoinAmount,
                                 BigDecimal maxJoinAmount, Integer minContractPeriodMonth,
                                 Integer maxContractPeriodMonth, Boolean autoTransferAvailableYn,
                                 Boolean autoRenewalAvailableYn) {
        BankingProduct bp = findByProductId(productId);
        bp.update(bankingProductType, minJoinAmount, maxJoinAmount, minContractPeriodMonth,
                maxContractPeriodMonth, autoTransferAvailableYn, autoRenewalAvailableYn);
        return bp;
    }

    @Transactional
    public void delete(Long productId) {
        BankingProduct bp = findByProductId(productId);
        bankingProductRepository.delete(bp);
    }
}

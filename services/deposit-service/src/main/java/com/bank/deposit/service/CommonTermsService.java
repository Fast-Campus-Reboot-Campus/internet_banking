package com.bank.deposit.service;

import com.bank.deposit.domain.entity.*;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.exception.ErrorCode;
import com.bank.deposit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonTermsService {

    private final CommonTermsTemplateRepository templateRepository;
    private final CommonTermsVersionRepository versionRepository;
    private final CommonTermsConsentRepository consentRepository;
    private final ProductCommonTermRepository productCommonTermRepository;
    private final ContractCommonTermsConsentRepository contractConsentRepository;
    private final ContractRepository contractRepository;
    private final ProductRepository productRepository;

    // ── 상품 ↔ 공통 약관 ─────────────────────────────────────────────────────

    public List<ProductCommonTerm> findProductCommonTerms(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return productCommonTermRepository.findByProductId(productId);
    }

    @Transactional
    public ProductCommonTerm linkProductCommonTerm(Long productId, Long termsTemplateId, Boolean isRequired) {
        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        templateRepository.findById(termsTemplateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공통 약관 템플릿을 찾을 수 없습니다."));
        if (productCommonTermRepository.existsByProductIdAndTermsTemplateId(productId, termsTemplateId)) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 연결된 공통 약관입니다.");
        }
        return productCommonTermRepository.save(ProductCommonTerm.builder()
                .productId(productId)
                .termsTemplateId(termsTemplateId)
                .isRequired(isRequired != null && isRequired)
                .build());
    }

    @Transactional
    public void unlinkProductCommonTerm(Long productId, Long termsTemplateId) {
        productCommonTermRepository.deleteByProductIdAndTermsTemplateId(productId, termsTemplateId);
    }

    // ── 계약 ↔ 공통 약관 동의 ────────────────────────────────────────────────

    public List<ContractCommonTermsConsent> findContractCommonConsents(Long contractId) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));
        return contractConsentRepository.findByContractId(contractId);
    }

    @Transactional
    public ContractCommonTermsConsent saveContractCommonConsent(Long contractId, Long termsTemplateId,
                                                                Long termsVersionId, Long customerId,
                                                                String agreedYn, String agreedAt,
                                                                String consentMethodCd, String consentTool,
                                                                String signedDocUrl, String signedDocHash,
                                                                String clientIp, String retentionUntil) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));
        templateRepository.findById(termsTemplateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공통 약관 템플릿을 찾을 수 없습니다."));
        versionRepository.findById(termsVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공통 약관 버전을 찾을 수 없습니다."));

        String statusCd = "Y".equals(agreedYn) ? "AGREE" : "REFUSE";

        CommonTermsConsent consent = consentRepository.save(CommonTermsConsent.builder()
                .termsTemplateId(termsTemplateId)
                .termsVersionId(termsVersionId)
                .customerId(customerId)
                .bizDivCd("DEP")
                .consentTargetId(contractId)
                .consentStatusCd(statusCd)
                .agreedYn(agreedYn)
                .agreedAt(agreedAt)
                .consentMethodCd(consentMethodCd)
                .consentTool(consentTool)
                .signedDocUrl(signedDocUrl)
                .signedDocHash(signedDocHash)
                .clientIp(clientIp)
                .retentionUntil(retentionUntil)
                .build());

        return contractConsentRepository.save(ContractCommonTermsConsent.builder()
                .contractId(contractId)
                .consentId(consent.getConsentId())
                .build());
    }
}

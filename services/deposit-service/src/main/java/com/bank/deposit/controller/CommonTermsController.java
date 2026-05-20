package com.bank.deposit.controller;

import com.bank.deposit.domain.entity.ContractCommonTermsConsent;
import com.bank.deposit.domain.entity.ProductCommonTerm;
import com.bank.deposit.dto.request.CommonTermsConsentRequest;
import com.bank.deposit.dto.request.ProductCommonTermRequest;
import com.bank.deposit.service.CommonTermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommonTermsController {

    private final CommonTermsService commonTermsService;

    // ── 상품 ↔ 공통 약관 ─────────────────────────────────────────────────────

    @GetMapping("/products/{productId}/common-terms")
    public List<ProductCommonTerm> getProductCommonTerms(@PathVariable Long productId) {
        return commonTermsService.findProductCommonTerms(productId);
    }

    @PostMapping("/products/{productId}/common-terms")
    public ResponseEntity<ProductCommonTerm> linkProductCommonTerm(
            @PathVariable Long productId,
            @Valid @RequestBody ProductCommonTermRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commonTermsService.linkProductCommonTerm(productId, req.termsTemplateId(), req.isRequired()));
    }

    @DeleteMapping("/products/{productId}/common-terms/{termsTemplateId}")
    public ResponseEntity<Void> unlinkProductCommonTerm(
            @PathVariable Long productId,
            @PathVariable Long termsTemplateId) {
        commonTermsService.unlinkProductCommonTerm(productId, termsTemplateId);
        return ResponseEntity.noContent().build();
    }

    // ── 계약 ↔ 공통 약관 동의 ────────────────────────────────────────────────

    @GetMapping("/contracts/{contractId}/common-terms")
    public List<ContractCommonTermsConsent> getContractCommonConsents(@PathVariable Long contractId) {
        return commonTermsService.findContractCommonConsents(contractId);
    }

    @PostMapping("/contracts/{contractId}/common-terms")
    public ResponseEntity<ContractCommonTermsConsent> saveContractCommonConsent(
            @PathVariable Long contractId,
            @Valid @RequestBody CommonTermsConsentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commonTermsService.saveContractCommonConsent(
                        contractId, req.termsTemplateId(), req.termsVersionId(),
                        req.customerId(), req.agreedYn(), req.agreedAt(),
                        req.consentMethodCd(), req.consentTool(),
                        req.signedDocUrl(), req.signedDocHash(),
                        req.clientIp(), req.retentionUntil()));
    }
}

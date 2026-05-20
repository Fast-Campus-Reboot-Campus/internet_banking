package com.bank.deposit.controller;

import com.bank.deposit.domain.entity.BankingProduct;
import com.bank.deposit.dto.request.BankingProductRequest;
import com.bank.deposit.service.BankingProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BankingProductController {

    private final BankingProductService bankingProductService;

    @PostMapping("/products/{productId}/banking")
    public ResponseEntity<BankingProduct> create(@PathVariable Long productId,
                                                  @RequestBody BankingProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankingProductService.create(productId, req.bankingProductType(),
                        req.minJoinAmount(), req.maxJoinAmount(),
                        req.minContractPeriodMonth(), req.maxContractPeriodMonth(),
                        req.autoTransferAvailableYn(), req.autoRenewalAvailableYn()));
    }

    @GetMapping("/products/{productId}/banking")
    public BankingProduct get(@PathVariable Long productId) {
        return bankingProductService.findByProductId(productId);
    }

    @PutMapping("/products/{productId}/banking")
    public BankingProduct update(@PathVariable Long productId,
                                  @RequestBody BankingProductRequest req) {
        return bankingProductService.update(productId, req.bankingProductType(),
                req.minJoinAmount(), req.maxJoinAmount(),
                req.minContractPeriodMonth(), req.maxContractPeriodMonth(),
                req.autoTransferAvailableYn(), req.autoRenewalAvailableYn());
    }

    @DeleteMapping("/products/{productId}/banking")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        bankingProductService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}

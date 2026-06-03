package com.bank.customer.party;

import com.bank.common.web.ApiResponse;
import com.bank.customer.party.dto.AddTaxResidencyRequest;
import com.bank.customer.party.dto.ForeignerInfoResponse;
import com.bank.customer.party.dto.PersonInfoResponse;
import com.bank.customer.party.dto.TaxResidencyResponse;
import com.bank.customer.party.dto.UpdatePassportRequest;
import com.bank.customer.party.dto.UpdatePersonInfoRequest;
import com.bank.customer.party.dto.UpdateStayRequest;
import com.bank.customer.party.service.PersonInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
public class PersonInfoController {

    private final PersonInfoService personInfoService;

    // ── 개인정보 (party_person) ───────────────────────────────────────────────

    @GetMapping("/person-info")
    public ResponseEntity<ApiResponse<PersonInfoResponse>> getPersonInfo(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(personInfoService.getPersonInfo(customerId)));
    }

    @PutMapping("/person-info")
    public ResponseEntity<ApiResponse<PersonInfoResponse>> updatePersonInfo(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody UpdatePersonInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(personInfoService.updatePersonInfo(customerId, request)));
    }

    // ── 외국인정보 (foreigner_info) ───────────────────────────────────────────

    @GetMapping("/foreigner-info")
    public ResponseEntity<ApiResponse<ForeignerInfoResponse>> getForeignerInfo(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(personInfoService.getForeignerInfo(customerId)));
    }

    @PutMapping("/foreigner-info/passport")
    public ResponseEntity<ApiResponse<ForeignerInfoResponse>> updatePassport(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody UpdatePassportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personInfoService.updatePassport(customerId,
                        request.passportNo(), request.countryCode(), request.expiryDate())));
    }

    @PutMapping("/foreigner-info/stay")
    public ResponseEntity<ApiResponse<ForeignerInfoResponse>> updateStay(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody UpdateStayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personInfoService.updateStay(customerId,
                        request.stayQualificationCode(), request.stayExpiryDate())));
    }

    // ── 납세거주정보 (tax_residency_info) ─────────────────────────────────────

    @GetMapping("/tax-residencies")
    public ResponseEntity<ApiResponse<List<TaxResidencyResponse>>> getTaxResidencies(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(personInfoService.getTaxResidencies(customerId)));
    }

    @PostMapping("/tax-residencies")
    public ResponseEntity<ApiResponse<TaxResidencyResponse>> addTaxResidency(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody AddTaxResidencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(personInfoService.addTaxResidency(customerId, request)));
    }

    @DeleteMapping("/tax-residencies/{taxResidencyId}")
    public ResponseEntity<ApiResponse<Void>> deleteTaxResidency(
            @RequestHeader("X-Customer-Id") Long customerId,
            @PathVariable Long taxResidencyId) {
        personInfoService.deleteTaxResidency(customerId, taxResidencyId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

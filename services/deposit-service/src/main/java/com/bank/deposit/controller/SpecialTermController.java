package com.bank.deposit.controller;

import com.bank.deposit.domain.entity.SpecialTerm;
import com.bank.deposit.domain.entity.SpecialTermHistory;
import com.bank.deposit.domain.enums.SpecialTermStatus;
import com.bank.deposit.dto.request.SpecialTermCreateRequest;
import com.bank.deposit.dto.request.SpecialTermUpdateRequest;
import com.bank.deposit.service.SpecialTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/special-terms")
@RequiredArgsConstructor
public class SpecialTermController {

    private final SpecialTermService specialTermService;

    @GetMapping
    public List<SpecialTerm> list(@RequestParam(required = false) Boolean isActive) {
        return specialTermService.findAll(isActive);
    }

    @PostMapping
    public ResponseEntity<SpecialTerm> create(@Valid @RequestBody SpecialTermCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(specialTermService.create(req.specialTermName(), req.specialTermContent(),
                        req.specialTermSummary(), req.isRequired(), req.specialTermVersion(),
                        req.startedAt(), req.endedAt()));
    }

    @GetMapping("/{specialTermId}")
    public SpecialTerm get(@PathVariable Long specialTermId) {
        return specialTermService.findById(specialTermId);
    }

    @PutMapping("/{specialTermId}")
    public SpecialTerm update(@PathVariable Long specialTermId, @Valid @RequestBody SpecialTermUpdateRequest req) {
        return specialTermService.update(specialTermId, req.specialTermName(), req.specialTermContent(),
                req.specialTermVersion(), req.changeReason());
    }

    @PatchMapping("/{specialTermId}/status")
    public SpecialTerm changeStatus(@PathVariable Long specialTermId,
                                    @RequestBody java.util.Map<String, String> body) {
        SpecialTermStatus status = SpecialTermStatus.valueOf(body.get("status"));
        return specialTermService.changeStatus(specialTermId, status);
    }

    @GetMapping("/{specialTermId}/histories")
    public List<SpecialTermHistory> getHistory(@PathVariable Long specialTermId) {
        return specialTermService.findHistory(specialTermId);
    }
}

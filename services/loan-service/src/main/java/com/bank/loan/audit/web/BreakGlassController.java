package com.bank.loan.audit.web;

import com.bank.loan.audit.service.BreakGlassService;
import com.bank.loan.audit.web.dto.BreakGlassRequest;
import com.bank.loan.audit.web.dto.BreakGlassResponse;
import com.bank.loan.security.LoanActorContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/break-glass")
@RequiredArgsConstructor
public class BreakGlassController {

    private final BreakGlassService service;

    @PostMapping
    public ResponseEntity<BreakGlassResponse> breakGlass(
            @Valid @RequestBody BreakGlassRequest request,
            Authentication authentication) {
        LoanActorContext actor = LoanActorContext.from(authentication);
        return ResponseEntity.ok(service.execute(request, actor));
    }
}

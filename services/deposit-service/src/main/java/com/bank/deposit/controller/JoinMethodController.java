package com.bank.deposit.controller;

import com.bank.deposit.domain.entity.JoinMethod;
import com.bank.deposit.dto.request.JoinMethodCreateRequest;
import com.bank.deposit.service.JoinMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/join-methods")
@RequiredArgsConstructor
public class JoinMethodController {

    private final JoinMethodService joinMethodService;

    @GetMapping
    public List<JoinMethod> list() {
        return joinMethodService.findAll();
    }

    @PostMapping
    public ResponseEntity<JoinMethod> create(@RequestBody JoinMethodCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(joinMethodService.create(req.joinMethodName(), req.description()));
    }
}

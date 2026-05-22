package com.bank.deposit.controller;

import com.bank.deposit.dto.response.ProductRecommendResponse;
import com.bank.deposit.service.RecommendAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendAgentController {

    private final RecommendAgentService recommendAgentService;

    @GetMapping("/products/recommend-agent")
    public ProductRecommendResponse recommend(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "3") int periodMonth) {
        return recommendAgentService.recommend(customerId, periodMonth);
    }
}

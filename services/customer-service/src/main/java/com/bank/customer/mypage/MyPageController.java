package com.bank.customer.mypage;

import com.bank.common.web.ApiResponse;
import com.bank.customer.mypage.dto.MyPageResponse;
import com.bank.customer.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @RequestHeader("X-Customer-Id") Long customerId) {
        MyPageResponse response = myPageService.getMyPage(customerId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

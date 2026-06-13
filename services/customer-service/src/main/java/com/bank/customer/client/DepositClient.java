package com.bank.customer.client;

import com.bank.customer.client.dto.VerifyOwnerRequest;
import com.bank.customer.client.dto.VerifyOwnerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * deposit-service(수신) 호출 클라이언트. 비로그인 ID 조회/사용자암호 재설정의 본인확인에서
 * 계좌번호 + 계좌비밀번호로 소유 고객을 검증한다. deposit 의 계좌 자격 검증만 위임하고,
 * 이름·loginId·credential 매칭은 customer-service 가 직접 수행한다(도메인 책임 분리).
 */
@Slf4j
@Component
public class DepositClient {

    private final RestClient restClient;

    public DepositClient(@Value("${client.deposit-service.url:http://deposit-service:8082}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    /** 계좌번호 + 계좌비밀번호가 일치하면 소유 고객 ID. 불일치·계좌없음·호출오류면 빈 값. */
    public Optional<Long> verifyAccountOwner(String accountNumber, String accountPassword) {
        try {
            VerifyOwnerResponse res = restClient.post()
                    .uri("/api/internal/accounts/verify-owner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VerifyOwnerRequest(accountNumber, accountPassword))
                    .retrieve()
                    .body(VerifyOwnerResponse.class);
            if (res != null && res.matched() && res.customerId() != null) {
                return Optional.of(Long.parseLong(res.customerId()));
            }
        } catch (Exception e) {
            log.warn("deposit verify-owner 호출 실패: {}", e.toString());
        }
        return Optional.empty();
    }
}

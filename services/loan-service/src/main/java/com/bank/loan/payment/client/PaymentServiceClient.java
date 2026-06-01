package com.bank.loan.payment.client;

import com.bank.loan.payment.client.dto.PaymentRequest;
import com.bank.loan.payment.client.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentServiceClient {

    private static final String PAYMENTS_PATH = "/api/v1/payments";

    private final RestClient restClient;

    public PaymentServiceClient(RestClient.Builder builder, PaymentServiceProperties props) {
        this.restClient = builder.baseUrl(props.url()).build();
    }

    public PaymentResponse pay(String idempotencyKey, PaymentRequest req) {
        log.debug("payment-service 출금 요청 idemKey={} amount={}", idempotencyKey, req.transferAmount());
        PaymentResponse resp = restClient.post()
                .uri(PAYMENTS_PATH)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-User-Id", "SYSTEM")
                .header("X-Auth-Token-Id", "SYSTEM")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(PaymentResponse.class);
        log.info("payment-service 응답 idemKey={} status={} piId={}",
                idempotencyKey, resp != null ? resp.status() : null,
                resp != null ? resp.paymentInstructionId() : null);
        return resp;
    }
}

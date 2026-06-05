package com.bank.loan.payment.client;

import com.bank.loan.payment.client.dto.PaymentRequest;
import com.bank.loan.payment.client.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
                .header("X-Auth-Token-Id", deriveAuthTokenId(idempotencyKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(PaymentResponse.class);
        log.info("payment-service 응답 idemKey={} status={} piId={}",
                idempotencyKey, resp != null ? resp.status() : null,
                resp != null ? resp.paymentInstructionId() : null);
        return resp;
    }

    /**
     * X-Auth-Token-Id 파생값을 생성한다.
     *
     * payment 의 auth_token_id 컬럼은 VARCHAR(20) UNIQUE 라 호출마다 유일한 값이어야 한다
     * (고정값을 보내면 두 번째 호출부터 UNIQUE 위반으로 실패). 멱등키는 흐름별로 고유하므로
     * (EXEC-/AUTO-/ONL-/REV-) 이를 SHA-256 해시해 16진수 앞 20자로 파생한다. 멱등키 길이가
     * 20자를 넘거나 접두부가 겹칠 수 있어 단순 절단은 충돌하므로 해시를 쓴다. 같은 멱등키는
     * 항상 같은 값으로 파생되어 재시도 시 멱등성도 유지된다.
     */
    static String deriveAuthTokenId(String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}

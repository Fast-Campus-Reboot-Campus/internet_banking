package com.bank.loan.payment.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PaymentServiceClient#deriveAuthTokenId(String)} 검증.
 *
 * payment auth_token_id 컬럼은 VARCHAR(20) UNIQUE 라 호출마다 유일·결정적·20자 이내여야 한다.
 */
class PaymentServiceClientTest {

    @Test
    void 파생값은_20자_이내다() {
        String[] idemKeys = {
                "EXEC-100-1",
                "AUTO-100-200-20260601",
                "ONL-100-200-caller-idem-key-very-long-suffix",
                "REV-100-9000",
        };
        for (String idemKey : idemKeys) {
            assertThat(PaymentServiceClient.deriveAuthTokenId(idemKey)).hasSizeLessThanOrEqualTo(20);
        }
    }

    @Test
    void 서로_다른_멱등키는_서로_다른_값으로_파생된다() {
        String exec = PaymentServiceClient.deriveAuthTokenId("EXEC-100-1");
        String auto = PaymentServiceClient.deriveAuthTokenId("AUTO-100-200-20260601");
        String onl = PaymentServiceClient.deriveAuthTokenId("ONL-100-200-callerKey");
        String rev = PaymentServiceClient.deriveAuthTokenId("REV-100-9000");

        assertThat(exec).isNotEqualTo(auto).isNotEqualTo(onl).isNotEqualTo(rev);
        assertThat(auto).isNotEqualTo(onl).isNotEqualTo(rev);
        assertThat(onl).isNotEqualTo(rev);
    }

    @Test
    void 접두부가_겹치고_20자를_넘는_멱등키도_충돌하지_않는다() {
        // 단순 20자 절단이라면 "AUTO-100-200-2026060" 으로 같아져 충돌하는 케이스
        String d1 = PaymentServiceClient.deriveAuthTokenId("AUTO-100-200-20260601");
        String d2 = PaymentServiceClient.deriveAuthTokenId("AUTO-100-200-20260609");

        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    void 같은_멱등키는_항상_같은_값으로_파생된다() {
        assertThat(PaymentServiceClient.deriveAuthTokenId("AUTO-100-200-20260601"))
                .isEqualTo(PaymentServiceClient.deriveAuthTokenId("AUTO-100-200-20260601"));
    }
}

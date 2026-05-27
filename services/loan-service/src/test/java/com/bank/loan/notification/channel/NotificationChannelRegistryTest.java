package com.bank.loan.notification.channel;

import com.bank.loan.notification.outbox.NotificationOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * plan 03 step 2: 알림 채널 어댑터 + 레지스트리 분기 단위 검증.
 */
class NotificationChannelRegistryTest {

    private StubSmsAdapter sms;
    private StubKakaoAdapter kakao;
    private StubEmailAdapter email;
    private StubAppPushAdapter push;
    private NotificationChannelRegistry registry;

    @BeforeEach
    void setUp() {
        sms = new StubSmsAdapter();
        kakao = new StubKakaoAdapter();
        email = new StubEmailAdapter();
        push = new StubAppPushAdapter();
        registry = new NotificationChannelRegistry(List.of(sms, kakao, email, push));
    }

    @Test
    void 채널_4종_매핑된다() {
        assertThat(registry.resolve("SMS")).isSameAs(sms);
        assertThat(registry.resolve("KAKAO_ALIMTALK")).isSameAs(kakao);
        assertThat(registry.resolve("EMAIL")).isSameAs(email);
        assertThat(registry.resolve("APP_PUSH")).isSameAs(push);
    }

    @Test
    void 미등록_channel_은_IllegalStateException() {
        assertThatThrownBy(() -> registry.resolve("UNKNOWN_CHANNEL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN_CHANNEL");
    }

    @Test
    void stub_어댑터들_각자_채널별_prefix_를_반환() {
        NotificationOutbox dummy = NotificationOutbox.builder()
                .eventTypeCd("APPLICATION_SUBMITTED")
                .referenceId(7L)
                .channelCd("SMS")
                .status(NotificationOutbox.STATUS_PENDING)
                .attemptNo(0).maxAttempt(3)
                .nextAttemptAt(OffsetDateTime.now())
                .idempotencyKey("APPLICATION_SUBMITTED:7:SMS")
                .build();

        assertThat(sms.send(dummy).providerMsgId()).startsWith("SMS-");
        assertThat(kakao.send(dummy).providerMsgId()).startsWith("KKO-");
        assertThat(email.send(dummy).providerMsgId()).startsWith("EML-");
        assertThat(push.send(dummy).providerMsgId()).startsWith("PSH-");

        assertThat(sms.send(dummy).success()).isTrue();
        assertThat(sms.send(dummy).responseCode()).isEqualTo("0000");
    }
}

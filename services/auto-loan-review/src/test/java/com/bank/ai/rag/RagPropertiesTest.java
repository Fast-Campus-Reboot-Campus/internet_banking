package com.bank.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagPropertiesTest {

    @Test
    void 기본값_enabled_false() {
        var props = new RagProperties(false, Map.of());
        assertThat(props.enabled()).isFalse();
    }

    @Test
    void callCapsPerTrack_트랙별_cap_반환() {
        var caps = Map.of("TRACK_1", 1, "TRACK_2", 2, "TRACK_3", 5);
        var props = new RagProperties(true, caps);

        assertThat(props.capForTrack("TRACK_1")).isEqualTo(1);
        assertThat(props.capForTrack("TRACK_2")).isEqualTo(2);
        assertThat(props.capForTrack("TRACK_3")).isEqualTo(5);
    }

    @Test
    void callCapsPerTrack_미설정_트랙은_기본값_1() {
        var props = new RagProperties(true, Map.of());
        assertThat(props.capForTrack("TRACK_3")).isEqualTo(1);
    }

    @Test
    void null_callCapsPerTrack_빈_map으로_초기화() {
        var props = new RagProperties(false, null);
        assertThat(props.callCapsPerTrack()).isEmpty();
        assertThat(props.capForTrack("TRACK_1")).isEqualTo(1);
    }
}

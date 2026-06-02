package com.bank.gateway.filter;

import com.bank.common.security.jwt.JwtClaims;
import com.bank.common.security.jwt.JwtProvider;
import com.bank.common.security.jwt.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider);
    }

    @Test
    @DisplayName("branch claim 없는 토큰 — 인바운드 X-User-Branch 위조 헤더가 빈 값으로 덮어씌워진다")
    void forgedBranchHeader_isOverwritten_whenTokenHasNoBranchClaim() {
        JwtClaims claims = new JwtClaims(1L, "emp@bank.com",
                List.of("ROLE_BRANCH_MANAGER"), TokenType.ACCESS, null, null);
        when(jwtProvider.parseClaims(anyString())).thenReturn(claims);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/loans")
                        .header("Authorization", "Bearer token123")
                        .header("X-User-Branch", "FORGED-BRANCH-001")
                        .header("X-User-Grade",  "FORGED-GRADE")
                        .build());

        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            captured.set(ex.getRequest());
            return Mono.empty();
        }).block();

        assertThat(captured.get().getHeaders().getFirst("X-User-Branch")).isEqualTo("");
        assertThat(captured.get().getHeaders().getFirst("X-User-Grade")).isEqualTo("");
        assertThat(captured.get().getHeaders().getFirst("X-User-Id")).isEqualTo("1");
    }

    @Test
    @DisplayName("branch claim 있는 토큰 — JWT claim 값이 전달된다 (인바운드 위조 무시)")
    void branchFromJwt_isForwarded_regardless_ofForgedHeader() {
        JwtClaims claims = new JwtClaims(2L, "mgr@bank.com",
                List.of("ROLE_BRANCH_MANAGER"), TokenType.ACCESS, "BR-001", "M2");
        when(jwtProvider.parseClaims(anyString())).thenReturn(claims);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/loans")
                        .header("Authorization", "Bearer token123")
                        .header("X-User-Branch", "FORGED-BRANCH-999")
                        .build());

        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            captured.set(ex.getRequest());
            return Mono.empty();
        }).block();

        assertThat(captured.get().getHeaders().getFirst("X-User-Branch")).isEqualTo("BR-001");
        assertThat(captured.get().getHeaders().getFirst("X-User-Grade")).isEqualTo("M2");
    }

    @Test
    @DisplayName("X-Customer-Id 위조 헤더도 JWT claim 으로 덮어씌워진다")
    void forgedCustomerIdHeader_isOverwritten() {
        JwtClaims claims = new JwtClaims(3L, "user@bank.com",
                List.of("ROLE_CUSTOMER"), TokenType.ACCESS, null, null);
        when(jwtProvider.parseClaims(anyString())).thenReturn(claims);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/loans")
                        .header("Authorization", "Bearer token123")
                        .header("X-Customer-Id", "99999")
                        .header("X-User-Id",     "99999")
                        .build());

        AtomicReference<ServerHttpRequest> captured = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            captured.set(ex.getRequest());
            return Mono.empty();
        }).block();

        assertThat(captured.get().getHeaders().get("X-Customer-Id")).containsExactly("3");
        assertThat(captured.get().getHeaders().get("X-User-Id")).containsExactly("3");
    }

    @Test
    @DisplayName("공개 경로는 인바운드 헤더를 그대로 통과시킨다 (JWT 검증 없음)")
    void publicPath_passesThrough_withoutModification() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isTrue();
    }
}

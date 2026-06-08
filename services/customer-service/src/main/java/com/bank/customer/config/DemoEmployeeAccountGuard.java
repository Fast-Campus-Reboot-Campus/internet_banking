package com.bank.customer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 운영(prod) 환경에서 데모 직원 계정을 로그인 불가로 비활성화한다.
 *
 * <p>V11/V12 가 시드한 관리자 데모 직원 계정(login_id audit01…ops01, password Employee1234!)은
 * Flyway 마이그레이션이라 프로파일 게이팅이 불가해 staging/prod 에도 그대로 생성된다
 * (login 가능한 공개 자격증명). 인증서 PIN(V10)을 시드 시리얼로 한정해 운영 no-op 으로 둔 것과
 * 동일한 수준의 환경 격리를 직원 계정에도 적용하기 위해, prod 기동 시 알려진 데모 login_id 의
 * 자격증명을 {@code CLOSED} 로 전환한다(LoginService 가 {@code isActive()} 검사로 차단 → CUST_012).
 *
 * <p>대상은 알려진 데모 login_id 로 한정(V10 의 시드 시리얼 한정과 동형)하고, local/test 에서는
 * {@code @Profile("prod")} 로 동작하지 않아 데모·테스트 로그인은 그대로 유지된다.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class DemoEmployeeAccountGuard implements ApplicationRunner {

    /** V11/V12 가 시드한 데모 직원 login_id (관리자 콘솔 7역할 + 심사역·운영). */
    private static final List<String> DEMO_LOGIN_IDS = List.of(
            "audit01", "review01", "risk01", "mkt01", "owner01",
            "staff01", "other01", "deputy01", "ops01");

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String placeholders = DEMO_LOGIN_IDS.stream().map(id -> "?").collect(Collectors.joining(","));
        int closed = jdbcTemplate.update(
                "UPDATE credential SET account_status_code = 'CLOSED', updated_at = NOW() " +
                        "WHERE login_id IN (" + placeholders + ") AND account_status_code <> 'CLOSED'",
                DEMO_LOGIN_IDS.toArray());

        if (closed > 0) {
            log.warn("[DemoEmployeeAccountGuard] prod 환경 — 데모 직원 계정 {}건을 CLOSED 로 비활성화했습니다. " +
                    "운영 직원 계정은 별도 발급 절차로 생성하세요.", closed);
        }
    }
}

-- =============================================================================
-- V8: 기본 FDS 탐지 룰 시드 데이터
-- 3종 룰: 로그인 실패 연속(BLOCK) / 인증서 실패 연속(BLOCK) / 비밀번호 잦은 변경(MONITOR)
-- =============================================================================

INSERT INTO fds_rule (
    fds_rule_code,
    fds_rule_name,
    fds_rule_category_code,
    fds_rule_target_event_code,
    fds_rule_condition_json,
    fds_rule_risk_weight,
    fds_rule_action_type_code,
    fds_rule_active_yn,
    fds_rule_effective_date,
    created_by,
    updated_by
) VALUES
-- 30분 내 로그인 실패 10회 → 차단
(
    'LOGIN_FAIL_BLOCK_10',
    '로그인 실패 10회 차단',
    'LOGIN_FAILURE_COUNT',
    'LOGIN_ATTEMPT',
    '{"window_minutes": 30, "threshold": 10}',
    90,
    'BLOCK',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
),
-- 10분 내 인증서 로그인 실패 5회 → 차단
(
    'CERT_FAIL_BLOCK_5',
    '인증서 로그인 실패 5회 차단',
    'CERT_FAILURE_COUNT',
    'CERT_LOGIN',
    '{"window_minutes": 10, "threshold": 5}',
    85,
    'BLOCK',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
),
-- 1일 내 비밀번호 변경 3회 이상 → 모니터링
(
    'PWD_CHANGE_MONITOR_3',
    '비밀번호 잦은 변경 모니터링',
    'PASSWORD_CHANGE_FREQ',
    'PASSWORD_CHANGE',
    '{"window_days": 1, "threshold": 3}',
    50,
    'MONITOR',
    'T',
    TO_CHAR(CURRENT_DATE, 'YYYYMMDD'),
    0,
    0
);

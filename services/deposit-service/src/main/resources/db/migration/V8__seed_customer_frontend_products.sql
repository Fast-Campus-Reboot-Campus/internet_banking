-- customer 브랜치 web/app/(personal)/products/deposit/list/page.tsx + [id]/page.tsx 기준
-- 예금(TERM) 4개 / 입출금자유(DEMAND) 10개 / 적금 5개 / 청약 2개
-- 상품명·금리·기간 모두 화면 표시값과 동일하게 삽입

-- ── 대상그룹 ────────────────────────────────────────────────────────────────────
INSERT INTO deposit_target_groups (target_group_id, target_group_name, description, is_active)
VALUES
    (1, '개인고객', '개인 인터넷뱅킹 고객',  TRUE),
    (3, '청년고객', '만 19~34세 청년 고객', TRUE),
    (4, '국군장병', '현역 군인',             TRUE)
ON CONFLICT (target_group_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 1. 예금 TERM (정기예금) 4개  banking_product_id = 6 ~ 9
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
) VALUES
    -- [id]/page.tsx PRODUCTS record 기준
    ( 6, 'DEPOSIT', 'AXful 정기예금',          'Digital AXful의 대표 정기예금',           1, 2.15, 1000000.00, NULL,  1, 36, TRUE,  TRUE, TRUE,  '20260101', 'SELLING'),
    ( 7, 'DEPOSIT', 'AXful 수퍼정기예금(개인)', '가입 조건을 직접 설계하는',               1, 2.10, 1000000.00, NULL,  1, 36, FALSE, TRUE, FALSE, '20260101', 'SELLING'),
    ( 8, 'DEPOSIT', '일반정기예금',             '목돈 모아 안정수익 마음든든',              1, 2.10, 1000000.00, NULL,  1, 36, TRUE,  TRUE, FALSE, '20260101', 'SELLING'),
    ( 9, 'DEPOSIT', 'AXful 청년도약계좌',       '청년의 자산형성을 응원합니다',             1, 3.50,    1000.00, NULL, 60, 60, FALSE, TRUE, FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 2. 입출금자유 DEMAND 10개  banking_product_id = 17 ~ 26
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
) VALUES
    (17, 'DEPOSIT', 'AXful 쏙머니통장',           '쇼핑용 아껴 쏙머니가 쏙~',                                               1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (18, 'DEPOSIT', '당선통장',                    '각종 공직선거 입후보자 및 입후보예정자 선거자금 관리 통장',               1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (19, 'DEPOSIT', 'AXful 생계비계좌',            '생계 유지에 필요한 자금을 최대 250만원까지 보호하는 압류방지 전용통장',  1, 0.10, 0.00, 2500000.00, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (20, 'DEPOSIT', 'AXful GS Pay통장',            'GS25와의 만남으로 더 풍성해진 혜택',                                    1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (21, 'DEPOSIT', '모니모 AXful 매일이자 통장',  '하루만 넣어도 이자가 쌓이는',                                           1, 0.50, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (22, 'DEPOSIT', 'AXful 모임금고',              '고인 여유자금을 연 2.0%(최대 1천만원)로 불리는',                         1, 2.00, 0.00, 10000000.00, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (23, 'DEPOSIT', 'AXful 스타통장',              'Digital AXful의 대표 통장',                                             1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (24, 'DEPOSIT', 'AXful 지갑통장',              '일상의 모든 지출을 한 곳에서 관리',                                      1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (25, 'DEPOSIT', 'AXful 자유입출금통장',        '언제든 자유롭게 입출금 가능한 기본 통장',                                1, 0.10, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (26, 'DEPOSIT', 'AXful 청년우대통장',          '만 19~34세 청년을 위한 우대금리 제공',                                   1, 0.50, 0.00, NULL, NULL, NULL, FALSE, FALSE, FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 3. 적금 SAVINGS 5개  banking_product_id = 10 ~ 14
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
) VALUES
    (10, 'SAVINGS', 'AXful 내맘대로적금',       '누구나 쉽게 자유롭게 DIY',               1, 2.95, 10000.00, 50000000.00,  1, 36, TRUE,  TRUE,  FALSE, '20260101', 'SELLING'),
    (11, 'SAVINGS', 'AXful 달러자적금',          '달러 가치상승 응원하는 두배이율',          1, 1.00, 10000.00, 10000000.00,  1,  6, TRUE,  FALSE, FALSE, '20260101', 'SELLING'),
    (12, 'SAVINGS', 'AXful 맑은하늘적금',        '맑은하늘 인증코드 금리도 Up',             1, 2.85, 10000.00, 50000000.00,  1, 36, TRUE,  TRUE,  FALSE, '20260101', 'SELLING'),
    (13, 'SAVINGS', 'AXful 장병내일준비적금',    '국군장병 미래대비 앞날준비',              1, 5.00,  1000.00,  1000000.00, 24, 24, FALSE, TRUE,  FALSE, '20260101', 'SELLING'),
    (14, 'SAVINGS', 'AXful 특★한 적금',         '고객 모두의 높은 수익을 위한 특별한 준비', 1, 2.00, 10000.00, 30000000.00,  1,  1, FALSE, FALSE, FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 4. 청약 SUBSCRIPTION 2개  banking_product_id = 15 ~ 16
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_banking_products (
    banking_product_id, deposit_product_type, deposit_product_name, description, department_id,
    base_interest_rate, min_join_amount, max_join_amount, min_period_month, max_period_month,
    is_early_termination_allowed, is_tax_benefit_available, is_auto_renewal_available,
    released_at, deposit_product_status
) VALUES
    (15, 'SUBSCRIPTION', '주택청약종합저축',        '청약 자격 및 주택마련 저축 상품',        1, 3.10, 20000.00, 500000.00,  24, 600, FALSE, FALSE, FALSE, '20260101', 'SELLING'),
    (16, 'SUBSCRIPTION', '청년 주택드림 청약통장',  '만 19~34세 청년을 위한 우대 청약통장',   1, 3.10, 20000.00, 1000000.00, 24, 600, FALSE, TRUE,  FALSE, '20260101', 'SELLING')
ON CONFLICT (banking_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 예금 상품 상세 (banking_deposit_products)
-- ═══════════════════════════════════════════════════════════════════
-- TERM (정기예금)
INSERT INTO banking_deposit_products (deposit_product_id, banking_product_id, deposit_type, is_compound_interest)
VALUES
    ( 2,  6, 'TERM',   FALSE),
    ( 3,  7, 'TERM',   FALSE),
    ( 4,  8, 'TERM',   FALSE),
    ( 5,  9, 'TERM',   FALSE)
ON CONFLICT (deposit_product_id) DO NOTHING;

-- DEMAND (입출금자유)
INSERT INTO banking_deposit_products (deposit_product_id, banking_product_id, deposit_type, is_compound_interest)
VALUES
    ( 6, 17, 'DEMAND', FALSE),
    ( 7, 18, 'DEMAND', FALSE),
    ( 8, 19, 'DEMAND', FALSE),
    ( 9, 20, 'DEMAND', FALSE),
    (10, 21, 'DEMAND', TRUE),   -- 매일이자 통장: 복리
    (11, 22, 'DEMAND', FALSE),
    (12, 23, 'DEMAND', FALSE),
    (13, 24, 'DEMAND', FALSE),
    (14, 25, 'DEMAND', FALSE),
    (15, 26, 'DEMAND', FALSE)
ON CONFLICT (deposit_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 적금 상품 상세 (deposit_savings_products)
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_savings_products (savings_product_id, banking_product_id, saving_type, monthly_payment_min_amount, monthly_payment_max_amount)
VALUES
    (4, 10, 'FREE',     10000.00, 1000000.00),  -- 내맘대로적금: 자유납입
    (5, 11, 'FREE',     10000.00,  500000.00),  -- 달러자적금
    (6, 12, 'FREE',     10000.00, 1000000.00),  -- 맑은하늘적금
    (7, 13, 'REGULAR',   1000.00,  100000.00),  -- 장병내일준비적금: 정기납입
    (8, 14, 'FREE',     10000.00, 3000000.00)   -- 특★한 적금
ON CONFLICT (savings_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 청약 상품 상세 (deposit_subscription_products)
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO deposit_subscription_products (banking_product_id, monthly_payment_amount, min_monthly_payment, max_monthly_payment, max_recognized_payment_amount)
VALUES
    (15, 100000.00,  20000.00,  500000.00, 10000000.00),
    (16, 200000.00,  20000.00, 1000000.00, 10000000.00)
ON CONFLICT (banking_product_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 금리 (banking_deposit_product_interest_rates)
-- 기준: [id]/page.tsx PRODUCT_RATES 테이블 (2026.05.25 기준)
-- rate_id 7 ~ 41
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO banking_deposit_product_interest_rates (
    rate_id, banking_product_id, rate_type,
    minimum_contract_period, maximum_contract_period,
    minimum_join_amount, maximum_join_amount,
    rate, condition_description,
    effective_start_date, effective_end_date, is_active
) VALUES

    -- ── AXful 정기예금 (product 6): 기간별 기본금리 7개 + 우대 1개 ──────────────
    --   기간(개월)      기본금리  고객금리 (2026.05.25 기준)
    --   1 이상~3 미만   1.80     2.45
    --   3 이상~6 미만   2.00     2.75
    --   6 이상~9 미만   2.10     2.85
    --   9 이상~12 미만  2.10     2.85
    --  12 이상~24 미만  2.15     2.90
    --  24 이상~36 미만  2.20     2.40
    --  36개월           2.20     2.40
    ( 7, 6, 'BASE',  1,  2, 1000000.00, NULL, 1.80, '1개월 이상 ~ 3개월 미만 기본금리',   '20260101', NULL, TRUE),
    ( 8, 6, 'BASE',  3,  5, 1000000.00, NULL, 2.00, '3개월 이상 ~ 6개월 미만 기본금리',   '20260101', NULL, TRUE),
    ( 9, 6, 'BASE',  6,  8, 1000000.00, NULL, 2.10, '6개월 이상 ~ 9개월 미만 기본금리',   '20260101', NULL, TRUE),
    (10, 6, 'BASE',  9, 11, 1000000.00, NULL, 2.10, '9개월 이상 ~ 12개월 미만 기본금리',  '20260101', NULL, TRUE),
    (11, 6, 'BASE', 12, 23, 1000000.00, NULL, 2.15, '12개월 이상 ~ 24개월 미만 기본금리', '20260101', NULL, TRUE),
    (12, 6, 'BASE', 24, 35, 1000000.00, NULL, 2.20, '24개월 이상 ~ 36개월 미만 기본금리', '20260101', NULL, TRUE),
    (13, 6, 'BASE', 36, 36, 1000000.00, NULL, 2.20, '36개월 기본금리',                   '20260101', NULL, TRUE),
    (14, 6, 'PREFERENTIAL', 3, 23, 1000000.00, NULL, 0.75, '비대면(인터넷·스타뱅킹) 가입 우대금리', '20260101', NULL, TRUE),

    -- ── AXful 수퍼정기예금(개인) (product 7): 기간별 기본금리 4개 + 우대 1개 ─────
    --   1 이상~6 미만   1.90     2.20
    --   6 이상~12 미만  2.00     2.25
    --  12 이상~24 미만  2.10     2.30
    --  24 이상~36 미만  2.15     2.30
    (15, 7, 'BASE',  1,  5, 1000000.00, NULL, 1.90, '1개월 이상 ~ 6개월 미만 기본금리',   '20260101', NULL, TRUE),
    (16, 7, 'BASE',  6, 11, 1000000.00, NULL, 2.00, '6개월 이상 ~ 12개월 미만 기본금리',  '20260101', NULL, TRUE),
    (17, 7, 'BASE', 12, 23, 1000000.00, NULL, 2.10, '12개월 이상 ~ 24개월 미만 기본금리', '20260101', NULL, TRUE),
    (18, 7, 'BASE', 24, 35, 1000000.00, NULL, 2.15, '24개월 이상 ~ 36개월 미만 기본금리', '20260101', NULL, TRUE),
    (19, 7, 'PREFERENTIAL', 12, 35, 1000000.00, NULL, 0.20, '비대면 가입 우대금리', '20260101', NULL, TRUE),

    -- ── 일반정기예금 (product 8): 기간별 기본금리 3개 ───────────────────────────
    --   1 이상~6 미만   1.85     2.25
    --   6 이상~12 미만  2.00     2.25
    --  12 이상~36 미만  2.10     2.25
    (20, 8, 'BASE',  1,  5, 1000000.00, NULL, 1.85, '1개월 이상 ~ 6개월 미만 기본금리',   '20260101', NULL, TRUE),
    (21, 8, 'BASE',  6, 11, 1000000.00, NULL, 2.00, '6개월 이상 ~ 12개월 미만 기본금리',  '20260101', NULL, TRUE),
    (22, 8, 'BASE', 12, 36, 1000000.00, NULL, 2.10, '12개월 이상 ~ 36개월 기본금리',     '20260101', NULL, TRUE),

    -- ── AXful 청년도약계좌 (product 9) ──────────────────────────────────────────
    --   60개월(기본)            3.50     4.50
    --   60개월(소득요건 충족)    3.50     6.00
    (23, 9, 'BASE',        60, 60, 1000.00, NULL, 3.50, '60개월 기본금리',                   '20260101', NULL, TRUE),
    (24, 9, 'PREFERENTIAL',60, 60, 1000.00, NULL, 1.00, '기본 소득요건 충족 우대 (+1.0%)',   '20260101', NULL, TRUE),
    (25, 9, 'PREFERENTIAL',60, 60, 1000.00, NULL, 1.50, '고소득 요건 추가 충족 우대 (+1.5%)', '20260101', NULL, TRUE),

    -- ── AXful 내맘대로적금 (product 10): 연 2.95%~3.55% ────────────────────────
    (26, 10, 'BASE',         1, 36, 10000.00, NULL, 2.95, '기본금리',         '20260101', NULL, TRUE),
    (27, 10, 'PREFERENTIAL', 1, 36, 10000.00, NULL, 0.60, '자동이체 설정 우대', '20260101', NULL, TRUE),

    -- ── AXful 달러자적금 (product 11): 연 1%~7.2% ──────────────────────────────
    (28, 11, 'BASE',         1,  6, 10000.00, NULL, 1.00, '기본금리',            '20260101', NULL, TRUE),
    (29, 11, 'PREFERENTIAL', 6,  6, 10000.00, NULL, 6.20, '달러 환전 실적 우대', '20260101', NULL, TRUE),

    -- ── AXful 맑은하늘적금 (product 12): 연 2.85%~3.85% ─────────────────────────
    (30, 12, 'BASE',         1, 36, 10000.00, NULL, 2.85, '기본금리',                 '20260101', NULL, TRUE),
    (31, 12, 'PREFERENTIAL', 1, 36, 10000.00, NULL, 1.00, '맑은하늘 인증코드 등록 우대', '20260101', NULL, TRUE),

    -- ── AXful 장병내일준비적금 (product 13): 연 5%~10.5% ────────────────────────
    (32, 13, 'BASE',         24, 24, 1000.00, NULL, 5.00, '기본금리',                    '20260101', NULL, TRUE),
    (33, 13, 'PREFERENTIAL', 24, 24, 1000.00, NULL, 5.50, '정부 기여금 및 납입 완료 우대', '20260101', NULL, TRUE),

    -- ── AXful 특★한 적금 (product 14): 연 2%~6% ────────────────────────────────
    (34, 14, 'BASE',         1, 1, 10000.00, NULL, 2.00, '기본금리',         '20260101', NULL, TRUE),
    (35, 14, 'PREFERENTIAL', 1, 1, 10000.00, NULL, 4.00, '특별 조건 충족 우대', '20260101', NULL, TRUE),

    -- ── 주택청약종합저축 (product 15): 연 3.1% ──────────────────────────────────
    (36, 15, 'BASE', 24, 600, 20000.00, NULL, 3.10, '기본금리', '20260101', NULL, TRUE),

    -- ── 청년 주택드림 청약통장 (product 16): 연 3.1%~4.5% ───────────────────────
    (37, 16, 'BASE',         24, 600, 20000.00, NULL, 3.10, '기본금리',              '20260101', NULL, TRUE),
    (38, 16, 'PREFERENTIAL', 24, 600, 20000.00, NULL, 1.40, '청년 소득 조건 충족 우대', '20260101', NULL, TRUE),

    -- ── 입출금자유: 명시적 금리 있는 상품만 ──────────────────────────────────────
    -- 모니모 AXful 매일이자 통장 (product 21): 연 0.5%
    (39, 21, 'BASE', NULL, NULL, 0.00, NULL, 0.50, '매일 이자 지급 기본금리', '20260101', NULL, TRUE),
    -- AXful 모임금고 (product 22): 연 2.0% (최대 1천만원)
    (40, 22, 'BASE', NULL, NULL, 0.00, 10000000.00, 2.00, '모임금고 기본금리 (최대 1천만원 한도)', '20260101', NULL, TRUE),
    -- AXful 청년우대통장 (product 26): 연 0.5% + 우대 0.5%
    (41, 26, 'BASE',         NULL, NULL, 0.00, NULL, 0.50, '기본금리',           '20260101', NULL, TRUE),
    (42, 26, 'PREFERENTIAL', NULL, NULL, 0.00, NULL, 0.50, '청년 우대금리 (+0.5%)', '20260101', NULL, TRUE)

ON CONFLICT (rate_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 가입 채널 (banking_deposit_product_join_channels)
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO banking_deposit_product_join_channels (channel_id, banking_product_id, join_channel_code)
VALUES
    -- TERM 정기예금 (product 6~9)
    (10,  6, 'WEB'),    (11,  6, 'MOBILE'),   -- AXful 정기예금: 인터넷·스타뱅킹
    (12,  7, 'BRANCH'),                        -- AXful 수퍼정기예금: 영업점
    (13,  8, 'BRANCH'),                        -- 일반정기예금: 영업점
    (14,  9, 'WEB'),    (15,  9, 'MOBILE'),   -- AXful 청년도약계좌: 인터넷·스타뱅킹
    -- 적금 (product 10~14)
    (16, 10, 'WEB'),    (17, 10, 'MOBILE'),   -- AXful 내맘대로적금
    (18, 11, 'MOBILE'),                        -- AXful 달러자적금: 스타뱅킹
    (19, 12, 'WEB'),    (20, 12, 'MOBILE'),   -- AXful 맑은하늘적금
    (21, 13, 'MOBILE'),                        -- AXful 장병내일준비적금: 스타뱅킹
    (22, 14, 'MOBILE'),                        -- AXful 특★한 적금: 스타뱅킹
    -- 청약 (product 15~16)
    (23, 15, 'WEB'),    (24, 15, 'MOBILE'),   -- 주택청약종합저축
    (25, 16, 'MOBILE'),                        -- 청년 주택드림 청약통장: 스타뱅킹
    -- 입출금자유 (product 17~26)
    (26, 17, 'BRANCH'),                        -- AXful 쏙머니통장: 영업점
    (27, 18, 'BRANCH'),                        -- 당선통장: 영업점
    (28, 19, 'MOBILE'),                        -- AXful 생계비계좌: 스타뱅킹
    (29, 20, 'MOBILE'),                        -- AXful GS Pay통장: 스타뱅킹
    (30, 21, 'BRANCH'),                        -- 모니모 AXful 매일이자 통장: 영업점
    (31, 22, 'MOBILE'),                        -- AXful 모임금고: 스타뱅킹
    (32, 23, 'MOBILE'),                        -- AXful 스타통장: 스타뱅킹
    (33, 24, 'WEB'),    (34, 24, 'MOBILE'),   -- AXful 지갑통장: 인터넷·스타뱅킹
    (35, 25, 'WEB'),    (36, 25, 'MOBILE'),   -- AXful 자유입출금통장: 인터넷·스타뱅킹
    (37, 26, 'WEB'),    (38, 26, 'MOBILE')    -- AXful 청년우대통장: 인터넷·스타뱅킹
ON CONFLICT (channel_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 가입 대상 (banking_deposit_product_target_groups)
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO banking_deposit_product_target_groups (banking_product_id, target_group_id)
VALUES
    -- TERM
    ( 6, 1), ( 7, 1), ( 8, 1),
    ( 9, 1), ( 9, 3),           -- 청년도약계좌: 개인 + 청년
    -- 적금
    (10, 1), (11, 1), (12, 1),
    (13, 4),                     -- 장병내일준비적금: 국군장병
    (14, 1),
    -- 청약
    (15, 1),
    (16, 1), (16, 3),            -- 청년 주택드림: 개인 + 청년
    -- 입출금자유
    (17, 1), (18, 1), (19, 1), (20, 1), (21, 1),
    (22, 1), (23, 1), (24, 1), (25, 1),
    (26, 1), (26, 3)             -- 청년우대통장: 개인 + 청년
ON CONFLICT (banking_product_id, target_group_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 시퀀스 동기화
-- ═══════════════════════════════════════════════════════════════════
SELECT setval(pg_get_serial_sequence('deposit_banking_products',               'banking_product_id'),  COALESCE((SELECT MAX(banking_product_id) FROM deposit_banking_products),  1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_products',               'deposit_product_id'),  COALESCE((SELECT MAX(deposit_product_id) FROM banking_deposit_products),  1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_savings_products',               'savings_product_id'),  COALESCE((SELECT MAX(savings_product_id) FROM deposit_savings_products),  1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_interest_rates', 'rate_id'),             COALESCE((SELECT MAX(rate_id)            FROM banking_deposit_product_interest_rates), 1), TRUE);
SELECT setval(pg_get_serial_sequence('banking_deposit_product_join_channels',  'channel_id'),          COALESCE((SELECT MAX(channel_id)         FROM banking_deposit_product_join_channels),  1), TRUE);
SELECT setval(pg_get_serial_sequence('deposit_target_groups',                  'target_group_id'),     COALESCE((SELECT MAX(target_group_id)    FROM deposit_target_groups),     1), TRUE);

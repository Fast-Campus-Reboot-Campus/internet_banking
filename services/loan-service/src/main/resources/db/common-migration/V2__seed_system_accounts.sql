-- 여신 시스템 계좌(수납/집행) seed — 공통 계좌 마스터(common_account).
-- 자동이체/온라인/역분개 환급의 수납계좌, 대출실행의 집행계좌로 사용한다.
-- account_nickname 을 역할 식별자로 사용(LOAN_COLLECTION / LOAN_DISBURSEMENT).
--
-- 계좌번호는 현재 더미값(자행코드 004 기준)이며, 운영 실계좌 확정 시 교체한다.
-- customer_id=0 은 시스템 계좌 표식(특정 고객 소유 아님).
INSERT INTO common_account
    (account_no,       customer_id, account_type_cd, bank_cd, account_nickname,   balance, currency_cd, account_status, created_at)
VALUES
    ('0040000000001',  0,           'SYSTEM',        '004',   'LOAN_COLLECTION',   0,       'KRW',       'ACTIVE',       now()),
    ('0040000000002',  0,           'SYSTEM',        '004',   'LOAN_DISBURSEMENT', 0,       'KRW',       'ACTIVE',       now());

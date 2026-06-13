-- V5__full_erd_schema.sql 에서 실수로 포함된 미사용 테이블 정리
--
-- 1) 한글 payment 테이블 6개
--    - payment-service 소속 테이블이 deposit-db에 잘못 생성됨
--    - deposit-service Java 코드에서 단 한 곳도 참조하지 않음
--    - FK 의존 순서대로 DROP
--
-- 2) ERD 단수형 중복 테이블 2개
--    - deposit_account  → deposit_accounts 가 실제 사용 (Account.java @Table)
--    - deposit_contract → deposit_contracts 가 실제 사용 (Contract.java @Table)

-- ── 1. 한글 payment 테이블 (FK 의존 순서: 자식 → 부모) ───────────────────────

DROP TABLE IF EXISTS "원장";
DROP TABLE IF EXISTS "상태이력";
DROP TABLE IF EXISTS "금융결제원청산거래";
DROP TABLE IF EXISTS "한국은행결제거래";
DROP TABLE IF EXISTS "결제지시";
DROP TABLE IF EXISTS "인증토큰";

-- ── 2. ERD 단수형 중복 테이블 ────────────────────────────────────────────────

DROP TABLE IF EXISTS deposit_account;
DROP TABLE IF EXISTS deposit_contract;

-- ── 3. 홍길동(9001) 계좌 잔액 현실화 ────────────────────────────────────────
-- V15 INSERT ON CONFLICT DO NOTHING 으로 기존 50M×4 잔액이 유지된 것을 보정.
-- 현금흐름 채점 시나리오: 총 잔액 6,200,000원, 월 잉여자금 1,640,000원
-- → total_balance(6.2M) < monthly_surplus*12(19.68M) → 저축 성장형(is_accumulate=True)

UPDATE deposit_accounts SET balance = 5000000.00  WHERE account_id = 2001 AND customer_id = '9001';
UPDATE deposit_accounts SET balance = 1200000.00  WHERE account_id = 2002 AND customer_id = '9001';
UPDATE deposit_accounts SET balance = 0.00        WHERE account_id = 2003 AND customer_id = '9001';
UPDATE deposit_accounts SET balance = 0.00        WHERE account_id = 2004 AND customer_id = '9001';

-- ── 4. 홍길동(9001) 거래 날짜 재조정 ────────────────────────────────────────
-- V15 심을 당시 NOW()-89d 로 설정했으나 migration 실행 후 시간이 지나면서
-- 90일 cutoff 밖으로 밀려 1건(3.3M)이 집계에서 누락됨.
-- → 3개월을 80/60/30일 기준으로 재설정하여 모두 집계 범위 내 유지.

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '80 days'
 WHERE transaction_number = 'TX-9001-M3-IN-01';

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '75 days'
 WHERE transaction_number = 'TX-9001-M3-OUT-01';

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '55 days'
 WHERE transaction_number = 'TX-9001-M2-IN-01';

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '45 days'
 WHERE transaction_number = 'TX-9001-M2-OUT-01';

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '25 days'
 WHERE transaction_number = 'TX-9001-M1-IN-01';

UPDATE deposit_transactions
   SET transaction_at = NOW() - INTERVAL '15 days'
 WHERE transaction_number = 'TX-9001-M1-OUT-01';

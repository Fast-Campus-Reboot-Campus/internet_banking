"""
테스트 픽스처.

SQLite 인메모리 DB를 매 테스트마다 새로 생성해 격리성을 보장한다.
`client`  — 시드 데이터가 채워진 TestClient (대부분의 테스트용)
`empty_client` — 테이블만 존재하고 데이터 없는 TestClient (빈 결과 시나리오)
`rich_client`  — 다중 고객·다중 계약 데이터가 있는 TestClient
"""

import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.main import app, get_db


# ── DDL ───────────────────────────────────────────────────────────────────────

_DDL = [
    """CREATE TABLE deposit_banking_products (
        banking_product_id INTEGER PRIMARY KEY,
        deposit_product_name TEXT NOT NULL,
        deposit_product_type TEXT,
        description TEXT,
        base_interest_rate NUMERIC,
        min_join_amount NUMERIC,
        max_join_amount NUMERIC,
        min_period_month INTEGER,
        max_period_month INTEGER,
        is_early_termination_allowed BOOLEAN,
        is_tax_benefit_available BOOLEAN,
        deposit_product_status TEXT
    )""",
    """CREATE TABLE banking_deposit_product_interest_rates (
        rate_id INTEGER PRIMARY KEY,
        banking_product_id INTEGER,
        rate_type TEXT,
        minimum_contract_period INTEGER,
        maximum_contract_period INTEGER,
        rate NUMERIC,
        condition_description TEXT
    )""",
    """CREATE TABLE deposit_special_terms (
        special_term_id INTEGER PRIMARY KEY,
        special_term_name TEXT,
        special_term_content TEXT,
        special_term_summary TEXT,
        is_required BOOLEAN,
        status TEXT
    )""",
    """CREATE TABLE deposit_accounts (
        account_id INTEGER PRIMARY KEY,
        account_number TEXT,
        customer_id TEXT,
        account_type TEXT,
        account_alias TEXT,
        balance NUMERIC,
        currency TEXT,
        account_status TEXT,
        opened_at TEXT,
        closed_at TEXT
    )""",
    """CREATE TABLE deposit_contracts (
        contract_id INTEGER PRIMARY KEY AUTOINCREMENT,
        contract_number TEXT,
        customer_id TEXT,
        banking_product_id INTEGER,
        join_amount NUMERIC,
        contract_interest_rate NUMERIC,
        started_at TEXT,
        maturity_at TEXT,
        contract_status TEXT
    )""",
    """CREATE TABLE deposit_interest_history (
        interest_id INTEGER PRIMARY KEY,
        contract_id INTEGER,
        account_id INTEGER,
        applied_interest_rate NUMERIC,
        interest_amount NUMERIC,
        interest_after_tax NUMERIC,
        interest_paid_at TEXT
    )""",
    """CREATE TABLE deposit_transactions (
        transaction_id INTEGER PRIMARY KEY,
        transaction_number TEXT,
        account_id INTEGER,
        transaction_type TEXT,
        status TEXT,
        amount NUMERIC,
        created_at TEXT
    )""",
    """CREATE TABLE deposit_contract_special_term_agreements (
        agreement_id INTEGER PRIMARY KEY AUTOINCREMENT,
        contract_id INTEGER,
        special_term_id INTEGER,
        agreed_at TEXT,
        is_agreed BOOLEAN
    )""",
]


def _create_tables(conn) -> None:
    for ddl in _DDL:
        conn.execute(text(ddl))


# ── 시드 데이터 ────────────────────────────────────────────────────────────────

def _seed_base(conn) -> None:
    """기본 시드: 상품 1, 금리 1, 특약 1, 계좌 1, 계약 1, 이자 1, 거래 2."""
    conn.execute(text("""
        INSERT INTO deposit_banking_products VALUES
        (1,'정기예금 플러스','DEPOSIT','안정적인 정기예금',3.5,100000,100000000,1,60,1,1,'SELLING')
    """))
    conn.execute(text("""
        INSERT INTO banking_deposit_product_interest_rates VALUES
        (1,1,'BASE',12,24,3.5,'기본금리'),
        (2,1,'PREFERENTIAL',12,24,0.3,'급여이체 우대')
    """))
    conn.execute(text("""
        INSERT INTO deposit_special_terms VALUES
        (1,'개인정보 수집 이용 동의','개인정보를 수집하고 이용합니다.','개인정보 동의 요약',1,'ACTIVE'),
        (2,'중도해지 약관','중도해지 시 약정이율의 50%가 적용됩니다.','중도해지 이율 안내',1,'ACTIVE')
    """))
    conn.execute(text("""
        INSERT INTO deposit_accounts VALUES
        (1,'001-123-000001','CUST001','DEPOSIT','내 예금',5000000,'KRW','ACTIVE','20260101',NULL)
    """))
    conn.execute(text("""
        INSERT INTO deposit_contracts VALUES
        (1,'CTR-001','CUST001',1,5000000,3.5,'20260101','20270101','ACTIVE')
    """))
    conn.execute(text("""
        INSERT INTO deposit_interest_history VALUES
        (1,1,1,3.5,175000,148050,'20261231')
    """))
    conn.execute(text("""
        INSERT INTO deposit_transactions VALUES
        (1,'TX-001',1,'DEPOSIT','COMPLETED',500000,'2026-05-01'),
        (2,'TX-002',1,'TRANSFER','COMPLETED',100000,'2026-05-10'),
        (3,'TX-003',1,'WITHDRAWAL','PENDING',50000,'2026-05-20')
    """))


def _seed_rich(conn) -> None:
    """풍부한 시드: 상품 3, 고객 2, 각종 다중 레코드."""
    conn.execute(text("""
        INSERT INTO deposit_banking_products VALUES
        (1,'정기예금 플러스','DEPOSIT','안정적인 정기예금',3.5,100000,100000000,1,60,1,1,'SELLING'),
        (2,'자유적금','SAVINGS','자유롭게 납입하는 적금',4.0,10000,50000000,12,36,0,1,'SELLING'),
        (3,'주택청약종합저축','SUBSCRIPTION','청약 자격 적립 상품',2.8,2000,1000000,1,600,0,0,'SELLING'),
        (4,'구 정기예금','DEPOSIT','판매 종료 상품',2.0,50000,50000000,1,12,1,0,'CLOSED')
    """))
    conn.execute(text("""
        INSERT INTO banking_deposit_product_interest_rates VALUES
        (1,1,'BASE',12,24,3.5,'기본금리'),
        (2,1,'PREFERENTIAL',12,24,0.3,'급여이체 우대'),
        (3,2,'BASE',12,36,4.0,'기본금리'),
        (4,2,'PREFERENTIAL',24,36,0.5,'자동이체 우대'),
        (5,3,'BASE',12,600,2.8,'기본금리')
    """))
    conn.execute(text("""
        INSERT INTO deposit_special_terms VALUES
        (1,'개인정보 수집 이용 동의','개인정보를 수집하고 이용합니다.','개인정보 동의 요약',1,'ACTIVE'),
        (2,'중도해지 약관','중도해지 시 약정이율의 50%가 적용됩니다.','중도해지 이율 안내',1,'ACTIVE'),
        (3,'구 이용약관','폐지된 약관입니다.','구 약관',0,'INACTIVE')
    """))
    conn.execute(text("""
        INSERT INTO deposit_accounts VALUES
        (1,'001-001-000001','CUST001','DEPOSIT','내 예금',5000000,'KRW','ACTIVE','20260101',NULL),
        (2,'001-001-000002','CUST001','SAVINGS','내 적금',1200000,'KRW','ACTIVE','20260301',NULL),
        (3,'001-002-000001','CUST002','DEPOSIT','예금',3000000,'KRW','ACTIVE','20260601',NULL)
    """))
    conn.execute(text("""
        INSERT INTO deposit_contracts VALUES
        (1,'CTR-001','CUST001',1,5000000,3.5,'20260101','20270101','ACTIVE'),
        (2,'CTR-002','CUST001',2,1200000,4.0,'20250101','20260101','MATURED'),
        (3,'CTR-003','CUST002',1,3000000,3.5,'20260601','20270601','ACTIVE')
    """))
    conn.execute(text("""
        INSERT INTO deposit_interest_history VALUES
        (1,1,1,3.5,175000,148050,'20261231'),
        (2,2,2,4.0,48000,40608,'20260101'),
        (3,3,3,3.5,105000,88830,'20261231')
    """))
    conn.execute(text("""
        INSERT INTO deposit_transactions VALUES
        (1,'TX-001',1,'DEPOSIT','COMPLETED',500000,'2026-05-01'),
        (2,'TX-002',1,'TRANSFER','COMPLETED',100000,'2026-05-10'),
        (3,'TX-003',2,'DEPOSIT','COMPLETED',200000,'2026-05-15'),
        (4,'TX-004',3,'WITHDRAWAL','COMPLETED',50000,'2026-05-20'),
        (5,'TX-005',1,'WITHDRAWAL','COMPLETED',30000,'2026-05-22')
    """))


# ── 픽스처 팩토리 ──────────────────────────────────────────────────────────────

def _make_client(seed_fn=None) -> TestClient:
    """SQLite 인메모리 DB 기반 TestClient를 반환한다."""
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    with engine.begin() as conn:
        _create_tables(conn)
        if seed_fn:
            seed_fn(conn)

    SessionFactory = sessionmaker(bind=engine, autoflush=False, autocommit=False)

    def override_get_db():
        db = SessionFactory()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    client = TestClient(app)
    client._engine = engine  # 픽스처 정리용
    return client


@pytest.fixture()
def client():
    """기본 시드 데이터가 들어 있는 TestClient."""
    c = _make_client(_seed_base)
    try:
        yield c
    finally:
        app.dependency_overrides.clear()
        c._engine.dispose()


@pytest.fixture()
def empty_client():
    """테이블만 있고 데이터는 없는 TestClient."""
    c = _make_client(seed_fn=None)
    try:
        yield c
    finally:
        app.dependency_overrides.clear()
        c._engine.dispose()


@pytest.fixture()
def rich_client():
    """다중 상품·고객·계약 시드 데이터가 있는 TestClient."""
    c = _make_client(_seed_rich)
    try:
        yield c
    finally:
        app.dependency_overrides.clear()
        c._engine.dispose()

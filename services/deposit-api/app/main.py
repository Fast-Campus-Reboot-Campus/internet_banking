"""
수신 시스템 FastAPI 애플리케이션.

테이블:
  deposit_banking_products                  — 수신 상품
  banking_deposit_product_interest_rates    — 상품별 금리
  deposit_special_terms                     — 특약
  deposit_accounts                          — 계좌
  deposit_contracts                         — 계약
  deposit_interest_history                  — 금리 적용(이자) 내역
  deposit_transactions                      — 거래내역
  deposit_contract_special_term_agreements  — 계약-특약 동의
"""

from collections.abc import Generator
from contextlib import asynccontextmanager
from datetime import date, timedelta
from uuid import uuid4

from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sqlalchemy import create_engine, text
from sqlalchemy.orm import Session, sessionmaker

from app.config import get_settings
from app.kafka import DepositKafkaProducer

# ── 설정 & Kafka ──────────────────────────────────────────────────────────────

_settings = get_settings()
_kafka = DepositKafkaProducer(_settings)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await _kafka.start()
    try:
        yield
    finally:
        await _kafka.stop()


# ── DB 설정 (테스트에서 get_db 를 override) ────────────────────────────────────

_engine = create_engine(
    _settings.database_url,
    connect_args={"check_same_thread": False} if "sqlite" in _settings.database_url else {},
)
_SessionLocal = sessionmaker(bind=_engine, autoflush=False, autocommit=False)


def get_db() -> Generator[Session, None, None]:
    db = _SessionLocal()
    try:
        yield db
    finally:
        db.close()


# ── Pydantic 스키마 ────────────────────────────────────────────────────────────

class ProductListItem(BaseModel):
    product_id: int
    product_name: str
    product_type: str | None = None
    description: str | None = None
    base_interest_rate: float | None = None
    min_join_amount: float | None = None
    max_join_amount: float | None = None
    min_period_month: int | None = None
    max_period_month: int | None = None
    is_early_termination_allowed: bool | None = None
    is_tax_benefit_available: bool | None = None
    product_status: str | None = None


class InterestRateItem(BaseModel):
    rate_id: int
    rate_type: str | None = None
    minimum_contract_period: int | None = None
    maximum_contract_period: int | None = None
    rate: float | None = None
    condition_description: str | None = None


class ProductDetailResponse(ProductListItem):
    interest_rates: list[InterestRateItem] = Field(default_factory=list)


class ContractCreateRequest(BaseModel):
    customer_id: str
    banking_product_id: int
    join_amount: float
    period_months: int


class ContractResponse(BaseModel):
    contract_id: int
    contract_number: str
    customer_id: str
    banking_product_id: int
    join_amount: float
    contract_interest_rate: float | None = None
    started_at: str
    maturity_at: str
    contract_status: str


class AccountResponse(BaseModel):
    account_id: int
    account_number: str
    customer_id: str
    account_type: str | None = None
    account_alias: str | None = None
    balance: float | None = None
    currency: str | None = None
    account_status: str | None = None
    opened_at: str | None = None


class TransactionItem(BaseModel):
    transaction_id: int
    transaction_number: str | None = None
    account_id: int
    transaction_type: str | None = None
    status: str | None = None
    amount: float | None = None
    created_at: str | None = None


class SpecialTermAgreementRequest(BaseModel):
    special_term_ids: list[int]
    agreed_at: str | None = None


class SpecialTermAgreementResponse(BaseModel):
    contract_id: int
    agreed_term_ids: list[int]
    agreed_at: str


class AppliedRateItem(BaseModel):
    interest_id: int
    contract_id: int
    account_id: int | None = None
    applied_interest_rate: float | None = None
    interest_amount: float | None = None
    interest_after_tax: float | None = None
    interest_paid_at: str | None = None


class CashFlowResponse(BaseModel):
    account_id: int
    period_start: str | None = None
    period_end: str | None = None
    total_inflow: float
    total_outflow: float
    net_flow: float
    transaction_count: int
    transactions: list[TransactionItem]


class RecommendItem(BaseModel):
    product_id: int
    product_name: str
    product_type: str | None = None
    base_interest_rate: float | None = None
    min_join_amount: float | None = None
    max_join_amount: float | None = None
    min_period_month: int | None = None
    max_period_month: int | None = None
    reason: str
    match_score: int


class RecommendResponse(BaseModel):
    customer_id: str | None = None
    recommendations: list[RecommendItem]


# ── 헬퍼 ────────────────────────────────────────────────────────────────────────

def _rows(db: Session, sql: str, params: dict | None = None) -> list[dict]:
    result = db.execute(text(sql), params or {})
    cols = result.keys()
    return [dict(zip(cols, row)) for row in result.fetchall()]


def _one(db: Session, sql: str, params: dict | None = None) -> dict | None:
    rows = _rows(db, sql, params)
    return rows[0] if rows else None


# ── FastAPI 앱 ────────────────────────────────────────────────────────────────

app = FastAPI(title="수신 시스템 API", version="0.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",   # Vite dev server
        "http://localhost:3000",   # 기타 Vue dev server
        "http://127.0.0.1:5173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── 헬스 ─────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "UP"}


# ── 상품 ──────────────────────────────────────────────────────────────────────

@app.get("/products", response_model=list[ProductListItem])
def list_products(
    product_type: str | None = Query(None),
    status: str | None = Query(None),
    db: Session = Depends(get_db),
):
    conditions = ["1=1"]
    params: dict = {}
    if product_type:
        conditions.append("deposit_product_type = :product_type")
        params["product_type"] = product_type
    if status:
        conditions.append("deposit_product_status = :status")
        params["status"] = status

    where = " AND ".join(conditions)
    rows = _rows(
        db,
        f"""
        SELECT banking_product_id AS product_id,
               deposit_product_name AS product_name,
               deposit_product_type AS product_type,
               description,
               base_interest_rate,
               min_join_amount,
               max_join_amount,
               min_period_month,
               max_period_month,
               is_early_termination_allowed,
               is_tax_benefit_available,
               deposit_product_status AS product_status
          FROM deposit_banking_products
         WHERE {where}
         ORDER BY banking_product_id
        """,
        params,
    )
    return rows


# ── 현금흐름 기반 추천 헬퍼 ─────────────────────────────────────────────────────

def _analyze_customer_cash_flow(db: Session, customer_id: str, months: int = 3) -> dict | None:
    """고객의 전체 계좌 거래 데이터를 분석해 현금흐름 지표를 반환한다.

    Returns:
        {
            total_balance    : 전체 계좌 잔액 합계
            monthly_surplus  : 월평균 잉여자금 (수입 - 지출) / months
            monthly_tx_count : 월평균 거래 건수
            has_data         : 거래 데이터 존재 여부
        }
        고객 계좌가 없으면 None
    """
    accounts = _rows(
        db,
        "SELECT account_id, balance FROM deposit_accounts WHERE customer_id = :cid",
        {"cid": customer_id},
    )
    if not accounts:
        return None

    total_balance = sum(float(a.get("balance") or 0) for a in accounts)
    id_list = ",".join(str(a["account_id"]) for a in accounts)

    rows = _rows(
        db,
        f"""
        SELECT transaction_type, amount
          FROM deposit_transactions
         WHERE account_id IN ({id_list})
           AND status = 'COMPLETED'
        """,
    )

    if not rows:
        return {
            "total_balance": total_balance,
            "monthly_surplus": 0.0,
            "monthly_tx_count": 0.0,
            "has_data": False,
        }

    inflow  = sum(float(r["amount"] or 0) for r in rows if r["transaction_type"] == "DEPOSIT")
    outflow = sum(
        float(r["amount"] or 0) for r in rows
        if r["transaction_type"] in ("WITHDRAWAL", "TRANSFER")
    )
    monthly_surplus  = (inflow - outflow) / months
    monthly_tx_count = len(rows) / months

    return {
        "total_balance":    total_balance,
        "monthly_surplus":  monthly_surplus,
        "monthly_tx_count": monthly_tx_count,
        "has_data":         True,
    }


def _cashflow_based_recommendations(db: Session, customer_id: str, cf: dict) -> dict:
    """현금흐름 분석 결과를 바탕으로 최대 5개 상품을 추천한다.

    점수 기준:
      - 공통 기본 점수: 50
      - DEPOSIT  : 잔액 ≥ min_join_amount → +20 / 잉여자금 > 0 → +10
      - SAVINGS  : 월 잉여자금 ≥ min_join_amount → +20 / 정기 거래(월 3건↑) → +10
      - SUBSCRIPT: 잉여자금 > 0 → +5
      - 금리 ≥ 3.5% → +10 / ≥ 3.0% → +5
    """
    total_balance    = cf["total_balance"]
    monthly_surplus  = cf["monthly_surplus"]
    monthly_tx_count = cf["monthly_tx_count"]

    products = _rows(
        db,
        """
        SELECT banking_product_id AS product_id,
               deposit_product_name AS product_name,
               deposit_product_type AS product_type,
               base_interest_rate,
               min_join_amount,
               max_join_amount,
               min_period_month,
               max_period_month
          FROM deposit_banking_products
         WHERE deposit_product_status = 'SELLING'
         ORDER BY base_interest_rate DESC
        """,
    )

    scored = []
    for p in products:
        score   = 50
        reasons: list[str] = []

        min_amt = float(p.get("min_join_amount") or 0)
        rate    = float(p.get("base_interest_rate") or 0)
        ptype   = p.get("product_type", "")

        if ptype == "DEPOSIT":
            if total_balance >= min_amt:
                score += 20
                reasons.append(f"보유 잔액({total_balance:,.0f}원)으로 가입 가능")
            if monthly_surplus > 0:
                score += 10
                reasons.append(f"월 {monthly_surplus:,.0f}원 잉여자금 확인")

        elif ptype == "SAVINGS":
            if monthly_surplus >= min_amt:
                score += 20
                reasons.append(f"월 여유자금({monthly_surplus:,.0f}원)으로 납입 가능")
            if monthly_tx_count >= 3:
                score += 10
                reasons.append("정기적 거래 패턴 확인")

        elif ptype == "SUBSCRIPTION":
            if monthly_surplus > 0:
                score += 5
                reasons.append("주택 마련 목적 추천")

        if rate >= 3.5:
            score += 10
            reasons.append("고금리 혜택")
        elif rate >= 3.0:
            score += 5

        if not reasons:
            reasons.append("기본 추천 상품")

        score = max(0, min(100, score))
        scored.append({**p, "reason": ", ".join(reasons), "match_score": score})

    scored.sort(key=lambda x: x["match_score"], reverse=True)
    return {"customer_id": customer_id, "recommendations": scored[:5]}


# ── 추천 엔드포인트 ────────────────────────────────────────────────────────────

@app.get("/products/recommend", response_model=RecommendResponse)
def recommend_products(
    customer_id: str | None = Query(None),
    available_amount: float | None = Query(None),
    preferred_period_months: int | None = Query(None),
    prefer_high_rate: bool = Query(True),
    db: Session = Depends(get_db),
):
    """상품 추천.

    - customer_id 만 제공하고 수동 파라미터(available_amount, preferred_period_months)가
      없으면 → 고객의 현금흐름을 자동 분석해 추천
    - 수동 파라미터가 하나라도 있으면 → 기존 파라미터 기반 추천 (하위 호환)
    """
    # ── 현금흐름 기반 추천 ──────────────────────────────────────────────────────
    if customer_id and available_amount is None and preferred_period_months is None:
        cf = _analyze_customer_cash_flow(db, customer_id)
        if cf:
            return _cashflow_based_recommendations(db, customer_id, cf)

    # ── 파라미터 기반 추천 (기존 로직 · 하위 호환) ──────────────────────────────
    conditions = ["deposit_product_status = 'SELLING'"]
    params: dict = {}

    if available_amount is not None:
        conditions.append("min_join_amount <= :amount AND max_join_amount >= :amount")
        params["amount"] = available_amount

    if preferred_period_months is not None:
        conditions.append("min_period_month <= :period AND max_period_month >= :period")
        params["period"] = preferred_period_months

    order = "base_interest_rate DESC" if prefer_high_rate else "base_interest_rate ASC"
    where = " AND ".join(conditions)

    rows = _rows(
        db,
        f"""
        SELECT banking_product_id AS product_id,
               deposit_product_name AS product_name,
               deposit_product_type AS product_type,
               base_interest_rate,
               min_join_amount,
               max_join_amount,
               min_period_month,
               max_period_month
          FROM deposit_banking_products
         WHERE {where}
         ORDER BY {order}
         LIMIT 5
        """,
        params,
    )

    recommendations = []
    for rank, row in enumerate(rows, start=1):
        reasons = []
        if available_amount is not None:
            reasons.append("가입 금액 조건 충족")
        if preferred_period_months is not None:
            reasons.append("희망 기간 조건 충족")
        if prefer_high_rate:
            reasons.append("고금리 우선 추천")
        reason = ", ".join(reasons) if reasons else "기본 추천 상품"
        match_score = max(10, 100 - (rank - 1) * 15)
        recommendations.append({**row, "reason": reason, "match_score": match_score})

    return {"customer_id": customer_id, "recommendations": recommendations}


@app.get("/products/{product_id}", response_model=ProductDetailResponse)
def get_product(product_id: int, db: Session = Depends(get_db)):
    row = _one(
        db,
        """
        SELECT banking_product_id AS product_id,
               deposit_product_name AS product_name,
               deposit_product_type AS product_type,
               description,
               base_interest_rate,
               min_join_amount,
               max_join_amount,
               min_period_month,
               max_period_month,
               is_early_termination_allowed,
               is_tax_benefit_available,
               deposit_product_status AS product_status
          FROM deposit_banking_products
         WHERE banking_product_id = :pid
        """,
        {"pid": product_id},
    )
    if not row:
        raise HTTPException(status_code=404, detail="상품을 찾을 수 없습니다.")

    rates = _rows(
        db,
        """
        SELECT rate_id,
               rate_type,
               minimum_contract_period,
               maximum_contract_period,
               rate,
               condition_description
          FROM banking_deposit_product_interest_rates
         WHERE banking_product_id = :pid
         ORDER BY rate_id
        """,
        {"pid": product_id},
    )
    return {**row, "interest_rates": rates}


# ── 계약 ──────────────────────────────────────────────────────────────────────

@app.post("/contracts", response_model=ContractResponse, status_code=201)
async def create_contract(req: ContractCreateRequest, db: Session = Depends(get_db)):
    product = _one(
        db,
        """
        SELECT banking_product_id, base_interest_rate, deposit_product_status
          FROM deposit_banking_products
         WHERE banking_product_id = :pid
        """,
        {"pid": req.banking_product_id},
    )
    if not product:
        raise HTTPException(status_code=404, detail="상품을 찾을 수 없습니다.")
    if product["deposit_product_status"] not in ("SELLING", None):
        raise HTTPException(status_code=422, detail="판매 중인 상품만 가입 가능합니다.")

    started = date.today()
    maturity = started + timedelta(days=req.period_months * 30)
    contract_number = f"CTR-{uuid4().hex[:8].upper()}"
    rate = float(product["base_interest_rate"] or 0)

    db.execute(
        text("""
            INSERT INTO deposit_contracts
                (contract_number, customer_id, banking_product_id,
                 join_amount, contract_interest_rate,
                 started_at, maturity_at, contract_status)
            VALUES
                (:cno, :cid, :pid,
                 :amt, :rate,
                 :started, :maturity, 'ACTIVE')
        """),
        {
            "cno": contract_number,
            "cid": req.customer_id,
            "pid": req.banking_product_id,
            "amt": req.join_amount,
            "rate": rate,
            "started": started.strftime("%Y%m%d"),
            "maturity": maturity.strftime("%Y%m%d"),
        },
    )
    db.commit()

    created = _one(
        db,
        """
        SELECT contract_id, contract_number, customer_id,
               banking_product_id, join_amount, contract_interest_rate,
               started_at, maturity_at, contract_status
          FROM deposit_contracts
         WHERE contract_number = :cno
        """,
        {"cno": contract_number},
    )

    # Kafka 이벤트 발행 — consultation-service 등 다른 서비스에 계약 완료 알림
    await _kafka.publish_contract_created(
        contract_id=created["contract_id"],
        contract_number=created["contract_number"],
        customer_id=created["customer_id"],
        product_id=created["banking_product_id"],
        join_amount=float(created["join_amount"]),
        interest_rate=float(created.get("contract_interest_rate") or 0),
    )

    return created


# ── 계좌 ──────────────────────────────────────────────────────────────────────

@app.get("/accounts", response_model=list[AccountResponse])
def list_accounts(
    customer_id: str = Query(...),
    db: Session = Depends(get_db),
):
    rows = _rows(
        db,
        """
        SELECT account_id, account_number, customer_id,
               account_type, account_alias, balance,
               currency, account_status, opened_at
          FROM deposit_accounts
         WHERE customer_id = :cid
         ORDER BY account_id
        """,
        {"cid": customer_id},
    )
    return rows


@app.get("/accounts/{account_id}/transactions", response_model=list[TransactionItem])
def list_transactions(
    account_id: int,
    start_date: str | None = Query(None),
    end_date: str | None = Query(None),
    db: Session = Depends(get_db),
):
    account = _one(
        db,
        "SELECT account_id FROM deposit_accounts WHERE account_id = :aid",
        {"aid": account_id},
    )
    if not account:
        raise HTTPException(status_code=404, detail="계좌를 찾을 수 없습니다.")

    conditions = ["account_id = :aid"]
    params: dict = {"aid": account_id}
    if start_date:
        conditions.append("created_at >= :start_date")
        params["start_date"] = start_date
    if end_date:
        conditions.append("created_at <= :end_date")
        params["end_date"] = end_date

    where = " AND ".join(conditions)
    rows = _rows(
        db,
        f"""
        SELECT transaction_id, transaction_number, account_id,
               transaction_type, status, amount,
               REPLACE(created_at, '-', '') AS created_at
          FROM deposit_transactions
         WHERE {where}
         ORDER BY created_at DESC, transaction_id DESC
        """,
        params,
    )
    return rows


@app.get("/accounts/{account_id}/cash-flow", response_model=CashFlowResponse)
def cash_flow(
    account_id: int,
    start_date: str | None = Query(None),
    end_date: str | None = Query(None),
    db: Session = Depends(get_db),
):
    account = _one(
        db,
        "SELECT account_id FROM deposit_accounts WHERE account_id = :aid",
        {"aid": account_id},
    )
    if not account:
        raise HTTPException(status_code=404, detail="계좌를 찾을 수 없습니다.")

    conditions = ["account_id = :aid", "status = 'COMPLETED'"]
    params: dict = {"aid": account_id}
    if start_date:
        conditions.append("created_at >= :start_date")
        params["start_date"] = start_date
    if end_date:
        conditions.append("created_at <= :end_date")
        params["end_date"] = end_date

    where = " AND ".join(conditions)
    rows = _rows(
        db,
        f"""
        SELECT transaction_id, transaction_number, account_id,
               transaction_type, status, amount,
               REPLACE(created_at, '-', '') AS created_at
          FROM deposit_transactions
         WHERE {where}
         ORDER BY created_at ASC, transaction_id ASC
        """,
        params,
    )

    inflow = sum(
        float(r["amount"] or 0) for r in rows if r["transaction_type"] == "DEPOSIT"
    )
    outflow = sum(
        float(r["amount"] or 0)
        for r in rows
        if r["transaction_type"] in ("WITHDRAWAL", "TRANSFER")
    )

    return {
        "account_id": account_id,
        "period_start": start_date,
        "period_end": end_date,
        "total_inflow": inflow,
        "total_outflow": outflow,
        "net_flow": inflow - outflow,
        "transaction_count": len(rows),
        "transactions": rows,
    }


# ── 특약 ──────────────────────────────────────────────────────────────────────

@app.post(
    "/contracts/{contract_id}/special-terms",
    response_model=SpecialTermAgreementResponse,
    status_code=201,
)
def agree_special_terms(
    contract_id: int,
    req: SpecialTermAgreementRequest,
    db: Session = Depends(get_db),
):
    contract = _one(
        db,
        "SELECT contract_id FROM deposit_contracts WHERE contract_id = :cid",
        {"cid": contract_id},
    )
    if not contract:
        raise HTTPException(status_code=404, detail="계약을 찾을 수 없습니다.")

    if not req.special_term_ids:
        raise HTTPException(status_code=422, detail="동의할 특약 ID가 없습니다.")

    agreed_at = req.agreed_at or date.today().strftime("%Y%m%d")
    agreed_ids: list[int] = []

    for tid in req.special_term_ids:
        term = _one(
            db,
            "SELECT special_term_id FROM deposit_special_terms WHERE special_term_id = :tid",
            {"tid": tid},
        )
        if not term:
            raise HTTPException(status_code=404, detail=f"특약 {tid}을(를) 찾을 수 없습니다.")

        db.execute(
            text("""
                INSERT INTO deposit_contract_special_term_agreements
                    (contract_id, special_term_id, agreed_at, is_agreed)
                VALUES (:cid, :tid, :agreed_at, 1)
            """),
            {"cid": contract_id, "tid": tid, "agreed_at": agreed_at},
        )
        agreed_ids.append(tid)

    db.commit()
    return {"contract_id": contract_id, "agreed_term_ids": agreed_ids, "agreed_at": agreed_at}


# ── 금리 적용 내역 ────────────────────────────────────────────────────────────

@app.get("/contracts/{contract_id}/applied-rates", response_model=list[AppliedRateItem])
def applied_rates(contract_id: int, db: Session = Depends(get_db)):
    contract = _one(
        db,
        "SELECT contract_id FROM deposit_contracts WHERE contract_id = :cid",
        {"cid": contract_id},
    )
    if not contract:
        raise HTTPException(status_code=404, detail="계약을 찾을 수 없습니다.")

    rows = _rows(
        db,
        """
        SELECT interest_id,
               contract_id,
               account_id,
               applied_interest_rate,
               interest_amount,
               interest_after_tax,
               interest_paid_at
          FROM deposit_interest_history
         WHERE contract_id = :cid
         ORDER BY interest_paid_at ASC, interest_id ASC
        """,
        {"cid": contract_id},
    )
    return rows

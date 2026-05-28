"""
수신 시스템 FastAPI + SQLite 통합 테스트.

커버 범위:
  1. 상품 조회         — GET  /products
  2. 상품 상세 조회    — GET  /products/{id}
  3. 계약 생성         — POST /contracts
  4. 계좌 조회         — GET  /accounts
  5. 거래내역 조회     — GET  /accounts/{id}/transactions
  6. 특약 동의         — POST /contracts/{id}/special-terms
  7. 금리 적용 내역    — GET  /contracts/{id}/applied-rates
  8. 현금흐름 분석     — GET  /accounts/{id}/cash-flow
  9. 에이전트 추천     — GET  /products/recommend
"""

import pytest


# ══════════════════════════════════════════════════════════════════════════════
# 1. 상품 조회  GET /products
# ══════════════════════════════════════════════════════════════════════════════

class TestListProducts:
    def test_status_200(self, client):
        assert client.get("/products").status_code == 200

    def test_returns_list(self, client):
        body = client.get("/products").json()
        assert isinstance(body, list)

    def test_seeded_product_present(self, client):
        names = [p["product_name"] for p in client.get("/products").json()]
        assert "정기예금 플러스" in names

    def test_required_fields_present(self, client):
        product = client.get("/products").json()[0]
        for field in (
            "product_id", "product_name", "product_type",
            "base_interest_rate", "min_join_amount", "max_join_amount",
            "min_period_month", "max_period_month", "product_status",
        ):
            assert field in product, f"필드 누락: {field}"

    def test_seeded_interest_rate(self, client):
        product = next(
            p for p in client.get("/products").json()
            if p["product_name"] == "정기예금 플러스"
        )
        assert float(product["base_interest_rate"]) == pytest.approx(3.5)

    def test_seeded_status_is_selling(self, client):
        product = client.get("/products").json()[0]
        assert product["product_status"] == "SELLING"

    def test_filter_by_type(self, rich_client):
        body = rich_client.get("/products", params={"product_type": "SAVINGS"}).json()
        assert all(p["product_type"] == "SAVINGS" for p in body)
        assert len(body) >= 1

    def test_filter_by_status_selling(self, rich_client):
        body = rich_client.get("/products", params={"status": "SELLING"}).json()
        assert all(p["product_status"] == "SELLING" for p in body)

    def test_filter_by_status_closed_excludes_selling(self, rich_client):
        body = rich_client.get("/products", params={"status": "CLOSED"}).json()
        assert all(p["product_status"] == "CLOSED" for p in body)

    def test_empty_db_returns_empty_list(self, empty_client):
        assert empty_client.get("/products").json() == []

    def test_seeded_join_amount_range(self, client):
        product = client.get("/products").json()[0]
        assert float(product["min_join_amount"]) == pytest.approx(100_000)
        assert float(product["max_join_amount"]) == pytest.approx(100_000_000)

    def test_seeded_period_range(self, client):
        product = client.get("/products").json()[0]
        assert product["min_period_month"] == 1
        assert product["max_period_month"] == 60


# ══════════════════════════════════════════════════════════════════════════════
# 2. 상품 상세 조회  GET /products/{product_id}
# ══════════════════════════════════════════════════════════════════════════════

class TestGetProduct:
    def test_status_200_for_existing(self, client):
        assert client.get("/products/1").status_code == 200

    def test_404_for_nonexistent(self, client):
        assert client.get("/products/999").status_code == 404

    def test_product_id_matches(self, client):
        assert client.get("/products/1").json()["product_id"] == 1

    def test_product_name_correct(self, client):
        assert client.get("/products/1").json()["product_name"] == "정기예금 플러스"

    def test_interest_rates_field_present(self, client):
        body = client.get("/products/1").json()
        assert "interest_rates" in body
        assert isinstance(body["interest_rates"], list)

    def test_interest_rates_not_empty(self, client):
        assert len(client.get("/products/1").json()["interest_rates"]) >= 1

    def test_interest_rate_fields(self, client):
        rate = client.get("/products/1").json()["interest_rates"][0]
        for field in ("rate_id", "rate_type", "rate", "minimum_contract_period", "maximum_contract_period"):
            assert field in rate, f"금리 필드 누락: {field}"

    def test_base_rate_value(self, client):
        rates = client.get("/products/1").json()["interest_rates"]
        base = next(r for r in rates if r["rate_type"] == "BASE")
        assert float(base["rate"]) == pytest.approx(3.5)

    def test_preferential_rate_present(self, client):
        rates = client.get("/products/1").json()["interest_rates"]
        types = [r["rate_type"] for r in rates]
        assert "PREFERENTIAL" in types

    def test_description_field(self, client):
        body = client.get("/products/1").json()
        assert "description" in body

    def test_multiple_products_distinct(self, rich_client):
        p1 = rich_client.get("/products/1").json()
        p2 = rich_client.get("/products/2").json()
        assert p1["product_id"] != p2["product_id"]
        assert p1["product_name"] != p2["product_name"]


# ══════════════════════════════════════════════════════════════════════════════
# 3. 계약 생성  POST /contracts
# ══════════════════════════════════════════════════════════════════════════════

class TestCreateContract:
    _payload = {
        "customer_id": "CUST099",
        "banking_product_id": 1,
        "join_amount": 3_000_000,
        "period_months": 12,
    }

    def test_status_201(self, client):
        assert client.post("/contracts", json=self._payload).status_code == 201

    def test_returns_contract_id(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert "contract_id" in body
        assert isinstance(body["contract_id"], int)

    def test_contract_number_generated(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["contract_number"].startswith("CTR-")

    def test_customer_id_matches(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["customer_id"] == "CUST099"

    def test_join_amount_matches(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert float(body["join_amount"]) == pytest.approx(3_000_000)

    def test_interest_rate_taken_from_product(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert float(body["contract_interest_rate"]) == pytest.approx(3.5)

    def test_status_is_active(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["contract_status"] == "ACTIVE"

    def test_maturity_at_is_set(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["maturity_at"]
        assert len(body["maturity_at"]) == 8  # YYYYMMDD

    def test_started_at_is_set(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["started_at"]
        assert len(body["started_at"]) == 8

    def test_nonexistent_product_returns_404(self, client):
        assert (
            client.post(
                "/contracts",
                json={**self._payload, "banking_product_id": 999},
            ).status_code
            == 404
        )

    def test_two_contracts_have_different_numbers(self, client):
        no1 = client.post("/contracts", json=self._payload).json()["contract_number"]
        no2 = client.post("/contracts", json=self._payload).json()["contract_number"]
        assert no1 != no2

    def test_banking_product_id_stored(self, client):
        body = client.post("/contracts", json=self._payload).json()
        assert body["banking_product_id"] == 1


# ══════════════════════════════════════════════════════════════════════════════
# 4. 계좌 조회  GET /accounts
# ══════════════════════════════════════════════════════════════════════════════

class TestListAccounts:
    def test_status_200(self, client):
        assert client.get("/accounts", params={"customer_id": "CUST001"}).status_code == 200

    def test_returns_list(self, client):
        body = client.get("/accounts", params={"customer_id": "CUST001"}).json()
        assert isinstance(body, list)

    def test_seeded_account_present(self, client):
        body = client.get("/accounts", params={"customer_id": "CUST001"}).json()
        assert len(body) >= 1

    def test_required_fields(self, client):
        account = client.get("/accounts", params={"customer_id": "CUST001"}).json()[0]
        for field in (
            "account_id", "account_number", "customer_id",
            "balance", "currency", "account_status", "opened_at",
        ):
            assert field in account, f"필드 누락: {field}"

    def test_seeded_account_number(self, client):
        numbers = [
            a["account_number"]
            for a in client.get("/accounts", params={"customer_id": "CUST001"}).json()
        ]
        assert "001-123-000001" in numbers

    def test_customer_id_filtered(self, client):
        accounts = client.get("/accounts", params={"customer_id": "CUST001"}).json()
        assert all(a["customer_id"] == "CUST001" for a in accounts)

    def test_seeded_balance(self, client):
        account = client.get("/accounts", params={"customer_id": "CUST001"}).json()[0]
        assert float(account["balance"]) == pytest.approx(5_000_000)

    def test_seeded_currency_krw(self, client):
        account = client.get("/accounts", params={"customer_id": "CUST001"}).json()[0]
        assert account["currency"] == "KRW"

    def test_seeded_status_active(self, client):
        account = client.get("/accounts", params={"customer_id": "CUST001"}).json()[0]
        assert account["account_status"] == "ACTIVE"

    def test_unknown_customer_returns_empty(self, client):
        body = client.get("/accounts", params={"customer_id": "NO_SUCH"}).json()
        assert body == []

    def test_customer_isolation(self, rich_client):
        cust1 = rich_client.get("/accounts", params={"customer_id": "CUST001"}).json()
        cust2 = rich_client.get("/accounts", params={"customer_id": "CUST002"}).json()
        ids1 = {a["account_id"] for a in cust1}
        ids2 = {a["account_id"] for a in cust2}
        assert ids1.isdisjoint(ids2)

    def test_rich_cust001_has_two_accounts(self, rich_client):
        accounts = rich_client.get("/accounts", params={"customer_id": "CUST001"}).json()
        assert len(accounts) == 2

    def test_missing_customer_id_returns_422(self, client):
        assert client.get("/accounts").status_code == 422


# ══════════════════════════════════════════════════════════════════════════════
# 5. 거래내역 조회  GET /accounts/{account_id}/transactions
# ══════════════════════════════════════════════════════════════════════════════

class TestListTransactions:
    def test_status_200(self, client):
        assert client.get("/accounts/1/transactions").status_code == 200

    def test_404_for_nonexistent_account(self, client):
        assert client.get("/accounts/999/transactions").status_code == 404

    def test_returns_list(self, client):
        assert isinstance(client.get("/accounts/1/transactions").json(), list)

    def test_seeded_transactions_present(self, client):
        body = client.get("/accounts/1/transactions").json()
        assert len(body) >= 1

    def test_required_fields(self, client):
        tx = client.get("/accounts/1/transactions").json()[0]
        for field in ("transaction_id", "account_id", "transaction_type", "status", "amount", "created_at"):
            assert field in tx, f"필드 누락: {field}"

    def test_seeded_transaction_number(self, client):
        numbers = [t["transaction_number"] for t in client.get("/accounts/1/transactions").json()]
        assert "TX-001" in numbers

    def test_account_id_filtered(self, client):
        txs = client.get("/accounts/1/transactions").json()
        assert all(t["account_id"] == 1 for t in txs)

    def test_date_filter_start(self, client):
        body = client.get(
            "/accounts/1/transactions",
            params={"start_date": "2026-05-10"},
        ).json()
        assert all(t["created_at"] >= "2026-05-10" for t in body)

    def test_date_filter_end(self, client):
        body = client.get(
            "/accounts/1/transactions",
            params={"end_date": "2026-05-09"},
        ).json()
        assert all(t["created_at"] <= "20260509" for t in body)

    def test_date_filter_range_narrows(self, client):
        all_count = len(client.get("/accounts/1/transactions").json())
        ranged = client.get(
            "/accounts/1/transactions",
            params={"start_date": "2026-05-10", "end_date": "2026-05-10"},
        ).json()
        assert len(ranged) < all_count

    def test_various_transaction_types_seeded(self, client):
        types = {t["transaction_type"] for t in client.get("/accounts/1/transactions").json()}
        assert "DEPOSIT" in types
        assert "TRANSFER" in types

    def test_amount_is_numeric(self, client):
        for tx in client.get("/accounts/1/transactions").json():
            assert isinstance(tx["amount"], (int, float))


# ══════════════════════════════════════════════════════════════════════════════
# 6. 특약 동의  POST /contracts/{contract_id}/special-terms
# ══════════════════════════════════════════════════════════════════════════════

class TestAgreeSpecialTerms:
    def test_status_201(self, client):
        resp = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [1]},
        )
        assert resp.status_code == 201

    def test_returns_contract_id(self, client):
        body = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [1]},
        ).json()
        assert body["contract_id"] == 1

    def test_agreed_term_ids_returned(self, client):
        body = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [1, 2]},
        ).json()
        assert sorted(body["agreed_term_ids"]) == [1, 2]

    def test_agreed_at_generated_if_omitted(self, client):
        body = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [1]},
        ).json()
        assert body["agreed_at"]
        assert len(body["agreed_at"]) == 8  # YYYYMMDD

    def test_agreed_at_custom_date(self, client):
        body = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [1], "agreed_at": "20260522"},
        ).json()
        assert body["agreed_at"] == "20260522"

    def test_nonexistent_contract_returns_404(self, client):
        assert (
            client.post(
                "/contracts/999/special-terms",
                json={"special_term_ids": [1]},
            ).status_code
            == 404
        )

    def test_nonexistent_term_returns_404(self, client):
        assert (
            client.post(
                "/contracts/1/special-terms",
                json={"special_term_ids": [999]},
            ).status_code
            == 404
        )

    def test_empty_term_ids_returns_422(self, client):
        assert (
            client.post(
                "/contracts/1/special-terms",
                json={"special_term_ids": []},
            ).status_code
            == 422
        )

    def test_single_term_agreement(self, client):
        body = client.post(
            "/contracts/1/special-terms",
            json={"special_term_ids": [2]},
        ).json()
        assert body["agreed_term_ids"] == [2]

    def test_idempotent_second_agreement(self, client):
        payload = {"special_term_ids": [1]}
        client.post("/contracts/1/special-terms", json=payload)
        resp2 = client.post("/contracts/1/special-terms", json=payload)
        assert resp2.status_code == 201


# ══════════════════════════════════════════════════════════════════════════════
# 7. 금리 적용 내역 조회  GET /contracts/{contract_id}/applied-rates
# ══════════════════════════════════════════════════════════════════════════════

class TestAppliedRates:
    def test_status_200(self, client):
        assert client.get("/contracts/1/applied-rates").status_code == 200

    def test_404_for_nonexistent_contract(self, client):
        assert client.get("/contracts/999/applied-rates").status_code == 404

    def test_returns_list(self, client):
        assert isinstance(client.get("/contracts/1/applied-rates").json(), list)

    def test_seeded_record_present(self, client):
        assert len(client.get("/contracts/1/applied-rates").json()) >= 1

    def test_required_fields(self, client):
        item = client.get("/contracts/1/applied-rates").json()[0]
        for field in (
            "interest_id", "contract_id",
            "applied_interest_rate", "interest_amount",
            "interest_after_tax", "interest_paid_at",
        ):
            assert field in item, f"필드 누락: {field}"

    def test_applied_rate_value(self, client):
        item = client.get("/contracts/1/applied-rates").json()[0]
        assert float(item["applied_interest_rate"]) == pytest.approx(3.5)

    def test_interest_amount_value(self, client):
        item = client.get("/contracts/1/applied-rates").json()[0]
        assert float(item["interest_amount"]) == pytest.approx(175_000)

    def test_after_tax_amount(self, client):
        item = client.get("/contracts/1/applied-rates").json()[0]
        assert float(item["interest_after_tax"]) == pytest.approx(148_050)

    def test_paid_at_date(self, client):
        item = client.get("/contracts/1/applied-rates").json()[0]
        assert item["interest_paid_at"] == "20261231"

    def test_contract_id_matches(self, client):
        for item in client.get("/contracts/1/applied-rates").json():
            assert item["contract_id"] == 1

    def test_empty_db_returns_empty_list(self, empty_client):
        # 빈 DB에서는 계약이 없으므로 404 또는 빈 리스트
        resp = empty_client.get("/contracts/1/applied-rates")
        assert resp.status_code in (200, 404)

    def test_multiple_histories(self, rich_client):
        items = rich_client.get("/contracts/1/applied-rates").json()
        # rich DB에서 contract 1의 이자 내역이 1건 이상
        assert len(items) >= 1


# ══════════════════════════════════════════════════════════════════════════════
# 8. 현금흐름 분석  GET /accounts/{account_id}/cash-flow
# ══════════════════════════════════════════════════════════════════════════════

class TestCashFlow:
    def test_status_200(self, client):
        assert client.get("/accounts/1/cash-flow").status_code == 200

    def test_404_for_nonexistent_account(self, client):
        assert client.get("/accounts/999/cash-flow").status_code == 404

    def test_required_fields(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        for field in (
            "account_id", "total_inflow", "total_outflow",
            "net_flow", "transaction_count", "transactions",
        ):
            assert field in body, f"필드 누락: {field}"

    def test_account_id_matches(self, client):
        assert client.get("/accounts/1/cash-flow").json()["account_id"] == 1

    def test_inflow_counts_deposit_type(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        # TX-001: DEPOSIT 500,000 → inflow 에 포함돼야 함
        assert float(body["total_inflow"]) == pytest.approx(500_000)

    def test_outflow_counts_transfer_and_withdrawal(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        # TX-002 TRANSFER 100,000 COMPLETED + TX-003 WITHDRAWAL 50,000 PENDING(제외)
        # status='COMPLETED' 만 집계하므로 100,000
        assert float(body["total_outflow"]) == pytest.approx(100_000)

    def test_net_flow_is_inflow_minus_outflow(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        assert float(body["net_flow"]) == pytest.approx(
            float(body["total_inflow"]) - float(body["total_outflow"])
        )

    def test_transaction_count_matches_transactions_length(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        assert body["transaction_count"] == len(body["transactions"])

    def test_only_completed_transactions_counted(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        # PENDING TX-003 는 집계에서 제외
        statuses = {t["status"] for t in body["transactions"]}
        assert "PENDING" not in statuses

    def test_date_filter_start(self, client):
        body = client.get(
            "/accounts/1/cash-flow",
            params={"start_date": "2026-05-10"},
        ).json()
        assert all(t["created_at"] >= "2026-05-10" for t in body["transactions"])

    def test_date_filter_end(self, client):
        body = client.get(
            "/accounts/1/cash-flow",
            params={"end_date": "2026-05-05"},
        ).json()
        assert all(t["created_at"] <= "20260505" for t in body["transactions"])

    def test_period_start_end_reflected(self, client):
        body = client.get(
            "/accounts/1/cash-flow",
            params={"start_date": "2026-05-01", "end_date": "2026-05-31"},
        ).json()
        assert body["period_start"] == "2026-05-01"
        assert body["period_end"] == "2026-05-31"

    def test_no_transactions_when_out_of_range(self, client):
        body = client.get(
            "/accounts/1/cash-flow",
            params={"start_date": "2030-01-01"},
        ).json()
        assert body["transaction_count"] == 0
        assert body["total_inflow"] == pytest.approx(0)
        assert body["total_outflow"] == pytest.approx(0)
        assert body["net_flow"] == pytest.approx(0)

    def test_transactions_field_is_list(self, client):
        body = client.get("/accounts/1/cash-flow").json()
        assert isinstance(body["transactions"], list)

    def test_rich_net_flow_multiple_accounts(self, rich_client):
        # rich DB: 계좌1 DEPOSIT 500,000 COMPLETED, TRANSFER 100,000 COMPLETED
        # WITHDRAWAL 30,000 COMPLETED → outflow 130,000
        body = rich_client.get("/accounts/1/cash-flow").json()
        assert float(body["total_inflow"]) == pytest.approx(500_000)
        assert float(body["total_outflow"]) == pytest.approx(130_000)


# ══════════════════════════════════════════════════════════════════════════════
# 9. 에이전트 추천  GET /products/recommend
# ══════════════════════════════════════════════════════════════════════════════

class TestRecommendProducts:
    def test_status_200(self, client):
        assert client.get("/products/recommend").status_code == 200

    def test_returns_recommendations_field(self, client):
        body = client.get("/products/recommend").json()
        assert "recommendations" in body
        assert isinstance(body["recommendations"], list)

    def test_seeded_product_appears_in_recommendations(self, client):
        body = client.get("/products/recommend").json()
        assert len(body["recommendations"]) >= 1

    def test_recommendation_required_fields(self, client):
        item = client.get("/products/recommend").json()["recommendations"][0]
        for field in (
            "product_id", "product_name", "base_interest_rate",
            "reason", "match_score",
        ):
            assert field in item, f"필드 누락: {field}"

    def test_customer_id_reflected(self, client):
        body = client.get(
            "/products/recommend",
            params={"customer_id": "CUST001"},
        ).json()
        assert body["customer_id"] == "CUST001"

    def test_amount_filter_excludes_out_of_range(self, rich_client):
        # 청약(상품3) max_join_amount=1,000,000 → available_amount=5,000,000 이면 제외
        body = rich_client.get(
            "/products/recommend",
            params={"available_amount": 5_000_000},
        ).json()
        names = [r["product_name"] for r in body["recommendations"]]
        assert "주택청약종합저축" not in names

    def test_period_filter_excludes_short_products(self, rich_client):
        # 상품1 max_period_month=60, 상품2 max_period_month=36 → 48개월 요청 시 상품2 제외
        body = rich_client.get(
            "/products/recommend",
            params={"preferred_period_months": 48},
        ).json()
        names = [r["product_name"] for r in body["recommendations"]]
        assert "자유적금" not in names

    def test_prefer_high_rate_sorts_by_rate_desc(self, rich_client):
        body = rich_client.get(
            "/products/recommend",
            params={"prefer_high_rate": "true"},
        ).json()
        rates = [r["base_interest_rate"] for r in body["recommendations"]]
        assert rates == sorted(rates, reverse=True)

    def test_prefer_low_rate_sorts_asc(self, rich_client):
        body = rich_client.get(
            "/products/recommend",
            params={"prefer_high_rate": "false"},
        ).json()
        rates = [r["base_interest_rate"] for r in body["recommendations"]]
        assert rates == sorted(rates)

    def test_match_score_is_integer(self, client):
        for item in client.get("/products/recommend").json()["recommendations"]:
            assert isinstance(item["match_score"], int)

    def test_match_score_range(self, rich_client):
        for item in rich_client.get("/products/recommend").json()["recommendations"]:
            assert 0 <= item["match_score"] <= 100

    def test_reason_is_nonempty_string(self, client):
        for item in client.get("/products/recommend").json()["recommendations"]:
            assert isinstance(item["reason"], str)
            assert len(item["reason"]) > 0

    def test_empty_db_returns_empty_recommendations(self, empty_client):
        body = empty_client.get("/products/recommend").json()
        assert body["recommendations"] == []

    def test_max_recommendations_capped_at_five(self, rich_client):
        body = rich_client.get("/products/recommend").json()
        assert len(body["recommendations"]) <= 5

    def test_closed_product_excluded(self, rich_client):
        # 상품4 '구 정기예금' 은 CLOSED → 추천 결과에 없어야 함
        body = rich_client.get("/products/recommend").json()
        names = [r["product_name"] for r in body["recommendations"]]
        assert "구 정기예금" not in names

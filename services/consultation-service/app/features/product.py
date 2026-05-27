"""상품 관련 feature executor.

담당 feature: PRODUCT_GUIDE, RATE_GUIDE, JOIN_CONDITION,
              PRODUCT_COMPARE, TERMS_RAG, FAQ
"""
from __future__ import annotations

from typing import Any

from app.features.base import FeatureExecutorBase
from app.rag import ProductRagEngine
from app.schemas import ChatbotFeatureExecuteRequest, ChatbotFeatureExecuteResponse


class ProductFeatureExecutor(FeatureExecutorBase):

    # ── PRODUCT_GUIDE ─────────────────────────────────────────────────────────

    def execute_product_guide(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        """상품 추천.

        우선순위:
          1. RAG + 현금흐름: customer_no 있고 RAG 준비됨 → 현금흐름 쿼리로 RAG 검색
          2. RAG 단독: customer_no 없고 RAG 준비됨 → query 텍스트로 RAG 검색
          3. Fallback: RAG 없음 → DB 전체 상품 목록
        """
        # ── 경로 1: RAG + 현금흐름 기반 개인화 추천 ───────────────────────────
        if self._rag and self._rag.is_ready() and request.customer_no:
            cf = self._analyze_customer_cash_flow(request.customer_no)
            if cf and cf["has_data"]:
                query = ProductRagEngine.build_cashflow_query(cf)
                rag_results = self._rag.search(query, top_k=5, doc_type="product")
                rows = self._enrich_rag_results(rag_results, cf)
                msg = "고객님의 거래 패턴과 상품 내용을 분석해 맞춤 상품을 추천해 드립니다."
                return self._data_response("PRODUCT_GUIDE", rows, msg, "등록된 수신 상품 데이터가 없습니다.")

        # ── 경로 2: RAG 단독 (쿼리 텍스트 기반) ──────────────────────────────
        if self._rag and self._rag.is_ready():
            query = request.query or "수신 상품 추천"
            rag_results = self._rag.search(query, top_k=5, doc_type="product")
            rows = self._enrich_rag_results(rag_results, cf=None)
            msg = "질문과 관련된 상품을 찾았습니다."
            return self._data_response("PRODUCT_GUIDE", rows, msg, "등록된 수신 상품 데이터가 없습니다.")

        # ── 경로 3: RAG 없음 → DB 전체 목록 ──────────────────────────────────
        rows = self._rows(
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
                   deposit_product_status AS product_status
              FROM deposit_banking_products
             WHERE deposit_product_status = 'SELLING'
             ORDER BY banking_product_id
             LIMIT 20
            """
        )
        return self._data_response("PRODUCT_GUIDE", rows, "상품 안내 조회를 완료했습니다.", "등록된 수신 상품 데이터가 없습니다.")

    # ── RATE_GUIDE ────────────────────────────────────────────────────────────

    def execute_rate_guide(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        rows = self._rows(
            """
            SELECT r.rate_id,
                   r.banking_product_id AS product_id,
                   p.deposit_product_name AS product_name,
                   r.rate_type,
                   r.minimum_contract_period,
                   r.maximum_contract_period,
                   r.rate AS interest_rate,
                   r.condition_description
              FROM banking_deposit_product_interest_rates r
              JOIN deposit_banking_products p ON p.banking_product_id = r.banking_product_id
             ORDER BY r.banking_product_id, r.rate_id
             LIMIT 20
            """
        )
        return self._data_response("RATE_GUIDE", rows, "금리/우대금리 조회를 완료했습니다.", "등록된 금리 데이터가 없습니다.")

    # ── JOIN_CONDITION ────────────────────────────────────────────────────────

    def execute_join_condition(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        rows = self._rows(
            """
            SELECT banking_product_id AS product_id,
                   deposit_product_name AS product_name,
                   min_join_amount,
                   max_join_amount,
                   min_period_month,
                   max_period_month,
                   is_early_termination_allowed,
                   is_tax_benefit_available,
                   deposit_product_status AS product_status
              FROM deposit_banking_products
             ORDER BY banking_product_id
             LIMIT 20
            """
        )
        return self._data_response("JOIN_CONDITION", rows, "가입 조건 조회를 완료했습니다.", "등록된 가입 조건 데이터가 없습니다.")

    # ── PRODUCT_COMPARE ───────────────────────────────────────────────────────

    def execute_product_compare(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        product_ids = request.compare_product_ids or ([request.product_id] if request.product_id else [])
        if product_ids:
            rows = self._rows(
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
                 WHERE banking_product_id IN :product_ids
                 ORDER BY banking_product_id
                """,
                {"product_ids": tuple(product_ids)},
                expanding_params=("product_ids",),
            )
        else:
            rows = self._rows(
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
                 ORDER BY base_interest_rate DESC, banking_product_id
                 LIMIT 5
                """
            )
        return self._data_response("PRODUCT_COMPARE", rows, "상품 비교 조회를 완료했습니다.", "비교할 상품 데이터가 없습니다.")

    # ── TERMS_RAG ─────────────────────────────────────────────────────────────

    def execute_terms_search(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        query = (request.query or "").strip()

        # RAG 준비됐으면 의미 기반 검색 우선
        if self._rag and self._rag.is_ready() and query:
            rag_results = self._rag.search(query, top_k=5, doc_type="term")
            if rag_results:
                rows = [{k: v for k, v in r.items() if not k.startswith("_")} for r in rag_results]
                return self._data_response("TERMS_RAG", rows, "관련 약관을 찾았습니다.", "검색 가능한 약관 데이터가 없습니다.")

        # Fallback: SQL LIKE 검색 (빈 쿼리 시 "%" → 전체 반환)
        # %, _, \ 이스케이프해 사용자 입력이 와일드카드로 동작하지 않도록 방지
        if query:
            escaped = query.replace("\\", "\\\\").replace("%", r"\%").replace("_", r"\_")
            like = f"%{escaped}%"
        else:
            like = "%"
        rows = self._rows(
            r"""
            SELECT special_term_id,
                   special_term_name,
                   special_term_content,
                   special_term_summary,
                   is_required,
                   status
              FROM deposit_special_terms
             WHERE special_term_name LIKE :query ESCAPE '\'
                OR special_term_content LIKE :query ESCAPE '\'
                OR special_term_summary LIKE :query ESCAPE '\'
             ORDER BY special_term_id
             LIMIT 10
            """,
            {"query": like},
        )
        return self._data_response("TERMS_RAG", rows, "약관 검색을 완료했습니다.", "검색 가능한 약관 데이터가 없습니다.")

    # ── FAQ ───────────────────────────────────────────────────────────────────

    def execute_faq(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        return ChatbotFeatureExecuteResponse(
            feature_code="FAQ",
            status="OK",
            message="수신 상품 FAQ 응답입니다.",
            data=[
                {"question": "예금과 적금의 차이는 무엇인가요?", "answer": "예금은 목돈을 맡기고, 적금은 정해진 주기로 납입하는 상품입니다."},
                {"question": "우대금리는 어떻게 적용되나요?", "answer": "상품별 우대 조건 충족 여부에 따라 기본금리에 추가됩니다."},
                {"question": "중도해지하면 어떻게 되나요?", "answer": "상품 약관의 중도해지이율이 적용될 수 있어 약관 확인이 필요합니다."},
            ],
        )

    # ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    def _enrich_rag_results(
        self, rag_results: list[dict[str, Any]], cf: dict[str, Any] | None
    ) -> list[dict[str, Any]]:
        """RAG 검색 결과에 추천 이유와 match_score 를 추가한다."""
        enriched = []
        for rank, r in enumerate(rag_results, start=1):
            reasons: list[str] = []

            ptype     = r.get("deposit_product_type") or r.get("product_type", "")
            min_amt   = float(r.get("min_join_amount") or 0)
            rate      = float(r.get("base_interest_rate") or 0)
            rag_score = float(r.get("_score", 0))

            if cf:
                total_balance   = cf.get("total_balance", 0)
                monthly_surplus = cf.get("monthly_surplus", 0)

                if ptype == "DEPOSIT" and total_balance >= min_amt:
                    reasons.append(f"보유 잔액({total_balance:,.0f}원)으로 가입 가능")
                if ptype == "SAVINGS" and monthly_surplus >= min_amt:
                    reasons.append(f"월 여유자금({monthly_surplus:,.0f}원)으로 납입 가능")
                if rate >= 3.5:
                    reasons.append("고금리 혜택")

            if not reasons:
                reasons.append(f"질문과 {rag_score:.0%} 유사도로 매칭된 상품")

            match_score = max(10, round(rag_score * 100) - (rank - 1) * 3)
            enriched.append({
                **{k: v for k, v in r.items() if not k.startswith("_")},
                "recommend_reason": ", ".join(reasons),
                "match_score":      match_score,
            })
        return enriched

"""사용자 금융정보 feature executor.

담당 feature: MY_ACCOUNTS, MY_PRODUCTS, CONTRACT_STATUS,
              MATURITY_SCHEDULE, INTEREST_HISTORY, MY_CASH_FLOW, CASH_FLOW_RECOMMEND
"""
from __future__ import annotations

from typing import Any

from app.features.base import FeatureExecutorBase
from app.schemas import ChatbotFeatureExecuteRequest, ChatbotFeatureExecuteResponse


class UserFinanceFeatureExecutor(FeatureExecutorBase):

    # ── MY_ACCOUNTS ───────────────────────────────────────────────────────────

    def execute_my_accounts(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("MY_ACCOUNTS", "계좌 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._account_rows(request.customer_no)
        return self._data_response(
            "MY_ACCOUNTS", rows, "내 계좌 조회를 완료했습니다.", "조회된 계좌가 없습니다.", requires_auth=True
        )

    # ── MATURITY_SCHEDULE ─────────────────────────────────────────────────────

    def execute_maturity_schedule(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("MATURITY_SCHEDULE", "만기 예정 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._contract_rows(request.customer_no)
        return self._data_response(
            "MATURITY_SCHEDULE", rows, "만기 예정 조회를 완료했습니다.", "조회된 만기 예정 계약이 없습니다.", requires_auth=True
        )

    # ── INTEREST_HISTORY ──────────────────────────────────────────────────────

    def execute_interest_history(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("INTEREST_HISTORY", "이자 내역 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._rows(
            """
            SELECT h.interest_id,
                   h.contract_id,
                   h.account_id,
                   h.applied_interest_rate,
                   h.interest_amount,
                   h.interest_after_tax AS interest_after_tax_amount,
                   h.interest_paid_at AS paid_at
              FROM deposit_interest_history h
              JOIN deposit_accounts a ON a.account_id = h.account_id
             WHERE a.customer_id = :customer_no
             ORDER BY h.interest_id DESC
             LIMIT 20
            """,
            {"customer_no": request.customer_no},
        )
        return self._data_response(
            "INTEREST_HISTORY", rows, "이자 내역 조회를 완료했습니다.", "조회된 이자 내역이 없습니다.", requires_auth=True
        )

    # ── MY_CASH_FLOW ──────────────────────────────────────────────────────────

    def execute_my_cash_flow(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("MY_CASH_FLOW", "현금 흐름 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._rows(
            """
            SELECT t.transaction_id,
                   a.account_number,
                   t.transaction_type,
                   t.amount,
                   t.transaction_status,
                   t.transaction_at
              FROM deposit_transactions t
              JOIN deposit_accounts a ON a.account_id = t.account_id
             WHERE a.customer_id = :customer_no
             ORDER BY t.transaction_at DESC
             LIMIT 20
            """,
            {"customer_no": request.customer_no},
        )
        return self._data_response(
            "MY_CASH_FLOW", rows, "현금 흐름 조회를 완료했습니다.", "조회된 거래 내역이 없습니다.", requires_auth=True
        )

    # ── CASH_FLOW_RECOMMEND ───────────────────────────────────────────────────

    def execute_cash_flow_recommend(
        self, request: ChatbotFeatureExecuteRequest
    ) -> ChatbotFeatureExecuteResponse:
        """현금흐름 분석 → LLM 기반 개인화 상품 추천.

        흐름:
          1. customer_no 인증 확인
          2. 현금흐름 분석 (잔액·월 잉여자금·거래 빈도)
          3. 판매 중인 수신 상품 전체 조회 → LLM 컨텍스트로 전달
          4. 대화 이력 → LLM 컨텍스트로 전달
          5. LlmAdapter.recommend() 호출 → 개인화 추천 생성
          6. LLM 미연결 시 룰 기반 fallback 추천
        """
        if not request.customer_no:
            return self._auth_required(
                "CASH_FLOW_RECOMMEND",
                "현금흐름 분석 추천에는 고객번호와 본인 인증이 필요합니다.",
            )

        # ── 1. 현금흐름 분석 ──────────────────────────────────────────────────
        cf = self._analyze_customer_cash_flow(request.customer_no)
        if cf is None:
            # 계좌 정보 없을 때 기본 현금흐름 값으로 대체
            cf = {
                "total_balance": 0,
                "monthly_surplus": 0,
                "monthly_tx_count": 0,
                "has_data": False,
            }

        # ── 2. 판매 중인 수신 상품 목록 (LLM 컨텍스트용) ──────────────────────
        products = self._rows(
            """
            SELECT banking_product_id AS product_id,
                   deposit_product_name,
                   deposit_product_type,
                   base_interest_rate,
                   min_join_amount,
                   max_join_amount,
                   min_period_month,
                   max_period_month,
                   is_early_termination_allowed,
                   is_tax_benefit_available
              FROM deposit_banking_products
             WHERE deposit_product_status = 'SELLING'
             ORDER BY base_interest_rate DESC
             LIMIT 10
            """
        )

        # ── 3. LLM 추천 ───────────────────────────────────────────────────────
        if self._llm_adapter:
            history_ctx = (
                self._build_history_context(request.chatbot_consultation_id)
                if request.chatbot_consultation_id
                else ""
            )
            try:
                recommendation = self._llm_adapter.recommend(
                    cash_flow=cf,
                    products=products,
                    user_query=request.query or "내 현금 흐름에 맞는 상품을 추천해줘",
                    history_ctx=history_ctx,
                )
            except Exception:
                recommendation = (
                    "죄송합니다, 추천 생성 중 오류가 발생했습니다. "
                    "상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요."
                )
        else:
            recommendation = self._rule_based_recommend(cf, products)

        # 상위 3개 상품을 recommended_product 형식으로 data에 포함
        top3 = products[:3]
        product_data = [
            {
                "row_type":          "recommended_product",
                "rank":              i + 1,
                "product_name":      p.get("deposit_product_name", ""),
                "product_type":      p.get("deposit_product_type", ""),
                "base_interest_rate": p.get("base_interest_rate"),
                "min_period_month":  p.get("min_period_month"),
                "max_period_month":  p.get("max_period_month"),
                "target_groups":     "개인고객",
            }
            for i, p in enumerate(top3)
        ]

        return ChatbotFeatureExecuteResponse(
            feature_code="CASH_FLOW_RECOMMEND",
            status="OK",
            message=recommendation,
            data=product_data,
            requires_auth=True,
        )

    # ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    def _rule_based_recommend(
        self, cf: dict[str, Any], products: list[dict[str, Any]]
    ) -> str:
        """LLM 미연결 시 현금흐름 지표 기반 룰 추천 텍스트."""
        total_balance   = float(cf.get("total_balance", 0))
        monthly_surplus = float(cf.get("monthly_surplus", 0))
        has_data        = cf.get("has_data", False)

        lines: list[str] = ["[상품 추천]\n"]

        if not has_data:
            lines.append(
                "현재 인기 있는 수신 상품을 순위대로 추천해 드립니다."
            )
        elif total_balance >= 10_000_000:
            lines.append(
                f"총 잔액 {total_balance:,.0f}원 — "
                "목돈이 있어 정기예금 상품을 추천드립니다."
            )
        elif monthly_surplus >= 500_000:
            lines.append(
                f"월 잉여자금 {monthly_surplus:,.0f}원 — "
                "정기 적금 납입에 적합합니다."
            )
        elif monthly_surplus > 0:
            lines.append(
                f"월 잉여자금 {monthly_surplus:,.0f}원 — "
                "소액 자유적금 상품을 추천드립니다."
            )
        else:
            lines.append(
                "현재 잉여자금이 적습니다. "
                "부담이 적은 자유납입 적금을 추천드립니다."
            )

        top = [
            p for p in products
            if (
                (total_balance >= 10_000_000 and p.get("deposit_product_type") == "DEPOSIT")
                or (monthly_surplus >= 100_000 and p.get("deposit_product_type") in ("SAVINGS", "SUBSCRIPTION"))
                or True
            )
        ][:3]

        if top:
            for i, p in enumerate(top, 1):
                name = p.get("deposit_product_name") or p.get("product_name", "")
                rate = p.get("base_interest_rate", "")
                ptype = p.get("deposit_product_type", "")
                type_label = {"DEPOSIT": "예금", "SAVINGS": "적금", "SUBSCRIPTION": "청약"}.get(ptype, ptype)
                lines.append(f"{i}위. {name} ({type_label}) — 기본금리 {rate}%")

        lines.append("\n더 자세한 상담은 '상담원 연결'을 이용해 주세요.")
        return "\n".join(lines)

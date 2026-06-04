"""저축 목표 기반 상품 추천 feature.

멀티턴 흐름:
  1. 목표 금액·기간 파싱
  2. 추가 질문: 월 납입 가능액 또는 목돈 여부
  3. 답변 수신 → 상품 필터링 + 이자 계산
  4. 여러 상품 비교 + 추천 근거 생성
  5. 결과 제시

한계:
  - 세션 상태는 프로세스 메모리(_SESSION dict)에 저장됨.
  - 서버 재시작 시 진행 중인 모든 저축 목표 세션이 초기화됨.
  - 멀티 워커(uvicorn --workers N) 환경에서는 워커 간 세션 공유 불가.
    프로덕션 전환 시 Redis 등 외부 저장소로 교체 필요.
"""
from __future__ import annotations

import re
from typing import Any

from app.features.base import FeatureExecutorBase
from app.schemas import ChatbotFeatureExecuteRequest, ChatbotFeatureExecuteResponse

# ── 세션 상태 저장소 (프로세스 내 메모리) ─────────────────────────────────────
# key: chatbot_consultation_id (int)
# value: {
#   "stage": "ASKED_MONTHLY" | "DONE",
#   "goal_amount": float,
#   "goal_months": int,
# }
# 주의: 서버 재시작 시 세션 초기화됨. 멀티 워커 환경 미지원.
_SESSION: dict[int, dict[str, Any]] = {}


# ── 파서 ────────────────────────────────────────────────────────────────────

def _parse_amount(text: str) -> float | None:
    """'500만원', '500만', '5000000원' 등을 float으로 변환."""
    text = text.replace(",", "").replace(" ", "")
    m = re.search(r"(\d+(?:\.\d+)?)\s*억", text)
    if m:
        return float(m.group(1)) * 1_0000_0000
    m = re.search(r"(\d+(?:\.\d+)?)\s*만", text)
    if m:
        return float(m.group(1)) * 10_000
    m = re.search(r"(\d+(?:\.\d+)?)\s*천", text)
    if m:
        return float(m.group(1)) * 1_000
    m = re.search(r"(\d{4,})", text)
    if m:
        return float(m.group(1))
    return None


def _parse_months(text: str) -> int | None:
    """'1년', '6개월', '12개월', '3달' 등을 개월수(int)로 변환."""
    m = re.search(r"(\d+)\s*년", text)
    if m:
        return int(m.group(1)) * 12
    m = re.search(r"(\d+)\s*개월", text)
    if m:
        return int(m.group(1))
    m = re.search(r"(\d+)\s*달", text)
    if m:
        return int(m.group(1))
    return None


def _has_lump_sum_intent(text: str) -> bool:
    """목돈을 한 번에 넣겠다는 의도 감지."""
    return any(k in text for k in ["목돈", "한 번에", "한번에", "일시", "예치", "넣어두"])


def _period_str(months: int) -> str:
    years, rem = divmod(months, 12)
    if rem == 0:
        return f"{years}년"
    if years == 0:
        return f"{months}개월"
    return f"{years}년 {rem}개월"


# ── 이자 계산 ────────────────────────────────────────────────────────────────

def _calc_savings_maturity(monthly: float, annual_rate: float, months: int) -> float:
    """적금 만기 수령액 계산 (월복리 근사).

    공식: monthly × ((1 + r)^n - 1) / r × (1 + r)
    r = annual_rate / 100 / 12 (월 이율)
    n = months
    """
    r = annual_rate / 100 / 12
    if r == 0:
        return monthly * months
    return monthly * ((1 + r) ** months - 1) / r * (1 + r)


def _calc_deposit_maturity(principal: float, annual_rate: float, months: int) -> float:
    """예금 만기 수령액 계산 (단리).

    공식: principal × (1 + annual_rate/100 × months/12)
    """
    return principal * (1 + annual_rate / 100 * months / 12)


# ── Feature Executor ─────────────────────────────────────────────────────────

class SavingsGoalFeatureExecutor(FeatureExecutorBase):

    def execute_savings_goal(
        self, request: ChatbotFeatureExecuteRequest
    ) -> ChatbotFeatureExecuteResponse:
        cid = request.chatbot_consultation_id
        query = (request.query or "").strip()
        session = _SESSION.get(cid) if cid else None

        # ── 진행 중인 세션: 추가 질문 답변 수신 ──────────────────────────────
        if session and session.get("stage") == "ASKED_MONTHLY":
            return self._handle_payment_answer(cid, query, session)

        # ── 새 요청: 목표 파싱 ────────────────────────────────────────────────
        goal_amount = _parse_amount(query)
        goal_months = _parse_months(query)

        missing = []
        if goal_amount is None:
            missing.append("목표 금액")
        if goal_months is None:
            missing.append("기간")

        if missing:
            questions = {
                "목표 금액": "목표 금액이 얼마인지 알려주세요. (예: 500만원)",
                "기간": "기간은 얼마나 생각하고 계세요? (예: 1년, 6개월)",
            }
            ask = " 그리고 ".join(questions[k] for k in missing)
            return ChatbotFeatureExecuteResponse(
                feature_code="SAVINGS_GOAL",
                status="NEED_INFO",
                message=f"목표 달성을 도와드릴게요! {ask}",
                data=[],
            )

        # ── 금액·기간 확인 → 세션 저장 후 추가 질문 ─────────────────────────
        if cid:
            _SESSION[cid] = {
                "stage": "ASKED_MONTHLY",
                "goal_amount": goal_amount,
                "goal_months": goal_months,
            }

        return ChatbotFeatureExecuteResponse(
            feature_code="SAVINGS_GOAL",
            status="NEED_INFO",
            message=(
                f"{_period_str(goal_months)} 동안 {goal_amount:,.0f}원을 모으는 목표군요! 😊\n\n"
                f"더 정확한 상품을 추천해 드리기 위해 여쭤볼게요.\n\n"
                f"매달 얼마 정도 납입하실 수 있으세요?\n"
                f"(목돈을 한 번에 넣으실 계획이라면 '목돈 ○○○만원' 처럼 알려주세요.)"
            ),
            data=[],
        )

    def _handle_payment_answer(
        self, cid: int | None, query: str, session: dict
    ) -> ChatbotFeatureExecuteResponse:
        """월 납입액 또는 목돈 답변 처리."""
        goal_amount = session["goal_amount"]
        goal_months = session["goal_months"]

        is_lump = _has_lump_sum_intent(query)
        amount = _parse_amount(query)

        if amount is None:
            return ChatbotFeatureExecuteResponse(
                feature_code="SAVINGS_GOAL",
                status="NEED_INFO",
                message="금액을 인식하지 못했어요. 예를 들어 '월 30만원' 또는 '목돈 300만원' 처럼 알려주세요.",
                data=[],
            )

        if cid and cid in _SESSION:
            del _SESSION[cid]

        return self._recommend(
            goal_amount=goal_amount,
            goal_months=goal_months,
            monthly_payment=None if is_lump else amount,
            lump_sum=amount if is_lump else None,
            query=query,
        )

    def _recommend(
        self,
        goal_amount: float,
        goal_months: int,
        monthly_payment: float | None,
        lump_sum: float | None,
        query: str,
    ) -> ChatbotFeatureExecuteResponse:
        """상품 조회 → 이자 계산 → 비교 → 추천."""
        is_lump = lump_sum is not None
        ptype_filter = "'DEPOSIT'" if is_lump else "'SAVINGS', 'DEPOSIT'"

        # ── 실제 DB 조회 (기간 조건 포함) ─────────────────────────────────────
        products = self._rows(
            f"""
            SELECT banking_product_id AS product_id,
                   deposit_product_name AS product_name,
                   deposit_product_type AS product_type,
                   base_interest_rate,
                   min_join_amount,
                   max_join_amount,
                   min_period_month,
                   max_period_month,
                   is_tax_benefit_available
              FROM deposit_banking_products
             WHERE deposit_product_status = 'SELLING'
               AND deposit_product_type IN ({ptype_filter})
               AND (min_period_month IS NULL OR min_period_month <= :months)
               AND (max_period_month IS NULL OR max_period_month >= :months)
             ORDER BY base_interest_rate DESC NULLS LAST
             LIMIT 6
            """,
            {"months": goal_months},
        )

        # 기간 조건 일치 상품 없을 때 fallback (기간 조건 제거)
        if not products:
            products = self._rows(
                f"""
                SELECT banking_product_id AS product_id,
                       deposit_product_name AS product_name,
                       deposit_product_type AS product_type,
                       base_interest_rate,
                       min_join_amount,
                       max_join_amount,
                       min_period_month,
                       max_period_month,
                       is_tax_benefit_available
                  FROM deposit_banking_products
                 WHERE deposit_product_status = 'SELLING'
                   AND deposit_product_type IN ({ptype_filter})
                 ORDER BY base_interest_rate DESC NULLS LAST
                 LIMIT 6
                """
            )

        # ── 상품별 이자 계산 ──────────────────────────────────────────────────
        enriched: list[dict[str, Any]] = []
        for p in products:
            rate = float(p.get("base_interest_rate") or 0)
            ptype = p.get("product_type", "")
            name = p.get("product_name") or ""
            min_join = p.get("min_join_amount")
            max_join = p.get("max_join_amount")

            if is_lump or ptype == "DEPOSIT":
                principal = lump_sum or goal_amount
                maturity = _calc_deposit_maturity(principal, rate, goal_months)
                interest = maturity - principal
                req_monthly = None
            else:
                mp = monthly_payment or (goal_amount / goal_months)
                maturity = _calc_savings_maturity(mp, rate, goal_months)
                interest = maturity - mp * goal_months
                req_monthly = mp

            achievable = maturity >= goal_amount

            enriched.append({
                "product_id":        p.get("product_id"),
                "product_name":      name,
                "product_type":      ptype,
                "base_interest_rate": rate,
                "min_join_amount":   min_join,
                "max_join_amount":   max_join,
                "min_period_month":  p.get("min_period_month"),
                "max_period_month":  p.get("max_period_month"),
                "is_tax_benefit_available": p.get("is_tax_benefit_available"),
                "goal_amount":       goal_amount,
                "goal_months":       goal_months,
                "maturity_amount":   round(maturity),
                "interest_amount":   round(interest),
                "required_monthly":  round(req_monthly) if req_monthly else None,
                "achievable":        achievable,
            })

        # 달성 가능 우선, 금리 내림차순 정렬
        enriched.sort(key=lambda x: (not x["achievable"], -x["base_interest_rate"]))

        # ── 추천 메시지 생성 ──────────────────────────────────────────────────
        if self._llm_adapter:
            message = self._llm_recommend(
                goal_amount, goal_months, monthly_payment, lump_sum, enriched, query
            )
        else:
            message = self._rule_recommend(
                goal_amount, goal_months, monthly_payment, lump_sum, enriched
            )

        return ChatbotFeatureExecuteResponse(
            feature_code="SAVINGS_GOAL",
            status="OK",
            message=message,
            data=enriched,
        )

    def _llm_recommend(
        self,
        goal_amount: float,
        goal_months: int,
        monthly_payment: float | None,
        lump_sum: float | None,
        products: list[dict],
        user_query: str,
    ) -> str:
        product_lines = []
        for p in products[:4]:
            name = p.get("product_name") or "알 수 없음"
            ptype = "적금" if p.get("product_type") == "SAVINGS" else "예금"
            rate = p.get("base_interest_rate", 0)
            maturity = p.get("maturity_amount", 0)
            interest = p.get("interest_amount", 0)
            achievable = "✅ 달성가능" if p.get("achievable") else "❌ 목표 미달"
            if monthly_payment and p.get("product_type") == "SAVINGS":
                product_lines.append(
                    f"- [{ptype}] {name}: 금리 {rate}%, 월 {monthly_payment:,.0f}원 납입 시 "
                    f"만기 {maturity:,.0f}원 (이자 {interest:,.0f}원) {achievable}"
                )
            else:
                principal = lump_sum or goal_amount
                product_lines.append(
                    f"- [{ptype}] {name}: 금리 {rate}%, {principal:,.0f}원 예치 시 "
                    f"만기 {maturity:,.0f}원 (이자 {interest:,.0f}원) {achievable}"
                )

        payment_info = (
            f"- 월 납입 가능액: {monthly_payment:,.0f}원" if monthly_payment
            else f"- 예치 목돈: {lump_sum:,.0f}원"
        )

        context = (
            f"[고객 저축 목표]\n"
            f"- 목표 금액: {goal_amount:,.0f}원\n"
            f"- 목표 기간: {_period_str(goal_months)}\n"
            f"{payment_info}\n\n"
            f"[상품별 계산 결과]\n" + "\n".join(product_lines)
        )

        system_prompt = (
            "당신은 인터넷 뱅킹 금융 상담 AI입니다.\n"
            "고객의 저축 목표와 계산된 상품 비교 데이터를 바탕으로 최적 상품을 추천하세요.\n"
            "규칙:\n"
            "1. 목표 금액·기간·납입액을 언급하며 맞춤 분석임을 강조하세요.\n"
            "2. 달성 가능한 상품 위주로 추천하고, 달성 불가 상품은 이유를 설명하세요.\n"
            "3. 상품 2~3개를 비교하고 1개를 최종 추천하며 근거를 명확히 하세요.\n"
            "4. 답변은 한국어로, 친절하고 구체적으로 (500자 이내)"
        )

        try:
            from langfuse.openai import OpenAI
            client = OpenAI(api_key=self._llm_adapter.api_key)
            response = client.chat.completions.create(
                model=self._llm_adapter.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "system", "content": context},
                    {"role": "user", "content": user_query},
                ],
                max_tokens=600,
                temperature=0.3,
            )
            return response.choices[0].message.content.strip()
        except Exception:
            return self._rule_recommend(goal_amount, goal_months, monthly_payment, lump_sum, products)

    def _rule_recommend(
        self,
        goal_amount: float,
        goal_months: int,
        monthly_payment: float | None,
        lump_sum: float | None,
        products: list[dict],
    ) -> str:
        payment_str = (
            f"월 {monthly_payment:,.0f}원 납입" if monthly_payment
            else f"{lump_sum:,.0f}원 예치"
        )

        lines = [
            f"[저축 목표 분석 결과]",
            f"목표: {_period_str(goal_months)} 동안 {goal_amount:,.0f}원 ({payment_str})\n",
        ]

        achievable = [p for p in products if p.get("achievable")]
        not_achievable = [p for p in products if not p.get("achievable")]

        if achievable:
            lines.append("✅ 목표 달성 가능한 상품:")
            for p in achievable[:3]:
                name = p.get("product_name") or "알 수 없음"
                rate = p.get("base_interest_rate", 0)
                maturity = p.get("maturity_amount", 0)
                interest = p.get("interest_amount", 0)
                ptype = "적금" if p.get("product_type") == "SAVINGS" else "예금"
                lines.append(
                    f"  • [{ptype}] {name}\n"
                    f"    금리 {rate}% → 만기 {maturity:,.0f}원 (이자 {interest:,.0f}원)"
                )
            best = achievable[0]
            best_type = "적금" if best.get("product_type") == "SAVINGS" else "예금"
            lines.append(
                f"\n💡 추천: [{best_type}] {best.get('product_name') or ''} "
                f"(금리 {best.get('base_interest_rate', 0)}%)"
            )
        else:
            lines.append("⚠️ 현재 조건으로는 목표 달성이 어렵습니다.")
            if not_achievable:
                best = not_achievable[0]
                lines.append(
                    f"가장 가까운 상품: {best.get('product_name') or ''} "
                    f"→ 만기 {best.get('maturity_amount', 0):,.0f}원"
                )
            lines.append("납입액을 늘리거나 기간을 연장하면 달성 가능합니다.")

        lines.append("\n더 자세한 상담은 '상담사 연결'을 이용해 주세요.")
        return "\n".join(lines)

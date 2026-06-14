"""저축 목표 기반 상품 추천 feature.

멀티턴 흐름:
  1. 목표 금액·기간 파싱
  2. 추가 질문: 월 납입 가능액 또는 목돈 여부
     (로그인 고객은 실제 월 잉여자금 자동 제안)
  3. 답변 수신 → 상품 필터링 + 이자 계산
  4. 여러 상품 비교 + 추천 근거 생성 (GPT 또는 룰 기반)
  5. 결과 제시 + 월별 납입 계획표 데이터

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
#   "customer_no": str | None,
#   "monthly_surplus": float | None,  # 실제 고객 월 잉여자금
# }
# 주의: 서버 재시작 시 세션 초기화됨. 멀티 워커 환경 미지원.
_SESSION: dict[int, dict[str, Any]] = {}


# ── 파서 ────────────────────────────────────────────────────────────────────

def _normalize_korean_amount(text: str) -> str:
    """한글 숫자를 아라비아 숫자로 치환 (예: '백만원' → '100만원')."""
    _MAP = [
        ("구백", "900"), ("팔백", "800"), ("칠백", "700"), ("육백", "600"),
        ("오백", "500"), ("사백", "400"), ("삼백", "300"), ("이백", "200"), ("일백", "100"), ("백", "100"),
        ("구십", "90"), ("팔십", "80"), ("칠십", "70"), ("육십", "60"),
        ("오십", "50"), ("사십", "40"), ("삼십", "30"), ("이십", "20"), ("일십", "10"), ("십", "10"),
        ("구만", "9만"), ("팔만", "8만"), ("칠만", "7만"), ("육만", "6만"),
        ("오만", "5만"), ("사만", "4만"), ("삼만", "3만"), ("이만", "2만"), ("일만", "1만"),
        ("구천", "9천"), ("팔천", "8천"), ("칠천", "7천"), ("육천", "6천"),
        ("오천", "5천"), ("사천", "4천"), ("삼천", "3천"), ("이천", "2천"), ("일천", "1천"),
        ("구억", "9억"), ("팔억", "8억"), ("칠억", "7억"), ("육억", "6억"),
        ("오억", "5억"), ("사억", "4억"), ("삼억", "3억"), ("이억", "2억"), ("일억", "1억"),
    ]
    for k, v in _MAP:
        text = text.replace(k, v)
    return text


def _parse_amount(text: str) -> float | None:
    """'500만원', '백만원', '이백오십만', '5000000원' 등을 float으로 변환."""
    text = text.replace(",", "").replace(" ", "")
    text = _normalize_korean_amount(text)
    m = re.search(r"(\d+(?:\.\d+)?)\s*억", text)
    if m:
        return float(m.group(1)) * 1_0000_0000
    m = re.search(r"(\d+(?:\.\d+)?)\s*만", text)
    if m:
        return float(m.group(1)) * 10_000
    m = re.search(r"(\d+(?:\.\d+)?)\s*천", text)
    if m:
        return float(m.group(1)) * 1_000
    # 순수 숫자: 4자리 이상 → 원 단위, 미만 → 만원 단위로 해석
    m = re.search(r"(\d+)", text)
    if m:
        n = float(m.group(1))
        return n if n >= 10_000 else n * 10_000
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


# ── 월별 납입 계획표 ─────────────────────────────────────────────────────────

def _monthly_plan(
    monthly: float | None,
    lump_sum: float | None,
    annual_rate: float,
    months: int,
    goal_amount: float,
) -> list[dict[str, Any]]:
    """월별 누적 금액 계획표 생성.

    Returns: [{month, cumulative, interest_earned, on_track}]
    """
    plan: list[dict[str, Any]] = []
    r = annual_rate / 100 / 12

    if lump_sum is not None:
        # 예금: 원금 + 누적 이자
        for m in range(1, months + 1):
            cumulative = lump_sum * (1 + annual_rate / 100 * m / 12)
            interest = cumulative - lump_sum
            plan.append({
                "month": m,
                "cumulative": round(cumulative),
                "interest_earned": round(interest),
                "on_track": cumulative >= goal_amount * (m / months),
            })
    else:
        mp = monthly or 0
        cumulative = 0.0
        for m in range(1, months + 1):
            if r > 0:
                cumulative = mp * ((1 + r) ** m - 1) / r * (1 + r)
            else:
                cumulative = mp * m
            interest = cumulative - mp * m
            plan.append({
                "month": m,
                "cumulative": round(cumulative),
                "interest_earned": round(interest),
                "on_track": cumulative >= goal_amount * (m / months),
            })
    return plan


# ── GPT 추천 메시지 생성 ──────────────────────────────────────────────────────

def _gpt_recommend(
    api_key: str,
    model: str,
    goal_amount: float,
    goal_months: int,
    monthly_payment: float | None,
    lump_sum: float | None,
    monthly_surplus: float | None,
    products: list[dict],
) -> str | None:
    """GPT-4o-mini로 저축 목표 맞춤 추천 메시지 생성. 실패 시 None 반환."""
    if not api_key:
        return None
    try:
        from openai import OpenAI
        achievable = [p for p in products if p.get("achievable")]
        best = (achievable or products)[:1]
        if not best:
            return None
        p = best[0]

        surplus_line = f"고객 월 잉여자금: {monthly_surplus:,.0f}원\n" if monthly_surplus else ""
        payment_line = (
            f"월 납입액: {monthly_payment:,.0f}원\n" if monthly_payment
            else f"목돈 예치액: {lump_sum:,.0f}원\n"
        )
        products_ctx = "\n".join(
            f"- {p2['product_name']} ({p2['product_type']}): "
            f"금리 {p2['base_interest_rate']}%, "
            f"만기 {p2['maturity_amount']:,}원, "
            f"이자 {p2['interest_amount']:,}원, "
            f"달성 {'가능' if p2['achievable'] else '불가'}"
            for p2 in products[:4]
        )
        prompt = (
            f"고객 저축 목표: {_period_str(goal_months)} 동안 {goal_amount:,.0f}원\n"
            f"{surplus_line}{payment_line}"
            f"\n추천 상품 후보:\n{products_ctx}\n\n"
            "위 정보를 바탕으로 고객에게 최적 상품 1개를 추천하고, "
            "왜 그 상품이 고객 상황에 맞는지 2~3문장으로 친절하게 설명해주세요. "
            "금액은 구체적으로 언급하고, 달성 가능 여부를 명확히 알려주세요."
        )
        client = OpenAI(api_key=api_key)
        resp = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": "당신은 AXful Bank의 친절한 금융 상담사입니다. 한국어로 간결하게 답변하세요."},
                {"role": "user", "content": prompt},
            ],
            max_tokens=300,
            temperature=0.4,
        )
        return resp.choices[0].message.content or None
    except Exception:
        return None


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

        # ── 결과 표시 후 후속 금액 입력 처리 ─────────────────────────────────
        if session and session.get("stage") == "RESULT_SHOWN":
            amount = _parse_amount(query)
            if amount is not None:
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

        # ── 고객 현금흐름 조회 (로그인 시) ──────────────────────────────────
        customer_no = (request.customer_no or "").strip()
        monthly_surplus: float | None = None
        if customer_no:
            try:
                cf = self._analyze_customer_cash_flow(customer_no)
                if cf and cf.get("has_data"):
                    monthly_surplus = float(cf.get("monthly_surplus") or 0)
            except Exception:
                pass

        # ── 금액·기간 확인 → 세션 저장 후 추가 질문 ─────────────────────────
        if cid:
            _SESSION[cid] = {
                "stage": "ASKED_MONTHLY",
                "goal_amount": goal_amount,
                "goal_months": goal_months,
                "customer_no": customer_no or None,
                "monthly_surplus": monthly_surplus,
            }

        # 월 잉여자금이 있으면 자동 제안
        if monthly_surplus and monthly_surplus > 0:
            required_monthly = goal_amount / goal_months
            surplus_msg = (
                f"고객님의 최근 월 평균 여유자금은 약 **{monthly_surplus:,.0f}원**입니다.\n\n"
            )
            if monthly_surplus >= required_monthly:
                surplus_msg += (
                    f"이 금액으로 매달 납입하시면 목표 달성이 가능합니다! 😊\n\n"
                    f"월 납입액을 얼마로 할까요? (여유자금 기준 {monthly_surplus:,.0f}원 추천)\n"
                    f"(목돈을 한 번에 넣으실 계획이라면 '목돈 ○○○만원' 처럼 알려주세요.)"
                )
            else:
                surplus_msg += (
                    f"목표 달성을 위해 필요한 월 납입액은 약 {required_monthly:,.0f}원인데,\n"
                    f"현재 여유자금({monthly_surplus:,.0f}원)보다 많습니다. ⚠️\n\n"
                    f"납입 가능한 금액을 알려주시면 달성 가능한 상품을 찾아드릴게요.\n"
                    f"(목돈을 한 번에 넣으실 계획이라면 '목돈 ○○○만원' 처럼 알려주세요.)"
                )
            return ChatbotFeatureExecuteResponse(
                feature_code="SAVINGS_GOAL",
                status="NEED_INFO",
                message=f"{_period_str(goal_months)} 동안 {goal_amount:,.0f}원을 모으는 목표군요! 😊\n\n{surplus_msg}",
                data=[],
            )

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
        monthly_surplus: float | None = session.get("monthly_surplus")

        is_lump = _has_lump_sum_intent(query)
        amount = _parse_amount(query)

        if amount is None:
            return ChatbotFeatureExecuteResponse(
                feature_code="SAVINGS_GOAL",
                status="NEED_INFO",
                message="금액을 인식하지 못했어요. 예를 들어 '월 30만원' 또는 '목돈 300만원' 처럼 알려주세요.",
                data=[],
            )

        # 월 납입액이 실제 잉여자금 초과 시 경고 (목돈 제외)
        warning = ""
        if not is_lump and monthly_surplus and monthly_surplus > 0 and amount > monthly_surplus:
            warning = (
                f"\n\n⚠️ 입력하신 월 납입액({amount:,.0f}원)이 "
                f"실제 여유자금({monthly_surplus:,.0f}원)을 초과합니다. "
                f"납입 부담이 생길 수 있으니 참고해 주세요."
            )

        # 세션을 RESULT_SHOWN 단계로 유지 (후속 금액 재시도 지원)
        if cid:
            _SESSION[cid] = {**session, "stage": "RESULT_SHOWN"}

        return self._recommend(
            goal_amount=goal_amount,
            goal_months=goal_months,
            monthly_payment=None if is_lump else amount,
            lump_sum=amount if is_lump else None,
            monthly_surplus=monthly_surplus,
            warning=warning,
        )

    def _recommend(
        self,
        goal_amount: float,
        goal_months: int,
        monthly_payment: float | None,
        lump_sum: float | None,
        monthly_surplus: float | None = None,
        warning: str = "",
    ) -> ChatbotFeatureExecuteResponse:
        """상품 조회 → 이자 계산 → 비교 → 추천."""
        is_lump = lump_sum is not None
        ptype_filter = "'DEPOSIT'" if is_lump else "'SAVINGS'"

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

            # 월별 계획표 (상위 상품 1개에만 생성)
            plan: list[dict] = []
            if len(enriched) == 0:  # 첫 번째 상품에만
                plan = _monthly_plan(
                    monthly=None if is_lump else (req_monthly or monthly_payment),
                    lump_sum=lump_sum,
                    annual_rate=rate,
                    months=goal_months,
                    goal_amount=goal_amount,
                )

            enriched.append({
                "row_type":          "savings_goal_product",
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
                "monthly_plan":      plan,
            })

        # 달성 가능 우선, 금리 내림차순 정렬
        enriched.sort(key=lambda x: (not x["achievable"], -x["base_interest_rate"]))

        # 첫 번째 상품에 월별 계획표 재생성 (정렬 후 best 상품 기준)
        if enriched:
            best = enriched[0]
            enriched[0]["monthly_plan"] = _monthly_plan(
                monthly=None if is_lump else (best.get("required_monthly") or monthly_payment),
                lump_sum=lump_sum,
                annual_rate=float(best.get("base_interest_rate") or 0),
                months=goal_months,
                goal_amount=goal_amount,
            )

        # ── 추천 메시지 생성 (GPT 우선, 룰 기반 폴백) ──────────────────────
        from app.config import get_settings
        s = get_settings()
        gpt_msg = _gpt_recommend(
            api_key=s.openai_api_key or "",
            model=s.openai_model,
            goal_amount=goal_amount,
            goal_months=goal_months,
            monthly_payment=monthly_payment,
            lump_sum=lump_sum,
            monthly_surplus=monthly_surplus,
            products=enriched,
        )
        message = (gpt_msg or self._rule_recommend(
            goal_amount, goal_months, monthly_payment, lump_sum, enriched
        )) + warning

        return ChatbotFeatureExecuteResponse(
            feature_code="SAVINGS_GOAL",
            status="OK",
            message=message,
            data=enriched,
        )

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

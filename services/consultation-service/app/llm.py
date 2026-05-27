# ──────────────────────────────────────────────────────────────────────────────
# Intent 분류
# ──────────────────────────────────────────────────────────────────────────────

# 우선순위 순서 — 앞쪽이 먼저 매칭됨
# CASH_FLOW_RECOMMEND 는 PRODUCT_GUIDE 보다 앞에 위치해야 한다
# (더 구체적인 "내 패턴/현금흐름 분석 기반" 의도이므로)
_INTENT_PRIORITY: list[str] = [
    "RATE_GUIDE",
    "JOIN_CONDITION",
    "PRODUCT_COMPARE",
    "TERMS_RAG",
    "CASH_FLOW_RECOMMEND",
    "PRODUCT_GUIDE",
    "FAQ",
]

_INTENT_KEYWORDS: dict[str, list[str]] = {
    "RATE_GUIDE": [
        "금리", "이자율", "이율", "금리는", "금리가", "금리 알", "금리 얼마", "이자 얼마",
    ],
    "JOIN_CONDITION": [
        "가입 조건", "가입조건", "가입 자격", "가입할 수", "가입 가능",
        "조건 알려", "조건이 뭐", "조건은",
    ],
    "PRODUCT_COMPARE": [
        "비교", "차이", "어떻게 달라", "뭐가 나아", "뭐가 좋아",
    ],
    "TERMS_RAG": [
        "약관", "중도해지", "수수료", "해지하면", "해지 시", "해지할 경우",
    ],
    "CASH_FLOW_RECOMMEND": [
        "내 패턴", "현금 흐름 분석", "현금흐름 분석", "내 소비 패턴", "내 거래 패턴",
        "내 상황에 맞", "나한테 맞는", "나에게 맞는", "내 수입", "내 지출",
        "맞춤 추천", "내 상황 분석", "분석해서 추천", "패턴 분석",
    ],
    "PRODUCT_GUIDE": [
        "상품 추천", "상품 알려", "어떤 상품", "예금 상품", "적금 상품", "청약 상품",
        "상품 종류", "상품 뭐가", "상품 뭐", "어떤 예금", "어떤 적금",
        "예금 알려", "적금 알려", "청약 알려", "추천해줘", "추천해 줘",
    ],
    "FAQ": [
        "자주 묻는", "faq", "FAQ", "자주하는 질문",
    ],
}


class IntentClassifier:
    """키워드 기반 우선순위 intent 분류기."""

    def classify(self, message: str) -> str | None:
        msg = message.lower()
        for feature_code in _INTENT_PRIORITY:
            if any(kw in msg for kw in _INTENT_KEYWORDS[feature_code]):
                return feature_code
        return None


# ──────────────────────────────────────────────────────────────────────────────
# DB 데이터 → 자연어 포맷터
# ──────────────────────────────────────────────────────────────────────────────

class FeatureAnswerFormatter:
    """feature 실행 결과(DB 데이터)를 고객 응답 텍스트로 포맷팅."""

    def format(self, feature_code: str, data: list[dict]) -> str:
        if not data:
            return "죄송합니다, 현재 해당 상품 정보를 찾을 수 없습니다. 상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요."

        handlers = {
            "RATE_GUIDE":      self._rate,
            "PRODUCT_GUIDE":   self._products,
            "JOIN_CONDITION":  self._join_condition,
            "PRODUCT_COMPARE": self._compare,
            "TERMS_RAG":       self._terms,
        }
        handler = handlers.get(feature_code)
        if handler:
            return handler(data)
        return f"조회된 정보가 {len(data)}건 있습니다. 더 자세한 내용은 상담사 연결을 이용해 주세요."

    def _rate(self, data: list[dict]) -> str:
        lines = ["[금리 안내]\n"]
        for row in data[:6]:
            name      = row.get("product_name", "")
            rate      = row.get("interest_rate", "")
            rate_type = "기본금리" if row.get("rate_type") == "BASE" else "우대금리"
            min_m     = row.get("minimum_contract_period", "")
            max_m     = row.get("maximum_contract_period", "")
            cond      = row.get("condition_description", "")
            lines.append(f"- {name} [{rate_type}] {rate}% ({min_m}~{max_m}개월)")
            if cond:
                lines.append(f"  ({cond})")
        lines.append("\n상세 조건은 영업점 또는 앱에서 확인해 주세요.")
        return "\n".join(lines)

    def _products(self, data: list[dict]) -> str:
        # 현금흐름 기반 추천 여부 판단 (match_score 필드 존재 시)
        is_personalized = any("match_score" in row for row in data)
        header = "[맞춤 상품 추천]\n" if is_personalized else "[수신 상품 목록]\n"
        lines = [header]
        for row in data[:5]:
            name   = row.get("product_name", "")
            ptype  = row.get("product_type", "")
            rate   = row.get("base_interest_rate", "")
            reason = row.get("recommend_reason", "")
            score  = row.get("match_score", "")
            if is_personalized and reason:
                lines.append(f"- {name} ({ptype}) 기본금리 {rate}%")
                lines.append(f"  ✓ {reason} (적합도 {score}점)")
            else:
                status = row.get("product_status", "")
                lines.append(f"- {name} ({ptype}) 기본금리 {rate}%  [{status}]")
        lines.append("\n특정 상품에 대해 더 알고 싶으시면 질문해 주세요.")
        return "\n".join(lines)

    def _join_condition(self, data: list[dict]) -> str:
        lines = ["[가입 조건 안내]\n"]
        for row in data[:5]:
            name      = row.get("product_name", "")
            min_amt   = row.get("min_join_amount", "")
            max_amt   = row.get("max_join_amount", "")
            min_month = row.get("min_period_month", "")
            max_month = row.get("max_period_month", "")
            early     = "가능" if row.get("is_early_termination_allowed") else "불가"
            tax       = "있음" if row.get("is_tax_benefit_available") else "없음"
            lines.append(
                f"- {name}: {min_amt:,}원~{max_amt:,}원 / "
                f"{min_month}~{max_month}개월 / 중도해지 {early} / 세제혜택 {tax}"
                if isinstance(min_amt, int) else
                f"- {name}: {min_amt}원~{max_amt}원 / {min_month}~{max_month}개월 / 중도해지 {early} / 세제혜택 {tax}"
            )
        return "\n".join(lines)

    def _compare(self, data: list[dict]) -> str:
        lines = ["[상품 비교]\n"]
        header = f"{'상품명':<20} {'유형':<12} {'금리':>6} {'기간':>12}"
        lines.append(header)
        lines.append("-" * 55)
        for row in data[:5]:
            name   = row.get("product_name", "")[:18]
            ptype  = row.get("product_type", "")[:10]
            rate   = str(row.get("base_interest_rate", ""))
            min_m  = str(row.get("min_period_month", ""))
            max_m  = str(row.get("max_period_month", ""))
            lines.append(f"{name:<20} {ptype:<12} {rate:>5}% {min_m}~{max_m}개월")
        return "\n".join(lines)

    def _terms(self, data: list[dict]) -> str:
        lines = ["[약관 검색 결과]\n"]
        for row in data[:3]:
            name    = row.get("special_term_name", "")
            summary = row.get("special_term_summary", "")
            lines.append(f"- {name}")
            if summary:
                lines.append(f"  {summary}")
        lines.append("\n전체 약관은 상품 설명서를 참조해 주세요.")
        return "\n".join(lines)


# ──────────────────────────────────────────────────────────────────────────────
# LLM 응답 (OpenAI)
# ──────────────────────────────────────────────────────────────────────────────

_SYSTEM_PROMPT = """당신은 인터넷 뱅킹 고객 상담 챗봇입니다.
예금, 적금, 청약 등 수신 금융상품에 대한 질문에 친절하고 정확하게 답변하세요.
모르는 내용이거나 본인 계좌/계약 조회처럼 인증이 필요한 경우에는
'상담사 연결이 필요합니다'라고 안내하세요.
답변은 한국어로, 간결하게 작성하세요."""


class LlmAdapter:
    """OpenAI API를 호출해 자유 텍스트 질문에 답변."""

    process_method_code = "BP003_GPT"

    def __init__(self, api_key: str, model: str = "gpt-4o-mini"):
        self.api_key = api_key
        self.model = model

    def answer(self, message: str, context: str = "") -> str:
        try:
            from openai import OpenAI
            client = OpenAI(api_key=self.api_key)

            messages = [{"role": "system", "content": _SYSTEM_PROMPT}]
            if context:
                messages.append({"role": "system", "content": f"[참고 정보]\n{context}"})
            messages.append({"role": "user", "content": message})

            response = client.chat.completions.create(
                model=self.model,
                messages=messages,
                max_tokens=500,
                temperature=0.3,
            )
            return response.choices[0].message.content.strip()
        except Exception as exc:
            return f"죄송합니다, 일시적인 오류가 발생했습니다. 상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요. ({exc})"

    def recommend(
        self,
        cash_flow: dict,
        products: list[dict],
        user_query: str,
        history_ctx: str = "",
    ) -> str:
        """현금흐름 분석 + 실제 상품 데이터 + 대화 이력 기반 개인화 추천 응답.

        Args:
            cash_flow   : _analyze_customer_cash_flow() 반환값
            products    : DB에서 조회한 판매 중인 수신 상품 목록
            user_query  : 고객 질문 텍스트
            history_ctx : _build_history_context() 반환값 (없으면 빈 문자열)
        """
        try:
            from openai import OpenAI
            client = OpenAI(api_key=self.api_key)

            # ── 현금흐름 요약 텍스트 ─────────────────────────────────────────────
            total_balance   = float(cash_flow.get("total_balance", 0))
            monthly_surplus = float(cash_flow.get("monthly_surplus", 0))
            tx_count        = float(cash_flow.get("monthly_tx_count", 0))
            has_data        = cash_flow.get("has_data", False)

            cf_text = (
                f"- 총 잔액: {total_balance:,.0f}원\n"
                f"- 월 평균 잉여자금(입금-출금): {monthly_surplus:,.0f}원\n"
                f"- 월 평균 거래 건수: {tx_count:.1f}건"
            ) if has_data else "거래 내역이 충분하지 않아 현금흐름 분석이 제한됩니다."

            # ── 상품 목록 텍스트 ─────────────────────────────────────────────────
            product_lines = [
                f"- {p.get('deposit_product_name') or p.get('product_name', '')} "
                f"({p.get('deposit_product_type') or p.get('product_type', '')}) "
                f"기본금리 {p.get('base_interest_rate', '')}%  "
                f"가입금액 {p.get('min_join_amount', '')}~{p.get('max_join_amount', '')}원  "
                f"{p.get('min_period_month', '')}~{p.get('max_period_month', '')}개월"
                for p in products[:8]
            ]
            product_text = (
                "\n".join(product_lines) if product_lines else "현재 판매 중인 수신 상품 없음"
            )

            # ── 시스템 프롬프트 ──────────────────────────────────────────────────
            system_prompt = (
                "당신은 인터넷 뱅킹 개인 금융 상담 AI입니다.\n"
                "고객의 현금흐름 데이터와 실제 판매 중인 수신 상품 정보를 바탕으로 "
                "고객에게 가장 알맞은 금융 상품을 구체적으로 추천하세요.\n"
                "규칙:\n"
                "1. 추천 근거를 반드시 명시하세요 (잔액·잉여자금·거래 패턴과 연계)\n"
                "2. 상품명과 금리를 구체적으로 언급하세요\n"
                "3. 고객이 대출·보험·펀드 등 수신 외 상품을 요청하면 "
                "'해당 상품은 담당 창구 또는 앱에서 안내 가능합니다'라고 안내하세요\n"
                "4. 답변은 한국어로, 친절하고 간결하게 (400자 이내)"
            )

            # ── 메시지 조립 ──────────────────────────────────────────────────────
            context_parts = [
                f"[고객 현금흐름 분석]\n{cf_text}",
                f"[현재 판매 중인 수신 상품]\n{product_text}",
            ]
            if history_ctx:
                context_parts.append(history_ctx)

            llm_messages: list[dict] = [
                {"role": "system", "content": system_prompt},
                {"role": "system", "content": "\n\n".join(context_parts)},
                {"role": "user",   "content": user_query or "내 현금 흐름에 맞는 상품을 추천해줘"},
            ]

            response = client.chat.completions.create(
                model=self.model,
                messages=llm_messages,
                max_tokens=600,
                temperature=0.3,
            )
            return response.choices[0].message.content.strip()

        except ImportError:
            return (
                "죄송합니다, AI 추천 서비스를 사용하려면 openai 패키지가 필요합니다. "
                "상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요."
            )
        except Exception:
            # 구체적인 에러 메시지를 외부에 노출하지 않는다
            return (
                "죄송합니다, 상품 추천 중 일시적인 오류가 발생했습니다. "
                "잠시 후 다시 시도하거나 '상담사 연결'을 선택해 주세요."
            )


# ──────────────────────────────────────────────────────────────────────────────
# 상담사 이관 fallback
# ──────────────────────────────────────────────────────────────────────────────

class LlmHandoffAdapter:
    process_method_code = "BP002"

    def answer(self, message: str) -> str:
        return (
            "시나리오로 즉시 처리하기 어려운 문의입니다. "
            "LLM 응답 검증 레이어가 연결되기 전까지는 상담사 연결을 권장합니다."
        )

# ──────────────────────────────────────────────────────────────────────────────
# Intent 분류
# ──────────────────────────────────────────────────────────────────────────────

# 우선순위 순서 — 앞쪽이 먼저 매칭됨
# CASH_FLOW_RECOMMEND 는 PRODUCT_GUIDE 보다 앞에 위치해야 한다
# (더 구체적인 "내 패턴/현금흐름 분석 기반" 의도이므로)
_INTENT_PRIORITY: list[str] = [
    "SAVINGS_GOAL",
    "RATE_GUIDE",
    "JOIN_CONDITION",
    "PRODUCT_COMPARE",
    "TERMS_RAG",
    "CASH_FLOW_RECOMMEND",
    "PRODUCT_GUIDE",
    "FAQ",
]

_INTENT_KEYWORDS: dict[str, list[str]] = {
    "SAVINGS_GOAL": [
        "모으고 싶", "모으고싶", "모으려고", "모으려 해", "모아야",
        "저축 목표", "저축목표", "목표 금액", "목표금액",
        "만원 모으", "억 모으", "천만원 모으",
        "저축 계획", "저축계획", "얼마 모으", "얼마나 모으",
        "모을 수 있", "몇 개월이면", "몇년이면",
    ],
    "RATE_GUIDE": [
        "금리 목록", "금리 보여", "금리 알려줘", "이자율 알려", "금리 현황",
        "금리 알려", "금리 어떻게", "금리가 어떻게", "금리가 얼마", "금리 얼마",
        "이자 얼마", "이자율이", "금리 비교", "어떤 금리", "금리 정보",
        "모든 금리", "전체 금리", "상품 금리", "금리 보여줘",
        "금리 말해줘", "금리들", "금리 다", "금리 전부",
    ],
    "JOIN_CONDITION": [
        "가입 조건", "가입조건", "가입 자격", "가입 대상",
        "가입할 수 있", "가입 가능", "가입이 가능",
    ],
    "PRODUCT_COMPARE": [
        "비교해줘", "비교해 줘", "어떻게 달라",
        "차이", "차이가", "차이점", "다른 점", "다른점", "다른가", "다른지",
        "뭐가 달라", "뭐가 다른", "어떻게 다른", "차이를 알려", "차이 알려",
        "무슨 차이", "어떤 차이", "어떤 점이 다른",
    ],
    "TERMS_RAG": [
        "약관", "중도해지", "수수료",
    ],
    "CASH_FLOW_RECOMMEND": [
        "내 패턴", "현금흐름 분석", "내 소비 패턴", "내 거래 패턴",
        "나한테 맞는", "나에게 맞는", "분석해서 추천", "패턴 분석",
        "내 상황에 맞는", "내 상황에 적합", "맞춤 추천",
        "장점순", "유리한 순", "좋은 순", "추천 순", "랭킹", "순위대로",
        "순위별", "순위별로", "순서별", "1위부터", "상품 순위", "추천 순위",
        "어떤 게 좋아", "어떤게 좋아", "뭐가 좋아", "뭐가 나아", "뭐가 더 좋",
        "어느 게 좋", "어느게 좋", "어떤 상품이 좋",
        "뭐가 더 적합", "어떤 게 적합", "어느 게 적합", "어느 쪽이 적합",
        "뭐가 좋을지", "어느 것이 좋", "어느 게 더", "어떤 게 더",
        "뭐가 더 나", "어떤 게 나", "어느 게 나",
        "무엇이 좋", "무엇이 더", "무엇이 적합",
    ],
    "PRODUCT_GUIDE": [
        "상품 목록", "상품 보여줘", "예금 상품 알려", "적금 상품 알려", "청약 상품 알려",
        "적금 종류", "예금 종류", "청약 종류", "상품 종류", "종류 알려", "어떤 종류",
        "적금 뭐가 있", "예금 뭐가 있", "청약 뭐가 있", "어떤 상품 있", "상품 뭐가 있",
        "적금 알려줘", "예금 알려줘", "청약 알려줘",
        "적금 목록", "예금 목록", "청약 목록",
        "적금 소개", "예금 소개", "상품 소개",
        "상품 추천", "상품 있", "예금 상품 있", "적금 상품 있",
    ],
    "FAQ": [
        "자주 묻는", "faq", "FAQ",
    ],
}


_PRODUCT_TYPES = ["예금", "적금", "청약", "저축"]
_PERSONAL_RECOMMEND_WORDS = [
    "나한테", "나에게", "내게", "내한테", "저한테", "저에게",
    "나한", "나에", "나는", "저는", "내가",
]
_RECOMMEND_WORDS = [
    "맞는", "맞춰", "맞춤", "적합", "적합한", "알맞", "알맞은",
    "추천", "좋은", "좋아", "좋아요", "나은", "낫", "유리",
    "어울리", "어울려", "맞아", "맞지", "골라", "선택",
]


class IntentClassifier:
    """키워드 기반 우선순위 intent 분류기."""

    def classify(self, message: str) -> str | None:
        msg = message.lower()

        has_personal = any(w in msg for w in _PERSONAL_RECOMMEND_WORDS)
        has_recommend = any(w in msg for w in _RECOMMEND_WORDS)

        # 1. 등록된 키워드 매칭 — 단, PRODUCT_COMPARE는 개인 추천 의도가 없을 때만
        for feature_code in _INTENT_PRIORITY:
            if any(kw in msg for kw in _INTENT_KEYWORDS[feature_code]):
                # 비교 질문이지만 개인 추천 의도가 함께 있으면 CASH_FLOW_RECOMMEND로
                if feature_code == "PRODUCT_COMPARE" and (has_personal or has_recommend):
                    return "CASH_FLOW_RECOMMEND"
                return feature_code

        has_product = any(p in msg for p in _PRODUCT_TYPES)
        has_personal = any(w in msg for w in _PERSONAL_RECOMMEND_WORDS)
        has_recommend = any(w in msg for w in _RECOMMEND_WORDS)

        # 2. "둘 중" / "어느 쪽" / "뭐가 더" 비교 후속 질문 → 이전 대화 맥락으로 현금흐름 추천
        _COMPARE_FOLLOW_UP = [
            "둘 중", "어느 쪽", "어느쪽", "어떤 쪽", "어떤쪽", "둘다", "둘 다",
            "그 중", "그중", "이 중", "이중",
            "뭐가 더 나", "뭐가 더 적합", "뭐가 더 좋", "뭐가 더 유리",
            "어떤 게 더 나", "어느 게 더 나", "더 나은 게", "더 적합한 게",
            "하나만", "딱 하나", "하나 골라", "하나 추천",
        ]
        if any(w in msg for w in _COMPARE_FOLLOW_UP):
            return "CASH_FLOW_RECOMMEND"

        # 3. "예금"과 "적금"이 같이 나올 때: 추천 의도가 있을 때만 CASH_FLOW_RECOMMEND
        #    (차이/비교 질문은 이미 위 PRODUCT_COMPARE 키워드에서 처리됨)
        if "예금" in msg and "적금" in msg and (has_personal or has_recommend):
            return "CASH_FLOW_RECOMMEND"

        # 4. 패턴 기반: 상품 + 나한테/나에게 + 맞는/적합/추천 조합
        if has_product and has_personal and has_recommend:
            return "CASH_FLOW_RECOMMEND"

        # 5. 상품 단독 입력 ("적금", "예금", "청약") → 상품 목록 안내
        _PRODUCT_ONLY = ["예금", "적금", "청약"]
        if any(msg.strip() == p or msg.strip().startswith(p + " ") or msg.strip().endswith(" " + p)
               for p in _PRODUCT_ONLY):
            return "PRODUCT_GUIDE"

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
        lines = ["[상품별 금리 안내]\n"]
        # 상품명 기준으로 그룹핑
        from collections import defaultdict
        grouped: dict = defaultdict(list)
        for row in data:
            grouped[row.get("product_name", "")].append(row)
        for name, rows in grouped.items():
            base_rows = [r for r in rows if r.get("rate_type") == "BASE"]
            pref_rows = [r for r in rows if r.get("rate_type") != "BASE"]
            base = base_rows[0] if base_rows else rows[0]
            rate = base.get("interest_rate", "")
            min_m = base.get("minimum_contract_period", "")
            max_m = base.get("maximum_contract_period", "")
            period_str = f" ({min_m}~{max_m}개월)" if min_m and max_m else ""
            pref_str = ""
            if pref_rows:
                pref_rates = [str(r.get("interest_rate","")) for r in pref_rows]
                pref_str = f" / 우대금리 최대 {max(pref_rates)}%"
            lines.append(f"- {name}: 기본금리 {rate}%{pref_str}{period_str}")
        lines.append("\n상세 조건은 영업점 또는 앱에서 확인해 주세요.")
        return "\n".join(lines)

    _TYPE_KO = {"DEPOSIT": "예금", "SAVINGS": "적금", "SUBSCRIPTION": "청약"}

    def _products(self, data: list[dict]) -> str:
        is_personalized = any("match_score" in row for row in data)
        header = "[맞춤 상품 추천]\n" if is_personalized else "[판매 중인 상품 목록]\n"
        lines = [header]
        for row in data:  # 전체 표시, 제한 없음
            name   = row.get("product_name") or row.get("deposit_product_name", "")
            ptype  = row.get("product_type") or row.get("deposit_product_type", "")
            ptype_ko = self._TYPE_KO.get(ptype, ptype)
            rate   = row.get("base_interest_rate", "")
            reason = row.get("recommend_reason", "")
            score  = row.get("match_score", "")
            rate_str = f" · 기본금리 {rate}%" if rate else ""
            if is_personalized and reason:
                lines.append(f"- [{ptype_ko}] {name}{rate_str}")
                lines.append(f"  ✓ {reason} (적합도 {score}점)")
            else:
                lines.append(f"- [{ptype_ko}] {name}{rate_str}")
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


import time

from langfuse.decorators import observe

from app.metrics import (
    chatbot_fallback_total,
    chatbot_llm_completion_tokens,
    chatbot_llm_duration_seconds,
    chatbot_llm_error_total,
    chatbot_llm_prompt_tokens,
)

# ──────────────────────────────────────────────────────────────────────────────
# LLM 응답 (OpenAI)
# ──────────────────────────────────────────────────────────────────────────────

_SYSTEM_PROMPT = """당신은 인터넷 뱅킹 고객 상담 챗봇입니다.
예금, 적금, 청약 등 수신 금융상품에 대한 질문에 친절하고 정확하게 답변하세요.

규칙:
1. 고객의 자연어 질문을 의도에 맞게 해석하세요. 구어체·줄임말도 이해하세요.
2. [참고 정보]에 추천 상품 목록이 있으면, 해당 상품에 대한 후속 질문으로 간주하고 그 정보를 바탕으로 답하세요.
3. 예금과 적금의 차이, 금리 개념, 가입 방법 등 일반 금융 질문에는 간단명료하게 설명하세요.
4. '제일 좋은', '가장 좋은', '최고의', '1위', '어떤 게 나아', '골라줘', '추천해줘' 등 단일 추천을 묻는 질문에는 상품 1개만 지정해서 답하세요.
5. 장점·특징·이유 등을 묻는 후속 질문에는 bullet point로 구체적으로 답하세요.
6. 본인 계좌/계약 조회처럼 인증이 필요한 경우에는 '상담사 연결이 필요합니다'라고 안내하세요.
7. 답변은 한국어로, 간결하게 (300자 이내) 작성하세요."""


class LlmAdapter:
    """OpenAI API를 호출해 자유 텍스트 질문에 답변."""

    process_method_code = "BP003_GPT"

    def __init__(self, api_key: str, model: str = "gpt-4o-mini"):
        self.api_key = api_key
        self.model = model

    @observe(name="llm-answer")
    def answer(self, message: str, context: str = "") -> tuple[str, bool]:
        """LLM 응답을 반환한다.

        Returns:
            (response_text, is_error) — is_error=True 이면 LLM 호출 실패를 의미하며
            호출자가 상담사 이관 등 fallback 처리를 수행해야 한다.
        """
        from langfuse.decorators import langfuse_context
        langfuse_context.update_current_trace(
            tags=["consultation-service"],
            metadata={"service": "consultation-service"},
        )
        start = time.perf_counter()
        is_error = False
        try:
            from langfuse.openai import OpenAI
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
            if response.usage:
                chatbot_llm_prompt_tokens.labels(method="answer").observe(response.usage.prompt_tokens)
                chatbot_llm_completion_tokens.labels(method="answer").observe(response.usage.completion_tokens)
            return response.choices[0].message.content.strip(), False
        except Exception as exc:
            is_error = True
            return (
                f"죄송합니다, 일시적인 오류가 발생했습니다. 상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요. ({exc})",
                True,
            )
        finally:
            chatbot_llm_duration_seconds.labels(method="answer").observe(time.perf_counter() - start)
            if is_error:
                chatbot_llm_error_total.labels(method="answer").inc()

    @observe(name="llm-recommend")
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
        from langfuse.decorators import langfuse_context
        langfuse_context.update_current_trace(
            tags=["consultation-service"],
            metadata={"service": "consultation-service"},
        )
        start = time.perf_counter()
        is_error = False
        try:
            from langfuse.openai import OpenAI
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
                "2. 상품명과 금리는 구체적인 상품 추천 요청 시에만 언급하세요. "
                "'예금과 적금 중 어느 게 맞나요?' 같은 **상품 유형 비교 질문**에는 상품명을 나열하지 말고 "
                "예금/적금 중 어느 유형이 더 적합한지만 간결하게 답하세요.\n"
                "3. '예금과 적금 중 나한테 적합한 거', '뭐가 더 나아?', '뭐가 더 적합해?' 같은 유형 비교 질문은 "
                "반드시 예금 또는 적금 중 **하나의 유형만 선택**해서 이유와 함께 답하세요. "
                "상품 목록 나열 금지. '둘 다 가능합니다' 절대 금지. "
                "기준: 월 잉여자금 30만원 이상→적금, 잔액 1000만원 이상→예금, 둘 다면 적금 우선.\n"
                "4. '하나만 추천해줘', '하나만 골라줘' 요청 시에만 구체적인 AXful 상품명을 추천하세요. 군인 전용(장병) 상품 추천 금지.\n"
                "5. '둘 중', '어느 쪽이', '뭐가 더' 같은 후속 질문은 이전 대화 이력을 참고해 맥락을 파악하고 답변하세요\n"
                "6. 거래 내역이 부족할 때도 질문 의도에 맞게 일반적인 기준으로 답변하세요\n"
                "7. 고객이 대출·보험·펀드 등 수신 외 상품을 요청하면 "
                "'해당 상품은 담당 창구 또는 앱에서 안내 가능합니다'라고 안내하세요\n"
                "8. 답변은 한국어로, 친절하고 간결하게 (400자 이내)"
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
            if response.usage:
                chatbot_llm_prompt_tokens.labels(method="recommend").observe(response.usage.prompt_tokens)
                chatbot_llm_completion_tokens.labels(method="recommend").observe(response.usage.completion_tokens)
            return response.choices[0].message.content.strip()

        except ImportError:
            is_error = True
            return (
                "죄송합니다, AI 추천 서비스를 사용하려면 openai 패키지가 필요합니다. "
                "상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요."
            )
        except Exception:
            is_error = True
            # 구체적인 에러 메시지를 외부에 노출하지 않는다
            return (
                "죄송합니다, 상품 추천 중 일시적인 오류가 발생했습니다. "
                "잠시 후 다시 시도하거나 '상담사 연결'을 선택해 주세요."
            )
        finally:
            chatbot_llm_duration_seconds.labels(method="recommend").observe(time.perf_counter() - start)
            if is_error:
                chatbot_llm_error_total.labels(method="recommend").inc()


# ──────────────────────────────────────────────────────────────────────────────
# 상담사 이관 fallback
# ──────────────────────────────────────────────────────────────────────────────

class LlmHandoffAdapter:
    process_method_code = "BP002"

    def answer(self, message: str) -> str:
        chatbot_fallback_total.inc()
        return (
            "시나리오로 즉시 처리하기 어려운 문의입니다. "
            "LLM 응답 검증 레이어가 연결되기 전까지는 상담사 연결을 권장합니다."
        )

# ──────────────────────────────────────────────────────────────────────────────
# Intent 분류
# ──────────────────────────────────────────────────────────────────────────────

# 우선순위 순서 — 앞쪽이 먼저 매칭됨
# CASH_FLOW_RECOMMEND 는 PRODUCT_GUIDE 보다 앞에 위치해야 한다
# (더 구체적인 "내 패턴/현금흐름 분석 기반" 의도이므로)
_INTENT_PRIORITY: list[str] = [
    "SAVINGS_GOAL",
    "MY_ACCOUNTS",
    "INTEREST_HISTORY",
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
    "MY_ACCOUNTS": [
        "내 계좌", "내계좌", "계좌 보여", "계좌 조회", "계좌조회",
        "계좌 목록", "계좌 알려", "내 통장", "통장 보여", "잔액 조회",
        "잔액 보여", "잔액 알려", "내 잔액",
    ],
    "INTEREST_HISTORY": [
        "이자 내역", "이자내역", "이자 조회", "이자 확인", "이자 얼마 받",
        "이자 받은", "이자 지급", "이자 내역 보여", "이자 내역 알려",
        "이번 달 이자", "이자 내역 조회",
    ],
    "RATE_GUIDE": [
        "금리 목록", "금리 보여", "금리 알려줘", "이자율 알려", "금리 현황",
        "금리 알려", "금리 어떻게", "금리가 어떻게", "금리가 얼마", "금리 얼마",
        "이자 얼마", "이자율이", "금리 비교", "어떤 금리", "금리 정보",
        "모든 금리", "전체 금리", "상품 금리", "금리 보여줘",
        "금리 말해줘", "금리들", "금리 다", "금리 전부",
        "우대금리", "우대 금리", "우대금리 조건", "우대금리 알려",
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


class LlmAdapter:
    """외부 AI 호출 없이 기존 서비스 생성자와 테스트 계약을 유지하는 어댑터."""

    process_method_code = "BP003_RULE"

    def __init__(self, api_key: str = "", model: str = "rule-based"):
        self.api_key = api_key
        self.model = model

    def answer(self, message: str, context: str = "") -> tuple[str, bool]:
        return (
            "자동 답변 범위를 벗어난 문의입니다. 상담사 연결을 이용해 주세요.",
            True,
        )

    def recommend(
        self,
        cash_flow: dict,
        products: list[dict],
        user_query: str,
        history_ctx: str = "",
    ) -> str:
        raise RuntimeError("상품 추천은 규칙 기반 추천 엔진에서만 처리합니다.")


class LlmHandoffAdapter:
    process_method_code = "BP002"

    def answer(self, message: str) -> str:
        try:
            from app.metrics import chatbot_fallback_total
            chatbot_fallback_total.inc()
        except Exception:
            pass
        return (
            "시나리오로 즉시 처리하기 어려운 문의입니다. "
            "상담사 연결을 이용해 주세요."
        )



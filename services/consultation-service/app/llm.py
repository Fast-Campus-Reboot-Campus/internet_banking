# ──────────────────────────────────────────────────────────────────────────────
# Intent 분류
# ──────────────────────────────────────────────────────────────────────────────

# 우선순위 순서 — 앞쪽이 먼저 매칭됨
# CASH_FLOW_RECOMMEND 는 PRODUCT_GUIDE 보다 앞에 위치해야 한다
# (더 구체적인 "내 패턴/현금흐름 분석 기반" 의도이므로)
_INTENT_PRIORITY: list[str] = [
    "SAVINGS_GOAL",
    "REINVESTMENT_RECOMMEND",
    "MATURITY_MANAGEMENT",
    "MATURITY_SCHEDULE",
    "SPENDING_PATTERN",
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
    "SPENDING_PATTERN": [
        "지출 패턴", "소비 패턴", "지출 분석", "소비 분석",
        "얼마나 썼", "얼마 썼", "돈 어디에 썼", "돈 어디 썼", "돈을 어디에",
        "이번 달 지출", "이번달 지출", "월별 지출", "지출 내역",
        "소비 현황", "지출 현황", "과소비", "지출이 늘", "지출 늘었",
        "많이 썼", "너무 썼", "지출 경고", "지출 알림",
        "지출", "소비",
    ],
    "REINVESTMENT_RECOMMEND": [
        "재투자", "재예치", "재가입", "다시 가입",
        "만기 추천", "만기 상품 추천", "만기 운용 추천",
        "어디에 넣", "어디다 넣", "어디에 재",
    ],
    "MATURITY_MANAGEMENT": [
        "만기 후 어떻게", "만기되면 어떻게", "만기 상품 어떻게", "만기 되면 어떻게",
        "만기됐", "만기 됐", "만기됐는데", "만기 됐는데",
        "만기되면", "만기 되면", "만기 후",
        "만기 운용", "갱신", "자동 갱신",
    ],
    "MATURITY_SCHEDULE": [
        "만기일", "만기 예정", "만기 언제", "만기일 언제",
        "곧 만기", "다음 달 만기", "이번 달 만기", "만기 상품",
        "만기 알려", "만기 확인", "만기 조회", "만기 내역",
        "언제 만기", "만기 날짜", "만기 일정",
    ],
    "SAVINGS_GOAL": [
        "모으고", "모으려", "모아야", "모을래", "모을게", "모아볼", "모아서",
        "모아야", "모아두", "모으면", "모으나", "모을 수", "모을수",
        "저축 목표", "저축목표", "목표 금액", "목표금액",
        "저축 계획", "저축계획", "얼마 모으", "얼마나 모으",
        "몇 개월이면", "몇년이면",
        "만원 목표", "억 목표", "천만원 목표", "원 목표",
        "개월안에", "개월 안에", "년안에", "년 안에", "달안에", "달 안에",
        "목표 달성", "목표로", "목표 세워", "목표를 세",
        "원 모으", "만원 모", "억 모",
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
                # 비교 질문 + 개인 추천 의도: 명시적 비교 키워드("비교", "차이")가 있으면 PRODUCT_COMPARE 유지
                if feature_code == "PRODUCT_COMPARE" and (has_personal or has_recommend):
                    has_explicit_compare = any(w in msg for w in ["비교", "차이", "다른 점", "다른점"])
                    if not has_explicit_compare:
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
            # "비교"가 명시적으로 있으면 PRODUCT_COMPARE로 처리
            if any(w in msg for w in ["비교", "차이", "다른 점"]):
                return "PRODUCT_COMPARE"
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


class OpenAIDocumentAnalyzer:
    """업로드된 문서(PDF 텍스트)를 분석하는 어댑터.

    OpenAI API 키가 있으면 GPT를 사용하고, 없으면 룰 기반 분석으로 폴백한다.
    """

    _SYSTEM_PROMPTS: dict[str, str] = {
        "CASH_FLOW": (
            "당신은 은행 재무 상담사입니다. 고객이 제공한 타행 거래내역을 분석해 "
            "월별 수입/지출 패턴, 주요 지출 카테고리, 저축 여력을 한국어로 요약해 주세요. "
            "구체적인 수치와 함께 금융 상품 추천 포인트도 함께 제시해 주세요."
        ),
        "TERMS": (
            "당신은 금융 약관 전문가입니다. 제공된 약관을 고객이 이해하기 쉽게 핵심 내용을 "
            "한국어로 설명해 주세요. 특히 중도해지 조건, 수수료, 주의사항을 강조해 주세요."
        ),
        "PRODUCT": (
            "당신은 은행 상품 전문가입니다. 제공된 상품 설명서를 바탕으로 금리 조건, "
            "가입 조건, 해지 방법, 세제 혜택을 고객이 쉽게 이해할 수 있도록 한국어로 요약해 주세요."
        ),
    }

    def __init__(self, api_key: str, model: str = "gpt-4o-mini") -> None:
        self._api_key = api_key
        self._model = model

    def analyze(self, text: str, analyze_type: str) -> str:
        if self._api_key:
            try:
                return self._analyze_with_openai(text, analyze_type)
            except Exception:
                pass
        return self._analyze_rule_based(text, analyze_type)

    def _analyze_with_openai(self, text: str, analyze_type: str) -> str:
        from openai import OpenAI
        system_prompt = self._SYSTEM_PROMPTS.get(analyze_type, self._SYSTEM_PROMPTS["TERMS"])
        client = OpenAI(api_key=self._api_key)
        resp = client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"다음 내용을 분석해 주세요:\n\n{text[:8000]}"},
            ],
            max_tokens=1000,
        )
        return resp.choices[0].message.content or "분석 결과를 가져올 수 없습니다."

    def _analyze_rule_based(self, text: str, analyze_type: str) -> str:
        import re

        if analyze_type == "CASH_FLOW":
            return self._analyze_cash_flow(text)
        if analyze_type == "TERMS":
            return self._analyze_terms(text)
        if analyze_type == "PRODUCT":
            return self._analyze_product(text)
        return "분석 유형을 인식할 수 없습니다."

    def _analyze_cash_flow(self, text: str) -> str:
        import re
        lines = [l.strip() for l in text.splitlines() if l.strip()]

        # 금액 패턴: 숫자+원 혹은 쉼표 포함 숫자
        amount_pattern = re.compile(r"([\+\-]?\d[\d,]*)\s*원?")

        income_keywords = ["입금", "급여", "월급", "이자", "수입", "환급", "적립"]
        expense_keywords = ["출금", "이체", "결제", "납부", "지출", "인출", "자동이체"]

        income_total = 0
        expense_total = 0
        income_items: list[str] = []
        expense_items: list[str] = []

        for line in lines:
            amounts = [int(m.group(1).replace(",", "")) for m in amount_pattern.finditer(line)]
            if not amounts:
                continue
            amt = amounts[-1]
            if any(k in line for k in income_keywords):
                income_total += amt
                if len(income_items) < 5:
                    income_items.append(f"  • {line[:40]}")
            elif any(k in line for k in expense_keywords):
                expense_total += amt
                if len(expense_items) < 5:
                    expense_items.append(f"  • {line[:40]}")

        surplus = income_total - expense_total
        parts: list[str] = ["📊 **거래내역 분석 결과**\n"]
        parts.append(f"💰 총 수입: {income_total:,}원")
        if income_items:
            parts.extend(income_items)
        parts.append(f"\n💸 총 지출: {expense_total:,}원")
        if expense_items:
            parts.extend(expense_items)
        parts.append(f"\n📈 월평균 잉여자금: {surplus:,}원")

        if surplus > 500_000:
            parts.append("\n✅ 잉여자금이 충분합니다. 정기적금이나 청약저축을 활용하면 목돈 마련에 유리합니다.")
        elif surplus > 0:
            parts.append("\n💡 소액 자동이체 적금으로 저축 습관을 만들어 보세요.")
        else:
            parts.append("\n⚠️ 지출이 수입을 초과하고 있습니다. 지출 항목 점검을 권장드립니다.")

        if income_total == 0 and expense_total == 0:
            parts = [
                "📄 거래내역 파일을 확인했습니다.\n",
                "수입·지출 금액을 자동으로 파악하기 어려운 형식입니다.\n",
                "주요 내용을 직접 확인해 주세요:\n",
            ]
            parts.extend(f"  • {l}" for l in lines[:10])

        return "\n".join(parts)

    def _analyze_terms(self, text: str) -> str:
        import re
        lines = [l.strip() for l in text.splitlines() if l.strip()]

        # 조항 번호(제N조) 또는 숫자. 형태로 시작하는 줄 추출
        article_pattern = re.compile(r"^(제\s*\d+\s*조|[\d]+\.)\s*(.+)")
        articles: list[str] = []
        for line in lines:
            m = article_pattern.match(line)
            if m:
                articles.append(f"  • {m.group(0)[:60]}")
            if len(articles) >= 10:
                break

        # 키워드별 관련 문장 추출
        highlights: dict[str, list[str]] = {
            "중도해지": [],
            "수수료": [],
            "이자": [],
            "만기": [],
        }
        for line in lines:
            for keyword, bucket in highlights.items():
                if keyword in line and len(bucket) < 2:
                    bucket.append(line[:80])

        parts = ["📋 **약관 분석 결과**\n"]

        if articles:
            parts.append("📌 주요 조항:")
            parts.extend(articles)

        for keyword, bucket in highlights.items():
            if bucket:
                parts.append(f"\n🔍 {keyword} 관련:")
                parts.extend(f"  • {s}" for s in bucket)

        if len(parts) == 1:
            parts.append("약관 내용을 확인했습니다. 주요 내용:")
            parts.extend(f"  • {l}" for l in lines[:8])

        parts.append("\n⚠️ 중도해지 시 이자 손실이 발생할 수 있으니 가입 전 약관을 꼼꼼히 확인하세요.")
        return "\n".join(parts)

    def _analyze_product(self, text: str) -> str:
        import re
        lines = [l.strip() for l in text.splitlines() if l.strip()]

        rate_pattern = re.compile(r"(\d+\.?\d*)\s*%")
        amount_pattern = re.compile(r"(\d[\d,]*)\s*원")
        period_pattern = re.compile(r"(\d+)\s*개월")

        rates: list[str] = []
        amounts: list[str] = []
        periods: list[str] = []
        features: list[str] = []

        feature_keywords = ["세제", "비과세", "소득공제", "우대", "혜택", "특징", "조건", "가입대상", "만기"]

        for line in lines:
            if rate_pattern.search(line) and len(rates) < 4:
                rates.append(f"  • {line[:70]}")
            if amount_pattern.search(line) and len(amounts) < 3:
                amounts.append(f"  • {line[:70]}")
            if period_pattern.search(line) and len(periods) < 3:
                periods.append(f"  • {line[:70]}")
            if any(k in line for k in feature_keywords) and len(features) < 5:
                features.append(f"  • {line[:70]}")

        parts = ["📄 **상품 설명서 분석 결과**\n"]
        if rates:
            parts.append("💹 금리 조건:")
            parts.extend(rates)
        if amounts:
            parts.append("\n💰 가입 금액:")
            parts.extend(amounts)
        if periods:
            parts.append("\n📅 가입 기간:")
            parts.extend(periods)
        if features:
            parts.append("\n✨ 주요 특징/혜택:")
            parts.extend(features)

        if len(parts) == 1:
            parts.append("상품 설명서 내용을 확인했습니다. 주요 내용:")
            parts.extend(f"  • {l}" for l in lines[:8])

        parts.append("\n💡 자세한 가입 조건은 영업점 또는 고객센터에 문의하세요.")
        return "\n".join(parts)


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




class RagAnswerGenerator:
    """RAG 검색 결과 + 사용자 질문 → 자연어 답변 생성기.

    OpenAI API 키가 있으면 GPT-4o-mini로 답변하고, 없으면 룰 기반으로 폴백한다.
    """

    _SYSTEM_PROMPT = (
        "당신은 AXful Bank의 친절한 금융 상품 상담사입니다. "
        "제공된 상품 정보를 근거로 고객 질문에 한국어로 답변해 주세요. "
        "상품 정보에 없는 내용은 추측하지 말고 '확인이 필요합니다'라고 안내하세요. "
        "답변은 간결하고 명확하게 작성하며, 필요시 불릿 포인트를 사용하세요."
    )

    def __init__(self, api_key: str, model: str = "gpt-4o-mini") -> None:
        self._api_key = api_key
        self._model = model

    def answer(self, query: str, rag_results: list[dict]) -> str:
        if not rag_results:
            return "현재 등록된 상품 정보 기준으로는 답변이 어렵습니다. 상담사 연결을 원하시면 '상담사 연결'을 선택해 주세요."

        context = self._build_context(rag_results)

        if self._api_key:
            try:
                return self._answer_with_openai(query, context)
            except Exception:
                pass

        return self._answer_rule_based(query, rag_results)

    def _build_context(self, rag_results: list[dict]) -> str:
        lines = []
        for i, r in enumerate(rag_results, 1):
            doc_type = r.get("_type", "product")
            if doc_type == "product":
                name    = r.get("deposit_product_name") or r.get("product_name", "")
                ptype   = r.get("deposit_product_type") or r.get("product_type", "")
                ptype_ko = {"DEPOSIT": "예금", "SAVINGS": "적금", "SUBSCRIPTION": "청약"}.get(ptype, ptype)
                rate    = r.get("base_interest_rate", "")
                min_amt = r.get("min_join_amount", "")
                max_amt = r.get("max_join_amount", "")
                min_m   = r.get("min_period_month", "")
                max_m   = r.get("max_period_month", "")
                early   = "가능" if r.get("is_early_termination_allowed") else "불가"
                tax     = "있음" if r.get("is_tax_benefit_available") else "없음"
                desc    = r.get("description", "")
                line = (
                    f"[상품 {i}] {name} ({ptype_ko})\n"
                    f"  - 기본금리: {rate}% | 가입금액: {min_amt}~{max_amt}원 | 기간: {min_m}~{max_m}개월\n"
                    f"  - 중도해지: {early} | 세제혜택: {tax}"
                )
                if desc:
                    line += f"\n  - 설명: {desc[:150]}"
            else:
                name    = r.get("special_term_name", "")
                summary = r.get("special_term_summary", "") or r.get("special_term_content", "")[:150]
                line    = f"[약관 {i}] {name}\n  - {summary}"
            lines.append(line)
        return "\n\n".join(lines)

    def _answer_with_openai(self, query: str, context: str) -> str:
        from openai import OpenAI
        client = OpenAI(api_key=self._api_key)
        resp = client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": self._SYSTEM_PROMPT},
                {"role": "user", "content": f"[관련 상품 정보]\n{context}\n\n[고객 질문]\n{query}"},
            ],
            max_tokens=600,
            temperature=0.3,
        )
        return resp.choices[0].message.content or "답변을 생성할 수 없습니다."

    def _answer_rule_based(self, query: str, rag_results: list[dict]) -> str:
        ptype_ko = {"DEPOSIT": "예금", "SAVINGS": "적금", "SUBSCRIPTION": "청약"}
        lines = ["관련 상품 정보를 찾았습니다.\n"]
        for r in rag_results[:3]:
            doc_type = r.get("_type", "product")
            if doc_type == "product":
                name  = r.get("deposit_product_name") or r.get("product_name", "")
                ptype = r.get("deposit_product_type") or r.get("product_type", "")
                rate  = r.get("base_interest_rate", "")
                min_m = r.get("min_period_month", "")
                max_m = r.get("max_period_month", "")
                lines.append(f"• {name} ({ptype_ko.get(ptype, ptype)}): 금리 {rate}%, 기간 {min_m}~{max_m}개월")
            else:
                name    = r.get("special_term_name", "")
                summary = r.get("special_term_summary", "")
                lines.append(f"• {name}: {summary[:80]}" if summary else f"• {name}")
        lines.append("\n자세한 내용은 상담사 연결을 이용해 주세요.")
        return "\n".join(lines)

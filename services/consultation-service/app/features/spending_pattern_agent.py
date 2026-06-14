"""지출 패턴 경고 에이전트 (Agent 4).

최근 3개월 출금 거래를 카테고리별로 분류하고,
이번 달 지출이 직전 2개월 평균 대비 크게 증가한 항목을 감지해
'이번 달 식비가 평소의 2배입니다' 형태의 인사이트를 제공한다.
"""
from __future__ import annotations

from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from typing import Any

from app.features.base import FeatureExecutorBase
from app.schemas import ChatbotFeatureExecuteRequest, ChatbotFeatureExecuteResponse

FEATURE_CODE = "SPENDING_PATTERN"

# 카테고리 정의: (카테고리명, 키워드 목록)
# transaction_summary 또는 transaction_memo를 소문자 변환 후 키워드 매칭
_CATEGORIES: list[tuple[str, list[str]]] = [
    ("식비",    ["식당", "음식", "카페", "편의점", "배달", "마트", "식비", "쿠팡이츠", "배민", "요기요", "점심", "저녁", "아침", "베이커리", "분식", "치킨", "피자", "햄버거"]),
    ("교통",    ["교통", "버스", "지하철", "택시", "카카오택시", "주유", "기름", "ktx", "고속버스", "톨", "주차", "철도"]),
    ("쇼핑",    ["쇼핑", "의류", "패션", "신발", "백화점", "아울렛", "온라인쇼핑", "쿠팡", "11번가", "지마켓", "옥션", "위메프", "티몬", "네이버쇼핑"]),
    ("의료",    ["병원", "의원", "약국", "한의원", "치과", "안과", "의료", "건강검진", "약값"]),
    ("공과금",  ["전기", "수도", "가스", "통신", "인터넷", "핸드폰", "휴대폰", "요금", "관리비"]),
    ("문화/여가", ["영화", "ott", "넷플릭스", "유튜브", "게임", "여행", "숙박", "호텔", "헬스", "운동", "피트니스", "스포츠", "독서", "책"]),
    ("금융",    ["보험", "대출", "이자", "연금", "증권", "투자", "펀드"]),
]
_CATEGORY_NAMES = [c[0] for c in _CATEGORIES]

# 이상 지출 감지 기준: 이번 달이 직전 2개월 평균의 N배 이상
_ALERT_RATIO = 1.5
# 경고 대상 최소 금액 (너무 소액은 무시)
_MIN_AMOUNT_FOR_ALERT = 10_000


class SpendingPatternAgent(FeatureExecutorBase):
    """월별 지출 패턴을 분석하고 이상 지출 항목을 경고한다."""

    def execute(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        customer_no = request.customer_no or "1"
        today = date.today()

        # 이번 달 1일 ~ 오늘
        this_month_start = today.replace(day=1)
        # 직전 2개월 범위 (60일 전부터 이번 달 1일 전까지)
        two_months_ago = (this_month_start - timedelta(days=1)).replace(day=1)
        cutoff = (two_months_ago - timedelta(days=1)).replace(day=1)

        tx_rows = self._fetch_outflow_transactions(customer_no, cutoff)

        if not tx_rows:
            return ChatbotFeatureExecuteResponse(
                feature_code=FEATURE_CODE,
                status="EMPTY",
                message="최근 3개월 출금 내역이 없어 지출 패턴을 분석할 수 없습니다.",
                data=[],
            )

        # 월별 × 카테고리별 집계
        # key: (year_month_str, category_name) → total_amount
        monthly_category: dict[tuple[str, str], float] = defaultdict(float)

        for row in tx_rows:
            tx_date = self._parse_date(row.get("transaction_at") or row.get("created_at"))
            if tx_date is None:
                continue
            amount = float(row.get("amount") or 0)
            summary = (str(row.get("transaction_summary") or "") + " " + str(row.get("transaction_memo") or "")).lower()
            category = self._classify_category(summary)
            ym = tx_date.strftime("%Y-%m")
            monthly_category[(ym, category)] += amount

        this_ym = today.strftime("%Y-%m")
        prev_months = sorted({ym for ym, _ in monthly_category.keys() if ym != this_ym})[-2:]

        alerts = self._detect_alerts(monthly_category, this_ym, prev_months)
        summary_by_category = self._summary_this_month(monthly_category, this_ym)
        total_this_month = sum(summary_by_category.values())

        message = self._format_message(alerts, summary_by_category, total_this_month, this_ym, prev_months)

        return ChatbotFeatureExecuteResponse(
            feature_code=FEATURE_CODE,
            status="OK",
            message=message,
            data=[
                {"row_type": "alert", **a} for a in alerts
            ] + [
                {"row_type": "monthly_summary", "year_month": this_ym, "category": cat, "amount": amt}
                for cat, amt in summary_by_category.items()
            ],
        )

    def _fetch_outflow_transactions(self, customer_no: str, cutoff: date) -> list[dict[str, Any]]:
        """3개월치 출금 트랜잭션 조회 (본인 계좌 간 이체 제외)."""
        accounts = self._rows(
            "SELECT account_id FROM deposit_accounts WHERE customer_id = :cno",
            {"cno": customer_no},
        )
        if not accounts:
            return []

        id_list = ",".join(str(a["account_id"]) for a in accounts)
        all_ids = {int(a["account_id"]) for a in accounts}

        rows = self._rows(
            f"""
            SELECT t.transaction_type,
                   t.direction_type,
                   t.amount,
                   t.transaction_summary,
                   t.transaction_memo,
                   t.transaction_at,
                   t.created_at,
                   t.counterparty_account_id,
                   t.counterparty_account_no,
                   t.status
              FROM deposit_transactions t
             WHERE t.account_id IN ({id_list})
               AND COALESCE(t.transaction_at, t.created_at) >= :cutoff
               AND t.direction_type = 'OUT'
             ORDER BY COALESCE(t.transaction_at, t.created_at)
            """,
            {"cutoff": cutoff.strftime("%Y-%m-%d")},
        )

        # 내부 이체 제외
        own_ids = all_ids
        filtered = []
        for r in rows:
            if str(r.get("status") or "").upper() not in ("", "SUCCESS", "COMPLETED", "NORMAL"):
                continue
            cp_id = r.get("counterparty_account_id")
            if cp_id is not None and int(cp_id) in own_ids:
                continue
            filtered.append(r)
        return filtered

    def _classify_category(self, text: str) -> str:
        for category_name, keywords in _CATEGORIES:
            if any(kw in text for kw in keywords):
                return category_name
        return "기타"

    def _detect_alerts(
        self,
        monthly_category: dict[tuple[str, str], float],
        this_ym: str,
        prev_months: list[str],
    ) -> list[dict[str, Any]]:
        if not prev_months:
            return []

        alerts = []
        all_categories = set(cat for _, cat in monthly_category.keys())

        for category in all_categories:
            this_amount = monthly_category.get((this_ym, category), 0.0)
            if this_amount < _MIN_AMOUNT_FOR_ALERT:
                continue

            prev_amounts = [monthly_category.get((ym, category), 0.0) for ym in prev_months]
            prev_avg = sum(prev_amounts) / len(prev_amounts) if prev_amounts else 0.0

            if prev_avg < _MIN_AMOUNT_FOR_ALERT:
                # 이전 달에 지출이 없다가 이번 달에 처음 발생한 경우 → 신규 지출로 별도 처리
                if this_amount >= _MIN_AMOUNT_FOR_ALERT * 5:
                    alerts.append({
                        "category": category,
                        "this_month_amount": this_amount,
                        "prev_avg_amount": prev_avg,
                        "ratio": None,
                        "alert_type": "NEW_SPENDING",
                        "message": f"이번 달 {category} 지출({this_amount:,.0f}원)이 새로 발생했습니다.",
                    })
                continue

            ratio = this_amount / prev_avg
            if ratio >= _ALERT_RATIO:
                ratio_text = f"{ratio:.1f}배" if ratio < 10 else "10배 이상"
                alerts.append({
                    "category": category,
                    "this_month_amount": this_amount,
                    "prev_avg_amount": prev_avg,
                    "ratio": round(ratio, 2),
                    "alert_type": "SPIKE",
                    "message": (
                        f"이번 달 {category} 지출({this_amount:,.0f}원)이 "
                        f"평소({prev_avg:,.0f}원)의 {ratio_text}입니다."
                    ),
                })

        # 비율 높은 순으로 정렬
        alerts.sort(key=lambda a: (a.get("ratio") or 99), reverse=True)
        return alerts

    def _summary_this_month(
        self,
        monthly_category: dict[tuple[str, str], float],
        this_ym: str,
    ) -> dict[str, float]:
        result: dict[str, float] = {}
        for (ym, cat), amt in monthly_category.items():
            if ym == this_ym and amt > 0:
                result[cat] = result.get(cat, 0.0) + amt
        return dict(sorted(result.items(), key=lambda x: x[1], reverse=True))

    def _format_message(
        self,
        alerts: list[dict[str, Any]],
        summary: dict[str, float],
        total: float,
        this_ym: str,
        prev_months: list[str],
    ) -> str:
        lines = ["[지출 패턴 분석]"]
        lines.append(f"분석 기준: {this_ym} (이번 달) vs 직전 {len(prev_months)}개월 평균\n")

        if alerts:
            lines.append("⚠ 이상 지출 감지:")
            for a in alerts[:5]:
                lines.append(f"  • {a['message']}")
        else:
            lines.append("이번 달 특이한 지출 변동은 없습니다.")

        if summary:
            lines.append("\n이번 달 카테고리별 지출:")
            for cat, amt in list(summary.items())[:6]:
                lines.append(f"  • {cat}: {amt:,.0f}원")
            lines.append(f"  합계: {total:,.0f}원")

        return "\n".join(lines)

    def _parse_date(self, value: Any) -> date | None:
        if value is None:
            return None
        if isinstance(value, datetime):
            return value.date()
        if isinstance(value, date):
            return value
        text_value = str(value).strip()
        for fmt in ("%Y-%m-%d %H:%M:%S%z", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
            try:
                return datetime.strptime(text_value[:19], fmt[:len(text_value[:19])]).date()
            except ValueError:
                continue
        try:
            return datetime.fromisoformat(text_value).date()
        except (ValueError, TypeError):
            return None

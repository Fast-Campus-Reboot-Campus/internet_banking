"""SAVINGS_GOAL feature 단위 테스트."""
import pytest
from unittest.mock import MagicMock, patch

from app.features.savings_goal import (
    _parse_amount,
    _parse_months,
    _calc_savings_maturity,
    _calc_deposit_maturity,
    _has_lump_sum_intent,
    _SESSION,
    SavingsGoalFeatureExecutor,
)
from app.schemas import ChatbotFeatureExecuteRequest


# ── 1. 파서 ─────────────────────────────────────────────────────────────────

class TestParseAmount:
    def test_만원(self):
        assert _parse_amount("500만원") == 5_000_000

    def test_억(self):
        assert _parse_amount("1억") == 1_0000_0000

    def test_순수숫자(self):
        assert _parse_amount("5000000") == 5_000_000

    def test_천(self):
        assert _parse_amount("500천원") == 500_000

    def test_없음(self):
        assert _parse_amount("돈") is None


class TestParseMonths:
    def test_년(self):
        assert _parse_months("1년") == 12

    def test_개월(self):
        assert _parse_months("6개월") == 6

    def test_년_개월(self):
        assert _parse_months("2년") == 24

    def test_달(self):
        assert _parse_months("3달") == 3

    def test_없음(self):
        assert _parse_months("언젠가") is None


# ── 3. 세션 격리 ─────────────────────────────────────────────────────────────

class TestSessionIsolation:
    def setup_method(self):
        _SESSION.clear()

    def test_sessions_do_not_mix(self):
        _SESSION[1] = {"stage": "ASKED_MONTHLY", "goal_amount": 5_000_000, "goal_months": 12}
        _SESSION[2] = {"stage": "ASKED_MONTHLY", "goal_amount": 10_000_000, "goal_months": 24}
        del _SESSION[1]
        assert _SESSION.get(1) is None
        assert _SESSION.get(2) is not None
        assert _SESSION[2]["goal_amount"] == 10_000_000

    def test_session_stage_asked_monthly_on_first_turn(self):
        """첫 입력 후 stage가 ASKED_MONTHLY로 저장되는지."""
        db = MagicMock()
        db.execute.return_value.mappings.return_value.all.return_value = []
        executor = SavingsGoalFeatureExecutor(db=db)
        req = ChatbotFeatureExecuteRequest(
            query="1년 동안 500만원 모으고 싶어",
            chatbot_consultation_id=99,
        )
        result = executor.execute_savings_goal(req)
        assert _SESSION.get(99) is not None
        assert _SESSION[99]["stage"] == "ASKED_MONTHLY"
        assert _SESSION[99]["goal_amount"] == 5_000_000
        assert _SESSION[99]["goal_months"] == 12
        assert result.status == "NEED_INFO"

    def test_second_turn_continues_session(self):
        """두 번째 입력이 같은 세션에서 이어지는지."""
        _SESSION[99] = {
            "stage": "ASKED_MONTHLY",
            "goal_amount": 5_000_000,
            "goal_months": 12,
        }
        db = MagicMock()
        db.execute.return_value.mappings.return_value.all.return_value = []
        executor = SavingsGoalFeatureExecutor(db=db)
        req = ChatbotFeatureExecuteRequest(
            query="월 30만원요",
            chatbot_consultation_id=99,
        )
        result = executor.execute_savings_goal(req)
        # 세션 소비 후 삭제
        assert _SESSION.get(99) is None
        assert result.feature_code == "SAVINGS_GOAL"


# ── 6. 이자 계산식 ───────────────────────────────────────────────────────────

class TestInterestCalc:
    def test_savings_maturity_positive(self):
        """적금 만기액 > 원금 합계."""
        result = _calc_savings_maturity(300_000, 3.0, 12)
        assert result > 300_000 * 12

    def test_deposit_maturity_simple_interest(self):
        """예금 단리: 500만 × 3% × 1년 = 15만 이자."""
        result = _calc_deposit_maturity(5_000_000, 3.0, 12)
        assert abs(result - 5_150_000) < 1  # 단리 정확값

    def test_zero_rate(self):
        """금리 0%이면 만기액 = 원금."""
        assert _calc_savings_maturity(100_000, 0, 12) == 1_200_000
        assert _calc_deposit_maturity(1_000_000, 0, 12) == 1_000_000


# ── 8. 상품 없을 때 안내 ─────────────────────────────────────────────────────

class TestNoProducts:
    def setup_method(self):
        _SESSION.clear()

    def test_no_products_returns_ok_with_empty_data(self):
        """DB에 상품이 없어도 status OK + 안내 메시지."""
        _SESSION[55] = {
            "stage": "ASKED_MONTHLY",
            "goal_amount": 5_000_000,
            "goal_months": 12,
        }
        db = MagicMock()
        db.execute.return_value.mappings.return_value.all.return_value = []
        executor = SavingsGoalFeatureExecutor(db=db)
        req = ChatbotFeatureExecuteRequest(
            query="월 30만원",
            chatbot_consultation_id=55,
        )
        result = executor.execute_savings_goal(req)
        assert result.status == "OK"
        assert "달성" in result.message or "조건" in result.message


# ── 목돈 감지 ────────────────────────────────────────────────────────────────

class TestLumpSumDetect:
    def test_lump_detected(self):
        assert _has_lump_sum_intent("목돈 300만원이요") is True
        assert _has_lump_sum_intent("한 번에 500만원 넣을게요") is True

    def test_monthly_not_lump(self):
        assert _has_lump_sum_intent("월 30만원요") is False

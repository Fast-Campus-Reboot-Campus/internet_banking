class LlmHandoffAdapter:
    process_method_code = "BP002"

    def answer(self, message: str) -> str:
        return (
            "시나리오로 즉시 처리하기 어려운 문의입니다. "
            "LLM 응답 검증 레이어가 연결되기 전까지는 상담사 연결을 권장합니다."
        )

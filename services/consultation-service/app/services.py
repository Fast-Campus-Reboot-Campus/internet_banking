from collections.abc import Callable
from datetime import datetime, timezone
from typing import Any

from sqlalchemy import bindparam, or_, select, text
from sqlalchemy.orm import Session, aliased

from app.kafka import KafkaEventPublisher
from app.llm import LlmHandoffAdapter
from app.models import (
    ChatConsultation,
    ChatMessageHistory,
    ChatbotConsultation,
    ChatbotNode,
    ChatbotNodeButton,
    ChatbotNodeFlow,
    ChatbotScenario,
    Consultation,
)
from app.schemas import (
    ButtonResponse,
    ChatbotCategoryResponse,
    ChatbotFeatureExecuteRequest,
    ChatbotFeatureExecuteResponse,
    ChatbotFeatureResponse,
    ChatbotMessageResponse,
    ChatbotStartResponse,
)

CODE_RECEPTION_METHOD_CHATBOT = 1
CODE_INQUIRY_PRODUCT = 1
CODE_RECEPTION_CHANNEL_CHAT = 1
CODE_CONSULTATION_STATUS_OPEN = 1
CODE_SCENARIO_TYPE_DEFAULT = 1
CODE_CATEGORY_PRODUCT_ADVICE = 1
CODE_NODE_TYPE_MESSAGE = 1
CODE_PROCESS_SCENARIO = 1
CODE_PROCESS_LLM = 2
CODE_SENDER_USER = 1
CODE_SENDER_BOT = 2
CODE_MESSAGE_TYPE_TEXT = 1


class ChatbotService:
    def __init__(self, db: Session, events: KafkaEventPublisher, llm: LlmHandoffAdapter):
        self.db = db
        self.events = events
        self.llm = llm

    async def start(self, customer_no: str, entry_screen: str, app_version: str) -> ChatbotStartResponse:
        scenario = self._get_active_scenario()
        if not scenario:
            scenario = self._ensure_default_scenario()
            self.db.commit()
        first_node = self._get_first_node(scenario.scenario_id)
        if not first_node:
            raise ValueError("활성화된 챗봇 시나리오가 없습니다.")

        consultation = Consultation(
            customer_no=customer_no,
            reception_method_code_id=CODE_RECEPTION_METHOD_CHATBOT,
            inquiry_type_code_id=CODE_INQUIRY_PRODUCT,
            reception_channel_code_id=CODE_RECEPTION_CHANNEL_CHAT,
            content_summary="챗봇 상담 시작",
            status_code_id=CODE_CONSULTATION_STATUS_OPEN,
            active_yn="Y",
        )
        self.db.add(consultation)
        self.db.flush()

        chatbot = ChatbotConsultation(
            consultation_id=consultation.consultation_id,
            scenario_id=scenario.scenario_id,
            process_method_code_id=CODE_PROCESS_SCENARIO,
            initial_intent=scenario.scenario_name,
            entry_screen=entry_screen,
            app_version=app_version,
        )
        self.db.add(chatbot)
        self.db.flush()
        self._record_message(
            chatbot,
            first_node,
            CODE_SENDER_BOT,
            first_node.response_message,
            None,
            CODE_PROCESS_SCENARIO,
        )
        self.db.commit()
        self.db.refresh(chatbot)

        await self.events.publish(
            "ChatbotConsultationStarted",
            {
                "consultationId": consultation.consultation_id,
                "chatbotConsultationId": chatbot.chatbot_consultation_id,
                "customerNo": customer_no,
            },
        )
        return ChatbotStartResponse(
            consultation_id=consultation.consultation_id,
            chatbot_consultation_id=chatbot.chatbot_consultation_id,
            node_id=first_node.node_id,
            message=first_node.response_message,
            buttons=self._button_responses(first_node.node_id),
        )

    async def handle_message(
        self,
        chatbot_consultation_id: int,
        message: str,
        button_value: str | None,
    ) -> ChatbotMessageResponse:
        chatbot = self.db.get(ChatbotConsultation, chatbot_consultation_id)
        if not chatbot:
            raise ValueError("챗봇 상담을 찾을 수 없습니다.")

        current_node_id = self._latest_node_id(chatbot.chatbot_consultation_id)
        self._record_message(
            chatbot,
            None,
            CODE_SENDER_USER,
            message or button_value or "",
            button_value,
            None,
        )
        next_node = self._resolve_next_node(chatbot.scenario_id, current_node_id, button_value)

        process_method = "SCENARIO"
        agent_transfer_required = False
        if next_node:
            response_message = next_node.response_message
            node_id = next_node.node_id
            process_code = CODE_PROCESS_SCENARIO
            if self._is_agent_node(next_node):
                process_method = "BP002_LLM"
                process_code = CODE_PROCESS_LLM
                response_message = self.llm.answer(message)
                agent_transfer_required = True
                chatbot.agent_connected_yn = "Y"
                self._open_chat_consultation(chatbot)
        else:
            process_method = "BP002_LLM"
            process_code = CODE_PROCESS_LLM
            response_message = self.llm.answer(message)
            agent_transfer_required = True
            node_id = current_node_id or 0
            chatbot.agent_connected_yn = "Y"
            self._open_chat_consultation(chatbot)

        chatbot.total_turn_count += 1
        self._record_message(chatbot, next_node, CODE_SENDER_BOT, response_message, None, process_code)
        self.db.commit()

        await self.events.publish(
            "ChatbotMessageHandled",
            {
                "chatbotConsultationId": chatbot.chatbot_consultation_id,
                "message": message,
                "processMethod": process_method,
                "agentTransferRequired": agent_transfer_required,
            },
        )
        if agent_transfer_required:
            await self.events.publish(
                "ChatbotAgentTransferRequested",
                {
                    "chatbotConsultationId": chatbot.chatbot_consultation_id,
                    "consultationId": chatbot.consultation_id,
                },
            )

        return ChatbotMessageResponse(
            consultation_id=chatbot.consultation_id,
            chatbot_consultation_id=chatbot.chatbot_consultation_id,
            node_id=node_id,
            message=response_message,
            buttons=self._button_responses(node_id) if node_id else [],
            process_method=process_method,
            agent_transfer_required=agent_transfer_required,
        )

    def categories(self) -> list[ChatbotCategoryResponse]:
        names = {
            "PRODUCT_ADVICE": ("금융상품 상담", "금융상품 관련 질문 전체"),
            "USER_FINANCE": ("사용자 금융정보 조회", "사용자 본인 금융정보 조회"),
            "STAFF_SUPPORT": ("직원 업무 지원", "직원용 내부 업무 지원"),
        }
        grouped: dict[str, list[str]] = {code: [] for code in names}
        for feature in self.features():
            grouped.setdefault(feature.category_code, []).append(feature.code)
        return [
            ChatbotCategoryResponse(
                code=code,
                name=name,
                description=description,
                features=grouped.get(code, []),
            )
            for code, (name, description) in names.items()
        ]

    def features(self) -> list[ChatbotFeatureResponse]:
        return [
            ChatbotFeatureResponse(
                code="PRODUCT_GUIDE",
                category_code="PRODUCT_ADVICE",
                name="예금/적금/청약 상품 안내",
                summary="수신 상품 목록과 핵심 조건을 안내합니다.",
                sample_questions=["예금 상품 알려줘", "청약 상품 가입 조건 알려줘"],
                api_status="MOCK_READY",
            ),
            ChatbotFeatureResponse(
                code="RATE_GUIDE",
                category_code="PRODUCT_ADVICE",
                name="금리/우대금리 설명",
                summary="기본금리, 우대금리, 최종 적용금리 설명 흐름을 제공합니다.",
                sample_questions=["우대금리 조건 알려줘", "적금 금리 설명해줘"],
                api_status="MOCK_READY",
            ),
            ChatbotFeatureResponse(
                code="JOIN_CONDITION",
                category_code="PRODUCT_ADVICE",
                name="가입 조건 안내",
                summary="대상 고객, 가입 채널, 기간, 금액 조건을 안내합니다.",
                sample_questions=["이 상품 가입할 수 있어?", "모바일로 가입 가능해?"],
                api_status="MOCK_READY",
            ),
            ChatbotFeatureResponse(
                code="PRODUCT_COMPARE",
                category_code="PRODUCT_ADVICE",
                name="상품 비교",
                summary="상품별 금리, 기간, 한도, 우대 조건 비교 응답을 준비합니다.",
                sample_questions=["정기예금과 적금 비교해줘", "청약이랑 예금 차이 알려줘"],
                api_status="MOCK_READY",
            ),
            ChatbotFeatureResponse(
                code="TERMS_RAG",
                category_code="PRODUCT_ADVICE",
                name="상품 설명서/약관 기반 RAG 응답",
                summary="약관 및 상품 설명서 검색 기반 답변 연결 지점입니다.",
                sample_questions=["중도해지 약관 알려줘", "상품설명서에서 수수료 찾아줘"],
                api_status="RAG_PENDING",
            ),
            ChatbotFeatureResponse(
                code="FAQ",
                category_code="PRODUCT_ADVICE",
                name="FAQ 응답",
                summary="반복 문의에 대한 고정 답변을 제공합니다.",
                sample_questions=["자주 묻는 질문 보여줘", "예금 FAQ 알려줘"],
                api_status="MOCK_READY",
            ),
            ChatbotFeatureResponse(
                code="MY_ACCOUNTS",
                category_code="USER_FINANCE",
                name="내 계좌 조회",
                summary="본인 인증 후 계좌 목록과 잔액 조회로 연결합니다.",
                sample_questions=["내 계좌 보여줘", "잔액 조회해줘"],
                api_status="AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="MY_PRODUCTS",
                category_code="USER_FINANCE",
                name="가입 상품 조회",
                summary="고객이 가입한 예금, 적금, 청약 상품을 조회합니다.",
                sample_questions=["내 가입 상품 알려줘", "내 적금 상품 보여줘"],
                api_status="AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="CONTRACT_STATUS",
                category_code="USER_FINANCE",
                name="계약 상태 조회",
                summary="계약 상태, 시작일, 만기일, 해지 가능 여부를 조회합니다.",
                sample_questions=["계약 상태 알려줘", "내 예금 계약 살아있어?"],
                api_status="AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="MATURITY_SCHEDULE",
                category_code="USER_FINANCE",
                name="만기 예정 조회",
                summary="만기 예정 상품과 예상 만기일을 조회합니다.",
                sample_questions=["곧 만기되는 상품 있어?", "만기일 알려줘"],
                api_status="AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="INTEREST_HISTORY",
                category_code="USER_FINANCE",
                name="이자 내역 조회",
                summary="이자 지급 및 예상 이자 내역 조회로 연결합니다.",
                sample_questions=["이자 내역 보여줘", "이번 달 이자 얼마야?"],
                api_status="AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="STAFF_CUSTOMER",
                category_code="STAFF_SUPPORT",
                name="고객 정보 조회",
                summary="직원 권한 확인 후 고객 기본 정보를 조회합니다.",
                sample_questions=["고객 연락처 확인"],
                api_status="STAFF_AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="STAFF_CONTRACT",
                category_code="STAFF_SUPPORT",
                name="고객 계약 조회",
                summary="직원용 고객 계약 목록 및 계약 상세 조회로 연결합니다.",
                sample_questions=["고객 계약 보여줘", "계약 상태 확인"],
                api_status="STAFF_AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="STAFF_ACCOUNT",
                category_code="STAFF_SUPPORT",
                name="고객 계좌 조회",
                summary="직원용 고객 계좌 목록과 상태 조회로 연결합니다.",
                sample_questions=["계좌 상태 확인"],
                api_status="STAFF_AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="STAFF_TRANSFER_FLOW",
                category_code="STAFF_SUPPORT",
                name="고객 이체 흐름 조회",
                summary="거래 원장 기반으로 이체 유형, 상대방 정보, 진행 상태를 추적합니다.",
                sample_questions=["이체 흐름 추적", "거래 진행 상태 확인"],
                api_status="STAFF_AUTH_REQUIRED",
            ),
            ChatbotFeatureResponse(
                code="STAFF_CONSULTATION_HISTORY",
                category_code="STAFF_SUPPORT",
                name="상담 이력 조회",
                summary="고객의 과거 상담 이력과 챗봇 전환 이력을 조회합니다.",
                sample_questions=["상담 이력 보여줘", "이전 문의 확인"],
                api_status="STAFF_AUTH_REQUIRED",
            ),
        ]

    def feature_detail(self, feature_code: str) -> ChatbotFeatureResponse | None:
        return next((feature for feature in self.features() if feature.code == feature_code), None)

    def execute_feature(
        self,
        feature_code: str,
        request: ChatbotFeatureExecuteRequest,
    ) -> ChatbotFeatureExecuteResponse:
        handlers: dict[str, Callable[[ChatbotFeatureExecuteRequest], ChatbotFeatureExecuteResponse]] = {
            "PRODUCT_GUIDE": self._execute_product_guide,
            "RATE_GUIDE": self._execute_rate_guide,
            "JOIN_CONDITION": self._execute_join_condition,
            "PRODUCT_COMPARE": self._execute_product_compare,
            "TERMS_RAG": self._execute_terms_search,
            "FAQ": self._execute_faq,
            "MY_ACCOUNTS": self._execute_my_accounts,
            "MY_PRODUCTS": lambda req: self._execute_customer_contracts(
                req, "MY_PRODUCTS", "가입 상품 조회를 완료했습니다.", "조회된 가입 상품이 없습니다."
            ),
            "CONTRACT_STATUS": lambda req: self._execute_customer_contracts(
                req, "CONTRACT_STATUS", "계약 상태 조회를 완료했습니다.", "조회된 계약 상태가 없습니다."
            ),
            "MATURITY_SCHEDULE": self._execute_maturity_schedule,
            "INTEREST_HISTORY": self._execute_interest_history,
            "STAFF_CUSTOMER": self._execute_staff_customer,
            "STAFF_CONTRACT": lambda req: self._execute_customer_contracts(
                req,
                "STAFF_CONTRACT",
                "직원용 고객 계약 조회를 완료했습니다.",
                "조회된 고객 계약이 없습니다.",
                requires_staff_auth=True,
            ),
            "STAFF_ACCOUNT": self._execute_staff_account,
            "STAFF_TRANSFER_FLOW": self._execute_staff_transfer_flow,
            "STAFF_CONSULTATION_HISTORY": self._execute_staff_consultation_history,
        }
        handler = handlers.get(feature_code)
        if not handler:
            return ChatbotFeatureExecuteResponse(
                feature_code=feature_code,
                status="NOT_FOUND",
                message="지원하지 않는 챗봇 기능입니다.",
            )
        return handler(request)

    def _execute_product_guide(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        rows = self._rows(
            """
            SELECT banking_product_id AS product_id,
                   deposit_product_name AS product_name,
                   deposit_product_type AS product_type,
                   description,
                   base_interest_rate,
                   min_join_amount,
                   max_join_amount,
                   min_period_month,
                   max_period_month,
                   deposit_product_status AS product_status
              FROM deposit_banking_products
             ORDER BY banking_product_id
             LIMIT 20
            """
        )
        return self._data_response("PRODUCT_GUIDE", rows, "상품 안내 조회를 완료했습니다.", "등록된 수신 상품 데이터가 없습니다.")

    def _execute_rate_guide(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        rows = self._rows(
            """
            SELECT r.rate_id,
                   r.banking_product_id AS product_id,
                   p.deposit_product_name AS product_name,
                   r.rate_type,
                   r.minimum_contract_period,
                   r.maximum_contract_period,
                   r.rate AS interest_rate,
                   r.condition_description
              FROM banking_deposit_product_interest_rates r
              JOIN deposit_banking_products p ON p.banking_product_id = r.banking_product_id
             ORDER BY r.banking_product_id, r.rate_id
             LIMIT 20
            """
        )
        return self._data_response("RATE_GUIDE", rows, "금리/우대금리 조회를 완료했습니다.", "등록된 금리 데이터가 없습니다.")

    def _execute_join_condition(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        rows = self._rows(
            """
            SELECT banking_product_id AS product_id,
                   deposit_product_name AS product_name,
                   min_join_amount,
                   max_join_amount,
                   min_period_month,
                   max_period_month,
                   is_early_termination_allowed,
                   is_tax_benefit_available,
                   deposit_product_status AS product_status
              FROM deposit_banking_products
             ORDER BY banking_product_id
             LIMIT 20
            """
        )
        return self._data_response("JOIN_CONDITION", rows, "가입 조건 조회를 완료했습니다.", "등록된 가입 조건 데이터가 없습니다.")

    def _execute_product_compare(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        product_ids = request.compare_product_ids or ([request.product_id] if request.product_id else [])
        if product_ids:
            rows = self._rows(
                """
                SELECT banking_product_id AS product_id,
                       deposit_product_name AS product_name,
                       deposit_product_type AS product_type,
                       base_interest_rate,
                       min_join_amount,
                       max_join_amount,
                       min_period_month,
                       max_period_month
                  FROM deposit_banking_products
                 WHERE banking_product_id IN :product_ids
                 ORDER BY banking_product_id
                """,
                {"product_ids": tuple(product_ids)},
                expanding_params=("product_ids",),
            )
        else:
            rows = self._rows(
                """
                SELECT banking_product_id AS product_id,
                       deposit_product_name AS product_name,
                       deposit_product_type AS product_type,
                       base_interest_rate,
                       min_join_amount,
                       max_join_amount,
                       min_period_month,
                       max_period_month
                  FROM deposit_banking_products
                 ORDER BY base_interest_rate DESC, banking_product_id
                 LIMIT 5
                """
            )
        return self._data_response("PRODUCT_COMPARE", rows, "상품 비교 조회를 완료했습니다.", "비교할 상품 데이터가 없습니다.")

    def _execute_terms_search(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        query = (request.query or "").strip()
        like = f"%{query}%" if query else "%%"
        rows = self._rows(
            """
            SELECT special_term_id,
                   special_term_name,
                   special_term_content,
                   special_term_summary,
                   is_required,
                   status
              FROM deposit_special_terms
             WHERE special_term_name LIKE :query
                OR special_term_content LIKE :query
                OR special_term_summary LIKE :query
             ORDER BY special_term_id
             LIMIT 10
            """,
            {"query": like},
        )
        return self._data_response("TERMS_RAG", rows, "약관 검색을 완료했습니다.", "검색 가능한 약관 데이터가 없습니다.")

    def _execute_faq(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        return ChatbotFeatureExecuteResponse(
            feature_code="FAQ",
            status="OK",
            message="수신 상품 FAQ 응답입니다.",
            data=[
                {"question": "예금과 적금의 차이는 무엇인가요?", "answer": "예금은 목돈을 맡기고, 적금은 정해진 주기로 납입하는 상품입니다."},
                {"question": "우대금리는 어떻게 적용되나요?", "answer": "상품별 우대 조건 충족 여부에 따라 기본금리에 추가됩니다."},
                {"question": "중도해지하면 어떻게 되나요?", "answer": "상품 약관의 중도해지이율이 적용될 수 있어 약관 확인이 필요합니다."},
            ],
        )

    def _execute_my_accounts(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("MY_ACCOUNTS", "계좌 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._account_rows(request.customer_no)
        return self._data_response(
            "MY_ACCOUNTS", rows, "내 계좌 조회를 완료했습니다.", "조회된 계좌가 없습니다.", requires_auth=True
        )

    def _execute_maturity_schedule(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no:
            return self._auth_required("MATURITY_SCHEDULE", "만기 예정 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._contract_rows(request.customer_no)
        return self._data_response(
            "MATURITY_SCHEDULE", rows, "만기 예정 조회를 완료했습니다.", "조회된 만기 예정 계약이 없습니다.", requires_auth=True
        )

    def _execute_interest_history(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
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

    def _execute_staff_customer(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no or not request.staff_id:
            return self._staff_auth_required("STAFF_CUSTOMER", "직원 고객 정보 조회에는 고객번호와 직원 권한이 필요합니다.")
        rows = self._account_rows(request.customer_no)
        return self._data_response(
            "STAFF_CUSTOMER", rows, "직원용 고객 정보 조회를 완료했습니다.", "조회된 고객 정보가 없습니다.", requires_staff_auth=True
        )

    def _execute_staff_account(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no or not request.staff_id:
            return self._staff_auth_required("STAFF_ACCOUNT", "직원 고객 계좌 조회에는 고객번호와 직원 권한이 필요합니다.")
        rows = self._account_rows(request.customer_no)
        return self._data_response(
            "STAFF_ACCOUNT", rows, "직원용 고객 계좌 조회를 완료했습니다.", "조회된 고객 계좌가 없습니다.", requires_staff_auth=True
        )

    def _execute_staff_transfer_flow(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no or not request.staff_id:
            return self._staff_auth_required("STAFF_TRANSFER_FLOW", "이체 흐름 조회에는 고객번호와 직원 권한이 필요합니다.")
        rows = self._rows(
            """
            SELECT t.transaction_id,
                   t.transaction_number,
                   a.account_number,
                   a.customer_id AS customer_no,
                   t.transaction_type,
                   t.status AS transaction_status,
                   t.amount,
                   t.created_at
              FROM deposit_transactions t
              JOIN deposit_accounts a ON a.account_id = t.account_id
             WHERE a.customer_id = :customer_no
             ORDER BY t.transaction_id DESC
             LIMIT 20
            """,
            {"customer_no": request.customer_no},
        )
        return self._data_response(
            "STAFF_TRANSFER_FLOW", rows, "이체 흐름 조회를 완료했습니다.", "조회된 이체 내역이 없습니다.", requires_staff_auth=True
        )

    def _execute_staff_consultation_history(self, request: ChatbotFeatureExecuteRequest) -> ChatbotFeatureExecuteResponse:
        if not request.customer_no or not request.staff_id:
            return self._staff_auth_required("STAFF_CONSULTATION_HISTORY", "상담 이력 조회에는 고객번호와 직원 권한이 필요합니다.")
        rows = self._rows(
            """
            SELECT consultation_id,
                   customer_no,
                   content_summary,
                   status_code_id,
                   answer_summary,
                   consulted_at,
                   completed_at
              FROM consultation
             WHERE customer_no = :customer_no
             ORDER BY consultation_id DESC
             LIMIT 20
            """,
            {"customer_no": request.customer_no},
        )
        return self._data_response(
            "STAFF_CONSULTATION_HISTORY", rows, "상담 이력 조회를 완료했습니다.", "조회된 상담 이력이 없습니다.", requires_staff_auth=True
        )

    def _execute_customer_contracts(
        self,
        request: ChatbotFeatureExecuteRequest,
        feature_code: str,
        ok_message: str,
        empty_message: str,
        requires_staff_auth: bool = False,
    ) -> ChatbotFeatureExecuteResponse:
        if requires_staff_auth and (not request.customer_no or not request.staff_id):
            return self._staff_auth_required(feature_code, "계약 조회에는 고객번호와 직원 권한이 필요합니다.")
        if not requires_staff_auth and not request.customer_no:
            return self._auth_required(feature_code, "계약 조회에는 고객번호와 본인 인증이 필요합니다.")
        rows = self._contract_rows(request.customer_no or "")
        return self._data_response(
            feature_code,
            rows,
            ok_message,
            empty_message,
            requires_auth=not requires_staff_auth,
            requires_staff_auth=requires_staff_auth,
        )

    def _account_rows(self, customer_no: str) -> list[dict[str, Any]]:
        return self._rows(
            """
            SELECT account_id,
                   account_number,
                   customer_id AS customer_no,
                   account_type,
                   account_alias,
                   balance,
                   currency,
                   account_status,
                   opened_at,
                   closed_at
              FROM deposit_accounts
             WHERE customer_id = :customer_no
             ORDER BY account_id
             LIMIT 20
            """,
            {"customer_no": customer_no},
        )

    def _contract_rows(self, customer_no: str) -> list[dict[str, Any]]:
        return self._rows(
            """
            SELECT c.contract_id,
                   c.contract_number AS contract_no,
                   c.customer_id AS customer_no,
                   c.banking_product_id AS product_id,
                   p.deposit_product_name AS product_name,
                   c.join_amount,
                   c.contract_interest_rate,
                   c.started_at,
                   c.maturity_at,
                   c.contract_status
              FROM deposit_contracts c
              LEFT JOIN deposit_banking_products p ON p.banking_product_id = c.banking_product_id
             WHERE c.customer_id = :customer_no
             ORDER BY c.contract_id
             LIMIT 20
            """,
            {"customer_no": customer_no},
        )

    def _rows(
        self,
        sql: str,
        params: dict[str, Any] | None = None,
        expanding_params: tuple[str, ...] = (),
    ) -> list[dict[str, Any]]:
        try:
            statement = text(sql)
            for param in expanding_params:
                statement = statement.bindparams(bindparam(param, expanding=True))
            result = self.db.execute(statement, params or {})
            return [dict(row._mapping) for row in result]
        except Exception:
            self.db.rollback()
            return []

    def _data_response(
        self,
        feature_code: str,
        rows: list[dict[str, Any]],
        ok_message: str,
        empty_message: str,
        requires_auth: bool = False,
        requires_staff_auth: bool = False,
    ) -> ChatbotFeatureExecuteResponse:
        return ChatbotFeatureExecuteResponse(
            feature_code=feature_code,
            status="OK" if rows else "EMPTY",
            message=ok_message if rows else empty_message,
            data=rows,
            requires_auth=requires_auth,
            requires_staff_auth=requires_staff_auth,
        )

    def _auth_required(self, feature_code: str, message: str) -> ChatbotFeatureExecuteResponse:
        return ChatbotFeatureExecuteResponse(
            feature_code=feature_code,
            status="AUTH_REQUIRED",
            message=message,
            requires_auth=True,
        )

    def _staff_auth_required(self, feature_code: str, message: str) -> ChatbotFeatureExecuteResponse:
        return ChatbotFeatureExecuteResponse(
            feature_code=feature_code,
            status="STAFF_AUTH_REQUIRED",
            message=message,
            requires_staff_auth=True,
        )

    def seed_default_scenario(self) -> tuple[int, int]:
        scenario = self._ensure_default_scenario()
        self.db.commit()
        first = self._get_first_node(scenario.scenario_id)
        return scenario.scenario_id, first.node_id if first else 0

    def _ensure_default_scenario(self) -> ChatbotScenario:
        scenario = self._get_active_scenario()
        if not scenario:
            scenario = ChatbotScenario(
                scenario_name="기본 수신 상담",
                scenario_desc="금융상품 상담, 사용자 금융정보 조회, 직원 업무 지원 챗봇 시나리오",
                scenario_type_code_id=CODE_SCENARIO_TYPE_DEFAULT,
                consultation_category_code_id=CODE_CATEGORY_PRODUCT_ADVICE,
                reception_channel_code_id=CODE_RECEPTION_CHANNEL_CHAT,
                active_yn="Y",
            )
            self.db.add(scenario)
            self.db.flush()

        start = self._ensure_node(
            scenario.scenario_id,
            "상담 시작",
            "안녕하세요. 필요한 상담 유형을 선택해 주세요.",
            1,
        )
        for spec in self._default_flow_specs():
            node = self._ensure_node(
                scenario.scenario_id,
                spec["node_name"],
                spec["response_message"],
                int(spec["sort_order"]),
            )
            self._ensure_button(start.node_id, spec["button_text"], spec["button_value"], int(spec["sort_order"]))
            self._ensure_flow(start.node_id, node.node_id, int(spec["sort_order"]), str(spec["button_value"]))
        self._deactivate_legacy_start_options(start.node_id, {spec["button_value"] for spec in self._default_flow_specs()})
        return scenario

    def _default_flow_specs(self) -> list[dict[str, Any]]:
        return [
            {
                "button_text": "금융상품 상담",
                "button_value": "PRODUCT_ADVICE",
                "node_name": "금융상품 상담",
                "response_message": "예금/적금/청약, 금리, 가입 조건, 상품 비교, 약관 기반 응답과 FAQ를 안내합니다.",
                "sort_order": 1,
            },
            {
                "button_text": "사용자 금융정보 조회",
                "button_value": "USER_FINANCE",
                "node_name": "사용자 금융정보 조회",
                "response_message": "본인 계좌, 가입 상품, 계약 상태, 만기 예정, 이자 내역 조회를 지원합니다.",
                "sort_order": 2,
            },
            {
                "button_text": "직원 업무 지원",
                "button_value": "STAFF_SUPPORT",
                "node_name": "직원 업무 지원",
                "response_message": "직원용 고객 정보, 계약, 계좌, 이체 흐름, 상담 이력 조회를 지원합니다.",
                "sort_order": 3,
            },
            {
                "button_text": "상담사 연결",
                "button_value": "AGENT",
                "node_name": "상담사 연결",
                "response_message": "상담사 연결이 필요한 문의로 접수하겠습니다.",
                "sort_order": 4,
            },
        ]

    def _ensure_node(self, scenario_id: int, node_name: str, response_message: str, sort_order: int) -> ChatbotNode:
        node = self.db.scalars(
            select(ChatbotNode).where(
                ChatbotNode.scenario_id == scenario_id,
                ChatbotNode.node_name == node_name,
            )
        ).first()
        if node:
            return node
        node = ChatbotNode(
            scenario_id=scenario_id,
            node_type_code_id=CODE_NODE_TYPE_MESSAGE,
            node_name=node_name,
            response_message=response_message,
            sort_order=sort_order,
            active_yn="Y",
        )
        self.db.add(node)
        self.db.flush()
        return node

    def _ensure_button(self, node_id: int, button_text: str, button_value: str, sort_order: int) -> None:
        button = self.db.scalars(
            select(ChatbotNodeButton).where(
                ChatbotNodeButton.node_id == node_id,
                ChatbotNodeButton.button_value == button_value,
            )
        ).first()
        if not button:
            self.db.add(
                ChatbotNodeButton(
                    node_id=node_id,
                    button_text=button_text,
                    button_value=button_value,
                    sort_order=sort_order,
                    active_yn="Y",
                )
            )

    def _ensure_flow(self, current_node_id: int, next_node_id: int, sort_order: int, branch_value: str) -> None:
        flow = self.db.scalars(
            select(ChatbotNodeFlow).where(
                ChatbotNodeFlow.current_node_id == current_node_id,
                ChatbotNodeFlow.next_node_id == next_node_id,
            )
        ).first()
        if not flow:
            self.db.add(
                ChatbotNodeFlow(
                    current_node_id=current_node_id,
                    next_node_id=next_node_id,
                    sort_order=sort_order,
                    chatbot_flow_type_cd="BUTTON",
                    branch_criteria_cd="BUTTON_VALUE",
                    branch_value=branch_value,
                    active_yn="Y",
                )
            )

    def _deactivate_legacy_start_options(self, node_id: int, allowed_values: set[str]) -> None:
        for button in self.db.scalars(select(ChatbotNodeButton).where(ChatbotNodeButton.node_id == node_id)).all():
            if button.button_value not in allowed_values:
                button.active_yn = "N"

    def _get_active_scenario(self) -> ChatbotScenario | None:
        return self.db.scalars(
            select(ChatbotScenario)
            .where(ChatbotScenario.active_yn == "Y", ChatbotScenario.scenario_id.is_not(None))
            .order_by((ChatbotScenario.scenario_name == "기본 수신 상담").desc(), ChatbotScenario.scenario_id)
        ).first()

    def _get_first_node(self, scenario_id: int) -> ChatbotNode | None:
        return self.db.scalars(
            select(ChatbotNode)
            .where(ChatbotNode.scenario_id == scenario_id, ChatbotNode.active_yn == "Y")
            .order_by(ChatbotNode.sort_order, ChatbotNode.node_id)
        ).first()

    def _resolve_next_node(
        self,
        scenario_id: int | None,
        current_node_id: int | None,
        button_value: str | None,
    ) -> ChatbotNode | None:
        if not current_node_id or not button_value:
            return None
        NextNode = aliased(ChatbotNode)
        flow = self.db.scalars(
            select(ChatbotNodeFlow)
            .join(NextNode, NextNode.node_id == ChatbotNodeFlow.next_node_id)
            .where(
                ChatbotNodeFlow.current_node_id == current_node_id,
                ChatbotNodeFlow.branch_value == button_value,
                ChatbotNodeFlow.active_yn == "Y",
                NextNode.scenario_id == scenario_id,
            )
            .order_by(ChatbotNodeFlow.sort_order)
        ).first()
        return self.db.get(ChatbotNode, flow.next_node_id) if flow else None

    def _button_responses(self, node_id: int) -> list[ButtonResponse]:
        buttons = self.db.scalars(
            select(ChatbotNodeButton)
            .where(ChatbotNodeButton.node_id == node_id, ChatbotNodeButton.active_yn == "Y")
            .order_by(ChatbotNodeButton.sort_order, ChatbotNodeButton.id)
        ).all()
        return [ButtonResponse(id=button.id, text=button.button_text, value=button.button_value) for button in buttons]

    def _record_message(
        self,
        chatbot: ChatbotConsultation,
        node: ChatbotNode | None,
        sender_type_code_id: int,
        message_content: str,
        button_value: str | None,
        process_method_code_id: int | None,
    ) -> None:
        last_sequence = self.db.execute(
            select(ChatMessageHistory.sequence_no)
            .where(ChatMessageHistory.chatbot_consultation_id == chatbot.chatbot_consultation_id)
            .order_by(ChatMessageHistory.sequence_no.desc())
            .limit(1)
        ).scalar_one_or_none()
        self.db.add(
            ChatMessageHistory(
                chatbot_consultation_id=chatbot.chatbot_consultation_id,
                node_id=node.node_id if node else None,
                sequence_no=(last_sequence or 0) + 1,
                sender_type_code_id=sender_type_code_id,
                message_type_code_id=CODE_MESSAGE_TYPE_TEXT,
                message_content=message_content,
                button_value=button_value,
                process_method_code_id=process_method_code_id,
            )
        )

    def _latest_node_id(self, chatbot_consultation_id: int) -> int | None:
        return self.db.execute(
            select(ChatMessageHistory.node_id)
            .where(ChatMessageHistory.chatbot_consultation_id == chatbot_consultation_id)
            .where(ChatMessageHistory.node_id.is_not(None))
            .order_by(ChatMessageHistory.sequence_no.desc())
            .limit(1)
        ).scalar_one_or_none()

    def _open_chat_consultation(self, chatbot: ChatbotConsultation) -> None:
        exists = self.db.scalars(
            select(ChatConsultation).where(ChatConsultation.chatbot_consultation_id == chatbot.chatbot_consultation_id)
        ).first()
        if not exists:
            self.db.add(
                ChatConsultation(
                    consultation_id=chatbot.consultation_id,
                    chatbot_consultation_id=chatbot.chatbot_consultation_id,
                    total_turn_count=0,
                    active_yn="Y",
                    agent_requested_at=datetime.now(timezone.utc),  # 대기열 조회용
                )
            )

    def _is_agent_node(self, node: ChatbotNode) -> bool:
        return node.node_name == "상담사 연결"


# ──────────────────────────────────────────────────────────────────────────────
# 인간 상담원 채팅 서비스
# ──────────────────────────────────────────────────────────────────────────────

CODE_SENDER_USER = 1
CODE_SENDER_BOT = 2
CODE_SENDER_AGENT = 3

_SENDER_LABEL = {
    CODE_SENDER_USER: "USER",
    CODE_SENDER_BOT: "BOT",
    CODE_SENDER_AGENT: "AGENT",
}


def _chat_status(chat: ChatConsultation) -> str:
    if chat.active_yn == "N":
        return "ENDED"
    if chat.agent_connected_at:
        return "CONNECTED"
    return "WAITING"


class ChatService:
    """상담사 채팅 상담 관리 서비스.

    흐름:
      1. ChatbotService 가 상담사 이관을 감지하면 ChatConsultation 생성 (WAITING)
      2. 상담사가 get_waiting_queue() 로 목록 확인
      3. connect_agent() 로 수락 → CONNECTED
      4. send_message() 로 양방향 메시지 교환
      5. end_chat() 로 종료 → ENDED

    Kafka 이벤트:
      - AgentTransferRequested  (chatbot_service 에서 이미 발행)
      - AgentConnected
      - ChatMessageSent
      - ChatEnded
    """

    def __init__(self, db: Session, events: KafkaEventPublisher):
        self.db = db
        self.events = events

    # ── 조회 ────────────────────────────────────────────────────────────────

    def get_waiting_queue(self) -> list[dict[str, Any]]:
        """상담사 수락 대기 중인 채팅 목록 (JOIN consultation 으로 customer_no 포함)."""
        rows = self.db.execute(
            select(
                ChatConsultation.chat_consultation_id,
                ChatConsultation.consultation_id,
                ChatConsultation.chatbot_consultation_id,
                ChatConsultation.agent_requested_at.label("waiting_since"),
                Consultation.customer_no,
            )
            .join(Consultation, Consultation.consultation_id == ChatConsultation.consultation_id)
            .where(
                ChatConsultation.active_yn == "Y",
                ChatConsultation.agent_connected_at.is_(None),
                ChatConsultation.agent_requested_at.is_not(None),
            )
            .order_by(ChatConsultation.agent_requested_at)
        )
        return [dict(row._mapping) for row in rows]

    def get_consultation(self, chat_consultation_id: int) -> ChatConsultation:
        chat = self.db.get(ChatConsultation, chat_consultation_id)
        if not chat:
            raise ValueError(f"채팅 상담을 찾을 수 없습니다. id={chat_consultation_id}")
        return chat

    def get_messages(self, chat_consultation_id: int) -> list[ChatMessageHistory]:
        """챗봇 메시지 + 상담사 메시지를 통합하여 시간 순으로 반환."""
        chat = self.get_consultation(chat_consultation_id)

        if chat.chatbot_consultation_id:
            condition = or_(
                ChatMessageHistory.chat_consultation_id == chat_consultation_id,
                ChatMessageHistory.chatbot_consultation_id == chat.chatbot_consultation_id,
            )
        else:
            condition = ChatMessageHistory.chat_consultation_id == chat_consultation_id

        return list(
            self.db.scalars(
                select(ChatMessageHistory)
                .where(condition)
                .order_by(ChatMessageHistory.chat_message_history_id)
            ).all()
        )

    # ── 상태 변경 ────────────────────────────────────────────────────────────

    async def connect_agent(self, chat_consultation_id: int, employee_id: int) -> ChatConsultation:
        """상담사가 대기 중인 상담을 수락한다.

        Kafka: AgentConnected 이벤트 발행 (consultation.chat.events)
        """
        chat = self.get_consultation(chat_consultation_id)
        if chat.agent_connected_at:
            raise ValueError("이미 상담사가 연결된 상담입니다.")

        now = datetime.now(timezone.utc)
        chat.employee_id = employee_id
        chat.agent_connected_at = now
        chat.chat_started_at = now
        if chat.agent_requested_at:
            requested_at = chat.agent_requested_at
            if requested_at.tzinfo is None:
                requested_at = requested_at.replace(tzinfo=timezone.utc)
            delta = (now - requested_at).total_seconds()
            chat.waiting_seconds = int(delta)

        self.db.commit()
        self.db.refresh(chat)

        consultation = self.db.get(Consultation, chat.consultation_id)
        customer_no = consultation.customer_no if consultation else "UNKNOWN"

        await self.events.publish_chat(
            "AgentConnected",
            {
                "chatConsultationId": chat_consultation_id,
                "consultationId": chat.consultation_id,
                "employeeId": employee_id,
                "customerNo": customer_no,
            },
        )
        return chat

    async def send_message(
        self,
        chat_consultation_id: int,
        message: str,
        sender_type_code_id: int,
    ) -> ChatMessageHistory:
        """상담사 또는 고객이 메시지를 전송한다.

        sender_type_code_id: 1=USER, 2=BOT, 3=AGENT
        Kafka: ChatMessageSent 이벤트 발행
        """
        chat = self.get_consultation(chat_consultation_id)

        last_seq = self.db.execute(
            select(ChatMessageHistory.sequence_no)
            .where(ChatMessageHistory.chat_consultation_id == chat_consultation_id)
            .order_by(ChatMessageHistory.sequence_no.desc())
            .limit(1)
        ).scalar_one_or_none()

        msg = ChatMessageHistory(
            chat_consultation_id=chat_consultation_id,
            chatbot_consultation_id=None,          # 상담사 메시지는 chatbot_id 미설정
            sequence_no=(last_seq or 0) + 1,
            sender_type_code_id=sender_type_code_id,
            message_type_code_id=CODE_MESSAGE_TYPE_TEXT,
            message_content=message,
            process_method_code_id=None,
        )
        self.db.add(msg)
        chat.total_turn_count += 1
        self.db.commit()
        self.db.refresh(msg)

        await self.events.publish_chat(
            "ChatMessageSent",
            {
                "chatConsultationId": chat_consultation_id,
                "senderType": _SENDER_LABEL.get(sender_type_code_id, "UNKNOWN"),
                "message": message,
            },
        )
        return msg

    async def end_chat(
        self,
        chat_consultation_id: int,
        satisfaction_score: int | None = None,
    ) -> ChatConsultation:
        """상담을 종료한다.

        Kafka: ChatEnded 이벤트 발행
        """
        chat = self.get_consultation(chat_consultation_id)
        if chat.active_yn == "N":
            raise ValueError("이미 종료된 상담입니다.")

        now = datetime.now(timezone.utc)
        chat.chat_ended_at = now
        chat.active_yn = "N"
        if satisfaction_score is not None:
            chat.satisfaction_score = satisfaction_score
        if chat.chat_started_at:
            started_at = chat.chat_started_at
            if started_at.tzinfo is None:
                started_at = started_at.replace(tzinfo=timezone.utc)
            delta = (now - started_at).total_seconds()
            chat.chat_seconds = int(delta)

        consultation = self.db.get(Consultation, chat.consultation_id)
        if consultation:
            consultation.completed_at = now

        self.db.commit()
        self.db.refresh(chat)

        await self.events.publish_chat(
            "ChatEnded",
            {
                "chatConsultationId": chat_consultation_id,
                "consultationId": chat.consultation_id,
                "satisfactionScore": satisfaction_score,
            },
        )
        return chat

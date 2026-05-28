"""
deposit-api Kafka 이벤트 발행기.

발행 이벤트:
  - ContractCreated : 고객이 수신 상품 계약을 완료했을 때
"""

import json
import logging
from typing import Any

from app.config import Settings

logger = logging.getLogger(__name__)


class DepositKafkaProducer:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._producer = None

    async def start(self) -> None:
        if not self._settings.kafka_enabled:
            logger.info("[Kafka] DEPOSIT_KAFKA_ENABLED=false → 비활성화")
            return
        try:
            from aiokafka import AIOKafkaProducer
        except ImportError as exc:
            raise RuntimeError(
                "DEPOSIT_KAFKA_ENABLED=true 이지만 aiokafka가 없습니다. requirements.txt 확인"
            ) from exc

        self._producer = AIOKafkaProducer(
            bootstrap_servers=self._settings.kafka_bootstrap_servers
        )
        await self._producer.start()
        logger.info("[Kafka] producer 시작 (bootstrap=%s)", self._settings.kafka_bootstrap_servers)

    async def stop(self) -> None:
        if self._producer:
            await self._producer.stop()
            logger.info("[Kafka] producer 종료")

    async def publish_contract_created(
        self,
        contract_id: int,
        contract_number: str,
        customer_id: str,
        product_id: int,
        join_amount: float,
        interest_rate: float,
    ) -> None:
        """고객이 수신 상품 계약을 완료했을 때 발행."""
        await self._publish(
            event_type="ContractCreated",
            payload={
                "contractId":     contract_id,
                "contractNumber": contract_number,
                "customerId":     customer_id,
                "productId":      product_id,
                "joinAmount":     join_amount,
                "interestRate":   interest_rate,
            },
        )

    async def _publish(self, event_type: str, payload: dict[str, Any]) -> None:
        if not self._producer:
            return
        message = {"eventType": event_type, "payload": payload}
        data = json.dumps(message, ensure_ascii=False, default=str).encode("utf-8")
        await self._producer.send_and_wait(self._settings.kafka_topic_deposit_events, data)
        logger.info("[Kafka] 발행 event=%s payload=%s", event_type, payload)

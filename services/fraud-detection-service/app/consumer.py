from __future__ import annotations

import json
import threading
from collections.abc import Callable

from confluent_kafka import Consumer

from app.clients import TransactionServiceClient
from app.schemas import FraudDecisionRequest, PredictionRequest, PredictionResponse, TransactionEvent
from app.settings import settings


def score_transaction(request: PredictionRequest) -> PredictionResponse:
    normalized_amount = min(request.amount / 10000, 1.0)
    fraud_score = round(normalized_amount, 4)
    return PredictionResponse(
        transaction_id=request.transaction_id,
        fraud_score=fraud_score,
        is_fraud=fraud_score >= settings.fraud_threshold,
        model_version="baseline-rule-model",
    )


def build_decision(event: TransactionEvent) -> FraudDecisionRequest:
    prediction = score_transaction(
        PredictionRequest(
            transaction_id=event.transaction_id,
            amount=event.amount,
            currency=event.currency,
            source_account_id=event.source_account_id,
            destination_account_id=event.destination_account_id,
        )
    )
    return FraudDecisionRequest(
        approved=not prediction.is_fraud,
        fraud_score=prediction.fraud_score,
        model_version=prediction.model_version,
        reason="approved-by-model" if not prediction.is_fraud else "flagged-by-model",
    )


class FraudEventConsumer:
    def __init__(
        self,
        transaction_client: TransactionServiceClient,
        should_run: Callable[[], bool] | None = None,
    ) -> None:
        self.transaction_client = transaction_client
        self.should_run = should_run or (lambda: settings.kafka_enabled)
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None
        self._consumer: Consumer | None = None

    def start(self) -> None:
        if not self.should_run() or self._thread is not None:
            return

        self._consumer = Consumer(
            {
                "bootstrap.servers": settings.kafka_bootstrap_servers,
                "group.id": settings.kafka_group_id,
                "auto.offset.reset": "earliest",
            }
        )
        self._consumer.subscribe(["transaction-events"])
        self._thread = threading.Thread(target=self._poll_loop, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread is not None:
            self._thread.join(timeout=2.0)
        if self._consumer is not None:
            self._consumer.close()
        self._thread = None
        self._consumer = None
        self._stop_event.clear()

    def _poll_loop(self) -> None:
        if self._consumer is None:
            return

        while not self._stop_event.is_set():
            message = self._consumer.poll(1.0)
            if message is None or message.error():
                continue

            payload = json.loads(message.value().decode("utf-8"))
            event = TransactionEvent.model_validate(payload)
            if event.status != "PENDING":
                continue

            decision = build_decision(event)
            self.transaction_client.submit_fraud_decision(event.transaction_id, decision)

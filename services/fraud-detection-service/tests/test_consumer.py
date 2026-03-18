from app.consumer import build_decision, score_transaction
from app.schemas import PredictionRequest, TransactionEvent


def test_score_transaction_flags_large_amounts() -> None:
    prediction = score_transaction(
        PredictionRequest(
            transaction_id="txn-1",
            amount=9500.0,
            currency="USD",
            source_account_id="src-1",
            destination_account_id="dst-1",
        )
    )

    assert prediction.is_fraud is True
    assert prediction.fraud_score == 0.95


def test_build_decision_approves_low_risk_pending_event() -> None:
    decision = build_decision(
        TransactionEvent(
            eventType="TRANSACTION_PENDING",
            transactionId="txn-2",
            sourceAccountId="src-1",
            destinationAccountId="dst-1",
            amount=125.0,
            currency="USD",
            status="PENDING",
            idempotencyKey="idemp-1",
            occurredAt="2026-03-19T00:00:00Z",
        )
    )

    assert decision.approved is True
    assert decision.reason == "approved-by-model"

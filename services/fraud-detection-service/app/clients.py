import httpx

from app.schemas import FraudDecisionRequest


class TransactionServiceClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")

    def submit_fraud_decision(self, transaction_id: str, decision: FraudDecisionRequest) -> None:
        response = httpx.post(
            f"{self.base_url}/api/v1/transactions/{transaction_id}/fraud-decision",
            json=decision.model_dump(by_alias=True),
            timeout=5.0,
        )
        response.raise_for_status()

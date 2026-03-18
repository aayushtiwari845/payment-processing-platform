from pydantic import BaseModel, ConfigDict, Field


class PredictionRequest(BaseModel):
    transaction_id: str = Field(..., min_length=1)
    amount: float = Field(..., gt=0)
    currency: str = Field(..., min_length=3, max_length=3)
    source_account_id: str = Field(..., min_length=1)
    destination_account_id: str = Field(..., min_length=1)


class PredictionResponse(BaseModel):
    transaction_id: str
    fraud_score: float
    is_fraud: bool
    model_version: str


class TransactionEvent(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    event_type: str = Field(alias="eventType")
    transaction_id: str = Field(alias="transactionId")
    source_account_id: str = Field(alias="sourceAccountId")
    destination_account_id: str = Field(alias="destinationAccountId")
    amount: float
    currency: str
    status: str
    idempotency_key: str = Field(alias="idempotencyKey")
    occurred_at: str = Field(alias="occurredAt")


class FraudDecisionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    approved: bool
    fraud_score: float = Field(alias="fraudScore")
    model_version: str = Field(alias="modelVersion")
    reason: str

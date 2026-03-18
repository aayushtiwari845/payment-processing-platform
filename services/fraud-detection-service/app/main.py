from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.clients import TransactionServiceClient
from app.consumer import FraudEventConsumer, score_transaction
from app.schemas import PredictionRequest, PredictionResponse
from app.settings import settings


transaction_client = TransactionServiceClient(settings.transaction_service_base_url)
fraud_event_consumer = FraudEventConsumer(transaction_client)


@asynccontextmanager
async def lifespan(_: FastAPI):
    fraud_event_consumer.start()
    try:
        yield
    finally:
        fraud_event_consumer.stop()


app = FastAPI(title="fraud-detection-service", version="0.1.0", lifespan=lifespan)
Instrumentator().instrument(app).expose(app, endpoint="/metrics", include_in_schema=False)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest) -> PredictionResponse:
    return score_transaction(request)

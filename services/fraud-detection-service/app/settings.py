from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "fraud-detection-service"
    kafka_enabled: bool = False
    transaction_service_base_url: str = "http://localhost:8082"
    transaction_service_internal_token: str = "payment-internal-token"
    fraud_threshold: float = 0.8

    model_config = SettingsConfigDict(env_prefix="FRAUD_", extra="ignore")


settings = Settings()

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "deposit-api"
    app_version: str = "0.1.0"

    # DB — 기본값 없음: 반드시 환경변수 또는 .env 에서 주입
    database_url: str = "sqlite:///./deposit.db"

    # Kafka
    kafka_enabled: bool = False
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic_deposit_events: str = "deposit.contract.events"

    model_config = SettingsConfigDict(
        env_prefix="DEPOSIT_",
        env_file=".env",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()

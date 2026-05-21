from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "consultation-service"
    app_version: str = "0.1.0"
    database_url: str = "postgresql+psycopg://deposit:deposit@localhost:5432/deposit_db"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_enabled: bool = False
    kafka_topic_chatbot_events: str = "consultation.chatbot.events"
    kafka_topic_chat_events: str = "consultation.chat.events"
    llm_confidence_threshold: int = 70

    model_config = SettingsConfigDict(
        env_prefix="CONSULTATION_",
        env_file=".env",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()

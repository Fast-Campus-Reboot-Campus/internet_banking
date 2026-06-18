from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "goal-agent"
    # 기본값은 로컬 개발용 deposit DB. 운영 환경에서는 DATABASE_URL 환경변수로 주입하세요.
    database_url: str = "postgresql+psycopg://deposit:deposit@localhost:5433/deposit_db"
    cors_origins: list[str] = ["http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3001", "http://127.0.0.1:3001"]
    anthropic_api_key: str = ""
    # 내부 서비스 인증 키. 설정 시 X-Internal-Token 헤더로 검증. 빈 값이면 인증 비활성화(개발 전용).
    api_key: str = ""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()

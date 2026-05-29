# =============================================================================
# deposit-api — FastAPI 수신(예금) 시스템 API
# =============================================================================
FROM python:3.11-slim

WORKDIR /app

# psycopg(binary) 의 런타임 의존성 (postgres client lib)
RUN apt-get update && \
    apt-get install -y --no-install-recommends libpq5 && \
    rm -rf /var/lib/apt/lists/*

# 의존성 먼저 설치 (레이어 캐시)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY app/ ./app/

# 포트 (기본 8090 — 다른 Spring 서비스와 충돌 없게)
EXPOSE 8090

# 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]

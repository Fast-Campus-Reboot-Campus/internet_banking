"""Fraud Investigation Agent — HTTP 서버 러너 (어드민 콘솔 연동).

CLI(run_investigation.py)가 한 사건을 터미널에 그리는 것과 달리, 이 러너는 같은
조사 루프를 **HTTP API**(src/agent/api.py)로 노출해 web/admin 콘솔이 호출하게 한다.

사용법:
  pip install -r requirements.txt          # fastapi · uvicorn 포함
  python scripts/serve.py                  # 기본 0.0.0.0:8090
  FRAUD_AGENT_PORT=8090 python scripts/serve.py

프론트는 NEXT_PUBLIC_FRAUD_AGENT_URL 로 이 주소를 가리킨다(기본 http://localhost:8090).
LLM 은 기본 mock — 키 없이 동작. 실연결은 TRIAGE_LLM_PROVIDER / TRIAGE_REAL_TOOLS env.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

# scripts/ 에서 바로 실행 가능하도록 src 를 경로에 추가 (run_investigation.py 와 동일)
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))


def main() -> None:
    import uvicorn

    host = os.getenv("FRAUD_AGENT_HOST", "0.0.0.0")
    port = int(os.getenv("FRAUD_AGENT_PORT", "8090"))
    uvicorn.run("agent.api:app", host=host, port=port, reload=False)


if __name__ == "__main__":
    main()

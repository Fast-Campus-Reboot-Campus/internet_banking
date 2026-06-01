#!/usr/bin/env bash
# =============================================================================
# Oracle Cloud Free Tier (Ubuntu 22.04 ARM) 초기 셋업 스크립트
#
# 사용:
#   ssh ubuntu@<oracle-public-ip>
#   curl -fsSL https://raw.githubusercontent.com/<owner>/<repo>/main/infra/oracle/bootstrap.sh -o bootstrap.sh
#   chmod +x bootstrap.sh
#   ./bootstrap.sh
#
# 사전 조건:
#   - Oracle VM 생성: VM.Standard.A1.Flex (ARM, 4 OCPU, 24GB RAM)
#   - Ingress Rule 열림: 22 (SSH 본인 IP만), 8080 (gateway)
#   - SSH 키로 ubuntu 사용자 로그인 가능
#
# 실행 후 수동 작업:
#   1. GHCR PAT 로 docker login
#   2. .env.prod 를 scp 로 전송 → ~/app/.env.prod
#   3. infra/docker/docker-compose.prod.yml + infra/ 디렉토리 전송 (deploy-infra 워크플로우가 자동 처리)
#   4. systemctl enable --now ib-stack 로 자동 기동 활성화
# =============================================================================
set -euo pipefail

APP_DIR="${HOME}/app"
LOG_PREFIX="[bootstrap]"

log() { echo "$LOG_PREFIX $*"; }

# ─── 1. 시스템 패키지 ───────────────────────────────────────────────────────
log "1/6 시스템 패키지 업데이트 + 필수 도구 설치"
sudo apt-get update -y
sudo apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  ufw \
  fail2ban \
  htop \
  jq

# ─── 2. Docker + Compose 플러그인 ──────────────────────────────────────────
if command -v docker >/dev/null 2>&1; then
  log "2/6 Docker 이미 설치됨, 건너뜀"
else
  log "2/6 Docker 설치"
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

  sudo apt-get update -y
  sudo apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

  sudo usermod -aG docker "$USER"
  log "   ⚠️  Docker 그룹 적용 위해 한번 로그아웃/재로그인 필요"
fi

# ─── 3. 방화벽 (ufw) ───────────────────────────────────────────────────────
log "3/6 ufw 방화벽 룰 설정"
sudo ufw --force reset
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 8080/tcp comment 'Gateway HTTP'
# 필요시 추가: sudo ufw allow 443/tcp comment 'HTTPS'
sudo ufw --force enable

# ─── 4. fail2ban (SSH brute force 방어) ────────────────────────────────────
log "4/6 fail2ban 활성화 (sshd jail)"
sudo systemctl enable --now fail2ban

# ─── 5. 앱 디렉토리 + Docker 로그 회전 ─────────────────────────────────────
log "5/6 ~/app 디렉토리 생성 + Docker 로그 회전 설정"
mkdir -p "$APP_DIR/infra/docker"
mkdir -p "$APP_DIR/infra/prometheus"
mkdir -p "$APP_DIR/infra/grafana"
mkdir -p "$APP_DIR/infra/ai-db/init"

# Docker daemon 로그 회전 (개별 컨테이너 외 daemon 자체)
sudo mkdir -p /etc/docker
if [ ! -f /etc/docker/daemon.json ]; then
  sudo tee /etc/docker/daemon.json >/dev/null <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
JSON
  sudo systemctl restart docker
fi

# ─── 6. systemd 유닛 (재부팅시 compose 자동 기동) ─────────────────────────
log "6/6 systemd 유닛 등록 (ib-stack.service)"
sudo tee /etc/systemd/system/ib-stack.service >/dev/null <<EOF
[Unit]
Description=Internet Banking docker compose stack
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
User=$USER
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod up -d
ExecStop=/usr/bin/docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod down

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
log "   → systemctl enable --now ib-stack  ← .env.prod, compose 파일 배치 후 실행"

# ─── 완료 ─────────────────────────────────────────────────────────────────
log "✅ bootstrap 완료"
echo
echo "다음 단계 (수동):"
echo "  1) 로그아웃 후 재접속  (docker 그룹 적용)"
echo "  2) GHCR 로그인:"
echo "       echo \$GHCR_PAT | docker login ghcr.io -u <github-user> --password-stdin"
echo "  3) .env.prod 전송:"
echo "       scp .env.prod  ubuntu@<oracle-ip>:~/app/.env.prod"
echo "  4) deploy-infra 워크플로우 수동 실행 (compose/prometheus/grafana 파일 sync)"
echo "  5) sudo systemctl enable --now ib-stack"
echo "  6) docker compose -f ~/app/infra/docker/docker-compose.prod.yml ps"

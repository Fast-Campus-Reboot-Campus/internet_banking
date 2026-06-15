# Oracle Cloud 운영 셋업

## 1. VM 생성 (Oracle Cloud 콘솔)

- **리전**: ap-seoul-1 (또는 ap-tokyo-1)
- **Compartment**: 자유
- **Shape**: `VM.Standard.A1.Flex` (Always Free)
  - OCPU: 4
  - Memory: 24 GB
- **Image**: Canonical Ubuntu 22.04
- **Boot Volume**: 100 GB (Always Free 한도 내)
- **SSH Keys**: 본인 공개키 등록 → 개인키는 로컬 안전 보관

## 2. 네트워크 (Subnet → Security List → Ingress Rules)

| Source CIDR | Protocol | Port | 용도 |
|-------------|----------|------|------|
| `<본인 IP>/32` | TCP | 22 | SSH |
| `0.0.0.0/0` | TCP | 8080 | Gateway HTTP |
| `0.0.0.0/0` | TCP | 443 | HTTPS (선택, Let's Encrypt 구성시) |

Oracle Ubuntu 이미지에는 iptables 가 기본 차단 룰을 가지고 있으므로 OS 레벨 ufw 와 별개로
**VCN Security List** 에서도 위 룰을 반드시 추가.

## 3. 초기 셋업

```bash
ssh ubuntu@<oracle-public-ip>

# bootstrap 스크립트 다운로드 + 실행
curl -fsSL https://raw.githubusercontent.com/<owner>/internet_banking/main/infra/oracle/bootstrap.sh -o bootstrap.sh
chmod +x bootstrap.sh
./bootstrap.sh

# Docker 그룹 적용을 위해 한번 로그아웃 → 재접속
exit
ssh ubuntu@<oracle-public-ip>
```

## 4. GHCR 로그인 (이미지 pull 권한)

GitHub → Settings → Developer settings → Personal access tokens (classic) 에서
`read:packages` 권한 PAT 발급.

```bash
echo <GHCR_PAT> | docker login ghcr.io -u <github-user> --password-stdin
```

## 5. .env.prod 전송

로컬에서 `.env.prod.sample` 복사 → 실제 값 채운 뒤:

```bash
scp .env.prod ubuntu@<oracle-public-ip>:~/app/.env.prod
chmod 600 ~/app/.env.prod   # Oracle 서버에서 권한 조정
```

## 6. 인프라 파일 동기화

GitHub Actions `Deploy infra` 워크플로우 수동 실행:

- GitHub → Actions → Deploy infra → Run workflow

또는 로컬에서 수동 SCP:

```bash
scp -r infra/docker infra/prometheus infra/grafana infra/ai-db \
  ubuntu@<oracle-public-ip>:~/app/infra/
```

## 7. 전체 스택 기동

```bash
ssh ubuntu@<oracle-public-ip>
cd ~/app

# 모든 이미지 pull
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod pull

# 기동
docker compose -f infra/docker/docker-compose.prod.yml --env-file .env.prod up -d

# 자동 기동 활성화 (재부팅시 복구)
sudo systemctl enable --now ib-stack

# 상태 확인
docker compose -f infra/docker/docker-compose.prod.yml ps
docker compose -f infra/docker/docker-compose.prod.yml logs -f api-gateway
```

## 8. GitHub Actions secrets 등록

GitHub → Settings → Secrets and variables → Actions:

| 시크릿 | 값 |
|--------|------|
| `ORACLE_SSH_HOST` | Oracle 공인 IP |
| `ORACLE_SSH_USER` | `ubuntu` |
| `ORACLE_SSH_KEY` | SSH 개인키 (PEM 내용 전체) |

이후 main 브랜치에 푸시하면 변경된 서비스만 자동 빌드/배포.

## 9. 운영 점검

```bash
# 메모리 사용량
free -h
docker stats --no-stream

# 컨테이너 상태
docker compose -f ~/app/infra/docker/docker-compose.prod.yml ps

# 특정 서비스 로그
docker compose -f ~/app/infra/docker/docker-compose.prod.yml logs --tail=200 loan-service

# 디스크
df -h
docker system df
```

## 10. 트러블슈팅

| 증상 | 원인 / 조치 |
|------|-------------|
| `Cannot connect to the Docker daemon` | `usermod -aG docker $USER` 후 재로그인 필요 |
| `denied: requested access to the resource is denied` (GHCR) | PAT 만료. 재발급 후 `docker login` 다시 |
| 외부에서 8080 접속 불가 | VCN Security List 룰 누락. OCI 콘솔에서 0.0.0.0/0 8080 추가 |
| OOM kill (메모리 부족) | `docker stats` 로 범인 서비스 식별. `mem_limit` 조정 또는 일부 서비스 OFF |
| Kafka 헬스체크 실패 (ARM) | `apache/kafka:3.8.0` 이미지가 ARM 미지원이면 `bitnami/kafka` 등으로 교체 검토 |

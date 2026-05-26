#!/usr/bin/env bash
# services/payment-service/infra/init-topics.sh
#
# 결제계 Kafka 토픽 초기 생성 스크립트
# 기준: 결제계_Kafka토픽정의서_v3.1.xlsx 시트 2 (토픽_종합표) — 총 18개
#
# 토픽 분류:
#   KFTC 외부망 (kftc-kafka)  : 2개 일반 + 2개 DLQ = 4개
#   BOK 외부망  (bok-kafka)   : 2개 일반 + 2개 DLQ = 4개
#   내부 이벤트 (internal-kafka): 3개 일반 + 3개 DLQ + 4개 수신계 = 10개
#   합계: 18개
#
# ★ account.status.changed.dlq 미포함:
#   xlsx 시트 6 (DLQ_토픽)에 정의 없음.
#   시트 11 주석: "없음 정의 - 캐시무효화이라 DLQ 불필요"
#   필요 시 사용자가 별도 추가.
#
# 실행 전제: docker-compose-kafka.yml 기반 서비스가 healthy 상태
# 실행 위치: 프로젝트 루트 또는 services/payment-service/
# 실행 방법: bash services/payment-service/infra/init-topics.sh

set -euo pipefail

# ---------------------------------------------------------------------------
# 컨테이너 이름 (docker-compose-kafka.yml의 container_name 과 일치)
# ---------------------------------------------------------------------------
KFTC_CONTAINER="payment-kftc-kafka"
BOK_CONTAINER="payment-bok-kafka"
INTERNAL_CONTAINER="payment-internal-kafka"

# docker exec 안에서 실행 — localhost는 컨테이너 내부 주소
KFTC_BOOTSTRAP="localhost:29092"
BOK_BOOTSTRAP="localhost:29093"
INTERNAL_BOOTSTRAP="localhost:29094"

# Retention (ms) — 토픽정의서 v3.1 시트 7 (설정_명세)
NORMAL_RETENTION_MS=604800000     # 7일
DLQ_RETENTION_MS=2592000000       # 30일

# ---------------------------------------------------------------------------
# 유틸 함수
# ---------------------------------------------------------------------------

check_container() {
  local container=$1
  if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
    echo "❌ 컨테이너 미기동: ${container}"
    echo "   먼저 docker compose -f docker-compose-kafka.yml up -d ${container} 을 실행하세요."
    exit 1
  fi
}

# create_topic <container> <bootstrap> <topic> <partitions> <replication> <retention_ms>
# 출력 3종:
#   ✅ 생성됨       — 신규 생성 성공
#   ⏭  이미 존재    — --if-not-exists 로 skip (정상)
#   ❌ 실패         — 브로커 오류 등 진짜 실패 → exit 1
create_topic() {
  local container=$1
  local bootstrap=$2
  local topic=$3
  local partitions=$4
  local replication=$5
  local retention_ms=$6

  printf "  %-40s (p=%s r=%s) ... " "$topic" "$partitions" "$replication"

  local output exit_code=0
  output=$(docker exec "$container" kafka-topics.sh \
      --bootstrap-server "$bootstrap" \
      --create \
      --if-not-exists \
      --topic "$topic" \
      --partitions "$partitions" \
      --replication-factor "$replication" \
      --config "retention.ms=${retention_ms}" \
      --config "cleanup.policy=delete" \
      2>&1) || exit_code=$?

  if [ "$exit_code" -ne 0 ]; then
    echo "❌ 실패 (exit=${exit_code})"
    echo "   └─ ${output}"
    exit 1
  elif echo "$output" | grep -qi "already exists"; then
    echo "⏭  이미 존재 (skip)"
  else
    echo "✅ 생성됨"
  fi
}

# ---------------------------------------------------------------------------
# 시작
# ---------------------------------------------------------------------------

echo "======================================================================"
echo " 결제계 Kafka 토픽 초기화"
echo " 기준: 결제계_Kafka토픽정의서_v3.1.xlsx 시트 2 (토픽_종합표)"
echo " 총 18개 토픽"
echo "======================================================================"
echo ""

# 컨테이너 기동 확인
echo "[ 컨테이너 상태 확인 ]"
check_container "$KFTC_CONTAINER"
echo "  ✅ ${KFTC_CONTAINER}"
check_container "$BOK_CONTAINER"
echo "  ✅ ${BOK_CONTAINER}"
check_container "$INTERNAL_CONTAINER"
echo "  ✅ ${INTERNAL_CONTAINER}"
echo ""

# ---------------------------------------------------------------------------
# 1. KFTC 외부망 토픽 (kftc-kafka 클러스터)
#    기준: 토픽정의서 시트 3 (KFTC외부망_토픽)
#    토픽 2개 (일반) + 2개 (DLQ) = 4개
# ---------------------------------------------------------------------------
echo "----------------------------------------------------------------------"
echo " 1. KFTC 외부망 토픽  →  클러스터: ${KFTC_CONTAINER}"
echo "    시트 3 (KFTC외부망_토픽) + 시트 6 (DLQ_토픽)"
echo "----------------------------------------------------------------------"

# 일반 토픽 (retention 7일, 파티션 3, Producer Record Key: clearing_no)
create_topic "$KFTC_CONTAINER" "$KFTC_BOOTSTRAP" \
  "kftc.network.request"      3 1 "$NORMAL_RETENTION_MS"
create_topic "$KFTC_CONTAINER" "$KFTC_BOOTSTRAP" \
  "kftc.network.response"     3 1 "$NORMAL_RETENTION_MS"

# DLQ 토픽 (retention 30일, 파티션 1 — 순서 보장 불필요)
create_topic "$KFTC_CONTAINER" "$KFTC_BOOTSTRAP" \
  "kftc.network.request.dlq"  1 1 "$DLQ_RETENTION_MS"
create_topic "$KFTC_CONTAINER" "$KFTC_BOOTSTRAP" \
  "kftc.network.response.dlq" 1 1 "$DLQ_RETENTION_MS"

echo ""

# ---------------------------------------------------------------------------
# 2. BOK 외부망 토픽 (bok-kafka 클러스터)
#    기준: 토픽정의서 시트 4 (BOK외부망_토픽)
#    토픽 2개 (일반) + 2개 (DLQ) = 4개
# ---------------------------------------------------------------------------
echo "----------------------------------------------------------------------"
echo " 2. BOK 외부망 토픽   →  클러스터: ${BOK_CONTAINER}"
echo "    시트 4 (BOK외부망_토픽) + 시트 6 (DLQ_토픽)"
echo "----------------------------------------------------------------------"

create_topic "$BOK_CONTAINER" "$BOK_BOOTSTRAP" \
  "bok.network.request"       3 1 "$NORMAL_RETENTION_MS"
create_topic "$BOK_CONTAINER" "$BOK_BOOTSTRAP" \
  "bok.network.response"      3 1 "$NORMAL_RETENTION_MS"

create_topic "$BOK_CONTAINER" "$BOK_BOOTSTRAP" \
  "bok.network.request.dlq"   1 1 "$DLQ_RETENTION_MS"
create_topic "$BOK_CONTAINER" "$BOK_BOOTSTRAP" \
  "bok.network.response.dlq"  1 1 "$DLQ_RETENTION_MS"

echo ""

# ---------------------------------------------------------------------------
# 3. 내부 이벤트 토픽 (internal-kafka 클러스터)
#    기준: 토픽정의서 시트 5 (내부이벤트_토픽)
#    토픽 3개 (일반) + 3개 (DLQ) = 6개
# ---------------------------------------------------------------------------
echo "----------------------------------------------------------------------"
echo " 3. 내부 이벤트 토픽  →  클러스터: ${INTERNAL_CONTAINER}"
echo "    시트 5 (내부이벤트_토픽) + 시트 6 (DLQ_토픽)"
echo "----------------------------------------------------------------------"

create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.completed"         3 1 "$NORMAL_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.failed"            3 1 "$NORMAL_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.reversed"          3 1 "$NORMAL_RETENTION_MS"

create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.completed.dlq"     1 1 "$DLQ_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.failed.dlq"        1 1 "$DLQ_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "payment.reversed.dlq"      1 1 "$DLQ_RETENTION_MS"

echo ""

# ---------------------------------------------------------------------------
# 4. 수신계 이벤트 토픽 (internal-kafka 클러스터)
#    기준: 토픽정의서 시트 11 (수신계_이벤트_토픽)
#    Producer: deposit-service / Consumer: payment-service
#    토픽 4개 (일반) — DLQ 없음 (시트 11: 캐시무효화이라 DLQ 불필요)
# ---------------------------------------------------------------------------
echo "----------------------------------------------------------------------"
echo " 4. 수신계 이벤트 토픽 →  클러스터: ${INTERNAL_CONTAINER}"
echo "    시트 11 (수신계_이벤트_토픽) / Producer: deposit-service"
echo "    ★ DLQ 없음: 시트 6 미포함, 시트 11 주석 '캐시무효화이라 DLQ 불필요'"
echo "----------------------------------------------------------------------"

create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "account.status.changed"    3 1 "$NORMAL_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "account.fraud-reported"    3 1 "$NORMAL_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "account.holder.changed"    3 1 "$NORMAL_RETENTION_MS"
create_topic "$INTERNAL_CONTAINER" "$INTERNAL_BOOTSTRAP" \
  "account.closed"            3 1 "$NORMAL_RETENTION_MS"

echo ""

# ---------------------------------------------------------------------------
# 결과 확인
# ---------------------------------------------------------------------------
echo "======================================================================"
echo " 토픽 생성 완료 — 총 18개 (토픽정의서 v3.1 시트 2 기준)"
echo "======================================================================"
echo ""

echo "=== KFTC 클러스터 토픽 목록 (${KFTC_CONTAINER}) ==="
docker exec "$KFTC_CONTAINER" kafka-topics.sh \
  --bootstrap-server "$KFTC_BOOTSTRAP" --list

echo ""
echo "=== BOK 클러스터 토픽 목록 (${BOK_CONTAINER}) ==="
docker exec "$BOK_CONTAINER" kafka-topics.sh \
  --bootstrap-server "$BOK_BOOTSTRAP" --list

echo ""
echo "=== Internal 클러스터 토픽 목록 (${INTERNAL_CONTAINER}) ==="
docker exec "$INTERNAL_CONTAINER" kafka-topics.sh \
  --bootstrap-server "$INTERNAL_BOOTSTRAP" --list

echo ""
echo "Kafka UI에서도 확인 가능: http://localhost:8090"

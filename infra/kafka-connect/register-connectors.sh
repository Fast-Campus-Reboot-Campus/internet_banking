#!/bin/sh
set -e
CONNECT_URL=http://kafka-connect:8083

echo "==> Checking connector: loan-review-es-sink"
status=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/connectors/loan-review-es-sink")

if [ "$status" = "200" ]; then
  echo "loan-review-es-sink already registered — skipping."
else
  curl -sf -X POST \
    -H "Content-Type: application/json" \
    --data-binary @/connectors/loan-review-es-sink.json \
    "$CONNECT_URL/connectors"
  echo ""
  echo "loan-review-es-sink registered."
fi

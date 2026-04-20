#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.cloud}"
MODE="${2:-prod}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: env file not found: $ENV_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

COMPOSE_FILES=(-f "$ROOT_DIR/docker-compose.cloud.yml")
if [[ "$MODE" == "prod" ]]; then
  COMPOSE_FILES+=(-f "$ROOT_DIR/docker-compose.prod.yml")
elif [[ "$MODE" != "dev" ]]; then
  echo "ERROR: mode must be 'prod' or 'dev'"
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker not found"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl not found"
  exit 1
fi

PUBLIC_ROOT="${PUBLIC_ROOT_URL:-http://127.0.0.1}"
API_URL="${API_CHECK_URL:-$PUBLIC_ROOT/api/labs/list?pageNum=1&pageSize=1}"

echo "INFO: checking compose service state"
docker compose --env-file "$ENV_FILE" "${COMPOSE_FILES[@]}" ps

echo "INFO: checking frontend root -> $PUBLIC_ROOT"
curl -fsS "$PUBLIC_ROOT" >/dev/null

echo "INFO: checking backend api -> $API_URL"
curl -fsS "$API_URL" >/dev/null

echo "OK: public endpoints are reachable"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.cloud}"
MODE="${2:-prod}"

COMPOSE_FILES=(-f "$ROOT_DIR/docker-compose.cloud.yml")
if [[ "$MODE" == "prod" ]]; then
  COMPOSE_FILES+=(-f "$ROOT_DIR/docker-compose.prod.yml")
elif [[ "$MODE" != "dev" ]]; then
  echo "ERROR: mode must be 'prod' or 'dev'"
  exit 1
fi

"$ROOT_DIR/scripts/cloud-preflight.sh" "$ENV_FILE"

echo "INFO: deploy mode = $MODE"
echo "INFO: env file = $ENV_FILE"

docker compose --env-file "$ENV_FILE" "${COMPOSE_FILES[@]}" up -d --build
docker compose --env-file "$ENV_FILE" "${COMPOSE_FILES[@]}" ps

echo "NEXT: $ROOT_DIR/scripts/cloud-postcheck.sh $ENV_FILE $MODE"

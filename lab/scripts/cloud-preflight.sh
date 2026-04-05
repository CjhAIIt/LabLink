#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.cloud}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker not found"
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "ERROR: docker compose not available"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: env file not found: $ENV_FILE"
  echo "copy .env.cloud.example to .env.cloud first"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

if [[ -z "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  echo "ERROR: MYSQL_ROOT_PASSWORD is empty"
  exit 1
fi

if [[ -z "${JWT_SECRET:-}" || "${JWT_SECRET:-}" == "ChangeThisJwtSecretBeforeProduction" ]]; then
  echo "ERROR: JWT_SECRET is still placeholder"
  exit 1
fi

if [[ -z "${APP_SECURITY_ALLOWED_ORIGINS_CSV:-}" ]]; then
  echo "ERROR: APP_SECURITY_ALLOWED_ORIGINS_CSV is empty"
  exit 1
fi

if [[ -z "${MAIL_USERNAME:-}" ]]; then
  echo "ERROR: MAIL_USERNAME is empty"
  exit 1
fi

if [[ -z "${MAIL_PASSWORD:-}" ]]; then
  echo "ERROR: MAIL_PASSWORD is empty"
  exit 1
fi

for port in 80 8081 3306; do
  if command -v ss >/dev/null 2>&1; then
    if ss -ltn "( sport = :$port )" | tail -n +2 | grep -q .; then
      echo "WARN: port $port is already in use"
    fi
  elif command -v netstat >/dev/null 2>&1; then
    if netstat -ltn 2>/dev/null | awk '{print $4}' | grep -q ":$port$"; then
      echo "WARN: port $port is already in use"
    fi
  fi
done

echo "OK: docker present"
echo "OK: env file loaded from $ENV_FILE"
echo "NEXT: docker compose --env-file \"$ENV_FILE\" -f docker-compose.cloud.yml up -d --build"

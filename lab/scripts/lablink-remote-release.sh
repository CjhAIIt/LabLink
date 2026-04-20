#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="/opt/lab-recruitment"
RUNTIME_DIR="$APP_ROOT/runtime"
WEB_ROOT="/var/www/lab-recruitment"
SERVICE_NAME="lab-recruitment"
DB_NAME="lab_recruitment"
RELEASE_DIR=""
DB_MODE="preserve"

usage() {
  cat <<'EOF'
Usage:
  lablink-release.sh --release-dir /home/ubuntu/lablink-upload-YYYYMMDD-HHMMSS [--db-mode preserve|reset-init]
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --release-dir)
      RELEASE_DIR="$2"
      shift 2
      ;;
    --db-mode)
      DB_MODE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$RELEASE_DIR" ]]; then
  echo "Missing --release-dir" >&2
  usage >&2
  exit 1
fi

if [[ "$DB_MODE" != "preserve" && "$DB_MODE" != "reset-init" ]]; then
  echo "Unsupported db mode: $DB_MODE" >&2
  exit 1
fi

BACKEND_JAR="$RELEASE_DIR/lab-recruitment-1.0.0.jar"
FRONTEND_ARCHIVE=$(find "$RELEASE_DIR" -maxdepth 1 -type f -name 'frontend-dist-*.tgz' | head -n 1)
INIT_SQL="$RELEASE_DIR/init.sql"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="$APP_ROOT/manual-backup/$TIMESTAMP"

if [[ ! -f "$BACKEND_JAR" ]]; then
  echo "Missing backend jar: $BACKEND_JAR" >&2
  exit 1
fi

if [[ -z "${FRONTEND_ARCHIVE:-}" || ! -f "$FRONTEND_ARCHIVE" ]]; then
  echo "Missing frontend archive in $RELEASE_DIR" >&2
  exit 1
fi

if [[ "$DB_MODE" == "reset-init" && ! -f "$INIT_SQL" ]]; then
  echo "Missing init.sql for reset-init mode" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

if [[ -f "$RUNTIME_DIR/lab-recruitment-1.0.0.jar" ]]; then
  cp "$RUNTIME_DIR/lab-recruitment-1.0.0.jar" "$BACKUP_DIR/lab-recruitment-1.0.0.jar"
fi

if [[ -f "$RUNTIME_DIR/backend-host.env" ]]; then
  cp "$RUNTIME_DIR/backend-host.env" "$BACKUP_DIR/backend-host.env"
fi

if [[ -f /etc/systemd/system/${SERVICE_NAME}.service ]]; then
  cp "/etc/systemd/system/${SERVICE_NAME}.service" "$BACKUP_DIR/${SERVICE_NAME}.service"
fi

if [[ -f /etc/nginx/sites-available/lab-recruitment ]]; then
  cp /etc/nginx/sites-available/lab-recruitment "$BACKUP_DIR/nginx-lab-recruitment.conf"
fi

if [[ -d "$WEB_ROOT" ]]; then
  tar -czf "$BACKUP_DIR/frontend-www.tgz" -C "$WEB_ROOT" .
fi

mysqldump --single-transaction --routines --events --databases "$DB_NAME" > "$BACKUP_DIR/${DB_NAME}.sql"

systemctl stop "$SERVICE_NAME"

if [[ "$DB_MODE" == "reset-init" ]]; then
  mysql -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;"
  mysql -e "CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  mysql "$DB_NAME" < "$INIT_SQL"
fi

install -m 0644 -o root -g root "$BACKEND_JAR" "$RUNTIME_DIR/lab-recruitment-1.0.0.jar"
mkdir -p "$WEB_ROOT"
find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
tar -xzf "$FRONTEND_ARCHIVE" -C "$WEB_ROOT" --strip-components=1
chown -R www-data:www-data "$WEB_ROOT"

systemctl start "$SERVICE_NAME"
sleep 15

if [[ "$(systemctl is-active "$SERVICE_NAME")" != "active" ]]; then
  journalctl -u "$SERVICE_NAME" -n 120 --no-pager >&2 || true
  exit 1
fi

curl -fsS "http://127.0.0.1:8081/labs/list?pageNum=1&pageSize=1" >/dev/null
curl -fsS "http://127.0.0.1/" >/dev/null

rm -rf "$RELEASE_DIR"

echo "Release completed."
echo "Backup: $BACKUP_DIR"
echo "DB mode: $DB_MODE"

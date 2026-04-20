#!/usr/bin/env bash
set -euo pipefail

umask 077

DB_NAME="${DB_NAME:-lab_recruitment}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/lab-recruitment/db-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
TARGET_DIR="$BACKUP_ROOT/$(date +%Y/%m)"
TARGET_FILE="$TARGET_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "$TARGET_DIR"

mysqldump --single-transaction --routines --events --databases "$DB_NAME" \
  | gzip -9 > "$TARGET_FILE"

find "$BACKUP_ROOT" -type f -name '*.sql.gz' -mtime +"$RETENTION_DAYS" -delete
find "$BACKUP_ROOT" -type d -empty -delete

echo "Created backup: $TARGET_FILE"

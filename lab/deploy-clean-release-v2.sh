#!/bin/bash
set -euo pipefail

TIMESTAMP="$1"
RELEASE_ROOT="/opt/lab-recruitment/source/releases"
NEW_RELEASE="$RELEASE_ROOT/$TIMESTAMP"
ARCHIVE_FILE="/tmp/release-head.zip"
FRONTEND_TAR="/tmp/frontend-dist.tar"
FRONTEND_TMP="/tmp/frontend-dist-clean"
RUNTIME_JAR="/opt/lab-recruitment/runtime/lab-recruitment-1.0.0.jar"
WEBROOT="/var/www/lab-recruitment"
SERVICE_NAME="lab-recruitment.service"

cleanup() {
  rm -f /tmp/release-head.zip /tmp/frontend-dist.tar /tmp/deploy-clean-release-v2.sh
  rm -rf /tmp/frontend-dist-clean /tmp/frontend-dist-hotfix /tmp/frontend-dist-delivery-status-fix /tmp/lab-recruitment-frontend-dist
  rm -f /tmp/lab-recruitment-frontend-dist.tar.gz /tmp/lab-recruitment-frontend-dist.zip
}

rm -rf "$NEW_RELEASE"
mkdir -p "$NEW_RELEASE"
unzip -q "$ARCHIVE_FILE" -d "$NEW_RELEASE"

rm -rf "$FRONTEND_TMP"
mkdir -p "$FRONTEND_TMP"
tar -xf "$FRONTEND_TAR" -C "$FRONTEND_TMP"

cd "$NEW_RELEASE"
mvn -q -DskipTests clean package

cp "$NEW_RELEASE/target/lab-recruitment-1.0.0.jar" "$RUNTIME_JAR"
find "$WEBROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cp -a "$FRONTEND_TMP/." "$WEBROOT/"
chown -R www-data:www-data "$WEBROOT"

systemctl restart "$SERVICE_NAME"
systemctl is-active --quiet "$SERVICE_NAME"
curl -fsS "http://127.0.0.1:8081/api/labs/list?pageNum=1&pageSize=1" > /dev/null
curl -fsS "http://127.0.0.1/" > /dev/null

find "$RELEASE_ROOT" -mindepth 1 -maxdepth 1 -type d ! -name "$TIMESTAMP" -exec rm -rf {} +
find /opt/lab-recruitment/backups -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cleanup
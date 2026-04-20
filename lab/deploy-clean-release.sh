#!/bin/bash
set -euo pipefail

TIMESTAMP="$1"
RELEASE_ROOT="/opt/lab-recruitment/source/releases"
NEW_RELEASE="$RELEASE_ROOT/$TIMESTAMP"
ARCHIVE_FILE="/tmp/base-release.tar"
FRONTEND_TMP="/tmp/frontend-dist-clean"
RUNTIME_JAR="/opt/lab-recruitment/runtime/lab-recruitment-1.0.0.jar"
WEBROOT="/var/www/lab-recruitment"
SERVICE_NAME="lab-recruitment.service"
TEMP_PREV_JAR="/tmp/lab-recruitment-prev-${TIMESTAMP}.jar"
TEMP_PREV_WEB="/tmp/lab-recruitment-prev-web-${TIMESTAMP}"

cleanup() {
  rm -f /tmp/base-release.tar /tmp/deploy-clean-release.sh /tmp/latest-workspace.patch
  rm -rf /tmp/frontend-dist-clean /tmp/frontend-dist-hotfix /tmp/frontend-dist-delivery-status-fix /tmp/lab-recruitment-frontend-dist
  rm -f /tmp/lab-recruitment-frontend-dist.tar.gz /tmp/lab-recruitment-frontend-dist.zip
  rm -f "$TEMP_PREV_JAR"
  rm -rf "$TEMP_PREV_WEB"
}

rm -rf "$NEW_RELEASE"
mkdir -p "$NEW_RELEASE"
tar -xf "$ARCHIVE_FILE" -C "$NEW_RELEASE"

if [ -f /tmp/latest-workspace.patch ] && [ -s /tmp/latest-workspace.patch ]; then
  cd "$NEW_RELEASE"
  git apply --whitespace=nowarn /tmp/latest-workspace.patch
fi

cd "$NEW_RELEASE"
mvn -q -DskipTests clean package

cp "$RUNTIME_JAR" "$TEMP_PREV_JAR"
mkdir -p "$TEMP_PREV_WEB"
if [ -d "$WEBROOT" ]; then
  cp -a "$WEBROOT/." "$TEMP_PREV_WEB/"
fi

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
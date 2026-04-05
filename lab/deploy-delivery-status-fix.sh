#!/bin/bash
set -e

TIMESTAMP="$1"
SOURCE_DIR="/opt/lab-recruitment/source/releases/6900059"
FRONTEND_TMP="/tmp/frontend-dist-delivery-status-fix"
BACKUP_DIR="/opt/lab-recruitment/backups/${TIMESTAMP}-delivery-status-fix"

cd "$SOURCE_DIR"
mvn -q -DskipTests package

mkdir -p "$BACKUP_DIR/frontend"
cp /opt/lab-recruitment/runtime/lab-recruitment-1.0.0.jar "$BACKUP_DIR/"
if [ -d /var/www/lab-recruitment ]; then
  cp -r /var/www/lab-recruitment/. "$BACKUP_DIR/frontend/"
fi

cp target/lab-recruitment-1.0.0.jar /opt/lab-recruitment/runtime/lab-recruitment-1.0.0.jar
rm -rf /var/www/lab-recruitment/*
cp -r "$FRONTEND_TMP"/. /var/www/lab-recruitment/
chown -R www-data:www-data /var/www/lab-recruitment

systemctl restart lab-recruitment.service
systemctl is-active lab-recruitment.service
curl -I -s http://127.0.0.1:8081/api/labs/list?pageNum=1\&pageSize=1 | head -n 1
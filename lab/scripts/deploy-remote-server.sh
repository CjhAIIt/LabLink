#!/usr/bin/env bash
set -euo pipefail

APP_ROOT=/opt/lab-recruitment
APP_DIR=$APP_ROOT/app
WEB_ROOT=/var/www/lab-recruitment
DATA_ROOT=/data/lab-recruitment
UPLOAD_ZIP=/home/ubuntu/lab-recruitment-upload/lab-recruitment-deploy-min.zip
DB_NAME=lab_recruitment
DB_USER=lab_app
DB_PASS=__DB_PASS__
JWT_SECRET_VALUE=__JWT_SECRET__
MAIL_USERNAME_VALUE=__MAIL_USERNAME__
MAIL_PASSWORD_VALUE=__MAIL_PASSWORD__
SKIP_DB="${SKIP_DB:-0}"

echo '[1/8] prepare directories'
mkdir -p "$APP_ROOT" "$WEB_ROOT" "$DATA_ROOT/uploads" "$DATA_ROOT/judge-work" "$DATA_ROOT/logs"
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"
unzip -oq "$UPLOAD_ZIP" -d "$APP_DIR"

echo '[2/8] write backend env'
cat > "$APP_ROOT/backend.env" <<ENVEOF
SERVER_PORT=8081
DB_URL=jdbc:mysql://127.0.0.1:3306/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
DB_USERNAME=$DB_USER
DB_PASSWORD=$DB_PASS
JWT_SECRET=$JWT_SECRET_VALUE
MAIL_USERNAME=$MAIL_USERNAME_VALUE
MAIL_PASSWORD=$MAIL_PASSWORD_VALUE
FILE_UPLOAD_PATH=/app/uploads/
JUDGE_PYTHON_COMMAND=python3
JUDGE_C_COMPILER_COMMAND=gcc
JUDGE_CPP_COMPILER_COMMAND=g++
JUDGE_JAVA_COMPILER_COMMAND=javac
JUDGE_JAVA_COMMAND=java
JUDGE_WORK_DIR=/app/judge-work
APP_SECURITY_ALLOWED_ORIGINS_CSV=http://101.35.79.76
ENVEOF
chmod 600 "$APP_ROOT/backend.env"

echo '[3/8] write docker-compose'
cat > "$APP_ROOT/docker-compose.yml" <<COMPOSEEOF
version: '3.8'
services:
  backend:
    build:
      context: /opt/lab-recruitment/app
      dockerfile: Dockerfile.backend
    container_name: lab-backend
    restart: unless-stopped
    network_mode: host
    env_file:
      - /opt/lab-recruitment/backend.env
    volumes:
      - /data/lab-recruitment/uploads:/app/uploads
      - /data/lab-recruitment/judge-work:/app/judge-work
      - /data/lab-recruitment/logs:/app/logs
COMPOSEEOF
chmod 600 "$APP_ROOT/docker-compose.yml"

if [[ "$SKIP_DB" == "1" ]]; then
  echo '[4/8] SKIP_DB=1, skip mysql operations'
else
  echo '[4/8] prepare mysql database and user'
  mysql --defaults-file=/etc/mysql/debian.cnf <<SQLEOF
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASS';
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
ALTER USER '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASS';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQLEOF

  USER_TABLE_COUNT=$(mysql --defaults-file=/etc/mysql/debian.cnf -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='t_user';")
  if [ "$USER_TABLE_COUNT" = "0" ]; then
    echo '[4/8] import init.sql into empty database'
    mysql --defaults-file=/etc/mysql/debian.cnf "$DB_NAME" < "$APP_DIR/src/main/resources/init.sql"
  else
    echo '[4/8] database already initialized, skip init.sql'
  fi
fi

echo '[5/8] build and start backend container'
cd "$APP_ROOT"
docker-compose down --remove-orphans || true
docker-compose build backend
docker-compose up -d backend
sleep 10

echo '[6/8] build frontend static files'
docker build -t lab-frontend-build "$APP_DIR/frontend"
docker rm -f lab-frontend-export >/dev/null 2>&1 || true
docker create --name lab-frontend-export lab-frontend-build >/dev/null
find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
docker cp lab-frontend-export:/usr/share/nginx/html/. "$WEB_ROOT"/
docker rm -f lab-frontend-export >/dev/null

echo '[7/8] write nginx site config'
cat > /etc/nginx/sites-available/lab-recruitment <<'NGINXEOF'
server {
    listen 80 default_server;
    server_name 101.35.79.76 _;

    root /var/www/lab-recruitment;
    index index.html;
    client_max_body_size 20m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;
}
NGINXEOF
ln -sf /etc/nginx/sites-available/lab-recruitment /etc/nginx/sites-enabled/lab-recruitment
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo '[8/8] verify backend and frontend'
curl -i -sS 'http://127.0.0.1:8081/labs/list?pageNum=1&pageSize=1' | head -n 20
curl -i -sS 'http://127.0.0.1/api/labs/list?pageNum=1&pageSize=1' | head -n 20
curl -i -sS 'http://127.0.0.1/' | head -n 20

echo 'DEPLOY_FIXED_OK'

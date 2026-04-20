#!/usr/bin/env bash
set -euo pipefail

SITE_FILE="/etc/nginx/sites-available/lab-recruitment"
SELF_KEY="/etc/ssl/private/lablink-selfsigned.key"
SELF_CRT="/etc/ssl/certs/lablink-selfsigned.crt"
MODE=""
DOMAIN=""
EMAIL=""
SERVER_IP=""

usage() {
  cat <<'EOF'
Usage:
  lablink-enable-https.sh --self-signed-ip 101.35.79.76
  lablink-enable-https.sh --domain app.example.com --email admin@example.com
EOF
}

render_http_block() {
  cat <<EOF
server {
    listen 80 default_server;
    server_name $1;

    root /var/www/lab-recruitment;
    index index.html;
    client_max_body_size 20m;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
}

render_self_signed_site() {
  cat <<EOF
server {
    listen 80 default_server;
    server_name $SERVER_IP _;

    root /var/www/lab-recruitment;
    index index.html;
    client_max_body_size 20m;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

server {
    listen 443 ssl http2;
    server_name $SERVER_IP _;

    ssl_certificate $SELF_CRT;
    ssl_certificate_key $SELF_KEY;
    ssl_session_timeout 1d;
    ssl_session_cache shared:LabLinkSSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;

    root /var/www/lab-recruitment;
    index index.html;
    client_max_body_size 20m;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --self-signed-ip)
      MODE="self-signed"
      SERVER_IP="$2"
      shift 2
      ;;
    --domain)
      MODE="letsencrypt"
      DOMAIN="$2"
      shift 2
      ;;
    --email)
      EMAIL="$2"
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

if [[ "$MODE" == "self-signed" ]]; then
  if [[ -z "$SERVER_IP" ]]; then
    echo "Missing server IP" >&2
    exit 1
  fi

  openssl req -x509 -nodes -days 365 \
    -newkey rsa:2048 \
    -keyout "$SELF_KEY" \
    -out "$SELF_CRT" \
    -subj "/CN=$SERVER_IP" \
    -addext "subjectAltName = IP:$SERVER_IP"

  render_self_signed_site > "$SITE_FILE"
  nginx -t
  systemctl reload nginx
  echo "Self-signed HTTPS enabled for https://$SERVER_IP/"
  exit 0
fi

if [[ "$MODE" == "letsencrypt" ]]; then
  if [[ -z "$DOMAIN" || -z "$EMAIL" ]]; then
    echo "Domain mode requires --domain and --email" >&2
    exit 1
  fi

  if ! command -v certbot >/dev/null 2>&1; then
    echo "certbot not installed" >&2
    exit 1
  fi

  render_http_block "$DOMAIN" > "$SITE_FILE"
  nginx -t
  systemctl reload nginx

  certbot --nginx --non-interactive --agree-tos --redirect \
    -m "$EMAIL" \
    -d "$DOMAIN"

  echo "HTTPS enabled for https://$DOMAIN/"
  exit 0
fi

usage >&2
exit 1

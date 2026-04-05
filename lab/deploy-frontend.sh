#!/bin/bash

# 云服务器前端部署脚本
# 使用方法: sudo ./deploy-frontend.sh

set -e

echo "========================================="
echo "    大学计算机竞赛实验室招新系统"
echo "    前端部署脚本"
echo "========================================="

# 配置变量
FRONTEND_DIST_PATH="./frontend/dist"
DEPLOY_PATH="/var/www/html"
NGINX_CONF_PATH="/etc/nginx/sites-available/lab-recruitment"
NGINX_ENABLED_PATH="/etc/nginx/sites-enabled"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "请使用 sudo 运行此脚本"
    exit 1
fi

echo ""
echo "步骤 1: 检查前端打包文件..."
if [ ! -d "$FRONTEND_DIST_PATH" ]; then
    echo "错误: 前端打包文件不存在，请先运行 'cd frontend && npm run build'"
    exit 1
fi
echo "✓ 前端打包文件检查通过"

echo ""
echo "步骤 2: 安装 Nginx..."
if ! command -v nginx &> /dev/null; then
    apt-get update
    apt-get install nginx -y
    echo "✓ Nginx 安装完成"
else
    echo "✓ Nginx 已安装"
fi

echo ""
echo "步骤 3: 创建部署目录..."
mkdir -p $DEPLOY_PATH
echo "✓ 部署目录创建完成"

echo ""
echo "步骤 4: 复制前端文件到部署目录..."
rm -rf $DEPLOY_PATH/*
cp -r $FRONTEND_DIST_PATH/* $DEPLOY_PATH/
echo "✓ 前端文件复制完成"

echo ""
echo "步骤 5: 配置 Nginx..."
cat > $NGINX_CONF_PATH << 'EOF'
server {
    listen 80;
    server_name _;

    root /var/www/html;
    index index.html;

    # 前端静态文件
    location / {
        try_files $uri $uri/ /index.html;
        
        # 缓存配置
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # 代理后端 API 请求
    location /api/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript 
               application/x-javascript application/xml+rss 
               application/javascript application/json;
}
EOF

echo "✓ Nginx 配置文件创建完成"

echo ""
echo "步骤 6: 启用站点配置..."
ln -sf $NGINX_CONF_PATH $NGINX_ENABLED_PATH/lab-recruitment

# 删除默认配置（如果存在）
if [ -f "$NGINX_ENABLED_PATH/default" ]; then
    rm $NGINX_ENABLED_PATH/default
fi

echo "✓ 站点配置已启用"

echo ""
echo "步骤 7: 测试 Nginx 配置..."
nginx -t
echo "✓ Nginx 配置测试通过"

echo ""
echo "步骤 8: 重启 Nginx..."
systemctl restart nginx
systemctl enable nginx
echo "✓ Nginx 已重启并设置为开机自启"

echo ""
echo "步骤 9: 配置防火墙..."
if command -v ufw &> /dev/null; then
    ufw allow 80/tcp
    ufw allow 443/tcp
    echo "✓ 防火墙规则已添加"
fi

echo ""
echo "========================================="
echo "    部署完成！"
echo "========================================="
echo ""
echo "访问地址:"
echo "  HTTP:  http://$(curl -s ifconfig.me)"
echo "  本地:  http://localhost"
echo ""
echo "后端 API 地址:"
echo "  http://$(curl -s ifconfig.me)/api/"
echo ""
echo "查看 Nginx 日志:"
echo "  访问日志: tail -f /var/log/nginx/access.log"
echo "  错误日志: tail -f /var/log/nginx/error.log"
echo ""
echo "重启 Nginx:"
echo "  sudo systemctl restart nginx"
echo ""
echo "========================================="

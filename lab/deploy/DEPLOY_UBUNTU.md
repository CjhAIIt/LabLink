# LabLink Ubuntu 部署说明

## 目录约定

- `/opt/lablink/backend`
- `/opt/lablink/frontend`
- `/opt/lablink/uploads/public`
- `/opt/lablink/uploads/protected`
- `/opt/lablink/judge-work`
- `/opt/lablink/logs`
- `/opt/lablink/mysql`

## 一、部署前检查

```bash
cd /opt/lablink/lab/frontend && npm ci && npm run build
cd /opt/lablink/lab && mvn -q -DskipTests package
sudo ss -lntp | grep -E '(:80|:443|:8081|:3306|:6379|:5672)'
sudo nginx -t
```

如果生产环境使用 Redis / RabbitMQ，请先确认：

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
sudo rabbitmqctl status
```

## 二、传统部署

### 1. 安装依赖

```bash
sudo apt update
sudo apt install -y openjdk-11-jdk maven nginx mysql-server redis-server rabbitmq-server nodejs npm
sudo systemctl enable --now nginx mysql redis-server rabbitmq-server
```

### 2. 初始化目录

```bash
sudo mkdir -p /opt/lablink/{backend,frontend,uploads/public,uploads/protected,judge-work,logs}
sudo chown -R ubuntu:ubuntu /opt/lablink
```

### 3. 数据库备份与发布前备份

```bash
mkdir -p /opt/lablink/backup
mysqldump -uroot -p --single-transaction --routines --triggers lab_recruitment > /opt/lablink/backup/lab_recruitment_$(date +%F_%H%M%S).sql
cp -f /opt/lablink/backend/lab-recruitment-1.0.0.jar /opt/lablink/backup/lab-recruitment-1.0.0.jar.$(date +%F_%H%M%S) 2>/dev/null || true
tar -czf /opt/lablink/backup/frontend_dist_$(date +%F_%H%M%S).tar.gz -C /opt/lablink/frontend dist 2>/dev/null || true
```

### 4. 后端发布

```bash
cd /opt/lablink/lab
mvn -q -DskipTests package
cp -f target/lab-recruitment-1.0.0.jar /opt/lablink/backend/lab-recruitment-1.0.0.jar
cp -f deploy/env/backend-host.env.example /opt/lablink/backend/backend-host.env
```

按生产环境修改 `/opt/lablink/backend/backend-host.env`。

### 5. 前端发布

```bash
cd /opt/lablink/lab/frontend
cp -f .env.production.example .env.production
npm ci
npm run build
rsync -av --delete dist/ /opt/lablink/frontend/dist/
```

### 6. 安装 systemd 与 Nginx

```bash
sudo cp /opt/lablink/lab/deploy/systemd/lablink.service /etc/systemd/system/lablink.service
sudo systemctl daemon-reload
sudo systemctl enable lablink
sudo systemctl restart lablink

sudo cp /opt/lablink/lab/deploy/nginx/lablink.conf /etc/nginx/sites-available/lablink.conf
sudo ln -sf /etc/nginx/sites-available/lablink.conf /etc/nginx/sites-enabled/lablink.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

## 三、Docker 部署

### 1. 准备环境变量

```bash
cd /opt/lablink/lab/deploy
cp env/docker-compose.env.example env/docker-compose.env
```

修改 `env/docker-compose.env` 中的数据库、JWT、邮箱、AI 配置和站点域名。

### 2. 构建并启动

```bash
cd /opt/lablink/lab/deploy
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml build
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml up -d
```

如果需要 Redis / RabbitMQ：

```bash
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml --profile optional up -d
```

### 3. 常用运维

```bash
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml ps
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml logs -f backend
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml restart backend frontend
docker compose --env-file env/docker-compose.env -f docker-compose.ubuntu.yml down
```

## 四、上线后验证

```bash
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/api/auth/captcha
sudo systemctl status lablink --no-pager
sudo journalctl -u lablink -n 200 --no-pager
tail -n 200 /opt/lablink/logs/application.log
```

重点验证：

- 登录页、学生端、教师端、管理端可访问
- `/api` 反向代理正常
- 手机端访问 `/m/login`、`/m/student/dashboard`、`/m/admin/dashboard` 正常
- 文件上传、消息通知、签到、笔试、AI 面试记录链路正常

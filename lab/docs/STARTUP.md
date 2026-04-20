# 启动说明

## 环境要求

- `JDK 11` 或更高
- `Maven 3.6+`
- `Node.js 18+`
- `MySQL 8.x`

## 初始化数据库

```sql
CREATE DATABASE lab_recruitment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -uroot -p lab_recruitment < src/main/resources/init.sql
```

默认数据库账号：

- 用户名：`root`
- 密码：`123456`

## 启动后端

```bash
mvn clean package -DskipTests
java -jar target/lab-recruitment-1.0.0.jar
```

默认地址：`http://localhost:8081`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:3000`

## Windows 本地启动脚本

```powershell
Copy-Item .env.cloud.example .env.cloud
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

## Docker 本地联调

```bash
docker compose -f docker-compose.local.yml up -d
```

默认端口：

- MySQL：`3307`
- 后端：`8081`
- 前端：`80`

## 默认测试账号

默认密码：`Lab123456`

- `superadmin`
- `cs_admin`
- `ai_admin`
- `ee_admin`
- `20231001`
- `20231003`

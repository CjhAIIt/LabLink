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
- 密码：`cjh041217`

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

### 换端口启动

Vite 默认端口是 3000，被占用时可以直接换：

```bash
cd frontend
npx vite --port 5193 --host 127.0.0.1
```

**换端口不会导致登录失败**：后端在非生产环境下放行回环地址的任意端口，
开发代理也会在转发前抹掉 `Origin` 头，所以登录、发布公告这类写操作都正常。
若出现 `403 Invalid CORS request`，说明后端跑在 `production` 环境，
需要把 `APP_SECURITY_ALLOWED_ORIGINS_CSV` 设为前端实际来源。

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

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| `superadmin` | 学校管理员 | 全部学院与实验室的治理视角 |
| `cs_admin` | 学院管理员 | 计算机与软件工程学院 |
| `ai_admin` | 学院管理员 | 人工智能学院 |
| `ee_admin` | 学院管理员 | 电子工程学院 |
| `me_admin` | 学院管理员 | 机械工程学院 |
| `mgt_admin` | 学院管理员 | 管理工程学院 |
| `art_admin` | 学院管理员 | 艺术设计学院 |
| `fla_admin` | 学院管理员 | 通识教育与外国语学院 |
| `20239999` | 学生 | 学生视角 |

登录失败时先看浏览器控制台或页面提示：

- `Invalid CORS request` / `拒绝访问`：前端来源不在后端白名单，见 [DEPLOYMENT-SECURITY.md](./DEPLOYMENT-SECURITY.md) 第 3 节
- `请求过于频繁，请稍后再试`：登录接口限流为 60 秒内 8 次，等待后重试
- `账号或密码错误`：账号不存在或口令不对，注意 `20231001`、`20231003` 这两个账号在当前数据库中并不存在

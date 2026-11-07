# 部署与安全配置说明

本文档说明生产部署必须配置的环境变量、启动时的安全自检规则，以及各配置项的取值建议。

## 1. 必须配置的环境变量

`docker-compose.cloud.yml` 中所有敏感项都使用 `${VAR:?说明}` 形式，**缺失或为空时 `docker compose` 直接报错，不会带着弱配置启动**。

| 变量 | 说明 | 生成方式 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 口令 | `openssl rand -base64 24` |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字符 | `openssl rand -hex 32` |
| `APP_SECURITY_ALLOWED_ORIGINS_CSV` | 允许访问 API 的前端来源，逗号分隔 | 手填真实域名 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 邮箱服务凭据，注册验证码与密码重置依赖 | 邮箱服务商提供 |

可选：

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `APP_JUDGE_ENABLED` | `false` | 是否开启在线判题，见第 4 节 |
| `APP_SECURITY_ENVIRONMENT` | `local` | 生产环境必须为 `production`，compose 中已固定设置 |

## 2. 启动安全自检

后端在容器 refresh 阶段执行安全自检（`StartupSecurityValidator`），**早于 Web 服务器开始接受请求**。

- `APP_SECURITY_ENVIRONMENT=production` 时，以下任一情况会导致进程启动失败：
  - JWT 密钥为空、少于 32 字符，或仍是仓库中出现过的默认值
  - 数据库口令仍是仓库中出现过的默认值
  - CORS 来源白名单为空、包含通配符 `*`，或仍是示例占位域名
- `local` 环境下只打印 WARN，不阻断启动，方便本地开发。

对应的单元测试见 `src/test/java/com/lab/recruitment/config/StartupSecurityValidatorTest.java`。

`scripts/cloud-preflight.sh` 会在部署前做同样的检查，建议在 `docker compose up` 之前先执行：

```bash
bash scripts/cloud-preflight.sh .env.cloud
```

## 3. CORS 与端口暴露

### 3.1 CORS

来源白名单来自 `AppSecurityProperties.resolveAllowedOrigins()`，优先级为：

1. 环境变量 `APP_SECURITY_ALLOWED_ORIGINS_CSV`（逗号分隔）
2. `application.yml` 中的 `app.security.allowed-origins` 列表（默认仅本机开发地址）

行为说明：

- 白名单为具体域名时使用 `allowedOrigins` + `allowCredentials(true)`
- 白名单包含 `*` 时改用 `allowedOriginPatterns` 并**关闭** `allowCredentials`，避免出现「任意来源 + 携带凭据」的组合

### 3.2 端口

`docker-compose.cloud.yml` 中 MySQL 与后端只绑定回环地址：

```yaml
ports:
  - "127.0.0.1:3306:3306"
  - "127.0.0.1:8081:8081"
```

对外部署统一走 frontend 容器的 nginx（`0.0.0.0:80`），便于在代理层统一配置 TLS、安全响应头与访问日志。

如果需要从外部直连数据库，请使用 SSH 隧道，不要把 3306 映射到公网。

### 3.3 文件上传

`/file/upload` 需要登录后才能调用（`SecurityConfig` 中已从 `permitAll` 移除），`/file/view` 与 `/uploads/**` 保持公开以支持外链预览。

前端所有上传入口（`FileUpload.vue` 组件、`Space.vue` / `LabWorkspace.vue` 的自定义上传）均已携带 `Authorization` 请求头，无需额外改动。

## 4. 在线判题（高风险功能）

在线判题会**编译并执行用户提交的 Python / Java / C / C++ 代码**。当前实现只做了以下限制：

- 墙钟超时（默认 5 秒）
- Java 进程堆内存上限（默认 128 MB）
- 输出捕获上限（默认 64 KB）
- 独立的临时工作目录，结束后清理
- 接口级限流（默认每 IP 每分钟 10 次）

**这些都不构成真正的安全隔离**：没有容器或用户隔离，没有网络限制，评测进程与应用同用户运行。因此：

- 判题功能**默认关闭**（`APP_JUDGE_ENABLED=false`）
- 生产镜像**默认不安装**编译器与解释器；确需开启时构建时传入 `--build-arg INSTALL_JUDGE_TOOLCHAIN=true`
- 开启前请自行准备隔离方案，推荐顺序：
  1. 把判题拆成独立的非特权容器，`--network none` + 只读根文件系统 + `--memory` / `--pids-limit` / `--cpus` 限制
  2. 或使用 nsjail / gVisor / Firecracker 等沙箱
  3. 或对接第三方判题服务

在完成隔离之前，不要在公网环境开启该功能。

## 5. 日志

`application.yml` 默认**不打印 SQL**，避免手机号、邮箱、密码哈希等绑定参数写入日志。

本地开发需要看 SQL 时，用 dev profile 启动：

```bash
java -jar app.jar --spring.profiles.active=dev
```

或本地的 `scripts/start-local.ps1`（已内置该 profile）。

## 6. 默认账号

`init.sql` 会写入以下示例账号，**全部共用同一个默认口令**：

| 账号 | 角色 |
| --- | --- |
| `superadmin` | 学校管理员 |
| `cs_admin` / `ai_admin` / `ee_admin` | 学院管理员 |
| `20231001` / `20231003` | 学生 |

生产部署前必须：

1. 修改这些账号的口令
2. 清除或停用不需要的示例账号
3. 不要把默认口令写进任何对外文档

`PasswordEncodingTest` 会校验 `init.sql` 中的哈希与文档记录的默认口令一致，改动其一就要同步另一处。

## 7. 部署前检查清单

- [ ] `.env.cloud` 中四项必填变量均已替换为真实值，且不是仓库中的默认值
- [ ] `bash scripts/cloud-preflight.sh .env.cloud` 通过
- [ ] `superadmin` 等示例账号口令已修改，必要账号已停用
- [ ] nginx 已配置 TLS 证书
- [ ] `APP_JUDGE_ENABLED` 与实际意图一致（不确定就保持 `false`）
- [ ] `docker compose --env-file .env.cloud -f docker-compose.cloud.yml up -d --build` 启动后日志中出现「生产环境安全自检通过」

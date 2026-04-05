# 安徽信息工程学院实验室管理平台

基于 `Spring Boot 2.7`、`MyBatis-Plus`、`MySQL 8`、`Vue 3`、`Element Plus` 的全校版实验室管理平台，面向安徽信息工程学院的学校管理员、学院管理员、实验室负责人和学生，覆盖学院主数据、实验室档案、实验室创建审批、成员申请、公告发布和统计分析等场景。

## 项目定位

- 面向范围：安徽信息工程学院全校
- 组织结构：学校 -> 学院 -> 实验室 -> 成员
- 当前角色：学校管理员、学院管理员 / 实验室负责人、学生
- 对齐文档：[AIIT-SCHOOL-EDITION.md](/C:/Users/cjh/IdeaProjects/lab/docs/AIIT-SCHOOL-EDITION.md)

## 目录结构

```text
.
├─ src/main/java                     后端业务代码
├─ src/main/resources
│  ├─ application.yml               后端主配置
│  ├─ init.sql                      数据库初始化脚本
│  └─ mapper                        MyBatis Mapper XML
├─ frontend                         Vue 3 前端
├─ scripts                          启动、检查、部署脚本
├─ docker-compose.local.yml         本地 Docker 编排
├─ docker-compose.cloud.yml         云端 Docker 编排
└─ docs                             说明文档
```

## 技术栈

- 后端：`Java 11+`、`Spring Boot 2.7.14`、`Spring Security`、`MyBatis-Plus`
- 前端：`Vue 3`、`Vite`、`Element Plus`、`Pinia`
- 数据库：`MySQL 8.x`
- 构建：`Maven`、`npm`
- 部署：`Docker Compose`

## 默认数据库配置

- 数据库名：`lab_recruitment`
- 用户名：`root`
- 密码：`cjh041217`

默认配置见 [application.yml](/C:/Users/cjh/IdeaProjects/lab/src/main/resources/application.yml)。

如果本机尚未创建数据库，可执行：

```sql
CREATE DATABASE lab_recruitment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后导入初始化脚本：

```bash
mysql -uroot -p lab_recruitment < src/main/resources/init.sql
```

## 本地启动

### 后端

```bash
mvn clean package -DskipTests
java -jar target/lab-recruitment-1.0.0.jar
```

也可以直接在 IDEA 中运行：

```text
com.lab.recruitment.LabRecruitmentApplication
```

默认地址：`http://localhost:8081`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:3000`

## Windows 一键启动

先准备环境变量文件：

```powershell
Copy-Item .env.cloud.example .env.cloud
```

再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

脚本会启动：

- MySQL Docker 容器
- 后端服务
- 前端开发服务

默认端口：

- 前端：`http://localhost:3000/`
- 后端：`http://localhost:8081/`
- MySQL：`localhost:3307`

## Docker 启动

### 本地联调

```bash
docker compose -f docker-compose.local.yml up -d
```

### 云端部署

```bash
cp .env.cloud.example .env.cloud
docker compose --env-file .env.cloud -f docker-compose.cloud.yml up -d --build
```

## 默认示例账号

默认密码：`Lab123456`

- 学校管理员：`superadmin`
- 学院管理员：`cs_admin`
- 学院管理员：`ai_admin`
- 学院管理员：`ee_admin`
- 学生：`20231001`
- 学生：`20231003`

## 当前已对齐的全校版内容

- 平台名称统一为“安徽信息工程学院实验室管理平台”
- 前端登录、注册、平台介绍、菜单栏文案切换为学校版
- 初始化学院主数据切换为安徽信息工程学院全校版语境
- 初始化公告与实验室示例数据切换为学校版语境
- 文档新增学校版改造说明

## 当前仍需继续推进的需求

- 老师注册审批链
- 指导老师申请与变更流程
- 审批日志、状态机与历史记录
- 对应前后端审批页面补齐

## 相关文档

- [STARTUP.md](/C:/Users/cjh/IdeaProjects/lab/docs/STARTUP.md)
- [AIIT-SCHOOL-EDITION.md](/C:/Users/cjh/IdeaProjects/lab/docs/AIIT-SCHOOL-EDITION.md)
- [application.yml](/C:/Users/cjh/IdeaProjects/lab/src/main/resources/application.yml)
- [init.sql](/C:/Users/cjh/IdeaProjects/lab/src/main/resources/init.sql)

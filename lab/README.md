# LabLink 实验室管理平台

基于 `Spring Boot 2.7`、`MyBatis-Plus`、`MySQL 8`、`Vue 3`、`Element Plus` 的全校版实验室管理平台，面向安徽信息工程学院的学校管理员、学院管理员、实验室负责人和学生，覆盖学院主数据、实验室档案、实验室创建审批、成员申请、公告发布和统计分析等场景。前端已适配桌面浏览器与手机浏览器两套界面形态，并预留了原生打包的接入点。

## 项目定位

- 面向范围：安徽信息工程学院全校
- 组织结构：学校 -> 学院 -> 实验室 -> 成员
- 当前角色：学校管理员、学院管理员 / 实验室负责人、学生
- 对齐文档：[AIIT-SCHOOL-EDITION.md](/C:/Users/cjh/Documents/LabLink/lab/docs/AIIT-SCHOOL-EDITION.md)

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
- 密码：本地开发为 `cjh041217`，生产环境必须通过 `DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` 环境变量覆盖

默认配置见 [application.yml](/C:/Users/cjh/Documents/LabLink/lab/src/main/resources/application.yml)。

如果本机尚未创建数据库，可执行：

```sql
CREATE DATABASE lab_recruitment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后导入初始化脚本：

```bash
mysql -uroot -p lab_recruitment < src/main/resources/init.sql
```

## 生产部署必读

生产部署前必须配置密钥，否则后端会拒绝启动。完整说明见 [DEPLOYMENT-SECURITY.md](/C:/Users/cjh/Documents/LabLink/lab/docs/DEPLOYMENT-SECURITY.md)。

```bash
cp .env.cloud.example .env.cloud
# 逐项替换 MYSQL_ROOT_PASSWORD / JWT_SECRET / APP_SECURITY_ALLOWED_ORIGINS_CSV 等真实值
bash scripts/cloud-preflight.sh .env.cloud
docker compose --env-file .env.cloud -f docker-compose.cloud.yml up -d --build
```

要点：

- 后端在启动阶段执行安全自检，密钥仍是默认值、CORS 白名单含通配符等问题会直接终止进程
- MySQL 与后端端口只绑定 `127.0.0.1`，统一由 nginx 出网
- 在线判题会执行用户提交的代码，默认关闭，开启前请先阅读安全说明

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

**以下账号仅供本地开发使用，生产部署前必须修改口令并停用不需要的账号。**

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

## 前端界面升级

- 品牌标识统一为 LabLink，侧边栏、登录页、注册页、平台介绍页同步更新
- 全局样式补齐响应式断点（`960px` 侧边栏抽屉化、`768px` 移动端形态）与页面骨架类
- 学生端 / 教师端 / 管理端三套布局支持窄屏抽屉式侧边栏与遮罩层
- 新增移动端布局外壳与移动端页面体系，路由按视口宽度在桌面端与移动端之间自动分流
- 平台介绍页、实验室详情页在窗口重新可见时自动刷新数据
- 管理端综合检索新增学院、公告范围、显示条数筛选
- 路由改为按需加载，Element Plus 改为按需引入并统一中文语言包
- 抽取 AppShell / AuthLayout / MobileShell 三个共用外壳，三端桌面布局与公开页不再各写一份
- 侧边栏支持收成 64px 图标栏，移动端为抽屉，两种折叠状态相互独立
- 登录 / 注册 / 教师注册改为居中单栏卡片，平台介绍页改为导航 + 首屏 + 特性网格
- 移动端页面样式统一收敛到 assets/css/mobile.css，指标网格改为两列

详细说明见 [FRONTEND-UI-GUIDE.md](/C:/Users/cjh/Documents/LabLink/lab/docs/FRONTEND-UI-GUIDE.md) 与 [MOBILE-ADAPTATION.md](/C:/Users/cjh/Documents/LabLink/lab/docs/MOBILE-ADAPTATION.md)。

## 工程质量

- 后端补齐单元测试，`mvn test` 覆盖启动安全自检与在线判题开关
- 新增 GitHub Actions：后端构建测试打包、前端 ESLint 与构建
- 根目录补充 `.gitignore`，运行期日志、上传文件与构建产物不再入库
- 移除冗余的 vuex 依赖与未被路由引用的旧版布局组件
- 引入 Vitest 前端测试体系，路由互斥解析、门户角色、请求拦截器、主题切换共 33 项用例并接入 CI
- 修复会话失效后停留在工作台且数据为空的问题：401 时先清空本地会话再跳登录页

## 当前仍需继续推进的需求

- 老师注册审批链
- 指导老师申请与变更流程
- 审批日志、状态机与历史记录
- 对应前后端审批页面补齐

## 相关文档

- [STARTUP.md](/C:/Users/cjh/Documents/LabLink/lab/docs/STARTUP.md)
- [AIIT-SCHOOL-EDITION.md](/C:/Users/cjh/Documents/LabLink/lab/docs/AIIT-SCHOOL-EDITION.md)
- [DEPLOYMENT-SECURITY.md](/C:/Users/cjh/Documents/LabLink/lab/docs/DEPLOYMENT-SECURITY.md)
- [FRONTEND-UI-GUIDE.md](/C:/Users/cjh/Documents/LabLink/lab/docs/FRONTEND-UI-GUIDE.md)
- [FRONTEND-REDESIGN-PROMPT.md](/C:/Users/cjh/Documents/LabLink/lab/docs/FRONTEND-REDESIGN-PROMPT.md)
- [MOBILE-ADAPTATION.md](/C:/Users/cjh/Documents/LabLink/lab/docs/MOBILE-ADAPTATION.md)
- [Desktop_Android_Development_Guide.md](/C:/Users/cjh/Documents/LabLink/lab/docs/Desktop_Android_Development_Guide.md)
- [application.yml](/C:/Users/cjh/Documents/LabLink/lab/src/main/resources/application.yml)
- [init.sql](/C:/Users/cjh/Documents/LabLink/lab/src/main/resources/init.sql)

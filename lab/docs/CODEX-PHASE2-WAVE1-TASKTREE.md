# LabLink 二期第一波 Codex 任务树

本任务树基于当前仓库现状整理，不按原始需求文档“从零设计”，而是按现有 `Spring Boot 2.7 + MyBatis-Plus + Vue 3` 代码结构拆解为可连续执行的实施批次。

## 1. 当前仓库基线

### 已有能力

- 权限雏形已存在：`UserAccessServiceImpl`、`CurrentUserAccessor`、`SecurityConfig`
- 考勤雏形已存在：`AttendanceWorkflowServiceImpl` 及 `t_attendance_task / t_attendance_schedule / t_attendance_session / t_attendance_record`
- 设备雏形已存在：`Equipment`、`EquipmentBorrow`
- 统计雏形已存在：`StatisticsServiceImpl`、`StatisticsController`
- 文件上传雏形已存在：`FileUploadController`、`FileStorageService`
- 审计雏形已存在：`AuditLogServiceImpl`，但仍为同步直写数据库
- 学校版角色语义已存在：学校管理员、学院管理员、实验室管理员的访问画像已能推导

### 当前主要缺口

- 角色、菜单、按钮、数据范围没有形成统一 RBAC 模型
- 前端仍以静态路由和布局内硬编码菜单为主
- 成员资料、审核、归档没有独立领域模型
- 考勤缺少请假、修正日志、统一规则抽象、幂等控制
- 设备模块仍偏轻，缺少分类、维修、盘点闭环
- 搜索、统计、文件、审计、通知、缓存、MQ 都未形成统一基础设施
- 返回结构、异常结构、接口前缀仍不完全统一
- 运行时建表逻辑较重，后续应逐步收敛到 Flyway

### 本波明确冻结

- AI 模拟面试
- AI 正式面试
- 线下笔试
- 招新评分与录取链路
- Elasticsearch
- 微服务拆分

## 2. 实施顺序

1. T0 范围冻结与公共基线收口
2. T1 统一权限与数据范围
3. T2 成员资料、审核、归档
4. T3 考勤管理升级
5. T4 设备管理升级
6. T5 综合搜索
7. T6 统计分析与可视化
8. T7 统一文件存储
9. T8 Redis 缓存接入
10. T9 RabbitMQ 异步能力
11. T10 审计日志与通知中心
12. T11 数据库脚本、索引、接口文档交付

## 3. 任务明细

### T0. 范围冻结与公共基线收口

- 任务名称：冻结非本期模块，统一新接口的返回和异常规范，为后续模块接入打底
- 涉及目录：
  - `src/main/java/com/lab/recruitment/utils`
  - `src/main/java/com/lab/recruitment/exception`
  - `src/main/java/com/lab/recruitment/config`
  - `src/main/resources/application.yml`
  - `frontend/src/utils/request.js`
  - `frontend/src/router/index.js`
  - `frontend/src/layouts`
  - `frontend/src/views/student`
- 新增表：无
- 新增接口：
  - 无新增业务接口
  - 兼容策略：前端请求层同时兼容 `code=200` 和 `code=0`，后续新模块统一输出 `code=0`
- 页面清单：
  - 从菜单隐藏笔试、面试、招新评分相关入口
  - 保留旧页面文件，但不在本期导航暴露
- 验收条件：
  - 本期范围外页面不出现在导航和快捷入口中
  - 新增接口统一使用 `/api/**`
  - 新模块返回结构统一为 `{ code: 0, message: "success", data: ... }`
  - 全局异常处理返回统一 JSON，不再直接返回裸字符串

### T1. 统一权限与数据范围

- 任务名称：建立平台角色、菜单、按钮、数据范围四层权限模型
- 涉及目录：
  - `src/main/java/com/lab/recruitment/config`
  - `src/main/java/com/lab/recruitment/support`
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/mapper`
  - `src/main/resources/mapper`
  - `frontend/src/router`
  - `frontend/src/layouts`
  - `frontend/src/stores`
  - `frontend/src/api`
  - `frontend/src/components`
- 新增表：
  - `t_role`
  - `t_permission`
  - `t_menu`
  - `t_user_role`
  - `t_role_permission`
  - `t_role_menu`
- 复用表：
  - `t_user_identity`
  - `t_platform_post`
  - `t_lab_member`
  - `t_lab_teacher_relation`
- 新增接口：
  - `GET /api/auth/me`
  - `GET /api/auth/menus`
  - `GET /api/auth/permissions`
  - `GET /api/users/{id}/roles`
  - `PUT /api/users/{id}/roles`
  - `GET /api/users/{id}/data-scope`
- 页面清单：
  - 管理端用户与角色管理页
  - 管理端菜单权限配置页
  - 管理端按钮权限配置页
- 验收条件：
  - 统一角色包含 `SUPER_ADMIN / COLLEGE_ADMIN / LAB_ADMIN / TEACHER / STUDENT`
  - 后端接口不再只依赖硬编码 `ROLE_ADMIN / ROLE_SUPER_ADMIN`
  - 前端路由和菜单由服务端返回配置驱动
  - 学校、学院、实验室、个人四级数据范围校验可复用到各管理接口

### T2. 成员资料、审核、归档

- 任务名称：建立学生资料提交、审核、归档和版本历史闭环
- 涉及目录：
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/entity`
  - `src/main/java/com/lab/recruitment/dto`
  - `src/main/java/com/lab/recruitment/vo`
  - `src/main/java/com/lab/recruitment/mapper`
  - `frontend/src/views/student`
  - `frontend/src/views/admin`
  - `frontend/src/views/teacher`
  - `frontend/src/api`
  - `frontend/src/components`
- 新增表：
  - `t_student_profile`
  - `t_student_profile_review`
  - `t_student_profile_archive`
- 新增接口：
  - `GET /api/profiles/me`
  - `POST /api/profiles`
  - `PUT /api/profiles/{id}`
  - `POST /api/profiles/{id}/submit`
  - `GET /api/profile-reviews/pending`
  - `GET /api/profile-reviews/history`
  - `POST /api/profile-reviews/{id}/approve`
  - `POST /api/profile-reviews/{id}/reject`
  - `GET /api/profiles/{id}/archives`
- 页面清单：
  - 学生端 `我的资料`
  - 学生端 `提交审核`
  - 管理端 `成员资料审核页`
  - 管理端 `成员档案历史页`
  - 教师端 `成员资料查看页`
- 验收条件：
  - 资料状态支持 `DRAFT / PENDING / APPROVED / REJECTED / ARCHIVED`
  - 审核意见必填
  - 审核通过自动生成归档快照
  - 学生重复修改时保留历史版本
  - 管理端能查看审核记录与归档版本

### T3. 考勤管理升级

- 任务名称：在现有考勤流程上补齐规则、请假、修正、统计、图片与幂等
- 涉及目录：
  - `src/main/java/com/lab/recruitment/service/impl/AttendanceWorkflowServiceImpl.java`
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/entity`
  - `src/main/java/com/lab/recruitment/dto`
  - `src/main/java/com/lab/recruitment/mapper`
  - `frontend/src/views/admin`
  - `frontend/src/views/student`
  - `frontend/src/views/teacher`
  - `frontend/src/api/attendanceWorkflow.js`
- 新增表：
  - `t_attendance_leave`
  - `t_attendance_change_log`
- 复用并扩展表：
  - `t_attendance_task`
  - `t_attendance_schedule`
  - `t_attendance_session`
  - `t_attendance_record`
  - `t_attendance_photo`
- 新增接口：
  - `GET /api/attendance/rules`
  - `POST /api/attendance/rules`
  - `GET /api/attendance/plans`
  - `POST /api/attendance/plans`
  - `GET /api/attendance/sign/current`
  - `POST /api/attendance/sign`
  - `POST /api/attendance/leaves`
  - `GET /api/attendance/leaves/pending`
  - `POST /api/attendance/leaves/{id}/approve`
  - `POST /api/attendance/leaves/{id}/reject`
  - `POST /api/attendance/records/{id}/correct`
  - `GET /api/attendance/statistics`
- 页面清单：
  - 管理端 `考勤规则管理页`
  - 管理端 `考勤计划管理页`
  - 管理端 `请假审批页`
  - 管理端 `异常修正页`
  - 学生端 `签到页`
  - 学生端 `请假申请页`
  - 教师端 `考勤查看页`
- 验收条件：
  - 现有考勤模型继续可用，不推倒重建
  - 支持 `NORMAL / LATE / ABSENT / LEAVE / SUPPLEMENT`
  - 支持请假 `PENDING / APPROVED / REJECTED`
  - 图片通过统一文件服务上传，不再直接写路径
  - 高并发签到可防重复提交

### T4. 设备管理升级

- 任务名称：在现有设备台账基础上补齐分类、唯一编码、借还、维修、盘点闭环
- 涉及目录：
  - `src/main/java/com/lab/recruitment/entity`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/mapper`
  - `frontend/src/views/admin`
  - `frontend/src/views/student`
  - `frontend/src/views/teacher`
  - `frontend/src/components/admin/EquipmentManagementPanel.vue`
  - `frontend/src/api/equipment.js`
- 新增表：
  - `t_device_category`
  - `t_device_maintenance_record`
  - `t_device_inventory_record`
- 复用并扩展表：
  - `t_equipment`
  - `t_equipment_borrow`
- 新增接口：
  - `GET /api/devices/categories`
  - `POST /api/devices/categories`
  - `GET /api/devices`
  - `POST /api/devices`
  - `PUT /api/devices/{id}`
  - `GET /api/devices/{id}`
  - `GET /api/devices/borrow-records`
  - `POST /api/devices/{id}/borrow`
  - `POST /api/devices/borrow-records/{id}/return`
  - `GET /api/devices/maintenance-records`
  - `POST /api/devices/maintenance-records`
  - `GET /api/devices/inventory-records`
  - `POST /api/devices/inventory-records`
- 页面清单：
  - 管理端 `设备列表页`
  - 管理端 `设备详情页`
  - 管理端 `设备编辑页`
  - 管理端 `借还记录页`
  - 管理端 `维修记录页`
  - 管理端 `盘点记录页`
  - 学生端 `实验室设备查看页`
  - 教师端 `设备查看页`
- 验收条件：
  - 设备编号唯一校验
  - 支持按实验室、分类、状态筛选
  - 设备状态支持 `IDLE / IN_USE / BORROWED / MAINTAINING / SCRAPPED`
  - 借还状态支持 `BORROWING / RETURNED / OVERDUE`

### T5. 综合搜索

- 任务名称：建立统一搜索结果结构和分类搜索接口，优先使用 MySQL 模糊搜索
- 涉及目录：
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/mapper`
  - `src/main/resources/db/migration`
  - `frontend/src/views/admin/SearchCenter.vue`
  - `frontend/src/components`
  - `frontend/src/api`
- 新增表：
  - `t_search_keyword_hot`
- 新增接口：
  - `GET /api/search/global`
  - `GET /api/search/labs`
  - `GET /api/search/users`
  - `GET /api/search/profiles`
  - `GET /api/search/devices`
  - `GET /api/search/files`
- 页面清单：
  - 管理端 `综合搜索页`
  - 前端通用 `搜索结果分组组件`
  - 前端通用 `搜索栏组件`
- 验收条件：
  - 搜索结果统一返回 `type / id / title / subtitle / status / extra`
  - 支持实验室、用户、资料档案、设备、公告、考勤计划
  - 高频关键词可记录热词
  - 本期只使用 MySQL 索引和模糊匹配，不引入 ES

### T6. 统计分析与可视化

- 任务名称：在现有统计服务基础上重构管理看板接口和图表组件
- 涉及目录：
  - `src/main/java/com/lab/recruitment/service/impl/StatisticsServiceImpl.java`
  - `src/main/java/com/lab/recruitment/controller/StatisticsController.java`
  - `frontend/src/views/admin/Statistics.vue`
  - `frontend/src/views/teacher/Dashboard.vue`
  - `frontend/src/views/student/Dashboard.vue`
  - `frontend/src/components`
  - `frontend/src/api/statistics.js`
- 新增表：无
- 新增接口：
  - `GET /api/statistics/dashboard`
  - `GET /api/statistics/labs`
  - `GET /api/statistics/members`
  - `GET /api/statistics/attendance`
  - `GET /api/statistics/devices`
  - `GET /api/statistics/profiles`
- 页面清单：
  - 管理端 `统计看板页`
  - 教师端 `统计概览页`
  - 通用 `图表卡片组件`
  - 通用 `状态标签组件`
- 验收条件：
  - 支持时间区间筛选
  - 接口与图表组件解耦
  - 可展示实验室数、成员数、学院分布、出勤率、请假率、设备状态分布、审核通过率、待审核数

### T7. 统一文件存储

- 任务名称：把当前散落的文件上传逻辑收敛为统一文件服务和关联模型
- 涉及目录：
  - `src/main/java/com/lab/recruitment/config/FileStorageService.java`
  - `src/main/java/com/lab/recruitment/controller/FileUploadController.java`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/mapper`
  - `frontend/src/components/FileUpload.vue`
  - `frontend/src/utils/file.js`
  - `frontend/src/api`
- 新增表：
  - `t_file_object`
  - `t_business_file_relation`
- 新增接口：
  - `POST /api/files/upload`
  - `GET /api/files/{id}/preview`
  - `GET /api/files/{id}/download`
  - `GET /api/files/{id}/meta`
  - `POST /api/files/relations`
- 页面清单：
  - 通用 `上传组件`
  - 业务附件选择器
  - 文件预览弹窗
- 验收条件：
  - 上传、预览、下载统一走文件服务
  - 文件大小、类型校验统一
  - 业务模块只保存 `file_id` 或业务关联，不直接保存裸路径
  - 兼容本地存储，后续可平滑切 MinIO / OSS

### T8. Redis 缓存接入

- 任务名称：按明确场景接入 Redis，不把缓存当主存储
- 涉及目录：
  - `pom.xml`
  - `src/main/resources/application.yml`
  - `src/main/java/com/lab/recruitment/config`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/support`
- 新增表：无
- 新增接口：无公共接口
- 页面清单：无新增页面
- 验收条件：
  - 引入 `Spring Cache` 或 `RedisTemplate`
  - 覆盖实验室详情、权限菜单、统计数据、搜索结果缓存
  - 覆盖签到幂等锁
  - 更新操作具备缓存失效逻辑
  - TTL 与需求文档一致并集中配置

### T9. RabbitMQ 异步能力

- 任务名称：接入 RabbitMQ，承接通知、审计、统计刷新等异步链路
- 涉及目录：
  - `pom.xml`
  - `src/main/resources/application.yml`
  - `src/main/java/com/lab/recruitment/config`
  - `src/main/java/com/lab/recruitment/service`
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/support`
- 新增表：
  - `t_message_outbox`
  - `t_message_consume_log`
- 新增接口：无公共接口
- 页面清单：无新增页面
- 验收条件：
  - 配置交换机 `lablink.business.exchange / lablink.audit.exchange / lablink.notify.exchange`
  - 资料审核、考勤提醒、审计入库、统计刷新接入消息发送
  - 消费端支持重试、死信、幂等校验
  - 主事务失败时不发送成功消息

### T10. 审计日志与通知中心

- 任务名称：把关键操作审计和系统通知独立成基础模块
- 涉及目录：
  - `src/main/java/com/lab/recruitment/service/AuditLogService.java`
  - `src/main/java/com/lab/recruitment/service/impl/AuditLogServiceImpl.java`
  - `src/main/java/com/lab/recruitment/service/SystemNotificationService.java`
  - `src/main/java/com/lab/recruitment/controller`
  - `src/main/java/com/lab/recruitment/config`
  - `frontend/src/views/admin`
  - `frontend/src/api`
- 新增表：
  - 优先复用并扩展 `t_audit_log`
  - 优先复用 `t_system_notification`
- 新增接口：
  - `GET /api/audit/logs`
  - `GET /api/audit/logs/{id}`
  - `GET /api/notifications`
  - `POST /api/notifications/read`
- 页面清单：
  - 管理端 `审计日志页`
  - 管理端 `通知中心页`
- 验收条件：
  - 登录、资料提交、资料审核、考勤修正、设备借还、权限变更、数据导出都有审计记录
  - 审计日志经 MQ 异步入库
  - 日志支持按操作人、业务类型、时间区间过滤

### T11. 数据库脚本、索引、接口文档交付

- 任务名称：补齐 schema、索引、迁移脚本、接口文档与统一分页规范
- 涉及目录：
  - `src/main/resources/db/migration`
  - `src/main/resources/init.sql`
  - `src/main/java/com/lab/recruitment/config/DatabaseSchemaUpdate.java`
  - `src/main/java/com/lab/recruitment/config/PlatformAttendanceSchemaRunner.java`
  - `docs`
  - `frontend/src/api`
- 新增表：
  - 汇总前述任务新增表
- 新增接口：
  - 无新增业务接口
  - 输出接口文档：`docs/api-phase2-wave1.md` 或 OpenAPI 文件
- 页面清单：无新增页面
- 验收条件：
  - 所有核心表包含 `id / created_by / created_at / updated_by / updated_at / deleted / version`
  - 补齐 `user_id / lab_id / college_id / student_no / device_code / status / created_at` 索引
  - 新增表优先走 Flyway，逐步削减运行时建表逻辑
  - 输出数据库脚本与接口文档，可供联调和验收使用

## 4. 前端公共组件任务

以下组件不单独成任务，但必须穿插在 T1-T7 中同步建设：

- `frontend/src/components/common/PageTable.vue`
- `frontend/src/components/common/SearchBar.vue`
- `frontend/src/components/common/StatusTag.vue`
- `frontend/src/components/common/ChartCard.vue`
- `frontend/src/components/common/FileUploader.vue`

验收要求：

- 表格页具备统一分页、筛选、排序、空态、加载态
- 搜索栏支持关键字、状态、时间区间
- 状态标签统一映射颜色和文案
- 图表卡片支持加载骨架和空数据提示
- 上传组件支持类型校验、大小校验、预览、单多文件模式

## 5. 推荐目录落位

后端建议新增模块目录，避免继续全部堆在平铺包下：

- `src/main/java/com/lab/recruitment/module/auth`
- `src/main/java/com/lab/recruitment/module/profile`
- `src/main/java/com/lab/recruitment/module/attendance`
- `src/main/java/com/lab/recruitment/module/device`
- `src/main/java/com/lab/recruitment/module/search`
- `src/main/java/com/lab/recruitment/module/statistics`
- `src/main/java/com/lab/recruitment/module/file`
- `src/main/java/com/lab/recruitment/module/audit`
- `src/main/java/com/lab/recruitment/module/notify`

前端建议新增：

- `frontend/src/views/admin/phase2`
- `frontend/src/views/student/phase2`
- `frontend/src/views/teacher/phase2`
- `frontend/src/components/common`
- `frontend/src/api/modules`

## 6. 建议第一批直接开工项

如果按最小可交付路径启动，优先顺序建议是：

1. T0 公共基线收口
2. T1 权限与数据范围
3. T2 成员资料、审核、归档
4. T7 统一文件存储
5. T3 考勤升级

原因：

- 权限和数据范围是所有管理模块的前置条件
- 资料审核归档是本波最明确、最闭环、最能体现平台能力的模块
- 文件服务会影响资料、考勤图片、设备附件三条线
- 考勤现有基础最好，适合在统一基础设施到位后快速升级

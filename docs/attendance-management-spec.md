# LabLink 出勤管理功能完善方案

## 项目背景

LabLink 是一个高校实验室管理平台，技术栈如下：

- **后端**：Spring Boot 2.7.14 + Java 11 + MyBatis-Plus 3.5.15 + MySQL 8.0.33
- **前端**：Vue 3.3.0 + Element Plus 2.4.0 + Pinia + Vue Router 4
- **认证**：JWT Bearer Token，角色分为 STUDENT / TEACHER / ADMIN / SUPER_ADMIN
- **响应格式**：`{ code: 200|0|500, message: string, data: any }`
- **分页**：MyBatis-Plus `Page<T>`，参数为 `pageNum` / `pageSize`

---

## 现有出勤系统概述

### 已有实体

| 实体 | 表名 | 说明 |
|------|------|------|
| AttendanceTask | t_attendance_task | 学期级别的考勤任务，含学院、学期、起止日期、状态 |
| AttendanceSchedule | t_attendance_schedule | 每周排班，含星期、签到时间窗口 |
| AttendanceSession | t_attendance_session | 单次签到会话（60秒），含签到码、二维码、状态 |
| AttendanceRecord | t_attendance_record | 个人考勤记录，状态：SIGNED/LATE/ABSENT/LEAVE |
| AttendanceSessionRecord | t_attendance_session_record | 实时会话记录 |
| AttendanceLeave | t_attendance_leave | 请假申请，含审批流程 |
| AttendanceChangeLog | t_attendance_change_log | 状态变更审计日志 |
| AttendancePhoto | t_attendance_photo | 签到现场照片 |

### 已有 API 端点（`/attendance/*`）

- `POST /attendance/session/create` — 创建签到会话
- `GET /attendance/session/active` — 获取当前活跃会话
- `POST /attendance/session/sign` — 学生签到
- `GET /attendance/manage/list` — 考勤列表（管理员）
- `POST /attendance/manage/tag` — 标记缺勤/请假
- `GET /attendance/export` — 导出 Excel
- `/attendance-workflow/*` — 请假/补签审批流

---

## 待完善功能：类钉钉出勤管理

参考钉钉考勤功能，在现有基础上补充以下能力，使出勤管理更完整、更实用。

---

## 一、功能模块清单

### 1. 实时出勤看板（管理员/教师）

**目标**：管理员和教师可实时查看当前实验室成员的出勤状态，类似钉钉"今日考勤"面板。

**需求**：
- 展示今日应到人数、已签到、迟到、缺勤、请假人数
- 按状态筛选成员列表（已签到 / 迟到 / 缺勤 / 请假）
- 支持手动标记成员状态（缺勤 → 请假、缺勤 → 补签）
- 数据每 30 秒自动刷新（轮询）

**新增 API**：
```
GET /attendance/dashboard/today?labId={labId}
```
返回：
```json
{
  "date": "2026-04-17",
  "total": 20,
  "signed": 12,
  "late": 2,
  "absent": 4,
  "leave": 2,
  "members": [
    {
      "userId": 1,
      "realName": "张三",
      "studentId": "2021001",
      "status": "SIGNED",
      "signTime": "09:05:23",
      "avatarUrl": null
    }
  ]
}
```

---

### 2. 出勤统计报表（管理员/教师）

**目标**：按时间段、成员、状态维度统计出勤数据，支持导出。

**需求**：
- 按周/月/学期查看个人出勤率
- 展示出勤趋势折线图（Element Plus ECharts 或 Chart.js）
- 支持按学院、实验室、成员筛选
- 导出 Excel（Apache POI，已有基础）

**新增 API**：
```
GET /attendance/stats/summary?labId=&startDate=&endDate=&userId=
```
返回：
```json
{
  "userId": 1,
  "realName": "张三",
  "totalDays": 30,
  "signedDays": 25,
  "lateDays": 3,
  "absentDays": 2,
  "leaveDays": 0,
  "attendanceRate": 83.3,
  "dailyRecords": [
    { "date": "2026-04-01", "status": "SIGNED", "signTime": "09:02:11" }
  ]
}
```

```
GET /attendance/stats/batch?labId=&startDate=&endDate=   （批量，返回 List）
GET /attendance/stats/export?labId=&startDate=&endDate=  （导出 Excel）
```

---

### 3. 异常出勤处理（管理员）

**目标**：管理员可批量处理异常出勤记录（忘记打卡、迟到说明等）。

**需求**：
- 列出所有异常记录（ABSENT 且无请假）
- 支持批量标记为"已处理"并填写备注
- 支持单条修改状态（ABSENT → LEAVE / SIGNED）
- 变更记录写入 AttendanceChangeLog（已有）

**新增 API**：
```
GET  /attendance/anomaly/list?labId=&date=&pageNum=&pageSize=
POST /attendance/anomaly/handle
```
请求体：
```json
{
  "recordIds": [1, 2, 3],
  "targetStatus": "LEAVE",
  "remark": "事后补假条"
}
```

---

### 4. 请假管理增强

**目标**：完善现有请假流程，增加请假统计和批量审批。

**需求**：
- 管理员可查看待审批请假列表（分页）
- 支持批量审批（通过/拒绝）
- 请假记录与出勤记录联动（审批通过后自动更新 AttendanceRecord 状态为 LEAVE）
- 学生可查看自己的请假历史及审批状态

**新增/修改 API**：
```
GET  /attendance-workflow/leave/pending?labId=&pageNum=&pageSize=
POST /attendance-workflow/leave/batch-approve
```
请求体：
```json
{
  "leaveIds": [1, 2],
  "action": "APPROVE",
  "comment": "同意"
}
```

---

### 5. 出勤通知推送

**目标**：关键出勤事件触发系统通知（复用现有 SystemNotification 机制）。

**触发场景**：
| 事件 | 通知对象 | 内容 |
|------|----------|------|
| 签到会话开始 | 实验室全体成员 | "【签到提醒】XX实验室签到已开始，请在60秒内完成签到" |
| 成员缺勤 | 该成员本人 | "【出勤提醒】您今日（4月17日）未签到，请联系管理员" |
| 请假审批通过 | 申请人 | "【请假审批】您的请假申请已通过" |
| 请假审批拒绝 | 申请人 | "【请假审批】您的请假申请已被拒绝，原因：..." |

**实现方式**：复用 `PlatformEventPublisher` + `SystemNotification` 实体，无需新增表。

---


## 二、数据库变更

### 新增字段

```sql
-- t_attendance_record 增加备注字段（异常处理用）
ALTER TABLE t_attendance_record
  ADD COLUMN handle_remark VARCHAR(200) NULL COMMENT '异常处理备注',
  ADD COLUMN handled_by    BIGINT       NULL COMMENT '处理人ID',
  ADD COLUMN handled_at    DATETIME     NULL COMMENT '处理时间';

-- t_attendance_leave 增加批量审批支持
ALTER TABLE t_attendance_leave
  ADD COLUMN batch_id VARCHAR(64) NULL COMMENT '批量审批批次ID';
```

### 新增表

```sql
-- 出勤统计缓存表（可选，提升报表查询性能）
CREATE TABLE t_attendance_stats_cache (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id      BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  stat_date   DATE         NOT NULL,
  stat_month  VARCHAR(7)   NOT NULL COMMENT '格式 2026-04',
  signed_days INT          NOT NULL DEFAULT 0,
  late_days   INT          NOT NULL DEFAULT 0,
  absent_days INT          NOT NULL DEFAULT 0,
  leave_days  INT          NOT NULL DEFAULT 0,
  updated_at  DATETIME     NOT NULL,
  UNIQUE KEY uk_lab_user_month (lab_id, user_id, stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 三、前端页面清单

| 页面 | 路径 | 角色 | 说明 |
|------|------|------|------|
| 今日出勤看板 | `/admin/attendance-dashboard` | ADMIN/TEACHER | 实时看板，含状态筛选 |
| 出勤统计报表 | `/admin/attendance-stats` | ADMIN/TEACHER | 图表 + 表格 + 导出 |
| 异常出勤处理 | `/admin/attendance-anomaly` | ADMIN | 批量处理异常记录 |
| 请假审批列表 | `/admin/attendance-leave` | ADMIN | 批量审批请假 |
| 学生出勤历史 | `/student/attendance-history` | STUDENT | 个人出勤记录 |
| 移动端出勤 | `/m/attendance` | STUDENT | 签到 + 历史 + 请假 |

---

## 四、后端文件清单

### 新增/修改 Controller
- `AttendanceController.java` — 新增 dashboard、stats、anomaly 端点
- `AttendanceWorkflowController.java` — 新增批量审批端点

### 新增/修改 Service
- `AttendanceDashboardService.java` — 今日看板数据聚合
- `AttendanceStatsService.java` — 统计报表逻辑
- `AttendanceAnomalyService.java` — 异常处理逻辑
- `AttendanceWorkflowService.java` — 修改，增加批量审批

### 新增 DTO
- `AttendanceDashboardVO.java` — 看板响应
- `AttendanceStatsVO.java` — 统计响应
- `AttendanceAnomalyHandleDTO.java` — 异常处理请求
- `LeaveApprovalBatchDTO.java` — 批量审批请求

### 新增 Mapper
- `AttendanceStatsCacheMapper.java` — 统计缓存表操作

---

## 五、实现优先级

| 优先级 | 功能 | 理由 |
|--------|------|------|
| P0 | 今日出勤看板 | 核心需求，类钉钉最直观的功能 |
| P0 | 出勤统计报表 | 管理员最常用，支撑决策 |
| P1 | 异常出勤处理 | 减少管理员手动操作 |
| P1 | 请假管理增强（批量审批） | 提升审批效率 |
| P2 | 出勤通知推送 | 提升用户体验 |


---

## 六、与现有代码的集成要点

1. **复用 AttendanceRecord**：所有状态变更必须写 `AttendanceChangeLog`，保持审计完整性
2. **复用 PlatformEventPublisher**：通知推送通过现有事件机制，不引入新依赖
3. **复用 Excel 导出**：统计报表导出复用 Apache POI 现有工具类
4. **JWT 鉴权**：所有新端点按现有 `@PreAuthorize` 模式添加角色校验
5. **分页规范**：列表接口统一使用 `Page<T>` + `pageNum`/`pageSize`
6. **Flyway 迁移**：数据库变更通过新增 `V12__attendance_enhancement.sql` 执行

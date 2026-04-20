# Skill: LabLink 出勤管理功能实现

## 你的任务

你是一个全栈开发助手，负责为 LabLink 高校实验室管理平台实现"类钉钉出勤管理"功能。请严格按照本文档的规范和优先级逐步实现。

---

## 项目技术栈

- **后端**：Spring Boot 2.7.14 + Java 11 + MyBatis-Plus 3.5.15 + MySQL 8.0.33
- **前端**：Vue 3.3.0 + Element Plus 2.4.0 + Pinia + Vue Router 4 + Axios
- **认证**：JWT Bearer Token，角色：STUDENT / TEACHER / ADMIN / SUPER_ADMIN
- **API 响应格式**：`Result<T>` 包装，`{ code, message, data }`
- **分页**：MyBatis-Plus `Page<T>`，参数 `pageNum` / `pageSize`
- **数据库迁移**：Flyway，新文件命名为 `V12__attendance_enhancement.sql`

---

## 项目目录结构

```
E:\java\LabLink-new\
├── lab\                          # 后端 Spring Boot 项目
│   └── src\main\java\com\lab\recruitment\
│       ├── controller\           # REST 控制器
│       ├── service\              # 服务接口
│       ├── service\impl\         # 服务实现
│       ├── entity\               # MyBatis-Plus 实体
│       ├── dto\                  # 请求 DTO
│       ├── vo\                   # 响应 VO
│       ├── mapper\               # MyBatis Mapper
│       └── utils\Result.java     # 统一响应包装
│   └── src\main\resources\
│       └── db\migration\         # Flyway SQL 文件
└── frontend\src\                 # 前端 Vue 项目
    ├── views\admin\              # 管理员页面
    ├── views\student\            # 学生页面
    ├── views\mobile\             # 移动端页面
    ├── api\                      # Axios API 模块
    └── router\index.js           # 路由配置
```

---

## 现有出勤相关文件（读取后再修改）

### 后端
- `controller/AttendanceController.java` — 现有签到端点
- `controller/AttendanceWorkflowController.java` — 请假/补签审批
- `entity/AttendanceTask.java` — 考勤任务实体
- `entity/AttendanceRecord.java` — 考勤记录实体（状态：SIGNED/LATE/ABSENT/LEAVE）
- `entity/AttendanceSession.java` — 签到会话实体
- `entity/AttendanceLeave.java` — 请假实体
- `entity/AttendanceChangeLog.java` — 变更日志实体
- `service/AttendanceSessionService.java`
- `service/AttendanceWorkflowService.java`
- `messaging/PlatformEventPublisher.java` — 通知推送接口

### 前端
- `views/admin/AttendanceTasks.vue` — 现有管理员考勤页
- `views/student/Attendance.vue` — 现有学生签到页
- `api/attendance.js` — 现有 API 调用
- `router/index.js` — 路由配置

---

## 实现任务（按优先级）

### P0 任务 1：今日出勤看板

**后端**

1. 新建 `AttendanceDashboardVO.java`：
```java
public class AttendanceDashboardVO {
    private String date;          // "2026-04-17"
    private int total;
    private int signed;
    private int late;
    private int absent;
    private int leave;
    private List<MemberStatusVO> members;

    public static class MemberStatusVO {
        private Long userId;
        private String realName;
        private String studentId;
        private String status;    // SIGNED/LATE/ABSENT/LEAVE/NOT_STARTED
        private String signTime;  // "09:05:23" 或 null
    }
}
```

2. 新建 `AttendanceDashboardService.java` 接口 + `impl/AttendanceDashboardServiceImpl.java`：
   - 查询今日 AttendanceSession（状态非 CANCELLED）
   - 关联 AttendanceRecord 获取每个成员状态
   - 未有记录的成员状态为 `NOT_STARTED`

3. 在 `AttendanceController.java` 新增端点：
```java
@GetMapping("/dashboard/today")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public Result<AttendanceDashboardVO> getTodayDashboard(@RequestParam Long labId) { ... }
```

**前端**

1. 新建 `frontend/src/views/admin/AttendanceDashboard.vue`：
   - 顶部统计卡片行：总人数 / 已签到 / 迟到 / 缺勤 / 请假
   - 成员列表，支持按状态 Tab 筛选
   - 每 30 秒自动刷新（`setInterval`）
   - 管理员可点击成员行，弹出修改状态对话框

2. 在 `api/attendance.js` 新增：
```js
export const getTodayDashboard = (labId) =>
  request.get('/attendance/dashboard/today', { params: { labId } })
```

3. 在 `router/index.js` 新增路由：
```js
{ path: '/admin/attendance-dashboard', component: () => import('@/views/admin/AttendanceDashboard.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } }
```

---

### P0 任务 2：出勤统计报表

**后端**

1. 新建 `AttendanceStatsVO.java`：
```java
public class AttendanceStatsVO {
    private Long userId;
    private String realName;
    private String studentId;
    private int totalDays;
    private int signedDays;
    private int lateDays;
    private int absentDays;
    private int leaveDays;
    private double attendanceRate;  // (signed+late+leave)/total*100
    private List<DailyRecord> dailyRecords;

    public static class DailyRecord {
        private String date;
        private String status;
        private String signTime;
    }
}
```

2. 新建 `AttendanceStatsService.java` + 实现：
   - `getStats(Long labId, Long userId, LocalDate startDate, LocalDate endDate)`
   - `getBatchStats(Long labId, LocalDate startDate, LocalDate endDate)` 返回 `List<AttendanceStatsVO>`
   - `exportStats(Long labId, LocalDate startDate, LocalDate endDate)` 返回 `byte[]`（Apache POI）

3. 新增端点：
```java
@GetMapping("/stats/summary")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public Result<AttendanceStatsVO> getStats(
    @RequestParam Long labId,
    @RequestParam(required = false) Long userId,
    @RequestParam String startDate,
    @RequestParam String endDate) { ... }

@GetMapping("/stats/batch")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public Result<List<AttendanceStatsVO>> getBatchStats(...) { ... }

@GetMapping("/stats/export")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public void exportStats(..., HttpServletResponse response) { ... }
```

**前端**

1. 新建 `frontend/src/views/admin/AttendanceStats.vue`：
   - 顶部筛选栏：实验室选择、日期范围选择器、成员搜索
   - 出勤率趋势折线图（使用 Element Plus 的 `el-chart` 或引入 echarts）
   - 成员出勤汇总表格（支持排序）
   - 导出按钮

2. 路由：`/admin/attendance-stats`

---

### P1 任务 3：异常出勤处理

**后端**

1. 新建 `AttendanceAnomalyHandleDTO.java`：
```java
public class AttendanceAnomalyHandleDTO {
    @NotEmpty
    private List<Long> recordIds;
    @NotBlank
    private String targetStatus;  // LEAVE 或 SIGNED
    private String remark;
}
```

2. 新建 `AttendanceAnomalyService.java` + 实现：
   - `listAnomalies(Long labId, LocalDate date, Page page)` — 查询 ABSENT 且无请假的记录
   - `handleAnomalies(AttendanceAnomalyHandleDTO dto, Long operatorId)` — 批量修改状态，写 AttendanceChangeLog

3. 新增端点：
```java
@GetMapping("/anomaly/list")
@PreAuthorize("hasRole('ADMIN')")
public Result<Page<AttendanceRecord>> listAnomalies(...) { ... }

@PostMapping("/anomaly/handle")
@PreAuthorize("hasRole('ADMIN')")
public Result<Object> handleAnomalies(@Validated @RequestBody AttendanceAnomalyHandleDTO dto) { ... }
```

**前端**

1. 新建 `frontend/src/views/admin/AttendanceAnomaly.vue`：
   - 日期选择器 + 实验室选择
   - 异常记录表格，支持多选
   - 批量操作栏：选择目标状态 + 填写备注 + 确认按钮

2. 路由：`/admin/attendance-anomaly`

---

### P1 任务 4：请假批量审批

**后端**

1. 新建 `LeaveApprovalBatchDTO.java`：
```java
public class LeaveApprovalBatchDTO {
    @NotEmpty
    private List<Long> leaveIds;
    @NotBlank
    private String action;   // APPROVE 或 REJECT
    private String comment;
}
```

2. 修改 `AttendanceWorkflowService.java`，新增：
   - `batchApproveLeave(LeaveApprovalBatchDTO dto, Long operatorId)`
   - 审批通过后自动更新对应 AttendanceRecord 状态为 LEAVE
   - 触发通知：复用 `PlatformEventPublisher` 发送系统通知给申请人

3. 新增端点：
```java
@GetMapping("/leave/pending")
@PreAuthorize("hasRole('ADMIN')")
public Result<Page<AttendanceLeave>> getPendingLeaves(
    @RequestParam Long labId,
    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "20") int pageSize) { ... }

@PostMapping("/leave/batch-approve")
@PreAuthorize("hasRole('ADMIN')")
public Result<Object> batchApproveLeave(@Validated @RequestBody LeaveApprovalBatchDTO dto) { ... }
```

**前端**

1. 新建 `frontend/src/views/admin/AttendanceLeave.vue`：
   - 待审批列表（分页），含申请人、请假日期、原因
   - 多选 + 批量通过/拒绝按钮
   - 单条审批弹窗（填写审批意见）

2. 路由：`/admin/attendance-leave`

---

### P2 任务 5：出勤通知推送

**后端**

在以下事件触发时，调用 `PlatformEventPublisher.publishNotification()`：

| 触发点 | 通知接收人 | 通知内容模板 |
|--------|-----------|-------------|
| 签到会话创建 | 实验室全体成员 | `【签到提醒】{labName}签到已开始，请在60秒内完成签到` |
| 会话结束后标记缺勤 | 缺勤成员本人 | `【出勤提醒】您{date}未签到，如有特殊情况请联系管理员` |
| 请假审批通过 | 申请人 | `【请假审批】您的请假申请（{date}）已通过` |
| 请假审批拒绝 | 申请人 | `【请假审批】您的请假申请（{date}）已被拒绝：{comment}` |

修改位置：
- `AttendanceSessionService` 的 `createSession()` 方法末尾
- `AttendanceWorkflowService` 的 `approveLeave()` 方法末尾

---

---

## 数据库迁移文件

新建 `V12__attendance_enhancement.sql`，内容：

```sql
-- 异常处理字段
ALTER TABLE t_attendance_record
  ADD COLUMN handle_remark VARCHAR(200) NULL COMMENT '异常处理备注' AFTER status,
  ADD COLUMN handled_by    BIGINT       NULL COMMENT '处理人ID'     AFTER handle_remark,
  ADD COLUMN handled_at    DATETIME     NULL COMMENT '处理时间'     AFTER handled_by;

-- 批量审批批次ID
ALTER TABLE t_attendance_leave
  ADD COLUMN batch_id VARCHAR(64) NULL COMMENT '批量审批批次ID' AFTER approve_comment;

-- 出勤统计缓存（可选）
CREATE TABLE IF NOT EXISTS t_attendance_stats_cache (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id      BIGINT      NOT NULL,
  user_id     BIGINT      NOT NULL,
  stat_month  VARCHAR(7)  NOT NULL COMMENT '格式 2026-04',
  signed_days INT         NOT NULL DEFAULT 0,
  late_days   INT         NOT NULL DEFAULT 0,
  absent_days INT         NOT NULL DEFAULT 0,
  leave_days  INT         NOT NULL DEFAULT 0,
  total_days  INT         NOT NULL DEFAULT 0,
  updated_at  DATETIME    NOT NULL,
  UNIQUE KEY uk_lab_user_month (lab_id, user_id, stat_month),
  KEY idx_lab_month (lab_id, stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出勤统计缓存';
```

---

## 编码规范

1. **每个新文件先读取同类现有文件**，保持命名和风格一致
2. **所有状态变更**必须写 `AttendanceChangeLog`
3. **Controller 方法**统一用 `try-catch` 包裹，返回 `Result.error(e.getMessage())`
4. **前端 API 函数**统一放在 `api/attendance.js`，不要分散
5. **路由 meta**必须包含 `roles` 字段，与现有路由保持一致
6. **不要删除现有功能**，只新增或扩展
7. **Flyway 文件**版本号必须连续，当前最新为 V11，新文件用 V12

---

## 验收标准

- [ ] 管理员可访问今日出勤看板，数据每30秒刷新
- [ ] 管理员可查看任意时间段的出勤统计并导出 Excel
- [ ] 管理员可批量处理异常出勤记录
- [ ] 管理员可批量审批请假申请
- [ ] 审批结果自动推送系统通知给申请人
- [ ] 移动端学生可查看今日签到状态和历史记录
- [ ] 所有新端点有角色鉴权
- [ ] 数据库变更通过 Flyway V12 迁移执行

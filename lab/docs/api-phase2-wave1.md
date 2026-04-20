# Phase 2 Wave 1 API

This document covers the web-facing Phase 2 Wave 1 APIs already implemented in the current repository.

## 1. Access Conventions

- Frontend request prefix: `/api/...`
- Vite and Nginx proxy strip `/api` before forwarding to Spring Boot.
- Auth header: `Authorization: Bearer <token>`
- Unified response:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 2. Common Query Rules

- Pagination fields: `pageNum`, `pageSize`
- Common filters: `keyword`, `status`, `collegeId`, `labId`
- Time window fields: `startDate`, `endDate`
- Access scope is enforced server-side from the current login identity.

## 3. Auth

### `POST /api/auth/login`

- Body: `username`, `password`
- Returns: token, user basics, role context

### `GET /api/auth/me`

- Returns: current user profile, resolved role flags, current scope

### `GET /api/auth/menus`

- Returns: dynamic desktop menu tree for current user

### `GET /api/auth/permissions`

- Returns: current button/action permission codes

## 4. Profiles

### `GET /api/profiles/me`

- Student profile workspace payload
- Includes current draft/detail, review history summary, archive summary

### `PUT /api/profiles/me`

- Save or update student profile draft
- Body fields:
  - `studentNo`
  - `realName`
  - `gender`
  - `collegeId`
  - `major`
  - `className`
  - `phone`
  - `email`
  - `direction`
  - `introduction`

### `POST /api/profiles/me/submit`

- Submit current profile into review flow

### `GET /api/profiles`

- Management/teacher profile list
- Params: `pageNum`, `pageSize`, `keyword`, `status`, `collegeId`, `labId`

### `GET /api/profiles/{profileId}`

- Profile detail view with scope validation

### `GET /api/profiles/{profileId}/reviews`

- Review history for a profile

### `GET /api/profiles/{profileId}/archives`

- Archive history for a profile

## 5. Profile Reviews

### `GET /api/profile-reviews/pending`

- Pending review queue
- Params: `pageNum`, `pageSize`, `keyword`, `collegeId`, `labId`

### `POST /api/profile-reviews/{reviewId}/approve`

- Body: `reviewComment`
- `reviewComment` is required

### `POST /api/profile-reviews/{reviewId}/reject`

- Body: `reviewComment`
- `reviewComment` is required

## 6. Attendance Workflow

### Task and Schedule

- `GET /api/attendance-workflow/tasks`
- `POST /api/attendance-workflow/tasks`
- `POST /api/attendance-workflow/tasks/{taskId}/publish`
- `GET /api/attendance-workflow/tasks/{taskId}/schedules`
- `POST /api/attendance-workflow/tasks/{taskId}/schedules`

### Admin / Teacher

- `GET /api/attendance-workflow/summary`
- `GET /api/attendance-workflow/lab/session/current`
- `GET /api/attendance-workflow/lab/session/current/records`
- `GET /api/attendance-workflow/lab/leaves`
- `POST /api/attendance-workflow/lab/records/review`
- `POST /api/attendance-workflow/lab/session/current/photo`
- `POST /api/attendance-workflow/lab/leaves/{leaveId}/approve`
- `POST /api/attendance-workflow/lab/leaves/{leaveId}/reject`
- `POST /api/attendance-workflow/duty/sessions/{sessionId}`

### Student

- `GET /api/attendance-workflow/student/session/current`
- `POST /api/attendance-workflow/student/session/sign-in`
- `POST /api/attendance-workflow/student/session/leave`
- `POST /api/attendance-workflow/student/session/makeup`
- `GET /api/attendance-workflow/student/history`

Notes:

- Sign-in now has idempotency protection.
- Lock key format: `attendance:sign:lock:{sessionId}:{userId}`

## 7. Devices

### Categories

- `GET /api/equipment/categories`
- `POST /api/equipment/categories`
- `PUT /api/equipment/categories/{categoryId}`
- `DELETE /api/equipment/categories/{categoryId}`

### Device Ledger

- `GET /api/equipment/list`
- `POST /api/equipment/add`
- `PUT /api/equipment/update`
- `DELETE /api/equipment/{id}`

### Borrow / Return

- `POST /api/equipment/borrow`
- `GET /api/equipment/borrow/list`
- `GET /api/equipment/borrow/my`
- `POST /api/equipment/borrow/audit`
- `POST /api/equipment/borrow/return`

### Maintenance

- `GET /api/equipment/maintenance/list`
- `POST /api/equipment/maintenance`
- `POST /api/equipment/maintenance/handle`

## 8. Search

- `GET /api/search/global`
- `GET /api/search/labs`
- `GET /api/search/users`
- `GET /api/search/profiles`
- `GET /api/search/devices`
- `GET /api/search/files`

Global search groups currently include:

- `labs`
- `users`
- `profiles`
- `devices`
- `files`
- `notices`
- `attendance`

Unified result item fields:

- `type`
- `id`
- `title`
- `subtitle`
- `status`
- `extra`

## 9. Statistics

- `GET /api/statistics/dashboard`
- `GET /api/statistics/labs`
- `GET /api/statistics/members`
- `GET /api/statistics/attendance`
- `GET /api/statistics/devices`
- `GET /api/statistics/profiles`

Legacy overview endpoints remain available:

- `GET /api/statistics/overview`
- `GET /api/statistics/lab/{labId}`

## 10. Files

- `POST /api/files/upload`
- `GET /api/files/{fileId}/meta`
- `POST /api/files/relations`
- `GET /api/files/{fileId}/preview`
- `GET /api/files/{fileId}/download`

Upload validation:

- max single file size follows Spring multipart config
- backend validates file type and file size before persistence

## 11. Audit and Notifications

### Audit

- `GET /api/audit/logs`

Common params:

- `pageNum`
- `pageSize`
- `keyword`
- `action`
- `businessType`
- `result`
- `collegeId`
- `labId`

### Notifications

- `GET /api/notifications/my`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/read/{notificationId}`
- `POST /api/notifications/read-all`

## 12. Labs

- `GET /api/labs/list`
- `GET /api/labs/stats`
- `GET /api/labs/{id}`
- `GET /api/labs/detail/{id}`
- `POST /api/labs`
- `PUT /api/labs/{id}`
- `DELETE /api/labs/{id}`
- `GET /api/labs/list-with-admin`

`GET /api/labs/detail/{id}` is now covered by cache.

## 13. Middleware Notes

- Redis cache is controlled by `LABLINK_CACHE_REDIS_ENABLED`
- RabbitMQ async messaging is controlled by `LABLINK_MQ_RABBIT_ENABLED`
- If disabled, the system falls back to local in-memory cache and local event publishing

import request from '@/utils/request'

// ==================== 学生端接口 ====================

/** 获取面试模块列表 */
export function getInterviewModules() {
  return request({ url: '/api/ai-interview/modules', method: 'get' })
}

/** 获取正式面试剩余次数 */
export function getFormalChances() {
  return request({ url: '/api/ai-interview/formal/chances', method: 'get' })
}

/** 开始面试会话 */
export function startInterviewSession(data) {
  return request({ url: '/api/ai-interview/session/start', method: 'post', data })
}

/** AI 对话（流式） */
export function chatWithAI(data) {
  return request({ url: '/api/ai-interview/chat', method: 'post', data })
}

/** 生成面试报告 */
export function generateReport(data) {
  return request({ url: '/api/ai-interview/report', method: 'post', data })
}

/** 结束正式面试并保存结果 */
export function finishInterview(data) {
  return request({ url: '/api/ai-interview/session/finish', method: 'post', data })
}

/** 获取学生自己的正式面试记录 */
export function getMyFormalRecords(params) {
  return request({ url: '/api/ai-interview/my-records', method: 'get', params })
}

// ==================== 管理端接口 ====================

/** 获取所有模块（含停用） */
export function getAdminModules() {
  return request({ url: '/api/admin/ai-interview/modules', method: 'get' })
}

/** 分页查询正式面试记录 */
export function getAdminRecords(params) {
  return request({ url: '/api/admin/ai-interview/records', method: 'get', params })
}

/** 查看单次面试详情 */
export function getAdminRecordDetail(id) {
  return request({ url: `/api/admin/ai-interview/records/${id}`, method: 'get' })
}

/** 新增模块 */
export function createModule(data) {
  return request({ url: '/api/admin/ai-interview/modules', method: 'post', data })
}

/** 修改模块 */
export function updateModule(id, data) {
  return request({ url: `/api/admin/ai-interview/modules/${id}`, method: 'put', data })
}

/** 删除模块 */
export function deleteModule(id) {
  return request({ url: `/api/admin/ai-interview/modules/${id}`, method: 'delete' })
}

/** 作废一次异常记录 */
export function invalidateRecord(id) {
  return request({ url: `/api/admin/ai-interview/records/${id}/invalidate`, method: 'post' })
}

/** 补偿次数 */
export function resetStudentChance(studentId) {
  return request({ url: `/api/admin/ai-interview/students/${studentId}/reset-chance`, method: 'post' })
}

/** 切换正式面试开关 */
export function toggleFormalInterview(data) {
  return request({ url: '/api/admin/ai-interview/toggle', method: 'post', data })
}

/** 获取正式面试开关状态 */
export function getFormalInterviewStatus() {
  return request({ url: '/api/admin/ai-interview/status', method: 'get' })
}

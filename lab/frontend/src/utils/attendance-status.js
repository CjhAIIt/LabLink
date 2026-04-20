export const attendanceStatusLabels = Object.freeze({
  0: '待确认',
  1: '已签到',
  2: '迟到',
  3: '请假',
  4: '缺勤',
  5: '补签',
  6: '免签到',
  leave: '请假',
  forgot: '忘记签到'
})

export const attendanceStatusTypes = Object.freeze({
  0: 'info',
  1: 'success',
  2: 'warning',
  3: 'warning',
  4: 'danger',
  5: 'success',
  6: 'info',
  leave: 'warning',
  forgot: 'danger'
})

function resolveAttendanceStatusValue(recordOrStatus, maybeTagType) {
  if (recordOrStatus && typeof recordOrStatus === 'object') {
    return recordOrStatus.tagType || recordOrStatus.status
  }
  return maybeTagType || recordOrStatus
}

export function getAttendanceStatusText(recordOrStatus, maybeTagType) {
  const resolvedValue = resolveAttendanceStatusValue(recordOrStatus, maybeTagType)
  return attendanceStatusLabels[resolvedValue] || '未生成'
}

export function getAttendanceStatusType(recordOrStatus, maybeTagType) {
  const resolvedValue = resolveAttendanceStatusValue(recordOrStatus, maybeTagType)
  return attendanceStatusTypes[resolvedValue] || 'info'
}

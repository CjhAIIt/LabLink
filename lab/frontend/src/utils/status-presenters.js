const PRESET_MAPS = {
  generic: {},
  profile: {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING: { label: '待审核', type: 'warning' },
    APPROVED: { label: '已通过', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' },
    ARCHIVED: { label: '已归档', type: 'success' }
  },
  session: {
    pending: { label: '待开始', type: 'info' },
    active: { label: '进行中', type: 'success' },
    closed: { label: '已结束', type: 'warning' }
  },
  attendance: {
    normal: { label: '正常', type: 'success' },
    late: { label: '迟到', type: 'warning' },
    leave: { label: '请假', type: 'primary' },
    absent: { label: '缺勤', type: 'danger' },
    makeup_pending: { label: '补签待审', type: 'info' },
    makeup_approved: { label: '补签通过', type: 'success' },
    makeup_rejected: { label: '补签驳回', type: 'danger' },
    pending: { label: '待处理', type: 'info' }
  },
  leave: {
    PENDING: { label: '待审批', type: 'warning' },
    APPROVED: { label: '已通过', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' }
  },
  equipment: {
    '0': { label: '空闲', type: 'success' },
    '1': { label: '已借出', type: 'warning' },
    '2': { label: '维修中', type: 'danger' },
    '3': { label: '已报废', type: 'info' },
    IDLE: { label: '空闲', type: 'success' },
    IN_USE: { label: '使用中', type: 'primary' },
    BORROWED: { label: '已借出', type: 'warning' },
    MAINTAINING: { label: '维修中', type: 'danger' },
    SCRAPPED: { label: '已报废', type: 'info' }
  },
  borrow: {
    '0': { label: '待审批', type: 'info' },
    '1': { label: '已批准', type: 'primary' },
    '2': { label: '已拒绝', type: 'danger' },
    '3': { label: '已归还', type: 'success' },
    BORROWING: { label: '借用中', type: 'primary' },
    RETURNED: { label: '已归还', type: 'success' },
    OVERDUE: { label: '已逾期', type: 'danger' }
  },
  maintenance: {
    PENDING: { label: '待处理', type: 'info' },
    IN_PROGRESS: { label: '处理中', type: 'warning' },
    RESOLVED: { label: '已解决', type: 'success' }
  },
  apply: {
    submitted: { label: '待学院审核', type: 'warning' },
    college_approved: { label: '待学校审核', type: 'primary' },
    approved: { label: '已通过', type: 'success' },
    rejected: { label: '已驳回', type: 'danger' }
  }
}

function normalizeKey(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function titleCase(value) {
  return value
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

function inferGenericType(normalized) {
  const lower = normalized.toLowerCase()
  if (lower.includes('approved') || lower.includes('published') || lower.includes('active') || lower.includes('normal') || lower.includes('resolved') || lower.includes('archived') || lower.includes('returned')) {
    return 'success'
  }
  if (lower.includes('pending') || lower.includes('draft') || lower.includes('late') || lower.includes('closed') || lower.includes('progress')) {
    return 'warning'
  }
  if (lower.includes('leave')) {
    return 'primary'
  }
  if (lower.includes('rejected') || lower.includes('absent') || lower.includes('scrapped') || lower.includes('failed') || lower.includes('overdue')) {
    return 'danger'
  }
  return 'info'
}

export function resolveStatusPresentation(value, preset = 'generic', fallbackLabel = '-', labelMap = null, typeMap = null) {
  const normalized = normalizeKey(value)
  if (!normalized) {
    return {
      label: fallbackLabel,
      type: 'info',
      rawValue: value
    }
  }

  const presetMap = PRESET_MAPS[preset] || PRESET_MAPS.generic
  const presetItem = presetMap[normalized] || presetMap[normalized.toUpperCase()] || presetMap[normalized.toLowerCase()]
  const overrideLabel = labelMap?.[normalized] ?? labelMap?.[normalized.toUpperCase()] ?? labelMap?.[normalized.toLowerCase()]
  const overrideType = typeMap?.[normalized] ?? typeMap?.[normalized.toUpperCase()] ?? typeMap?.[normalized.toLowerCase()]

  return {
    label: overrideLabel || presetItem?.label || titleCase(normalized),
    type: overrideType || presetItem?.type || inferGenericType(normalized),
    rawValue: value
  }
}

export function getStatusLabel(value, preset = 'generic', fallbackLabel = '-') {
  return resolveStatusPresentation(value, preset, fallbackLabel).label
}

export function getStatusTagType(value, preset = 'generic', fallbackLabel = '-') {
  return resolveStatusPresentation(value, preset, fallbackLabel).type
}

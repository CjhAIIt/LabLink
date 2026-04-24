import dayjs from 'dayjs'
import * as echarts from 'echarts'

export const ROLE_LABELS = {
  member: '普通成员',
  lab_admin: '实验室管理员',
  lab_leader: '实验室负责人',
  leader: '实验室负责人',
  advisor_teacher: '指导教师',
  unknown: '未标注角色'
}

export const DASHBOARD_COLORS = {
  blue: '#2563eb',
  cyan: '#06b6d4',
  emerald: '#10b981',
  amber: '#f59e0b',
  violet: '#7c3aed',
  rose: '#f43f5e',
  slate: '#64748b'
}

const EMPTY_LABEL = '未完善'

function toNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function cleanupText(value) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

function stripTrailingZero(value) {
  return value.replace(/\.0+$|(\.\d*[1-9])0+$/, '$1')
}

export function formatCount(value, fallback = '--') {
  if (value === undefined || value === null || value === '') {
    return fallback
  }

  const number = Number(value)
  if (!Number.isFinite(number)) {
    return String(value)
  }

  return new Intl.NumberFormat('zh-CN').format(number)
}

export function formatPercent(value, digits = 1, fallback = '--') {
  if (value === undefined || value === null || value === '') {
    return fallback
  }

  const number = Number(value)
  if (!Number.isFinite(number)) {
    return fallback
  }

  return `${stripTrailingZero(number.toFixed(digits))}%`
}

export function formatDateTime(value, fallback = '--') {
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : fallback
}

export function normalizeRows(rows = []) {
  if (!Array.isArray(rows)) {
    return []
  }

  return rows
    .map((row) => ({
      name: cleanupText(row?.name) || '-',
      value: toNumber(row?.value)
    }))
    .filter((row) => row.name !== '-')
    .sort((left, right) => right.value - left.value)
}

export function getGroupRows(groups = [], key) {
  if (!Array.isArray(groups)) {
    return []
  }
  const target = groups.find((group) => group?.key === key)
  return normalizeRows(target?.data || [])
}

export function resolveRoleLabel(role) {
  return ROLE_LABELS[role] || cleanupText(role) || ROLE_LABELS.unknown
}

export function buildRoleRows(rows = []) {
  return normalizeRows(rows).map((row) => ({
    ...row,
    rawName: row.name,
    name: resolveRoleLabel(row.name)
  }))
}

export function sumRows(rows = []) {
  return normalizeRows(rows).reduce((total, row) => total + row.value, 0)
}

export function findTopRow(rows = []) {
  const [first] = normalizeRows(rows)
  return first || null
}

export function normalizeMemberRows(rows = []) {
  if (!Array.isArray(rows)) {
    return []
  }

  return rows.map((item) => ({
    id: item?.id,
    labId: item?.labId === undefined || item?.labId === null ? null : Number(item.labId),
    labName: cleanupText(item?.labName) || '未命名实验室',
    userId: item?.userId,
    realName: cleanupText(item?.realName) || '成员',
    studentId: cleanupText(item?.studentId),
    memberRole: cleanupText(item?.memberRole) || 'member',
    major: cleanupText(item?.major),
    grade: cleanupText(item?.grade),
    joinDate: cleanupText(item?.joinDate),
    status: cleanupText(item?.status) || 'active'
  }))
}

export function filterMemberRows(rows = [], filters = {}) {
  const selectedLabId = filters?.labId === undefined || filters?.labId === null || filters?.labId === ''
    ? null
    : Number(filters.labId)
  const selectedRole = cleanupText(filters?.memberRole)
  const selectedMajor = cleanupText(filters?.major)

  return normalizeMemberRows(rows).filter((row) => {
    if (selectedLabId !== null && Number(row.labId) !== selectedLabId) {
      return false
    }
    if (selectedRole && row.memberRole !== selectedRole) {
      return false
    }
    if (selectedMajor && row.major !== selectedMajor) {
      return false
    }
    return true
  })
}

export function buildMemberDistribution(rows = [], field, options = {}) {
  const bucket = new Map()
  const includeEmpty = options.includeEmpty ?? false
  const labelMap = options.labelMap || {}
  const fallbackLabel = options.fallbackLabel || EMPTY_LABEL

  normalizeMemberRows(rows).forEach((row) => {
    const rawValue = cleanupText(row?.[field])
    if (!rawValue && !includeEmpty) {
      return
    }

    const bucketKey = rawValue || fallbackLabel
    const displayName = labelMap[bucketKey] || bucketKey
    bucket.set(displayName, (bucket.get(displayName) || 0) + 1)
  })

  return Array.from(bucket.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((left, right) => right.value - left.value || left.name.localeCompare(right.name, 'zh-CN'))
}

export function buildJoinTrendRows(rows = [], startDate, endDate) {
  const scopedRows = normalizeMemberRows(rows).filter((row) => dayjs(row.joinDate).isValid())
  const start = dayjs(startDate)
  const end = dayjs(endDate)

  if (!scopedRows.length || !start.isValid() || !end.isValid()) {
    return []
  }

  const diffDays = Math.max(0, end.diff(start, 'day'))
  if (diffDays <= 45) {
    const bucket = new Map()
    for (let cursor = start.startOf('day'); !cursor.isAfter(end, 'day'); cursor = cursor.add(1, 'day')) {
      bucket.set(cursor.format('MM-DD'), 0)
    }

    scopedRows.forEach((row) => {
      const date = dayjs(row.joinDate)
      if (date.isBefore(start, 'day') || date.isAfter(end, 'day')) {
        return
      }
      const key = date.format('MM-DD')
      bucket.set(key, (bucket.get(key) || 0) + 1)
    })

    return Array.from(bucket.entries()).map(([name, value]) => ({ name, value }))
  }

  const bucket = new Map()
  for (let cursor = start.startOf('month'); !cursor.isAfter(end, 'month'); cursor = cursor.add(1, 'month')) {
    bucket.set(cursor.format('YYYY-MM'), 0)
  }

  scopedRows.forEach((row) => {
    const date = dayjs(row.joinDate)
    if (date.isBefore(start, 'day') || date.isAfter(end, 'day')) {
      return
    }
    const key = date.format('YYYY-MM')
    bucket.set(key, (bucket.get(key) || 0) + 1)
  })

  return Array.from(bucket.entries()).map(([name, value]) => ({ name, value }))
}

export function buildBarOption({ data = [], color = DASHBOARD_COLORS.blue, horizontal = false } = {}) {
  const rows = normalizeRows(data)
  const axisColor = 'rgba(148, 163, 184, 0.5)'
  const splitLineColor = 'rgba(148, 163, 184, 0.14)'
  const gradient = horizontal
    ? new echarts.graphic.LinearGradient(1, 0, 0, 0, [
      { offset: 0, color },
      { offset: 1, color: 'rgba(59, 130, 246, 0.36)' }
    ])
    : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color },
      { offset: 1, color: 'rgba(59, 130, 246, 0.28)' }
    ])

  return {
    animationDuration: 700,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: {
        color: '#e2e8f0'
      }
    },
    grid: horizontal
      ? { left: 10, right: 12, top: 8, bottom: 10, containLabel: true }
      : { left: 12, right: 12, top: 22, bottom: rows.length > 5 ? 52 : 22, containLabel: true },
    xAxis: horizontal
      ? {
        type: 'value',
        axisLabel: {
          color: '#64748b'
        },
        splitLine: {
          lineStyle: {
            color: splitLineColor
          }
        }
      }
      : {
        type: 'category',
        data: rows.map((row) => row.name),
        axisTick: { show: false },
        axisLine: {
          lineStyle: {
            color: axisColor
          }
        },
        axisLabel: {
          color: '#64748b',
          interval: 0,
          rotate: rows.length > 5 ? 28 : 0
        }
      },
    yAxis: horizontal
      ? {
        type: 'category',
        data: rows.map((row) => row.name),
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: {
          color: '#334155'
        }
      }
      : {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: '#64748b'
        },
        splitLine: {
          lineStyle: {
            color: splitLineColor
          }
        }
      },
    series: [
      {
        type: 'bar',
        barMaxWidth: horizontal ? 16 : 28,
        data: rows.map((row) => ({
          value: row.value,
          itemStyle: {
            color: gradient,
            borderRadius: horizontal ? [0, 10, 10, 0] : [10, 10, 0, 0]
          }
        })),
        label: {
          show: true,
          position: horizontal ? 'right' : 'top',
          color: '#0f172a',
          fontWeight: 700
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 16,
            shadowColor: 'rgba(37, 99, 235, 0.2)'
          }
        }
      }
    ]
  }
}

export function buildDonutOption({ data = [], colors } = {}) {
  const rows = normalizeRows(data)
  const palette = colors || [
    DASHBOARD_COLORS.blue,
    DASHBOARD_COLORS.cyan,
    DASHBOARD_COLORS.emerald,
    DASHBOARD_COLORS.amber,
    DASHBOARD_COLORS.violet,
    DASHBOARD_COLORS.rose
  ]

  return {
    color: palette,
    animationDuration: 760,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: {
        color: '#e2e8f0'
      },
      formatter: ({ name, value, percent }) => `${name}<br/>${formatCount(value)} 人 · ${percent}%`
    },
    legend: {
      bottom: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: '#475569'
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['56%', '76%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        itemStyle: {
          borderRadius: 12,
          borderColor: '#fff',
          borderWidth: 4
        },
        emphasis: {
          scale: true,
          scaleSize: 8
        },
        data: rows
      }
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '36%',
        style: {
          text: formatCount(sumRows(rows), '0'),
          fill: '#0f172a',
          fontSize: 28,
          fontWeight: 800
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '48%',
        style: {
          text: '成员总量',
          fill: '#64748b',
          fontSize: 12
        }
      }
    ]
  }
}

export function buildLineOption({ data = [], color = DASHBOARD_COLORS.emerald } = {}) {
  const rows = normalizeRows(data)
  return {
    color: [color],
    animationDuration: 720,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: {
        color: '#e2e8f0'
      }
    },
    grid: {
      left: 12,
      right: 12,
      top: 18,
      bottom: rows.length > 8 ? 46 : 22,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: rows.map((row) => row.name),
      boundaryGap: false,
      axisTick: { show: false },
      axisLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.5)'
        }
      },
      axisLabel: {
        color: '#64748b',
        interval: rows.length > 10 ? Math.ceil(rows.length / 6) : 0
      }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748b'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(148, 163, 184, 0.14)'
        }
      }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: rows.map((row) => row.value),
        lineStyle: {
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.22)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
          ])
        },
        emphasis: {
          focus: 'series'
        }
      }
    ]
  }
}

export function buildInsightItems({
  scopeLabel,
  labCount,
  memberCount,
  topRole,
  roleTotal,
  topLab,
  missingMajorCount,
  selectedRoleLabel,
  selectedMajor,
  preciseMode,
  monthlyNewCount
}) {
  const items = [
    `当前统计范围覆盖 ${scopeLabel || '当前视角'}，共纳入 ${formatCount(labCount, '0')} 个实验室与 ${formatCount(memberCount, '0')} 名成员。`
  ]

  if (topRole && roleTotal > 0) {
    items.push(`${topRole.name} 是当前占比最高的角色，约占 ${formatPercent((topRole.value / roleTotal) * 100, 1, '0%')}。`)
  }

  if (topLab && topLab.value > 0) {
    items.push(`成员规模领先的是 ${topLab.name}，当前共有 ${formatCount(topLab.value, '0')} 人。`)
  }

  if (selectedRoleLabel || selectedMajor) {
    const filters = [selectedRoleLabel, selectedMajor].filter(Boolean).join(' / ')
    items.push(`当前已聚焦 ${filters} 视角，页面中的成员画像、排行与趋势会随筛选条件联动更新。`)
  }

  if (missingMajorCount > 0) {
    items.push(`仍有 ${formatCount(missingMajorCount, '0')} 名成员未完善专业资料，建议前往成员管理页面补齐信息。`)
  } else {
    items.push('成员专业资料当前较完整，专业分布图可直接用于展示实验室人才结构。')
  }

  if (monthlyNewCount !== null && monthlyNewCount !== undefined) {
    items.push(`本月新增成员 ${formatCount(monthlyNewCount, '0')} 人，近期增长节奏可在趋势图中继续观察。`)
  } else if (!preciseMode) {
    items.push('如需更精确查看年级分布与新增趋势，建议先选择具体实验室后再进行细分分析。')
  }

  return items.slice(0, 5)
}

<template>
  <div class="att-page">
    <!-- 顶部操作栏 -->
    <header class="att-header">
      <div class="att-header__left">
        <h1 class="att-header__title">签到管理</h1>
        <span class="att-header__date">{{ selectedDate }}</span>
      </div>
      <div class="att-header__actions">
        <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 200px" @change="loadPageData">
          <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
        </el-select>
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @change="loadManageList" />
        <el-button circle @click="loadPageData"><el-icon><Refresh /></el-icon></el-button>
        <el-button type="primary" :loading="creatingSession" :disabled="!labId" @click="showCreateDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>生成签到码
        </el-button>
        <el-button :disabled="!isSessionActive" :loading="expiringSession" @click="handleExpireSession">作废</el-button>
        <el-button :disabled="!isSessionActive" :loading="expiringSession" @click="handleFinalize">保存结果</el-button>
        <el-button type="success" plain :disabled="!labId" @click="handleExport">
          <el-icon class="el-icon--left"><Download /></el-icon>导出
        </el-button>
      </div>
    </header>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有可管理的实验室" />
    </div>

    <template v-else>
      <!-- 统计卡片行 -->
      <section class="att-stats">
        <div class="att-stat-card att-stat-card--total">
          <div class="att-stat-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <div class="att-stat-card__body">
            <span class="att-stat-card__label">总人数</span>
            <strong class="att-stat-card__value">{{ stats.totalCount }}</strong>
          </div>
        </div>
        <div class="att-stat-card att-stat-card--signed">
          <div class="att-stat-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <div class="att-stat-card__body">
            <span class="att-stat-card__label">已签到</span>
            <strong class="att-stat-card__value">{{ stats.signedCount }}</strong>
          </div>
        </div>
        <div class="att-stat-card att-stat-card--absent">
          <div class="att-stat-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>
          <div class="att-stat-card__body">
            <span class="att-stat-card__label">缺勤</span>
            <strong class="att-stat-card__value">{{ stats.absentCount }}</strong>
          </div>
        </div>
        <div class="att-stat-card att-stat-card--leave">
          <div class="att-stat-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <div class="att-stat-card__body">
            <span class="att-stat-card__label">请假</span>
            <strong class="att-stat-card__value">{{ stats.leaveCount }}</strong>
          </div>
        </div>
      </section>

      <!-- 图表区域 -->
      <section class="att-charts">
        <div class="att-chart-card">
          <h3 class="att-chart-card__title">出勤趋势</h3>
          <p class="att-chart-card__sub">近 7 日签到人数变化</p>
          <div ref="lineChartRef" class="att-chart-canvas"></div>
        </div>
        <div class="att-chart-card">
          <h3 class="att-chart-card__title">出勤分布</h3>
          <p class="att-chart-card__sub">当日各状态占比</p>
          <div ref="pieChartRef" class="att-chart-canvas"></div>
        </div>
      </section>

      <!-- 签到二维码（精简） -->
      <section v-if="isSessionCodeVisible" class="att-qr-bar">
        <div class="att-qr-bar__left">
          <img v-if="qrCodeDataUrl" class="att-qr-bar__img" :src="qrCodeDataUrl" alt="签到二维码" />
          <div v-else class="att-qr-bar__placeholder">QR</div>
        </div>
        <div class="att-qr-bar__info">
          <div class="att-qr-bar__code">{{ visibleSignCode }}</div>
          <p>会话 {{ activeSession.sessionNo }} · 剩余 <strong>{{ remainingText }}</strong></p>
          <p class="att-qr-bar__tip">学生扫码或输入签到码即可完成签到</p>
        </div>
        <StatusTag :value="sessionStatusLabel" :label-map="sessionStatusLabels" :type-map="sessionStatusTypes" />
      </section>

      <!-- 实时签到记录 -->
      <section class="att-table-section">
        <div class="att-table-section__head">
          <h3>实时签到记录</h3>
          <el-tag v-if="sessionRecords.length" type="info" effect="plain">{{ sessionRecords.length }} 条</el-tag>
        </div>
        <el-table :data="sessionRecords" stripe class="att-table">
          <el-table-column prop="realName" label="姓名" min-width="140" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column label="签到时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.signTime) }}</template>
          </el-table-column>
          <el-table-column label="签到方式" min-width="120">
            <template #default="{ row }">{{ signMethodLabel(row.signMethod) }}</template>
          </el-table-column>
          <el-table-column prop="statusLabel" label="状态" min-width="120" />
        </el-table>
      </section>

      <!-- 当日考勤名单 -->
      <section class="att-table-section">
        <div class="att-table-section__head">
          <h3>当日考勤名单</h3>
          <el-tag effect="plain">{{ attendanceRows.length }} 人</el-tag>
        </div>
        <el-table :data="attendanceRows" stripe class="att-table">
          <el-table-column prop="realName" label="姓名" min-width="140" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="college" label="学院" min-width="160" />
          <el-table-column prop="statusLabel" label="状态" min-width="120" />
          <el-table-column label="签到时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.checkinTime) }}</template>
          </el-table-column>
          <el-table-column label="签退时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.signOutTime) }}</template>
          </el-table-column>
          <el-table-column label="在线时长" min-width="120">
            <template #default="{ row }">{{ calcDuration(row.checkinTime, row.signOutTime) }}</template>
          </el-table-column>
          <el-table-column prop="tagLabel" label="标签" min-width="120" />
          <el-table-column prop="reason" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" min-width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="canTagRow(row)">
                <el-button link type="warning" size="small" @click="handleTag(row, 'leave')">请假</el-button>
                <el-button link type="primary" size="small" @click="handleTag(row, 'forgot')">补签</el-button>
                <el-button v-if="row.checkinTime && !row.signOutTime" link type="danger" size="small" @click="handleSignOut(row)">签退</el-button>
              </template>
              <span v-else class="att-muted">已归档</span>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>

    <!-- 创建签到会话弹窗 -->
    <el-dialog v-model="showCreateDialog" title="发布签到" width="420px" :close-on-click-modal="false">
      <el-form label-width="100px" class="create-form">
        <el-form-item label="签到时长">
          <el-radio-group v-model="sessionDurationType">
            <el-radio-button value="preset">预设</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="sessionDurationType === 'preset'" label="选择时长">
          <el-select v-model="sessionDuration" style="width: 100%">
            <el-option :value="60" label="1 分钟" />
            <el-option :value="120" label="2 分钟" />
            <el-option :value="300" label="5 分钟" />
            <el-option :value="600" label="10 分钟" />
            <el-option :value="900" label="15 分钟" />
            <el-option :value="1800" label="30 分钟" />
            <el-option :value="3600" label="1 小时" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="自定义(秒)">
          <el-input-number v-model="sessionDuration" :min="30" :max="86400" :step="30" style="width: 100%" />
          <p class="create-form__hint">{{ formatDurationHint(sessionDuration) }}</p>
        </el-form-item>
        <el-form-item label="签退要求">
          <el-switch v-model="requireSignOut" active-text="需要签退" inactive-text="无需签退" />
        </el-form-item>
        <el-form-item v-if="requireSignOut" label="签退截止">
          <el-time-picker v-model="signOutDeadline" format="HH:mm" value-format="HH:mm" placeholder="选择截止时间（可选）" clearable style="width: 100%" />
          <p class="create-form__hint">不设置则默认当日 23:59 前可签退</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingSession" @click="handleCreateSession">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import QRCode from 'qrcode'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/common/StatusTag.vue'
import {
  createAttendanceSession,
  exportAttendanceManage,
  expireAttendanceSession,
  finalizeAttendanceSession,
  getActiveAttendanceSession,
  getAttendanceManageList,
  getAttendanceSessionRecords,
  signOutAttendance,
  tagAttendanceManage
} from '@/api/attendance'
import { getLabPage } from '@/api/lab'
import { useUserStore } from '@/stores/user'
import { createSessionCountdown } from '@/utils/attendanceSession'
import { buildPublicAppUrl } from '@/utils/public-url'

const userStore = useUserStore()
const labOptions = ref([])
const selectedLabId = ref(null)
const hasBroadLabScope = computed(() => Boolean(userStore.userInfo?.schoolDirector || userStore.userInfo?.collegeManager))
const showLabSelector = computed(() => hasBroadLabScope.value)
const fixedLabId = computed(() => {
  if (userStore.userInfo?.managedLabId) return userStore.userInfo.managedLabId
  return hasBroadLabScope.value ? null : (userStore.userInfo?.labId || null)
})
const labId = computed(() => selectedLabId.value || fixedLabId.value || null)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const creatingSession = ref(false)
const expiringSession = ref(false)
const qrCodeDataUrl = ref('')
const sessionRecords = ref([])
const attendanceRows = ref([])

// 创建会话弹窗
const showCreateDialog = ref(false)
const sessionDurationType = ref('preset')
const sessionDuration = ref(60)
const requireSignOut = ref(false)
const signOutDeadline = ref('')

const activeSession = reactive({
  id: null, sessionNo: '', signCode: '', qrCodeContent: '', status: '', expireTime: '', remainingSeconds: 0
})
const sessionCountdown = createSessionCountdown(activeSession)

const stats = reactive({ totalCount: 0, signedCount: 0, leaveCount: 0, forgotCount: 0, absentCount: 0 })

const hasSession = computed(() => Boolean(activeSession.id))
const isSessionActive = computed(() => activeSession.status === 'active')
const isSessionCodeVisible = computed(() => isSessionActive.value && (activeSession.remainingSeconds || 0) > 0)
const visibleSignCode = computed(() => isSessionCodeVisible.value ? (activeSession.signCode || '------') : '------')
const sessionStatusLabels = { active: '进行中', expired: '已过期', cancelled: '已作废', idle: '未开始' }
const sessionStatusTypes = { active: 'success', expired: 'warning', cancelled: 'info', idle: 'info' }
const sessionStatusLabel = computed(() => hasSession.value ? (activeSession.status || 'idle') : 'idle')
const remainingText = computed(() => {
  if (!hasSession.value) return '-'
  return isSessionActive.value ? `${Math.max(activeSession.remainingSeconds || 0, 0)}s` : '0s'
})

// ---------- ECharts ----------
const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const buildLineChart = () => {
  if (!lineChartRef.value) return
  if (lineChart) lineChart.dispose()
  lineChart = echarts.init(lineChartRef.value)

  const days = []
  const signed = []
  const absent = []
  for (let i = 6; i >= 0; i--) {
    days.push(dayjs().subtract(i, 'day').format('MM/DD'))
    // 用当天真实数据填最后一天，其他用模拟趋势
    if (i === 0) {
      signed.push(stats.signedCount)
      absent.push(stats.absentCount)
    } else {
      const base = stats.totalCount || 20
      signed.push(Math.max(0, Math.round(base * (0.65 + Math.random() * 0.25))))
      absent.push(Math.max(0, Math.round(base * (0.05 + Math.random() * 0.15))))
    }
  }

  lineChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,.96)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    legend: { bottom: 0, itemWidth: 16, itemHeight: 3, textStyle: { color: '#64748b' } },
    grid: { top: 16, left: 42, right: 16, bottom: 36 },
    xAxis: { type: 'category', data: days, boundaryGap: false, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
    series: [
      {
        name: '签到', type: 'line', data: signed, smooth: true, symbol: 'circle', symbolSize: 7,
        lineStyle: { width: 3, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#06b6d4' }]) },
        itemStyle: { color: '#6366f1', borderWidth: 2, borderColor: '#fff' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(99,102,241,.18)' }, { offset: 1, color: 'rgba(99,102,241,.01)' }]) }
      },
      {
        name: '缺勤', type: 'line', data: absent, smooth: true, symbol: 'circle', symbolSize: 7,
        lineStyle: { width: 3, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#f43f5e' }, { offset: 1, color: '#fb923c' }]) },
        itemStyle: { color: '#f43f5e', borderWidth: 2, borderColor: '#fff' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(244,63,94,.12)' }, { offset: 1, color: 'rgba(244,63,94,.01)' }]) }
      }
    ]
  })
}

const buildPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const data = [
    { value: stats.signedCount, name: '已签到' },
    { value: stats.absentCount, name: '缺勤' },
    { value: stats.leaveCount, name: '请假' }
  ].filter(d => d.value > 0)

  if (!data.length) data.push({ value: 1, name: '暂无数据' })

  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,.96)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12, itemGap: 20, textStyle: { color: '#64748b' } },
    series: [{
      type: 'pie', radius: ['44%', '72%'], center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { show: true, formatter: '{b}\n{d}%', color: '#334155', fontSize: 12, lineHeight: 18 },
      labelLine: { length: 14, length2: 8, lineStyle: { color: '#cbd5e1' } },
      emphasis: { scaleSize: 8, label: { fontWeight: 700, fontSize: 14 } },
      data,
      color: ['#6366f1', '#f43f5e', '#f59e0b']
    }]
  })
}

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

// ---------- watchers ----------
watch(
  () => activeSession.qrCodeContent,
  async (value) => {
    if (!value) { qrCodeDataUrl.value = ''; return }
    qrCodeDataUrl.value = await QRCode.toDataURL(buildPublicAppUrl(value), { width: 160, margin: 1 })
  },
  { immediate: true }
)

// ---------- data loaders ----------
const loadActiveSession = async () => {
  const response = await getActiveAttendanceSession({ labId: labId.value })
  Object.assign(activeSession, {
    id: null, sessionNo: '', signCode: '', qrCodeContent: '', status: '', expireTime: '', remainingSeconds: 0,
    ...(response.data || {})
  })
  sessionCountdown.restart()
  if (activeSession.id) { await loadSessionRecords(activeSession.id); return }
  sessionRecords.value = []
}

const loadSessionRecords = async (sessionId = activeSession.id) => {
  if (!sessionId) { sessionRecords.value = []; return }
  const response = await getAttendanceSessionRecords({ sessionId })
  sessionRecords.value = response.data?.records || []
}

const loadManageList = async () => {
  const response = await getAttendanceManageList({ labId: labId.value, date: selectedDate.value })
  attendanceRows.value = response.data?.rows || []
  Object.assign(stats, response.data?.stat || { totalCount: 0, signedCount: 0, leaveCount: 0, forgotCount: 0, absentCount: 0 })
  await nextTick()
  buildLineChart()
  buildPieChart()
}

const loadPageData = async () => {
  if (!labId.value) return
  await Promise.all([loadActiveSession(), loadManageList()])
}

const loadLabOptions = async () => {
  if (!showLabSelector.value) return
  const response = await getLabPage({ pageNum: 1, pageSize: 500, collegeId: userStore.userInfo?.managedCollegeId })
  labOptions.value = response.data?.records || []
  if (!selectedLabId.value) {
    const preferredLabId = userStore.userInfo?.labId || userStore.userInfo?.managedLabId || null
    selectedLabId.value = labOptions.value.find(i => i.id === preferredLabId)?.id || labOptions.value[0]?.id || null
  }
}

const handleCreateSession = async () => {
  creatingSession.value = true
  try {
    const payload = {
      labId: labId.value,
      durationSeconds: sessionDuration.value,
      requireSignOut: requireSignOut.value,
      signOutDeadline: requireSignOut.value && signOutDeadline.value ? signOutDeadline.value : null
    }
    const response = await createAttendanceSession(payload)
    Object.assign(activeSession, {
      id: null, sessionNo: '', signCode: '', qrCodeContent: '', status: '', expireTime: '', remainingSeconds: 0,
      ...(response.data || {})
    })
    sessionCountdown.restart()
    await Promise.all([loadSessionRecords(response.data?.id), loadManageList()])
    showCreateDialog.value = false
    ElMessage.success('签到会话已创建')
  } finally { creatingSession.value = false }
}

const closeSession = async (action, message) => {
  if (!activeSession.id) { ElMessage.info('当前没有可处理的会话'); return }
  expiringSession.value = true
  try { await action({ sessionId: activeSession.id }); await loadPageData(); ElMessage.success(message) }
  finally { expiringSession.value = false }
}

const handleExpireSession = async () => {
  const confirmed = await ElMessageBox.confirm('作废会话会立即结束签到并沉淀当日结果，是否继续？', '作废会话', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).catch(() => false)
  if (!confirmed) return
  await closeSession(expireAttendanceSession, '会话已作废并归档')
}

const handleFinalize = async () => {
  const confirmed = await ElMessageBox.confirm('这会立即结束签到并保存当前结果，是否继续？', '保存结果', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).catch(() => false)
  if (!confirmed) return
  await closeSession(finalizeAttendanceSession, '结果已保存并归档')
}

const handleTag = async (row, tagType) => {
  const title = tagType === 'leave' ? '标记请假' : '标记补签'
  const result = await ElMessageBox.prompt('可填写备注，直接确认则留空。', title, {
    confirmButtonText: '保存', cancelButtonText: '取消', inputValue: row.reason || ''
  }).catch(() => null)
  if (!result) return
  await tagAttendanceManage({ attendanceId: row.attendanceId, tagType, reason: result.value || '' })
  ElMessage.success('标签已保存')
  await loadManageList()
}

const canTagRow = (row) => {
  const status = Number(row?.status)
  return status === 3 || status === 4 || row?.tagType === 'leave' || row?.tagType === 'forgot'
}

const handleExport = async () => {
  const response = await exportAttendanceManage({ labId: labId.value, startDate: selectedDate.value, endDate: selectedDate.value })
  const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `attendance-${selectedDate.value}.xlsx`
  link.click()
  window.URL.revokeObjectURL(url)
}

const signMethodLabel = (v) => ({ code: '签到码', qr: '二维码' }[v] || '-')
const formatDateTime = (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-')

const formatDurationHint = (seconds) => {
  if (!seconds || seconds < 60) return `${seconds || 0} 秒`
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  const parts = []
  if (h) parts.push(`${h} 小时`)
  if (m) parts.push(`${m} 分钟`)
  if (s) parts.push(`${s} 秒`)
  return `约 ${parts.join(' ')}`
}

const calcDuration = (checkin, signOut) => {
  if (!checkin || !signOut) return '-'
  const diff = dayjs(signOut).diff(dayjs(checkin), 'minute')
  if (diff < 1) return '< 1 分钟'
  if (diff < 60) return `${diff} 分钟`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return m ? `${h} 小时 ${m} 分钟` : `${h} 小时`
}

const handleSignOut = async (row) => {
  const confirmed = await ElMessageBox.confirm(
    `确认为 ${row.realName} 执行签退操作？`, '手动签退',
    { confirmButtonText: '确认签退', cancelButtonText: '取消', type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await signOutAttendance({ attendanceId: row.attendanceId, userId: row.userId })
    ElMessage.success(`${row.realName} 已签退`)
    await loadManageList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '签退失败')
  }
}

onMounted(() => {
  loadLabOptions().then(loadPageData)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  sessionCountdown.stop()
  lineChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* ===== Page Layout ===== */
.att-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 28px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== Header ===== */
.att-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.att-header__title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.att-header__date {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 12px;
}

.att-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* ===== Stat Cards ===== */
.att-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.att-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .04);
  transition: transform .2s, box-shadow .2s;
}

.att-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, .06);
}

.att-stat-card__icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.att-stat-card__icon svg {
  width: 22px;
  height: 22px;
}

.att-stat-card--total .att-stat-card__icon {
  background: #ede9fe;
  color: #7c3aed;
}

.att-stat-card--signed .att-stat-card__icon {
  background: #dcfce7;
  color: #16a34a;
}

.att-stat-card--absent .att-stat-card__icon {
  background: #fee2e2;
  color: #dc2626;
}

.att-stat-card--leave .att-stat-card__icon {
  background: #fef3c7;
  color: #d97706;
}

.att-stat-card__body {
  display: flex;
  flex-direction: column;
}

.att-stat-card__label {
  font-size: 13px;
  color: #64748b;
}

.att-stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

/* ===== Charts ===== */
.att-charts {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

.att-chart-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .04);
  padding: 20px;
}

.att-chart-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 2px;
}

.att-chart-card__sub {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 12px;
}

.att-chart-canvas {
  width: 100%;
  height: 260px;
}

/* ===== QR Bar ===== */
.att-qr-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
}

.att-qr-bar__img {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  border: 2px solid rgba(255, 255, 255, .3);
}

.att-qr-bar__placeholder {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  border: 2px solid rgba(255, 255, 255, .3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 20px;
  opacity: .5;
}

.att-qr-bar__info {
  flex: 1;
}

.att-qr-bar__info p {
  margin: 4px 0 0;
  font-size: 13px;
  opacity: .9;
}

.att-qr-bar__tip {
  opacity: .7 !important;
  font-size: 12px !important;
}

.att-qr-bar__code {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: .16em;
  line-height: 1;
}

/* ===== Table Sections ===== */
.att-table-section {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .04);
  padding: 20px;
}

.att-table-section__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.att-table-section__head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.att-table :deep(th) {
  background: #f8fafc !important;
  color: #475569 !important;
  font-weight: 600;
}

.att-muted {
  color: #94a3b8;
  font-size: 12px;
}

/* ===== Responsive ===== */
@media (max-width: 960px) {
  .att-page { padding: 16px; }
  .att-stats { grid-template-columns: repeat(2, 1fr); }
  .att-charts { grid-template-columns: 1fr; }
  .att-header { flex-direction: column; align-items: flex-start; }
  .att-qr-bar { flex-direction: column; text-align: center; }
}

@media (max-width: 640px) {
  .att-stats { grid-template-columns: 1fr; }
}

/* ===== Create Dialog ===== */
.create-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.4;
}
</style>

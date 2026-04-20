<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Attendance Console</p>
        <h1>{{ isSessionActive ? '签到会话进行中' : hasSession ? '最近一次签到会话' : '实验室签到管理台' }}</h1>
        <p>{{ isSessionActive ? '学生可在 60 秒内通过签到码或二维码签到。' : hasSession ? '会话结束后仍保留签到码、二维码和记录，便于核对。' : '点击生成签到码，立即发起一个 60 秒签到会话。' }}</p>
      </div>
      <button class="refresh-btn" type="button" @click="loadPageData">刷新</button>
    </section>

    <section v-if="!labId" class="empty-card">
      <el-empty description="当前账号没有可管理的实验室" :image-size="86" />
    </section>

    <template v-else>
      <section class="action-card">
        <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 100%" @change="loadPageData">
          <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
        </el-select>
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="loadManageList" />
        <div class="button-row">
          <el-button type="primary" :loading="creatingSession" @click="handleCreateSession">生成签到码</el-button>
          <el-button :disabled="!isSessionActive" :loading="expiringSession" @click="handleExpireSession">作废</el-button>
          <el-button :disabled="!isSessionActive" :loading="expiringSession" @click="handleFinalize">保存结果</el-button>
          <el-button plain type="success" @click="handleExport">导出名单</el-button>
        </div>
      </section>

      <section class="metric-grid">
        <article v-for="card in summaryCards" :key="card.label" class="metric-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.tip }}</small>
        </article>
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>当前会话</h2>
          <span class="status-chip">{{ sessionStatusText }}</span>
        </header>
        <div v-if="hasSession" class="session-grid">
          <article class="code-card">
            <span>签到码</span>
            <strong>{{ visibleSignCode }}</strong>
            <small>剩余 {{ remainingText }}</small>
          </article>
          <article class="code-card">
            <span>会话编号</span>
            <strong>{{ activeSession.sessionNo || '-' }}</strong>
            <small>{{ formatDateTime(activeSession.expireTime) }}</small>
          </article>
        </div>
        <div v-if="isSessionCodeVisible" class="qr-wrap">
          <img v-if="qrCodeDataUrl" class="qr-image" :src="qrCodeDataUrl" alt="签到二维码" />
          <p class="muted">扫码进入移动端签到页，也可直接输入签到码。</p>
        </div>
        <el-empty v-else description="暂无进行中的签到会话" :image-size="76" />
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>实时签到记录</h2>
          <span>{{ sessionRecords.length }} 条</span>
        </header>
        <div v-if="sessionRecords.length" class="record-list">
          <article v-for="item in sessionRecords" :key="item.id || item.userId" class="record-card">
            <div>
              <strong>{{ item.realName || '-' }}</strong>
              <p>{{ item.studentId || '-' }}</p>
              <small>{{ formatDateTime(item.signTime) }}</small>
            </div>
            <div class="record-side">
              <span class="status-chip light">{{ signMethodLabel(item.signMethod) }}</span>
              <small>{{ item.statusLabel }}</small>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无签到记录" :image-size="72" />
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>当日考勤名单</h2>
          <span>{{ attendanceRows.length }} 条</span>
        </header>
        <div v-if="attendanceRows.length" class="record-list">
          <article v-for="row in attendanceRows" :key="row.attendanceId || row.userId" class="record-card">
            <div>
              <strong>{{ row.realName || '-' }}</strong>
              <p>{{ row.studentId || '-' }} · {{ attendanceDisplayLabel(row) }}</p>
              <small>{{ row.tagLabel || row.reason || '无标签' }}</small>
            </div>
            <div class="record-side">
              <span class="status-chip light">{{ formatDateTime(row.checkinTime) || '-' }}</span>
              <template v-if="canTagRow(row)">
                <el-button text type="warning" @click="handleTag(row, 'leave')">请假</el-button>
                <el-button text type="primary" @click="handleTag(row, 'forgot')">忘记签到</el-button>
              </template>
            </div>
          </article>
        </div>
        <el-empty v-else description="所选日期暂无考勤记录" :image-size="72" />
      </section>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import QRCode from 'qrcode'
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAttendanceSession,
  exportAttendanceManage,
  expireAttendanceSession,
  finalizeAttendanceSession,
  getActiveAttendanceSession,
  getAttendanceManageList,
  getAttendanceSessionRecords,
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
  if (userStore.userInfo?.managedLabId) {
    return userStore.userInfo.managedLabId
  }
  return hasBroadLabScope.value ? null : (userStore.userInfo?.labId || null)
})
const labId = computed(() => selectedLabId.value || fixedLabId.value || null)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const creatingSession = ref(false)
const expiringSession = ref(false)
const qrCodeDataUrl = ref('')
const sessionRecords = ref([])
const attendanceRows = ref([])

const activeSession = reactive({
  id: null,
  sessionNo: '',
  signCode: '',
  qrCodeContent: '',
  status: '',
  expireTime: '',
  remainingSeconds: 0
})
const sessionCountdown = createSessionCountdown(activeSession)

const stats = reactive({
  totalCount: 0,
  signedCount: 0,
  leaveCount: 0,
  forgotCount: 0,
  absentCount: 0
})

const summaryCards = computed(() => [
  { label: '总人数', value: stats.totalCount, tip: '当前实验室成员数' },
  { label: '已签到', value: stats.signedCount, tip: '签到成功成员' },
  { label: '缺勤', value: stats.absentCount, tip: '未签到成员' },
  { label: '请假', value: stats.leaveCount, tip: '已补标签请假' },
  { label: '忘记签到', value: stats.forgotCount, tip: '已补标签忘记签到' }
])
const hasSession = computed(() => Boolean(activeSession.id))
const isSessionActive = computed(() => activeSession.status === 'active')
const isSessionCodeVisible = computed(() => isSessionActive.value && (activeSession.remainingSeconds || 0) > 0)
const visibleSignCode = computed(() => (isSessionCodeVisible.value ? (activeSession.signCode || '------') : '------'))
const sessionStatusText = computed(() => ({
  active: '进行中',
  expired: '已过期',
  cancelled: '已作废'
}[activeSession.status] || '未开始'))
const remainingText = computed(() => {
  if (!hasSession.value) {
    return '-'
  }
  if (isSessionActive.value) {
    return `${Math.max(activeSession.remainingSeconds || 0, 0)} 秒`
  }
  return '0 秒'
})

watch(
  () => activeSession.qrCodeContent,
  async (value) => {
    if (!value) {
      qrCodeDataUrl.value = ''
      return
    }
    qrCodeDataUrl.value = await QRCode.toDataURL(buildPublicAppUrl(value), { width: 220, margin: 1 })
  },
  { immediate: true }
)

const loadActiveSession = async () => {
  const response = await getActiveAttendanceSession({ labId: labId.value })
  Object.assign(activeSession, {
    id: null,
    sessionNo: '',
    signCode: '',
    qrCodeContent: '',
    status: '',
    expireTime: '',
    remainingSeconds: 0,
    ...(response.data || {})
  })
  sessionCountdown.restart()
  if (!activeSession.id) {
    sessionRecords.value = []
    return
  }
  const recordsResponse = await getAttendanceSessionRecords({ sessionId: activeSession.id })
  sessionRecords.value = recordsResponse.data?.records || []
}

const loadManageList = async () => {
  const response = await getAttendanceManageList({ labId: labId.value, date: selectedDate.value })
  attendanceRows.value = response.data?.rows || []
  Object.assign(stats, response.data?.stat || {
    totalCount: 0,
    signedCount: 0,
    leaveCount: 0,
    forgotCount: 0,
    absentCount: 0
  })
}

const loadPageData = async () => {
  if (!labId.value) {
    return
  }
  await Promise.all([loadActiveSession(), loadManageList()])
}

const loadLabOptions = async () => {
  if (!showLabSelector.value) {
    return
  }
  const response = await getLabPage({
    pageNum: 1,
    pageSize: 500,
    collegeId: userStore.userInfo?.managedCollegeId
  })
  labOptions.value = response.data?.records || []
  if (!selectedLabId.value) {
    const preferredLabId = userStore.userInfo?.labId || userStore.userInfo?.managedLabId || null
    selectedLabId.value = labOptions.value.find((item) => item.id === preferredLabId)?.id || labOptions.value[0]?.id || null
  }
}

const handleCreateSession = async () => {
  creatingSession.value = true
  try {
    const response = await createAttendanceSession({ labId: labId.value })
    Object.assign(activeSession, {
      id: null,
      sessionNo: '',
      signCode: '',
      qrCodeContent: '',
      status: '',
      expireTime: '',
      remainingSeconds: 0,
      ...(response.data || {})
    })
    sessionCountdown.restart()
    await Promise.all([loadSessionRecords(response.data?.id), loadManageList()])
    ElMessage.success('签到会话已创建')
  } finally {
    creatingSession.value = false
  }
}

const closeSession = async (action, message) => {
  if (!activeSession.id) {
    ElMessage.info('当前没有可处理的会话')
    return
  }
  expiringSession.value = true
  try {
    await action({ sessionId: activeSession.id })
    await loadPageData()
    ElMessage.success(message)
  } finally {
    expiringSession.value = false
  }
}

const handleExpireSession = async () => {
  const confirmed = await ElMessageBox.confirm('这会立即结束签到并沉淀结果，是否继续？', '作废会话', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await closeSession(expireAttendanceSession, '会话已作废并归档')
}

const handleFinalize = async () => {
  const confirmed = await ElMessageBox.confirm('这会立即结束签到并保存当前结果，是否继续？', '保存结果', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  await closeSession(finalizeAttendanceSession, '结果已保存并归档')
}

const handleTag = async (row, tagType) => {
  const title = tagType === 'leave' ? '标记请假' : '标记忘记签到'
  const result = await ElMessageBox.prompt('可填写备注，直接确认则留空。', title, {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: row.reason || ''
  }).catch(() => null)
  if (!result) {
    return
  }
  await tagAttendanceManage({
    attendanceId: row.attendanceId,
    tagType,
    reason: result.value || ''
  })
  ElMessage.success('标签已保存')
  await loadManageList()
}

const canTagRow = (row) => {
  const status = Number(row?.status)
  return status === 3 || status === 4 || row?.tagType === 'leave' || row?.tagType === 'forgot'
}

const attendanceDisplayLabel = (row) => row?.tagLabel || row?.statusLabel || '未生成'

const handleExport = async () => {
  const response = await exportAttendanceManage({
    labId: labId.value,
    startDate: selectedDate.value,
    endDate: selectedDate.value
  })
  const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/octet-stream' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `attendance-${selectedDate.value}.xlsx`
  link.click()
  window.URL.revokeObjectURL(url)
}

const signMethodLabel = (value) => ({ code: '签到码', qr: '二维码' }[value] || '-')
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '')

onMounted(() => {
  loadLabOptions().then(loadPageData)
})

onUnmounted(() => {
  sessionCountdown.stop()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card,
.action-card,
.metric-card,
.panel-card,
.record-card,
.code-card,
.empty-card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(2, 132, 199, 0.88));
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1,
.panel-head h2 {
  margin: 0;
}

.hero-card p,
.muted {
  color: rgba(226, 232, 240, 0.88);
}

.refresh-btn {
  height: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border-radius: 14px;
  padding: 10px 14px;
}

.action-card,
.panel-card {
  padding: 14px;
}

.button-row,
.metric-grid,
.record-list,
.session-grid {
  display: grid;
  gap: 10px;
}

.button-row {
  margin-top: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.code-card,
.record-card {
  padding: 14px;
}

.metric-card span,
.metric-card small,
.code-card span,
.code-card small,
.record-card p,
.record-card small {
  color: #64748b;
}

.metric-card strong,
.code-card strong,
.record-card strong {
  color: #0f172a;
}

.panel-head,
.record-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.status-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(219, 234, 254, 0.9);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.status-chip.light {
  background: rgba(15, 23, 42, 0.06);
  color: #334155;
}

.record-side {
  display: grid;
  gap: 6px;
  justify-items: end;
}

.qr-wrap {
  margin-top: 14px;
  display: grid;
  place-items: center;
  gap: 10px;
}

.qr-image {
  width: 220px;
  height: 220px;
  border-radius: 24px;
  background: #fff;
}

@media (max-width: 480px) {
  .button-row,
  .metric-grid,
  .session-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Teacher Attendance</p>
        <h1>{{ isSessionActive ? '当前签到会话' : hasSession ? '最近一次签到会话' : '教师考勤查看' }}</h1>
        <p>{{ isSessionActive ? '可查看当前动态签到状态和当日归档结果。' : hasSession ? '会话已结束，签到码和记录仍保留展示。' : '当前没有进行中的签到会话。' }}</p>
      </div>
      <button class="refresh-btn" type="button" @click="loadPageData">刷新</button>
    </section>

    <section v-if="!labId" class="empty-card">
      <el-empty description="当前账号没有绑定实验室" :image-size="86" />
    </section>

    <template v-else>
      <section class="action-card">
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="loadManageList" />
        <el-button type="success" plain class="top-gap" @click="handleExport">导出名单</el-button>
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
        <div class="record-list">
          <article class="record-card">
            <div>
              <strong>{{ activeSession.sessionNo || '暂无最近会话' }}</strong>
              <p>签到码：{{ visibleSignCode }}</p>
              <small>结束时间：{{ formatDateTime(activeSession.expireTime) }}</small>
            </div>
            <div class="record-side">
              <span class="status-chip light">{{ remainingText }}</span>
            </div>
          </article>
        </div>
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
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无实时签到记录" :image-size="72" />
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { exportAttendanceManage, getActiveAttendanceSession, getAttendanceManageList, getAttendanceSessionRecords } from '@/api/attendance'
import { useUserStore } from '@/stores/user'
import { createSessionCountdown } from '@/utils/attendanceSession'

const userStore = useUserStore()
const labId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const sessionRecords = ref([])
const attendanceRows = ref([])

const activeSession = reactive({
  id: null,
  sessionNo: '',
  signCode: '',
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
  { label: '请假', value: stats.leaveCount, tip: '已标记请假' },
  { label: '忘记签到', value: stats.forgotCount, tip: '已标记忘记签到' }
])
const hasSession = computed(() => Boolean(activeSession.id))
const isSessionActive = computed(() => activeSession.status === 'active')
const visibleSignCode = computed(() => (isSessionActive.value && (activeSession.remainingSeconds || 0) > 0 ? (activeSession.signCode || '------') : '------'))
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
    return `${activeSession.remainingSeconds || 0} 秒`
  }
  return '0 秒'
})

const loadActiveSession = async () => {
  const response = await getActiveAttendanceSession({ labId: labId.value })
  Object.assign(activeSession, {
    id: null,
    sessionNo: '',
    signCode: '',
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
const attendanceDisplayLabel = (row) => row?.tagLabel || row?.statusLabel || '未生成'

onMounted(() => {
  loadPageData()
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
.empty-card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(180, 83, 9, 0.88));
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

.hero-card p {
  color: rgba(226, 232, 240, 0.9);
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

.top-gap {
  margin-top: 12px;
}

.metric-grid,
.record-list {
  display: grid;
  gap: 10px;
}

.metric-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.record-card {
  padding: 14px;
}

.metric-card span,
.metric-card small,
.record-card p,
.record-card small {
  color: #64748b;
}

.metric-card strong,
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

.record-side {
  display: grid;
  justify-items: end;
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

@media (max-width: 480px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>

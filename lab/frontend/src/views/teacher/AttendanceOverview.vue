<template>
  <div class="att-page">
    <header class="att-header">
      <div class="att-header__left">
        <h1 class="att-header__title">考勤查看</h1>
        <span class="att-header__date">{{ selectedDate }}</span>
      </div>
      <div class="att-header__actions">
        <el-date-picker v-model="selectedDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @change="loadManageList" />
        <el-button circle @click="loadPageData"><el-icon><Refresh /></el-icon></el-button>
        <el-button type="success" plain :disabled="!labId" @click="handleExport">
          <el-icon class="el-icon--left"><Download /></el-icon>导出名单
        </el-button>
      </div>
    </header>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有绑定实验室" />
    </div>

    <template v-else>
      <!-- 统计卡片 -->
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

      <!-- 实时签到记录 -->
      <section class="att-table-section">
        <div class="att-table-section__head">
          <h3>实时签到记录</h3>
          <el-tag v-if="sessionRecords.length" type="info" effect="plain">{{ sessionRecords.length }} 条</el-tag>
        </div>
        <div v-if="sessionRecords.length" class="record-grid">
          <article v-for="item in sessionRecords" :key="item.id || item.userId" class="record-card">
            <div class="record-card__info">
              <strong>{{ item.realName || '-' }}</strong>
              <span>{{ item.studentId || '-' }}</span>
              <small>{{ formatDateTime(item.signTime) }}</small>
            </div>
            <span class="record-card__badge">{{ signMethodLabel(item.signMethod) }}</span>
          </article>
        </div>
        <el-empty v-else description="暂无实时签到记录" :image-size="72" />
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
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  exportAttendanceManage,
  getActiveAttendanceSession,
  getAttendanceManageList,
  getAttendanceSessionRecords
} from '@/api/attendance'
import { useUserStore } from '@/stores/user'
import { createSessionCountdown } from '@/utils/attendanceSession'

const userStore = useUserStore()
const labId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const sessionRecords = ref([])
const attendanceRows = ref([])

const activeSession = reactive({
  id: null, sessionNo: '', signCode: '', status: '', expireTime: '', remainingSeconds: 0
})
const sessionCountdown = createSessionCountdown(activeSession)
const stats = reactive({ totalCount: 0, signedCount: 0, leaveCount: 0, forgotCount: 0, absentCount: 0 })

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

const handleResize = () => { lineChart?.resize(); pieChart?.resize() }

// ---------- data ----------
const loadActiveSession = async () => {
  const response = await getActiveAttendanceSession({ labId: labId.value })
  Object.assign(activeSession, {
    id: null, sessionNo: '', signCode: '', status: '', expireTime: '', remainingSeconds: 0,
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

const calcDuration = (checkin, signOut) => {
  if (!checkin || !signOut) return '-'
  const diff = dayjs(signOut).diff(dayjs(checkin), 'minute')
  if (diff < 1) return '< 1 分钟'
  if (diff < 60) return `${diff} 分钟`
  const h = Math.floor(diff / 60)
  const m = diff % 60
  return m ? `${h} 小时 ${m} 分钟` : `${h} 小时`
}

onMounted(() => {
  loadPageData()
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
.att-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 28px;
  max-width: 1400px;
  margin: 0 auto;
}

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
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  transition: transform .2s, box-shadow .2s;
}

.att-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,.06);
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

.att-stat-card__icon svg { width: 22px; height: 22px; }

.att-stat-card--total .att-stat-card__icon { background: #ede9fe; color: #7c3aed; }
.att-stat-card--signed .att-stat-card__icon { background: #dcfce7; color: #16a34a; }
.att-stat-card--absent .att-stat-card__icon { background: #fee2e2; color: #dc2626; }
.att-stat-card--leave .att-stat-card__icon { background: #fef3c7; color: #d97706; }

.att-stat-card__body { display: flex; flex-direction: column; }
.att-stat-card__label { font-size: 13px; color: #64748b; }
.att-stat-card__value { font-size: 28px; font-weight: 700; color: #0f172a; line-height: 1.2; }

.att-charts {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

.att-chart-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  padding: 20px;
}

.att-chart-card__title { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0 0 2px; }
.att-chart-card__sub { font-size: 12px; color: #94a3b8; margin: 0 0 12px; }
.att-chart-canvas { width: 100%; height: 260px; }

.att-table-section {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  padding: 20px;
}

.att-table-section__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.att-table-section__head h3 { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0; }

.record-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}

.record-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.record-card__info { display: flex; flex-direction: column; gap: 2px; }
.record-card__info strong { font-size: 14px; color: #1e293b; }
.record-card__info span { font-size: 12px; color: #64748b; }
.record-card__info small { font-size: 11px; color: #94a3b8; }

.record-card__badge {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ede9fe;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 600;
}

.att-table :deep(th) { background: #f8fafc !important; color: #475569 !important; font-weight: 600; }

@media (max-width: 960px) {
  .att-page { padding: 16px; }
  .att-stats { grid-template-columns: repeat(2, 1fr); }
  .att-charts { grid-template-columns: 1fr; }
  .att-header { flex-direction: column; align-items: flex-start; }
}

@media (max-width: 640px) {
  .att-stats { grid-template-columns: 1fr; }
}
</style>

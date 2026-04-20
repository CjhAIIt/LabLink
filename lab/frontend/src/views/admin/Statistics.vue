<template>
  <div v-loading="loading" class="page-shell statistics-page">
    <section class="page-hero statistics-hero">
      <div class="hero-copy">
        <p class="eyebrow">统计分析</p>
        <h1>{{ pageTitle }}</h1>
        <p class="hero-subtitle">
          查看当前管理范围内的成员、考勤、设备和资料治理统计指标。
        </p>
      </div>
      <div class="hero-meta">
        <div class="hero-chip">{{ scopeLabel }}</div>
        <div class="hero-chip muted">{{ rangeLabel }}</div>
      </div>
    </section>

    <TablePageCard class="panel-card filter-card" title="时间范围" subtitle="统计筛选">
      <template #header-extra>
        <el-button type="primary" :loading="loading" @click="loadStatistics">刷新</el-button>
        <el-button @click="useLast30Days">最近 30 天</el-button>
      </template>
      <div class="filter-row">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          unlink-panels
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </div>
    </TablePageCard>

    <section class="summary-grid">
      <MetricCard v-for="card in summaryCards" :key="card.label" :label="card.label" :value="card.value" :tip="card.tip" />
    </section>

    <section class="pending-grid">
      <MetricCard v-for="card in pendingCards" :key="card.label" :label="card.label" :value="card.value" />
    </section>

    <section class="content-grid">
      <ChartCard title="实验室概览" class="panel-card">
        <div class="group-stack">
          <section v-for="group in labGroups" :key="group.key" class="group-block">
            <header class="group-title">{{ group.title }}</header>
            <div v-if="group.data?.length" class="metric-list">
              <article v-for="item in group.data" :key="`${group.key}-${item.name}`" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ item.name || '-' }}</span>
                  <strong>{{ formatPlainValue(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill" :style="{ width: calcWidth(item.value, group.data) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
        </div>
      </ChartCard>

      <ChartCard title="成员概览" class="panel-card">
        <div class="group-stack">
          <section v-for="group in memberGroups" :key="group.key" class="group-block">
            <header class="group-title">{{ group.title }}</header>
            <div v-if="group.data?.length" class="metric-list">
              <article v-for="item in group.data" :key="`${group.key}-${item.name}`" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ normalizeMemberName(group.key, item.name) }}</span>
                  <strong>{{ formatPlainValue(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill emerald" :style="{ width: calcWidth(item.value, group.data) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
        </div>
      </ChartCard>

      <ChartCard title="考勤统计" :tag-label="formatPercent(attendance.attendanceRate)" tag-type="primary" class="panel-card">
        <div class="section-grid compact">
          <MetricCard label="出勤率" :value="formatPercent(attendance.attendanceRate)" compact />
          <MetricCard label="请假率" :value="formatPercent(attendance.leaveRate)" compact />
          <MetricCard label="记录数" :value="attendance.totalRecords ?? 0" compact />
        </div>
        <div class="group-stack">
          <section class="group-block">
            <header class="group-title">状态分布</header>
            <div v-if="attendance.statusDistribution?.length" class="metric-list">
              <article v-for="item in attendance.statusDistribution" :key="item.name" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ formatPlainValue(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill amber" :style="{ width: calcWidth(item.value, attendance.statusDistribution) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
          <section class="group-block">
            <header class="group-title">月度趋势</header>
            <div v-if="attendance.monthlyTrend?.length" class="metric-list">
              <article v-for="item in attendance.monthlyTrend" :key="item.name" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ formatPercent(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill sky" :style="{ width: calcPercentWidth(item.value) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
        </div>
      </ChartCard>

      <ChartCard title="设备统计" :tag-label="devices.totalDevices ?? 0" class="panel-card">
        <div class="group-stack">
          <section v-for="group in deviceGroups" :key="group.key" class="group-block">
            <header class="group-title">{{ group.title }}</header>
            <div v-if="group.data?.length" class="metric-list">
              <article v-for="item in group.data" :key="`${group.key}-${item.name}`" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ formatPlainValue(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill plum" :style="{ width: calcWidth(item.value, group.data) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
        </div>
      </ChartCard>

      <ChartCard title="资料统计" :tag-label="formatPercent(profiles.approvedRate)" tag-type="success" class="panel-card">
        <div class="section-grid compact">
          <MetricCard label="资料总数" :value="profiles.totalProfiles ?? 0" compact />
          <MetricCard label="待审核" :value="profiles.pendingCount ?? 0" compact />
          <MetricCard label="通过率" :value="formatPercent(profiles.approvedRate)" compact />
        </div>
        <div class="group-stack">
          <section v-for="group in profileGroups" :key="group.key" class="group-block">
            <header class="group-title">{{ group.title }}</header>
            <div v-if="group.data?.length" class="metric-list">
              <article v-for="item in group.data" :key="`${group.key}-${item.name}`" class="metric-row">
                <div class="metric-row-head">
                  <span>{{ item.name }}</span>
                  <strong>{{ formatPlainValue(item.value) }}</strong>
                </div>
                <div class="metric-track">
                  <span class="metric-fill rose" :style="{ width: calcWidth(item.value, group.data) }" />
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无数据" :image-size="54" />
          </section>
        </div>
      </ChartCard>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import ChartCard from '@/components/common/ChartCard.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import {
  getStatisticsAttendance,
  getStatisticsDashboard,
  getStatisticsDevices,
  getStatisticsLabs,
  getStatisticsMembers,
  getStatisticsProfiles
} from '@/api/statistics'

const loading = ref(false)
const dateRange = ref([
  dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])
const dashboard = ref({})
const labGroups = ref([])
const memberGroups = ref([])
const attendance = ref({})
const devices = ref({})
const profiles = ref({})

const scopeLabel = computed(() => dashboard.value.scopeName || '当前范围')
const scopeType = computed(() => dashboard.value.scopeType || 'lab')
const pageTitle = computed(() => {
  if (scopeType.value === 'school') return '学校统计'
  if (scopeType.value === 'college') return '学院统计'
  return '实验室统计'
})
const rangeLabel = computed(() => {
  const [startDate, endDate] = dateRange.value || []
  if (!startDate || !endDate) {
    return '自定义范围'
  }
  return `${startDate} 至 ${endDate}`
})

const summaryCards = computed(() => {
  const summary = dashboard.value.summary || {}
  return [
    { label: '实验室数', value: summary.labCount ?? 0, tip: '当前范围内纳入管理的实验室数量' },
    { label: '成员数', value: summary.memberCount ?? 0, tip: '当前范围内的活跃成员数量' },
    { label: '设备数', value: summary.deviceCount ?? 0, tip: '已登记的设备资产数量' },
    { label: '文件数', value: summary.fileCount ?? 0, tip: '管理范围内资料空间文件数量' },
    { label: '出勤率', value: formatPercent(summary.attendanceRate), tip: '正常、迟到和补签通过记录占比' },
    { label: '资料通过率', value: formatPercent(summary.profileApprovedRate), tip: '已通过和已归档资料占比' }
  ]
})

const pendingCards = computed(() => {
  const pending = dashboard.value.pending || {}
  return [
    { label: '待审请假', value: pending.pendingLeaves ?? 0 },
    { label: '待审资料', value: pending.pendingProfiles ?? 0 },
    { label: '待处理维修', value: pending.pendingMaintenance ?? 0 }
  ]
})

const deviceGroups = computed(() => [
  { key: 'statusDistribution', title: '状态分布', data: devices.value.statusDistribution || [] },
  { key: 'categoryDistribution', title: '分类分布', data: devices.value.categoryDistribution || [] },
  { key: 'maintenanceDistribution', title: '维修分布', data: devices.value.maintenanceDistribution || [] }
])

const profileGroups = computed(() => [
  { key: 'statusDistribution', title: '状态分布', data: profiles.value.statusDistribution || [] },
  { key: 'directionDistribution', title: '方向分布', data: profiles.value.directionDistribution || [] },
  { key: 'orgDistribution', title: '组织分布', data: profiles.value.orgDistribution || [] }
])

const buildParams = () => {
  const [startDate, endDate] = dateRange.value || []
  return {
    startDate,
    endDate
  }
}

const loadStatistics = async () => {
  loading.value = true
  try {
    const params = buildParams()
    const [
      dashboardRes,
      labsRes,
      membersRes,
      attendanceRes,
      devicesRes,
      profilesRes
    ] = await Promise.all([
      getStatisticsDashboard(params),
      getStatisticsLabs(params),
      getStatisticsMembers(params),
      getStatisticsAttendance(params),
      getStatisticsDevices(params),
      getStatisticsProfiles(params)
    ])

    dashboard.value = dashboardRes.data || {}
    labGroups.value = labsRes.data || []
    memberGroups.value = membersRes.data || []
    attendance.value = attendanceRes.data || {}
    devices.value = devicesRes.data || {}
    profiles.value = profilesRes.data || {}
  } finally {
    loading.value = false
  }
}

const useLast30Days = () => {
  dateRange.value = [
    dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD')
  ]
  loadStatistics()
}

const normalizeMemberName = (groupKey, value) => {
  if (groupKey !== 'memberRoles') {
    return value || '-'
  }
  const map = {
    member: '成员',
    lab_admin: '实验室管理员',
    lab_leader: '实验室负责人',
    advisor_teacher: '指导教师'
  }
  return map[value] || value || '-'
}

const formatPlainValue = (value) => {
  if (value === undefined || value === null || value === '') {
    return '0'
  }
  return `${value}`
}

const formatPercent = (value) => {
  const number = Number(value ?? 0)
  if (Number.isNaN(number)) {
    return '0%'
  }
  return `${Math.round(number * 100) / 100}%`
}

const calcWidth = (value, rows) => {
  const max = Math.max(...(rows || []).map((item) => Number(item.value) || 0), 0)
  if (max <= 0) {
    return '0%'
  }
  const current = Number(value) || 0
  return `${Math.max(8, Math.round((current / max) * 100))}%`
}

const calcPercentWidth = (value) => {
  const current = Math.max(0, Math.min(100, Number(value) || 0))
  return `${current}%`
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-page {
  background: #f8fafc;
}

.statistics-hero {
  background:
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.22), transparent 28%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(8, 47, 73, 0.9));
}

.hero-copy {
  display: grid;
  gap: 10px;
}

.hero-copy h1 {
  color: #fff;
  margin: 0;
}

.hero-subtitle {
  max-width: 760px;
  color: rgba(226, 232, 240, 0.92);
}

.hero-meta {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-chip {
  padding: 10px 14px;
  border-radius: 999px;
  color: #e0f2fe;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.hero-chip.muted {
  color: rgba(226, 232, 240, 0.88);
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card,
.pending-card,
.mini-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

.summary-label,
.pending-label,
.mini-label {
  color: #64748b;
  font-size: 13px;
}

.summary-value,
.pending-value,
.mini-value {
  color: #0f172a;
  font-size: 26px;
}

.summary-tip {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.panel-card {
  border-radius: 22px;
}

.group-stack {
  display: grid;
  gap: 22px;
}

.group-block {
  display: grid;
  gap: 14px;
}

.group-title {
  color: #0f172a;
  font-weight: 600;
}

.metric-list {
  display: grid;
  gap: 14px;
}

.metric-row {
  display: grid;
  gap: 8px;
}

.metric-row-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
  font-size: 14px;
}

.metric-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.metric-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0f766e, #14b8a6);
}

.metric-fill.emerald {
  background: linear-gradient(90deg, #15803d, #22c55e);
}

.metric-fill.amber {
  background: linear-gradient(90deg, #d97706, #f59e0b);
}

.metric-fill.sky {
  background: linear-gradient(90deg, #0369a1, #38bdf8);
}

.metric-fill.plum {
  background: linear-gradient(90deg, #7c3aed, #a855f7);
}

.metric-fill.rose {
  background: linear-gradient(90deg, #e11d48, #fb7185);
}

.section-grid.compact {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .content-grid,
  .summary-grid,
  .pending-grid,
  .section-grid.compact {
    grid-template-columns: 1fr;
  }

  .hero-meta {
    justify-items: start;
  }
}
</style>

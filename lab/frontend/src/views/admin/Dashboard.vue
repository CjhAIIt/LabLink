<template>
  <div class="page-shell">
    <section class="page-hero admin-hero">
      <div>
        <p class="eyebrow">管理工作台</p>
        <h1>{{ dashboardTitle }}</h1>
        <p class="hero-subtitle">
          聚合实验室组织、招新、考勤、资料空间和公告数据，把管理过程转成可展示、可追溯、可分析的比赛级看板。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-chip">范围：{{ scopeLabel }}</div>
        <div class="hero-chip muted">角色：{{ managementRoleLabel }}</div>
        <el-button class="hero-button" plain :loading="exportLoading" @click="handleExport">
          导出治理简报
        </el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <el-card v-if="pendingApprovals.length" shadow="never" class="panel-card pending-panel">
      <template #header>
        <div class="panel-header">
          <span>{{ pendingSectionTitle }}</span>
          <el-tag type="warning" effect="plain">{{ overview.pendingApprovalTotal ?? 0 }} 项</el-tag>
        </div>
      </template>

      <div class="pending-grid">
        <article v-for="item in pendingApprovals" :key="item.label" class="pending-card">
          <div class="pending-main">
            <span class="pending-label">{{ item.label }}</span>
            <strong class="pending-value">{{ item.value }}</strong>
          </div>
          <p>{{ item.description }}</p>
          <el-button v-if="item.route" link type="primary" @click="goRoute(item.route)">立即处理</el-button>
        </article>
      </div>
    </el-card>

    <section class="content-grid two-column">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>最新申请</span>
            <el-tag type="primary" effect="plain">{{ recentApplies.length }} 条</el-tag>
          </div>
        </template>

        <el-table :data="recentApplies" stripe>
          <el-table-column prop="studentName" label="学生" min-width="120" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column v-if="showLabColumn" prop="labName" label="实验室" min-width="150" />
          <el-table-column prop="status" label="状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>最新公告</span>
            <el-tag type="success" effect="plain">{{ notices.length }} 条</el-tag>
          </div>
        </template>

        <div v-if="!notices.length" class="empty-panel">
          <el-empty description="暂无公告" />
        </div>
        <div v-else class="notice-list">
          <article v-for="notice in notices" :key="notice.id" class="notice-item">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.content }}</p>
            <span>{{ formatDateTime(notice.publishTime) }}</span>
          </article>
        </div>
      </el-card>
    </section>

    <section class="content-grid two-column">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>{{ primaryPanelTitle }}</span>
          </div>
        </template>

        <div v-if="primaryList.length" class="rank-list">
          <div v-for="(item, index) in primaryList" :key="`${item.name}-${index}`" class="rank-item">
            <div class="rank-item-main">
              <span class="rank-index">{{ index + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
            </div>
            <div class="rank-value">{{ primaryValue(item) }}</div>
          </div>
        </div>
        <div v-else class="empty-panel">
          <el-empty description="暂无统计数据" />
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>{{ secondaryPanelTitle }}</span>
          </div>
        </template>

        <div v-if="secondaryList.length" class="rank-list">
          <div v-for="(item, index) in secondaryList" :key="`${item.name}-${index}`" class="rank-item">
            <div class="rank-item-main">
              <span class="rank-index">{{ index + 1 }}</span>
              <span class="rank-name">{{ secondaryName(item) }}</span>
            </div>
            <div class="rank-value">{{ secondaryValue(item) }}</div>
          </div>
        </div>
        <div v-else class="empty-panel">
          <el-empty description="暂无统计数据" />
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLatestNotices } from '@/api/notices'
import { getOverviewStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import { downloadCsv } from '@/utils/export'

const router = useRouter()
const userStore = useUserStore()
const overview = ref({})
const notices = ref([])
const exportLoading = ref(false)

const scopeType = computed(() => {
  if (overview.value.scopeType) {
    return overview.value.scopeType
  }
  if (userStore.userInfo?.schoolDirector) {
    return 'school'
  }
  if (userStore.userInfo?.collegeManager) {
    return 'college'
  }
  return 'lab'
})

const scopeLabel = computed(() => {
  if (scopeType.value === 'school') {
    return '全校实验室'
  }
  return overview.value.scopeName || (scopeType.value === 'college' ? '当前学院' : '当前实验室')
})

const dashboardTitle = computed(() => {
  if (scopeType.value === 'school') {
    return '学校全局实验室治理概览'
  }
  if (scopeType.value === 'college') {
    return `${scopeLabel.value}治理概览`
  }
  return `${scopeLabel.value}运营概览`
})

const managementRoleLabel = computed(() => {
  if (userStore.userInfo?.schoolDirector) {
    return '学校管理员'
  }
  if (userStore.userInfo?.collegeManager) {
    return '学院管理员'
  }
  if (userStore.userInfo?.labManager) {
    return '实验室管理员'
  }
  return '管理员'
})

const recentApplies = computed(() => overview.value.recentApplies || [])
const pendingApprovals = computed(() => overview.value.pendingApprovals || [])
const showLabColumn = computed(() => scopeType.value !== 'lab')
const pendingSectionTitle = computed(() =>
  scopeType.value === 'lab' ? '待办事项总览' : '待办审批总览'
)

const metricCards = computed(() => {
  if (scopeType.value === 'school') {
    return [
      { label: '学院总数', value: overview.value.collegeCount ?? 0, tip: '学校治理覆盖学院数' },
      { label: '实验室总数', value: overview.value.labCount ?? 0, tip: '纳入平台的实验室规模' },
      { label: '指导老师', value: overview.value.teacherCount ?? 0, tip: '参与实验室指导的老师数量' },
      { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '当前有效成员总量' },
      { label: '待处理事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '学校层面的待办审批总数' }
    ]
  }

  if (scopeType.value === 'college') {
    return [
      { label: '实验室总数', value: overview.value.labCount ?? 0, tip: '本学院纳入管理的实验室数量' },
      { label: '指导老师', value: overview.value.teacherCount ?? 0, tip: '本学院实验室指导教师数量' },
      { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '本学院实验室有效成员总量' },
      { label: '开放招新', value: overview.value.openPlanCount ?? 0, tip: '当前仍在开放的招新计划数量' },
      { label: '待处理事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '本学院审批链上的待办数量' }
    ]
  }

  return [
    { label: '成员规模', value: overview.value.memberCount ?? 0, tip: '当前实验室正式成员数' },
    { label: '在招计划', value: overview.value.openPlanCount ?? 0, tip: '正在开放招新的计划数量' },
    { label: '待审核事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '待实验室管理员处理的事项' },
    { label: '月出勤率', value: `${overview.value.attendanceRate ?? 0}%`, tip: '考勤记录综合出勤率' },
    { label: '资料文件', value: overview.value.fileCount ?? 0, tip: '实验室空间累计文件量' }
  ]
})

const primaryList = computed(() =>
  scopeType.value === 'lab' ? overview.value.recentFiles || [] : overview.value.activityRanking || []
)

const secondaryList = computed(() =>
  scopeType.value === 'lab'
    ? attendanceSummaryList(overview.value.attendanceSummary)
    : overview.value.recruitConversionRanking || []
)

const primaryPanelTitle = computed(() =>
  scopeType.value === 'lab' ? '资料空间最近上传' : '实验室活跃度排行'
)

const secondaryPanelTitle = computed(() =>
  scopeType.value === 'lab' ? '考勤结构' : '招新转化率排行'
)

const loadDashboard = async () => {
  const [statsRes, noticeRes] = await Promise.all([getOverviewStatistics(), getLatestNotices({ limit: 6 })])
  overview.value = statsRes.data || {}
  notices.value = noticeRes.data || []
}

const attendanceSummaryList = (summary) => {
  if (!summary) {
    return []
  }
  return [
    { name: '出勤', value: summary.presentCount ?? 0 },
    { name: '迟到', value: summary.lateCount ?? 0 },
    { name: '补签', value: summary.makeupCount ?? 0 },
    { name: '缺勤', value: summary.absentCount ?? 0 }
  ]
}

const secondaryName = (item) => item.name || '-'
const secondaryValue = (item) => (scopeType.value === 'lab' ? item.value : `${item.value}%`)
const primaryValue = (item) => {
  if (scopeType.value !== 'lab') {
    return item.value
  }
  return formatDateTime(item.createTime)
}

const statusLabel = (status) => {
  const map = {
    submitted: '待审核',
    leader_approved: '初审通过',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  const map = {
    submitted: 'warning',
    leader_approved: 'primary',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const goRoute = async (route) => {
  if (!route) {
    return
  }
  await router.push(route)
}

const appendTableRows = (rows, title, headers, items, cellsBuilder) => {
  rows.push([], [title], headers)
  if (!items.length) {
    rows.push(['暂无数据'])
    return
  }
  for (const item of items) {
    rows.push(cellsBuilder(item))
  }
}

const buildExportRows = () => {
  const rows = [
    ['导出时间', dayjs().format('YYYY-MM-DD HH:mm:ss')],
    ['统计范围', scopeLabel.value],
    ['角色视角', managementRoleLabel.value]
  ]

  appendTableRows(
    rows,
    '核心指标',
    ['指标', '数值', '说明'],
    metricCards.value,
    (item) => [item.label, item.value, item.tip]
  )

  appendTableRows(
    rows,
    pendingSectionTitle.value,
    ['事项', '数量', '说明', '处理入口'],
    pendingApprovals.value,
    (item) => [item.label, item.value, item.description, item.route || '-']
  )

  appendTableRows(
    rows,
    '最新申请',
    showLabColumn.value ? ['学生', '学号', '实验室', '状态', '提交时间'] : ['学生', '学号', '状态', '提交时间'],
    recentApplies.value,
    (item) =>
      showLabColumn.value
        ? [item.studentName || '-', item.studentId || '-', item.labName || '-', statusLabel(item.status), formatDateTime(item.createTime)]
        : [item.studentName || '-', item.studentId || '-', statusLabel(item.status), formatDateTime(item.createTime)]
  )

  appendTableRows(
    rows,
    primaryPanelTitle.value,
    ['名称', '数值'],
    primaryList.value,
    (item) => [item.name || '-', primaryValue(item)]
  )

  appendTableRows(
    rows,
    secondaryPanelTitle.value,
    ['名称', '数值'],
    secondaryList.value,
    (item) => [secondaryName(item), secondaryValue(item)]
  )

  appendTableRows(
    rows,
    '最新公告',
    ['标题', '发布时间', '内容'],
    notices.value,
    (item) => [item.title || '-', formatDateTime(item.publishTime), item.content || '-']
  )

  return rows
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    downloadCsv(
      `aiit-${scopeType.value}-dashboard-${dayjs().format('YYYYMMDD-HHmmss')}.csv`,
      buildExportRows()
    )
    ElMessage.success('治理简报已导出')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.admin-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(15, 118, 110, 0.86)),
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.26), transparent 28%);
}

.hero-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-button {
  border-color: rgba(255, 255, 255, 0.28);
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.1);
}

.hero-chip {
  padding: 10px 14px;
  border-radius: 999px;
  color: #ecfeff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-chip.muted {
  color: rgba(236, 254, 255, 0.9);
}

.pending-panel :deep(.el-card__body) {
  padding-top: 0;
}

.pending-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.pending-card {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.96), rgba(240, 249, 255, 0.9));
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.pending-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.pending-label {
  color: #0f172a;
  font-weight: 600;
}

.pending-value {
  color: #0f766e;
  font-size: 28px;
}

.pending-card p {
  color: #64748b;
  line-height: 1.6;
}

.notice-list {
  display: grid;
  gap: 14px;
}

.notice-item {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(240, 249, 255, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.notice-item p,
.notice-item span {
  color: #64748b;
}
</style>

<template>
  <div class="page-shell">
    <section class="page-hero stats-hero">
      <div>
        <p class="eyebrow">统计分析</p>
        <h1>{{ pageTitle }}</h1>
        <p class="hero-subtitle">
          从招新、考勤、资料空间和活跃度多个维度输出决策信息，把信息管理系统升级为可分析、可比较、可支撑答辩的管理平台。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-chip">统计范围：{{ scopeLabel }}</div>
        <div class="hero-chip muted">角色：{{ managementRoleLabel }}</div>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <section class="stats-grid">
      <el-card v-for="section in sections" :key="section.key" shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>{{ section.title }}</span>
            <el-tag effect="plain">{{ section.data.length }} 项</el-tag>
          </div>
        </template>

        <div v-if="section.data.length" class="bar-list">
          <div v-for="item in section.data" :key="`${section.key}-${item.name}`" class="bar-item">
            <div class="bar-head">
              <span>{{ formatSectionName(section.key, item.name) }}</span>
              <strong>{{ formatValue(section.key, item.value) }}</strong>
            </div>
            <div class="bar-track">
              <span class="bar-fill" :style="{ width: calcWidth(item.value, section.data) }" />
            </div>
          </div>
        </div>
        <div v-else class="empty-panel">
          <el-empty description="暂无统计数据" />
        </div>
      </el-card>
    </section>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>最新申请动态</span>
          <el-tag type="warning" effect="plain">{{ recentApplies.length }} 条</el-tag>
        </div>
      </template>

      <el-table :data="recentApplies" stripe>
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="120" />
        <el-table-column v-if="showLabColumn" prop="labName" label="实验室" min-width="150" />
        <el-table-column prop="status" label="申请状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { getOverviewStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const overview = ref({})

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

const pageTitle = computed(() => {
  if (scopeType.value === 'school') {
    return '学校实验室治理分析'
  }
  if (scopeType.value === 'college') {
    return `${scopeLabel.value}实验室治理分析`
  }
  return `${scopeLabel.value}运营分析`
})

const showLabColumn = computed(() => scopeType.value !== 'lab')
const recentApplies = computed(() => overview.value.recentApplies || [])

const metricCards = computed(() => {
  if (scopeType.value === 'school') {
    return [
      { label: '实验室总数', value: overview.value.labCount ?? 0, tip: '全校纳入管理的实验室' },
      { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '实验室在组成员总量' },
      { label: '月出勤率', value: `${overview.value.attendanceRate ?? 0}%`, tip: '全校考勤综合出勤率' },
      { label: '资料文件', value: overview.value.fileCount ?? 0, tip: '实验室空间累计文件数' },
      { label: '待办事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '学校层面的审批待办总数' }
    ]
  }

  if (scopeType.value === 'college') {
    return [
      { label: '实验室总数', value: overview.value.labCount ?? 0, tip: '本学院纳入治理的实验室' },
      { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '本学院实验室在组成员总量' },
      { label: '月出勤率', value: `${overview.value.attendanceRate ?? 0}%`, tip: '本学院考勤综合出勤率' },
      { label: '资料文件', value: overview.value.fileCount ?? 0, tip: '本学院资料空间累计文件数' },
      { label: '待办事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '本学院审批链上的待办总数' }
    ]
  }

  return [
    { label: '成员规模', value: overview.value.memberCount ?? 0, tip: '当前实验室有效成员数' },
    { label: '申请总量', value: overview.value.applyCount ?? 0, tip: '收到的全部招新申请' },
    { label: '待办事项', value: overview.value.pendingApprovalTotal ?? 0, tip: '待实验室管理员处理的事项' },
    { label: '月出勤率', value: `${overview.value.attendanceRate ?? 0}%`, tip: '实验室综合出勤率' },
    { label: '已归档文件', value: overview.value.archivedFileCount ?? 0, tip: '已归档资料数量' }
  ]
})

const sections = computed(() => {
  if (scopeType.value === 'school') {
    return [
      { key: 'collegeDistribution', title: '学院实验室分布', data: overview.value.collegeDistribution || [] },
      { key: 'activityRanking', title: '实验室活跃度排行', data: overview.value.activityRanking || [] },
      { key: 'recruitConversionRanking', title: '招新转化率排行', data: overview.value.recruitConversionRanking || [] },
      { key: 'monthlyAttendanceTrend', title: '月度出勤率趋势', data: overview.value.monthlyAttendanceTrend || [] },
      { key: 'collegeComparison', title: '学院综合对比', data: overview.value.collegeComparison || [] },
      { key: 'teacherGuidanceRanking', title: '指导老师覆盖排行', data: overview.value.teacherGuidanceRanking || [] }
    ]
  }

  if (scopeType.value === 'college') {
    return [
      { key: 'activityRanking', title: '实验室活跃度排行', data: overview.value.activityRanking || [] },
      { key: 'recruitConversionRanking', title: '招新转化率排行', data: overview.value.recruitConversionRanking || [] },
      { key: 'applyStatus', title: '申请状态分布', data: overview.value.applyStatus || [] },
      { key: 'monthlyApplyTrend', title: '月度申请趋势', data: overview.value.monthlyApplyTrend || [] },
      { key: 'monthlyAttendanceTrend', title: '月度出勤率趋势', data: overview.value.monthlyAttendanceTrend || [] },
      { key: 'teacherGuidanceRanking', title: '指导老师覆盖情况', data: overview.value.teacherGuidanceRanking || [] }
    ]
  }

  return [
    { key: 'memberRoles', title: '成员角色结构', data: overview.value.memberRoles || [] },
    { key: 'applyStatus', title: '申请状态分布', data: overview.value.applyStatus || [] },
    { key: 'monthlyApplyTrend', title: '月度申请趋势', data: overview.value.monthlyApplyTrend || [] },
    { key: 'monthlyAttendanceTrend', title: '月度出勤率趋势', data: overview.value.monthlyAttendanceTrend || [] },
    { key: 'activityRanking', title: '实验室活跃构成', data: overview.value.activityRanking || [] }
  ]
})

const loadStatistics = async () => {
  const response = await getOverviewStatistics()
  overview.value = response.data || {}
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

const memberRoleLabel = (role) => {
  const map = {
    member: '实验室成员',
    lab_leader: '学生负责人'
  }
  return map[role] || role || '-'
}

const formatSectionName = (sectionKey, name) => {
  if (sectionKey === 'applyStatus') {
    return statusLabel(name)
  }
  if (sectionKey === 'memberRoles') {
    return memberRoleLabel(name)
  }
  return name || '-'
}

const formatValue = (sectionKey, value) => {
  if (sectionKey === 'monthlyAttendanceTrend' || sectionKey === 'recruitConversionRanking') {
    return `${value}%`
  }
  return value
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

const calcWidth = (value, items) => {
  const max = Math.max(...items.map((item) => Number(item.value) || 0), 0)
  if (max <= 0 || !value) {
    return '0%'
  }
  return `${Math.max(14, Math.round((Number(value) / max) * 100))}%`
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.stats-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(8, 145, 178, 0.88)),
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.2), transparent 30%);
}

.hero-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-chip {
  padding: 10px 14px;
  border-radius: 999px;
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-chip.muted {
  color: rgba(240, 249, 255, 0.86);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.bar-list {
  display: grid;
  gap: 14px;
}

.bar-item {
  display: grid;
  gap: 10px;
}

.bar-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #0f172a;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #0f766e, #0ea5e9);
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .hero-side {
    justify-items: start;
  }
}
</style>

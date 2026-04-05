<template>
  <div class="page-shell stats-page">
    <section class="page-hero stats-hero">
      <div class="hero-content">
        <div class="hero-text">
          <p class="eyebrow">数据洞察</p>
          <h1>{{ pageTitle }}</h1>
          <p class="hero-subtitle">
            全维度数据透视，实时掌握实验室运营动态，助力精细化治理与科学决策。
          </p>
        </div>
        <div class="hero-side">
          <div class="hero-chip">
            <el-icon><OfficeBuilding /></el-icon>
            {{ scopeLabel }}
          </div>
          <div class="hero-chip muted">
            <el-icon><User /></el-icon>
            {{ managementRoleLabel }}
          </div>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="card in metricCards" :key="card.label" class="metric-card-wrapper">
        <article class="metric-card">
          <div class="metric-info">
            <span class="metric-label">{{ card.label }}</span>
            <strong class="metric-value">{{ card.value }}</strong>
          </div>
          <div class="metric-icon">
            <el-icon v-if="card.label.includes('实验室')"><School /></el-icon>
            <el-icon v-else-if="card.label.includes('成员')"><User /></el-icon>
            <el-icon v-else-if="card.label.includes('出勤')"><Calendar /></el-icon>
            <el-icon v-else-if="card.label.includes('文件')"><Files /></el-icon>
            <el-icon v-else><List /></el-icon>
          </div>
          <el-tooltip :content="card.tip" placement="top">
            <el-icon class="info-trigger"><InfoFilled /></el-icon>
          </el-tooltip>
        </article>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="stats-tabs">
      <el-tab-pane label="运营概览" name="overview">
        <div class="stats-grid">
          <el-card v-for="section in overviewSections" :key="section.key" shadow="never" class="panel-card chart-card">
            <template #header>
              <div class="panel-header">
                <span class="title-with-dot">{{ section.title }}</span>
              </div>
            </template>

            <!-- 简单的可视化展示：使用进度条或环形图 -->
            <div v-if="section.data.length" class="viz-container">
              <div v-if="isDistribution(section.key)" class="pie-placeholder">
                <el-progress 
                  type="circle" 
                  :percentage="getMainPercentage(section.data)" 
                  :stroke-width="12"
                  :color="vizColors"
                >
                  <template #default="{ percentage }">
                    <div class="pie-center">
                      <span class="pie-val">{{ percentage }}%</span>
                      <span class="pie-lab">{{ section.data[0]?.name }}</span>
                    </div>
                  </template>
                </el-progress>
                <div class="viz-legend">
                  <div v-for="(item, idx) in section.data.slice(0, 4)" :key="item.name" class="legend-item">
                    <span class="dot" :style="{ backgroundColor: vizColors[idx % vizColors.length] }"></span>
                    <span class="name">{{ formatSectionName(section.key, item.name) }}</span>
                    <span class="val">{{ item.value }}</span>
                  </div>
                </div>
              </div>

              <div v-else class="bar-list modern-bars">
                <div v-for="item in section.data" :key="`${section.key}-${item.name}`" class="bar-item">
                  <div class="bar-head">
                    <span>{{ formatSectionName(section.key, item.name) }}</span>
                    <strong>{{ formatValue(section.key, item.value) }}</strong>
                  </div>
                  <div class="bar-track">
                    <span class="bar-fill" :style="{ width: calcWidth(item.value, section.data), background: getBarColor(item, section.key) }" />
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-panel">
              <el-empty description="暂无统计数据" :image-size="60" />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="趋势分析" name="trends">
        <div class="stats-grid single-col">
          <el-card shadow="never" class="panel-card trend-card">
            <template #header>
              <div class="panel-header">
                <span class="title-with-dot">核心指标趋势</span>
              </div>
            </template>
            <div class="trend-content">
              <!-- 这里模拟一个折线图 -->
              <div class="line-chart-sim">
                <div class="chart-y-axis">
                  <span>100%</span>
                  <span>50%</span>
                  <span>0%</span>
                </div>
                <div class="chart-main">
                  <svg viewBox="0 0 600 200" class="trend-svg">
                    <!-- 模拟折线 -->
                    <polyline
                      fill="none"
                      stroke="#0ea5e9"
                      stroke-width="3"
                      points="50,150 150,100 250,120 350,60 450,80 550,40"
                    />
                    <!-- 模拟数据点 -->
                    <circle cx="50" cy="150" r="4" fill="#0ea5e9" />
                    <circle cx="150" cy="100" r="4" fill="#0ea5e9" />
                    <circle cx="250" cy="120" r="4" fill="#0ea5e9" />
                    <circle cx="350" cy="60" r="4" fill="#0ea5e9" />
                    <circle cx="450" cy="80" r="4" fill="#0ea5e9" />
                    <circle cx="550" cy="40" r="4" fill="#0ea5e9" />
                  </svg>
                  <div class="chart-x-axis">
                    <span v-for="m in lastSixMonths" :key="m">{{ m }}</span>
                  </div>
                </div>
              </div>
              <div class="trend-summary">
                <div class="trend-stat">
                  <label>月度出勤波动</label>
                  <strong class="up">+12.4%</strong>
                </div>
                <div class="trend-stat">
                  <label>招新增长率</label>
                  <strong class="up">+8.2%</strong>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="实时动态" name="activity">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span class="title-with-dot">最新申请动态</span>
              <el-tag type="warning" effect="light" round>{{ recentApplies.length }} 条待处理</el-tag>
            </div>
          </template>

          <el-table :data="recentApplies" stripe>
            <el-table-column prop="studentName" label="学生姓名" min-width="120" />
            <el-table-column prop="studentId" label="学号" min-width="120" />
            <el-table-column v-if="showLabColumn" prop="labName" label="实验室" min-width="150" />
            <el-table-column prop="status" label="申请状态" min-width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOverviewStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import { 
  OfficeBuilding, 
  User, 
  School, 
  Calendar, 
  Files, 
  List, 
  InfoFilled 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const overview = ref({})
const activeTab = ref(route.query.tab || 'overview')

watch(() => route.query.tab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab
  }
})

watch(activeTab, (newTab) => {
  if (!newTab) {
    return
  }
  if (route.query.tab === newTab) {
    return
  }
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: newTab
    }
  })
})

const vizColors = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']

const scopeType = computed(() => {
  if (overview.value.scopeType) return overview.value.scopeType
  if (userStore.userInfo?.schoolDirector) return 'school'
  if (userStore.userInfo?.collegeManager) return 'college'
  return 'lab'
})

const scopeLabel = computed(() => {
  if (scopeType.value === 'school') return '全校实验室'
  return overview.value.scopeName || (scopeType.value === 'college' ? '当前学院' : '当前实验室')
})

const managementRoleLabel = computed(() => {
  if (userStore.userInfo?.schoolDirector) return '学校管理员'
  if (userStore.userInfo?.collegeManager) return '学院管理员'
  if (userStore.userInfo?.labManager) return '实验室管理员'
  return '管理员'
})

const pageTitle = computed(() => {
  if (scopeType.value === 'school') return '全校实验室治理分析'
  if (scopeType.value === 'college') return `${scopeLabel.value}治理分析`
  return `${scopeLabel.value}运营分析`
})

const showLabColumn = computed(() => scopeType.value !== 'lab')
const recentApplies = computed(() => overview.value.recentApplies || [])

const lastSixMonths = computed(() => {
  const months = []
  for (let i = 5; i >= 0; i--) {
    months.push(dayjs().subtract(i, 'month').format('MM月'))
  }
  return months
})

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

const overviewSections = computed(() => {
  if (scopeType.value === 'school') {
    return [
      { key: 'collegeDistribution', title: '学院实验室分布', data: overview.value.collegeDistribution || [] },
      { key: 'activityRanking', title: '实验室活跃度排行', data: overview.value.activityRanking || [] },
      { key: 'recruitConversionRanking', title: '招新转化率排行', data: overview.value.recruitConversionRanking || [] },
      { key: 'collegeComparison', title: '学院综合能力对比', data: overview.value.collegeComparison || [] }
    ]
  }

  if (scopeType.value === 'college') {
    return [
      { key: 'activityRanking', title: '实验室活跃度排行', data: overview.value.activityRanking || [] },
      { key: 'applyStatus', title: '申请状态分布', data: overview.value.applyStatus || [] },
      { key: 'recruitConversionRanking', title: '招新转化率排行', data: overview.value.recruitConversionRanking || [] },
      { key: 'teacherGuidanceRanking', title: '指导老师覆盖情况', data: overview.value.teacherGuidanceRanking || [] }
    ]
  }

  return [
    { key: 'memberRoles', title: '成员角色结构', data: overview.value.memberRoles || [] },
    { key: 'applyStatus', title: '申请状态分布', data: overview.value.applyStatus || [] },
    { key: 'activityRanking', title: '实验室活跃构成', data: overview.value.activityRanking || [] }
  ]
})

const isDistribution = (key) => ['collegeDistribution', 'applyStatus', 'memberRoles'].includes(key)

const getMainPercentage = (data) => {
  if (!data.length) return 0
  const total = data.reduce((sum, item) => sum + Number(item.value), 0)
  if (total === 0) return 0
  return Math.round((Number(data[0].value) / total) * 100)
}

const getBarColor = (item, key) => {
  if (key === 'recruitConversionRanking') return 'linear-gradient(90deg, #3b82f6, #0ea5e9)'
  if (key === 'activityRanking') return 'linear-gradient(90deg, #8b5cf6, #d946ef)'
  return 'linear-gradient(90deg, #0f766e, #10b981)'
}

const loadStatistics = async () => {
  const response = await getOverviewStatistics()
  overview.value = response.data || {}
}

const statusLabel = (status) => {
  const map = { submitted: '待审核', leader_approved: '初审通过', approved: '已通过', rejected: '已驳回' }
  return map[status] || status || '-'
}

const formatSectionName = (sectionKey, name) => {
  if (sectionKey === 'applyStatus') return statusLabel(name)
  if (sectionKey === 'memberRoles') return name === 'lab_leader' ? '学生负责人' : '实验室成员'
  return name || '-'
}

const formatValue = (sectionKey, value) => {
  if (sectionKey === 'recruitConversionRanking') return `${value}%`
  return value
}

const statusTagType = (status) => {
  const map = { submitted: 'warning', leader_approved: 'primary', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const calcWidth = (value, items) => {
  const max = Math.max(...items.map((item) => Number(item.value) || 0), 0)
  if (max <= 0 || !value) return '0%'
  return `${Math.max(10, Math.round((Number(value) / max) * 100))}%`
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.stats-page {
  background-color: #f8fafc;
}

.stats-hero {
  background: linear-gradient(135deg, #0f172a 0%, #0369a1 100%);
  color: white;
  padding: 40px 60px;
  margin: 0 0 24px 0;
  border-radius: 24px;
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.hero-text h1 {
  font-size: 28px;
  margin: 8px 0;
  color: white;
}

.hero-subtitle {
  color: #bae6fd;
  font-size: 15px;
}

.hero-side {
  display: flex;
  gap: 12px;
}

.hero-chip {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 99px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  transition: transform 0.2s;
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: #3b82f6;
}

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  color: #0f172a;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.info-trigger {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #cbd5e1;
  cursor: help;
  font-size: 14px;
}

.stats-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.stats-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 16px;
}

.single-col {
  grid-template-columns: 1fr;
}

.chart-card {
  border-radius: 16px;
}

.title-with-dot {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.title-with-dot::before {
  content: '';
  width: 4px;
  height: 16px;
  background: #3b82f6;
  border-radius: 2px;
}

.viz-container {
  padding: 10px 0;
}

.pie-placeholder {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 0;
}

.pie-center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pie-val {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.pie-lab {
  font-size: 12px;
  color: #64748b;
}

.viz-legend {
  display: grid;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.legend-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-item .name {
  color: #475569;
  flex: 1;
}

.legend-item .val {
  font-weight: 600;
  color: #1e293b;
}

.modern-bars {
  display: grid;
  gap: 20px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-head {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.bar-track {
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 4px;
}

.line-chart-sim {
  height: 240px;
  display: flex;
  gap: 12px;
  padding: 20px;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
}

.chart-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.trend-svg {
  flex: 1;
  width: 100%;
}

.chart-x-axis {
  display: flex;
  justify-content: space-around;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}

.trend-content {
  display: flex;
  align-items: center;
  gap: 40px;
}

.trend-summary {
  width: 200px;
  display: grid;
  gap: 24px;
}

.trend-stat label {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.trend-stat strong {
  font-size: 20px;
}

.trend-stat .up {
  color: #10b981;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .trend-content {
    flex-direction: column;
  }
}
</style>

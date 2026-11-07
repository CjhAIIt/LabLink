<template>
  <div class="portal-page">
    <header class="portal-nav">
      <BrandLogo size="sm" title="LabLink" subtitle="高校实验室综合管理平台" />
      <nav class="portal-nav-links">
        <router-link to="/register">学生注册</router-link>
        <router-link to="/teacher-register">教师注册</router-link>
        <router-link class="nav-cta" to="/login">登录</router-link>
      </nav>
    </header>

    <main class="main-content">
      <section class="hero">
        <div class="hero-content">
          <span class="hero-eyebrow">面向学校、学院与实验室的一体化平台</span>
          <h1>实验室发现与学院治理</h1>
          <p>
            从学院主数据到实验室档案，一站式浏览实验室与学院信息。登录后进入考勤、设备、成员、公告等管理与协同。
          </p>
          <div class="hero-actions">
            <router-link class="primary-btn" to="/login">登录进入平台</router-link>
            <router-link class="ghost-btn" to="/register">学生注册</router-link>
            <router-link class="ghost-btn" to="/teacher-register">教师注册</router-link>
          </div>
        </div>

        <div class="hero-features">
          <div v-for="item in platformFeatures" :key="item.title" class="feature-card">
            <span class="feature-icon"><el-icon :size="18"><component :is="item.icon" /></el-icon></span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.text }}</p>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <div class="panel-eyebrow">学院与实验室</div>
            <div class="panel-title">按学院筛选实验室</div>
          </div>
          <div class="panel-stats">
            <div class="stat">
              <div class="stat-label">学院数</div>
              <div class="stat-value">{{ colleges.length }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">实验室数</div>
              <div class="stat-value">{{ labCountMap.all ?? 0 }}</div>
            </div>
          </div>
        </div>

        <div class="filters">
          <el-select v-model="filters.collegeId" clearable placeholder="全部学院" class="filter-item">
            <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
          </el-select>
          <el-input v-model="filters.keyword" clearable placeholder="搜索实验室名称" class="filter-item" @keyup.enter="loadLabs" />
          <el-button type="primary" :loading="loading" @click="loadLabs">查询</el-button>
        </div>

        <div class="college-strip">
          <button
            class="college-pill"
            :class="{ active: !filters.collegeId }"
            type="button"
            @click="selectCollege(null)"
          >
            全部
            <span class="pill-meta">{{ labCountMap.all ?? '-' }}</span>
          </button>
          <button
            v-for="college in colleges"
            :key="college.id"
            class="college-pill"
            :class="{ active: filters.collegeId === college.id }"
            type="button"
            @click="selectCollege(college.id)"
          >
            {{ college.collegeName }}
            <span class="pill-meta">{{ labCountMap[college.id] ?? '-' }}</span>
          </button>
        </div>
      </section>

      <section class="labs-section">
        <div class="section-title-row">
          <div class="section-title">
            <span class="section-tag">实验室列表</span>
            <span class="section-hint">{{ selectedCollegeName }}</span>
          </div>
          <router-link class="ghost-link" to="/login">登录后管理考勤与设备</router-link>
        </div>

        <div v-loading="loading" class="labs-grid">
          <button
            v-for="lab in labs"
            :key="lab.id"
            class="lab-card"
            type="button"
            @click="openLabDetail(lab)"
          >
            <div class="lab-header">
              <div class="lab-name">{{ lab.labName }}</div>
              <div class="lab-meta">{{ lab.labCode || `LAB-${lab.id}` }}</div>
            </div>
            <div class="lab-desc">{{ lab.labDesc || '暂无简介' }}</div>
            <div class="lab-badges">
              <span v-if="lab.location" class="badge">{{ lab.location }}</span>
              <span v-if="lab.teacherName" class="badge">导师：{{ lab.teacherName }}</span>
              <span v-if="lab.requireSkill" class="badge">要求：{{ lab.requireSkill }}</span>
            </div>
            <div class="lab-footer">
              <span>成员 {{ lab.currentNum ?? 0 }} / {{ lab.recruitNum ?? '-' }}</span>
              <span class="lab-action">查看详情</span>
            </div>
          </button>
        </div>

        <div class="pagination-row">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { DocumentChecked, OfficeBuilding, TrendCharts, UserFilled } from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage, getLabStats } from '@/api/lab'

const platformFeatures = [
  { title: '多角色协同', text: '学校、学院、实验室、教师与学生分层治理，权限按角色下发。', icon: UserFilled },
  { title: '实验室档案', text: '实验室基础信息、招新计划与成员状态集中沉淀，随时可查。', icon: OfficeBuilding },
  { title: '审批闭环', text: '教师注册、实验室创建与成员申请全部走审批流程，状态可追踪。', icon: DocumentChecked },
  { title: '运营数据', text: '考勤、资料空间与公告统一汇总，支持综合检索与统计分析。', icon: TrendCharts }
]

const router = useRouter()
const colleges = ref([])
const labs = ref([])
const loading = ref(false)

const pagination = reactive({
  pageNum: 1,
  pageSize: 12,
  total: 0
})

const filters = reactive({
  collegeId: null,
  keyword: ''
})

const labCountMap = reactive({
  all: null
})

const selectedCollegeName = computed(() => {
  if (!filters.collegeId) {
    return '全部学院'
  }
  const match = colleges.value.find((item) => item.id === filters.collegeId)
  return match ? match.collegeName : '学院'
})

const loadColleges = async () => {
  const response = await getCollegeOptions()
  colleges.value = response.data || []
}

const loadLabs = async () => {
  loading.value = true
  try {
    const response = await getLabPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      collegeId: filters.collegeId || undefined,
      labName: filters.keyword || undefined
    })
    labs.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadCollegeStats = async () => {
  const res = await getLabStats()
  const total = res.data?.total
  labCountMap.all = typeof total === 'number' ? total : 0
  ;(res.data?.byCollege || []).forEach((row) => {
    const collegeId = row.collegeId
    if (collegeId == null) {
      return
    }
    labCountMap[collegeId] = Number(row.labCount) || 0
  })
  colleges.value.forEach((college) => {
    if (college?.id == null) {
      return
    }
    if (labCountMap[college.id] == null) {
      labCountMap[college.id] = 0
    }
  })
}

const refreshIntroData = async ({ includeColleges = false } = {}) => {
  if (includeColleges || !colleges.value.length) {
    await loadColleges()
  }
  await loadCollegeStats()
  await loadLabs()
}

const selectCollege = (collegeId) => {
  filters.collegeId = collegeId
  pagination.pageNum = 1
  loadLabs()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadLabs()
}

const openLabDetail = (lab) => {
  if (!lab?.id) {
    return
  }
  router.push(`/lab-info/${lab.id}`)
}

const handleWindowFocus = () => {
  refreshIntroData()
}

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    refreshIntroData()
  }
}

onMounted(async () => {
  await refreshIntroData({ includeColleges: true })
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.portal-page {
  min-height: 100vh;
  background: var(--ll-bg-body);
  color: var(--ll-text-primary);
}

/* ---------- 顶部导航 ---------- */
.portal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ll-space-4);
  height: 64px;
  padding: 0 var(--ll-space-8);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ll-border-subtle);
  position: sticky;
  top: 0;
  z-index: var(--ll-z-sticky);
}

.portal-nav-links {
  display: flex;
  align-items: center;
  gap: var(--ll-space-5);
  font-size: var(--ll-text-sm);
}

.portal-nav-links a {
  color: var(--ll-text-secondary);
  transition: color var(--ll-transition-fast);
}

.portal-nav-links a:hover {
  color: var(--ll-text-primary);
}

.portal-nav-links .nav-cta {
  padding: 6px 16px;
  border-radius: var(--ll-radius-md);
  background: var(--ll-brand-600);
  color: var(--ll-text-inverse);
  font-weight: 500;
}

.portal-nav-links .nav-cta:hover {
  background: var(--ll-brand-700);
  color: var(--ll-text-inverse);
}

/* ---------- 主内容 ---------- */
.main-content {
  max-width: 1360px;
  margin: 0 auto;
  padding: var(--ll-space-6) var(--ll-space-8) var(--ll-space-12);
  display: flex;
  flex-direction: column;
  gap: var(--ll-space-8);
}

/* ---------- 首屏 ---------- */
.hero {
  display: grid;
  gap: var(--ll-space-6);
  padding: var(--ll-space-6) 0 var(--ll-space-2);
}

.hero-eyebrow {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--ll-radius-full);
  background: var(--ll-brand-50);
  color: var(--ll-brand-700);
  font-size: var(--ll-text-xs);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.hero h1 {
  margin-top: var(--ll-space-3);
  font-size: 36px;
  font-weight: 600;
  line-height: 1.3;
}

.hero p {
  margin-top: var(--ll-space-3);
  max-width: 640px;
  font-size: var(--ll-text-base);
  line-height: 1.7;
  color: var(--ll-text-secondary);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ll-space-3);
  margin-top: var(--ll-space-4);
}

.primary-btn,
.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 var(--ll-space-6);
  border-radius: var(--ll-radius-md);
  font-size: var(--ll-text-base);
  font-weight: 500;
  transition: background-color var(--ll-transition-fast), border-color var(--ll-transition-fast),
    color var(--ll-transition-fast), box-shadow var(--ll-transition-fast);
}

.primary-btn {
  background: var(--ll-brand-600);
  color: var(--ll-text-inverse);
  box-shadow: var(--ll-shadow-sm);
}

.primary-btn:hover {
  background: var(--ll-brand-700);
  color: var(--ll-text-inverse);
}

.ghost-btn {
  border: 1px solid var(--ll-border-default);
  color: var(--ll-text-primary);
  background: var(--ll-bg-surface);
}

.ghost-btn:hover {
  border-color: var(--ll-brand-300);
  color: var(--ll-brand-700);
  background: var(--ll-brand-50);
}

/* ---------- 特性网格 ---------- */
.hero-features {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid var(--ll-border-subtle);
  border-bottom: 1px solid var(--ll-border-subtle);
}

.feature-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-content: center;
  align-items: center;
  column-gap: var(--ll-space-3);
  row-gap: 2px;
  min-height: 76px;
  padding: var(--ll-space-3) var(--ll-space-4);
  border-right: 1px solid var(--ll-border-subtle);
}

.feature-card:last-child {
  border-right: 0;
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  grid-row: span 2;
  border-radius: var(--ll-radius-sm);
  background: var(--ll-brand-50);
  color: var(--ll-brand-600);
  margin-bottom: var(--ll-space-1);
}

.feature-card strong {
  font-size: var(--ll-text-base);
  font-weight: 600;
}

.feature-card p {
  font-size: var(--ll-text-sm);
  line-height: 1.45;
  color: var(--ll-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ---------- 筛选面板 ---------- */
.panel {
  background: var(--ll-bg-surface);
  border: 1px solid var(--ll-border-subtle);
  border-radius: var(--ll-radius-md);
  box-shadow: none;
  padding: var(--ll-space-5) var(--ll-space-6);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ll-space-4);
  flex-wrap: wrap;
}

.panel-eyebrow {
  font-size: var(--ll-text-xs);
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ll-text-tertiary);
}

.panel-title {
  margin-top: 4px;
  font-size: var(--ll-text-xl);
  font-weight: 600;
  letter-spacing: -0.01em;
}

.panel-stats {
  display: flex;
  gap: var(--ll-space-6);
}

.stat-label {
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
}

.stat-value {
  margin-top: 2px;
  font-size: var(--ll-text-2xl);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ll-text-primary);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ll-space-3);
  margin-top: var(--ll-space-5);
}

.filter-item {
  width: 220px;
}

.college-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ll-space-2);
  margin-top: var(--ll-space-5);
  padding-top: var(--ll-space-5);
  border-top: 1px solid var(--ll-border-subtle);
}

.college-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: var(--ll-radius-full);
  border: 1px solid var(--ll-border-subtle);
  background: var(--ll-bg-surface);
  color: var(--ll-text-secondary);
  font-size: var(--ll-text-sm);
  transition: all var(--ll-transition-fast);
}

.college-pill:hover {
  border-color: var(--ll-brand-300);
  color: var(--ll-brand-700);
}

.college-pill.active {
  background: var(--ll-brand-600);
  border-color: var(--ll-brand-600);
  color: var(--ll-text-inverse);
}

.pill-meta {
  font-size: var(--ll-text-xs);
  opacity: 0.7;
  font-variant-numeric: tabular-nums;
}

/* ---------- 实验室列表 ---------- */
.labs-section {
  display: grid;
  gap: var(--ll-space-4);
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ll-space-4);
  flex-wrap: wrap;
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: var(--ll-space-3);
}

.section-tag {
  font-size: var(--ll-text-lg);
  font-weight: 600;
}

.section-hint {
  font-size: var(--ll-text-sm);
  color: var(--ll-text-tertiary);
}

.ghost-link {
  font-size: var(--ll-text-sm);
  color: var(--ll-text-secondary);
}

.ghost-link:hover {
  color: var(--ll-text-brand);
}

.labs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--ll-space-3);
}

.lab-card {
  display: grid;
  gap: var(--ll-space-3);
  align-content: start;
  text-align: left;
  padding: var(--ll-space-5);
  border-radius: var(--ll-radius-md);
  background: var(--ll-bg-surface);
  border: 1px solid var(--ll-border-subtle);
  box-shadow: none;
  transition: border-color var(--ll-transition-base), background-color var(--ll-transition-base),
    transform var(--ll-transition-base);
}

.lab-card:hover {
  border-color: var(--ll-brand-200);
  background: var(--ll-gray-25);
  transform: translateY(-1px);
}

.lab-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ll-space-3);
}

.lab-name {
  font-size: var(--ll-text-lg);
  font-weight: 600;
  color: var(--ll-text-primary);
}

.lab-meta {
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.lab-desc {
  font-size: var(--ll-text-sm);
  line-height: 1.65;
  color: var(--ll-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lab-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.badge {
  padding: 2px 8px;
  border-radius: var(--ll-radius-xs);
  background: var(--ll-gray-100);
  color: var(--ll-text-secondary);
  font-size: var(--ll-text-xs);
}

.lab-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--ll-space-3);
  border-top: 1px solid var(--ll-border-subtle);
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
}

.lab-action {
  color: var(--ll-text-brand);
  font-weight: 500;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: var(--ll-space-4);
}

@media (max-width: 768px) {
  .portal-nav {
    padding: 0 var(--ll-space-4);
  }

  .portal-nav-links {
    gap: var(--ll-space-3);
  }

  .main-content {
    padding: var(--ll-space-4) var(--ll-space-4) var(--ll-space-10);
    gap: var(--ll-space-6);
  }

  .hero h1 {
    font-size: 26px;
  }

  .hero p {
    font-size: var(--ll-text-base);
  }

  .hero-actions .primary-btn,
  .hero-actions .ghost-btn {
    flex: 1 1 100%;
  }

  .filter-item {
    width: 100%;
  }

  .hero-features {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-card:nth-child(2) {
    border-right: 0;
  }

  .feature-card:nth-child(n + 3) {
    border-top: 1px solid var(--ll-border-subtle);
  }

  .labs-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: var(--ll-space-4);
  }
}
</style>

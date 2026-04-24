<template>
  <div class="portal-page">
    <div class="bg-grid"></div>
    <div class="ambient-glow"></div>

    <header class="hero">
      <div class="hero-content">
        <BrandLogo size="lg" title="LabLink" subtitle="高校实验室综合管理平台" />
        <h1>实验室发现与学院治理</h1>
        <p>从学院主数据到实验室档案，一站式浏览实验室与学院信息。登录后进入考勤、设备、成员、公告等管理与协同。</p>
        <div class="hero-actions">
          <router-link class="primary-btn" to="/login">登录进入平台</router-link>
          <router-link class="ghost-btn" to="/register">学生注册</router-link>
          <router-link class="ghost-btn" to="/teacher-register">教师注册</router-link>
        </div>
      </div>
    </header>

    <main class="main-content">
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
            v-for="(lab, index) in labs"
            :key="lab.id"
            class="lab-card"
            type="button"
            :style="{ transitionDelay: `${index * 0.06}s` }"
            @click="openLabDetail(lab)"
          >
            <div class="lab-logo">
              <img v-if="lab.logoUrl" :src="resolveMedia(lab.logoUrl)" alt="实验室 Logo" />
              <span v-else>{{ (lab.labName || 'L').charAt(0) }}</span>
            </div>
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
import BrandLogo from '@/components/BrandLogo.vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage, getLabStats } from '@/api/lab'
import { resolveFileUrl } from '@/utils/file'

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
const resolveMedia = (value) => resolveFileUrl(value)

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
  --bg: #f8fafc;
  --panel: #ffffff;
  --line: #e2e8f0;
  --text: #0f172a;
  --muted: #64748b;
  --brand: #0ea5e9;
  --brand-2: #2563eb;
  --shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.1);
  --radius: 20px;
  min-height: 100vh;
  background-color: var(--bg);
  color: var(--text);
  position: relative;
  overflow-x: hidden;
  padding-bottom: 80px;
}

.bg-grid {
  position: fixed;
  inset: 0;
  pointer-events: none;
  opacity: 0.6;
  z-index: 0;
  background-image:
    linear-gradient(#e2e8f0 1px, transparent 1px),
    linear-gradient(90deg, #e2e8f0 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0));
}

.ambient-glow {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(14, 165, 233, 0.12) 0%, transparent 45%),
    radial-gradient(circle at 80% 60%, rgba(37, 99, 235, 0.12) 0%, transparent 45%);
}

.hero {
  padding: 72px 24px 40px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.hero-content {
  max-width: 900px;
  margin: 0 auto;
}

.hero h1 {
  margin: 22px 0 12px;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 1.05;
  letter-spacing: -1px;
}

.hero p {
  margin: 0 auto;
  max-width: 720px;
  color: var(--muted);
  line-height: 1.7;
  font-size: 16px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 26px;
}

.primary-btn,
.ghost-btn,
.ghost-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.primary-btn {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-2), var(--brand));
}

.ghost-btn,
.ghost-link {
  border: 1px solid var(--line);
  color: var(--text);
  background: #fff;
}

.primary-btn:hover,
.ghost-btn:hover,
.ghost-link:hover {
  transform: translateY(-2px);
  border-color: rgba(14, 165, 233, 0.5);
}

.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

.panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.panel-eyebrow {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #0f766e;
  margin-bottom: 6px;
}

.panel-title {
  font-size: 18px;
  font-weight: 800;
}

.panel-stats {
  display: flex;
  gap: 14px;
}

.stat {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 10px 14px;
  min-width: 110px;
}

.stat-label {
  font-size: 12px;
  color: var(--muted);
}

.stat-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
}

.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 16px;
}

.filter-item {
  min-width: 220px;
  flex: 1;
}

.college-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.college-pill {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--text);
  padding: 8px 12px;
  border-radius: 999px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.college-pill.active {
  border-color: rgba(14, 165, 233, 0.55);
  background: rgba(14, 165, 233, 0.08);
  color: #075985;
}

.pill-meta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  min-width: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(2, 132, 199, 0.12);
  color: #075985;
  font-size: 12px;
  font-weight: 800;
}

.labs-section {
  margin-top: 24px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin: 8px 0 16px;
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.section-tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #075985;
  font-size: 12px;
  font-weight: 800;
}

.section-hint {
  color: var(--muted);
  font-weight: 700;
}

.labs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.lab-card {
  text-align: left;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow);
  padding: 18px 18px 16px;
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.lab-logo {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 14px;
  background: linear-gradient(135deg, #0f172a, #0f766e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
}

.lab-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lab-card:hover {
  transform: translateY(-6px);
  border-color: rgba(14, 165, 233, 0.55);
  box-shadow: 0 20px 40px -10px rgba(14, 165, 233, 0.16);
}

.lab-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.lab-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
}

.lab-meta {
  font-size: 12px;
  font-weight: 800;
  color: #0f766e;
}

.lab-desc {
  color: var(--muted);
  line-height: 1.65;
  font-size: 14px;
  min-height: 46px;
}

.lab-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.badge {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.28);
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.lab-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  color: var(--muted);
  font-weight: 700;
  font-size: 13px;
}

.lab-action {
  color: #0284c7;
  font-weight: 800;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.lab-detail {
  display: grid;
  gap: 12px;
}

.detail-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px;
  align-items: baseline;
}

.detail-label {
  color: var(--muted);
  font-weight: 800;
  font-size: 13px;
}

.detail-value {
  color: var(--text);
  line-height: 1.7;
}

@media (max-width: 680px) {
  .filter-item {
    min-width: 100%;
  }

  .detail-row {
    grid-template-columns: 1fr;
  }
}
</style>

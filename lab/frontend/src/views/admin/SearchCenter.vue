<template>
  <div class="page-shell">
    <section class="page-hero search-hero">
      <div>
        <p class="eyebrow">综合检索</p>
        <h1>统一入口检索学院、实验室、学生与公告</h1>
        <p class="hero-subtitle">
          面向管理信息系统类作品，强化“快速查询、多维检索、结果聚合”的展示能力，
          让学校管理员和学院管理员能够在一个页面里完成高频信息定位。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-chip">统一检索入口</div>
        <div class="hero-chip muted">学院 / 实验室 / 学生 / 公告</div>
      </div>
    </section>

    <el-card shadow="never" class="panel-card search-panel">
      <div class="search-row">
        <el-input
          v-model="keyword"
          clearable
          size="large"
          placeholder="输入学院名、实验室名、学生姓名、学号或公告关键词"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" :loading="loading" @click="handleSearch">
          开始检索
        </el-button>
        <el-button size="large" :loading="exportLoading" :disabled="!keyword.trim()" @click="handleExport">
          导出结果
        </el-button>
        <el-button size="large" @click="resetSearch">清空</el-button>
      </div>
    </el-card>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <section class="results-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>学院结果</span>
            <el-tag effect="plain">{{ totals.colleges }}</el-tag>
          </div>
        </template>
        <div v-if="results.colleges.length" class="result-list">
          <article v-for="college in results.colleges" :key="college.id" class="result-item">
            <strong>{{ college.collegeName }}</strong>
            <p>{{ college.collegeCode || '未设置编码' }}</p>
          </article>
        </div>
        <el-empty v-else :description="emptyDescription('学院')" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>实验室结果</span>
            <el-tag effect="plain">{{ totals.labs }}</el-tag>
          </div>
        </template>
        <div v-if="results.labs.length" class="result-list">
          <article v-for="lab in results.labs" :key="lab.id" class="result-item">
            <strong>{{ lab.labName }}</strong>
            <p>{{ lab.teacherName || '待维护' }} · {{ lab.location || '未维护地点' }}</p>
          </article>
        </div>
        <el-empty v-else :description="emptyDescription('实验室')" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>学生结果</span>
            <el-tag effect="plain">{{ totals.students }}</el-tag>
          </div>
        </template>
        <div v-if="results.students.length" class="result-list">
          <article v-for="student in results.students" :key="student.id" class="result-item">
            <strong>{{ student.realName }}</strong>
            <p>{{ student.studentId || '无学号' }} · {{ student.major || '未填写专业' }}</p>
          </article>
        </div>
        <el-empty v-else :description="emptyDescription('学生')" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>公告结果</span>
            <el-tag effect="plain">{{ totals.notices }}</el-tag>
          </div>
        </template>
        <div v-if="results.notices.length" class="result-list">
          <article v-for="notice in results.notices" :key="notice.id" class="result-item">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.publishScopeLabel || notice.publishScope || '未设置范围' }}</p>
          </article>
        </div>
        <el-empty v-else :description="emptyDescription('公告')" />
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAdminStudentPage } from '@/api/admin'
import { getCollegePage } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import { getNoticePage } from '@/api/notices'
import { downloadCsv } from '@/utils/export'

const keyword = ref('')
const loading = ref(false)
const exportLoading = ref(false)
const hasSearched = ref(false)

const results = reactive({
  colleges: [],
  labs: [],
  students: [],
  notices: []
})

const totals = reactive({
  colleges: 0,
  labs: 0,
  students: 0,
  notices: 0
})

const metricCards = computed(() => [
  { label: '学院命中', value: totals.colleges, tip: '按学院名称与编码检索' },
  { label: '实验室命中', value: totals.labs, tip: '按名称、地点与指导教师检索' },
  { label: '学生命中', value: totals.students, tip: '按姓名、学号和专业检索' },
  { label: '公告命中', value: totals.notices, tip: '按标题和内容关键字检索' }
])

const fillResults = (target, source = []) => {
  target.splice(0, target.length, ...source)
}

const fetchSearchResults = (normalizedKeyword, pageSize) =>
  Promise.all([
    getCollegePage({ pageNum: 1, pageSize, keyword: normalizedKeyword }),
    getLabPage({ pageNum: 1, pageSize, labName: normalizedKeyword }),
    getAdminStudentPage({ pageNum: 1, pageSize, keyword: normalizedKeyword }),
    getNoticePage({ pageNum: 1, pageSize, keyword: normalizedKeyword })
  ])

const applySearchResults = ([collegeRes, labRes, studentRes, noticeRes]) => {
  fillResults(results.colleges, collegeRes.data?.records || [])
  fillResults(results.labs, labRes.data?.records || [])
  fillResults(results.students, studentRes.data?.records || [])
  fillResults(results.notices, noticeRes.data?.records || [])

  totals.colleges = collegeRes.data?.total || 0
  totals.labs = labRes.data?.total || 0
  totals.students = studentRes.data?.total || 0
  totals.notices = noticeRes.data?.total || 0
}

const handleSearch = async () => {
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    ElMessage.warning('请输入检索关键词')
    return
  }

  loading.value = true
  hasSearched.value = true

  try {
    const responseSet = await fetchSearchResults(normalizedKeyword, 6)
    applySearchResults(responseSet)
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    ElMessage.warning('请输入检索关键词')
    return
  }

  exportLoading.value = true
  try {
    const [collegeRes, labRes, studentRes, noticeRes] = await fetchSearchResults(normalizedKeyword, 50)
    const rows = [
      ['检索关键词', normalizedKeyword],
      ['导出时间', new Date().toLocaleString()],
      ['学院命中', collegeRes.data?.total || 0],
      ['实验室命中', labRes.data?.total || 0],
      ['学生命中', studentRes.data?.total || 0],
      ['公告命中', noticeRes.data?.total || 0],
      [],
      ['学院结果'],
      ['学院名称', '学院编码']
    ]

    for (const item of collegeRes.data?.records || []) {
      rows.push([item.collegeName || '-', item.collegeCode || '-'])
    }

    rows.push([], ['实验室结果'], ['实验室名称', '指导老师', '地点'])
    for (const item of labRes.data?.records || []) {
      rows.push([item.labName || '-', item.teacherName || '-', item.location || '-'])
    }

    rows.push([], ['学生结果'], ['姓名', '学号', '学院', '专业'])
    for (const item of studentRes.data?.records || []) {
      rows.push([item.realName || '-', item.studentId || '-', item.college || '-', item.major || '-'])
    }

    rows.push([], ['公告结果'], ['标题', '发布范围', '内容摘要'])
    for (const item of noticeRes.data?.records || []) {
      rows.push([
        item.title || '-',
        item.publishScopeLabel || item.publishScope || '-',
        item.content || '-'
      ])
    }

    downloadCsv(`aiit-search-${normalizedKeyword}-${Date.now()}.csv`, rows)
    ElMessage.success('检索结果已导出')
  } finally {
    exportLoading.value = false
  }
}

const resetSearch = () => {
  keyword.value = ''
  hasSearched.value = false
  fillResults(results.colleges)
  fillResults(results.labs)
  fillResults(results.students)
  fillResults(results.notices)
  totals.colleges = 0
  totals.labs = 0
  totals.students = 0
  totals.notices = 0
}

const emptyDescription = (label) => {
  if (!hasSearched.value) {
    return `输入关键词后开始检索${label}`
  }
  return `未检索到相关${label}`
}
</script>

<style scoped>
.search-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(2, 132, 199, 0.88)),
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.18), transparent 32%);
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
  color: rgba(240, 249, 255, 0.88);
}

.search-panel {
  margin-bottom: 18px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 12px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.result-list {
  display: grid;
  gap: 12px;
}

.result-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(240, 249, 255, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.result-item p {
  margin: 0;
  color: #64748b;
}

@media (max-width: 900px) {
  .results-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .search-row {
    grid-template-columns: 1fr;
  }

  .hero-side {
    justify-items: start;
  }
}
</style>

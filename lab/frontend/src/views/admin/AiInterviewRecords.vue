<template>
  <div class="interview-records">
    <div class="page-top">
      <div>
        <h1>AI 面试记录</h1>
        <p class="subtitle">查看学生正式 AI 面试的评分、评价和详细对话记录。</p>
      </div>
    </div>

    <div class="mobile-filter-shell">
      <div class="mobile-filter-shell__header">
        <div>
          <strong>筛选条件</strong>
          <div class="mobile-filter-shell__summary">{{ filterSummary }}</div>
        </div>
        <button class="mobile-filter-shell__toggle" type="button" @click="filtersExpanded = !filtersExpanded">
          {{ filtersExpanded ? '收起筛选' : '展开筛选' }}
        </button>
      </div>
      <div class="filter-bar" :hidden="isPhoneWide && !filtersExpanded">
        <el-input v-model="filters.studentName" placeholder="学生姓名/学号" clearable style="width: 170px" />
        <el-select v-model="filters.moduleId" placeholder="面试模块" clearable style="width: 160px">
          <el-option v-for="m in moduleOptions" :key="m.id" :label="m.moduleName" :value="m.id" />
        </el-select>
        <el-select v-model="filters.scoreRange" placeholder="分数区间" clearable style="width: 140px">
          <el-option label="90-100 优秀" value="90-100" />
          <el-option label="80-89 良好" value="80-89" />
          <el-option label="60-79 合格" value="60-79" />
          <el-option label="0-59 需加强" value="0-59" />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="desktop-mobile-card-list mobile-only">
      <article v-for="row in records" :key="row.id" class="desktop-mobile-card">
        <div class="desktop-mobile-card__header">
          <div class="desktop-mobile-card__title-wrap">
            <strong class="desktop-mobile-card__title">{{ row.studentName || '未命名学生' }}</strong>
            <span class="desktop-mobile-card__subtitle">{{ row.studentNo || '学号未登记' }} · {{ row.college || '学院未登记' }}</span>
          </div>
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </div>

        <div class="desktop-mobile-card__grid">
          <div class="desktop-mobile-card__line">
            <span class="desktop-mobile-card__line-label">实验室</span>
            <span class="desktop-mobile-card__line-value">{{ row.labName || '-' }}</span>
          </div>
          <div class="desktop-mobile-card__line">
            <span class="desktop-mobile-card__line-label">模块 / 次数</span>
            <span class="desktop-mobile-card__line-value">{{ row.moduleName || '-' }} · 第{{ row.attemptNo || 0 }}次</span>
          </div>
          <div class="desktop-mobile-card__line">
            <span class="desktop-mobile-card__line-label">评分 / 推荐</span>
            <span class="desktop-mobile-card__line-value">
              <span class="score-cell" :class="scoreClass(row.score)">{{ row.score }}</span>
              <el-tag :type="row.recommendNext ? 'success' : 'info'" size="small" style="margin-left:8px">
                {{ row.recommendNext ? '推荐' : '待评估' }}
              </el-tag>
            </span>
          </div>
          <div class="desktop-mobile-card__line">
            <span class="desktop-mobile-card__line-label">开始时间</span>
            <span class="desktop-mobile-card__line-value">{{ row.startTime }}</span>
          </div>
          <div class="desktop-mobile-card__line">
            <span class="desktop-mobile-card__line-label">标签</span>
            <div class="desktop-mobile-card__badge-row">
              <el-tag v-for="(t, i) in parseTags(row.tagsJson)" :key="i" size="small">{{ t }}</el-tag>
            </div>
          </div>
        </div>

        <div class="desktop-mobile-card__actions">
          <el-button type="primary" plain @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status !== '作废'" type="warning" plain @click="handleInvalidate(row)">作废</el-button>
        </div>
      </article>
    </div>

    <el-table :data="records" stripe v-loading="loading" style="width: 100%" class="desktop-only">
      <el-table-column prop="studentName" label="学生" width="100" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="college" label="学院" min-width="130" show-overflow-tooltip />
      <el-table-column prop="labName" label="申请实验室" min-width="150" show-overflow-tooltip />
      <el-table-column prop="moduleName" label="模块" width="130" />
      <el-table-column prop="attemptNo" label="第几次" width="80" align="center">
        <template #default="{ row }"><el-tag size="small">第{{ row.attemptNo }}次</el-tag></template>
      </el-table-column>
      <el-table-column label="评分" width="90" align="center">
        <template #default="{ row }">
          <span class="score-cell" :class="scoreClass(row.score)">{{ row.score }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="tagsJson" label="标签" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="(t, i) in parseTags(row.tagsJson)" :key="i" size="small" style="margin-right:4px">{{ t }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推荐" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.recommendNext ? 'success' : 'info'" size="small">{{ row.recommendNext ? '推荐' : '待评估' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button link type="warning" size="small" @click="handleInvalidate(row)" v-if="row.status !== '作废'">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :layout="paginationLayout"
      style="margin-top: 16px; justify-content: flex-end;"
      @current-change="loadRecords"
      @size-change="loadRecords"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminModules, getAdminRecords, invalidateRecord } from '@/api/aiInterview'
import { useViewport } from '@/composables/useViewport'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const records = ref([])
const moduleOptions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filtersExpanded = ref(typeof window === 'undefined' ? true : window.innerWidth > 768)
const filters = ref({ studentName: '', moduleId: '', scoreRange: '', dateRange: null })
const { isPhoneWide } = useViewport()

const scoreClass = (s) => (s >= 80 ? 'high' : s >= 60 ? 'mid' : 'low')
const statusType = (s) => (s === '已完成' ? 'success' : s === '作废' ? 'danger' : 'info')
const parseTags = (json) => {
  try {
    return JSON.parse(json)
  } catch {
    return []
  }
}

const filterSummary = computed(() => {
  const parts = []
  if (filters.value.studentName.trim()) parts.push(filters.value.studentName.trim())
  if (filters.value.moduleId) {
    const moduleName = moduleOptions.value.find((item) => Number(item.id) === Number(filters.value.moduleId))?.moduleName
    parts.push(moduleName || '面试模块')
  }
  if (filters.value.scoreRange) parts.push(filters.value.scoreRange)
  if (filters.value.dateRange?.length) parts.push(filters.value.dateRange.join(' 至 '))
  return parts.length ? parts.join(' / ') : '全部记录'
})

const paginationLayout = computed(() => (isPhoneWide.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'))
const detailBasePath = computed(() => {
  if (route.path.startsWith('/m/teacher/')) {
    return '/m/teacher/ai-interview-records'
  }
  if (route.path.startsWith('/m/admin/')) {
    return '/m/admin/ai-interview-records'
  }
  if (route.path.startsWith('/teacher/')) {
    return '/teacher/ai-interview-records'
  }
  return '/admin/ai-interview-records'
})

onMounted(async () => {
  try {
    const r = await getAdminModules()
    moduleOptions.value = r?.data || []
  } catch {}
  await loadRecords()
})

async function loadRecords() {
  loading.value = true
  try {
    const [startDate, endDate] = filters.value.dateRange || []
    const r = await getAdminRecords({
      studentName: filters.value.studentName || undefined,
      moduleId: filters.value.moduleId || undefined,
      scoreRange: filters.value.scoreRange || undefined,
      startDate,
      endDate,
      page: page.value,
      pageSize: pageSize.value
    })
    records.value = r?.data?.records || r?.data?.list || []
    total.value = r?.data?.total || 0
  } catch {}
  loading.value = false
}

function handleSearch() {
  page.value = 1
  loadRecords()
}

function viewDetail(row) {
  router.push(`${detailBasePath.value}/${row.id}`)
}

async function handleInvalidate(row) {
  try {
    await ElMessageBox.confirm(`确认作废该条记录，学生：${row.studentName}`, '作废确认', { type: 'warning' })
    await invalidateRecord(row.id)
    ElMessage.success('已作废')
    await loadRecords()
  } catch {}
}
</script>

<style scoped>
.interview-records { padding: 0 4px; }
.page-top { margin-bottom: 20px; }
.page-top h1 { font-size: 20px; font-weight: 700; color: #0f172a; margin: 0 0 4px; }
.subtitle { font-size: 13px; color: #64748b; margin: 0; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; align-items: center; }
.score-cell { font-weight: 700; font-size: 15px; }
.score-cell.high { color: #10b981; }
.score-cell.mid { color: #f59e0b; }
.score-cell.low { color: #ef4444; }
</style>

<template>
  <div class="interview-records">
    <div class="page-top">
      <div>
        <h1>AI 面试记录</h1>
        <p class="subtitle">查看学生正式 AI 面试的评分、评价和详细对话记录。</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-input v-model="filters.studentName" placeholder="学生姓名" clearable style="width: 160px" />
      <el-select v-model="filters.moduleId" placeholder="面试模块" clearable style="width: 160px">
        <el-option v-for="m in moduleOptions" :key="m.id" :label="m.moduleName" :value="m.id" />
      </el-select>
      <el-select v-model="filters.scoreRange" placeholder="分数区间" clearable style="width: 140px">
        <el-option label="90-100 优秀" value="90-100" />
        <el-option label="80-89 良好" value="80-89" />
        <el-option label="60-79 合格" value="60-79" />
        <el-option label="0-59 需加强" value="0-59" />
      </el-select>
      <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
      <el-button type="primary" @click="loadRecords">查询</el-button>
    </div>

    <!-- Table -->
    <el-table :data="records" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="studentName" label="学生" width="100" />
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
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button link type="warning" size="small" @click="handleInvalidate(row)" v-if="row.status !== '作废'">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      layout="total, prev, pager, next" style="margin-top: 16px; justify-content: flex-end;" @current-change="loadRecords" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminRecords, getInterviewModules, invalidateRecord } from '@/api/aiInterview'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const moduleOptions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filters = ref({ studentName: '', moduleId: '', scoreRange: '', dateRange: null })

const scoreClass = (s) => s >= 80 ? 'high' : s >= 60 ? 'mid' : 'low'
const statusType = (s) => s === '已完成' ? 'success' : s === '作废' ? 'danger' : 'info'
const parseTags = (json) => { try { return JSON.parse(json) } catch { return [] } }

onMounted(async () => {
  try { const r = await getInterviewModules(); moduleOptions.value = r?.data || [] } catch {}
  await loadRecords()
})

async function loadRecords() {
  loading.value = true
  try {
    const r = await getAdminRecords({ ...filters.value, page: page.value, pageSize: pageSize.value })
    records.value = r?.data?.records || r?.data?.list || []
    total.value = r?.data?.total || 0
  } catch {}
  loading.value = false
}

function viewDetail(row) { router.push(`/admin/ai-interview-records/${row.id}`) }

async function handleInvalidate(row) {
  try {
    await ElMessageBox.confirm(`确定作废该条记录？学生：${row.studentName}`, '作废确认', { type: 'warning' })
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

<template>
  <div class="audit-page">
    <section class="page-hero audit-hero">
      <div class="hero-header">
        <div>
          <div class="eyebrow">审计追踪</div>
          <h2>操作日志</h2>
          <p class="hero-text">查看是谁在什么范围内，通过哪个请求路径执行了什么操作。</p>
        </div>
        <el-button type="primary" @click="refresh">刷新</el-button>
      </div>
    </section>

    <TablePageCard title="操作日志" subtitle="审计列表" :count-label="`${pagination.total} 条记录`">
      <template #filters>
        <SearchToolbar
          v-model="filters.keyword"
          placeholder="按操作人、动作、路径、详情搜索"
          @search="handleSearch"
          @reset="resetFilters"
        >
          <el-form-item label="动作">
            <el-input v-model="filters.action" clearable placeholder="可选的动作编码" />
          </el-form-item>
          <el-form-item label="对象">
            <el-input v-model="filters.targetType" clearable placeholder="可选的目标类型" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="filters.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="filters.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </SearchToolbar>
      </template>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="160">
          <template #default="{ row }">
            <div class="actor-cell">
              <strong>{{ row.actorRealName || row.actorUsername || '未知用户' }}</strong>
              <span>{{ row.actorUsername || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operatorRole" label="角色" min-width="120" />
        <el-table-column prop="action" label="动作" min-width="180" />
        <el-table-column prop="targetType" label="目标类型" min-width="140" />
        <el-table-column prop="targetId" label="目标 ID" width="100" />
        <el-table-column label="范围" min-width="140">
          <template #default="{ row }">C{{ row.collegeId || '-' }} / L{{ row.labId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方法" width="100" />
        <el-table-column prop="requestPath" label="路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="IP" min-width="120" />
        <el-table-column prop="result" label="结果" width="100" />
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
      </el-table>
      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchRows"
          @current-change="fetchRows"
        />
      </template>
    </TablePageCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getAuditLogPage } from '@/api/audit'

const loading = ref(false)
const rows = ref([])

const filters = reactive({
  keyword: '',
  action: '',
  targetType: '',
  startTime: '',
  endTime: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchRows = async () => {
  loading.value = true
  try {
    const response = await getAuditLogPage({
      pageNum: pagination.current,
      pageSize: pagination.size,
      keyword: filters.keyword.trim() || undefined,
      action: filters.action.trim() || undefined,
      targetType: filters.targetType.trim() || undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined
    })
    rows.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchRows()
}

const resetFilters = () => {
  Object.assign(filters, { keyword: '', action: '', targetType: '', startTime: '', endTime: '' })
  pagination.current = 1
  fetchRows()
}

const refresh = () => {
  fetchRows()
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(
    2,
    '0'
  )} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  fetchRows()
})
</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 20px;
}

.audit-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.88)),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 35%);
}

.hero-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.hero-header h2 {
  margin: 6px 0 10px;
  font-size: 30px;
}

.hero-text {
  margin: 0;
  color: rgba(226, 232, 240, 0.86);
}

.actor-cell {
  display: grid;
  gap: 4px;
}

.actor-cell span {
  color: #64748b;
  font-size: 12px;
}
</style>

<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">我的申请</p>
          <h2>申请状态跟踪</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadApplies">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px">
            <el-option label="待审核" value="submitted" />
            <el-option label="初审通过" value="leader_approved" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadApplies">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <TablePageCard title="申请记录" subtitle="状态跟踪" :count-label="`${pagination.total} 条`">
      <div v-if="isMobile" v-loading="loading" class="apply-mobile-list">
        <template v-if="applies.length">
          <article v-for="row in applies" :key="row.id" class="apply-mobile-card">
            <div class="apply-mobile-head">
              <div class="apply-mobile-title-group">
                <strong>{{ row.labName || '未命名实验室' }}</strong>
                <span>{{ row.planTitle || '未命名招新计划' }}</span>
              </div>
              <StatusTag :value="row.status" preset="apply" />
            </div>

            <div class="apply-mobile-meta">
              <span>提交时间：{{ formatDateTime(row.createTime) }}</span>
            </div>

            <div class="apply-mobile-section">
              <label>申请理由</label>
              <p>{{ row.applyReason || '暂无申请理由' }}</p>
            </div>

            <div class="apply-mobile-section">
              <label>审核意见</label>
              <p>{{ row.auditComment || '暂未反馈审核意见' }}</p>
            </div>
          </article>
        </template>
        <el-empty v-else description="暂无申请记录" />
      </div>

      <el-table v-else v-loading="loading" :data="applies" stripe>
        <el-table-column prop="labName" label="实验室" min-width="160" />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <StatusTag :value="row.status" preset="apply" />
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请理由" min-width="240" show-overflow-tooltip />
        <el-table-column prop="auditComment" label="审核意见" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </template>
    </TablePageCard>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'

const loading = ref(false)
const applies = ref([])
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 768 : false)

const filters = reactive({
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const updateViewport = () => {
  isMobile.value = window.innerWidth <= 768
}

const loadApplies = async () => {
  loading.value = true
  try {
    const response = await getMyLabApplyPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: filters.status || undefined
    })
    applies.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadApplies()
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  loadApplies()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.apply-mobile-list {
  display: grid;
  gap: 14px;
}

.apply-mobile-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.apply-mobile-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.apply-mobile-title-group {
  display: grid;
  gap: 6px;
}

.apply-mobile-title-group strong {
  color: #0f172a;
  font-size: 16px;
}

.apply-mobile-title-group span,
.apply-mobile-meta,
.apply-mobile-section p {
  color: #475569;
  line-height: 1.7;
}

.apply-mobile-meta {
  font-size: 12px;
}

.apply-mobile-section {
  display: grid;
  gap: 6px;
}

.apply-mobile-section label {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .apply-mobile-head {
    flex-direction: column;
  }
}
</style>

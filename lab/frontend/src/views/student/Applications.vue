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

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="applies" stripe>
        <el-table-column prop="labName" label="实验室" min-width="160" />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请理由" min-width="240" show-overflow-tooltip />
        <el-table-column prop="auditComment" label="审核意见" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'

const loading = ref(false)
const applies = ref([])

const filters = reactive({
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

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

onMounted(() => {
  loadApplies()
})
</script>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

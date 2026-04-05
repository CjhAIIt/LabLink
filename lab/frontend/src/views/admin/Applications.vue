<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">申请审核</p>
          <h2>实验室入组申请闭环</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadApplications">刷新</el-button>
          <el-button type="primary" plain :loading="exporting" @click="exportApplicants">导出名单</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="学生姓名 / 学号 / 实验室" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px">
            <el-option label="待审核" value="submitted" />
            <el-option label="初审通过" value="leader_approved" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadApplications">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="applications" stripe>
        <el-table-column prop="studentName" label="学生" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="120" />
        <el-table-column prop="grade" label="班级" min-width="140" />
        <el-table-column prop="labName" label="实验室" min-width="150" />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请理由" min-width="220" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="240">
          <template #default="{ row }">
            <el-button v-if="row.status === 'submitted'" link type="primary" @click="handleAudit(row, 'leaderApprove')">
              初审通过
            </el-button>
            <el-button
              v-if="row.status === 'submitted' || row.status === 'leader_approved'"
              link
              type="success"
              @click="handleAudit(row, 'approve')"
            >
              终审通过
            </el-button>
            <el-button
              v-if="row.status !== 'approved' && row.status !== 'rejected'"
              link
              type="danger"
              @click="handleAudit(row, 'reject')"
            >
              驳回
            </el-button>
          </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { auditLabApply, getLabApplyPage } from '@/api/labApplies'
import { downloadCsv } from '@/utils/export'

const loading = ref(false)
const exporting = ref(false)
const applications = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  status: ''
})

const buildQuery = (extra = {}) => ({
  pageNum: pagination.pageNum,
  pageSize: pagination.pageSize,
  keyword: filters.keyword || undefined,
  status: filters.status || undefined,
  ...extra
})

const loadApplications = async () => {
  loading.value = true
  try {
    const response = await getLabApplyPage(buildQuery())
    applications.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const exportApplicants = async () => {
  exporting.value = true
  try {
    const response = await getLabApplyPage(buildQuery({ pageNum: 1, pageSize: 1000 }))
    const records = response.data.records || []
    downloadCsv(
      `lab-applicants-${dayjs().format('YYYYMMDD-HHmmss')}.csv`,
      [
        ['姓名', '学号', '班级'],
        ...records.map((item) => [item.studentName || '', item.studentId || '', item.grade || ''])
      ]
    )
    ElMessage.success('报名名单已导出')
  } finally {
    exporting.value = false
  }
}

const handleAudit = async (row, action) => {
  const actionLabel = action === 'leaderApprove' ? '初审通过' : action === 'approve' ? '终审通过' : '驳回申请'

  const result = await ElMessageBox.prompt(`请输入${actionLabel}备注`, '审核确认', {
    inputPlaceholder: '可选，留空也可直接提交',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  }).catch(() => null)

  if (!result) {
    return
  }

  await auditLabApply(row.id, {
    action,
    auditComment: result.value
  })
  ElMessage.success('申请处理完成')
  await loadApplications()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadApplications()
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
  loadApplications()
})
</script>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

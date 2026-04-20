<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">教师注册审批</p>
          <h2>支持学院初审与学校终审的教师注册管理</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="工号 / 姓名 / 邮箱 / 学院" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option label="待学院审核" value="submitted" />
            <el-option label="待学校审核" value="college_approved" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <TablePageCard title="教师注册审批" subtitle="审批队列" :count-label="`${pagination.total} 条`">
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="teacherNo" label="工号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="collegeName" label="所属学院" min-width="160" />
        <el-table-column prop="title" label="职称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="流程状态" min-width="120">
          <template #default="{ row }">
            <StatusTag :value="row.status" preset="apply" />
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="学院审核" min-width="220">
          <template #default="{ row }">
            <div class="audit-cell">
              <span>{{ row.collegeAuditComment || '待处理' }}</span>
              <small>{{ formatDateTime(row.collegeAuditTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学校审核" min-width="220">
          <template #default="{ row }">
            <div class="audit-cell">
              <span>{{ row.schoolAuditComment || '待处理' }}</span>
              <small>{{ formatDateTime(row.schoolAuditTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="生成账号" min-width="120">
          <template #default="{ row }">{{ row.generatedUserName || '-' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="canCollegeApprove(row)">
              <el-button link type="primary" @click="handleAudit(row, 'collegeApprove', '学院审核通过')">
                学院通过
              </el-button>
              <el-button link type="danger" @click="handleAudit(row, 'reject', '学院驳回申请')">
                驳回
              </el-button>
            </template>

            <template v-else-if="canSchoolApprove(row)">
              <el-button link type="success" @click="handleAudit(row, 'schoolApprove', '学校审核通过')">
                学校通过
              </el-button>
              <el-button link type="danger" @click="handleAudit(row, 'reject', '学校驳回申请')">
                驳回
              </el-button>
            </template>

            <template v-else-if="canSuperAdminReject(row)">
              <el-button link type="danger" @click="handleAudit(row, 'reject', '学校驳回申请')">
                驳回
              </el-button>
            </template>

            <span v-else class="done-text">无可执行操作</span>
          </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { auditTeacherRegisterApply, getTeacherRegisterApplyPage } from '@/api/teacherRegisterApplies'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  status: ''
})

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')

const loadPageData = async () => {
  loading.value = true
  try {
    const response = await getTeacherRegisterApplyPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    records.value = response.data?.records || []
    pagination.total = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleAudit = async (row, action, title) => {
  const result = await ElMessageBox.prompt(`请输入“${title}”备注`, title, {
    inputPlaceholder: '可选，留空也可以直接提交',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  }).catch(() => null)

  if (!result) {
    return
  }

  await auditTeacherRegisterApply(row.id, {
    action,
    auditComment: result.value
  })
  ElMessage.success('教师注册申请处理完成')
  await loadPageData()
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadPageData()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadPageData()
}

const canCollegeApprove = (row) => !isSuperAdmin.value && row.status === 'submitted'
const canSchoolApprove = (row) => isSuperAdmin.value && row.status === 'college_approved'
const canSuperAdminReject = (row) => isSuperAdmin.value && row.status === 'submitted'

const statusLabel = (status) => {
  const map = {
    submitted: '待学院审核',
    college_approved: '待学校审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  const map = {
    submitted: 'warning',
    college_approved: 'primary',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  loadPageData()
})
</script>

<style scoped>
.audit-cell {
  display: grid;
  gap: 6px;
}

.audit-cell small {
  color: #64748b;
}

.done-text {
  color: #64748b;
  font-size: 13px;
}
</style>

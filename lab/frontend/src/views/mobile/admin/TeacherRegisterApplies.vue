<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="filters.keyword" clearable placeholder="搜索工号、姓名、邮箱或学院" @keyup.enter="resetAndFetch" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 120px" @change="resetAndFetch">
        <el-option label="待学院审" value="submitted" />
        <el-option label="待学校审" value="college_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
    </section>

    <section class="card-list">
      <article v-for="row in rows" :key="row.id" class="record-card">
        <div class="card-head">
          <div>
            <strong>{{ row.realName || '未命名教师' }}</strong>
            <p>{{ row.teacherNo || '-' }} · {{ row.collegeName || '-' }}</p>
          </div>
          <span class="status-pill" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        </div>

        <div class="meta-grid">
          <div><span>职称</span><strong>{{ row.title || '-' }}</strong></div>
          <div><span>手机号</span><strong>{{ row.phone || '-' }}</strong></div>
          <div><span>邮箱</span><strong>{{ row.email || '-' }}</strong></div>
          <div><span>账号</span><strong>{{ row.generatedUserName || '-' }}</strong></div>
        </div>

        <div class="section-block">
          <label>申请说明</label>
          <div>{{ row.applyReason || '-' }}</div>
        </div>
        <div class="section-block compact-grid">
          <div>
            <label>学院审核</label>
            <div>{{ row.collegeAuditComment || '待处理' }}</div>
          </div>
          <div>
            <label>学校审核</label>
            <div>{{ row.schoolAuditComment || '待处理' }}</div>
          </div>
        </div>

        <div class="action-row">
          <el-button v-if="canCollegeApprove(row)" text type="primary" @click="handleAudit(row, 'collegeApprove', '学院审核通过')">
            学院通过
          </el-button>
          <el-button v-if="canSchoolApprove(row)" text type="success" @click="handleAudit(row, 'schoolApprove', '学校审核通过')">
            学校通过
          </el-button>
          <el-button v-if="canReject(row)" text type="danger" @click="handleAudit(row, 'reject', '驳回申请')">
            驳回
          </el-button>
        </div>
      </article>

      <el-empty v-if="!loading && rows.length === 0" description="暂无教师注册申请" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditTeacherRegisterApply, getTeacherRegisterApplyPage } from '@/api/teacherRegisterApplies'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const loadingMore = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const filters = reactive({
  keyword: '',
  status: ''
})

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const hasMore = computed(() => rows.value.length < total.value)

const fetchPage = async (page) => {
  const response = await getTeacherRegisterApplyPage({
    pageNum: page,
    pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined
  })
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    rows.value = await fetchPage(1)
  } finally {
    loading.value = false
  }
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchPage(nextPage)
    pageNum.value = nextPage
    rows.value = rows.value.concat(list)
  } finally {
    loadingMore.value = false
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
  ElMessage.success('申请处理完成')
  await resetAndFetch()
}

const canCollegeApprove = (row) => isCollegeManager.value && row.status === 'submitted'
const canSchoolApprove = (row) => isSuperAdmin.value && row.status === 'college_approved'
const canReject = (row) => (isCollegeManager.value && row.status === 'submitted') || (isSuperAdmin.value && row.status !== 'approved' && row.status !== 'rejected')

const statusLabel = (value) => ({
  submitted: '待学院审',
  college_approved: '待学校审',
  approved: '已通过',
  rejected: '已驳回'
}[value] || value || '-')

const statusClass = (value) => ({
  submitted: 'pending',
  college_approved: 'progress',
  approved: 'success',
  rejected: 'danger'
}[value] || 'default')

onMounted(() => {
  resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.record-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 10px;
}

.card-list {
  display: grid;
  gap: 10px;
}

.record-card {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head strong,
.section-block label,
.meta-grid strong {
  color: #0f172a;
}

.card-head p,
.section-block div,
.meta-grid span,
.muted {
  color: #64748b;
}

.card-head p {
  margin: 6px 0 0;
}

.meta-grid,
.compact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.meta-grid span {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.section-block {
  display: grid;
  gap: 6px;
}

.section-block label {
  font-size: 13px;
  font-weight: 700;
}

.section-block div {
  line-height: 1.7;
  white-space: pre-wrap;
}

.action-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.pending {
  color: #b45309;
  background: rgba(254, 243, 199, 0.92);
}

.status-pill.progress {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.92);
}

.status-pill.success {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.danger {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.92);
}

.load-more {
  display: flex;
  justify-content: center;
}

@media (max-width: 560px) {
  .toolbar-card,
  .meta-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>

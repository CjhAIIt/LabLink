<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="filters.keyword" clearable placeholder="搜索学生、学号或实验室" @keyup.enter="resetAndFetch" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 120px" @change="resetAndFetch">
        <el-option label="待审核" value="submitted" />
        <el-option label="初审通过" value="leader_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
    </section>

    <section class="card-list">
      <article v-for="row in rows" :key="row.id" class="record-card">
        <div class="card-head">
          <div>
            <strong>{{ row.studentName || '未命名学生' }}</strong>
            <p>{{ row.studentId || '-' }} · {{ row.labName || '实验室待定' }}</p>
          </div>
          <span class="status-pill" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        </div>

        <div class="meta-grid">
          <div><span>班级</span><strong>{{ row.grade || '-' }}</strong></div>
          <div><span>计划</span><strong>{{ row.planTitle || '-' }}</strong></div>
          <div><span>提交时间</span><strong>{{ formatDateTime(row.createTime) }}</strong></div>
          <div><span>简历</span><a v-if="row.resume" class="file-link" :href="resolveFileUrl(row.resume)" target="_blank">查看</a><strong v-else>-</strong></div>
        </div>

        <div class="section-block">
          <label>申请理由</label>
          <div>{{ row.applyReason || '未填写' }}</div>
        </div>
        <div class="section-block">
          <label>审核意见</label>
          <div>{{ row.auditComment || '暂无审核意见' }}</div>
        </div>

        <div class="action-row">
          <el-button v-if="row.status === 'submitted'" text type="primary" @click="handleAudit(row, 'leaderApprove', '初审通过')">
            初审通过
          </el-button>
          <el-button
            v-if="row.status === 'submitted' || row.status === 'leader_approved'"
            text
            type="success"
            @click="handleAudit(row, 'approve', '终审通过')"
          >
            终审通过
          </el-button>
          <el-button
            v-if="row.status !== 'approved' && row.status !== 'rejected'"
            text
            type="danger"
            @click="handleAudit(row, 'reject', '驳回申请')"
          >
            驳回
          </el-button>
        </div>
      </article>

      <el-empty v-if="!loading && rows.length === 0" description="暂无成员申请" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditLabApply, getLabApplyPage } from '@/api/labApplies'
import { resolveFileUrl } from '@/utils/file'

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

const hasMore = computed(() => rows.value.length < total.value)

const buildQuery = (page) => ({
  pageNum: page,
  pageSize,
  keyword: filters.keyword || undefined,
  status: filters.status || undefined
})

const fetchPage = async (page) => {
  const response = await getLabApplyPage(buildQuery(page))
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

  await auditLabApply(row.id, {
    action,
    auditComment: result.value
  })
  ElMessage.success('申请处理完成')
  await resetAndFetch()
}

const statusLabel = (value) => ({
  submitted: '待审核',
  leader_approved: '初审通过',
  approved: '已通过',
  rejected: '已驳回'
}[value] || value || '-')

const statusClass = (value) => ({
  submitted: 'pending',
  leader_approved: 'progress',
  approved: 'success',
  rejected: 'danger'
}[value] || 'default')

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

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

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.meta-grid span {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.file-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 700;
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

@media (max-width: 480px) {
  .toolbar-card,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>

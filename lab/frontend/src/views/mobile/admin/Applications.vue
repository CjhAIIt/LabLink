<template>
  <MobilePageContainer>
    <section class="admin-review-hero">
      <div>
        <span class="mobile-kicker">Review Desk</span>
        <h1>学生加入审核</h1>
        <p>手机端默认展示核心字段，详情和操作都放在申请卡片内。</p>
      </div>
      <button class="admin-review-refresh" type="button" :disabled="loading" @click="resetAndFetch">刷新</button>
    </section>

    <MobileSearchBar
      v-model="filters.keyword"
      placeholder="搜索学生、学号或实验室"
      filter-text="状态筛选"
      @search="resetAndFetch"
      @filter="filterVisible = true"
    />

    <section class="review-card-flow">
      <article v-for="row in rows" :key="row.id" class="review-card">
        <div class="review-card__head">
          <div>
            <span>{{ row.studentId || '-' }}</span>
            <h2>{{ row.studentName || '未命名学生' }}</h2>
            <p>{{ row.labName || '实验室待定' }}</p>
          </div>
          <MobileStatusTag :type="statusType(row.status)" :label="statusLabel(row.status)" />
        </div>

        <div class="review-card__meta">
          <div><span>学院 / 班级</span><strong>{{ row.collegeName || row.college || row.grade || '-' }}</strong></div>
          <div><span>招新计划</span><strong>{{ row.planTitle || '-' }}</strong></div>
          <div><span>提交时间</span><strong>{{ formatDateTime(row.createTime) }}</strong></div>
          <div>
            <span>简历</span>
            <a v-if="row.resume" class="file-link" :href="resolveFileUrl(row.resume)" target="_blank">查看简历</a>
            <strong v-else>-</strong>
          </div>
        </div>

        <section>
          <label>申请理由</label>
          <p>{{ row.applyReason || '未填写' }}</p>
        </section>
        <section>
          <label>审核意见</label>
          <p>{{ row.auditComment || '暂无审核意见' }}</p>
        </section>

        <div class="review-card__actions">
          <el-button v-if="row.status === 'submitted'" plain type="primary" @click="handleAudit(row, 'leaderApprove', '初审通过')">
            初审通过
          </el-button>
          <el-button
            v-if="row.status === 'submitted' || row.status === 'leader_approved'"
            type="success"
            @click="handleAudit(row, 'approve', '终审通过')"
          >
            同意
          </el-button>
          <el-button
            v-if="row.status !== 'approved' && row.status !== 'rejected'"
            plain
            type="danger"
            @click="handleAudit(row, 'reject', '驳回申请')"
          >
            驳回
          </el-button>
        </div>
      </article>

      <MobileEmptyState
        v-if="!loading && rows.length === 0"
        icon="Tickets"
        title="暂无成员申请"
        description="新的入组申请会出现在这里，可按状态筛选处理。"
      />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>

    <MobileFilterSheet
      v-model="filterVisible"
      title="筛选审核状态"
      description="减少列表干扰，手机端只保留最有用的筛选。"
      @reset="resetFilters"
      @apply="applyFilters"
    >
      <div class="mobile-chip-row">
        <button
          v-for="item in statusOptions"
          :key="item.value"
          class="mobile-chip"
          :class="{ active: filters.status === item.value }"
          type="button"
          @click="filters.status = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </MobileFilterSheet>
  </MobilePageContainer>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditLabApply, getLabApplyPage } from '@/api/labApplies'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobileFilterSheet from '@/components/mobile/MobileFilterSheet.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileSearchBar from '@/components/mobile/MobileSearchBar.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { resolveFileUrl } from '@/utils/file'

const loading = ref(false)
const loadingMore = ref(false)
const filterVisible = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const filters = reactive({
  keyword: '',
  status: ''
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'submitted' },
  { label: '初审通过', value: 'leader_approved' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' }
]

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

const resetFilters = () => {
  filters.status = ''
  filterVisible.value = false
  resetAndFetch()
}

const applyFilters = () => {
  filterVisible.value = false
  resetAndFetch()
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

const statusType = (value) => ({
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
.admin-review-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #197a78 58%, #2aa3a1);
  box-shadow: 0 22px 48px rgba(15, 118, 110, 0.2);
  display: grid;
  gap: 16px;
}

.mobile-kicker {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  font-size: 12px;
  font-weight: 900;
}

.admin-review-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.admin-review-hero p {
  margin: 0;
  color: rgba(240, 253, 250, 0.88);
  line-height: 1.68;
}

.admin-review-refresh {
  width: fit-content;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  font-weight: 900;
}

.review-card-flow {
  display: grid;
  gap: 13px;
}

.review-card {
  padding: 16px;
  display: grid;
  gap: 13px;
  border-radius: 24px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.review-card__head,
.review-card__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.review-card__head span {
  color: #197a78;
  font-size: 12px;
  font-weight: 900;
}

.review-card__head h2 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 18px;
}

.review-card__head p {
  margin: 5px 0 0;
  color: #64748b;
}

.review-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.review-card__meta div {
  padding: 11px;
  border-radius: 16px;
  background: #f8fafc;
  display: grid;
  gap: 5px;
}

.review-card__meta span,
.review-card section label {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.review-card__meta strong,
.review-card section label {
  color: #172033;
}

.file-link {
  color: #197a78;
  text-decoration: none;
  font-weight: 900;
}

.review-card section {
  display: grid;
  gap: 6px;
}

.review-card section p {
  margin: 0;
  color: #64748b;
  line-height: 1.72;
  white-space: pre-wrap;
}

.review-card__actions .el-button {
  flex: 1 1 0;
}

.mobile-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mobile-chip {
  min-height: 40px;
  padding: 8px 13px;
  border: 1px solid rgba(15, 118, 110, 0.13);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  font-weight: 900;
}

.mobile-chip.active {
  color: #0f5c59;
  background: #e8f8f5;
  border-color: rgba(15, 118, 110, 0.24);
}

.load-more {
  display: flex;
  justify-content: center;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 430px) {
  .review-card__head,
  .review-card__actions {
    flex-direction: column;
  }

  .review-card__actions .el-button {
    width: 100%;
  }

  .review-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>

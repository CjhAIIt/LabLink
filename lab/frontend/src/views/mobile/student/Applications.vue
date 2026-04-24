<template>
  <MobilePageContainer>
    <section class="applications-hero">
      <div>
        <span class="mobile-kicker">Applications</span>
        <h1>我的申请记录</h1>
        <p>查看实验室申请的审核进度、反馈意见和提交时间。</p>
      </div>
      <button class="applications-refresh" type="button" :disabled="loading" @click="resetAndFetch">刷新</button>
    </section>

    <div class="applications-toolbar">
      <button class="filter-trigger" type="button" @click="filterVisible = true">
        <el-icon :size="18"><Filter /></el-icon>
        <span>{{ currentStatusLabel }}</span>
      </button>
    </div>

    <section class="application-card-flow">
      <article v-for="row in applies" :key="row.id" class="application-card">
        <div class="application-card__head">
          <div>
            <span>{{ row.planTitle || '招新计划' }}</span>
            <h2>{{ row.labName || '未命名实验室' }}</h2>
          </div>
          <MobileStatusTag :type="statusType(row.status)" :label="statusLabel(row.status)" />
        </div>

        <div class="application-card__timeline">
          <div>
            <span>提交时间</span>
            <strong>{{ formatDateTime(row.createTime) }}</strong>
          </div>
          <div>
            <span>当前阶段</span>
            <strong>{{ statusLabel(row.status) }}</strong>
          </div>
        </div>

        <section>
          <label>申请理由</label>
          <p>{{ row.applyReason || '未填写' }}</p>
        </section>
        <section>
          <label>审核意见</label>
          <p>{{ row.auditComment || '暂未返回审核意见' }}</p>
        </section>
      </article>

      <MobileEmptyState
        v-if="!loading && applies.length === 0"
        icon="Tickets"
        title="暂无申请记录"
        description="找到合适的实验室后，可以在详情页提交申请。"
      >
        <el-button type="primary" @click="$router.push('/m/student/labs')">去实验室广场</el-button>
      </MobileEmptyState>
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="applies.length" class="muted">已经到底了</span>
    </div>

    <MobileFilterSheet
      v-model="filterVisible"
      title="筛选申请状态"
      description="选择一个状态后，列表会只显示对应申请。"
      @reset="resetFilters"
      @apply="applyFilter"
    >
      <div class="mobile-chip-row">
        <button
          v-for="item in statusOptions"
          :key="item.value"
          class="mobile-chip"
          :class="{ active: status === item.value }"
          type="button"
          @click="status = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </MobileFilterSheet>
  </MobilePageContainer>
</template>

<script setup>
import dayjs from 'dayjs'
import { Filter } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobileFilterSheet from '@/components/mobile/MobileFilterSheet.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'

const status = ref('')
const filterVisible = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const applies = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'submitted' },
  { label: '初审通过', value: 'leader_approved' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' }
]

const hasMore = computed(() => applies.value.length < total.value)
const currentStatusLabel = computed(() => statusOptions.find((item) => item.value === status.value)?.label || '全部状态')

const fetchPage = async (page) => {
  const response = await getMyLabApplyPage({ pageNum: page, pageSize, status: status.value || undefined })
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    applies.value = await fetchPage(1)
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
    applies.value = applies.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const resetFilters = () => {
  status.value = ''
  filterVisible.value = false
  resetAndFetch()
}

const applyFilter = () => {
  filterVisible.value = false
  resetAndFetch()
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
.applications-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 54%, #19a7b8);
  box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
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

.applications-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.applications-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.applications-refresh,
.filter-trigger,
.mobile-chip {
  min-height: 44px;
  border-radius: 16px;
  font-weight: 900;
}

.applications-refresh {
  width: fit-content;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

.applications-toolbar {
  display: flex;
  justify-content: flex-end;
}

.filter-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid rgba(51, 136, 187, 0.13);
  background: #ffffff;
  color: #176b9a;
}

.application-card-flow {
  display: grid;
  gap: 13px;
}

.application-card {
  padding: 16px;
  display: grid;
  gap: 13px;
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.application-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.application-card__head span {
  color: #3388bb;
  font-size: 12px;
  font-weight: 900;
}

.application-card__head h2 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 18px;
  line-height: 1.25;
}

.application-card__timeline {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.application-card__timeline div {
  padding: 11px;
  border-radius: 16px;
  background: #f8fafc;
  display: grid;
  gap: 5px;
}

.application-card__timeline span,
.application-card section label {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.application-card__timeline strong,
.application-card section label {
  color: #172033;
}

.application-card section {
  display: grid;
  gap: 6px;
}

.application-card section p {
  margin: 0;
  color: #64748b;
  line-height: 1.72;
  white-space: pre-wrap;
}

.mobile-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mobile-chip {
  padding: 8px 13px;
  border: 1px solid rgba(51, 136, 187, 0.13);
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
}

.mobile-chip.active {
  color: #176b9a;
  background: #eaf6fc;
  border-color: rgba(51, 136, 187, 0.26);
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
  .application-card__head {
    flex-direction: column;
  }

  .application-card__timeline {
    grid-template-columns: 1fr;
  }
}
</style>

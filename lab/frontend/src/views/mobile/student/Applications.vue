<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-select v-model="status" clearable placeholder="全部状态" style="width: 100%" @change="resetAndFetch">
        <el-option label="待审核" value="submitted" />
        <el-option label="初审通过" value="leader_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section class="card-list">
      <article v-for="row in applies" :key="row.id" class="apply-card">
        <div class="card-head">
          <div>
            <strong>{{ row.labName || '未命名实验室' }}</strong>
            <p>{{ row.planTitle || '招新计划' }}</p>
          </div>
          <span class="status-pill" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        </div>
        <p>提交时间：{{ formatDateTime(row.createTime) }}</p>
        <div class="section-block">
          <label>申请理由</label>
          <div>{{ row.applyReason || '未填写' }}</div>
        </div>
        <div class="section-block">
          <label>审核意见</label>
          <div>{{ row.auditComment || '暂未返回审核意见' }}</div>
        </div>
      </article>

      <el-empty v-if="!loading && applies.length === 0" description="暂无申请记录" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="applies.length" class="muted">已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'

const status = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const applies = ref([])

const hasMore = computed(() => applies.value.length < total.value)

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
.apply-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.card-list {
  display: grid;
  gap: 10px;
}

.apply-card {
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
.section-block label {
  color: #0f172a;
}

.card-head p,
.apply-card p,
.section-block div,
.muted {
  color: #64748b;
}

.card-head p,
.apply-card p {
  margin: 6px 0 0;
}

.section-block {
  display: grid;
  gap: 6px;
}

.section-block label {
  font-weight: 700;
  font-size: 13px;
}

.section-block div {
  line-height: 1.7;
  white-space: pre-wrap;
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
</style>

<template>
  <div class="m-page">
    <section class="filters">
      <el-select v-model="status" clearable placeholder="全部状态" style="width: 100%">
        <el-option label="待审核" value="submitted" />
        <el-option label="初审通过" value="leader_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section v-loading="loading" class="list">
      <article v-for="row in applies" :key="row.id" class="card">
        <div class="card-head">
          <div class="title">
            <strong>{{ row.labName || '未命名实验室' }}</strong>
            <span>{{ row.planTitle || '招新计划' }}</span>
          </div>
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </div>
        <div class="meta">
          <span>提交时间：{{ formatDateTime(row.createTime) }}</span>
        </div>
        <div class="section">
          <label>申请理由</label>
          <p>{{ row.applyReason || '暂无申请理由' }}</p>
        </div>
        <div class="section">
          <label>审核意见</label>
          <p>{{ row.auditComment || '暂未反馈审核意见' }}</p>
        </div>
      </article>

      <el-empty v-if="!loading && applies.length === 0" description="暂无申请记录" :image-size="80" />

      <div class="load-more">
        <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
        <span v-else-if="applies.length" class="no-more">已到底</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref, watch } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'

const status = ref('')
const loading = ref(false)
const loadingMore = ref(false)

const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const applies = ref([])

const hasMore = computed(() => applies.value.length < (total.value || 0))

const statusLabel = (value) => {
  const map = {
    submitted: '待审核',
    leader_approved: '初审通过',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[value] || value || '-'
}

const statusTagType = (value) => {
  const map = {
    submitted: 'warning',
    leader_approved: 'primary',
    approved: 'success',
    rejected: 'danger'
  }
  return map[value] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const fetchPage = async (page) => {
  const res = await getMyLabApplyPage({
    pageNum: page,
    pageSize,
    status: status.value || undefined
  })
  const pageData = res.data || {}
  total.value = pageData.total || 0
  return pageData.records || []
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
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const next = pageNum.value + 1
    const records = await fetchPage(next)
    pageNum.value = next
    applies.value = applies.value.concat(records)
  } finally {
    loadingMore.value = false
  }
}

watch(
  () => status.value,
  () => {
    resetAndFetch()
  }
)

onMounted(() => {
  resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filters {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.title {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.title strong {
  color: #0f172a;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title span {
  color: #64748b;
  font-size: 12px;
}

.meta {
  color: #64748b;
  font-size: 12px;
}

.section {
  display: grid;
  gap: 6px;
}

.section label {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.section p {
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.load-more {
  padding: 10px 0 2px 0;
  display: flex;
  justify-content: center;
}

.no-more {
  font-size: 12px;
  color: #94a3b8;
}
</style>

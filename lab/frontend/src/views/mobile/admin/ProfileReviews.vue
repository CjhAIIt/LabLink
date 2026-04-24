<template>
  <div class="m-page">
    <section class="toolbar-card">
      <div class="tab-row">
        <button class="tab-btn" :class="{ active: activeTab === 'pending' }" type="button" @click="switchTab('pending')">
          待审核
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'all' }" type="button" @click="switchTab('all')">
          全部资料
        </button>
      </div>
      <el-input v-model="filters.keyword" clearable placeholder="搜索姓名、学号或专业" @keyup.enter="resetAndFetch" />
      <el-select v-if="activeTab === 'all'" v-model="filters.status" clearable placeholder="状态" style="width: 120px" @change="resetAndFetch">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section class="metric-grid">
      <article v-for="card in summaryCards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.tip }}</small>
      </article>
    </section>

    <section class="card-list" v-loading="loading">
      <article v-for="row in rows" :key="row.reviewId || row.profileId || row.id" class="record-card">
        <div class="card-head">
          <div>
            <strong>{{ row.realName || '-' }}</strong>
            <p>{{ row.studentNo || '-' }} · {{ row.major || '未填写专业' }}</p>
          </div>
          <span class="status-pill" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        </div>

        <div class="meta-grid">
          <div>
            <span>学院</span>
            <strong>{{ row.collegeName || '-' }}</strong>
          </div>
          <div>
            <span>实验室</span>
            <strong>{{ row.labName || '-' }}</strong>
          </div>
          <div>
            <span>班级</span>
            <strong>{{ row.className || '-' }}</strong>
          </div>
          <div>
            <span>{{ activeTab === 'pending' ? '提交时间' : '更新时间' }}</span>
            <strong>{{ formatDateTime(activeTab === 'pending' ? row.submitTime : row.updateTime) }}</strong>
          </div>
        </div>

        <div class="action-row">
          <el-button text type="primary" @click="openDetail(row)">查看详情</el-button>
          <el-button v-if="activeTab === 'pending'" text type="success" @click="handleReviewAction('approve', row)">通过</el-button>
          <el-button v-if="activeTab === 'pending'" text type="danger" @click="handleReviewAction('reject', row)">驳回</el-button>
        </div>
      </article>

      <el-empty v-if="!loading && rows.length === 0" description="暂无资料记录" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>

    <el-drawer v-model="detailVisible" title="资料详情" size="92%">
      <div v-if="detailLoading" class="drawer-loading">正在加载资料详情...</div>
      <div v-else-if="detailProfile" class="drawer-body">
        <section class="detail-card">
          <div class="detail-grid">
            <article v-for="item in detailItems" :key="item.label" class="detail-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </section>

        <section class="detail-card">
          <header class="section-head">
            <h3>个人介绍</h3>
          </header>
          <div class="multi-line">{{ detailProfile.introduction || '-' }}</div>
        </section>

        <section class="detail-card">
          <header class="section-head">
            <h3>资料附件</h3>
          </header>
          <a
            v-if="detailProfile.attachmentUrl"
            class="file-link"
            :href="resolveFileUrl(detailProfile.attachmentUrl)"
            target="_blank"
            rel="noreferrer"
          >
            {{ getFileNameFromUrl(detailProfile.attachmentUrl, '成员资料附件') }}
          </a>
          <span v-else class="muted">暂无附件</span>
        </section>

        <section class="detail-card">
          <header class="section-head">
            <h3>审核历史</h3>
          </header>
          <div v-if="reviewHistory.length" class="timeline-list">
            <article v-for="item in reviewHistory" :key="item.id" class="timeline-card">
              <div class="timeline-head">
                <strong>版本 {{ item.versionNo }}</strong>
                <span :class="['status-pill', statusClass(item.reviewStatus)]">{{ statusLabel(item.reviewStatus) }}</span>
              </div>
              <p>{{ item.reviewComment || '无审核意见' }}</p>
              <small>{{ formatDateTime(item.reviewTime || item.createTime) }}</small>
            </article>
          </div>
          <el-empty v-else description="暂无审核历史" :image-size="72" />
        </section>

        <section class="detail-card">
          <header class="section-head">
            <h3>归档历史</h3>
          </header>
          <div v-if="archiveHistory.length" class="timeline-list">
            <article v-for="item in archiveHistory" :key="item.id" class="timeline-card">
              <div class="timeline-head">
                <strong>版本 {{ item.versionNo }}</strong>
                <span class="status-pill success">已归档</span>
              </div>
              <p>{{ item.snapshot?.direction || item.snapshot?.major || '已生成归档快照' }}</p>
              <small>{{ formatDateTime(item.archivedAt || item.createTime) }}</small>
            </article>
          </div>
          <el-empty v-else description="暂无归档历史" :image-size="72" />
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveProfileReview,
  getPendingProfileReviewPage,
  getProfileArchives,
  getProfileDetail,
  getProfilePage,
  getProfileReviews,
  rejectProfileReview
} from '@/api/profile'
import { getFileNameFromUrl, resolveFileUrl } from '@/utils/file'

const pageSize = 8
const activeTab = ref('pending')
const loading = ref(false)
const loadingMore = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const detailProfile = ref(null)
const reviewHistory = ref([])
const archiveHistory = ref([])

const filters = reactive({
  keyword: '',
  status: ''
})

const hasMore = computed(() => rows.value.length < total.value)

const summaryCards = computed(() => [
  { label: activeTab.value === 'pending' ? '待审核' : '资料总数', value: total.value, tip: activeTab.value === 'pending' ? '当前待审核资料数量' : '当前筛选结果' },
  { label: '当前页', value: rows.value.length, tip: '已加载到手机端的记录数' },
  { label: '驳回数', value: rows.value.filter((item) => item.status === 'REJECTED').length, tip: '当前页已驳回资料' },
  { label: '归档数', value: rows.value.filter((item) => item.status === 'ARCHIVED').length, tip: '当前页已归档资料' }
])

const detailItems = computed(() => {
  if (!detailProfile.value) {
    return []
  }
  return [
    { label: '姓名', value: detailProfile.value.realName || '-' },
    { label: '学号', value: detailProfile.value.studentNo || '-' },
    { label: '学院', value: detailProfile.value.collegeName || '-' },
    { label: '实验室', value: detailProfile.value.labName || '-' },
    { label: '专业', value: detailProfile.value.major || '-' },
    { label: '班级', value: detailProfile.value.className || '-' },
    { label: '手机号', value: detailProfile.value.phone || '-' },
    { label: '邮箱', value: detailProfile.value.email || '-' },
    { label: '方向', value: detailProfile.value.direction || '-' },
    { label: '状态', value: statusLabel(detailProfile.value.status) },
    { label: '提交时间', value: formatDateTime(detailProfile.value.submittedAt) },
    { label: '最近审核', value: formatDateTime(detailProfile.value.lastReviewTime) }
  ]
})

const buildParams = (page) => ({
  pageNum: page,
  pageSize,
  keyword: filters.keyword || undefined,
  status: activeTab.value === 'all' ? filters.status || undefined : undefined
})

const fetchPage = async (page) => {
  const response = activeTab.value === 'pending'
    ? await getPendingProfileReviewPage(buildParams(page))
    : await getProfilePage(buildParams(page))
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    rows.value = await fetchPage(1)
  } catch (error) {
    ElMessage.error(error.message || '加载资料列表失败')
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
  } catch (error) {
    ElMessage.error(error.message || '加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

const switchTab = async (tab) => {
  if (activeTab.value === tab) {
    return
  }
  activeTab.value = tab
  filters.status = ''
  await resetAndFetch()
}

const openDetail = async (row) => {
  const profileId = row.profileId || row.id
  if (!profileId) {
    return
  }

  detailVisible.value = true
  detailLoading.value = true
  detailProfile.value = null
  reviewHistory.value = []
  archiveHistory.value = []

  try {
    const [detailRes, reviewRes, archiveRes] = await Promise.all([
      getProfileDetail(profileId),
      getProfileReviews(profileId),
      getProfileArchives(profileId)
    ])
    detailProfile.value = detailRes.data || null
    reviewHistory.value = reviewRes.data || []
    archiveHistory.value = archiveRes.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载资料详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleReviewAction = async (action, row) => {
  const label = action === 'approve' ? '通过' : '驳回'
  const result = await ElMessageBox.prompt(`请输入“${label}”意见`, `${label}资料`, {
    inputType: 'textarea',
    inputPattern: /[\s\S]{2,}/,
    inputErrorMessage: '审核意见至少输入 2 个字符',
    confirmButtonText: label,
    cancelButtonText: '取消'
  }).catch(() => null)

  if (!result) {
    return
  }

  try {
    const payload = { reviewComment: result.value }
    if (action === 'approve') {
      await approveProfileReview(row.reviewId, payload)
      ElMessage.success('资料已审核通过')
    } else {
      await rejectProfileReview(row.reviewId, payload)
      ElMessage.success('资料已驳回')
    }

    if (detailVisible.value && detailProfile.value?.id === (row.profileId || row.id)) {
      await openDetail(row)
    }
    await resetAndFetch()
  } catch (error) {
    ElMessage.error(error.message || '资料审核失败')
  }
}

const statusLabel = (value) => ({
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  ARCHIVED: '已归档'
}[value] || value || '-')

const statusClass = (value) => ({
  DRAFT: 'default',
  PENDING: 'pending',
  APPROVED: 'success',
  REJECTED: 'danger',
  ARCHIVED: 'progress'
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

.metric-grid,
.card-list,
.meta-grid,
.detail-grid,
.timeline-list {
  display: grid;
  gap: 10px;
}

.metric-grid,
.meta-grid,
.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.toolbar-card,
.metric-card,
.record-card,
.detail-card,
.timeline-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card,
.metric-card,
.record-card,
.detail-card,
.timeline-card {
  padding: 14px;
}

.toolbar-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 120px auto;
  gap: 10px;
}

.metric-card {
  display: grid;
  gap: 6px;
}

.metric-card span,
.metric-card small,
.card-head p,
.meta-grid span,
.detail-item span,
.timeline-card p,
.timeline-card small,
.drawer-loading,
.muted {
  color: #64748b;
}

.metric-card strong,
.card-head strong,
.meta-grid strong,
.detail-item strong,
.section-head h3,
.timeline-head strong {
  color: #0f172a;
}

.metric-card strong {
  font-size: 24px;
}

.tab-row,
.card-head,
.action-row,
.section-head,
.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.tab-row {
  justify-content: flex-start;
}

.tab-btn {
  border: 0;
  background: rgba(241, 245, 249, 0.92);
  color: #475569;
  padding: 10px 14px;
  border-radius: 999px;
  font-weight: 700;
}

.tab-btn.active {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.record-card {
  display: grid;
  gap: 10px;
}

.card-head p,
.section-head h3,
.timeline-card p {
  margin: 0;
}

.meta-grid span,
.detail-item span {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.detail-item {
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fafc;
}

.drawer-body {
  display: grid;
  gap: 12px;
  padding-bottom: 20px;
}

.section-head h3 {
  font-size: 15px;
}

.multi-line {
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.timeline-card {
  display: grid;
  gap: 8px;
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

.status-pill.default {
  color: #475569;
  background: rgba(241, 245, 249, 0.92);
}

.load-more {
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .toolbar-card,
  .metric-grid,
  .meta-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-card {
    grid-template-columns: 1fr;
  }
}
</style>

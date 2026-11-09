<template>
  <div class="page-shell">
    <section class="page-hero page-hero--brand">
      <div>
        <p class="eyebrow">学生工作台</p>
        <h1>实验室申请与信息查询</h1>
        <p class="hero-subtitle">
          查看开放招新计划、提交入组申请、跟踪审核结果，并及时获取实验室公告。
        </p>
      </div>
      <div class="hero-note-card">
        <span>当前身份：{{ userStore.realName || '同学' }}</span>
        <strong>{{ joinedLabLabel }}</strong>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="card in metricCards"
        :key="card.label"
        class="metric-card"
        :class="card.tone ? `metric-card--${card.tone}` : ''"
      >
        <div class="metric-head">
          <span class="metric-icon" :class="card.tone ? `metric-icon--${card.tone}` : ''">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </span>
          <span class="metric-label">{{ card.label }}</span>
        </div>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <section class="content-grid two-column">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>开放招新计划</span>
            <el-tag type="success" effect="plain">{{ activePlans.length }} 项</el-tag>
          </div>
        </template>

        <div v-if="!activePlans.length" class="empty-panel">
          <el-empty description="当前暂无开放计划" />
        </div>
        <div v-else class="plan-list">
          <article v-for="plan in activePlans.slice(0, 4)" :key="plan.id" class="plan-item">
            <div>
              <strong>{{ plan.title }}</strong>
              <p>{{ plan.labName }} · {{ plan.collegeName || '未分配学院' }}</p>
            </div>
            <el-tag type="primary" effect="plain">{{ plan.quota }} 人</el-tag>
          </article>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>最新公告</span>
            <el-tag type="warning" effect="plain">{{ notices.length }} 条</el-tag>
          </div>
        </template>

        <div v-if="!notices.length" class="empty-panel">
          <el-empty description="暂无公告" />
        </div>
        <div v-else class="notice-list">
          <article v-for="notice in notices" :key="notice.id" class="notice-item">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.content }}</p>
            <span>{{ formatDateTime(notice.publishTime) }}</span>
          </article>
        </div>
      </el-card>
    </section>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>我的申请记录</span>
          <router-link class="inline-link" to="/student/applications">查看全部</router-link>
        </div>
      </template>

      <el-table :data="myApplies" stripe>
        <el-table-column prop="labName" label="实验室" min-width="150" show-overflow-tooltip />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { Bell, Document, Opportunity, Timer } from '@element-plus/icons-vue'
import { getMyLabApplyPage } from '@/api/labApplies'
import { getLatestNotices } from '@/api/notices'
import { getActiveRecruitPlans } from '@/api/recruitPlans'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activePlans = ref([])
const notices = ref([])
const myApplies = ref([])

const joinedLabLabel = computed(() =>
  userStore.userInfo?.labId ? `已加入实验室 #${userStore.userInfo.labId}` : '当前未加入实验室'
)

const metricCards = computed(() => {
  const applies = myApplies.value || []
  return [
    { label: '开放计划', value: activePlans.value.length, tip: '当前正在招募的实验室计划', icon: Opportunity, tone: 'info' },
    { label: '我的申请', value: applies.length, tip: '已提交的实验室申请数量', icon: Document },
    { label: '待处理结果', value: applies.filter((item) => item.status !== 'approved' && item.status !== 'rejected').length, tip: '仍在流程中的申请', icon: Timer, tone: 'warning' },
    { label: '最新公告', value: notices.value.length, tip: '与你当前可见范围相关的公告', icon: Bell }
  ]
})

const loadDashboard = async () => {
  const [plansRes, noticeRes, applyRes] = await Promise.all([
    getActiveRecruitPlans(),
    getLatestNotices({ limit: 6 }),
    getMyLabApplyPage({ pageNum: 1, pageSize: 5 })
  ])
  activePlans.value = plansRes.data || []
  notices.value = noticeRes.data || []
  myApplies.value = applyRes.data.records || []
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
  loadDashboard()
})
</script>

<style scoped>

.hero-note-card {
  padding: 16px 18px;
  border-radius: var(--ll-radius-lg);
  color: var(--ll-on-hero);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: grid;
  gap: 6px;
}

.plan-list,
.notice-list {
  display: grid;
}

.plan-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--ll-space-4);
  padding: var(--ll-space-3) var(--ll-space-4);
  border-bottom: 1px solid var(--ll-border-subtle);
  transition: background-color var(--ll-transition-fast);
}

/* 公告：标题与时间一行，正文独占一行，避免标题被挤成多行 */
.notice-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 4px var(--ll-space-4);
  padding: var(--ll-space-3) var(--ll-space-4);
  border-bottom: 1px solid var(--ll-border-subtle);
  transition: background-color var(--ll-transition-fast);
}

.notice-item strong {
  min-width: 0;
  font-weight: 600;
  color: var(--ll-text-primary);
}

.notice-item span {
  grid-column: 2;
  grid-row: 1;
  white-space: nowrap;
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.notice-item p {
  grid-column: 1 / -1;
  font-size: var(--ll-text-sm);
  line-height: 1.6;
  color: var(--ll-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.plan-item:last-child,
.notice-item:last-child {
  border-bottom: none;
}

.plan-item:hover,
.notice-item:hover {
  background: var(--ll-bg-muted);
}

.plan-item p,
.notice-item p,
.notice-item span {
  color: var(--ll-text-secondary);
}

.inline-link {
  color: var(--ll-brand-600);
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
}
</style>

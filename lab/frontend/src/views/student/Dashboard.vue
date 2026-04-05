<template>
  <div class="page-shell">
    <section class="page-hero student-hero">
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
      <article v-for="card in metricCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
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
        <el-table-column prop="labName" label="实验室" min-width="150" />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" />
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
    { label: '开放计划', value: activePlans.value.length, tip: '当前正在招募的实验室计划' },
    { label: '我的申请', value: applies.length, tip: '已提交的实验室申请数量' },
    { label: '待处理结果', value: applies.filter((item) => item.status !== 'approved' && item.status !== 'rejected').length, tip: '仍在流程中的申请' },
    { label: '最新公告', value: notices.value.length, tip: '与你当前可见范围相关的公告' }
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
.student-hero {
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.94), rgba(14, 165, 233, 0.86)),
    radial-gradient(circle at top right, rgba(254, 240, 138, 0.22), transparent 30%);
}

.hero-note-card {
  padding: 16px 18px;
  border-radius: 22px;
  color: #f0fdfa;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: grid;
  gap: 6px;
}

.plan-list,
.notice-list {
  display: grid;
  gap: 12px;
}

.plan-item,
.notice-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.96), rgba(236, 254, 255, 0.9));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.plan-item p,
.notice-item p,
.notice-item span {
  color: #64748b;
}

.inline-link {
  color: #0f766e;
  text-decoration: none;
  font-weight: 600;
}
</style>

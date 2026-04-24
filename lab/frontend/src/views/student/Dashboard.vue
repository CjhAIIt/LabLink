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
      <MetricCard v-for="card in metricCards" :key="card.label" :label="card.label" :value="card.value" :tip="card.tip" />
    </section>

    <section class="content-grid two-column">
      <TablePageCard title="开放招新计划" subtitle="可申请实验室" :count-label="`${activePlans.length} 项`" count-tag-type="success">

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
      </TablePageCard>

      <TablePageCard title="最新公告" subtitle="通知与动态" :count-label="`${notices.length} 条`" count-tag-type="warning">

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
      </TablePageCard>
    </section>

    <TablePageCard title="我的申请记录" subtitle="最近投递" :count-label="`${myApplies.length} 条`">
      <template #header-extra>
        <router-link class="inline-link" to="/student/applications">查看全部</router-link>
      </template>

      <el-table :data="myApplies" stripe>
        <el-table-column prop="labName" label="实验室" min-width="150" />
        <el-table-column prop="planTitle" label="招新计划" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <StatusTag :value="row.status" preset="apply" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </TablePageCard>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { getMyLabApplyPage } from '@/api/labApplies'
import { getLatestNotices } from '@/api/notices'
import { getActiveRecruitPlans } from '@/api/recruitPlans'
import MetricCard from '@/components/common/MetricCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activePlans = ref([])
const notices = ref([])
const myApplies = ref([])

const joinedLabLabel = computed(() =>
  userStore.userInfo?.labName || '暂未加入实验室'
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

<template>
  <div v-loading="loading" class="page-shell teacher-dashboard">
    <section class="page-hero teacher-hero">
      <div class="hero-copy">
        <p class="eyebrow">教师工作台</p>
        <h1>{{ pageTitle }}</h1>
        <p class="hero-subtitle">
          在桌面端集中查看实验室动态、创建申请进度和最新公告。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-note-card">
          <span>当前用户</span>
          <strong>{{ userStore.realName || '教师' }}</strong>
          <small>{{ scopeLabel }}</small>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <MetricCard v-for="card in summaryCards" :key="card.label" :label="card.label" :value="card.value" :tip="card.tip" />
    </section>

    <section class="content-grid two-column">
      <ChartCard title="最近创建申请" class="panel-card">
        <template #header-extra>
          <router-link class="inline-link" to="/teacher/create-applies">查看全部</router-link>
        </template>

        <div v-if="!applications.length" class="empty-panel">
          <el-empty description="暂无创建申请" />
        </div>
        <el-table v-else :data="applications" stripe>
          <el-table-column prop="labName" label="实验室" min-width="180" />
          <el-table-column prop="collegeName" label="学院" min-width="140" />
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <StatusTag :value="row.status" preset="apply" />
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </ChartCard>

      <ChartCard title="最新公告" class="panel-card">
        <template #header-extra>
          <router-link class="inline-link" to="/teacher/notices">公告中心</router-link>
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
      </ChartCard>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import ChartCard from '@/components/common/ChartCard.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getLabCreateApplyPage } from '@/api/labCreateApplies'
import { getLatestNotices } from '@/api/notices'
import { getStatisticsDashboard } from '@/api/statistics'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const applications = ref([])
const notices = ref([])
const applyTotal = ref(0)
const stats = ref({})

const scopeLabel = computed(() => stats.value.scopeName || '当前实验室')
const pageTitle = computed(() => `${scopeLabel.value}概览`)

const summaryCards = computed(() => {
  const summary = stats.value.summary || {}
  const pending = stats.value.pending || {}
  return [
    {
      label: '成员数',
      value: summary.memberCount ?? 0,
      tip: '当前教师权限范围内的活跃成员'
    },
    {
      label: '设备数',
      value: summary.deviceCount ?? 0,
      tip: '当前实验室登记在册的设备资产'
    },
    {
      label: '出勤率',
      value: formatPercent(summary.attendanceRate),
      tip: '所选时间范围内的考勤出勤率'
    },
    {
      label: '待审资料',
      value: pending.pendingProfiles ?? 0,
      tip: '仍在等待审核的学生资料'
    },
    {
      label: '创建申请',
      value: applyTotal.value,
      tip: '教师提交的实验室创建申请数量'
    },
    {
      label: '公告数',
      value: notices.value.length,
      tip: '当前范围内可见的最新公告数量'
    }
  ]
})

const loadDashboard = async () => {
  loading.value = true
  try {
    const params = {
      startDate: dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD')
    }
    const [applyRes, noticeRes, statsRes] = await Promise.all([
      getLabCreateApplyPage({ pageNum: 1, pageSize: 20 }),
      getLatestNotices({ limit: 6 }),
      getStatisticsDashboard(params)
    ])

    applications.value = applyRes.data?.records || []
    applyTotal.value = applyRes.data?.total || 0
    notices.value = noticeRes.data || []
    stats.value = statsRes.data || {}
  } finally {
    loading.value = false
  }
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const formatPercent = (value) => {
  const number = Number(value ?? 0)
  if (Number.isNaN(number)) {
    return '0%'
  }
  return `${Math.round(number * 100) / 100}%`
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.teacher-dashboard {
  background: #f8fafc;
}

.teacher-hero {
  background:
    radial-gradient(circle at top right, rgba(190, 242, 100, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(21, 128, 61, 0.94), rgba(22, 163, 74, 0.88));
}

.hero-copy {
  display: grid;
  gap: 10px;
}

.hero-copy h1 {
  margin: 0;
  color: #fff;
}

.hero-subtitle {
  max-width: 700px;
  color: rgba(240, 253, 244, 0.92);
}

.hero-note-card {
  padding: 16px 18px;
  border-radius: 22px;
  color: #f0fdf4;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: grid;
  gap: 6px;
}

.hero-note-card small {
  color: rgba(240, 253, 244, 0.84);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  color: #0f172a;
  font-size: 26px;
}

.metric-tip {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.notice-list {
  display: grid;
  gap: 12px;
}

.notice-item {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.96), rgba(240, 253, 244, 0.88));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.notice-item p,
.notice-item span {
  color: #64748b;
}

.inline-link {
  color: #15803d;
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>

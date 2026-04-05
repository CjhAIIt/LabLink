<template>
  <div class="page-shell">
    <section class="page-hero teacher-hero">
      <div>
        <p class="eyebrow">教师工作台</p>
        <h1>教师注册已接入审批链，实验室创建申请可在线流转</h1>
        <p class="hero-subtitle">
          在这里可以查看自己的实验室创建申请进度、跟踪学院与学校审批状态，并获取学校和学院的最新公告。
        </p>
      </div>
      <div class="hero-note-card">
        <span>当前账号</span>
        <strong>{{ userStore.realName || '教师' }}</strong>
        <small>{{ userStore.userInfo?.college || '未绑定学院' }}</small>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in summaryCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <section class="content-grid two-column">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>最近申请</span>
            <router-link class="inline-link" to="/teacher/create-applies">查看全部</router-link>
          </div>
        </template>

        <div v-if="!applications.length" class="empty-panel">
          <el-empty description="暂无实验室创建申请记录" />
        </div>
        <el-table v-else :data="applications" stripe>
          <el-table-column prop="labName" label="实验室名称" min-width="180" />
          <el-table-column prop="collegeName" label="所属学院" min-width="140" />
          <el-table-column label="流程状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>最新公告</span>
            <router-link class="inline-link" to="/teacher/notices">公告中心</router-link>
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
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { getLabCreateApplyPage } from '@/api/labCreateApplies'
import { getLatestNotices } from '@/api/notices'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const applications = ref([])
const notices = ref([])
const applyTotal = ref(0)

const summaryCards = computed(() => [
  {
    label: '累计申请',
    value: applyTotal.value,
    tip: '教师本人已提交的实验室创建申请总数'
  },
  {
    label: '待审批',
    value: applications.value.filter((item) => item.status === 'submitted' || item.status === 'college_approved').length,
    tip: '仍在学院或学校审批流程中的申请'
  },
  {
    label: '已通过',
    value: applications.value.filter((item) => item.status === 'approved').length,
    tip: '已完成审批并生成实验室的申请'
  },
  {
    label: '最新公告',
    value: notices.value.length,
    tip: '当前可见范围内最近发布的公告数量'
  }
])

const loadDashboard = async () => {
  const [applyRes, noticeRes] = await Promise.all([
    getLabCreateApplyPage({ pageNum: 1, pageSize: 20 }),
    getLatestNotices({ limit: 6 })
  ])

  applications.value = applyRes.data?.records || []
  applyTotal.value = applyRes.data?.total || 0
  notices.value = noticeRes.data || []
}

const statusLabel = (status) => {
  const map = {
    submitted: '待学院审核',
    college_approved: '待学校审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  const map = {
    submitted: 'warning',
    college_approved: 'primary',
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
.teacher-hero {
  background:
    linear-gradient(135deg, rgba(21, 128, 61, 0.94), rgba(22, 163, 74, 0.86)),
    radial-gradient(circle at top right, rgba(190, 242, 100, 0.22), transparent 30%);
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
</style>

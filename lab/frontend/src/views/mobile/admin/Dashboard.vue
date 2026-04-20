<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Admin Mobile</p>
        <h1>{{ dashboardTitle }}</h1>
        <p>{{ dashboardSubtitle }}</p>
      </div>
      <button class="refresh-btn" type="button" :disabled="loading" @click="loadPageData">
        刷新
      </button>
    </section>

    <section class="metric-grid">
      <article v-for="card in metricCards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.tip }}</small>
      </article>
    </section>

    <section class="quick-grid">
      <button v-for="item in quickActions" :key="item.path" class="quick-card" type="button" @click="router.push(item.path)">
        <strong>{{ item.title }}</strong>
        <span>{{ item.description }}</span>
      </button>
    </section>

    <section v-if="pendingApprovals.length" class="panel-card">
      <header class="panel-head">
        <h2>待办事项</h2>
        <span>{{ pendingApprovals.length }} 项</span>
      </header>
      <div class="task-list">
        <button
          v-for="item in pendingApprovals"
          :key="item.label"
          class="task-card"
          type="button"
          @click="openTask(item)"
        >
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.description }}</p>
          </div>
          <span class="task-value">{{ item.value }}</span>
        </button>
      </div>
    </section>

    <section class="panel-card">
      <header class="panel-head">
        <h2>最新公告</h2>
        <button class="text-btn" type="button" @click="router.push('/m/admin/notices')">更多</button>
      </header>
      <div v-if="notices.length" class="notice-list">
        <article v-for="item in notices" :key="item.id" class="notice-card">
          <strong>{{ item.title || '公告' }}</strong>
          <p>{{ item.content || '暂无内容' }}</p>
          <span>{{ formatDate(item.publishTime || item.createTime) }}</span>
        </article>
      </div>
      <el-empty v-else description="暂无公告" :image-size="72" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLatestNotices } from '@/api/notices'
import { getUnreadNotificationCount } from '@/api/notifications'
import { getOverviewStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import { mapPathToSurface } from '@/utils/portal'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const overview = ref({})
const notices = ref([])
const unreadCount = ref(0)

const isSchoolManager = computed(() => Boolean(userStore.userInfo?.schoolDirector || userStore.userRole === 'super_admin'))
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager))

const dashboardTitle = computed(() => {
  if (isSchoolManager.value) {
    return '学校实验室总览'
  }
  if (isCollegeManager.value) {
    return `${userStore.userInfo?.collegeName || userStore.userInfo?.college || '学院'}管理看板`
  }
  return `${overview.value.scopeName || userStore.userInfo?.labName || '实验室'}运营看板`
})

const dashboardSubtitle = computed(() => {
  if (isSchoolManager.value) {
    return '关注学院、实验室、成员和审批趋势。'
  }
  if (isCollegeManager.value) {
    return '聚焦本学院实验室建设、教师和成员数据。'
  }
  return '移动端优先处理实验室运营、待办和公告。'
})

const pendingApprovals = computed(() => overview.value.pendingApprovals || [])
const quickActions = computed(() =>
  [
    userStore.hasPermission('lab:manage') ? { path: '/m/admin/labs', title: '实验室管理', description: '查看实验室、提交或处理基础信息' } : null,
    userStore.hasPermission('lab:apply:audit') ? { path: '/m/admin/applications', title: '成员申请审批', description: '处理入组申请与简历查看' } : null,
    userStore.hasPermission('lab:create:audit') ? { path: '/m/admin/create-applies', title: '实验室创建审批', description: '教师创建实验室的审批流程' } : null,
    userStore.hasPermission('teacher:register:audit') ? { path: '/m/admin/teacher-register-applies', title: '教师注册审批', description: '教师账号注册的学院或学校审核' } : null,
    userStore.hasPermission('attendance:task:manage') || userStore.hasPermission('attendance:record:manage')
      ? { path: '/m/admin/attendance', title: '考勤管理', description: '任务、动态码、请假和记录修正' }
      : null,
    userStore.hasPermission('member:manage') ? { path: '/m/admin/members', title: '成员管理', description: '查看成员并进行负责人或移除操作' } : null,
    userStore.hasPermission('profile:review') ? { path: '/m/admin/profiles', title: '档案审核', description: '查看资料详情并完成通过或驳回' } : null,
    userStore.hasPermission('device:manage') ? { path: '/m/admin/devices', title: '设备中心', description: '设备台账、借还记录和维修处理' } : null,
    userStore.hasPermission('notice:manage') ? { path: '/m/admin/notices-manage', title: '公告管理', description: '发布、编辑和删除学校、学院或实验室公告' } : null,
    { path: '/m/admin/notifications', title: '消息中心', description: `未读 ${unreadCount.value} 条，统一处理提醒` },
    { path: '/m/admin/notices', title: '公告查看', description: '快速浏览最新公告内容' }
  ].filter(Boolean)
)

const metricCards = computed(() => {
  if (isSchoolManager.value) {
    return [
      { label: '学院数', value: overview.value.collegeCount ?? 0, tip: '当前接入学院' },
      { label: '实验室数', value: overview.value.labCount ?? 0, tip: '平台实验室规模' },
      { label: '教师数', value: overview.value.teacherCount ?? 0, tip: '参与指导的教师' },
      { label: '待处理', value: overview.value.pendingApprovalTotal ?? 0, tip: '学校层面的审批事项' }
    ]
  }

  if (isCollegeManager.value) {
    return [
      { label: '实验室数', value: overview.value.labCount ?? 0, tip: '学院范围内实验室' },
      { label: '教师数', value: overview.value.teacherCount ?? 0, tip: '学院指导教师' },
      { label: '正式成员', value: overview.value.formalMemberCount ?? 0, tip: '学院成员规模' },
      { label: '待处理', value: overview.value.pendingApprovalTotal ?? 0, tip: '学院层审批事项' }
    ]
  }

  return [
    { label: '成员规模', value: overview.value.memberCount ?? 0, tip: '当前实验室成员' },
    { label: '开放计划', value: overview.value.openPlanCount ?? 0, tip: '仍在开放的招新计划' },
    { label: '待处理', value: overview.value.pendingApprovalTotal ?? 0, tip: '需要处理的事项' },
    { label: '考勤率', value: `${overview.value.attendanceRate ?? 0}%`, tip: '最近考勤出勤率' }
  ]
})

const loadPageData = async () => {
  loading.value = true
  try {
    const [overviewRes, noticeRes, unreadRes] = await Promise.all([
      getOverviewStatistics(),
      getLatestNotices({ size: 4 }),
      getUnreadNotificationCount()
    ])
    overview.value = overviewRes.data || {}
    notices.value = noticeRes.data || []
    unreadCount.value = unreadRes.data?.unreadCount || 0
  } finally {
    loading.value = false
  }
}

const openTask = async (item) => {
  const route = item?.route ? mapPathToSurface(item.route, 'mobile') : '/m/admin/labs'
  await router.push(route)
}

const formatDate = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  loadPageData()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card {
  border-radius: 24px;
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.95), rgba(15, 118, 110, 0.9));
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.8;
}

.hero-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 1.1;
}

.hero-card p {
  margin: 0;
  color: rgba(226, 232, 240, 0.9);
  line-height: 1.6;
}

.refresh-btn {
  height: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
  border-radius: 14px;
  padding: 10px 14px;
}

.metric-grid,
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.quick-card,
.panel-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.metric-card {
  padding: 14px;
  display: grid;
  gap: 6px;
}

.metric-card span,
.metric-card small {
  color: #64748b;
}

.metric-card strong {
  color: #0f172a;
  font-size: 24px;
}

.quick-card {
  padding: 14px;
  text-align: left;
  display: grid;
  gap: 6px;
}

.quick-card strong {
  color: #0f172a;
  font-size: 15px;
}

.quick-card span {
  color: #64748b;
  line-height: 1.6;
}

.panel-card {
  padding: 14px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.panel-head span,
.text-btn {
  color: #0f766e;
}

.text-btn {
  border: 0;
  background: transparent;
  font-weight: 700;
}

.task-list,
.notice-list {
  display: grid;
  gap: 10px;
}

.task-card,
.notice-card {
  border-radius: 16px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
}

.task-card strong,
.notice-card strong {
  color: #0f172a;
}

.task-card p,
.notice-card p,
.notice-card span {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.task-value {
  color: #0f766e;
  font-size: 24px;
  font-weight: 800;
}

.notice-card {
  display: grid;
  gap: 8px;
}

@media (max-width: 480px) {
  .metric-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>

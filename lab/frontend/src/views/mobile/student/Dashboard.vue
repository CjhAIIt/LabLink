<template>
  <MobilePageContainer>
    <section class="student-home-hero">
      <div>
        <span class="mobile-kicker">LabLink Portal</span>
        <h1>{{ userStore.realName || '同学' }}，今天从哪里开始？</h1>
        <p>实验室申请、考勤、公告和消息集中在这里，手机端优先处理高频事项。</p>
      </div>
      <button class="student-home-refresh" type="button" :disabled="loading" @click="refresh">刷新</button>
    </section>

    <section class="student-home-metrics">
      <MobileStatCard label="实验室" :value="stats.labCount ?? 0" description="可浏览团队" />
      <MobileStatCard label="学院" :value="stats.collegeCount ?? 0" description="已接入学院" accent="#19A7B8" />
      <MobileStatCard label="我的申请" :value="myApplyTotal" description="申请记录" accent="#D9861F" />
      <MobileStatCard label="未读消息" :value="unreadCount" description="消息与待办" accent="#DC3D43" />
    </section>

    <section class="student-home-quick">
      <button v-for="item in quickActions" :key="item.path" class="student-home-quick__card" type="button" @click="router.push(item.path)">
        <el-icon :size="22"><component :is="item.icon" /></el-icon>
        <strong>{{ item.title }}</strong>
        <span>{{ item.description }}</span>
      </button>
    </section>

    <section class="student-home-panel">
      <header>
        <div>
          <h2>最新公告</h2>
          <p>学校、学院和实验室的重要通知。</p>
        </div>
        <button class="text-btn" type="button" @click="router.push('/m/student/notices')">更多</button>
      </header>
      <div v-if="latestNotices.length" class="notice-list">
        <button v-for="item in latestNotices" :key="item.id" class="notice-card" type="button" @click="openNotice(item)">
          <div>
            <strong>{{ item.title || '公告' }}</strong>
            <span>{{ formatDate(item.publishTime || item.createTime || item.createdAt) }}</span>
          </div>
          <el-icon :size="18"><ArrowRight /></el-icon>
        </button>
      </div>
      <MobileEmptyState
        v-else
        icon="Bell"
        title="暂无最新公告"
        description="有新的通知时会在这里优先展示。"
      />
    </section>

    <MobileActionSheet v-model="drawerVisible" title="公告详情">
      <article class="notice-sheet">
        <h3>{{ activeNotice?.title || '公告详情' }}</h3>
        <span>{{ formatDate(activeNotice?.publishTime || activeNotice?.createTime || activeNotice?.createdAt) }}</span>
        <p>{{ activeNotice?.content || '暂无内容' }}</p>
      </article>
    </MobileActionSheet>
  </MobilePageContainer>
</template>

<script setup>
import {
  ArrowRight,
  Calendar,
  ChatDotRound,
  EditPen,
  Message,
  OfficeBuilding,
  School,
  Tickets,
  User
} from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabStats } from '@/api/lab'
import { getMyLabApplyPage } from '@/api/labApplies'
import { getLatestNotices } from '@/api/notices'
import { getUnreadNotificationCount } from '@/api/notifications'
import MobileActionSheet from '@/components/mobile/MobileActionSheet.vue'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatCard from '@/components/mobile/MobileStatCard.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const stats = ref({})
const myApplyTotal = ref(0)
const unreadCount = ref(0)
const latestNotices = ref([])
const drawerVisible = ref(false)
const activeNotice = ref(null)
const hasLabAccess = computed(() => Boolean(userStore.userInfo?.labId))

const quickActions = computed(() =>
  [
    { path: '/m/student/labs', title: '实验室广场', description: '浏览团队并提交申请', icon: OfficeBuilding },
    { path: '/m/student/applications', title: '申请记录', description: '查看审核进度和反馈', icon: Tickets },
    hasLabAccess.value ? { path: '/m/student/attendance', title: '考勤打卡', description: '签到码、历史记录和异常', icon: Calendar } : null,
    hasLabAccess.value ? { path: '/m/student/my-lab', title: '我的实验室', description: '成员、资料和快捷入口', icon: School } : null,
    { path: '/m/student/exam-center', title: '笔试中心', description: '查看笔试安排与结果', icon: EditPen },
    { path: '/m/student/ai-interview', title: 'AI 面试', description: '模拟练习与正式面试', icon: ChatDotRound },
    { path: '/m/student/notifications', title: '消息中心', description: `未读 ${unreadCount.value} 条`, icon: Message },
    { path: '/m/student/profile', title: '个人中心', description: '简历和成员资料', icon: User }
  ].filter(Boolean)
)

const refresh = async () => {
  loading.value = true
  try {
    const [statsRes, applyRes, noticesRes, unreadRes] = await Promise.all([
      getLabStats(),
      getMyLabApplyPage({ pageNum: 1, pageSize: 1 }),
      getLatestNotices({ size: 4 }),
      getUnreadNotificationCount()
    ])
    stats.value = statsRes.data || {}
    myApplyTotal.value = Number(applyRes.data?.total || 0)
    latestNotices.value = noticesRes.data || []
    unreadCount.value = unreadRes.data?.unreadCount || 0
  } finally {
    loading.value = false
  }
}

const openNotice = (item) => {
  activeNotice.value = item
  drawerVisible.value = true
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.student-home-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 52%, #19a7b8);
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

.student-home-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.student-home-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.student-home-refresh {
  width: fit-content;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  font-weight: 900;
}

.student-home-metrics,
.student-home-quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.student-home-quick__card,
.student-home-panel,
.notice-card {
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.student-home-quick__card {
  min-height: 132px;
  padding: 15px;
  display: grid;
  align-content: start;
  gap: 8px;
  text-align: left;
  color: #176b9a;
}

.student-home-quick__card strong {
  color: #172033;
}

.student-home-quick__card span {
  color: #64748b;
  line-height: 1.5;
}

.student-home-panel {
  padding: 17px;
  display: grid;
  gap: 14px;
}

.student-home-panel header,
.notice-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.student-home-panel h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.student-home-panel header p,
.notice-card span {
  margin: 5px 0 0;
  color: #64748b;
}

.text-btn {
  min-height: 36px;
  border: 0;
  background: transparent;
  color: #176b9a;
  font-weight: 900;
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-card {
  width: 100%;
  padding: 14px;
  text-align: left;
}

.notice-card strong {
  color: #172033;
}

.notice-sheet h3 {
  margin: 0 0 8px;
  color: #172033;
  font-size: 20px;
}

.notice-sheet span {
  color: #64748b;
}

.notice-sheet p {
  margin: 14px 0 0;
  color: #334155;
  line-height: 1.82;
  white-space: pre-wrap;
}

@media (max-width: 430px) {
  .student-home-metrics,
  .student-home-quick {
    grid-template-columns: 1fr;
  }
}
</style>

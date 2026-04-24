<template>
  <MobilePageContainer>
    <MobileEmptyState
      v-if="!userStore.userInfo?.labId"
      icon="OfficeBuilding"
      title="还没有加入实验室"
      description="去实验室广场看看适合你的团队，提交简历后即可申请加入。"
    >
      <el-button type="primary" @click="router.push('/m/student/labs')">前往实验室广场</el-button>
    </MobileEmptyState>

    <template v-else>
      <section v-if="lab" class="my-lab-hero">
        <div>
          <span class="mobile-kicker">My Lab</span>
          <h1>{{ lab.labName }}</h1>
          <p>{{ lab.labDesc || lab.basicInfo || '暂无实验室简介。' }}</p>
        </div>
        <button class="my-lab-refresh" type="button" :disabled="loading" @click="loadLabData">刷新</button>
      </section>

      <section v-if="lab" class="my-lab-overview">
        <MobileStatCard label="我的角色" :value="roleLabel(myMemberRole)" description="实验室内身份" />
        <MobileStatCard label="成员人数" :value="members.length" description="当前活跃成员" accent="#19A7B8" />
        <MobileStatCard label="指导教师" :value="lab.teacherName || '-'" description="主要联系人" accent="#16A97A" />
        <MobileStatCard label="实验室位置" :value="lab.location || '-'" description="线下活动地点" accent="#D9861F" />
      </section>

      <section class="my-lab-action-grid">
        <button v-for="item in quickActions" :key="item.path" class="my-lab-action-card" type="button" @click="router.push(item.path)">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </button>
      </section>

      <section class="my-lab-panel">
        <header>
          <div>
            <h2>成员列表</h2>
            <p>{{ members.length }} 位成员，便于快速了解团队构成。</p>
          </div>
        </header>

        <div v-if="members.length" class="member-list">
          <article v-for="item in members" :key="item.id || item.userId || item.studentId" class="member-card">
            <el-avatar :size="42">{{ (item.realName || item.username || 'U').charAt(0) }}</el-avatar>
            <div>
              <strong>{{ item.realName || '未命名成员' }}</strong>
              <p>{{ item.studentId || item.username || '-' }} · {{ item.major || '专业待完善' }}</p>
            </div>
            <MobileStatusTag type="primary" :label="roleLabel(item.memberRole)" />
          </article>
        </div>

        <MobileEmptyState
          v-else
          icon="User"
          title="暂无成员信息"
          description="成员同步后会在这里展示。"
        />
      </section>
    </template>
  </MobilePageContainer>
</template>

<script setup>
import { Bell, Calendar, Files, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabById } from '@/api/lab'
import { getActiveLabMembers } from '@/api/labMembers'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatCard from '@/components/mobile/MobileStatCard.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const lab = ref(null)
const members = ref([])

const myMemberRole = computed(() => {
  const userId = userStore.userInfo?.id || userStore.userInfo?.userId
  return members.value.find((item) => Number(item.userId || item.id) === Number(userId))?.memberRole || userStore.userInfo?.memberRole || 'member'
})

const quickActions = [
  { path: '/m/student/attendance', title: '今日考勤', desc: '快速打卡与查看历史', icon: Calendar },
  { path: '/m/student/space', title: '资料入口', desc: '实验室资料与文件', icon: Files },
  { path: '/m/student/notices', title: '最新公告', desc: '查看实验室通知', icon: Bell },
  { path: '/m/student/profile', title: '成员资料', desc: '简历与资料审核', icon: User }
]

const loadLabData = async () => {
  if (!userStore.userInfo?.labId) {
    return
  }
  loading.value = true
  try {
    const [labRes, memberRes] = await Promise.all([
      getLabById(userStore.userInfo.labId),
      getActiveLabMembers({ labId: userStore.userInfo.labId })
    ])
    lab.value = labRes.data || null
    members.value = memberRes.data || []
  } finally {
    loading.value = false
  }
}

const roleLabel = (role) => ({
  lab_admin: '管理员',
  lab_leader: '负责人',
  leader: '负责人',
  member: '成员'
}[role] || '成员')

onMounted(() => {
  loadLabData()
})
</script>

<style scoped>
.my-lab-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 55%, #19a7b8);
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

.my-lab-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.my-lab-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.my-lab-refresh {
  width: fit-content;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  font-weight: 900;
}

.my-lab-overview,
.my-lab-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.my-lab-action-card,
.my-lab-panel,
.member-card {
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.my-lab-action-card {
  min-height: 132px;
  padding: 15px;
  display: grid;
  align-content: start;
  gap: 8px;
  text-align: left;
  color: #176b9a;
}

.my-lab-action-card strong {
  color: #172033;
}

.my-lab-action-card span {
  color: #64748b;
  line-height: 1.5;
}

.my-lab-panel {
  padding: 17px;
  display: grid;
  gap: 14px;
}

.my-lab-panel header h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.my-lab-panel header p {
  margin: 5px 0 0;
  color: #64748b;
}

.member-list {
  display: grid;
  gap: 10px;
}

.member-card {
  padding: 13px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.member-card strong {
  color: #172033;
}

.member-card p {
  margin: 5px 0 0;
  color: #64748b;
  line-height: 1.4;
}

@media (max-width: 430px) {
  .my-lab-overview,
  .my-lab-action-grid {
    grid-template-columns: 1fr;
  }

  .member-card {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .member-card .mobile-status-tag {
    grid-column: 2;
  }
}
</style>

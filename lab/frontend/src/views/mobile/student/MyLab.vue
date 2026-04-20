<template>
  <div class="m-page">
    <section v-if="!userStore.userInfo?.labId" class="empty-card">
      <el-empty description="你还没有加入实验室，可以先去实验室广场投递申请。" :image-size="88">
        <template #default>
          <el-button type="primary" @click="router.push('/m/student/labs')">去实验室广场</el-button>
        </template>
      </el-empty>
    </section>

    <template v-else>
      <section v-if="lab" class="hero-card">
        <div>
          <p class="eyebrow">My Lab</p>
          <h1>{{ lab.labName }}</h1>
          <p>{{ lab.labDesc || lab.basicInfo || '暂无实验室简介' }}</p>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="loadLabData">刷新</button>
      </section>

      <section v-if="lab" class="grid-card">
        <article class="info-card"><span>实验室编码</span><strong>{{ lab.labCode || `#${lab.id}` }}</strong></article>
        <article class="info-card"><span>指导教师</span><strong>{{ lab.teacherName || '-' }}</strong></article>
        <article class="info-card"><span>位置</span><strong>{{ lab.location || '-' }}</strong></article>
        <article class="info-card"><span>成员人数</span><strong>{{ members.length }}</strong></article>
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>成员列表</h2>
          <span>{{ members.length }} 人</span>
        </header>
        <div v-if="members.length" class="member-list">
          <article v-for="item in members" :key="item.id || item.userId || item.studentId" class="member-card">
            <div>
              <strong>{{ item.realName || '未命名成员' }}</strong>
              <p>{{ item.studentId || item.username || '-' }}</p>
            </div>
            <div class="member-meta">
              <span>{{ roleLabel(item.memberRole) }}</span>
              <small>{{ item.major || '未填写专业' }}</small>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无成员信息" :image-size="80" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabById } from '@/api/lab'
import { getActiveLabMembers } from '@/api/labMembers'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const lab = ref(null)
const members = ref([])

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
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card,
.grid-card,
.panel-card,
.empty-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(14, 116, 144, 0.88));
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
  font-size: 22px;
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

.grid-card {
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-card {
  padding: 12px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  display: grid;
  gap: 4px;
}

.info-card span,
.member-card p,
.member-meta small,
.panel-head span {
  color: #64748b;
}

.info-card strong,
.panel-head h2,
.member-card strong,
.member-meta span {
  color: #0f172a;
}

.panel-card {
  padding: 14px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.member-list {
  display: grid;
  gap: 10px;
}

.member-card {
  border-radius: 16px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.member-card p {
  margin: 6px 0 0;
}

.member-meta {
  text-align: right;
  display: grid;
  gap: 4px;
}

@media (max-width: 480px) {
  .grid-card {
    grid-template-columns: 1fr;
  }
}
</style>

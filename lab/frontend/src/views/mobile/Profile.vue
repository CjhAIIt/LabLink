<template>
  <div class="m-page">
    <section class="card hero">
      <div class="hero-left">
        <el-avatar :size="54">{{ userInitial }}</el-avatar>
        <div class="hero-text">
          <strong>{{ userStore.realName || userStore.userName || '用户' }}</strong>
          <span class="muted">{{ roleLabel }}</span>
        </div>
      </div>
      <el-button plain :loading="loading" @click="refresh">刷新</el-button>
    </section>

    <section class="card">
      <div class="row">
        <span class="label">账号</span>
        <span class="value">{{ userStore.userName || '-' }}</span>
      </div>
      <div class="row">
        <span class="label">姓名</span>
        <span class="value">{{ userStore.realName || '-' }}</span>
      </div>
      <div class="row">
        <span class="label">学院</span>
        <span class="value">{{ userStore.userInfo?.collegeName || '-' }}</span>
      </div>
      <div class="row">
        <span class="label">实验室</span>
        <span class="value">{{ userStore.userInfo?.labId ? `#${userStore.userInfo.labId}` : '未加入' }}</span>
      </div>
    </section>

    <section class="card actions">
      <button class="action" type="button" @click="router.push(resolveHome())">
        <div class="action-main">
          <strong>返回首页</strong>
          <span>继续浏览功能</span>
        </div>
        <el-icon :size="18" class="arrow"><ArrowRight /></el-icon>
      </button>
      <button class="action danger" type="button" @click="logout">
        <div class="action-main">
          <strong>退出登录</strong>
          <span>清除本地登录状态</span>
        </div>
        <el-icon :size="18" class="arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'U')
const roleLabel = computed(() => {
  const role = userStore.userPortalRole
  if (role === 'admin') return '管理员'
  if (role === 'teacher') return '教师'
  if (role === 'student') return '学生'
  return role || '用户'
})

const resolveHome = () => {
  const role = userStore.userPortalRole
  if (role === 'teacher') return '/m/teacher/dashboard'
  return '/m/student/dashboard'
}

const refresh = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/access/profile')
    userStore.setUserInfo(res.data || {})
  } finally {
    loading.value = false
  }
}

const logout = async () => {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
  userStore.clearUserInfo()
  await router.push('/login')
}

onMounted(() => {
  if (!userStore.userInfo?.id) {
    refresh()
  }
})
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 14px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.hero-text strong {
  color: #0f172a;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.muted {
  color: #64748b;
  font-size: 12px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.row:last-child {
  border-bottom: 0;
}

.label {
  color: #64748b;
  font-size: 13px;
}

.value {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions {
  padding: 0;
  overflow: hidden;
}

.action {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 0;
  background: transparent;
  text-align: left;
  border-top: 1px solid rgba(226, 232, 240, 0.7);
}

.action:first-child {
  border-top: 0;
}

.action-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-main strong {
  color: #0f172a;
  font-size: 14px;
}

.action-main span {
  color: #64748b;
  font-size: 12px;
}

.action.danger .action-main strong,
.action.danger .arrow {
  color: #b91c1c;
}

.arrow {
  color: #94a3b8;
}
</style>

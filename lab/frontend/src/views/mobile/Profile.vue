<template>
  <div class="m-page">
    <section class="profile-hero">
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
        <span class="value">{{ userStore.userInfo?.college || '-' }}</span>
      </div>
      <div v-if="userStore.userInfo?.major" class="row">
        <span class="label">专业</span>
        <span class="value">{{ userStore.userInfo.major }}</span>
      </div>
      <div v-if="userStore.userInfo?.email" class="row">
        <span class="label">邮箱</span>
        <span class="value">{{ userStore.userInfo.email }}</span>
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
  if (role === 'admin') return '/m/admin/dashboard'
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


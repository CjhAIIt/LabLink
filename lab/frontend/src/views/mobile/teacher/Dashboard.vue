<template>
  <div class="m-page">
    <section class="hero hero--top">
      <div class="hero-text">
        <p class="hero-eyebrow">教师端</p>
        <h1 class="hero-title">你好，{{ userStore.realName || '老师' }}</h1>
        <p class="hero-sub">发起实验室创建申请，并查看审批进度。</p>
      </div>
      <button class="hero-refresh" type="button" :disabled="loading" @click="refresh">
        <el-icon :size="18"><Refresh /></el-icon>
      </button>
    </section>

    <section class="grid">
      <div class="stat-card">
        <p class="stat-label">创建申请</p>
        <strong class="stat-value">{{ createApplyTotal ?? '--' }}</strong>
      </div>
      <div class="stat-card">
        <p class="stat-label">我的学院</p>
        <strong class="stat-value">{{ userStore.userInfo?.collegeName || userStore.userInfo?.college || '--' }}</strong>
      </div>
    </section>

    <section class="quick">
      <button class="quick-card" type="button" @click="router.push('/m/teacher/create-applies')">
        <div class="quick-icon">
          <el-icon :size="22"><Edit /></el-icon>
        </div>
        <div class="quick-body">
          <strong>实验室创建申请</strong>
          <span>发起申请 / 查看进度</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
      <button class="quick-card" type="button" @click="router.push('/m/teacher/notices')">
        <div class="quick-icon notices">
          <el-icon :size="22"><Bell /></el-icon>
        </div>
        <div class="quick-body">
          <strong>公告中心</strong>
          <span>最新通知与提醒</span>
        </div>
        <el-icon :size="18" class="quick-arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<script setup>
import { ArrowRight, Bell, Edit, Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLabCreateApplyPage } from '@/api/labCreateApplies'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const createApplyTotal = ref(null)

const refresh = async () => {
  loading.value = true
  try {
    const res = await getLabCreateApplyPage({ pageNum: 1, pageSize: 1 })
    const page = res.data || {}
    createApplyTotal.value = page.total ?? page.totalCount ?? page.count ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refresh()
})
</script>


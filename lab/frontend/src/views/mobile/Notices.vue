<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="keyword" clearable placeholder="搜索公告标题" @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section class="card-list" v-loading="loading">
      <button v-for="item in notices" :key="item.id" class="notice-card" type="button" @click="openNotice(item)">
        <div class="card-head">
          <strong>{{ item.title || '公告' }}</strong>
          <span>{{ formatDate(item.publishTime || item.createTime || item.createdAt) }}</span>
        </div>
        <p>{{ item.content || '暂无内容' }}</p>
      </button>

      <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="notices.length" class="muted">已经到底了</span>
    </div>

    <el-drawer v-model="drawerVisible" :with-header="false" size="92%">
      <div class="drawer-body">
        <div class="drawer-head">
          <strong>{{ activeNotice?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="drawerVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ formatDateTime(activeNotice?.publishTime || activeNotice?.createTime || activeNotice?.createdAt) }}</p>
        <div class="drawer-content">{{ activeNotice?.content || '暂无内容' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { Close, Search } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { getNoticePage } from '@/api/notices'

const keyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const notices = ref([])
const drawerVisible = ref(false)
const activeNotice = ref(null)

const hasMore = computed(() => notices.value.length < total.value)

const fetchPage = async (page) => {
  const response = await getNoticePage({ pageNum: page, pageSize, keyword: keyword.value || undefined })
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    notices.value = await fetchPage(1)
  } finally {
    loading.value = false
  }
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchPage(nextPage)
    pageNum.value = nextPage
    notices.value = notices.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const openNotice = (notice) => {
  activeNotice.value = notice
  drawerVisible.value = true
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${formatDate(value)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

watch(
  () => keyword.value,
  (value, oldValue) => {
    if (!value && oldValue) {
      resetAndFetch()
    }
  }
)

onMounted(() => {
  resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.notice-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.card-list {
  display: grid;
  gap: 10px;
}

.notice-card {
  padding: 14px;
  text-align: left;
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head strong {
  color: #0f172a;
  font-size: 15px;
}

.card-head span,
.notice-card p,
.drawer-meta,
.muted {
  color: #64748b;
}

.notice-card p,
.drawer-content {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.load-more {
  display: flex;
  justify-content: center;
}

.drawer-body {
  padding: 14px 14px calc(14px + env(safe-area-inset-bottom));
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.drawer-head strong {
  color: #0f172a;
  font-size: 18px;
}

.drawer-close {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
}

.drawer-content {
  color: #334155;
}
</style>

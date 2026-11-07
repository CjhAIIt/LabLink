<template>
  <div class="m-page">
    <section class="toolbar">
      <el-input v-model="keyword" placeholder="搜索公告标题" clearable @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section v-loading="loading" class="list">
      <button v-for="item in notices" :key="item.id" class="card" type="button" @click="open(item)">
        <div class="card-head">
          <strong class="title">{{ item.title || '公告' }}</strong>
          <span class="time">{{ formatTime(item.createTime || item.createdAt) }}</span>
        </div>
        <p class="preview">{{ item.content || '暂无内容' }}</p>
      </button>

      <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="80" />

      <div class="load-more">
        <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
        <span v-else-if="notices.length" class="no-more">已到底</span>
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">{{ active?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="drawerVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ formatTime(active?.createTime || active?.createdAt) }}</p>
        <div class="drawer-body">{{ active?.content || '暂无内容' }}</div>
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
const active = ref(null)

const hasMore = computed(() => notices.value.length < (total.value || 0))

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const open = (notice) => {
  active.value = notice
  drawerVisible.value = true
}

const fetchPage = async (page) => {
  const res = await getNoticePage({
    pageNum: page,
    pageSize,
    keyword: keyword.value?.trim() || undefined
  })
  const pageData = res.data || {}
  total.value = pageData.total || 0
  return pageData.records || []
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
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const next = pageNum.value + 1
    const records = await fetchPage(next)
    pageNum.value = next
    notices.value = notices.value.concat(records)
  } finally {
    loadingMore.value = false
  }
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
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.title {
  color: #0f172a;
  font-size: 15px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  color: #64748b;
  font-size: 12px;
  flex-shrink: 0;
}

.preview {
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.load-more {
  padding: 10px 0 2px 0;
  display: flex;
  justify-content: center;
}

.no-more {
  font-size: 12px;
  color: #94a3b8;
}

.drawer {
  padding: 14px 14px calc(14px + env(safe-area-inset-bottom)) 14px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.drawer-title {
  font-size: 16px;
  color: #0f172a;
}

.drawer-close {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.drawer-meta {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 10px;
}

.drawer-body {
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>

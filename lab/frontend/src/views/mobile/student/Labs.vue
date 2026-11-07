<template>
  <div class="m-page">
    <section class="search">
      <el-input v-model="keyword" placeholder="搜索实验室名称 / 方向" clearable @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" :loading="loading" @click="resetAndFetch">搜索</el-button>
    </section>

    <section class="list">
      <button
        v-for="lab in labs"
        :key="lab.id"
        class="lab-card"
        type="button"
        @click="router.push(`/m/student/labs/${lab.id}`)"
      >
        <div class="lab-head">
          <strong class="lab-name">{{ lab.labName || '未命名实验室' }}</strong>
          <span class="lab-code">{{ lab.labCode || `#${lab.id}` }}</span>
        </div>
        <p class="lab-desc">{{ lab.labDesc || lab.basicInfo || '暂无介绍' }}</p>
        <div class="lab-meta">
          <span class="meta-chip">
            <el-icon :size="16"><User /></el-icon>
            <span>{{ lab.teacherName || '待维护' }}</span>
          </span>
          <span v-if="lab.location" class="meta-chip">
            <el-icon :size="16"><Location /></el-icon>
            <span>{{ lab.location }}</span>
          </span>
        </div>
      </button>

      <el-empty v-if="!loading && labs.length === 0" description="暂无实验室" :image-size="80" />

      <div class="load-more">
        <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
        <span v-else-if="labs.length" class="no-more">已到底</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { Location, Search, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLabPage } from '@/api/lab'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)

const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const labs = ref([])

const hasMore = computed(() => labs.value.length < (total.value || 0))

const fetchPage = async (page) => {
  const res = await getLabPage({
    pageNum: page,
    pageSize,
    keyword: keyword.value?.trim() || undefined
  })
  const pageData = res.data || {}
  const records = pageData.records || pageData.list || pageData.items || pageData.data || []
  const newTotal = pageData.total ?? pageData.totalCount ?? pageData.count ?? records.length
  total.value = Number(newTotal) || 0
  return records
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    const records = await fetchPage(1)
    labs.value = Array.isArray(records) ? records : []
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
    labs.value = labs.value.concat(Array.isArray(records) ? records : [])
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

.search {
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

.lab-card {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lab-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.lab-name {
  font-size: 15px;
  color: #0f172a;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lab-code {
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}

.lab-desc {
  color: #334155;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lab-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff;
  color: #475569;
  font-size: 12px;
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
</style>

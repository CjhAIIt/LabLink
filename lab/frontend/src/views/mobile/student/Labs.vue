<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="keyword" clearable placeholder="搜索实验室名称或研究方向" @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" :loading="loading" @click="resetAndFetch">搜索</el-button>
    </section>

    <section v-if="!hasResume" class="notice-card warning">
      <div>
        <strong>先上传简历，再报名实验室</strong>
        <p>报名入口已经恢复。请先在个人资料页上传简历，并可从这里直接下载模板。</p>
      </div>
      <div class="notice-actions">
        <el-button text @click="router.push('/m/student/profile')">去上传</el-button>
        <a class="file-link" :href="resumeTemplateUrl" download>下载模板</a>
      </div>
    </section>

    <section class="card-list">
      <button v-for="lab in labs" :key="lab.id" class="lab-card" type="button" @click="router.push(`/m/student/labs/${lab.id}`)">
        <div class="card-head">
          <strong>{{ lab.labName || '未命名实验室' }}</strong>
          <span>{{ lab.labCode || `#${lab.id}` }}</span>
        </div>
        <p class="description">{{ lab.labDesc || lab.basicInfo || '暂无实验室介绍' }}</p>
        <div class="meta-row">
          <span>{{ lab.teacherName || '待完善' }}</span>
          <span>{{ lab.location || '位置待完善' }}</span>
        </div>
      </button>

      <el-empty v-if="!loading && labs.length === 0" description="暂无实验室" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="labs.length" class="muted">已经到底了</span>
    </div>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLabPage } from '@/api/lab'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const labs = ref([])
const resumeTemplateUrl = '/templates/member-application-template.docx'

const hasResume = computed(() => Boolean(userStore.userInfo?.resume))
const hasMore = computed(() => labs.value.length < total.value)

const fetchPage = async (page) => {
  const response = await getLabPage({ pageNum: page, pageSize, keyword: keyword.value || undefined })
  const pageData = response.data || {}
  total.value = Number(pageData.total || 0)
  return pageData.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    labs.value = await fetchPage(1)
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
    labs.value = labs.value.concat(list)
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
  display: grid;
  gap: 14px;
}

.toolbar-card,
.notice-card,
.lab-card {
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

.notice-card {
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.notice-card.warning {
  border-color: rgba(245, 158, 11, 0.22);
  background: rgba(255, 251, 235, 0.96);
}

.notice-card strong,
.lab-card strong {
  color: #0f172a;
}

.notice-card p,
.description,
.meta-row,
.card-head span,
.muted {
  color: #64748b;
}

.notice-card p,
.description {
  margin: 6px 0 0;
  line-height: 1.6;
}

.notice-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.file-link {
  color: #d97706;
  text-decoration: none;
  font-weight: 700;
}

.card-list {
  display: grid;
  gap: 10px;
}

.lab-card {
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

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.load-more {
  display: flex;
  justify-content: center;
}
</style>

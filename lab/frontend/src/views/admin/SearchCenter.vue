<template>
  <div class="page-shell">
    <section class="page-hero search-hero">
      <div>
        <p class="eyebrow">综合搜索</p>
        <h1>在一个入口中统一搜索实验室、成员、资料、设备、文件、公告和考勤任务</h1>
        <p class="hero-subtitle">
          搜索结果按类型分组展示，并自动遵循学校、学院和实验室的数据权限范围。
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-chip">统一结果结构</div>
        <div class="hero-chip muted">实验室 / 成员 / 资料 / 设备 / 文件 / 公告 / 考勤</div>
      </div>
    </section>

    <TablePageCard class="search-panel" title="搜索条件" subtitle="综合搜索">
      <div class="search-row">
        <el-input
          v-model="keyword"
          clearable
          size="large"
          placeholder="可按实验室名称、成员姓名、学号、设备编号、文件名、公告标题等搜索"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-if="isSchoolDirector" v-model="searchOptions.collegeId" clearable size="large" placeholder="学院">
          <el-option v-for="item in collegeOptions" :key="item.id" :label="item.collegeName" :value="item.id" />
        </el-select>
        <el-select v-model="searchOptions.limit" size="large" placeholder="每组条数">
          <el-option :value="6" label="前 6 条" />
          <el-option :value="12" label="前 12 条" />
          <el-option :value="20" label="前 20 条" />
        </el-select>
        <el-button type="primary" size="large" :loading="loading" @click="handleSearch">搜索</el-button>
        <el-button size="large" :loading="exportLoading" :disabled="!groups.length" @click="handleExport">导出</el-button>
        <el-button size="large" @click="resetSearch">重置</el-button>
      </div>
    </TablePageCard>

    <section class="metric-grid">
      <MetricCard v-for="card in metricCards" :key="card.label" :label="card.label" :value="card.value" :tip="card.tip" />
    </section>

    <section class="results-grid">
      <TablePageCard
        v-for="group in groups"
        :key="group.type"
        :title="group.label"
        subtitle="分类结果"
        :count-label="group.total"
      >
        <div v-if="group.items?.length" class="result-list">
          <article v-for="item in group.items" :key="`${group.type}-${item.id}`" class="result-item">
            <div class="result-main">
              <strong>{{ item.title }}</strong>
              <p>{{ item.subtitle || '-' }}</p>
            </div>
            <div class="result-side">
              <StatusTag :value="item.status" />
              <router-link v-if="item.extra?.path" class="inline-link" :to="item.extra.path">打开</router-link>
            </div>
          </article>
        </div>
        <el-empty v-else :description="emptyDescription(group.label)" />
      </TablePageCard>
    </section>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import MetricCard from '@/components/common/MetricCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getCollegeOptions } from '@/api/colleges'
import { globalSearch } from '@/api/search'
import { useUserStore } from '@/stores/user'
import { downloadCsv } from '@/utils/export'

const userStore = useUserStore()

const keyword = ref('')
const loading = ref(false)
const exportLoading = ref(false)
const collegeOptions = ref([])
const searchResult = ref(null)

const searchOptions = reactive({
  collegeId: null,
  limit: 6
})

const isSchoolDirector = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const groups = computed(() => searchResult.value?.groups || [])

const metricCards = computed(() => [
  {
    label: '命中总数',
    value: searchResult.value?.total || 0,
    tip: '当前权限范围内匹配到的可见结果'
  },
  {
    label: '结果分组',
    value: groups.value.length,
    tip: '已返回的分类数量'
  },
  {
    label: '搜索关键词',
    value: searchResult.value?.keyword || '-',
    tip: '最近一次执行的查询词'
  },
  {
    label: '数据范围',
    value: searchResult.value?.scopeLevel || '-',
    tip: '本次查询解析出的权限范围'
  }
])

const loadCollegeOptions = async () => {
  if (!isSchoolDirector.value) {
    return
  }
  const response = await getCollegeOptions()
  collegeOptions.value = response.data || []
}

const handleSearch = async () => {
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    ElMessage.warning('请先输入搜索关键词')
    return
  }

  loading.value = true
  try {
    const response = await globalSearch({
      keyword: normalizedKeyword,
      limit: searchOptions.limit,
      collegeId: isSchoolDirector.value ? searchOptions.collegeId || undefined : undefined
    })
    searchResult.value = response.data || null
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  if (!groups.value.length) {
    return
  }

  exportLoading.value = true
  try {
    const rows = [
      ['关键词', searchResult.value?.keyword || '-'],
      ['范围', searchResult.value?.scopeLevel || '-'],
      ['导出时间', new Date().toLocaleString()],
      ['命中总数', searchResult.value?.total || 0],
      []
    ]

    groups.value.forEach((group) => {
      rows.push([group.label, group.total])
      rows.push(['标题', '说明', '状态', '路径'])
      ;(group.items || []).forEach((item) => {
        rows.push([
          item.title || '-',
          item.subtitle || '-',
          item.status || '-',
          item.extra?.path || '-'
        ])
      })
      rows.push([])
    })

    downloadCsv(`lablink-search-${Date.now()}.csv`, rows)
    ElMessage.success('搜索结果已导出')
  } finally {
    exportLoading.value = false
  }
}

const resetSearch = () => {
  keyword.value = ''
  searchOptions.collegeId = null
  searchOptions.limit = 6
  searchResult.value = null
}

const emptyDescription = (label) => `当前权限范围内暂无“${label}”相关结果`

onMounted(() => {
  loadCollegeOptions()
})
</script>

<style scoped>
.search-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(2, 132, 199, 0.88)),
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.18), transparent 32%);
}

.hero-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-chip {
  padding: 10px 14px;
  border-radius: 999px;
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-chip.muted {
  color: rgba(240, 249, 255, 0.88);
}

.search-panel {
  margin-bottom: 18px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) repeat(4, auto);
  gap: 12px;
  align-items: center;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.result-list {
  display: grid;
  gap: 12px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(240, 249, 255, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.result-main {
  display: grid;
  gap: 6px;
}

.result-main p {
  margin: 0;
  color: #64748b;
}

.result-side {
  min-width: 110px;
  display: grid;
  justify-items: end;
  gap: 8px;
}

.inline-link {
  color: #0284c7;
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 900px) {
  .results-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .search-row {
    grid-template-columns: 1fr;
  }

  .hero-side {
    justify-items: start;
  }

  .result-item {
    flex-direction: column;
  }

  .result-side {
    justify-items: start;
  }
}
</style>

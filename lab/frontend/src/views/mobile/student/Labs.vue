<template>
  <MobilePageContainer>
    <section class="labs-mobile-hero">
      <div>
        <span class="mobile-kicker">Lab Square</span>
        <h1>发现适合你的实验室</h1>
        <p>按研究方向、招新状态和指导教师快速筛选，先完善简历会更容易被老师了解。</p>
      </div>
      <button class="labs-mobile-hero__action" type="button" :disabled="loading" @click="resetAndFetch">
        刷新
      </button>
    </section>

    <MobileSearchBar
      v-model="keyword"
      placeholder="搜索实验室名称、教师或研究方向"
      filter-text="筛选"
      @search="resetAndFetch"
      @filter="filterVisible = true"
    />

    <section v-if="!hasResume" class="resume-guide-card">
      <div>
        <strong>申请实验室前请先提交个人简历</strong>
        <p>完善学习经历、项目经历和技能标签后，老师能更快判断你是否适合团队。</p>
      </div>
      <el-button type="primary" @click="router.push('/m/student/profile')">去提交简历</el-button>
    </section>

    <div class="mobile-chip-row" aria-label="实验室分类筛选">
      <button
        v-for="item in categoryTabs"
        :key="item.value"
        class="mobile-chip"
        :class="{ active: category === item.value }"
        type="button"
        @click="category = item.value"
      >
        {{ item.label }}
      </button>
    </div>

    <section v-loading="loading" class="lab-card-flow">
      <article v-for="lab in filteredLabs" :key="lab.id" class="lab-square-card">
        <div class="lab-square-card__top">
          <div class="lab-square-card__title">
            <span>{{ lab.collegeName || lab.college || '学院待完善' }}</span>
            <h2>{{ lab.labName || '未命名实验室' }}</h2>
          </div>
          <MobileStatusTag :type="recruitStatusType(lab)" :label="recruitStatusLabel(lab)" />
        </div>

        <p class="lab-square-card__desc">{{ lab.labDesc || lab.basicInfo || '该实验室暂未填写介绍。' }}</p>

        <div class="lab-square-card__meta">
          <div>
            <span>指导老师</span>
            <strong>{{ lab.teacherName || lab.advisors || '待完善' }}</strong>
          </div>
          <div>
            <span>成员规模</span>
            <strong>{{ lab.currentNum ?? 0 }} / {{ lab.recruitNum ?? '不限' }}</strong>
          </div>
          <div>
            <span>位置</span>
            <strong>{{ lab.location || '待完善' }}</strong>
          </div>
        </div>

        <div class="lab-square-card__tags">
          <span v-for="tag in resolveTags(lab)" :key="tag">{{ tag }}</span>
        </div>

        <div class="lab-square-card__actions">
          <el-button plain @click="router.push(`/m/student/labs/${lab.id}`)">查看详情</el-button>
          <el-button type="primary" :disabled="Boolean(userStore.userInfo?.labId)" @click="router.push(`/m/student/labs/${lab.id}`)">
            {{ userStore.userInfo?.labId ? '已加入实验室' : '申请加入' }}
          </el-button>
        </div>
      </article>

      <MobileEmptyState
        v-if="!loading && filteredLabs.length === 0"
        icon="Search"
        title="暂时没有匹配的实验室"
        description="可以换一个关键词，或先清空筛选条件再看看。"
      >
        <el-button type="primary" plain @click="clearFilters">清空筛选</el-button>
      </MobileEmptyState>
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="labs.length" class="muted">已经到底了</span>
    </div>

    <MobileFilterSheet
      v-model="filterVisible"
      title="筛选实验室"
      description="手机端筛选收纳到这里，列表保持干净可读。"
      @reset="clearFilters"
      @apply="applyFilterSheet"
    >
      <div class="filter-group">
        <strong>招新状态</strong>
        <div class="mobile-chip-row">
          <button
            v-for="item in categoryTabs"
            :key="item.value"
            class="mobile-chip"
            :class="{ active: category === item.value }"
            type="button"
            @click="category = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </MobileFilterSheet>
  </MobilePageContainer>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLabPage } from '@/api/lab'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobileFilterSheet from '@/components/mobile/MobileFilterSheet.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileSearchBar from '@/components/mobile/MobileSearchBar.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const category = ref('all')
const filterVisible = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const labs = ref([])

const hasResume = computed(() => Boolean(String(userStore.userInfo?.resume || '').trim()))
const hasMore = computed(() => labs.value.length < total.value)
const userCollege = computed(() => userStore.userInfo?.collegeName || userStore.userInfo?.college || '')

const categoryTabs = [
  { label: '全部', value: 'all' },
  { label: '招新中', value: 'open' },
  { label: '我的学院', value: 'college' },
  { label: '有名额', value: 'quota' }
]

const filteredLabs = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return labs.value.filter((lab) => {
    const text = [
      lab.labName,
      lab.teacherName,
      lab.advisors,
      lab.requireSkill,
      lab.researchDirection,
      lab.labDesc,
      lab.basicInfo
    ].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !query || text.includes(query)
    const matchesCategory = {
      all: true,
      open: isRecruiting(lab),
      college: userCollege.value && [lab.collegeName, lab.college].includes(userCollege.value),
      quota: Number(lab.recruitNum || 0) <= 0 || Number(lab.currentNum || 0) < Number(lab.recruitNum || 0)
    }[category.value]
    return matchesKeyword && matchesCategory
  })
})

const isRecruiting = (lab) => {
  const status = String(lab.recruitStatus || lab.status || '').toLowerCase()
  if (['open', 'recruiting', 'active', '1'].includes(status)) {
    return true
  }
  const recruitNum = Number(lab.recruitNum || 0)
  return recruitNum <= 0 || Number(lab.currentNum || 0) < recruitNum
}

const recruitStatusLabel = (lab) => (isRecruiting(lab) ? '招新中' : '暂未招新')
const recruitStatusType = (lab) => (isRecruiting(lab) ? 'active' : 'default')

const resolveTags = (lab) => {
  const raw = lab.tags || lab.requireSkill || lab.researchDirection || ''
  const tags = String(raw)
    .split(/[，,、/\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
  return tags.length ? tags.slice(0, 4) : ['科研协同', '项目实践']
}

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

const clearFilters = () => {
  keyword.value = ''
  category.value = 'all'
  filterVisible.value = false
  resetAndFetch()
}

const applyFilterSheet = () => {
  filterVisible.value = false
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
.labs-mobile-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 56%, #19a7b8);
  box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
  display: grid;
  gap: 16px;
}

.mobile-kicker {
  display: inline-flex;
  width: fit-content;
  min-height: 28px;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  font-size: 12px;
  font-weight: 900;
}

.labs-mobile-hero h1 {
  margin: 12px 0 8px;
  font-size: 27px;
  line-height: 1.12;
}

.labs-mobile-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.labs-mobile-hero__action {
  min-height: 44px;
  width: fit-content;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  font-weight: 900;
}

.resume-guide-card {
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(217, 134, 31, 0.2);
  background: linear-gradient(180deg, #fff8ed, #ffffff);
  display: grid;
  gap: 14px;
}

.resume-guide-card strong {
  color: #7c3f08;
}

.resume-guide-card p {
  margin: 6px 0 0;
  color: #9a5a13;
  line-height: 1.65;
}

.mobile-chip-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.mobile-chip-row::-webkit-scrollbar {
  display: none;
}

.mobile-chip {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 8px 13px;
  border: 1px solid rgba(51, 136, 187, 0.13);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #64748b;
  font-weight: 800;
}

.mobile-chip.active {
  color: #176b9a;
  background: #eaf6fc;
  border-color: rgba(51, 136, 187, 0.26);
}

.lab-card-flow {
  display: grid;
  gap: 13px;
}

.lab-square-card {
  padding: 16px;
  display: grid;
  gap: 13px;
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.lab-square-card__top,
.lab-square-card__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.lab-square-card__title {
  min-width: 0;
}

.lab-square-card__title span {
  color: #3388bb;
  font-size: 12px;
  font-weight: 900;
}

.lab-square-card__title h2 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 18px;
  line-height: 1.25;
}

.lab-square-card__desc {
  margin: 0;
  color: #475569;
  line-height: 1.72;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.lab-square-card__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.lab-square-card__meta div {
  min-width: 0;
  padding: 11px;
  border-radius: 16px;
  background: #f8fafc;
  display: grid;
  gap: 5px;
}

.lab-square-card__meta span {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 800;
}

.lab-square-card__meta strong {
  color: #172033;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.lab-square-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.lab-square-card__tags span {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  color: #176b9a;
  background: #eaf6fc;
  font-size: 12px;
  font-weight: 800;
}

.lab-square-card__actions .el-button {
  flex: 1 1 0;
}

.filter-group {
  display: grid;
  gap: 10px;
}

.filter-group strong {
  color: #172033;
}

.load-more {
  display: flex;
  justify-content: center;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 430px) {
  .lab-square-card__meta {
    grid-template-columns: 1fr;
  }

  .lab-square-card__top,
  .lab-square-card__actions {
    flex-direction: column;
  }

  .lab-square-card__actions .el-button {
    width: 100%;
  }
}
</style>

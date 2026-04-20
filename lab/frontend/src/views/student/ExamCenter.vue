<template>
  <div class="ec-page">
    <header class="ec-header">
      <div>
        <h1 class="ec-header__title">笔试中心</h1>
        <p class="ec-header__sub">查看可参加的实验室笔试，完成考试获取面试机会</p>
      </div>
      <div class="ec-header__actions">
        <el-select v-model="statusFilter" clearable placeholder="筛选状态" style="width: 140px" @change="loadExams">
          <el-option label="全部" value="" />
          <el-option label="进行中" value="ongoing" />
          <el-option label="未开始" value="upcoming" />
          <el-option label="已结束" value="ended" />
          <el-option label="已参加" value="submitted" />
        </el-select>
        <el-button circle @click="loadExams"><el-icon><Refresh /></el-icon></el-button>
      </div>
    </header>

    <section v-loading="loading" class="ec-grid">
      <el-empty v-if="!loading && !exams.length" description="暂无可参加的笔试" />
      <article v-for="exam in exams" :key="exam.id" class="ec-card" @click="goDetail(exam)">
        <div class="ec-card__top">
          <span class="ec-card__lab">{{ exam.labName }}</span>
          <el-tag :type="statusType(exam.status)" size="small" effect="dark" round>{{ statusLabel(exam.status) }}</el-tag>
        </div>
        <h3 class="ec-card__title">{{ exam.title }}</h3>
        <p class="ec-card__desc">{{ exam.description || '暂无考试说明' }}</p>
        <div class="ec-card__meta">
          <span><el-icon><Clock /></el-icon>{{ exam.duration }} 分钟</span>
          <span><el-icon><Calendar /></el-icon>{{ formatDate(exam.startTime) }}</span>
          <span v-if="exam.totalScore"><el-icon><TrendCharts /></el-icon>满分 {{ exam.totalScore }}</span>
        </div>
        <div class="ec-card__foot">
          <span class="ec-card__eligibility" :class="eligibilityClass(exam)">{{ eligibilityText(exam) }}</span>
          <el-button v-if="exam.status === 'ongoing' && exam.eligible" type="primary" size="small" round>进入考试</el-button>
          <el-button v-else-if="exam.status === 'submitted'" size="small" round>查看结果</el-button>
          <el-button v-else size="small" round>查看详情</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Clock, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { getStudentLabExamPage } from '@/api/writtenExam'

const router = useRouter()
const loading = ref(false)
const exams = ref([])
const statusFilter = ref('')

const loadExams = async () => {
  loading.value = true
  try {
    const res = await getStudentLabExamPage({ status: statusFilter.value || undefined })
    exams.value = res.data?.records || res.data?.list || res.data || []
  } catch { exams.value = [] }
  finally { loading.value = false }
}

const goDetail = (exam) => {
  if (exam.status === 'submitted') {
    router.push(`/student/exam-center/${exam.id}/result`)
  } else {
    router.push(`/student/exam-center/${exam.id}`)
  }
}

const statusLabel = (s) => ({ upcoming: '未开始', ongoing: '进行中', ended: '已结束', submitted: '已参加' }[s] || s)
const statusType = (s) => ({ upcoming: 'info', ongoing: 'success', ended: 'danger', submitted: 'warning' }[s] || 'info')
const eligibilityText = (exam) => {
  if (exam.eligible) return '可参加'
  return exam.eligibilityReason || '暂不可参加'
}
const eligibilityClass = (exam) => exam.eligible ? 'eligible' : 'ineligible'
const formatDate = (v) => v ? dayjs(v).format('MM/DD HH:mm') : '-'

onMounted(loadExams)
</script>

<style scoped>
.ec-page { display: flex; flex-direction: column; gap: 20px; padding: 24px 28px; max-width: 1200px; margin: 0 auto; }
.ec-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; }
.ec-header__title { font-size: 22px; font-weight: 700; color: #303133; margin: 0; }
.ec-header__sub { font-size: 13px; color: #909399; margin: 4px 0 0; }
.ec-header__actions { display: flex; align-items: center; gap: 8px; }
.ec-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 16px; }
.ec-card { background: #fff; border-radius: 8px; border: 1px solid #e4e7ed; padding: 20px; cursor: pointer; transition: all .3s; display: flex; flex-direction: column; gap: 12px; }
.ec-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,.1); border-color: #409eff; }
.ec-card__top { display: flex; justify-content: space-between; align-items: center; }
.ec-card__lab { font-size: 12px; font-weight: 600; color: #409eff; background: #ecf5ff; padding: 3px 10px; border-radius: 4px; }
.ec-card__title { font-size: 16px; font-weight: 600; color: #303133; margin: 0; }
.ec-card__desc { font-size: 13px; color: #606266; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin: 0; }
.ec-card__meta { display: flex; gap: 16px; font-size: 12px; color: #909399; flex-wrap: wrap; }
.ec-card__meta span { display: flex; align-items: center; gap: 4px; }
.ec-card__foot { display: flex; justify-content: space-between; align-items: center; margin-top: auto; padding-top: 12px; border-top: 1px solid #e4e7ed; }
.ec-card__eligibility { font-size: 12px; font-weight: 600; }
.ec-card__eligibility.eligible { color: #67c23a; }
.ec-card__eligibility.ineligible { color: #f56c6c; }
@media (max-width: 640px) { .ec-page { padding: 16px; } .ec-grid { grid-template-columns: 1fr; } }
</style>

<template>
  <div v-loading="loading" class="growth-page">
    <GradPathNav
      :assessment-ready="dashboard.hasResult"
      title="成长中心"
      description="先完成 20 道题测评，再根据你的真实倾向解锁个性化路径、阶段学习方案和实践入口。"
    >
      <template #actions>
        <el-button v-if="dashboard.hasResult && !showAssessment" @click="restartAssessment">
          重新测评
        </el-button>
        <el-button
          v-if="dashboard.hasResult && !showAssessment"
          type="primary"
          @click="router.push('/student/guide/practice')"
        >
          前往实践中心
        </el-button>
        <el-tag v-else type="warning" effect="plain">
          完成测评后解锁路径广场、实践中心和 AI 面试
        </el-tag>
      </template>
    </GradPathNav>

    <el-alert
      v-if="!dashboard.hasResult"
      title="成长中心当前以测评为入口，请先完成 20 道题测评，再解锁路径广场、能力匹配、学习路线、实践中心和 AI 面试。"
      type="warning"
      :closable="false"
      show-icon
    />

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
      <el-button type="primary" plain @click="loadDashboard(true)">重新加载</el-button>
    </div>

    <section v-if="showAssessment && questions.length" class="assessment-shell">
      <el-card class="assessment-intro" shadow="never">
        <div class="intro-copy">
          <div>
            <p class="eyebrow">先做测评</p>
            <h2>在决定重点投入哪个方向前，请先完成全部 20 道题。</h2>
            <p>
              这不是性格测试，而是围绕任务偏好、学习方式、执行习惯和角色预期进行判断，
              让平台给出真正适合你的方向建议，而不是把所有路线都平均推荐一遍。
            </p>
          </div>
          <div class="step-badge">
            <strong>{{ currentStep }}</strong>
            <span>/ {{ totalSteps }}</span>
          </div>
        </div>

        <div class="progress-row">
          <el-progress :percentage="progressPercent" :stroke-width="10" />
          <span>已完成 {{ answeredCount }} / {{ questions.length }}</span>
        </div>
      </el-card>

      <div class="question-list">
        <el-card
          v-for="question in visibleQuestions"
          :key="question.id"
          class="question-card"
          shadow="never"
        >
          <div class="question-card-shell">
            <div class="question-content">
              <div class="question-head">
                <div>
                  <p class="question-index">Q{{ question.questionNo }}</p>
                  <h3>{{ question.title }}</h3>
                </div>
                <el-tag class="dimension-tag" effect="plain">{{ question.dimension }}</el-tag>
              </div>
              <p class="question-desc">{{ question.description }}</p>
            </div>

            <el-radio-group v-model="answers[question.id]" class="option-group">
              <el-radio
                v-for="option in question.options"
                :key="option.id"
                :label="option.optionKey"
                class="option-item"
                :class="{ selected: answers[question.id] === option.optionKey }"
              >
                <div class="option-copy">
                  <strong>{{ option.optionKey }}. {{ option.optionTitle }}</strong>
                  <span>{{ option.optionDesc }}</span>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </el-card>
      </div>

      <div class="assessment-actions">
        <el-button :disabled="currentStep === 1" @click="currentStep -= 1">上一步</el-button>
        <el-button v-if="currentStep < totalSteps" type="primary" @click="goNextStep">
          下一步
        </el-button>
        <el-button v-else type="primary" :loading="submitting" @click="handleSubmit">
          提交测评
        </el-button>
      </div>
    </section>

    <template v-else>
      <section class="result-grid">
        <section class="hero-card">
          <p class="eyebrow">结果总览</p>
          <h2>{{ topTrack?.name || '推荐成长路径' }}</h2>
          <p class="summary-text">{{ dashboard.latestResult?.summary }}</p>

          <div class="hero-meta">
            <MetricCard label="最高匹配度" :value="topTrack?.matchScore ?? 0" tip="系统动态计算" compact />
            <MetricCard label="已答题数" :value="dashboard.latestResult?.answerCount || 0" tip="总计 20 题" compact />
            <MetricCard label="最近测评" :value="dashboard.latestResult?.createTime || '-'" tip="最近一次提交时间" compact />
          </div>

          <div class="hero-actions">
            <el-button
              v-if="topTrack"
              type="primary"
              @click="router.push(`/student/guide/learn?track=${topTrack.code}`)"
            >
              从这条路径开始
            </el-button>
            <el-button
              v-if="topTrack"
              @click="router.push(`/student/guide/practice?track=${topTrack.code}&keyword=${encodeURIComponent(topTrack.recommendedKeyword || '')}`)"
            >
              前往实践中心
            </el-button>
          </div>
        </section>

        <TablePageCard class="top-card" title="TOP 3 推荐路径" subtitle="动态结果" count-label="动态结果" count-tag-type="success">
          <div class="top-list">
            <button
              v-for="track in topTracks"
              :key="track.code"
              class="top-item"
              @click="router.push(`/student/guide/match?track=${track.code}`)"
            >
              <div>
                <strong>{{ track.name }}</strong>
                <p>{{ track.subtitle }}</p>
              </div>
              <span>{{ track.matchScore }}</span>
            </button>
          </div>
        </TablePageCard>
      </section>

      <section class="ranking-grid">
        <TablePageCard class="ranking-card" title="全部路径排行" subtitle="完整排序" :count-label="`共 ${ranking.length} 条`">
          <div class="ranking-list">
            <div v-for="track in ranking" :key="track.code" class="ranking-item">
              <div class="rank-copy">
                <strong>{{ track.name }}</strong>
                <p>{{ track.subtitle }}</p>
              </div>
              <div class="rank-progress">
                <el-progress :percentage="track.matchScore" :stroke-width="10" />
              </div>
            </div>
          </div>
        </TablePageCard>

        <TablePageCard class="quick-card" title="建议动作" subtitle="先做这三步" count-label="三步" count-tag-type="warning">
          <div class="quick-list">
            <div class="quick-item">
              <strong>1. 先确认主路径</strong>
              <p>
                优先投入排名最高的路径，不要把精力平均分散到太多方向上。
                只有当前两条路径结果真的非常接近时，才建议并行准备。
              </p>
            </div>
            <div class="quick-item">
              <strong>2. 按阶段学习路线推进</strong>
              <p>
                每条路径都拆成了清晰的阶段。先建立知识闭环，再进入题库训练和项目实践，
                不要只停留在囤资料阶段。
              </p>
            </div>
            <div class="quick-item">
              <strong>3. 用题库和 AI 面试双重验证</strong>
              <p>
                实践中心负责验证题目通过率，AI 面试负责验证表达与讲解能力，
                两者都需要，不要只做其中一项。
              </p>
            </div>
          </div>
        </TablePageCard>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GradPathNav from '@/components/GradPathNav.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { clearGrowthDashboardCache, loadGrowthDashboard } from '@/utils/growthCenterCache'
import {
  getGrowthAssessmentQuestions,
  submitGrowthAssessment
} from '@/api/growthCenter'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const showAssessment = ref(false)
const currentStep = ref(1)
const pageSize = 5
const loadError = ref('')

const dashboard = reactive({
  hasResult: false,
  tracks: [],
  latestResult: null
})

const questions = ref([])
const versionNo = ref(1)
const answers = reactive({})

const totalSteps = computed(() => Math.max(1, Math.ceil(questions.value.length / pageSize)))
const answeredCount = computed(() =>
  questions.value.filter((question) => Boolean(answers[question.id])).length
)
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})
const visibleQuestions = computed(() => {
  const start = (currentStep.value - 1) * pageSize
  return questions.value.slice(start, start + pageSize)
})
const ranking = computed(() => dashboard.latestResult?.ranking || [])
const topTracks = computed(() => dashboard.latestResult?.topTracks || [])
const topTrack = computed(() => topTracks.value[0] || null)

const applyDashboardData = (dashboardData = {}) => {
  dashboard.hasResult = Boolean(dashboardData.hasResult)
  dashboard.tracks = dashboardData.tracks || []
  dashboard.latestResult = dashboardData.latestResult || null
  showAssessment.value = !dashboard.hasResult
}

const loadDashboard = async (force = false) => {
  loading.value = true
  loadError.value = ''
  try {
    const dashboardData = await loadGrowthDashboard({ force })
    applyDashboardData(dashboardData)
    if (showAssessment.value && (force || !questions.value.length)) {
      await loadAssessmentQuestions()
    }
  } catch (error) {
    loadError.value = '加载成长中心数据失败，请稍后重试。'
    questions.value = []
  } finally {
    loading.value = false
  }
}

const loadAssessmentQuestions = async () => {
  const response = await getGrowthAssessmentQuestions()
  versionNo.value = response.data.versionNo || 1
  questions.value = response.data.questions || []
}

const restartAssessment = async () => {
  loadError.value = ''
  Object.keys(answers).forEach((key) => {
    delete answers[key]
  })
  currentStep.value = 1
  await loadAssessmentQuestions()
  showAssessment.value = true
}

const validateCurrentStep = () => {
  const missing = visibleQuestions.value.find((question) => !answers[question.id])
  if (missing) {
    ElMessage.warning(`请先完成第 ${missing.questionNo} 题。`)
    return false
  }
  return true
}

const goNextStep = () => {
  if (!validateCurrentStep()) {
    return
  }
  currentStep.value += 1
}

const handleSubmit = async () => {
  if (!validateCurrentStep()) {
    return
  }
  if (answeredCount.value !== questions.value.length) {
    ElMessage.warning('请先完成全部 20 道测评题后再提交。')
    return
  }

  submitting.value = true
  try {
    await submitGrowthAssessment({
      versionNo: versionNo.value,
      answers: questions.value.map((question) => ({
        questionId: question.id,
        optionKey: answers[question.id]
      }))
    })
    ElMessage.success('测评已提交，推荐路径已更新。')
    clearGrowthDashboardCache()
    await loadDashboard(true)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.growth-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1360px;
  margin: 0 auto;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.assessment-shell,
.question-list,
.top-list,
.ranking-list,
.quick-list {
  display: grid;
  gap: 18px;
}

.assessment-intro,
.question-card,
.hero-card,
.top-card,
.ranking-card,
.quick-card {
  border: 1px solid rgba(14, 165, 233, 0.14);
  border-radius: 24px;
}

.assessment-intro,
.hero-card {
  padding: 20px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 25%),
    linear-gradient(135deg, #082f49, #0f766e);
  color: #f8fafc;
}

.intro-copy {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: rgba(186, 230, 253, 0.92);
}

.assessment-intro h2,
.hero-card h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.25;
}

.assessment-intro p,
.summary-text {
  margin: 16px 0 0;
  line-height: 1.9;
  color: rgba(226, 232, 240, 0.94);
}

.step-badge {
  min-width: 120px;
  text-align: center;
  padding: 22px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.step-badge strong {
  font-size: 42px;
  line-height: 1;
}

.step-badge span {
  font-size: 16px;
  color: rgba(226, 232, 240, 0.92);
}

.progress-row {
  margin-top: 20px;
  display: grid;
  gap: 12px;
}

.progress-row span {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.92);
}

.question-card {
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background:
    radial-gradient(circle at top right, rgba(186, 230, 253, 0.22), transparent 26%),
    linear-gradient(180deg, #ffffff, #f8fbff);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.question-card :deep(.el-card__body) {
  padding: 0;
}

.question-card-shell {
  display: flex;
  flex-direction: column;
}

.question-content {
  padding: 24px 24px 8px;
}

.question-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.question-index {
  display: inline-flex;
  align-items: center;
  margin: 0 0 10px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #0284c7;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.question-head h3 {
  margin: 0;
  font-size: 28px;
  line-height: 1.28;
  color: #0f172a;
}

.dimension-tag {
  border-color: rgba(14, 165, 233, 0.18);
  color: #0369a1;
  background: rgba(240, 249, 255, 0.9);
}

.question-desc {
  margin: 14px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.option-group {
  margin-top: 8px;
  display: grid;
  gap: 12px;
  padding: 0 24px 24px;
}

.option-item {
  margin: 0;
  width: 100%;
  display: flex;
  align-items: center;
  min-height: 82px;
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.94));
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease,
    background 0.2s ease;
}

.option-item:hover {
  border-color: rgba(56, 189, 248, 0.36);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.08);
  transform: translateY(-1px);
}

.option-item.selected {
  border-color: rgba(14, 165, 233, 0.34);
  background:
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.18), transparent 42%),
    linear-gradient(180deg, #f0f9ff, #f8fbff);
  box-shadow: 0 14px 30px rgba(14, 165, 233, 0.12);
}

.option-item :deep(.el-radio__input) {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  align-self: center;
  margin-top: 0;
}

.option-item :deep(.el-radio__inner) {
  width: 16px;
  height: 16px;
  border-color: rgba(14, 165, 233, 0.36);
  background: #fff;
}

.option-item.selected :deep(.el-radio__inner) {
  border-color: #0ea5e9;
  background: #0ea5e9;
}

.option-item :deep(.el-radio__label) {
  display: flex;
  align-items: center;
  flex: 1;
  width: 100%;
  min-width: 0;
  padding-left: 10px;
  white-space: normal;
  line-height: normal;
}

.option-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 48px;
}

.option-copy strong,
.option-copy span {
  display: block;
}

.option-copy strong {
  font-size: 16px;
  line-height: 1.45;
  color: #0f172a;
}

.option-copy span {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.75;
}

.assessment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.result-grid,
.ranking-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(320px, 0.88fr);
  gap: 18px;
}

.hero-meta {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-meta :deep(.metric-card) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.12);
}

.hero-meta :deep(.metric-card__label),
.hero-meta :deep(.metric-card__tip),
.hero-meta :deep(.metric-card__value) {
  color: #e2e8f0;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.top-item,
.ranking-item,
.quick-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, #ffffff, #f8fafc);
}

.top-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.top-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.08);
}

.top-item strong,
.ranking-item strong,
.quick-item strong {
  color: #0f172a;
}

.top-item p,
.quick-item p,
.rank-copy p {
  margin: 8px 0 0;
  line-height: 1.8;
  color: #475569;
}

.top-item span {
  font-size: 26px;
  font-weight: 800;
  color: #0f766e;
}

.ranking-item {
  display: grid;
  grid-template-columns: minmax(220px, 0.82fr) minmax(0, 1.18fr);
  gap: 16px;
  align-items: center;
}

@media (max-width: 960px) {
  .result-grid,
  .ranking-grid {
    grid-template-columns: 1fr;
  }

  .hero-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .load-error {
    flex-direction: column;
    align-items: stretch;
  }

  .intro-copy {
    flex-direction: column;
    align-items: flex-start;
  }

  .assessment-intro h2,
  .hero-card h2 {
    font-size: 26px;
  }

  .question-content,
  .option-group {
    padding-left: 18px;
    padding-right: 18px;
  }

  .question-head h3 {
    font-size: 22px;
  }

  .ranking-item {
    grid-template-columns: 1fr;
  }
}
</style>

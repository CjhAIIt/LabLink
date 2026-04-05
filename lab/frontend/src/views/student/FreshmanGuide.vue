<template>
  <div v-loading="loading" class="growth-page">
    <GradPathNav
      :assessment-ready="dashboard.hasResult"
      title="成长中心"
      description="先完成 20 道成长测评，再根据你的真实倾向动态生成成长路径、学习阶段和练习入口。这里不再只是静态引导页，而是整个成长中心的起点。"
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
          直接去练习中心
        </el-button>
        <el-tag v-else type="warning" effect="plain">
          完成测评后解锁路径广场、练习中心和 AI 面试
        </el-tag>
      </template>
    </GradPathNav>

    <el-alert
      v-if="!dashboard.hasResult"
      title="当前仍处于成长中心初始化阶段。请先完成 20 题成长测评，后续路径广场、能力匹配、学习路线、练习中心和 AI 面试都会在测评完成后解锁。"
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
            <p class="eyebrow">ASSESSMENT FIRST</p>
            <h2>先回答 20 道题，再决定你该走哪条成长路径</h2>
            <p>
              这套题不是性格测试，而是围绕任务偏好、学习方式、交付习惯和岗位期待来判断你更适合投入哪条路径。
              题目答完后，系统会给出动态推荐度，而不是所有方向都差不多。
            </p>
          </div>
          <div class="step-badge">
            <strong>{{ currentStep }}</strong>
            <span>/ {{ totalSteps }}</span>
          </div>
        </div>

        <div class="progress-row">
          <el-progress :percentage="progressPercent" :stroke-width="10" />
          <span>{{ answeredCount }} / {{ questions.length }} 已完成</span>
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
        <el-button :disabled="currentStep === 1" @click="currentStep -= 1">上一组</el-button>
        <el-button v-if="currentStep < totalSteps" type="primary" @click="goNextStep">
          下一组
        </el-button>
        <el-button v-else type="primary" :loading="submitting" @click="handleSubmit">
          提交测评
        </el-button>
      </div>
    </section>

    <template v-else>
      <section class="result-grid">
        <el-card class="hero-card" shadow="never">
          <p class="eyebrow">RESULT OVERVIEW</p>
          <h2>{{ topTrack?.name || '成长路径推荐' }}</h2>
          <p class="summary-text">{{ dashboard.latestResult?.summary }}</p>

          <div class="hero-meta">
            <div class="meta-item">
              <strong>{{ topTrack?.matchScore ?? 0 }}</strong>
              <span>首选推荐度</span>
            </div>
            <div class="meta-item">
              <strong>{{ dashboard.latestResult?.answerCount || 0 }}</strong>
              <span>已答题数</span>
            </div>
            <div class="meta-item">
              <strong>{{ dashboard.latestResult?.createTime || '-' }}</strong>
              <span>最近测评时间</span>
            </div>
          </div>

          <div class="hero-actions">
            <el-button
              v-if="topTrack"
              type="primary"
              @click="router.push(`/student/guide/learn?track=${topTrack.code}`)"
            >
              按首选路径开始
            </el-button>
            <el-button
              v-if="topTrack"
              @click="router.push(`/student/guide/practice?track=${topTrack.code}&keyword=${encodeURIComponent(topTrack.recommendedKeyword || '')}`)"
            >
              去练习中心
            </el-button>
          </div>
        </el-card>

        <el-card class="top-card" shadow="never">
          <template #header>
            <div class="section-head">
              <span>Top 3 推荐路径</span>
              <el-tag type="success" effect="plain">动态结果</el-tag>
            </div>
          </template>

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
        </el-card>
      </section>

      <section class="ranking-grid">
        <el-card class="ranking-card" shadow="never">
          <template #header>
            <div class="section-head">
              <span>完整路径排序</span>
              <el-tag effect="plain">共 {{ ranking.length }} 条</el-tag>
            </div>
          </template>

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
        </el-card>

        <el-card class="quick-card" shadow="never">
          <template #header>
            <div class="section-head">
              <span>推荐动作</span>
              <el-tag type="warning" effect="plain">先做这三步</el-tag>
            </div>
          </template>

          <div class="quick-list">
            <div class="quick-item">
              <strong>1. 先确认主路径</strong>
              <p>优先按首选路径推进，不要同时摊开太多方向。只有在前两名非常接近时，再考虑双路径准备。</p>
            </div>
            <div class="quick-item">
              <strong>2. 再去看学习路线</strong>
              <p>每条路径都拆成阶段式路线，先把知识闭环做出来，再去刷题和做项目，不要只囤资料。</p>
            </div>
            <div class="quick-item">
              <strong>3. 最后用题库和 AI 面试验证</strong>
              <p>练习中心负责题目通过率，AI 面试负责表达能力，两个都要补，不能只做其中一个。</p>
            </div>
          </div>
        </el-card>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GradPathNav from '@/components/GradPathNav.vue'
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
    loadError.value = '成长中心数据加载失败，请稍后重试'
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
    ElMessage.warning(`请先完成第 ${missing.questionNo} 题`)
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
    ElMessage.warning('请完成全部 20 道测评题目')
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
    ElMessage.success('成长测评已完成，推荐路径已更新')
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
}

.assessment-intro,
.hero-card {
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

.meta-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.meta-item strong,
.meta-item span {
  display: block;
}

.meta-item strong {
  font-size: 22px;
}

.meta-item span {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.88);
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
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

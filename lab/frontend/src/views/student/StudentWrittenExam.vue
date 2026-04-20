<template>
  <div class="exam-page">
    <header class="topbar">
      <div class="topbar-group">
        <el-button text @click="goBack">返回</el-button>
        <el-button @click="questionListVisible = !questionListVisible">
          {{ questionListVisible ? '收起题目列表' : '题目列表' }}
        </el-button>
        <div class="title-block">
          <strong>{{ pageTitle }}</strong>
          <span>{{ pageSubtitle }}</span>
        </div>
      </div>
      <div class="topbar-group">
        <el-select v-if="practiceMode" v-model="trackCode" style="width: 220px">
          <el-option label="公共题库" value="common" />
          <el-option v-for="track in gradPathTracks" :key="track.id" :label="track.name" :value="track.id" />
        </el-select>
        <el-button @click="goToInterview">AI 面试</el-button>
        <el-button @click="goNext">下一题</el-button>
        <el-button v-if="!practiceMode && !examSubmitted" type="primary" :loading="submittingExam" @click="submitExamPaper">
          提交本场笔试
        </el-button>
      </div>
    </header>

    <section class="status-row">
      <div class="status-card">
        <span>{{ practiceMode ? '当前方向' : '实验室' }}</span>
        <strong>{{ practiceMode ? trackLabel : (examContext.lab?.labName || '正式笔试') }}</strong>
      </div>
      <div class="status-card">
        <span>题目数量</span>
        <strong>{{ questionList.length }}</strong>
      </div>
      <div class="status-card">
        <span>{{ practiceMode ? '当前题型' : '已作答' }}</span>
        <strong>{{ practiceMode ? currentTypeLabel : `${answeredCount}/${questionList.length}` }}</strong>
      </div>
      <div class="status-card">
        <span>{{ practiceMode ? '推荐语言' : '通过分数' }}</span>
        <strong>{{ practiceMode ? (languageLabelMap[suggestedLanguage] || 'Java') : (examContext.exam?.passScore ?? '-') }}</strong>
      </div>
    </section>

    <el-alert v-if="practiceMode" title="当前为成长中心练习模式，支持共享题库中的单选、填空和编程题。" type="info" :closable="false" show-icon />
    <el-alert v-else-if="examSubmitted" :title="examMessage" type="success" :closable="false" show-icon />
    <el-alert v-else title="正式笔试只展示管理员选中的题目，不会展示成长中心全部练习题。" type="warning" :closable="false" show-icon />
    <el-alert
      v-if="currentQuestion?.questionType === 'programming' && languageMismatchMessage"
      :title="languageMismatchMessage"
      type="warning"
      :closable="false"
      show-icon
    />

    <section class="workspace" :class="{ collapsed: !questionListVisible }">
      <aside class="panel sidebar">
        <div class="panel-head">
          <div>
            <p class="eyebrow">{{ practiceMode ? 'PRACTICE BANK' : 'FORMAL EXAM' }}</p>
            <h3>题目列表</h3>
          </div>
          <el-button text @click="refreshCurrentMode">刷新</el-button>
        </div>

        <div v-if="practiceMode" class="toolbar">
          <el-input v-model="filters.keyword" clearable placeholder="搜索标题或描述" @keyup.enter="fetchPracticeQuestions" />
          <div class="topbar-group">
            <el-select v-model="filters.questionType" clearable placeholder="题型" style="width: 160px">
              <el-option label="单选题" value="single_choice" />
              <el-option label="填空题" value="fill_blank" />
              <el-option label="编程题" value="programming" />
            </el-select>
            <el-button type="primary" @click="fetchPracticeQuestions">搜索</el-button>
          </div>
        </div>

        <div v-loading="loadingQuestions" class="question-list">
          <el-empty v-if="!loadingQuestions && questionList.length === 0" description="暂无题目" />
          <button v-for="(item, index) in questionList" :key="item.id" class="question-item" :class="{ active: currentQuestion?.id === item.id }" @click="selectQuestion(item.id)">
            <div class="question-meta">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <el-tag size="small" effect="plain">{{ questionTypeLabelMap[item.questionType] || item.questionType }}</el-tag>
            </div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.content || '暂无题目描述' }}</p>
            <div class="question-foot">
              <span>{{ item.trackCode || 'common' }}</span>
              <span v-if="!practiceMode && isQuestionAnswered(item.id)">已作答</span>
            </div>
          </button>
        </div>

        <div v-if="practiceMode" class="pager">
          <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize" :page-sizes="[6, 9, 12]" :total="pagination.total" layout="total, sizes, prev, next" @current-change="fetchPracticeQuestions" @size-change="handlePageSizeChange" />
        </div>
      </aside>

      <section class="panel detail-panel">
        <el-empty v-if="!currentQuestion" description="请先选择一道题目" />
        <template v-else>
          <div class="panel-head">
            <div>
              <p class="eyebrow">QUESTION</p>
              <h3>{{ currentQuestion.title }}</h3>
            </div>
            <div class="topbar-group">
              <el-tag effect="plain">{{ questionTypeLabelMap[currentQuestion.questionType] || currentQuestion.questionType }}</el-tag>
              <el-tag v-if="currentQuestion.trackCode" effect="plain" type="success">{{ currentQuestion.trackCode }}</el-tag>
              <el-tag v-if="currentQuestion.difficulty" effect="plain" type="warning">{{ currentQuestion.difficulty }}</el-tag>
            </div>
          </div>

          <div class="detail-body">
            <section class="detail-block">
              <h4>题目描述</h4>
              <p>{{ currentQuestion.content || '暂无题目描述' }}</p>
            </section>

            <section v-if="currentQuestion.questionType === 'single_choice'" class="detail-block">
              <h4>选项</h4>
              <div class="option-list">
                <div v-for="option in currentQuestion.options || []" :key="option.label" class="option-row">
                  <strong>{{ option.label }}.</strong>
                  <span>{{ option.text }}</span>
                </div>
              </div>
            </section>

            <template v-if="currentQuestion.questionType === 'programming'">
              <section class="detail-block">
                <h4>输入格式</h4>
                <pre>{{ currentQuestion.inputFormat || '-' }}</pre>
              </section>
              <section class="detail-block">
                <h4>输出格式</h4>
                <pre>{{ currentQuestion.outputFormat || '-' }}</pre>
              </section>
              <section class="detail-block split">
                <div>
                  <h4>样例输入</h4>
                  <pre>{{ sampleCase.input || '-' }}</pre>
                </div>
                <div>
                  <h4>样例输出</h4>
                  <pre>{{ sampleCase.output || '-' }}</pre>
                </div>
              </section>
            </template>

            <section class="detail-block">
              <h4>标签</h4>
              <div class="tag-list">
                <el-tag v-for="tag in normalizedTags" :key="tag" effect="plain" type="info">{{ tag }}</el-tag>
              </div>
            </section>

            <section class="detail-block">
              <h4>练习提示</h4>
              <p>{{ currentQuestion.analysisHint || defaultHint }}</p>
            </section>
          </div>
        </template>
      </section>

      <section class="panel answer-panel">
        <template v-if="currentQuestion">
          <div class="panel-head">
            <div>
              <p class="eyebrow">{{ currentQuestion.questionType === 'programming' ? 'EDITOR' : 'ANSWER' }}</p>
              <h3>{{ practiceMode ? '作答区' : '正式答题区' }}</h3>
            </div>
            <div class="topbar-group">
              <template v-if="currentQuestion.questionType === 'programming'">
                <el-select v-model="selectedLanguage" :disabled="examSubmitted || !currentAvailableLanguages.length" style="width: 150px" @change="applyTemplate">
                  <el-option v-for="language in currentAllowedLanguages" :key="language" :label="languageLabelMap[language] || language" :value="language" />
                </el-select>
                <el-button :disabled="examSubmitted || !currentAvailableLanguages.length" @click="applyTemplate(selectedLanguage)">重置模板</el-button>
                <el-button v-if="practiceMode" :loading="running" :disabled="examSubmitted || !currentAvailableLanguages.length" @click="runProgramming('debug')">自测运行</el-button>
                <el-button type="primary" :loading="running" :disabled="examSubmitted || !currentAvailableLanguages.length" @click="runProgramming(practiceMode ? 'submit' : 'draft')">{{ practiceMode ? '提交答案' : '保存当前代码' }}</el-button>
              </template>
              <template v-else>
                <el-button type="primary" :loading="running" :disabled="examSubmitted" @click="submitObjective">{{ practiceMode ? '检查答案' : '保存当前答案' }}</el-button>
              </template>
            </div>
          </div>

          <div class="answer-body">
            <div v-if="currentQuestion.questionType === 'programming'" class="editor-wrap">
              <div ref="lineGutterRef" class="line-gutter"><span v-for="lineNo in lineNumbers" :key="lineNo">{{ lineNo }}</span></div>
              <textarea ref="codeEditorRef" v-model="code" class="code-editor" :disabled="examSubmitted" spellcheck="false" @keydown="handleEditorKeydown" @scroll="handleEditorScroll" />
            </div>

            <div v-else class="objective-wrap">
              <template v-if="currentQuestion.questionType === 'single_choice'">
                <label
                  v-for="option in currentQuestion.options || []"
                  :key="option.label"
                  class="objective-item"
                  :class="{ active: textAnswer === option.label }"
                >
                  <input v-model="textAnswer" type="radio" :value="option.label" :disabled="examSubmitted">
                  <div><strong>{{ option.label }}. {{ option.text }}</strong></div>
                </label>
              </template>
              <div v-else class="fill-wrap">
                <strong>填空答案</strong>
                <el-input v-model="textAnswer" placeholder="请输入你的答案" :disabled="examSubmitted" />
              </div>
            </div>

            <div v-if="practiceMode && currentQuestion.questionType === 'programming'" class="custom-input">
              <div class="question-meta"><strong>自测输入</strong><el-button text @click="customInput = sampleCase.input || ''">填入样例</el-button></div>
              <el-input v-model="customInput" type="textarea" :rows="4" :disabled="examSubmitted" placeholder="调试运行时优先使用这里的输入" />
            </div>

            <div class="result-box">
              <div class="question-meta"><strong>{{ practiceMode ? '运行结果' : (examSubmitted ? '提交结果' : '答题状态') }}</strong><el-tag :type="resultTagType">{{ resultTagText }}</el-tag></div>
              <pre class="result-output">{{ resultText }}</pre>
              <div v-if="canAnalyze" class="analysis-actions"><el-button :loading="analyzing" @click="handleAnalyze">AI 分析错误</el-button></div>
              <div v-if="aiAnalysis" class="analysis-box">{{ aiAnalysis }}</div>
            </div>

            <div v-if="submissionHistory.length" class="history-box">
              <strong>提交记录</strong>
              <div v-for="item in submissionHistory" :key="item.id" class="history-item">
                <span>{{ item.mode }}</span>
                <span>{{ item.at }}</span>
                <el-tag :type="item.tagType">{{ item.statusText }}</el-tag>
              </div>
            </div>
          </div>
        </template>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { analyzeGradPathCode } from '@/api/gradPath'
import { getGrowthPracticeQuestionDetail, getGrowthPracticeQuestions, submitGrowthPracticeAnswer } from '@/api/growthCenter'
import { getStudentExam, submitStudentExam } from '@/api/writtenExam'
import { defaultTrackId, getTrackById, gradPathTracks } from '@/constants/gradPath'

const route = useRoute()
const router = useRouter()
const examContext = reactive({ lab: null, exam: null })
const filters = reactive({ keyword: '', questionType: '' })
const pagination = reactive({ pageNum: 1, pageSize: 6, total: 0 })
const questionList = ref([])
const loadingQuestions = ref(false)
const currentQuestion = ref(null)
const selectedLanguage = ref('java')
const code = ref('')
const textAnswer = ref('')
const customInput = ref('')
const running = ref(false)
const runResult = ref(null)
const analyzing = ref(false)
const aiAnalysis = ref('')
const questionListVisible = ref(true)
const submissionHistory = ref([])
const codeEditorRef = ref(null)
const lineGutterRef = ref(null)
const submittingExam = ref(false)
const examSubmitted = ref(false)
const examResult = ref(null)
const examAnswers = reactive({})
const runtimeEnvironmentStatus = reactive({})
const runtimeEnvironmentDetails = reactive({})

const questionTypeLabelMap = { single_choice: '单选题', fill_blank: '填空题', programming: '编程题' }
const languageLabelMap = { java: 'Java', python: 'Python', c: 'C', cpp: 'C++' }
const codeTemplates = {
  java: `import java.util.*;\n\npublic class Main {\n  public static void main(String[] args) {\n    Scanner scanner = new Scanner(System.in);\n    // TODO: write your solution here\n  }\n}`,
  python: `import sys\n\n# TODO: write your solution here\n`,
  c: `#include <stdio.h>\n\nint main(void) {\n  // TODO: write your solution here\n  return 0;\n}`,
  cpp: `#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n  // TODO: write your solution here\n  return 0;\n}`
}

const practiceMode = computed(() => !route.params.labId)
const trackCode = computed({
  get: () => route.query.track || defaultTrackId,
  set: (value) => router.replace({ path: route.path, query: { ...route.query, track: value } })
})
const trackLabel = computed(() => trackCode.value === 'common' ? '公共题库' : getTrackById(trackCode.value).name)
const pageTitle = computed(() => practiceMode.value ? '成长中心练习中心' : (examContext.exam?.title || '实验室正式笔试'))
const pageSubtitle = computed(() => practiceMode.value ? '共享题库题目可直接被管理员加入正式笔试' : '当前只展示本场正式笔试已配置题目')
const currentTypeLabel = computed(() => currentQuestion.value ? questionTypeLabelMap[currentQuestion.value.questionType] : '题目')
const suggestedLanguage = computed(() => getTrackById(trackCode.value).preferredLanguage || 'java')
const normalizedTags = computed(() => normalizeTags(currentQuestion.value?.tags))
const sampleCase = computed(() => parseSampleCase(currentQuestion.value?.sampleCase))
const currentAllowedLanguages = computed(() => {
  const configuredLanguages = currentQuestion.value?.allowedLanguages?.length
    ? currentQuestion.value.allowedLanguages
    : ['java', 'python', 'c', 'cpp']
  return configuredLanguages.filter((language) => Boolean(runtimeEnvironmentStatus[language]))
})
const configuredLanguages = computed(() => currentQuestion.value?.allowedLanguages?.length ? currentQuestion.value.allowedLanguages : ['java', 'python', 'c', 'cpp'])
const unavailableConfiguredLanguages = computed(() => configuredLanguages.value.filter((language) => !runtimeEnvironmentStatus[language]))
const currentAvailableLanguages = computed(() => currentAllowedLanguages.value)
const languageMismatchMessage = computed(() => {
  if (currentQuestion.value?.questionType !== 'programming') return ''
  if (!Object.keys(runtimeEnvironmentStatus).length) return ''
  if (currentAvailableLanguages.value.length === 0) {
    return `当前服务器没有可用于本题的编程环境。管理员端显示为准。本题配置语言：${configuredLanguages.value.map((item) => languageLabelMap[item] || item).join('、') || '无'}。`
  }
  if (unavailableConfiguredLanguages.value.length === 0) return ''
  return `当前服务器暂不支持 ${unavailableConfiguredLanguages.value.map((item) => languageLabelMap[item] || item).join('、')}，已自动隐藏；当前可用：${currentAvailableLanguages.value.map((item) => languageLabelMap[item] || item).join('、')}。`
})
const lineNumbers = computed(() => Array.from({ length: Math.max(18, code.value.split('\n').length) }, (_, index) => index + 1))
const answeredCount = computed(() => questionList.value.filter((item) => isQuestionAnswered(item.id)).length)
const defaultHint = computed(() => currentQuestion.value?.questionType === 'programming' ? '先确认输入输出格式，再覆盖边界条件。' : '先回到题干确认真正问的是什么，再作答。')
const resultTagType = computed(() => {
  if (!practiceMode.value && !examSubmitted.value) return 'info'
  if (!practiceMode.value && examSubmitted.value) return examResult.value?.status === 2 ? 'success' : 'warning'
  if (!runResult.value) return 'info'
  if (runResult.value.correct === true) return 'success'
  if (runResult.value.status === 'wrong_answer') return 'warning'
  if (runResult.value.status === 'success') return 'success'
  return 'danger'
})
const resultTagText = computed(() => {
  if (!practiceMode.value && !examSubmitted.value) return '待提交'
  if (!practiceMode.value && examSubmitted.value) return examResult.value?.status === 2 ? '已通过' : '已提交'
  if (!runResult.value) return '尚未运行'
  if (runResult.value.correct === true) return 'Accepted'
  if (runResult.value.status === 'wrong_answer') return 'Wrong Answer'
  if (runResult.value.status === 'success') return 'Success'
  return 'Error'
})
const resultText = computed(() => {
  if (!practiceMode.value && !examSubmitted.value) {
    const answer = currentQuestion.value ? examAnswers[currentQuestion.value.id] : null
    if (!answer) return '当前题目尚未保存答案。切换题目会自动保存当前内容，完成后统一提交。'
    return currentQuestion.value?.questionType === 'programming'
      ? `当前语言：${languageLabelMap[answer.language] || answer.language || '-'}\n代码长度：${answer.code?.length || 0} 字符`
      : `当前答案：${answer.answer || '未填写'}`
  }
  if (!practiceMode.value && examSubmitted.value) return examResult.value?.aiRemark || '正式笔试已提交，等待管理员审核。'
  if (!runResult.value) return '点击“自测运行”或“提交答案”后，这里会显示结果。'
  return [runResult.value.message, runResult.value.stdout, runResult.value.error, runResult.value.stderr].filter(Boolean).join('\n\n') || '本次没有返回额外信息。'
})
const examMessage = computed(() => examResult.value?.status === 2 ? `正式笔试已通过，当前分数 ${examResult.value.totalScore ?? examResult.value.score ?? '-'} 分。` : '正式笔试已提交，等待管理员审核。')
const canAnalyze = computed(() => practiceMode.value && currentQuestion.value?.questionType === 'programming' && runResult.value && ['warning', 'danger'].includes(resultTagType.value))

const fetchPracticeQuestions = async () => {
  loadingQuestions.value = true
  try {
    const res = await getGrowthPracticeQuestions({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, trackCode: trackCode.value, questionType: filters.questionType || undefined, keyword: filters.keyword || undefined })
    questionList.value = res.data.list || []
    pagination.total = res.data.total || 0
    syncEnvironmentStatus(res.data.environmentStatus, res.data.environmentDetails)
    if (questionList.value.length) {
      const exists = questionList.value.some((item) => item.id === currentQuestion.value?.id)
      if (!exists) await selectQuestion(questionList.value[0].id)
    } else {
      currentQuestion.value = null
    }
  } finally {
    loadingQuestions.value = false
  }
}

const fetchExamQuestions = async () => {
  const res = await getStudentExam(route.params.labId)
  examContext.lab = res.data.lab
  examContext.exam = res.data.exam
  questionList.value = res.data.questions || []
  examSubmitted.value = Boolean(res.data.alreadySubmitted)
  examResult.value = res.data.submission || null
  syncEnvironmentStatus(res.data.environmentStatus, res.data.environmentDetails)
  if (questionList.value.length) await selectQuestion(questionList.value[0].id)
}

const selectQuestion = async (questionId) => {
  if (practiceMode.value) {
    const res = await getGrowthPracticeQuestionDetail(questionId)
    currentQuestion.value = res.data
    syncEnvironmentStatus(res.data.environmentStatus, res.data.environmentDetails)
  } else {
    currentQuestion.value = questionList.value.find((item) => item.id === questionId) || null
  }
  runResult.value = null
  aiAnalysis.value = ''
  hydrateAnswer()
}

const hydrateAnswer = () => {
  if (!currentQuestion.value) return
  const answer = examAnswers[currentQuestion.value.id] || {}
  if (currentQuestion.value.questionType === 'programming') {
    const language = answer.language && currentAvailableLanguages.value.includes(answer.language)
      ? answer.language
      : (currentAvailableLanguages.value[0] || '')
    selectedLanguage.value = language
    code.value = answer.code || (language ? codeTemplates[language] || '' : '')
    customInput.value = sampleCase.value.input || ''
    if (!practiceMode.value && language && !answer.code) examAnswers[currentQuestion.value.id] = { questionId: currentQuestion.value.id, language, code: code.value }
  } else {
    textAnswer.value = answer.answer || ''
  }
}

const applyTemplate = (language) => {
  if (examSubmitted.value || !language) return
  selectedLanguage.value = language
  code.value = codeTemplates[language] || ''
}

const runProgramming = async (mode) => {
  if (!currentAvailableLanguages.value.length) return ElMessage.warning('当前服务器不支持本题配置的编程语言，请联系管理员安装对应编译环境')
  if (!currentQuestion.value || !code.value.trim()) return ElMessage.warning('请先填写代码')
  if (!practiceMode.value && mode === 'draft') {
    syncProgrammingAnswer()
    return ElMessage.success('当前代码已保存')
  }
  running.value = true
  try {
    const res = await submitGrowthPracticeAnswer({ questionId: currentQuestion.value.id, mode, language: selectedLanguage.value, code: code.value, input: mode === 'debug' ? (customInput.value || sampleCase.value.input || '') : '' })
    runResult.value = res.data
    pushHistory(mode, res.data)
  } finally {
    running.value = false
  }
}

const submitObjective = async () => {
  if (!textAnswer.value) return ElMessage.warning('请先完成作答')
  if (!practiceMode.value) {
    syncObjectiveAnswer()
    return ElMessage.success('当前答案已保存')
  }
  running.value = true
  try {
    const res = await submitGrowthPracticeAnswer({ questionId: currentQuestion.value.id, answer: textAnswer.value })
    runResult.value = res.data
    pushHistory('submit', res.data)
  } finally {
    running.value = false
  }
}

const submitExamPaper = async () => {
  syncCurrentAnswer()
  submittingExam.value = true
  try {
    const answers = questionList.value.map((item) => ({ questionId: item.id, answer: examAnswers[item.id]?.answer || '', language: examAnswers[item.id]?.language || '', code: examAnswers[item.id]?.code || '' }))
    const res = await submitStudentExam({ labId: Number(route.params.labId), answers })
    examSubmitted.value = true
    examResult.value = res.data
    ElMessage.success('正式笔试已提交')
  } finally {
    submittingExam.value = false
  }
}

const handleAnalyze = async () => {
  analyzing.value = true
  try {
    const res = await analyzeGradPathCode({ questionTitle: currentQuestion.value.title, code: code.value, errorMsg: runResult.value?.error || runResult.value?.stderr || runResult.value?.message || 'Wrong Answer', output: sampleCase.value.output || '' })
    aiAnalysis.value = String(res.data || '')
  } finally {
    analyzing.value = false
  }
}

const pushHistory = (mode, result) => {
  submissionHistory.value.unshift({ id: `${Date.now()}-${Math.random()}`, mode: mode === 'debug' ? '自测运行' : '提交答案', at: formatDateTime(new Date()), statusText: result?.correct === true ? 'Accepted' : result?.status === 'wrong_answer' ? 'Wrong Answer' : result?.status === 'success' ? 'Success' : 'Error', tagType: result?.correct === true ? 'success' : result?.status === 'wrong_answer' ? 'warning' : result?.status === 'success' ? 'success' : 'danger' })
}

const syncProgrammingAnswer = () => {
  if (practiceMode.value || !currentQuestion.value || currentQuestion.value.questionType !== 'programming') return
  if (!selectedLanguage.value) return
  examAnswers[currentQuestion.value.id] = { questionId: currentQuestion.value.id, language: selectedLanguage.value, code: code.value }
}
const syncObjectiveAnswer = () => {
  if (practiceMode.value || !currentQuestion.value || currentQuestion.value.questionType === 'programming') return
  examAnswers[currentQuestion.value.id] = { questionId: currentQuestion.value.id, answer: textAnswer.value }
}
const syncCurrentAnswer = () => { if (!practiceMode.value && currentQuestion.value) { currentQuestion.value.questionType === 'programming' ? syncProgrammingAnswer() : syncObjectiveAnswer() } }
const isQuestionAnswered = (questionId) => Boolean(examAnswers[questionId]?.answer || examAnswers[questionId]?.code)
const goNext = async () => { if (!questionList.value.length) return; syncCurrentAnswer(); const index = questionList.value.findIndex((item) => item.id === currentQuestion.value?.id); const nextIndex = index >= 0 ? (index + 1) % questionList.value.length : 0; await selectQuestion(questionList.value[nextIndex].id) }
const refreshCurrentMode = () => practiceMode.value ? fetchPracticeQuestions() : fetchExamQuestions()
const handlePageSizeChange = () => { pagination.pageNum = 1; fetchPracticeQuestions() }
const parseSampleCase = (value) => { if (!value) return { input: '', output: '' }; if (typeof value === 'object') return value; try { return JSON.parse(value) } catch { return { input: '', output: '' } } }
const normalizeTags = (value) => { if (!value) return []; if (Array.isArray(value)) return value; if (typeof value === 'string') { try { return JSON.parse(value) } catch { return value.split(',').map((item) => item.trim()).filter(Boolean) } } return [] }
const handleEditorScroll = (event) => { if (lineGutterRef.value) lineGutterRef.value.scrollTop = event.target.scrollTop }
const formatDateTime = (value) => { const date = value instanceof Date ? value : new Date(value); if (Number.isNaN(date.getTime())) return '-'; const year = date.getFullYear(); const month = String(date.getMonth() + 1).padStart(2, '0'); const day = String(date.getDate()).padStart(2, '0'); const hour = String(date.getHours()).padStart(2, '0'); const minute = String(date.getMinutes()).padStart(2, '0'); return `${year}-${month}-${day} ${hour}:${minute}` }
const syncEnvironmentStatus = (status = {}, details = {}) => {
  Object.keys(runtimeEnvironmentStatus).forEach((key) => delete runtimeEnvironmentStatus[key])
  Object.entries(status || {}).forEach(([key, value]) => { runtimeEnvironmentStatus[key] = Boolean(value) })
  Object.keys(runtimeEnvironmentDetails).forEach((key) => delete runtimeEnvironmentDetails[key])
  Object.entries(details || {}).forEach(([key, value]) => { runtimeEnvironmentDetails[key] = value })
}
const goBack = () => router.push(practiceMode.value ? '/student/guide' : '/student/labs')
const goToInterview = () => router.push(`/student/guide/interview?track=${trackCode.value}`)

const handleEditorKeydown = (event) => {
  if (event.key !== 'Tab') return
  event.preventDefault()
  const start = event.target.selectionStart
  const end = event.target.selectionEnd
  code.value = `${code.value.slice(0, start)}  ${code.value.slice(end)}`
  requestAnimationFrame(() => { if (codeEditorRef.value) codeEditorRef.value.selectionStart = codeEditorRef.value.selectionEnd = start + 2 })
}

const initializePage = async () => {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.questionType = typeof route.query.type === 'string' ? route.query.type : ''
  questionList.value = []
  currentQuestion.value = null
  runResult.value = null
  aiAnalysis.value = ''
  submissionHistory.value = []
  code.value = ''
  textAnswer.value = ''
  customInput.value = ''
  if (practiceMode.value) {
    examContext.lab = null
    examContext.exam = null
    examSubmitted.value = false
    examResult.value = null
    await fetchPracticeQuestions()
  } else {
    await fetchExamQuestions()
  }
}

watch(() => route.fullPath, initializePage)
watch(trackCode, () => { if (practiceMode.value) { pagination.pageNum = 1; fetchPracticeQuestions() } })
watch(code, () => { if (!practiceMode.value && currentQuestion.value?.questionType === 'programming' && !examSubmitted.value) syncProgrammingAnswer() })
watch(selectedLanguage, () => { if (!practiceMode.value && currentQuestion.value?.questionType === 'programming' && !examSubmitted.value) syncProgrammingAnswer() })
watch(textAnswer, () => { if (!practiceMode.value && currentQuestion.value && currentQuestion.value.questionType !== 'programming' && !examSubmitted.value) syncObjectiveAnswer() })
onMounted(() => { questionListVisible.value = window.innerWidth > 1280; initializePage() })
</script>

<style scoped>
.exam-page { display: flex; flex-direction: column; gap: 14px; min-height: calc(100vh - 120px); }
.topbar, .panel { border: 1px solid rgba(148,163,184,.16); border-radius: 18px; }
.topbar, .topbar-group, .status-row, .question-meta, .question-foot, .option-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.topbar { justify-content: space-between; padding: 14px 18px; background: rgba(255,255,255,.94); }
.title-block { display: grid; gap: 4px; }
.title-block strong { color: #0f172a; font-size: 18px; }
.title-block span, .status-card span, .eyebrow { color: #64748b; font-size: 13px; }
.status-card { min-width: 120px; padding: 10px 14px; border-radius: 14px; background: linear-gradient(180deg,#fff,#f8fbff); border: 1px solid rgba(148,163,184,.16); }
.status-card strong { display: block; margin-top: 6px; color: #0f172a; }
.workspace { flex: 1; min-height: 0; display: grid; grid-template-columns: 320px minmax(340px,.95fr) minmax(420px,1.05fr); gap: 14px; }
.workspace.collapsed { grid-template-columns: 0 minmax(420px,1fr) minmax(460px,1.05fr); }
.sidebar, .detail-panel { background: rgba(255,255,255,.96); }
.answer-panel { background: #0f172a; color: #e2e8f0; }
.panel { min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.sidebar { transition: opacity .2s ease, transform .2s ease; }
.workspace.collapsed .sidebar { opacity: 0; pointer-events: none; transform: translateX(-24px); }
.panel-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; padding: 16px 18px; }
.panel-head h3 { margin: 0; color: inherit; }
.detail-panel .panel-head h3 { color: #0f172a; }
.toolbar { padding: 0 18px 16px; display: grid; gap: 12px; }
.question-list { flex: 1; min-height: 0; overflow-y: auto; padding: 0 18px 16px; display: grid; gap: 10px; }
.question-item { width: 100%; padding: 14px; text-align: left; cursor: pointer; border-radius: 16px; border: 1px solid rgba(148,163,184,.16); background: linear-gradient(180deg,#fff,#f8fbff); }
.question-item.active, .question-item:hover { border-color: rgba(16,185,129,.42); box-shadow: 0 14px 24px rgba(15,23,42,.08); transform: translateY(-2px); }
.question-item strong { display: block; margin-top: 10px; color: #0f172a; }
.question-item p { margin: 10px 0 0; color: #64748b; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.question-foot { margin-top: 12px; color: #64748b; font-size: 12px; }
.pager { padding: 14px 18px 18px; border-top: 1px solid rgba(148,163,184,.12); }
.detail-body { flex: 1; min-height: 0; overflow-y: auto; padding: 0 18px 18px; }
.detail-block { padding: 16px 0; border-bottom: 1px solid rgba(148,163,184,.12); }
.detail-block:last-child { border-bottom: none; }
.detail-block h4 { margin: 0 0 10px; color: #0f172a; }
.detail-block p, .detail-block pre { margin: 0; line-height: 1.8; color: #334155; white-space: pre-wrap; word-break: break-word; }
.split { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 16px; }
.option-list, .tag-list { display: grid; gap: 10px; }
.option-row { padding: 12px 14px; border-radius: 14px; background: #f8fafc; }
.answer-body { display: flex; flex-direction: column; min-height: 0; flex: 1; }
.editor-wrap { flex: 1; min-height: 320px; display: grid; grid-template-columns: 64px minmax(0,1fr); background: #111827; }
.line-gutter { overflow: hidden; padding: 16px 10px 16px 0; text-align: right; background: #111827; color: #64748b; border-right: 1px solid rgba(148,163,184,.12); font-family: Consolas,'Courier New',monospace; line-height: 1.75; }
.line-gutter span { display: block; height: 28px; }
.code-editor { width: 100%; height: 100%; resize: none; border: none; outline: none; padding: 16px 18px; background: #111827; color: #d1fae5; font-size: 15px; line-height: 1.75; font-family: Consolas,'Courier New',monospace; }
.objective-wrap { padding: 18px; display: grid; gap: 12px; background: #111827; }
.objective-item { display: grid; grid-template-columns: 24px minmax(0,1fr); gap: 12px; align-items: center; padding: 14px 16px; border-radius: 16px; background: rgba(30,41,59,.86); border: 1px solid rgba(148,163,184,.18); cursor: pointer; }
.objective-item.active { border-color: rgba(16,185,129,.42); background: rgba(16,185,129,.12); }
.fill-wrap { display: grid; gap: 12px; }
.fill-wrap strong { color: #e2e8f0; }
.custom-input, .result-box, .history-box { padding: 18px; border-top: 1px solid rgba(148,163,184,.12); }
.result-box { background: #f8fafc; color: #0f172a; }
.result-output { margin-top: 12px; padding: 16px; border-radius: 16px; background: #0f172a; color: #e2e8f0; white-space: pre-wrap; word-break: break-word; line-height: 1.7; }
.analysis-actions { margin-top: 12px; }
.analysis-box { margin-top: 14px; padding: 16px; border-radius: 16px; background: linear-gradient(180deg,#fff7ed,#fffbeb); border: 1px solid rgba(245,158,11,.2); color: #7c2d12; line-height: 1.8; white-space: pre-wrap; word-break: break-word; }
.history-box { display: grid; gap: 12px; }
.history-item { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding: 12px 14px; border-radius: 14px; background: rgba(30,41,59,.7); }
@media (max-width: 1200px) { .workspace, .workspace.collapsed { grid-template-columns: 1fr; } .sidebar { opacity: 1; pointer-events: auto; transform: none; } .split { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .topbar, .status-row { flex-direction: column; align-items: stretch; } .topbar-group { width: 100%; align-items: flex-start; } .topbar-group, .question-meta { flex-direction: column; } }
</style>

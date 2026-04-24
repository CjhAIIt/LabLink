<template>
  <div class="exam-take" @copy.prevent @paste.prevent @contextmenu.prevent>
    <!-- Cheat Warning Overlay -->
    <transition name="fade">
      <div v-if="showCheatWarning" class="cheat-overlay">
        <div class="cheat-dialog">
          <el-icon :size="48" color="#f56c6c"><WarningFilled /></el-icon>
          <h2>异常行为警告</h2>
          <p>检测到您切换了页面（第 {{ switchCount }} 次）</p>
          <p v-if="switchCount < 5">累计 5 次将自动提交试卷！</p>
          <p v-else>已达上限，试卷将自动提交。</p>
          <el-button type="danger" round @click="showCheatWarning = false">我知道了</el-button>
        </div>
      </div>
    </transition>

    <!-- Top Bar -->
    <header class="top-bar">
      <div class="top-bar__left">
        <el-icon :size="20" color="#409eff"><EditPen /></el-icon>
        <span class="exam-title">{{ examTitle }}</span>
      </div>
      <div class="top-bar__center">
        <el-icon :size="18"><Timer /></el-icon>
        <span class="countdown" :class="{ 'countdown--danger': remainingSeconds < 300 }">
          {{ formattedTime }}
        </span>
      </div>
      <div class="top-bar__right">
        <span class="save-indicator" :class="`save--${autoSaveStatus}`">
          <template v-if="autoSaveStatus === 'saving'">
            <el-icon class="is-loading"><Loading /></el-icon> 保存中...
          </template>
          <template v-else-if="autoSaveStatus === 'saved'">
            <el-icon><CircleCheckFilled /></el-icon> 已保存
          </template>
          <template v-else>
            <el-icon><InfoFilled /></el-icon> 未保存
          </template>
        </span>
      </div>
    </header>

    <!-- Main Content -->
    <div class="exam-body">
      <!-- Left Sidebar -->
      <aside class="sidebar">
        <div class="sidebar__title">题目导航</div>
        <div class="nav-grid">
          <button
            v-for="(q, idx) in questions"
            :key="q.id"
            class="nav-btn"
            :class="{
              'nav-btn--active': idx === currentIndex,
              'nav-btn--answered': isAnswered(q.id) && !flagged.has(q.id),
              'nav-btn--flagged': flagged.has(q.id)
            }"
            @click="currentIndex = idx"
          >
            {{ idx + 1 }}
          </button>
        </div>
        <div class="sidebar__legend">
          <span><i class="dot dot--answered"></i>已答</span>
          <span><i class="dot dot--unanswered"></i>未答</span>
          <span><i class="dot dot--flagged"></i>标记</span>
        </div>
        <div v-if="currentQuestion" class="sidebar__type">
          {{ questionTypeLabel(currentQuestion.type) }}
        </div>
        <div class="sidebar__stats">
          <p>已答：{{ answeredCount }} / {{ questions.length }}</p>
          <p>标记：{{ flagged.size }} 题</p>
        </div>
      </aside>

      <!-- Main Area -->
      <main class="main-area">
        <transition name="slide" mode="out-in">
          <div v-if="currentQuestion" :key="currentQuestion.id" class="question-card">
            <div class="question-header">
              <span class="question-num">第 {{ currentIndex + 1 }} 题</span>
              <el-tag :type="questionTagType(currentQuestion.type)" size="small" effect="dark" round>
                {{ questionTypeLabel(currentQuestion.type) }}
              </el-tag>
              <span class="question-score">{{ currentQuestion.score }} 分</span>
            </div>
            <div class="question-content" v-html="currentQuestion.content"></div>

            <!-- Single Choice -->
            <div v-if="currentQuestion.type === 'single_choice'" class="choice-list">
              <div
                v-for="(opt, oi) in currentQuestion.options"
                :key="oi"
                class="choice-card"
                :class="{ 'choice-card--selected': currentAnswer.answer === opt.key }"
                @click="setAnswer(opt.key)"
              >
                <span class="choice-key">{{ opt.key }}</span>
                <span class="choice-text">{{ opt.value }}</span>
              </div>
            </div>

            <!-- Judge (True/False) -->
            <div v-else-if="currentQuestion.type === 'judge'" class="choice-list">
              <div
                class="choice-card"
                :class="{ 'choice-card--selected': currentAnswer.answer === 'true' }"
                @click="setAnswer('true')"
              >
                <span class="choice-key">✓</span>
                <span class="choice-text">正确</span>
              </div>
              <div
                class="choice-card"
                :class="{ 'choice-card--selected': currentAnswer.answer === 'false' }"
                @click="setAnswer('false')"
              >
                <span class="choice-key">✗</span>
                <span class="choice-text">错误</span>
              </div>
            </div>

            <!-- Fill Blank -->
            <div v-else-if="currentQuestion.type === 'fill_blank'" class="fill-area">
              <el-input
                v-model="currentAnswer.answer"
                type="textarea"
                :rows="4"
                placeholder="请输入答案..."
                resize="vertical"
                class="fill-input"
                @input="markUnsaved"
              />
            </div>

            <!-- Programming -->
            <div v-else-if="currentQuestion.type === 'programming'" class="code-area">
              <div class="code-toolbar">
                <span class="code-toolbar__label">编程语言：</span>
                <el-select v-model="currentAnswer.language" size="small" style="width:130px" @change="onLanguageChange">
                  <el-option label="Java" value="java" />
                  <el-option label="Python" value="python" />
                  <el-option label="C" value="c" />
                  <el-option label="C++" value="cpp" />
                </el-select>
                <el-button size="small" :loading="compiling" @click="runCode('debug')">
                  <el-icon><VideoPlay /></el-icon> 编译运行
                </el-button>
                <el-button size="small" type="primary" :loading="compiling" @click="runCode('submit')">
                  <el-icon><CircleCheckFilled /></el-icon> 提交判题
                </el-button>
              </div>
              <div class="code-editor">
                <div class="line-gutter" ref="gutterRef">
                  <div v-for="n in lineCount" :key="n" class="line-num">{{ n }}</div>
                </div>
                <textarea
                  ref="codeRef"
                  v-model="currentAnswer.code"
                  class="code-textarea"
                  spellcheck="false"
                  wrap="off"
                  @keydown.tab.prevent="insertTab"
                  @scroll="syncScroll"
                  @input="markUnsaved"
                ></textarea>
              </div>
              <div v-if="codeResult" class="code-result" :class="`code-result--${codeResultType}`">
                <div class="code-result__header">
                  <el-tag :type="codeResultType" size="small" effect="dark">{{ codeResultLabel }}</el-tag>
                  <span v-if="codeResult.time" class="code-result__meta">耗时: {{ codeResult.time }}s</span>
                  <span v-if="codeResult.memory" class="code-result__meta">内存: {{ codeResult.memory }}KB</span>
                </div>
                <pre v-if="codeResult.stdout" class="code-result__output">{{ codeResult.stdout }}</pre>
                <pre v-if="codeResult.stderr" class="code-result__output code-result__error">{{ codeResult.stderr }}</pre>
                <pre v-if="codeResult.compile_output" class="code-result__output code-result__error">{{ codeResult.compile_output }}</pre>
                <pre v-if="codeResult.message" class="code-result__output">{{ codeResult.message }}</pre>
              </div>
            </div>
          </div>
        </transition>
      </main>
    </div>

    <!-- Bottom Bar -->
    <footer class="bottom-bar">
      <div class="bottom-bar__left">
        <el-button :disabled="currentIndex === 0" round @click="prevQuestion">
          <el-icon><ArrowLeft /></el-icon> 上一题
        </el-button>
        <el-button :disabled="currentIndex === questions.length - 1" round @click="nextQuestion">
          下一题 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="bottom-bar__right">
        <el-button
          :type="flagged.has(currentQuestion?.id) ? 'warning' : 'default'"
          round
          @click="toggleFlag"
        >
          <el-icon><StarFilled /></el-icon>
          {{ flagged.has(currentQuestion?.id) ? '取消标记' : '标记本题' }}
        </el-button>
        <el-button type="danger" round @click="confirmSubmit">
          <el-icon><CircleCheckFilled /></el-icon> 交卷
        </el-button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  WarningFilled, EditPen, Timer, Loading, CircleCheckFilled,
  InfoFilled, ArrowLeft, ArrowRight, StarFilled, VideoPlay
} from '@element-plus/icons-vue'
import {
  startExam, saveExamAnswer, submitExamPaper, getExamProgress, reportCheatEvent,
  runExamCode, judgeExamCode
} from '@/api/writtenExam'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId

const examTitle = ref('')
const questions = ref([])
const currentIndex = ref(0)
const answers = reactive({})
const flagged = ref(new Set())
const remainingSeconds = ref(0)
const switchCount = ref(0)
const autoSaveStatus = ref('unsaved')
const showCheatWarning = ref(false)
const gutterRef = ref(null)
const codeRef = ref(null)
const compiling = ref(false)
const codeResult = ref(null)

let timerInterval = null
let autoSaveInterval = null

const STORAGE_KEY = `exam_progress_${examId}`

const codeTemplates = {
  java: 'public class Main {\n  public static void main(String[] args) {\n    // 在此编写代码\n    \n  }\n}',
  python: '# 在此编写代码\n\n',
  c: '#include <stdio.h>\n\nint main() {\n  // 在此编写代码\n  \n  return 0;\n}',
  cpp: '#include <iostream>\nusing namespace std;\n\nint main() {\n  // 在此编写代码\n  \n  return 0;\n}'
}

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

const currentAnswer = computed(() => {
  if (!currentQuestion.value) return {}
  const qid = currentQuestion.value.id
  if (!answers[qid]) {
    answers[qid] = { answer: '', code: '', language: 'java' }
  }
  return answers[qid]
})

const formattedTime = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const lineCount = computed(() => {
  const code = currentAnswer.value?.code || ''
  return Math.max(code.split('\n').length, 20)
})

const answeredCount = computed(() => {
  return questions.value.filter(q => isAnswered(q.id)).length
})

function isAnswered(qid) {
  const a = answers[qid]
  if (!a) return false
  return !!(a.answer || a.code)
}

function questionTypeLabel(type) {
  const map = { single_choice: '单选题', judge: '判断题', fill_blank: '填空题', programming: '编程题' }
  return map[type] || type
}

function questionTagType(type) {
  const map = { single_choice: 'primary', judge: 'info', fill_blank: 'success', programming: 'warning' }
  return map[type] || 'info'
}

function setAnswer(key) {
  currentAnswer.value.answer = key
  markUnsaved()
}

function markUnsaved() {
  autoSaveStatus.value = 'unsaved'
}

function onLanguageChange(lang) {
  if (!currentAnswer.value.code || currentAnswer.value.code === codeTemplates[currentAnswer.value.language]) {
    currentAnswer.value.code = codeTemplates[lang] || ''
  }
}

function insertTab(e) {
  const ta = e.target
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const val = ta.value
  currentAnswer.value.code = val.substring(0, start) + '  ' + val.substring(end)
  nextTick(() => { ta.selectionStart = ta.selectionEnd = start + 2 })
  markUnsaved()
}

function syncScroll() {
  if (gutterRef.value && codeRef.value) {
    gutterRef.value.scrollTop = codeRef.value.scrollTop
  }
}

const codeResultType = computed(() => {
  if (!codeResult.value) return 'info'
  const s = codeResult.value.status
  if (s === 'success' || codeResult.value.correct === true) return 'success'
  if (s === 'wrong_answer') return 'warning'
  return 'danger'
})

const codeResultLabel = computed(() => {
  if (!codeResult.value) return ''
  const s = codeResult.value.status
  if (codeResult.value.correct === true) return 'Accepted'
  if (s === 'success') return '编译通过'
  if (s === 'wrong_answer') return 'Wrong Answer'
  if (s === 'compile_error') return '编译错误'
  if (s === 'runtime_error') return '运行错误'
  if (s === 'time_limit') return '超时'
  return '执行结果'
})

async function runCode(mode) {
  if (!currentQuestion.value || !currentAnswer.value.code?.trim()) {
    return ElMessage.warning('请先编写代码')
  }
  compiling.value = true
  codeResult.value = null
  try {
    const payload = {
      questionId: currentQuestion.value.id,
      language: currentAnswer.value.language || 'java',
      code: currentAnswer.value.code
    }
    let res
    if (mode === 'submit') {
      res = await judgeExamCode(payload)
    } else {
      payload.input = ''
      res = await runExamCode(payload)
    }
    codeResult.value = res.data
    markUnsaved()
  } catch (e) {
    codeResult.value = { status: 'error', message: e.response?.data?.message || '编译运行失败，请稍后重试' }
  } finally {
    compiling.value = false
  }
}

function prevQuestion() { if (currentIndex.value > 0) currentIndex.value-- }
function nextQuestion() { if (currentIndex.value < questions.value.length - 1) currentIndex.value++ }

function toggleFlag() {
  if (!currentQuestion.value) return
  const id = currentQuestion.value.id
  const newSet = new Set(flagged.value)
  if (newSet.has(id)) newSet.delete(id)
  else newSet.add(id)
  flagged.value = newSet
}

function buildAnswerList() {
  return questions.value.map(q => ({
    questionId: q.id,
    answer: answers[q.id]?.answer || '',
    code: answers[q.id]?.code || '',
    language: answers[q.id]?.language || 'java'
  }))
}

async function doAutoSave() {
  if (autoSaveStatus.value === 'saved') return
  try {
    autoSaveStatus.value = 'saving'
    await saveExamAnswer({ examId, answers: buildAnswerList() })
    autoSaveStatus.value = 'saved'
  } catch { autoSaveStatus.value = 'unsaved' }
}

function saveToLocal() {
  try {
    const data = { answers: { ...answers }, flagged: [...flagged.value], currentIndex: currentIndex.value, remainingSeconds: remainingSeconds.value }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  } catch {}
}

function restoreFromLocal() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return false
    const data = JSON.parse(raw)
    if (data.answers) Object.assign(answers, data.answers)
    if (data.flagged) flagged.value = new Set(data.flagged)
    if (data.currentIndex != null) currentIndex.value = data.currentIndex
    return true
  } catch { return false }
}

async function doSubmit() {
  try {
    await submitExamPaper({ examId, answers: buildAnswerList() })
    cleanup()
    localStorage.removeItem(STORAGE_KEY)
    ElMessage.success('试卷已提交')
    router.replace(`/student/exam-center/${examId}/result`)
  } catch { ElMessage.error('提交失败，请重试') }
}

function confirmSubmit() {
  const unanswered = questions.value.length - answeredCount.value
  ElMessageBox.confirm(
    `确认提交？${unanswered > 0 ? `还有 ${unanswered} 题未答，未答题目将不计分。` : ''}`,
    '提交试卷',
    { confirmButtonText: '确认提交', cancelButtonText: '继续答题', type: 'warning' }
  ).then(() => doSubmit()).catch(() => {})
}

function startCountdown() {
  timerInterval = setInterval(() => {
    if (remainingSeconds.value <= 0) { clearInterval(timerInterval); doSubmit(); return }
    remainingSeconds.value--
  }, 1000)
}

function handleVisibilityChange() {
  if (document.hidden) {
    switchCount.value++
    reportCheatEvent({ examId, eventType: 'tab_switch', detail: `第${switchCount.value}次切换` }).catch(() => {})
    if (switchCount.value >= 5) { showCheatWarning.value = true; setTimeout(() => doSubmit(), 2000) }
    else if (switchCount.value >= 3) { showCheatWarning.value = true }
  }
}

function handleBeforeUnload(e) {
  saveToLocal()
  e.preventDefault()
  e.returnValue = ''
}

function cleanup() {
  clearInterval(timerInterval)
  clearInterval(autoSaveInterval)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
}

watch(currentIndex, () => {
  codeResult.value = null
})

onMounted(async () => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('beforeunload', handleBeforeUnload)

  try {
    const res = await startExam(examId)
    const data = res.data || res
    examTitle.value = data.title || '在线笔试'
    questions.value = data.questions || []
    remainingSeconds.value = data.remainingSeconds || data.duration * 60 || 3600

    // init answer slots
    questions.value.forEach(q => {
      if (!answers[q.id]) {
        answers[q.id] = { answer: '', code: q.type === 'programming' ? codeTemplates.java : '', language: 'java' }
      }
    })

    // restore progress
    try {
      const prog = await getExamProgress(examId)
      const pd = prog.data || prog
      if (pd && pd.answers) {
        pd.answers.forEach(a => { answers[a.questionId] = { answer: a.answer || '', code: a.code || '', language: a.language || 'java' } })
      }
    } catch { restoreFromLocal() }

    startCountdown()
    autoSaveInterval = setInterval(() => { doAutoSave(); saveToLocal() }, 30000)
  } catch (err) {
    ElMessage.error('加载考试失败')
    console.error(err)
  }
})

onBeforeUnmount(() => { cleanup() })
</script>

<style scoped>
.exam-take {
  position: fixed; inset: 0; background: #f5f7fa; color: #303133;
  display: flex; flex-direction: column; font-family: 'Inter', sans-serif;
  user-select: none; overflow: hidden;
}
/* Cheat overlay */
.cheat-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(220, 38, 38, 0.25); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
}
.cheat-dialog {
  background: #fff; border: 2px solid #f56c6c; border-radius: 8px;
  padding: 40px 48px; text-align: center;
}
.cheat-dialog h2 { color: #f56c6c; margin: 16px 0 8px; font-size: 22px; }
.cheat-dialog p { color: #606266; margin: 4px 0; }
/* Top bar */
.top-bar {
  height: 50px; background: #fff; border-bottom: 1px solid #e4e7ed;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; flex-shrink: 0; z-index: 100;
}
.top-bar__left { display: flex; align-items: center; gap: 10px; }
.exam-title { font-size: 14px; font-weight: 600; color: #303133; }
.top-bar__center { display: flex; align-items: center; gap: 8px; }
.countdown { font-size: 20px; font-weight: 700; font-variant-numeric: tabular-nums; color: #409eff; letter-spacing: 1px; }
.countdown--danger { color: #f56c6c; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.5} }
.save-indicator { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.save--saved { color: #67c23a; }
.save--saving { color: #e6a23c; }
.save--unsaved { color: #909399; }
/* Body */
.exam-body { display: flex; flex: 1; overflow: hidden; }
/* Sidebar */
.sidebar {
  width: 240px; background: #fff; border-right: 1px solid #e4e7ed;
  padding: 20px 16px; display: flex; flex-direction: column; gap: 16px;
  overflow-y: auto; flex-shrink: 0;
}
.sidebar__title { font-size: 14px; font-weight: 600; color: #909399; text-transform: uppercase; letter-spacing: 1px; }
.nav-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.nav-btn {
  width: 36px; height: 36px; border-radius: 50%; border: 2px solid #e4e7ed;
  background: #e4e7ed; color: #909399; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all .2s; display: flex; align-items: center; justify-content: center;
}
.nav-btn:hover { border-color: #409eff; color: #e2e8f0; }
.nav-btn--active { border-color: #409eff; background: #409eff; color: #fff; }
.nav-btn--answered { background: #67c23a; border-color: #67c23a; color: #fff; }
.nav-btn--flagged { background: #e6a23c; border-color: #e6a23c; color: #fff; }
.sidebar__legend { display: flex; gap: 12px; font-size: 12px; color: #909399; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
.dot--answered { background: #67c23a; }
.dot--unanswered { background: #e4e7ed; }
.dot--flagged { background: #e6a23c; }
.sidebar__type { font-size: 13px; color: #409eff; font-weight: 600; }
.sidebar__stats { font-size: 13px; color: #909399; }
.sidebar__stats p { margin: 4px 0; }
/* Main area */
.main-area { flex: 1; overflow-y: auto; padding: 28px 36px; }
.question-card { background: #fff; border-radius: 12px; padding: 28px 32px; border: 1px solid #e4e7ed; }
.question-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.question-num { font-size: 16px; font-weight: 700; color: #409eff; }
.question-score { margin-left: auto; font-size: 13px; color: #909399; }
.question-content { font-size: 15px; line-height: 1.8; color: #303133; margin-bottom: 24px; }
/* Choice cards */
.choice-list { display: flex; flex-direction: column; gap: 10px; }
.choice-card {
  display: flex; align-items: center; gap: 14px; padding: 14px 18px;
  background: #f5f7fa; border: 2px solid #e4e7ed; border-radius: 10px;
  cursor: pointer; transition: all .2s;
}
.choice-card:hover { border-color: #c6e2ff; background: #ecf5ff; }
.choice-card--selected { border-color: #409eff; background: #ecf5ff; }
.choice-key {
  width: 32px; height: 32px; border-radius: 50%; background: #e4e7ed;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; color: #e2e8f0; flex-shrink: 0;
}
.choice-card--selected .choice-key { background: #409eff; color: #fff; }
.choice-text { font-size: 14px; color: #303133; }
/* Fill blank */
.fill-area { max-width: 700px; }
.fill-input :deep(.el-textarea__inner) {
  background: #fff; color: #303133; border: 1px solid #e4e7ed;
  font-size: 15px; border-radius: 8px;
}
/* Code editor */
.code-area { display: flex; flex-direction: column; gap: 10px; }
.code-toolbar { display: flex; align-items: center; gap: 10px; }
.code-toolbar__label { font-size: 13px; color: #909399; }
.code-editor {
  display: flex; height: 400px; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden;
}
.line-gutter {
  width: 48px; background: #fafafa; padding: 10px 0; overflow: hidden;
  text-align: right; flex-shrink: 0;
}
.line-num { height: 22px; line-height: 22px; padding-right: 10px; font-size: 12px; color: #c0c4cc; font-family: Consolas, monospace; }
.code-textarea {
  flex: 1; background: #fff; color: #303133; border: none; outline: none;
  padding: 10px 14px; font-family: Consolas, 'Courier New', monospace;
  font-size: 14px; line-height: 22px; resize: none; tab-size: 2;
}
/* Code result */
.code-result {
  border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px 18px; background: #fafafa;
}
.code-result--success { border-color: #b3e19d; background: #f0f9eb; }
.code-result--warning { border-color: #f3d19e; background: #fdf6ec; }
.code-result--danger { border-color: #fab6b6; background: #fef0f0; }
.code-result__header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.code-result__meta { font-size: 12px; color: #909399; }
.code-result__output {
  margin: 6px 0 0; padding: 10px 14px; border-radius: 6px; background: #1e1e1e; color: #d4d4d4;
  font-family: Consolas, 'Courier New', monospace; font-size: 13px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word; max-height: 200px; overflow-y: auto;
}
.code-result__error { color: #f56c6c; }
/* Bottom bar */
.bottom-bar {
  height: 60px; background: #fff; border-top: 1px solid #e4e7ed;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; flex-shrink: 0;
}
.bottom-bar__left, .bottom-bar__right { display: flex; gap: 10px; }
/* Transitions */
.slide-enter-active, .slide-leave-active { transition: all .25s ease; }
.slide-enter-from { opacity: 0; transform: translateX(30px); }
.slide-leave-to { opacity: 0; transform: translateX(-30px); }
.fade-enter-active, .fade-leave-active { transition: opacity .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
/* Scrollbar */
.main-area::-webkit-scrollbar, .sidebar::-webkit-scrollbar { width: 6px; }
.main-area::-webkit-scrollbar-thumb, .sidebar::-webkit-scrollbar-thumb { background: #e4e7ed; border-radius: 3px; }
.main-area::-webkit-scrollbar-track, .sidebar::-webkit-scrollbar-track { background: transparent; }
</style>

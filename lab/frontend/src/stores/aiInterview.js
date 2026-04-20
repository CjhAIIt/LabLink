import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAiInterviewStore = defineStore('aiInterview', () => {
  // ---- 面试配置 ----
  const mode = ref('mock')            // 'mock' | 'formal'
  const moduleId = ref(null)
  const moduleName = ref('')
  const sessionId = ref(null)

  // ---- 对话状态 ----
  const chatHistory = ref([])          // { role: 'user'|'assistant', content: string }
  const currentQuestion = ref('')
  const isAIThinking = ref(false)
  const questionCount = ref(0)
  const maxQuestions = ref(10)

  // ---- 面试阶段 ----
  const phase = ref('idle')            // idle | interviewing | finished | report
  const report = ref(null)             // { score, tags, summary, strengths, weaknesses, suggestions }

  // ---- 正式面试 ----
  const remainingChances = ref(2)

  // ---- 计算属性 ----
  const isFormal = computed(() => mode.value === 'formal')
  const canStartFormal = computed(() => remainingChances.value > 0)
  const hasReport = computed(() => report.value !== null)

  // ---- 操作 ----
  function setMode(m) { mode.value = m }
  function setModule(id, name) { moduleId.value = id; moduleName.value = name }
  function setSessionId(id) { sessionId.value = id }
  function setRemainingChances(n) { remainingChances.value = n }

  function addMessage(role, content) {
    chatHistory.value.push({ role, content })
    if (role === 'assistant') {
      questionCount.value++
      currentQuestion.value = content
    }
  }

  function setPhase(p) { phase.value = p }
  function setReport(r) { report.value = r }
  function setAIThinking(v) { isAIThinking.value = v }

  function reset() {
    moduleId.value = null
    moduleName.value = ''
    sessionId.value = null
    chatHistory.value = []
    currentQuestion.value = ''
    isAIThinking.value = false
    questionCount.value = 0
    phase.value = 'idle'
    report.value = null
  }

  return {
    mode, moduleId, moduleName, sessionId,
    chatHistory, currentQuestion, isAIThinking, questionCount, maxQuestions,
    phase, report, remainingChances,
    isFormal, canStartFormal, hasReport,
    setMode, setModule, setSessionId, setRemainingChances,
    addMessage, setPhase, setReport, setAIThinking, reset,
  }
})

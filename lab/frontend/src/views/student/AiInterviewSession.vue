<template>
  <div class="interview-session">
    <!-- Top Bar -->
    <div class="session-topbar">
      <div class="topbar-left">
        <span class="mode-tag" :class="store.isFormal ? 'formal' : 'mock'">{{ store.isFormal ? '正式面试' : '模拟练习' }}</span>
        <span class="module-name">{{ store.moduleName }}</span>
      </div>
      <div class="topbar-right">
        <span class="q-counter">{{ store.questionCount }} / {{ store.maxQuestions }} 题</span>
        <el-button size="small" type="danger" plain @click="endInterview" :disabled="store.phase === 'finished'">结束面试</el-button>
      </div>
    </div>

    <!-- Chat Area -->
    <div class="chat-area" ref="chatAreaRef">
      <!-- Welcome -->
      <div class="chat-welcome" v-if="!store.chatHistory.length && !store.isAIThinking">
        <div class="welcome-avatar">🤖</div>
        <p>你好！我是 AI 面试官。准备好了吗？让我们开始 <strong>{{ store.moduleName }}</strong> 方向的面试。</p>
        <el-button type="primary" @click="startChat" :loading="loadingFirst">开始第一题</el-button>
      </div>

      <!-- Messages -->
      <div v-for="(msg, i) in store.chatHistory" :key="i" class="chat-msg" :class="msg.role">
        <div class="msg-avatar">{{ msg.role === 'assistant' ? '🤖' : '🧑‍💻' }}</div>
        <div class="msg-bubble">
          <div class="msg-content">{{ msg.content }}</div>
        </div>
      </div>

      <!-- AI Thinking -->
      <div class="chat-msg assistant" v-if="store.isAIThinking">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble">
          <div class="typing-dots"><span></span><span></span><span></span></div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="input-area" v-if="store.phase === 'interviewing'">
      <div class="input-wrap">
        <el-input
          v-model="userInput" type="textarea" :rows="2" resize="none"
          :placeholder="isRecording ? '正在听写中…说完后点击停止或按 Enter 发送' : '输入你的回答…（或点击麦克风语音输入）'"
          @keydown.enter.exact.prevent="sendAnswer"
          :disabled="store.isAIThinking"
        />
        <button class="mic-btn" :class="{ recording: isRecording }" @click="toggleRecording" :disabled="store.isAIThinking">
          <svg v-if="!isRecording" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10a7 7 0 0014 0"/><line x1="12" y1="19" x2="12" y2="22"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
        </button>
        <button class="send-btn" @click="sendAnswer" :disabled="!userInput.trim() || store.isAIThinking">
          <svg viewBox="0 0 20 20" fill="currentColor"><path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/></svg>
        </button>
      </div>
      <div class="input-hint">
        <span v-if="store.isAIThinking">AI 正在思考中…</span>
        <span v-else-if="isRecording" class="recording-hint">🔴 正在录音…点击停止按钮或按 Enter 发送</span>
        <span v-else-if="speechError">语音识别不可用（{{ speechError }}），请手动输入</span>
        <span v-else>支持文字输入和语音输入</span>
      </div>
    </div>

    <!-- Finished Banner -->
    <div class="finished-banner" v-if="store.phase === 'finished'">
      <p>面试已结束，正在生成评估报告…</p>
      <el-button type="primary" @click="goReport" :loading="generatingReport">查看报告</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAiInterviewStore } from '@/stores/aiInterview'
import { chatWithAI, generateReport, startInterviewSession, finishInterview } from '@/api/aiInterview'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const store = useAiInterviewStore()
const chatAreaRef = ref(null)
const userInput = ref('')
const loadingFirst = ref(false)
const generatingReport = ref(false)
const homePath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview'))
const reportPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/ai-interview/report'))

// ---- 语音识别 ----
const isRecording = ref(false)
const speechError = ref(null)
let recognition = null
let shouldContinue = false
let finalTranscript = ''

function getSpeechCtor() {
  return window.SpeechRecognition || window.webkitSpeechRecognition || null
}

function startRecording() {
  speechError.value = null
  finalTranscript = ''
  shouldContinue = true
  const Ctor = getSpeechCtor()
  if (!Ctor) { speechError.value = '浏览器不支持'; return }
  if (!recognition) {
    recognition = new Ctor()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1
    recognition.onresult = (event) => {
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const text = (event.results[i]?.[0]?.transcript ?? '').trim()
        if (!text) continue
        if (event.results[i].isFinal) { finalTranscript += text } else { interim += text }
      }
      userInput.value = (finalTranscript + interim).trim()
    }
    recognition.onerror = (e) => { speechError.value = e?.error || 'unknown'; shouldContinue = false; isRecording.value = false }
    recognition.onend = () => { if (shouldContinue) { try { recognition.start() } catch { isRecording.value = false } } else { isRecording.value = false } }
  }
  try { recognition.start(); isRecording.value = true } catch { speechError.value = 'start_failed' }
}

function stopRecording() {
  shouldContinue = false
  isRecording.value = false
  if (recognition) { try { recognition.stop() } catch { try { recognition.abort() } catch {} } }
}

function toggleRecording() {
  if (isRecording.value) stopRecording()
  else startRecording()
}

onBeforeUnmount(() => {
  shouldContinue = false
  if (recognition) { try { recognition.abort() } catch {} }
  recognition = null
})

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
  })
}
watch(() => store.chatHistory.length, scrollToBottom)
watch(() => store.isAIThinking, scrollToBottom)

onMounted(() => {
  if (!store.moduleName) { router.replace(homePath.value); return }
  store.setPhase('interviewing')
})

async function startChat() {
  loadingFirst.value = true
  try {
    // 创建会话
    const session = await startInterviewSession({ mode: store.mode, moduleId: store.moduleId })
    if (session?.data?.sessionId) store.setSessionId(session.data.sessionId)
  } catch { /* continue even if session creation fails */ }

  await askAI('请开始第一个问题')
  loadingFirst.value = false
}

async function sendAnswer() {
  if (isRecording.value) stopRecording()
  const text = userInput.value.trim()
  if (!text || store.isAIThinking) return
  store.addMessage('user', text)
  userInput.value = ''
  finalTranscript = ''
  await askAI(text)
}

async function askAI(message) {
  store.setAIThinking(true)
  try {
    const res = await chatWithAI({
      sessionId: store.sessionId,
      message,
      moduleId: store.moduleId,
      moduleName: store.moduleName,
      mode: store.mode,
      chatHistory: store.chatHistory,
    })
    const reply = res?.data?.reply || res?.data?.content || '抱歉，AI 暂时无法回复，请稍后重试。'
    store.addMessage('assistant', reply)

    // 检查是否达到最大题数或 AI 发出结束信号
    if (store.questionCount >= store.maxQuestions || /面试.*结束|到此结束|感谢.*参加/.test(reply)) {
      store.setPhase('finished')
    }
  } catch {
    store.addMessage('assistant', '网络异常，请稍后重试。')
  }
  store.setAIThinking(false)
}

async function endInterview() {
  try {
    await ElMessageBox.confirm('确定要结束本次面试吗？', '提示', { type: 'warning' })
    store.setPhase('finished')
  } catch {}
}

async function goReport() {
  generatingReport.value = true
  try {
    const res = await generateReport({
      sessionId: store.sessionId,
      moduleId: store.moduleId,
      moduleName: store.moduleName,
      mode: store.mode,
      chatHistory: store.chatHistory,
    })
    store.setReport(res?.data || res)

    // 正式面试保存结果
    if (store.isFormal && store.sessionId) {
      try { await finishInterview({ sessionId: store.sessionId, report: res?.data || res }) } catch {}
    }
  } catch {
    store.setReport({ score: 0, tags: [], summary: '报告生成失败，请联系管理员。' })
  }
  generatingReport.value = false
  store.setPhase('report')
  router.push(reportPath.value)
}
</script>

<style scoped>
.interview-session { display: flex; flex-direction: column; min-height: calc(100dvh - 80px); max-width: 860px; margin: 0 auto; padding: 0 16px; }

.session-topbar { display: flex; align-items: center; justify-content: space-between; padding: 14px 0; border-bottom: 1px solid #f1f5f9; flex-shrink: 0; }
.topbar-left { display: flex; align-items: center; gap: 12px; }
.mode-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 20px; }
.mode-tag.mock { background: rgba(16,185,129,.1); color: #10b981; }
.mode-tag.formal { background: rgba(245,158,11,.1); color: #d97706; }
.module-name { font-size: 15px; font-weight: 600; color: #1e293b; }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.q-counter { font-size: 13px; color: #64748b; background: #f8fafc; padding: 4px 12px; border-radius: 8px; }

.chat-area { flex: 1; overflow-y: auto; padding: 24px 0; display: flex; flex-direction: column; gap: 18px; }

.chat-welcome { text-align: center; padding: 60px 20px; }
.welcome-avatar { font-size: 48px; margin-bottom: 16px; }
.chat-welcome p { font-size: 15px; color: #475569; margin: 0 0 24px; line-height: 1.6; }
.chat-welcome strong { color: #0ea5e9; }

.chat-msg { display: flex; gap: 12px; max-width: 80%; }
.chat-msg.user { flex-direction: row-reverse; align-self: flex-end; }
.msg-avatar { width: 36px; height: 36px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; background: #f8fafc; flex-shrink: 0; }
.chat-msg.user .msg-avatar { background: rgba(14,165,233,.08); }
.msg-bubble { padding: 14px 18px; border-radius: 16px; font-size: 14px; line-height: 1.7; color: #1e293b; }
.chat-msg.assistant .msg-bubble { background: #f8fafc; border: 1px solid #f1f5f9; border-top-left-radius: 4px; }
.chat-msg.user .msg-bubble { background: linear-gradient(135deg, #0ea5e9, #0284c7); color: #fff; border-top-right-radius: 4px; }

.typing-dots { display: flex; gap: 5px; padding: 4px 0; }
.typing-dots span { width: 7px; height: 7px; border-radius: 50%; background: #94a3b8; animation: blink 1.4s infinite both; }
.typing-dots span:nth-child(2) { animation-delay: .2s; }
.typing-dots span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%,80%,100% { opacity: .3; } 40% { opacity: 1; } }

.input-area { flex-shrink: 0; padding: 16px 0 20px; border-top: 1px solid #f1f5f9; }
.input-wrap { display: flex; gap: 10px; align-items: flex-end; }
.input-wrap :deep(.el-textarea__inner) { border-radius: 14px; padding: 12px 16px; font-size: 14px; border-color: #e2e8f0; }
.input-wrap :deep(.el-textarea__inner:focus) { border-color: #0ea5e9; box-shadow: 0 0 0 3px rgba(14,165,233,.1); }
.mic-btn { width: 44px; height: 44px; border-radius: 12px; border: 1.5px solid #e2e8f0; background: #fff; color: #64748b; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all .2s; }
.mic-btn:hover { border-color: #0ea5e9; color: #0ea5e9; }
.mic-btn:disabled { opacity: .5; cursor: not-allowed; }
.mic-btn.recording { border-color: #ef4444; background: rgba(239,68,68,.06); color: #ef4444; animation: pulse-mic 1.5s infinite; }
.mic-btn svg { width: 20px; height: 20px; }
@keyframes pulse-mic { 0%,100% { box-shadow: 0 0 0 0 rgba(239,68,68,.3); } 50% { box-shadow: 0 0 0 6px rgba(239,68,68,0); } }
.recording-hint { color: #ef4444; font-weight: 500; }
.send-btn { width: 44px; height: 44px; border-radius: 12px; border: none; background: #0ea5e9; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all .2s; }
.send-btn:hover { background: #0284c7; }
.send-btn:disabled { background: #cbd5e1; cursor: not-allowed; }
.send-btn svg { width: 18px; height: 18px; }
.input-hint { font-size: 12px; color: #94a3b8; margin-top: 6px; }

.finished-banner { flex-shrink: 0; text-align: center; padding: 28px; border-top: 1px solid #f1f5f9; }
.finished-banner p { font-size: 14px; color: #64748b; margin: 0 0 14px; }

@media (max-width: 768px) {
  .interview-session {
    min-height: calc(100dvh - 24px);
    padding: 0 4px;
  }

  .session-topbar,
  .topbar-right,
  .topbar-left {
    flex-wrap: wrap;
  }

  .session-topbar {
    gap: 10px;
    padding-top: 8px;
  }

  .chat-area {
    padding: 16px 0;
  }

  .chat-msg {
    max-width: 100%;
  }

  .msg-bubble {
    padding: 12px 14px;
    font-size: 13px;
  }

  .input-area {
    padding-bottom: calc(16px + env(safe-area-inset-bottom));
  }

  .input-wrap {
    display: grid;
    grid-template-columns: 1fr auto auto;
    align-items: end;
  }
}
</style>

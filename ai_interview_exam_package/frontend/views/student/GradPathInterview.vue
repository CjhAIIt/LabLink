<template>
  <div class="gradpath-page">
    <GradPathNav
      title="AI 模拟面试"
      description="通过语音和 GradPath 的面试服务进行模拟对话。这里保留了外部项目的 WebSocket 面试能力，但界面改造成更适合当前学生端的双栏工作台。"
    >
      <template #actions>
        <el-select v-model="selectedTrackId" style="width: 220px">
          <el-option
            v-for="track in gradPathTracks"
            :key="track.id"
            :label="track.name"
            :value="track.id"
          />
        </el-select>
      </template>
    </GradPathNav>

    <section class="interview-grid">
      <div class="control-column">
        <TablePageCard class="config-card" title="面试配置" subtitle="会话参数" :count-label="phaseText" :count-tag-type="phaseTagType || 'info'">

          <el-form label-position="top">
            <el-form-item label="目标岗位">
              <el-select v-model="interviewConfig.position" :disabled="isSessionActive">
                <el-option
                  v-for="option in positionOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="面试难度">
              <el-radio-group v-model="interviewConfig.difficulty" :disabled="isSessionActive">
                <el-radio-button
                  v-for="item in difficultyOptions"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <!-- "语音角色" selection removed as per user request -->
          </el-form>

          <div class="config-actions">
            <el-button type="primary" :loading="phase === 'connecting'" @click="startInterview">
              开始面试
            </el-button>
            <el-button :disabled="phase !== 'speaking'" @click="interruptInterview">打断回答</el-button>
            <el-button :disabled="!canFinish" type="danger" plain @click="finishInterview">
              结束面试
            </el-button>
          </div>

          <div class="signal-box">
            <div class="signal-row">
              <span>连接地址</span>
              <strong>{{ wsUrl || '未读取到配置' }}</strong>
            </div>
            <div class="signal-row">
              <span>实时音量</span>
              <div class="volume-bars">
                <span
                  v-for="index in 12"
                  :key="index"
                  :style="{ height: getVolumeBarHeight(index) }"
                />
              </div>
            </div>
          </div>
        </TablePageCard>

        <TablePageCard class="tips-card" title="使用建议" subtitle="语音模式" count-label="语音模式" count-tag-type="success">

          <div class="tips-list">
            <div class="tip-item">
              <strong>先保证环境可录音</strong>
              <p>首次进入会请求麦克风权限，建议使用 Chrome 或 Edge，并确认系统没有禁用录音设备。</p>
            </div>
            <div class="tip-item">
              <strong>回答不要过长</strong>
              <p>每次控制在 30-90 秒更合适，方便 AI 继续追问，也更接近真实一问一答节奏。</p>
            </div>
            <div class="tip-item">
              <strong>题目和项目都要能讲</strong>
              <p>如果只是代码过关，但解释不清楚方案取舍，真实面试里仍然容易失分。</p>
            </div>
          </div>
        </TablePageCard>
      </div>

      <TablePageCard class="dialog-card" title="面试对话" subtitle="GradPath 会话" :count-label="currentTrack.name" count-tag-type="info">

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="alert-block"
        />

        <div ref="chatContainer" class="chat-list">
          <div v-if="messages.length === 0 && !streamingText" class="empty-block">
            <h3>等待开始</h3>
            <p>点击左侧“开始面试”后，系统会建立 WebSocket 连接并进入语音对话。</p>
          </div>

          <div
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            class="message-row"
            :class="{ user: message.role === 'user' }"
          >
            <div class="avatar">{{ message.role === 'ai' ? 'AI' : 'ME' }}</div>
            <div class="bubble">
              {{ message.text }}
            </div>
          </div>

          <div v-if="streamingText" class="message-row">
            <div class="avatar">AI</div>
            <div class="bubble bubble-streaming">
              {{ streamingText }}
            </div>
          </div>
        </div>

        <div class="summary-panel">
          <div class="summary-head">
            <strong>面试总结</strong>
            <el-button text @click="clearSummary">清空</el-button>
          </div>
          <p>{{ summaryText || '结束面试后，GradPath 会返回一段总结与评价。' }}</p>
        </div>
      </TablePageCard>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import Recorder from 'recorder-core'
import 'recorder-core/src/engine/wav'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import GradPathNav from '@/components/GradPathNav.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getGradPathConfig } from '@/api/gradPath'
import { defaultTrackId, getTrackById, gradPathTracks } from '@/constants/gradPath'

const route = useRoute()
const router = useRouter()

const selectedTrackId = ref(route.query.track || defaultTrackId)
const wsUrl = ref('')
const chatContainer = ref(null)
const phase = ref('idle')
const messages = ref([])
const streamingText = ref('')
const summaryText = ref('')
const errorMessage = ref('')
const audioLevel = ref(0)

const interviewConfig = reactive({
  position: getTrackById(selectedTrackId.value).interviewPosition,
  difficulty: 'medium',
  voice: 'longxiaochun'
})

const difficultyOptions = [
  { label: '基础', value: 'easy' },
  { label: '进阶', value: 'medium' },
  { label: '高强度', value: 'hard' }
]



const currentTrack = computed(() => getTrackById(selectedTrackId.value))
const positionOptions = computed(() =>
  Array.from(new Set(gradPathTracks.map((item) => item.interviewPosition)))
)
const isSessionActive = computed(() =>
  ['connecting', 'listening', 'processing', 'speaking'].includes(phase.value)
)
const canFinish = computed(() => ['listening', 'processing', 'speaking'].includes(phase.value))
const phaseText = computed(() => {
  if (phase.value === 'connecting') return '连接中'
  if (phase.value === 'listening') return '正在聆听'
  if (phase.value === 'processing') return '正在思考'
  if (phase.value === 'speaking') return 'AI 回答中'
  if (phase.value === 'finished') return '已结束'
  return '待开始'
})
const phaseTagType = computed(() => {
  if (phase.value === 'listening') return 'success'
  if (phase.value === 'processing' || phase.value === 'connecting') return 'warning'
  if (phase.value === 'speaking') return 'primary'
  if (phase.value === 'finished') return 'info'
  return ''
})

let ws = null
let recorder = null
let vadTimer = null
let isStoppingRecorder = false
let turnIndex = 0
let audioQueue = []
let audioPlaying = false
let currentAudio = null

watch(
  () => route.query.track,
  (value) => {
    selectedTrackId.value = value || defaultTrackId
  }
)

watch(selectedTrackId, (value) => {
  if (value !== route.query.track) {
    router.replace(`/student/guide/interview?track=${value}`)
  }

  if (!isSessionActive.value) {
    interviewConfig.position = getTrackById(value).interviewPosition
  }
})

const ensureConfig = async () => {
  if (wsUrl.value) {
    return
  }

  try {
    const response = await getGradPathConfig()
    wsUrl.value = response.data.wsUrl || 'ws://localhost:8080/ws/interview'
    console.log('Using WebSocket URL:', wsUrl.value)
  } catch (error) {
    wsUrl.value = 'ws://localhost:8080/ws/interview'
    console.error('Failed to get GradPath config, using default WS URL:', wsUrl.value, error)
  }
}

const startInterview = async () => {
  if (isSessionActive.value) {
    return
  }

  await ensureConfig()
  cleanupSession()
  messages.value = []
  streamingText.value = ''
  summaryText.value = ''
  errorMessage.value = ''
  phase.value = 'connecting'
  turnIndex = 0

  try {
    console.log('Attempting to connect to WebSocket:', wsUrl.value)
    ws = new WebSocket(wsUrl.value)
    ws.onopen = () => {
      console.log('WebSocket connection opened.')
      sendMessage('interview.start', {
        position: interviewConfig.position,
        difficulty: interviewConfig.difficulty,
        voice: interviewConfig.voice,
        language: 'zh'
      })
    }
    ws.onmessage = async (event) => {
      const payload = JSON.parse(event.data)
      await handleSocketMessage(payload)
    }
    ws.onerror = (event) => {
      console.error('WebSocket error:', event)
      errorMessage.value = 'WebSocket 连接失败，请确认 GradPath 服务已经启动。'
      phase.value = 'idle'
    }
    ws.onclose = (event) => {
      console.log('WebSocket connection closed:', event)
      if (phase.value !== 'finished') {
        phase.value = 'idle'
      }
    }
  } catch (error) {
    console.error('Error establishing interview connection:', error)
    phase.value = 'idle'
    errorMessage.value = '无法建立面试连接。'
  }
}

const sendMessage = (type, data = {}) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(
      JSON.stringify({
        type,
        data,
        timestamp: Date.now()
      })
    )
  }
}

const handleSocketMessage = async (payload) => {
  const { type, data = {} } = payload

  if (type === 'interview.ready') {
    phase.value = 'listening'
    if (data.greeting) {
      messages.value.push({ role: 'ai', text: data.greeting })
      scrollToBottom()
    }
    await startRecording()
    return
  }

  if (type === 'stt.result') {
    if (data.text) {
      messages.value.push({ role: 'user', text: data.text })
      scrollToBottom()
    }
    phase.value = 'processing'
    return
  }

  if (type === 'ai.text') {
    phase.value = 'speaking'
    if (data.seq === 0 && !data.finished) {
      streamingText.value = ''
    }
    streamingText.value += data.delta || ''

    if (data.finished) {
      messages.value.push({ role: 'ai', text: streamingText.value })
      streamingText.value = ''
      scrollToBottom()
    }
    return
  }

  if (type === 'ai.audio') {
    enqueueAudio(data.audio, data.format)
    return
  }

  if (type === 'turn.complete') {
    phase.value = 'listening'
    await startRecording()
    return
  }

  if (type === 'interview.summary') {
    await stopRecording(false)
    phase.value = 'finished'
    summaryText.value = data.evaluation || '本次面试未返回总结内容。'
    return
  }

  if (type === 'interrupt.ack') {
    stopAudioPlayback()
    phase.value = 'listening'
    await startRecording()
    return
  }

  if (type === 'error') {
    errorMessage.value = data.message || '面试过程中出现异常。'
    phase.value = 'idle'
    await stopRecording(false)
  }
}

const startRecording = async () => {
  await stopRecording(false)

  try {
    recorder = Recorder({
      type: 'wav',
      sampleRate: 16000,
      bitRate: 16,
      onProcess: (_, powerLevel) => {
        audioLevel.value = Math.min(100, Math.round(powerLevel * 1.6))
      }
    })

    await recorder.open()
    recorder.start()
    startVadDetection()
  } catch (error) {
    phase.value = 'idle'
    errorMessage.value = '无法访问麦克风，请检查浏览器权限和录音设备。'
  }
}

const startVadDetection = () => {
  clearVadTimer()
  let silenceSince = null

  vadTimer = window.setInterval(async () => {
    if (phase.value !== 'listening') {
      silenceSince = null
      return
    }

    if (audioLevel.value < 4) {
      if (!silenceSince) {
        silenceSince = Date.now()
      }

      if (Date.now() - silenceSince > 1400) {
        await stopRecording(true)
      }
    } else {
      silenceSince = null
    }
  }, 180)
}

const stopRecording = async (sendAudio) => {
  clearVadTimer()

  if (!recorder || isStoppingRecorder) {
    return
  }

  isStoppingRecorder = true

  await new Promise((resolve) => {
    recorder.stop(
      async (blob) => {
        if (sendAudio) {
          turnIndex += 1
          const audio = await blobToBase64(blob)
          sendMessage('audio.data', {
            audio,
            format: 'wav',
            sampleRate: 16000,
            turnId: getTurnId(turnIndex)
          })
          phase.value = 'processing'
        }

        recorder.close()
        recorder = null
        audioLevel.value = 0
        isStoppingRecorder = false
        resolve()
      },
      () => {
        if (recorder) {
          recorder.close()
          recorder = null
        }
        audioLevel.value = 0
        isStoppingRecorder = false
        resolve()
      }
    )
  })
}

const blobToBase64 = (blob) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      const value = String(reader.result || '')
      resolve(value.split(',')[1] || '')
    }
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })

const enqueueAudio = (audio, format) => {
  if (!audio) return
  audioQueue.push({
    audio,
    mimeType: format && format.includes('mp3') ? 'audio/mpeg' : 'audio/mpeg'
  })

  if (!audioPlaying) {
    playNextAudio()
  }
}

const playNextAudio = () => {
  if (audioQueue.length === 0) {
    audioPlaying = false
    currentAudio = null
    return
  }

  audioPlaying = true
  const next = audioQueue.shift()
  currentAudio = new Audio(`data:${next.mimeType};base64,${next.audio}`)
  currentAudio.onended = () => {
    playNextAudio()
  }
  currentAudio.onerror = () => {
    playNextAudio()
  }
  currentAudio.play().catch(() => {
    playNextAudio()
  })
}

const interruptInterview = async () => {
  if (phase.value !== 'speaking') {
    return
  }

  stopAudioPlayback()
  streamingText.value = ''
  sendMessage('interrupt', {
    turnId: getTurnId(turnIndex)
  })
  phase.value = 'listening'
  await startRecording()
}

const finishInterview = async () => {
  if (!canFinish.value) {
    return
  }

  await stopRecording(false)
  sendMessage('interview.end')
  phase.value = 'processing'
}

const cleanupSession = () => {
  clearVadTimer()
  stopAudioPlayback()

  if (recorder) {
    recorder.close()
    recorder = null
  }

  if (ws) {
    ws.onclose = null
    ws.onerror = null
    ws.onmessage = null
    ws.close()
    ws = null
  }

  audioLevel.value = 0
}

const stopAudioPlayback = () => {
  audioQueue = []
  audioPlaying = false
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
}

const clearVadTimer = () => {
  if (vadTimer) {
    window.clearInterval(vadTimer)
    vadTimer = null
  }
}

const getTurnId = (index) => `turn_${String(index).padStart(3, '0')}`

const clearSummary = () => {
  summaryText.value = ''
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

const getVolumeBarHeight = (index) => {
  const base = Math.max(8, audioLevel.value * 0.45 - index * 1.8)
  return `${Math.min(40, base)}px`
}

onBeforeUnmount(() => {
  cleanupSession()
})

onMounted(() => {
  startInterview()
})

ensureConfig().catch(() => {
  ElMessage.warning('GradPath 面试地址读取失败，将使用默认地址。')
})
</script>

<style scoped>
.gradpath-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1360px;
  margin: 0 auto;
}

.interview-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.78fr) minmax(0, 1.22fr);
  gap: 18px;
}

.control-column {
  display: grid;
  gap: 18px;
  align-content: start;
}

.config-card,
.tips-card,
.dialog-card {
  border: 1px solid rgba(14, 165, 233, 0.14);
}

.config-card {
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.1), transparent 24%),
    linear-gradient(180deg, #ffffff, #f8fbff);
}

.config-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.signal-box {
  margin-top: 20px;
  display: grid;
  gap: 16px;
}

.signal-row {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.signal-row span {
  display: block;
  font-size: 13px;
  color: #64748b;
}

.signal-row strong {
  display: block;
  margin-top: 8px;
  word-break: break-all;
  color: #0f172a;
}

.volume-bars {
  margin-top: 12px;
  height: 42px;
  display: flex;
  align-items: end;
  gap: 5px;
}

.volume-bars span {
  width: 8px;
  border-radius: 999px;
  background: linear-gradient(180deg, #0ea5e9, #14b8a6);
  transition: height 0.12s ease;
}

.tips-list {
  display: grid;
  gap: 14px;
}

.tip-item {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.tip-item strong {
  color: #0f172a;
}

.tip-item p {
  margin: 8px 0 0;
  line-height: 1.8;
  color: #475569;
}

.alert-block {
  margin-bottom: 16px;
}

.dialog-card {
  display: flex;
  flex-direction: column;
}

.dialog-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 760px;
}

.chat-list {
  flex: 1;
  max-height: 600px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 6px;
}

.empty-block {
  min-height: 300px;
  display: grid;
  place-items: center;
  text-align: center;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fafc, #f0f9ff);
  color: #475569;
}

.empty-block h3 {
  margin: 0;
  color: #0f172a;
}

.empty-block p {
  margin: 8px 0 0;
  max-width: 360px;
  line-height: 1.8;
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #0ea5e9, #14b8a6);
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.message-row.user .avatar {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
}

.bubble {
  max-width: 76%;
  padding: 16px 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: #334155;
  line-height: 1.85;
  white-space: pre-wrap;
}

.bubble-streaming {
  border-color: rgba(14, 165, 233, 0.22);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.08);
}

.summary-panel {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fffdf7, #fffbeb);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.summary-head strong {
  color: #7c2d12;
}

.summary-panel p {
  margin: 10px 0 0;
  color: #92400e;
  line-height: 1.9;
  white-space: pre-wrap;
}

@media (max-width: 1080px) {
  .interview-grid {
    grid-template-columns: 1fr;
  }

  .dialog-card :deep(.el-card__body) {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .bubble {
    max-width: 100%;
  }
}
</style>

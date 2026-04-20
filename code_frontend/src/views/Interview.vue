<script setup>
import { ref, reactive, onUnmounted, nextTick, computed } from 'vue'
import Recorder from 'recorder-core'
import 'recorder-core/src/engine/wav'

// ==================== 状态 ====================
const state = reactive({
  status: 'idle', // idle | config | connecting | listening | processing | speaking
  connected: false,
  sessionId: '',
  messages: [],
  currentAiText: '',
  error: ''
})

// 面试配置
const config = reactive({
  position: 'Java后端开发',
  difficulty: 'medium',
  voice: 'longxiaochun'
})

const positions = ['Java后端开发', '前端开发', 'Python开发', '算法工程师', '产品经理']
const difficulties = [
  { value: 'easy', label: '入门', desc: '基础概念为主' },
  { value: 'medium', label: '中级', desc: '项目经验+原理' },
  { value: 'hard', label: '资深', desc: '架构设计+深度追问' }
]
const voices = [
  { value: 'longxiaochun', label: '沉稳男声' },
  { value: 'zhixiaobai', label: '温柔女声' },
  { value: 'zhiyan', label: '知性女声' }
]

const chatContainer = ref(null)
const audioLevel = ref(0)
let ws = null
let audioContext = null
let audioQueue = []
let isPlaying = false
let currentTurnId = 0
let vadTimer = null
let recorder = null  // Recorder 实例
let analyser = null
let levelTimer = null

// ==================== Computed ====================
const statusText = computed(() => ({
  idle: '', config: '配置面试', connecting: '正在连接...',
  listening: '正在聆听...', processing: 'AI 思考中...', speaking: 'Kairo 正在回答'
})[state.status] || '')

const isInRoom = computed(() => ['listening', 'processing', 'speaking'].includes(state.status))

// ==================== WebSocket ====================
function startInterview() {
  state.status = 'connecting'
  ws = new WebSocket('ws://localhost:8080/ws/interview')
  ws.onopen = () => {
    state.connected = true
    send('interview.start', {
      position: config.position,
      difficulty: config.difficulty,
      language: 'zh'
    })
  }
  ws.onmessage = (e) => handleMessage(JSON.parse(e.data))
  ws.onclose = () => { state.connected = false; state.status = 'idle' }
  ws.onerror = () => { state.error = '连接失败，请检查后端服务'; state.status = 'config' }
}

function send(type, data = {}) {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, data, timestamp: Date.now() }))
  }
}

// ==================== 消息处理 ====================
function handleMessage(msg) {
  const { type, data } = msg
  switch (type) {
    case 'interview.ready':
      state.sessionId = data.sessionId
      state.status = 'listening'
      state.messages.push({ role: 'ai', text: data.greeting })
      startRecording()
      break
    case 'stt.result':
      state.messages.push({ role: 'user', text: data.text })
      state.status = 'processing'
      break
    case 'ai.text':
      state.status = 'speaking'
      if (data.seq === 0 && !data.finished) state.currentAiText = ''
      state.currentAiText += data.delta
      if (data.finished) {
        state.messages.push({ role: 'ai', text: state.currentAiText })
        state.currentAiText = ''
      }
      scrollToBottom()
      break
    case 'ai.audio':
      enqueueAudio(data.audio, data.format)
      break
    case 'turn.complete':
      state.status = 'listening'
      startRecording()
      break
    case 'interrupt.ack':
      audioQueue = []; isPlaying = false
      break
    case 'interview.summary':
      state.status = 'idle'
      state.messages.push({ role: 'ai', text: '【面试总结】\n' + data.evaluation })
      break
    case 'error':
      state.error = data.message
      state.status = 'listening'
      break
  }
}

// ==================== 录音 (Recorder-core with 16kHz WAV) ====================
async function startRecording() {
  try {
    // 创建 Recorder 实例，严格配置为阿里云 Paraformer 要求的格式
    recorder = Recorder({
      type: 'wav',           // WAV 格式
      sampleRate: 16000,     // 16kHz 采样率
      bitRate: 16,           // 16-bit 位深度
      onProcess: (buffers, powerLevel, bufferDuration, bufferSampleRate) => {
        // 实时音量显示
        audioLevel.value = Math.min(100, powerLevel * 1.5)
      }
    })

    // 打开录音（请求麦克风权限）
    await recorder.open()

    // 开始录音
    recorder.start()

    // 启动 VAD 检测
    startVAD()

  } catch (err) {
    console.error('录音初始化失败:', err)
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      state.error = '麦克风权限被拒绝，请在浏览器设置中允许访问麦克风'
    } else if (err.name === 'NotFoundError') {
      state.error = '未检测到麦克风设备'
    } else {
      state.error = '无法访问麦克风: ' + (err.message || '未知错误')
    }
    state.status = 'config'
  }
}

function startVAD() {
  if (!recorder) return

  let silenceStart = null
  const THRESHOLD = 3        // 音量阈值（recorder-core 的 powerLevel 范围 0-100）
  const DURATION = 1500      // 静音持续时间（毫秒）

  function check() {
    if (state.status !== 'listening' || !recorder) return

    // 获取当前音量（通过 onProcess 回调更新的 audioLevel）
    const currentLevel = audioLevel.value

    if (currentLevel < THRESHOLD) {
      if (!silenceStart) {
        silenceStart = Date.now()
      } else if (Date.now() - silenceStart > DURATION) {
        stopRecording()
        return
      }
    } else {
      silenceStart = null
    }

    vadTimer = requestAnimationFrame(check)
  }

  check()
}

async function stopRecording() {
  if (vadTimer) {
    cancelAnimationFrame(vadTimer)
    vadTimer = null
  }

  if (!recorder) return

  try {
    // 停止录音
    recorder.stop(
      async (blob, duration) => {
        // 成功回调：将 WAV Blob 转换为 Base64 并发送
        const base64 = await blobToBase64(blob)

        // 关闭录音器
        recorder.close()
        recorder = null
        audioLevel.value = 0

        // 发送音频数据
        currentTurnId++
        send('audio.data', {
          audio: base64,
          format: 'wav',
          sampleRate: 16000,
          turnId: 'turn_' + String(currentTurnId).padStart(3, '0')
        })
        state.status = 'processing'
      },
      (err) => {
        // 错误回调
        console.error('录音停止失败:', err)
        state.error = '录音处理失败'
        recorder.close()
        recorder = null
        audioLevel.value = 0
      }
    )
  } catch (err) {
    console.error('停止录音异常:', err)
    state.error = '录音停止异常'
    if (recorder) {
      recorder.close()
      recorder = null
    }
    audioLevel.value = 0
  }
}

// Blob 转 Base64 工具函数
function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      // reader.result 格式: "data:audio/wav;base64,xxxxx"
      const base64 = reader.result.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

// ==================== 音频播放 ====================
function enqueueAudio(audioBase64, format) {
  audioQueue.push({ audioBase64, format })
  if (!isPlaying) playNext()
}
async function playNext() {
  if (audioQueue.length === 0) { isPlaying = false; return }
  isPlaying = true
  const { audioBase64 } = audioQueue.shift()
  try {
    const bytes = Uint8Array.from(atob(audioBase64), c => c.charCodeAt(0))
    const ctx = new AudioContext()
    const buffer = await ctx.decodeAudioData(bytes.buffer)
    const src = ctx.createBufferSource()
    src.buffer = buffer; src.connect(ctx.destination)
    src.onended = () => playNext()
    src.start()
  } catch { playNext() }
}

// ==================== 打断 & 结束 ====================
function handleInterrupt() {
  if (state.status !== 'speaking') return
  send('interrupt', { turnId: 'turn_' + String(currentTurnId).padStart(3, '0') })
  audioQueue = []; isPlaying = false; state.currentAiText = ''
  state.status = 'listening'; startRecording()
}
function endInterview() {
  stopRecording(); send('interview.end'); state.status = 'processing'
}

// ==================== 工具 ====================
function scrollToBottom() {
  nextTick(() => { if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight })
}
function enterConfig() { state.status = 'config' }

onUnmounted(() => {
  if (recorder) {
    recorder.close()
    recorder = null
  }
  if (vadTimer) cancelAnimationFrame(vadTimer)
  if (ws) ws.close()
  if (audioContext) audioContext.close()
})

</script>

<template>
  <div class="min-h-screen bg-black text-white overflow-hidden relative">
    <!-- 背景渐变光效 -->
    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-[-20%] left-[-10%] w-[600px] h-[600px] rounded-full bg-neon/5 blur-[120px]"></div>
      <div class="absolute bottom-[-20%] right-[-10%] w-[500px] h-[500px] rounded-full bg-aurora/5 blur-[120px]"></div>
    </div>

    <!-- ========== 首页 / 空闲态 ========== -->
    <div v-if="state.status === 'idle'" class="relative z-10 flex flex-col items-center justify-center min-h-screen px-6">
      <div class="w-24 h-24 rounded-full bg-gradient-to-br from-neon/20 to-aurora/20 border border-neon/30 flex items-center justify-center mb-8 animate-glow">
        <span class="text-4xl font-bold text-neon">K</span>
      </div>
      <h1 class="text-3xl font-bold mb-3 bg-gradient-to-r from-neon to-aurora bg-clip-text text-transparent">
        AI 模拟面试
      </h1>
      <p class="text-white/40 text-sm mb-10">与 Kairo 进行一场沉浸式技术面试</p>
      <button @click="enterConfig"
        class="px-10 py-3.5 rounded-full bg-neon/10 border border-neon/40 text-neon font-semibold text-sm
               hover:bg-neon/20 hover:border-neon/60 hover:shadow-[0_0_30px_rgba(16,185,129,0.2)]
               transition-all duration-300">
        开始面试
      </button>

      <!-- 历史消息回顾 -->
      <div v-if="state.messages.length" class="mt-12 w-full max-w-lg">
        <p class="text-white/30 text-xs mb-3 text-center">上次面试记录</p>
        <div class="max-h-60 overflow-y-auto space-y-2 px-2">
          <div v-for="(msg, i) in state.messages" :key="i"
               class="text-xs px-3 py-2 rounded-lg"
               :class="msg.role === 'ai' ? 'bg-white/5 text-white/60' : 'bg-neon/5 text-neon/60 ml-8'">
            {{ msg.text }}
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 配置页 ========== -->
    <div v-if="state.status === 'config'" class="relative z-10 flex flex-col items-center justify-center min-h-screen px-6">
      <h2 class="text-xl font-bold mb-8 text-white/90">面试配置</h2>

      <!-- 岗位选择 -->
      <div class="w-full max-w-md mb-6">
        <label class="text-xs text-white/40 mb-2 block">面试岗位</label>
        <div class="flex flex-wrap gap-2">
          <button v-for="p in positions" :key="p" @click="config.position = p"
            class="px-4 py-2 rounded-lg text-sm transition-all duration-300 border"
            :class="config.position === p
              ? 'bg-neon/10 border-neon/50 text-neon shadow-[0_0_15px_rgba(16,185,129,0.15)]'
              : 'bg-white/5 border-white/10 text-white/50 hover:border-white/20'">
            {{ p }}
          </button>
        </div>
      </div>

      <!-- 难度选择 -->
      <div class="w-full max-w-md mb-6">
        <label class="text-xs text-white/40 mb-2 block">面试难度</label>
        <div class="grid grid-cols-3 gap-3">
          <button v-for="d in difficulties" :key="d.value" @click="config.difficulty = d.value"
            class="p-4 rounded-xl text-center transition-all duration-300 border"
            :class="config.difficulty === d.value
              ? 'bg-neon/10 border-neon/50 shadow-[0_0_20px_rgba(16,185,129,0.15)]'
              : 'bg-white/5 border-white/10 hover:border-white/20'">
            <div class="text-sm font-semibold" :class="config.difficulty === d.value ? 'text-neon' : 'text-white/70'">{{ d.label }}</div>
            <div class="text-xs mt-1" :class="config.difficulty === d.value ? 'text-neon/60' : 'text-white/30'">{{ d.desc }}</div>
          </button>
        </div>
      </div>

      <!-- 声音选择 -->
      <div class="w-full max-w-md mb-10">
        <label class="text-xs text-white/40 mb-2 block">面试官声音</label>
        <div class="flex gap-3">
          <button v-for="v in voices" :key="v.value" @click="config.voice = v.value"
            class="flex-1 py-3 rounded-xl text-sm transition-all duration-300 border"
            :class="config.voice === v.value
              ? 'bg-aurora/10 border-aurora/50 text-aurora shadow-[0_0_15px_rgba(56,189,248,0.15)]'
              : 'bg-white/5 border-white/10 text-white/50 hover:border-white/20'">
            {{ v.label }}
          </button>
        </div>
      </div>

      <div class="flex gap-4">
        <button @click="state.status = 'idle'"
          class="px-8 py-3 rounded-full border border-white/20 text-white/50 text-sm hover:border-white/40 transition-all">
          返回
        </button>
        <button @click="startInterview"
          class="px-10 py-3 rounded-full bg-gradient-to-r from-neon to-emerald-400 text-black font-semibold text-sm
                 hover:shadow-[0_0_30px_rgba(16,185,129,0.4)] transition-all duration-300">
          进入面试间
        </button>
      </div>
    </div>

    <!-- ========== 连接中 ========== -->
    <div v-if="state.status === 'connecting'" class="relative z-10 flex flex-col items-center justify-center min-h-screen">
      <div class="w-16 h-16 rounded-full border-2 border-neon/30 border-t-neon animate-spin-slow mb-6"></div>
      <p class="text-white/50 text-sm">正在连接 Kairo...</p>
    </div>

    <!-- ========== 面试间 ========== -->
    <div v-if="isInRoom" class="relative z-10 flex flex-col h-screen">
      <!-- 顶栏 -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-white/5">
        <div class="flex items-center gap-3">
          <div class="w-2 h-2 rounded-full"
               :class="{ 'bg-neon animate-pulse': state.status === 'listening',
                          'bg-aurora animate-pulse': state.status === 'speaking',
                          'bg-amber-400': state.status === 'processing' }"></div>
          <span class="text-sm text-white/60">{{ statusText }}</span>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-white/30">Kairo · {{ config.position }}</span>
          <button @click="endInterview"
            class="ml-4 px-4 py-1.5 rounded-lg text-xs border border-red-500/30 text-red-400
                   hover:bg-red-500/10 transition-all">
            结束面试
          </button>
        </div>
      </div>

      <!-- 主体区域 -->
      <div class="flex-1 flex flex-col items-center overflow-hidden">
        <!-- AI 形象 -->
        <div class="flex-shrink-0 mt-8 mb-4 relative">
          <div class="w-28 h-28 rounded-full bg-gradient-to-br from-dark-700 to-dark-600
                      border border-white/10 flex items-center justify-center relative">
            <!-- 呼吸灯 -->
            <div class="absolute inset-0 rounded-full transition-all duration-1000"
                 :class="{ 'shadow-[0_0_40px_rgba(16,185,129,0.3)] animate-pulse-slow': state.status === 'speaking',
                            'shadow-[0_0_20px_rgba(56,189,248,0.2)]': state.status === 'listening' }"></div>
            <span class="text-5xl font-bold bg-gradient-to-br from-neon to-aurora bg-clip-text text-transparent relative z-10">K</span>
          </div>
          <!-- 说话时的频谱环 -->
          <svg v-if="state.status === 'speaking'" class="absolute inset-0 w-28 h-28" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="46" fill="none" stroke="rgba(16,185,129,0.2)" stroke-width="1"
                    stroke-dasharray="4 6" class="animate-spin-slow"/>
          </svg>
        </div>

        <!-- 对话区域 -->
        <div ref="chatContainer"
             class="flex-1 w-full max-w-2xl overflow-y-auto px-6 py-4 space-y-4 scrollbar-hide">
          <div v-for="(msg, i) in state.messages" :key="i"
               class="flex gap-3" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
            <!-- 头像 -->
            <div class="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-xs font-bold"
                 :class="msg.role === 'ai'
                   ? 'bg-gradient-to-br from-neon/20 to-aurora/20 text-neon border border-neon/20'
                   : 'bg-white/10 text-white/60 border border-white/10'">
              {{ msg.role === 'ai' ? 'K' : 'U' }}
            </div>
            <!-- 气泡 -->
            <div class="max-w-[75%] px-4 py-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap"
                 :class="msg.role === 'ai'
                   ? 'bg-white/5 border border-white/5 text-white/80 backdrop-blur-sm rounded-tl-sm'
                   : 'bg-neon/10 border border-neon/10 text-neon/90 rounded-tr-sm'">
              {{ msg.text }}
            </div>
          </div>

          <!-- 打字机效果 -->
          <div v-if="state.currentAiText" class="flex gap-3">
            <div class="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-xs font-bold
                        bg-gradient-to-br from-neon/20 to-aurora/20 text-neon border border-neon/20">K</div>
            <div class="max-w-[75%] px-4 py-3 rounded-2xl rounded-tl-sm text-sm leading-relaxed
                        bg-white/5 border border-white/5 text-white/80 backdrop-blur-sm whitespace-pre-wrap">
              {{ state.currentAiText }}<span class="inline-block w-0.5 h-4 bg-neon ml-0.5 animate-pulse"></span>
            </div>
          </div>

          <!-- 思考中 -->
          <div v-if="state.status === 'processing'" class="flex gap-3">
            <div class="w-8 h-8 rounded-full flex-shrink-0 flex items-center justify-center text-xs font-bold
                        bg-gradient-to-br from-neon/20 to-aurora/20 text-neon border border-neon/20">K</div>
            <div class="px-4 py-3 rounded-2xl rounded-tl-sm bg-white/5 border border-white/5 backdrop-blur-sm">
              <div class="flex gap-1.5">
                <span class="w-2 h-2 rounded-full bg-neon/40 animate-bounce" style="animation-delay:0ms"></span>
                <span class="w-2 h-2 rounded-full bg-neon/40 animate-bounce" style="animation-delay:150ms"></span>
                <span class="w-2 h-2 rounded-full bg-neon/40 animate-bounce" style="animation-delay:300ms"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部控制栏 -->
      <div class="flex-shrink-0 pb-8 pt-4 flex flex-col items-center gap-4 border-t border-white/5 bg-black/50 backdrop-blur-md">
        <!-- 打断按钮 -->
        <button v-if="state.status === 'speaking'" @click="handleInterrupt"
          class="px-6 py-2 rounded-full text-xs border border-amber-400/30 text-amber-400
                 hover:bg-amber-400/10 transition-all mb-2">
          打断回答
        </button>

        <!-- 麦克风按钮 -->
        <div class="relative">
          <!-- 声波扩散环 -->
          <template v-if="state.status === 'listening'">
            <div class="absolute inset-0 rounded-full border border-neon/30 animate-ripple"></div>
            <div class="absolute inset-0 rounded-full border border-neon/20 animate-ripple" style="animation-delay:0.5s"></div>
            <div class="absolute inset-0 rounded-full border border-neon/10 animate-ripple" style="animation-delay:1s"></div>
          </template>

          <div class="w-16 h-16 rounded-full flex items-center justify-center relative z-10 transition-all duration-300"
               :class="{
                 'bg-neon/20 border-2 border-neon/50 shadow-[0_0_30px_rgba(16,185,129,0.3)]': state.status === 'listening',
                 'bg-white/5 border-2 border-white/10': state.status !== 'listening',
                 'animate-spin-slow': state.status === 'processing'
               }">
            <!-- 麦克风图标 -->
            <svg v-if="state.status !== 'processing'" class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"
                 :class="state.status === 'listening' ? 'text-neon' : 'text-white/30'">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                    d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                    d="M19 10v2a7 7 0 0 1-14 0v-2M12 19v4M8 23h8"/>
            </svg>
            <!-- loading图标 -->
            <svg v-else class="w-6 h-6 text-amber-400" fill="none" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" opacity="0.2"/>
              <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </div>

        <!-- 音量指示条 -->
        <div v-if="state.status === 'listening'" class="flex items-center gap-1 h-4">
          <div v-for="n in 12" :key="n"
               class="w-1 rounded-full bg-neon/60 transition-all duration-75"
               :style="{ height: Math.max(4, (audioLevel * Math.sin(n * 0.5 + Date.now() * 0.003)) * 0.3) + 'px' }">
          </div>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <Transition name="slide">
      <div v-if="state.error"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-xl
               bg-red-500/10 border border-red-500/30 backdrop-blur-md text-red-400 text-sm flex items-center gap-3">
        {{ state.error }}
        <button @click="state.error = ''" class="text-red-400/60 hover:text-red-400 ml-2">✕</button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* 隐藏滚动条 */
.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }

/* 错误提示滑入动画 */
.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translate(-50%, -20px); }
</style>

<template>
  <div class="checkin-page">
    <!-- 未加入实验室 -->
    <div v-if="!hasLab" class="checkin-empty">
      <div class="checkin-empty__icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
          <path d="M3 21h18M9 8h1M9 12h1M9 16h1M14 8h1M14 12h1M14 16h1M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/>
        </svg>
      </div>
      <h2>当前未加入实验室</h2>
      <p>加入实验室后即可使用拍照打卡功能</p>
      <el-button type="primary" @click="$router.push('/student/labs')">浏览实验室</el-button>
    </div>

    <!-- 已加入实验室 -->
    <template v-else>
      <header class="checkin-header">
        <div>
          <h1 class="checkin-header__title">拍照打卡</h1>
          <p class="checkin-header__sub">{{ todayStr }} · {{ labName }}</p>
        </div>
        <el-tag v-if="todayCheckedIn" type="success" size="large" effect="dark" round>今日已打卡</el-tag>
      </header>

      <!-- 打卡操作区 -->
      <section class="checkin-card">
        <div class="checkin-card__camera">
          <!-- 预览区 -->
          <div class="camera-box">
            <video v-show="cameraActive && !capturedPhoto" ref="videoRef" autoplay playsinline muted class="camera-video"></video>
            <img v-if="capturedPhoto" :src="capturedPhoto" class="camera-preview" alt="拍照预览" />
            <div v-if="!cameraActive && !capturedPhoto" class="camera-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
              <span>点击下方按钮开启摄像头</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="camera-actions">
            <template v-if="!cameraActive && !capturedPhoto">
              <el-button type="primary" size="large" round @click="startCamera">
                <el-icon class="el-icon--left"><VideoCamera /></el-icon>开启摄像头
              </el-button>
              <el-button size="large" round @click="triggerFileInput">
                <el-icon class="el-icon--left"><PictureFilled /></el-icon>从相册选择
              </el-button>
              <input ref="fileInputRef" type="file" accept="image/*" capture="environment" hidden @change="handleFileSelect" />
            </template>
            <template v-else-if="cameraActive && !capturedPhoto">
              <el-button type="primary" size="large" round @click="takePhoto">
                <el-icon class="el-icon--left"><Camera /></el-icon>拍照
              </el-button>
              <el-button size="large" round @click="stopCamera">取消</el-button>
            </template>
            <template v-else-if="capturedPhoto">
              <el-button type="success" size="large" round :loading="submitting" @click="submitCheckIn">
                <el-icon class="el-icon--left"><CircleCheck /></el-icon>确认打卡
              </el-button>
              <el-button size="large" round @click="retake">重拍</el-button>
            </template>
          </div>
        </div>

        <!-- 位置信息 -->
        <div v-if="locationInfo" class="checkin-card__location">
          <el-icon><Location /></el-icon>
          <span>{{ locationInfo }}</span>
        </div>
      </section>

      <!-- 打卡记录 -->
      <section class="checkin-records">
        <div class="checkin-records__head">
          <h3>打卡记录</h3>
          <el-date-picker v-model="recordMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadRecords" />
        </div>
        <div v-if="records.length" class="record-grid">
          <article v-for="item in records" :key="item.id" class="record-item">
            <div class="record-item__date">
              <strong>{{ formatDay(item.checkInTime) }}</strong>
              <span>{{ formatWeekday(item.checkInTime) }}</span>
            </div>
            <div class="record-item__info">
              <p>{{ formatTime(item.checkInTime) }}</p>
              <span v-if="item.location" class="record-item__loc">{{ item.location }}</span>
            </div>
            <img v-if="item.photoUrl" :src="item.photoUrl" class="record-item__thumb" alt="打卡照片" />
            <el-tag :type="item.status === 'normal' ? 'success' : 'warning'" size="small">
              {{ item.status === 'normal' ? '正常' : '补卡' }}
            </el-tag>
          </article>
        </div>
        <el-empty v-else description="本月暂无打卡记录" :image-size="80" />
      </section>
    </template>

    <!-- 隐藏 canvas -->
    <canvas ref="canvasRef" hidden></canvas>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Camera, CircleCheck, Location, PictureFilled, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCheckInRecords, photoCheckIn } from '@/api/attendance'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const hasLab = computed(() => Boolean(userStore.userInfo?.labId))
const labName = computed(() => userStore.userInfo?.labName || '实验室')
const todayStr = dayjs().format('YYYY年MM月DD日 dddd')

// camera
const videoRef = ref(null)
const canvasRef = ref(null)
const fileInputRef = ref(null)
const cameraActive = ref(false)
const capturedPhoto = ref(null)
const capturedBlob = ref(null)
const submitting = ref(false)
const todayCheckedIn = ref(false)
const locationInfo = ref('')
let mediaStream = null

// records
const recordMonth = ref(dayjs().format('YYYY-MM'))
const records = ref([])

const startCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }
    })
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
    }
    cameraActive.value = true
  } catch (e) {
    ElMessage.error('无法访问摄像头，请检查权限设置')
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(t => t.stop())
    mediaStream = null
  }
  cameraActive.value = false
}

const takePhoto = () => {
  if (!videoRef.value || !canvasRef.value) return
  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0)
  canvas.toBlob((blob) => {
    capturedBlob.value = blob
    capturedPhoto.value = URL.createObjectURL(blob)
    stopCamera()
  }, 'image/jpeg', 0.85)
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  capturedBlob.value = file
  capturedPhoto.value = URL.createObjectURL(file)
  e.target.value = ''
}

const retake = () => {
  if (capturedPhoto.value) URL.revokeObjectURL(capturedPhoto.value)
  capturedPhoto.value = null
  capturedBlob.value = null
}

const submitCheckIn = async () => {
  if (!capturedBlob.value) { ElMessage.warning('请先拍照'); return }
  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('photo', capturedBlob.value, `checkin-${Date.now()}.jpg`)
    formData.append('labId', userStore.userInfo?.labId)
    if (locationInfo.value) formData.append('location', locationInfo.value)
    await photoCheckIn(formData)
    ElMessage.success('打卡成功')
    todayCheckedIn.value = true
    retake()
    await loadRecords()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '打卡失败，请重试')
  } finally {
    submitting.value = false
  }
}

const loadRecords = async () => {
  if (!hasLab.value) return
  try {
    const res = await getCheckInRecords({ month: recordMonth.value })
    records.value = res.data?.records || res.data || []
    const today = dayjs().format('YYYY-MM-DD')
    todayCheckedIn.value = records.value.some(r => dayjs(r.checkInTime).format('YYYY-MM-DD') === today)
  } catch { records.value = [] }
}

const getLocation = () => {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => { locationInfo.value = `${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)}` },
    () => { locationInfo.value = '' },
    { timeout: 5000 }
  )
}

const formatDay = (t) => dayjs(t).format('MM/DD')
const formatWeekday = (t) => dayjs(t).format('ddd')
const formatTime = (t) => dayjs(t).format('HH:mm:ss')

onMounted(() => {
  if (hasLab.value) {
    loadRecords()
    getLocation()
  }
})

onUnmounted(() => {
  stopCamera()
  if (capturedPhoto.value) URL.revokeObjectURL(capturedPhoto.value)
})
</script>

<style scoped>
.checkin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 28px;
  max-width: 900px;
  margin: 0 auto;
}

/* Empty state */
.checkin-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 20px;
  text-align: center;
}

.checkin-empty__icon { color: #cbd5e1; }

.checkin-empty h2 {
  font-size: 20px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

.checkin-empty p {
  color: #94a3b8;
  margin: 0;
}

/* Header */
.checkin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.checkin-header__title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.checkin-header__sub {
  font-size: 13px;
  color: #94a3b8;
  margin: 4px 0 0;
}

/* Camera card */
.checkin-card {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  padding: 24px;
}

.camera-box {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-video,
.camera-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #475569;
}

.camera-placeholder span {
  font-size: 13px;
  color: #64748b;
}

.camera-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.checkin-card__location {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f8fafc;
  font-size: 13px;
  color: #64748b;
}

/* Records */
.checkin-records {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  padding: 24px;
}

.checkin-records__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.checkin-records__head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.record-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.record-item__date {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 52px;
}

.record-item__date strong {
  font-size: 15px;
  color: #1e293b;
}

.record-item__date span {
  font-size: 11px;
  color: #94a3b8;
}

.record-item__info {
  flex: 1;
}

.record-item__info p {
  margin: 0;
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.record-item__loc {
  font-size: 12px;
  color: #94a3b8;
}

.record-item__thumb {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
}

@media (max-width: 640px) {
  .checkin-page { padding: 16px; }
  .camera-box { aspect-ratio: 3 / 4; }
}
</style>

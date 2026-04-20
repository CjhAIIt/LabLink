<template>
  <div class="m-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Student Check-in</p>
        <h1>{{ querySessionId ? '二维码签到' : '移动签到' }}</h1>
        <p>{{ querySessionId ? '当前页面已携带会话参数，可直接签到。' : '管理员创建会话后，输入 6 位签到码完成签到。' }}</p>
      </div>
      <button class="refresh-btn" type="button" @click="loadPageData">刷新</button>
    </section>

    <section v-if="!userStore.userInfo?.labId" class="empty-card">
      <el-empty description="加入实验室后才能使用签到功能" :image-size="86" />
    </section>

    <template v-else>
      <section class="panel-card">
        <header class="panel-head">
          <h2>签到入口</h2>
          <span class="status-chip">{{ isSessionActive ? '可签到' : '暂无会话' }}</span>
        </header>
        <el-form label-position="top">
          <el-form-item v-if="!querySessionId" label="签到码">
            <el-input v-model="signCode" maxlength="6" placeholder="输入 6 位签到码" inputmode="numeric" autofocus />
          </el-form-item>
          <el-button type="primary" :loading="signing" @click="handleSign">
            {{ querySessionId ? '立即签到' : '提交签到' }}
          </el-button>
        </el-form>
      </section>

      <section class="grid-card">
        <article class="info-card">
          <span>会话编号</span>
          <strong>{{ activeSession.sessionNo || '-' }}</strong>
        </article>
        <article class="info-card">
          <span>剩余时间</span>
          <strong>{{ isSessionActive ? `${activeSession.remainingSeconds || 0} 秒` : '-' }}</strong>
        </article>
        <article class="info-card">
          <span>结束时间</span>
          <strong>{{ formatDateTime(activeSession.expireTime) }}</strong>
        </article>
        <article class="info-card">
          <span>提示</span>
          <strong>{{ isSessionActive ? '结果会自动生成' : '等待管理员创建会话' }}</strong>
        </article>
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>最近记录</h2>
          <span>{{ historyTotal }} 条</span>
        </header>
        <div v-if="historyRows.length" class="record-list">
          <article v-for="row in historyRows" :key="row.id || row.attendanceDate" class="record-card">
            <div>
              <strong>{{ row.attendanceDate || '-' }}</strong>
              <p>{{ attendanceStatusLabel(row) }}</p>
              <small>{{ row.reason || '无备注' }}</small>
            </div>
            <div class="record-side">
              <span class="status-chip light">{{ formatDateTime(row.checkinTime || row.confirmTime) || '-' }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无考勤记录" :image-size="72" />
      </section>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveAttendanceSession, signAttendanceSession } from '@/api/attendance'
import { getMyAttendance } from '@/api/labSpace'
import { useUserStore } from '@/stores/user'
import { createSessionCountdown } from '@/utils/attendanceSession'
import { getAttendanceStatusText } from '@/utils/attendance-status'

const route = useRoute()
const userStore = useUserStore()
const querySessionId = computed(() => route.query.sessionId || '')
const signCode = ref('')
const signing = ref(false)
const historyRows = ref([])
const historyTotal = ref(0)

const activeSession = reactive({
  id: null,
  sessionNo: '',
  status: '',
  expireTime: '',
  remainingSeconds: 0
})
const sessionCountdown = createSessionCountdown(activeSession)
const isSessionActive = computed(() => activeSession.status === 'active')

const loadActiveSession = async () => {
  const response = await getActiveAttendanceSession()
  Object.assign(activeSession, {
    id: null,
    sessionNo: '',
    status: '',
    expireTime: '',
    remainingSeconds: 0,
    ...(response.data || {})
  })
  sessionCountdown.restart()
}

const loadHistory = async () => {
  const response = await getMyAttendance({ pageNum: 1, pageSize: 10 })
  historyRows.value = response.data?.records || []
  historyTotal.value = response.data?.total || 0
}

const loadPageData = async () => {
  await Promise.all([loadActiveSession(), loadHistory()])
}

const handleSign = async () => {
  signing.value = true
  try {
    if (querySessionId.value) {
      await signAttendanceSession({ sessionId: Number(querySessionId.value) })
    } else {
      if (!signCode.value.trim()) {
        ElMessage.warning('请输入签到码')
        return
      }
      await signAttendanceSession({ signCode: signCode.value.trim() })
    }
    signCode.value = ''
    ElMessage.success('签到成功')
    await loadPageData()
  } finally {
    signing.value = false
  }
}

const attendanceStatusLabel = (record) => getAttendanceStatusText(record)

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '')

onMounted(() => {
  loadPageData()
})

onUnmounted(() => {
  sessionCountdown.stop()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card,
.panel-card,
.grid-card,
.info-card,
.record-card,
.empty-card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(22, 163, 74, 0.88));
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1,
.panel-head h2 {
  margin: 0;
}

.hero-card p {
  color: rgba(226, 232, 240, 0.9);
}

.refresh-btn {
  height: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border-radius: 14px;
  padding: 10px 14px;
}

.panel-card,
.grid-card {
  padding: 14px;
}

.grid-card,
.record-list {
  display: grid;
  gap: 10px;
}

.grid-card {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-card,
.record-card {
  padding: 14px;
}

.panel-head,
.record-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.info-card span,
.record-card p,
.record-card small {
  color: #64748b;
}

.info-card strong,
.record-card strong {
  color: #0f172a;
}

.status-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(219, 234, 254, 0.9);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.status-chip.light {
  background: rgba(15, 23, 42, 0.06);
  color: #334155;
}

.record-side {
  display: grid;
  justify-items: end;
}

@media (max-width: 480px) {
  .grid-card {
    grid-template-columns: 1fr;
  }
}
</style>

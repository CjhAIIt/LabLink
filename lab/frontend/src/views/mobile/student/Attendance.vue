<template>
  <MobilePageContainer>
    <section class="attendance-hero">
      <div>
        <span class="mobile-kicker">Check-in</span>
        <h1>{{ querySessionId ? '二维码签到' : '今日考勤' }}</h1>
        <p>{{ isSessionActive ? '当前有可用签到会话，请在有效时间内完成打卡。' : '暂无进行中的签到会话，可先查看最近考勤记录。' }}</p>
      </div>
      <button class="attendance-refresh" type="button" :disabled="loading" @click="loadPageData">刷新</button>
    </section>

    <MobileEmptyState
      v-if="!userStore.userInfo?.labId"
      icon="OfficeBuilding"
      title="加入实验室后才能打卡"
      description="你还没有加入实验室，可以先去实验室广场寻找合适的团队。"
    >
      <el-button type="primary" @click="$router.push('/m/student/labs')">前往实验室广场</el-button>
    </MobileEmptyState>

    <template v-else>
      <section class="checkin-card" :class="{ active: isSessionActive }">
        <div class="checkin-card__header">
          <div>
            <span>今日状态</span>
            <h2>{{ isSessionActive ? '可以签到' : '等待签到会话' }}</h2>
          </div>
          <MobileStatusTag :type="isSessionActive ? 'active' : 'default'" :label="isSessionActive ? '可打卡' : '暂无会话'" />
        </div>

        <div v-if="isSessionActive || querySessionId" class="checkin-code-panel">
          <label v-if="!querySessionId">
            <span>签到码</span>
            <el-input
              v-model="signCode"
              maxlength="6"
              placeholder="输入 6 位签到码"
              inputmode="numeric"
              autofocus
              @keyup.enter="handleSign"
            />
          </label>
          <el-button type="primary" size="large" :loading="signing" @click="handleSign">
            {{ querySessionId ? '立即签到' : '提交签到' }}
          </el-button>
        </div>

        <p class="checkin-card__hint">
          {{ isSessionActive ? `剩余 ${activeSession.remainingSeconds || 0} 秒，结束后需要联系管理员处理异常。` : '管理员生成签到码后，这里会显示可打卡状态。' }}
        </p>
      </section>

      <section class="attendance-stat-grid">
        <MobileStatCard label="会话编号" :value="activeSession.sessionNo || '-'" description="当前签到会话" />
        <MobileStatCard label="剩余时间" :value="isSessionActive ? `${activeSession.remainingSeconds || 0}s` : '-'" description="动态码有效期" accent="#16A97A" />
        <MobileStatCard label="本周记录" :value="weekStats.total" description="最近 7 天记录数" accent="#19A7B8" />
        <MobileStatCard label="异常提醒" :value="weekStats.abnormal" description="缺勤、请假或忘签" accent="#D9861F" />
      </section>

      <section class="attendance-section">
        <header>
          <div>
            <h2>最近记录</h2>
            <p>共 {{ historyTotal }} 条，默认展示最近 10 条。</p>
          </div>
        </header>

        <div v-if="historyRows.length" class="attendance-record-list">
          <article v-for="row in historyRows" :key="row.id || row.attendanceDate" class="attendance-record-card">
            <div>
              <strong>{{ row.attendanceDate || '-' }}</strong>
              <p>{{ attendanceStatusLabel(row) }}</p>
              <small>{{ row.reason || '无备注' }}</small>
            </div>
            <MobileStatusTag type="primary" :label="formatDateTime(row.checkinTime || row.confirmTime) || '未打卡'" />
          </article>
        </div>

        <MobileEmptyState
          v-else
          icon="Calendar"
          title="暂无考勤记录"
          description="首次签到后，这里会展示你的历史记录和异常提醒。"
        />
      </section>
    </template>
  </MobilePageContainer>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveAttendanceSession, signAttendanceSession } from '@/api/attendance'
import { getMyAttendance } from '@/api/labSpace'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatCard from '@/components/mobile/MobileStatCard.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { useUserStore } from '@/stores/user'
import { createSessionCountdown } from '@/utils/attendanceSession'
import { getAttendanceStatusText } from '@/utils/attendance-status'

const route = useRoute()
const userStore = useUserStore()
const querySessionId = computed(() => route.query.sessionId || '')
const signCode = ref('')
const signing = ref(false)
const loading = ref(false)
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

const weekStats = computed(() => {
  const start = dayjs().subtract(6, 'day').startOf('day')
  const rows = historyRows.value.filter((row) => {
    const value = dayjs(row.attendanceDate || row.checkinTime || row.confirmTime)
    return value.isValid() && !value.isBefore(start)
  })
  const abnormal = rows.filter((row) => {
    const text = attendanceStatusLabel(row)
    return /缺勤|异常|请假|忘记|迟到/.test(text)
  }).length
  return {
    total: rows.length,
    abnormal
  }
})

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
  loading.value = true
  try {
    await Promise.all([loadActiveSession(), loadHistory()])
  } finally {
    loading.value = false
  }
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
const formatDateTime = (value) => (value ? dayjs(value).format('MM-DD HH:mm') : '')

onMounted(() => {
  loadPageData()
})

onUnmounted(() => {
  sessionCountdown.stop()
})
</script>

<style scoped>
.attendance-hero {
  padding: 22px;
  border-radius: 26px;
  color: #ffffff;
  background: linear-gradient(135deg, #15324b, #176b9a 48%, #16a97a);
  box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
  display: grid;
  gap: 16px;
}

.mobile-kicker {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  font-size: 12px;
  font-weight: 900;
}

.attendance-hero h1 {
  margin: 12px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.attendance-hero p {
  margin: 0;
  color: rgba(240, 249, 255, 0.88);
  line-height: 1.68;
}

.attendance-refresh {
  width: fit-content;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  font-weight: 900;
}

.checkin-card,
.attendance-section,
.attendance-record-card {
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.checkin-card {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.checkin-card.active {
  border-color: rgba(22, 169, 122, 0.22);
  background: linear-gradient(180deg, #effdf8, rgba(255, 255, 255, 0.96));
}

.checkin-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.checkin-card__header span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.checkin-card__header h2 {
  margin: 5px 0 0;
  color: #172033;
  font-size: 24px;
}

.checkin-code-panel {
  display: grid;
  gap: 12px;
}

.checkin-code-panel label {
  display: grid;
  gap: 8px;
}

.checkin-code-panel label span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.checkin-card__hint {
  margin: 0;
  color: #64748b;
  line-height: 1.65;
}

.attendance-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.attendance-section {
  padding: 17px;
  display: grid;
  gap: 14px;
}

.attendance-section header h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.attendance-section header p {
  margin: 5px 0 0;
  color: #64748b;
}

.attendance-record-list {
  display: grid;
  gap: 10px;
}

.attendance-record-card {
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.attendance-record-card strong {
  color: #172033;
}

.attendance-record-card p,
.attendance-record-card small {
  margin: 5px 0 0;
  color: #64748b;
  line-height: 1.45;
}

@media (max-width: 430px) {
  .attendance-stat-grid {
    grid-template-columns: 1fr;
  }

  .attendance-record-card,
  .checkin-card__header {
    flex-direction: column;
  }
}
</style>

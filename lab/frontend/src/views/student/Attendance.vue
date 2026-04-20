<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">学生签到</p>
          <h2>输入签到码或通过二维码直接签到</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!userStore.userInfo?.labId" class="empty-panel">
      <el-empty description="加入实验室后才能使用签到功能" />
    </div>

    <template v-else>
      <section class="metric-grid">
        <MetricCard label="当前状态" :value="isSessionActive ? '可签到' : '暂无会话'" tip="系统自动识别当前有效会话" />
        <MetricCard label="会话倒计时" :value="isSessionActive ? `${activeSession.remainingSeconds || 0} 秒` : '-'" tip="60 秒签到会话剩余时间" />
        <MetricCard label="最近记录" :value="historyRows.length" tip="最近考勤记录数量" />
      </section>

      <section class="content-grid two-column">
        <article class="sign-card">
          <div class="card-head">
            <div>
              <p class="panel-eyebrow">签到入口</p>
              <h3>{{ querySessionId ? '检测到二维码签到' : '输入签到码' }}</h3>
            </div>
            <span class="session-status">{{ isSessionActive ? '进行中' : '待开始' }}</span>
          </div>

          <p class="sign-tip">
            {{ querySessionId ? '点击下方按钮即可按当前二维码发起签到。' : '管理员生成签到码后，输入 6 位签到码完成签到。' }}
          </p>

          <el-form label-position="top">
            <el-form-item v-if="!querySessionId" label="签到码">
              <el-input v-model="signCode" maxlength="6" placeholder="请输入 6 位签到码" />
            </el-form-item>
            <el-button type="primary" :loading="signing" @click="handleSign">
              {{ querySessionId ? '立即签到' : '提交签到' }}
            </el-button>
          </el-form>
        </article>

        <article class="sign-card">
          <div class="card-head">
            <div>
              <p class="panel-eyebrow">当前会话</p>
              <h3>{{ activeSession.id ? activeSession.sessionNo : '暂无进行中会话' }}</h3>
            </div>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span>会话状态</span>
              <strong>{{ isSessionActive ? '进行中' : '未开始' }}</strong>
            </div>
            <div class="summary-item">
              <span>剩余时间</span>
              <strong>{{ isSessionActive ? `${activeSession.remainingSeconds || 0} 秒` : '-' }}</strong>
            </div>
            <div class="summary-item">
              <span>结束时间</span>
              <strong>{{ formatDateTime(activeSession.expireTime) }}</strong>
            </div>
            <div class="summary-item">
              <span>提示</span>
              <strong>{{ activeSession.id ? '会话结束后自动生成结果' : '等待管理员创建会话' }}</strong>
            </div>
          </div>
        </article>
      </section>

      <TablePageCard title="最近考勤结果" subtitle="个人记录" :count-label="`${historyTotal} 条`">
        <el-table :data="historyRows" stripe>
          <el-table-column prop="attendanceDate" label="日期" min-width="120" />
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">{{ attendanceStatusLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="签到时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.checkinTime || row.confirmTime) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="备注" min-width="220" show-overflow-tooltip />
        </el-table>
      </TablePageCard>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MetricCard from '@/components/common/MetricCard.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
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
  const response = await getMyAttendance({ pageNum: 1, pageSize: 12 })
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

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')

onMounted(() => {
  loadPageData()
})

onUnmounted(() => {
  sessionCountdown.stop()
})
</script>

<style scoped>
.sign-card {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-eyebrow,
.sign-tip,
.summary-item span {
  color: #64748b;
}

.panel-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.card-head h3 {
  margin: 0;
  color: #0f172a;
}

.session-status {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(219, 234, 254, 0.92);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.sign-tip {
  margin: 18px 0;
  line-height: 1.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
  display: grid;
  gap: 8px;
}

.summary-item strong {
  color: #0f172a;
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>

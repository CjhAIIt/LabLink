<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">晚自考勤</p>
          <h2>签到、补签与出勤记录</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!userStore.userInfo?.labId" class="empty-panel">
      <el-empty description="当前尚未加入实验室，无法进行晚自考勤。" />
    </div>

    <template v-else>
      <section class="metric-grid">
        <article v-for="card in metricCards" :key="card.label" class="metric-card">
          <span class="metric-label">{{ card.label }}</span>
          <strong class="metric-value">{{ card.value }}</strong>
          <span class="metric-tip">{{ card.tip }}</span>
        </article>
      </section>

      <section class="content-grid two-column">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>今日签到</span>
              <el-tag effect="plain">{{ currentDate }}</el-tag>
            </div>
          </template>

          <el-form label-width="80px" class="sign-form">
            <el-form-item label="签到状态">
              <el-select v-model="signForm.status" placeholder="选择签到状态">
                <el-option label="出勤" :value="1" />
                <el-option label="迟到" :value="2" />
                <el-option label="补签" :value="5" />
              </el-select>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="signForm.reason" type="textarea" :rows="3" maxlength="120" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitSignIn">提交签到</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>考勤概览</span>
              <el-tag type="success" effect="plain">月出勤率 {{ summary.monthlyRate ?? 0 }}%</el-tag>
            </div>
          </template>

          <div class="summary-grid">
            <div class="summary-item">
              <span>出勤</span>
              <strong>{{ summary.presentCount ?? 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>迟到</span>
              <strong>{{ summary.lateCount ?? 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>补签</span>
              <strong>{{ summary.makeupCount ?? 0 }}</strong>
            </div>
            <div class="summary-item">
              <span>缺勤</span>
              <strong>{{ summary.absentCount ?? 0 }}</strong>
            </div>
          </div>
        </el-card>
      </section>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>考勤记录</span>
            <el-tag type="primary" effect="plain">{{ history.length }} 条</el-tag>
          </div>
        </template>

        <el-table :data="history" stripe>
          <el-table-column prop="attendanceDate" label="日期" min-width="120" />
          <el-table-column prop="status" label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column label="确认时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.confirmTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { getAttendanceSummary, getMyAttendance, signInAttendance } from '@/api/labSpace'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentDate = dayjs().format('YYYY-MM-DD')
const summary = reactive({})
const history = ref([])
const signForm = reactive({
  status: 1,
  reason: ''
})

const metricCards = computed(() => [
  { label: '周出勤率', value: `${summary.weeklyRate ?? 0}%`, tip: '近 7 天签到表现' },
  { label: '月出勤率', value: `${summary.monthlyRate ?? 0}%`, tip: '近 30 天签到表现' },
  { label: '累计出勤', value: summary.presentCount ?? 0, tip: '正式签到次数' },
  { label: '异常记录', value: (summary.lateCount ?? 0) + (summary.absentCount ?? 0), tip: '迟到与缺勤总数' }
])

const loadPageData = async () => {
  const [summaryRes, historyRes] = await Promise.all([
    getAttendanceSummary(),
    getMyAttendance({ pageNum: 1, pageSize: 20 })
  ])
  Object.assign(summary, summaryRes.data || {})
  history.value = historyRes.data.records || []
}

const submitSignIn = async () => {
  await signInAttendance({
    attendanceDate: currentDate,
    status: signForm.status,
    reason: signForm.reason
  })
  ElMessage.success('签到成功')
  signForm.reason = ''
  await loadPageData()
}

const statusLabel = (status) => {
  const map = {
    1: '出勤',
    2: '迟到',
    3: '请假',
    4: '缺勤',
    5: '补签',
    6: '免考勤'
  }
  return map[status] || '待确认'
}

const statusTagType = (status) => {
  const map = {
    1: 'success',
    2: 'warning',
    3: 'primary',
    4: 'danger',
    5: 'info',
    6: ''
  }
  return map[status] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  loadPageData()
})
</script>

<style scoped>
.sign-form {
  max-width: 460px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  display: grid;
  gap: 6px;
}

.summary-item span {
  color: #64748b;
}
</style>

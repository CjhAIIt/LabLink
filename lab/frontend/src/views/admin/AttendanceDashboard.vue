<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">今日出勤看板</p>
          <h2>实时查看实验室成员出勤状态</h2>
        </div>
        <div class="toolbar-actions">
          <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 220px" @change="loadDashboard">
            <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
          </el-select>
          <el-button @click="loadDashboard">刷新数据</el-button>
        </div>
      </div>
    </section>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有可管理的实验室" />
    </div>

    <template v-else>
      <section class="metric-grid">
        <MetricCard label="总人数" :value="dashboard.total" tip="当前实验室在册成员" />
        <MetricCard label="已签到" :value="dashboard.signed" tip="签到成功成员" />
        <MetricCard label="迟到" :value="dashboard.late" tip="迟到签到成员" />
        <MetricCard label="缺勤" :value="dashboard.absent" tip="未签到成员" />
        <MetricCard label="请假" :value="dashboard.leave" tip="已请假成员" />
      </section>

      <section class="table-card">
        <div class="card-head">
          <h3>成员出勤明细</h3>
          <el-radio-group v-model="statusFilter" size="small" @change="applyFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="SIGNED">已签到</el-radio-button>
            <el-radio-button label="LATE">迟到</el-radio-button>
            <el-radio-button label="ABSENT">缺勤</el-radio-button>
            <el-radio-button label="LEAVE">请假</el-radio-button>
          </el-radio-group>
        </div>
<!-- PLACEHOLDER_TABLE -->
        <el-table :data="filteredMembers" stripe style="width: 100%">
          <el-table-column prop="studentId" label="学号" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="signTime" label="签到时间" width="140" />
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getTodayDashboard } from '@/api/attendance'
import { getAllLabs } from '@/api/lab'
import MetricCard from '@/components/common/MetricCard.vue'

const userStore = useUserStore()
const labOptions = ref([])
const selectedLabId = ref(null)
const statusFilter = ref('')
let refreshTimer = null

const dashboard = reactive({
  date: '',
  total: 0,
  signed: 0,
  late: 0,
  absent: 0,
  leave: 0,
  members: []
})

const showLabSelector = computed(() => userStore.userInfo?.role === 'super_admin')
const labId = computed(() => selectedLabId.value || userStore.userInfo?.labId)

const filteredMembers = computed(() => {
  if (!statusFilter.value) return dashboard.members
  return dashboard.members.filter(m => m.status === statusFilter.value)
})

const statusLabel = (status) => {
  const map = { SIGNED: '已签到', LATE: '迟到', ABSENT: '缺勤', LEAVE: '请假', NOT_STARTED: '未开始' }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = { SIGNED: 'success', LATE: 'warning', ABSENT: 'danger', LEAVE: 'info', NOT_STARTED: '' }
  return map[status] || ''
}

const applyFilter = () => {}

const loadDashboard = async () => {
  if (!labId.value) return
  try {
    const res = await getTodayDashboard({ labId: labId.value })
    const data = res.data?.data || res.data || {}
    dashboard.date = data.date || ''
    dashboard.total = data.total || 0
    dashboard.signed = data.signed || 0
    dashboard.late = data.late || 0
    dashboard.absent = data.absent || 0
    dashboard.leave = data.leave || 0
    dashboard.members = data.members || []
  } catch { /* ignore */ }
}

const loadLabOptions = async () => {
  if (!showLabSelector.value) return
  try {
    const res = await getAllLabs()
    const data = res.data?.data || res.data || {}
    labOptions.value = data.records || data || []
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadLabOptions()
  if (labId.value) loadDashboard()
  refreshTimer = setInterval(loadDashboard, 30000)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
})
</script>

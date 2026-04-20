<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">出勤统计报表</p>
          <h2>按时间段查看成员出勤率，支持导出 Excel</h2>
        </div>
        <div class="toolbar-actions">
          <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 220px" @change="loadStats">
            <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
          </el-select>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="loadStats" />
          <el-button @click="loadStats">查询</el-button>
          <el-button type="success" plain @click="handleExport">导出 Excel</el-button>
        </div>
      </div>
    </section>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有可管理的实验室" />
    </div>

    <template v-else>
      <section class="table-card">
        <el-table :data="statsList" stripe style="width: 100%" :default-sort="{ prop: 'attendanceRate', order: 'ascending' }">
          <el-table-column prop="studentId" label="学号" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="totalDays" label="应到天数" width="100" sortable />
          <el-table-column prop="signedDays" label="已签到" width="100" sortable />
          <el-table-column prop="lateDays" label="迟到" width="80" sortable />
          <el-table-column prop="absentDays" label="缺勤" width="80" sortable />
          <el-table-column prop="leaveDays" label="请假" width="80" sortable />
          <el-table-column prop="attendanceRate" label="出勤率(%)" width="120" sortable>
            <template #default="{ row }">
              <el-progress :percentage="row.attendanceRate" :stroke-width="14" :text-inside="true" />
            </template>
          </el-table-column>
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getAttendanceStatsBatch, exportAttendanceStats } from '@/api/attendance'
import { getAllLabs } from '@/api/lab'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const labOptions = ref([])
const selectedLabId = ref(null)
const statsList = ref([])
const dateRange = ref([])

const showLabSelector = computed(() => userStore.userInfo?.role === 'super_admin')
const labId = computed(() => selectedLabId.value || userStore.userInfo?.labId)

const loadStats = async () => {
  if (!labId.value || !dateRange.value || dateRange.value.length < 2) return
  try {
    const res = await getAttendanceStatsBatch({
      labId: labId.value,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    statsList.value = res.data?.data || res.data || []
  } catch { /* ignore */ }
}

const handleExport = async () => {
  if (!labId.value || !dateRange.value || dateRange.value.length < 2) {
    ElMessage.warning('请先选择日期范围')
    return
  }
  try {
    const res = await exportAttendanceStats({
      labId: labId.value,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `attendance-stats-${dateRange.value[0]}-${dateRange.value[1]}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
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
})
</script>

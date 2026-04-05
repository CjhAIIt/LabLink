<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">考勤任务</p>
          <h2>学院维度晚自习考勤任务配置</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadTasks">刷新</el-button>
          <el-button type="primary" @click="openTaskDialog()">新建任务</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item v-if="isSchoolDirector" label="学院">
          <el-select v-model="filters.collegeId" clearable placeholder="全部学院" style="width: 220px">
            <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="学期名称 / 任务名称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="metric-grid">
      <article v-for="card in summaryCards" :key="card.label" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-tip">{{ card.tip }}</span>
      </article>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="tasks" stripe>
        <el-table-column prop="collegeName" label="学院" min-width="160" />
        <el-table-column prop="semesterName" label="学期" min-width="160" />
        <el-table-column prop="taskName" label="任务名称" min-width="200" />
        <el-table-column label="执行周期" min-width="220">
          <template #default="{ row }">
            {{ row.startDate || '-' }} 至 {{ row.endDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="scheduleCount" label="排班数" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTaskDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="openScheduleDialog(row)">排班</el-button>
            <el-button
              v-if="row.status !== 'published'"
              link
              type="success"
              @click="publishTaskAction(row)"
            >
              发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="taskDialogVisible" :title="taskForm.id ? '编辑考勤任务' : '新建考勤任务'" width="720px">
      <el-form label-width="96px">
        <div class="two-column-form">
          <el-form-item label="学院">
            <el-select v-model="taskForm.collegeId" :disabled="!isSchoolDirector" placeholder="请选择学院">
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学期名称">
            <el-input v-model="taskForm.semesterName" />
          </el-form-item>
          <el-form-item label="任务名称">
            <el-input v-model="taskForm.taskName" />
          </el-form-item>
          <el-form-item label="执行周期">
            <el-date-picker
              v-model="taskDateRange"
              type="daterange"
              unlink-panels
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>
        </div>
        <el-form-item label="说明">
          <el-input v-model="taskForm.description" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingTask" @click="saveTaskAction">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scheduleDialogVisible" title="配置考勤排班" width="860px">
      <div class="schedule-toolbar">
        <div class="schedule-title">{{ currentTask?.taskName || '-' }}</div>
        <el-button type="primary" plain @click="addSchedule">新增一行</el-button>
      </div>

      <el-table :data="scheduleRows" stripe>
        <el-table-column label="星期" width="120">
          <template #default="{ row }">
            <el-select v-model="row.weekDay">
              <el-option v-for="item in weekOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="签到开始" min-width="150">
          <template #default="{ row }">
            <el-time-picker v-model="row.signInStart" value-format="HH:mm:ss" placeholder="开始时间" />
          </template>
        </el-table-column>
        <el-table-column label="签到结束" min-width="150">
          <template #default="{ row }">
            <el-time-picker v-model="row.signInEnd" value-format="HH:mm:ss" placeholder="结束时间" />
          </template>
        </el-table-column>
        <el-table-column label="迟到阈值(分)" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.lateThresholdMinutes" :min="1" :max="120" />
          </template>
        </el-table-column>
        <el-table-column label="签到码长度" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.signCodeLength" :min="4" :max="6" />
          </template>
        </el-table-column>
        <el-table-column label="有效期(分)" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.codeTtlMinutes" :min="1" :max="180" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeSchedule($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingSchedules" @click="saveSchedulesAction">保存排班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getAttendanceTaskPage,
  getAttendanceTaskSchedules,
  publishAttendanceTask,
  saveAttendanceTask,
  saveAttendanceTaskSchedules
} from '@/api/attendanceWorkflow'
import { getCollegeOptions } from '@/api/colleges'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const savingTask = ref(false)
const savingSchedules = ref(false)
const taskDialogVisible = ref(false)
const scheduleDialogVisible = ref(false)
const tasks = ref([])
const colleges = ref([])
const currentTask = ref(null)
const scheduleRows = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  collegeId: undefined,
  keyword: ''
})

const taskForm = reactive({
  id: null,
  collegeId: undefined,
  semesterName: '',
  taskName: '',
  description: '',
  startDate: '',
  endDate: ''
})

const taskDateRange = ref([])

const isSchoolDirector = computed(() => Boolean(userStore.userInfo?.schoolDirector))

const summaryCards = computed(() => [
  { label: '任务总数', value: pagination.total, tip: '当前可见范围内的考勤任务数量' },
  { label: '已发布', value: tasks.value.filter((item) => item.status === 'published').length, tip: '已经生效的考勤任务' },
  { label: '草稿', value: tasks.value.filter((item) => item.status !== 'published').length, tip: '还未发布的任务' },
  { label: '排班配置', value: tasks.value.reduce((sum, item) => sum + Number(item.scheduleCount || 0), 0), tip: '当前列表任务已配置的排班条数' }
])

const weekOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

const resetTaskForm = () => {
  Object.assign(taskForm, {
    id: null,
    collegeId: userStore.userInfo?.managedCollegeId || undefined,
    semesterName: '',
    taskName: '',
    description: '',
    startDate: '',
    endDate: ''
  })
  taskDateRange.value = []
}

const buildQuery = () => ({
  pageNum: pagination.pageNum,
  pageSize: pagination.pageSize,
  collegeId: isSchoolDirector.value ? filters.collegeId : userStore.userInfo?.managedCollegeId,
  keyword: filters.keyword || undefined
})

const loadOptions = async () => {
  const response = await getCollegeOptions()
  colleges.value = response.data || []
  if (!isSchoolDirector.value) {
    filters.collegeId = userStore.userInfo?.managedCollegeId
  }
}

const loadTasks = async () => {
  loading.value = true
  try {
    const response = await getAttendanceTaskPage(buildQuery())
    tasks.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const openTaskDialog = (row) => {
  resetTaskForm()
  if (row) {
    Object.assign(taskForm, {
      id: row.id,
      collegeId: row.collegeId,
      semesterName: row.semesterName,
      taskName: row.taskName,
      description: row.description || '',
      startDate: row.startDate,
      endDate: row.endDate
    })
    taskDateRange.value = [row.startDate, row.endDate]
  }
  taskDialogVisible.value = true
}

const saveTaskAction = async () => {
  if (!taskForm.collegeId) {
    ElMessage.warning('请选择学院')
    return
  }
  if (!taskForm.semesterName.trim() || !taskForm.taskName.trim()) {
    ElMessage.warning('请完整填写任务名称和学期名称')
    return
  }
  if (!taskDateRange.value?.length) {
    ElMessage.warning('请选择任务执行周期')
    return
  }

  savingTask.value = true
  try {
    const payload = {
      ...taskForm,
      startDate: taskDateRange.value[0],
      endDate: taskDateRange.value[1]
    }
    await saveAttendanceTask(payload)
    ElMessage.success('考勤任务已保存')
    taskDialogVisible.value = false
    await loadTasks()
  } finally {
    savingTask.value = false
  }
}

const openScheduleDialog = async (row) => {
  currentTask.value = row
  const response = await getAttendanceTaskSchedules(row.id)
  scheduleRows.value = (response.data || []).map((item) => ({
    id: item.id,
    weekDay: item.weekDay,
    signInStart: item.signInStart,
    signInEnd: item.signInEnd,
    lateThresholdMinutes: item.lateThresholdMinutes || 15,
    signCodeLength: item.signCodeLength || 4,
    codeTtlMinutes: item.codeTtlMinutes || 90,
    remark: item.remark || ''
  }))
  if (!scheduleRows.value.length) {
    addSchedule()
  }
  scheduleDialogVisible.value = true
}

const addSchedule = () => {
  scheduleRows.value.push({
    weekDay: 1,
    signInStart: '18:30:00',
    signInEnd: '21:30:00',
    lateThresholdMinutes: 15,
    signCodeLength: 4,
    codeTtlMinutes: 90,
    remark: ''
  })
}

const removeSchedule = (index) => {
  scheduleRows.value.splice(index, 1)
}

const saveSchedulesAction = async () => {
  if (!currentTask.value?.id) {
    return
  }
  if (!scheduleRows.value.length) {
    ElMessage.warning('至少保留一条排班')
    return
  }

  savingSchedules.value = true
  try {
    await saveAttendanceTaskSchedules(currentTask.value.id, scheduleRows.value)
    ElMessage.success('排班已保存')
    scheduleDialogVisible.value = false
    await loadTasks()
  } finally {
    savingSchedules.value = false
  }
}

const publishTaskAction = async (row) => {
  await publishAttendanceTask(row.id)
  ElMessage.success('考勤任务已发布')
  await loadTasks()
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadTasks()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadTasks()
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(async () => {
  resetTaskForm()
  await loadOptions()
  await loadTasks()
})
</script>

<style scoped>
.two-column-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.schedule-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.schedule-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

@media (max-width: 768px) {
  .two-column-form {
    grid-template-columns: 1fr;
  }

  .schedule-toolbar {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }
}
</style>

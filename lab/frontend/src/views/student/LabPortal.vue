<template>
  <div class="lab-portal">
    <el-empty
      v-if="!loadingOverview && !overview.lab"
      description="你还没有加入实验室。"
    />

    <template v-else>
      <el-skeleton :loading="loadingOverview" animated>
        <template #template>
          <el-card style="min-height: 220px" />
        </template>

        <template #default>
          <el-card class="hero-card" shadow="never">
            <div class="hero-content">
              <div class="hero-copy">
                <p class="hero-eyebrow">实验室空间</p>
                <h2>{{ overview.lab?.labName }}</h2>
                <p class="hero-desc">
                  {{ overview.lab?.labDesc || '该实验室暂未补充详细介绍。' }}
                </p>
                <div class="hero-meta">
                  <span>指导教师：{{ overview.lab?.teacherName || overview.lab?.advisors || '暂未填写' }}</span>
                  <span>当前管理员：{{ overview.lab?.currentAdmins || '暂未填写' }}</span>
                </div>
              </div>

              <div class="hero-stats">
                <div class="stat-card">
                  <div class="stat-label">正式成员</div>
                  <div class="stat-value">{{ overview.memberCount || 0 }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">招新状态</div>
                  <div class="stat-value">{{ getLabStatusText(overview.lab?.status) }}</div>
                </div>
              </div>
            </div>

            <div v-if="overview.members.length" class="member-list">
              <span class="member-label">成员</span>
              <el-tag
                v-for="member in overview.members"
                :key="member.id"
                class="member-tag"
                effect="plain"
              >
                {{ member.realName || member.username }}
              </el-tag>
            </div>
          </el-card>
        </template>
      </el-skeleton>

      <el-tabs v-model="activeTab" class="portal-tabs">
        <el-tab-pane label="设备" name="equipment">
          <TablePageCard
            title="实验室设备"
            subtitle="设备借用"
            :count-label="`${equipmentPagination.total} 项`"
          >
            <template #header-extra>
              <el-button type="primary" @click="fetchEquipment">刷新</el-button>
            </template>

            <template #filters>
              <el-form :inline="true" :model="equipmentSearch" class="toolbar">
                <el-form-item label="设备名称">
                  <el-input
                    v-model="equipmentSearch.name"
                    clearable
                    placeholder="请输入设备名称"
                    @keyup.enter="handleEquipmentSearch"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleEquipmentSearch">搜索</el-button>
                </el-form-item>
              </el-form>
            </template>

            <el-table :data="equipmentList" border stripe>
              <el-table-column prop="name" label="设备" min-width="180" />
              <el-table-column prop="type" label="类型" width="140" />
              <el-table-column prop="serialNumber" label="序列号" width="180" />
              <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label-map="equipmentStatusLabels" :type-map="equipmentStatusTypes" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="row.status !== 0"
                    @click="openBorrowDialog(row)"
                  >
                    借用
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <template #pagination>
              <el-pagination
                v-model:current-page="equipmentPagination.current"
                v-model:page-size="equipmentPagination.size"
                :total="equipmentPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchEquipment"
              />
            </template>
          </TablePageCard>

          <TablePageCard
            class="sub-card"
            title="我的借用记录"
            subtitle="借用进度"
            :count-label="`${borrowPagination.total} 条`"
          >
            <template #header-extra>
              <el-button @click="fetchMyBorrowList">刷新</el-button>
            </template>

            <el-table :data="myBorrowList" border stripe>
              <el-table-column label="设备" min-width="180">
                <template #default="{ row }">
                  {{ getEquipmentName(row.equipmentId) }}
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="借用原因" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label-map="borrowStatusLabels" :type-map="borrowStatusTypes" />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="提交时间" width="180" />
              <el-table-column prop="borrowTime" label="借出时间" width="180" />
              <el-table-column prop="returnTime" label="归还时间" width="180" />
            </el-table>

            <template #pagination>
              <el-pagination
                v-model:current-page="borrowPagination.current"
                v-model:page-size="borrowPagination.size"
                :total="borrowPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyBorrowList"
              />
            </template>
          </TablePageCard>
        </el-tab-pane>

        <el-tab-pane label="考勤" name="attendance">
          <TablePageCard
            title="我的考勤"
            subtitle="日常签到"
            :count-label="`${attendancePagination.total} 条`"
          >
            <template #header-extra>
              <el-button @click="fetchMyAttendance">刷新</el-button>
            </template>

            <el-alert
              :title="todayAttendanceTitle"
              :type="todayAttendanceType"
              :closable="false"
              show-icon
            />

            <el-table class="top-gap" :data="attendanceList" border stripe>
              <el-table-column prop="attendanceDate" label="日期" width="140" />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <StatusTag :value="row.tagType || row.status" :label-map="attendanceStatusLabels" :type-map="attendanceStatusTypes" />
                  </template>
                </el-table-column>
              <el-table-column prop="reason" label="备注 / 原因" min-width="220" show-overflow-tooltip />
              <el-table-column prop="confirmTime" label="确认时间" width="180" />
            </el-table>

            <template #pagination>
              <el-pagination
                v-model:current-page="attendancePagination.current"
                v-model:page-size="attendancePagination.size"
                :total="attendancePagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyAttendance"
              />
            </template>
          </TablePageCard>
        </el-tab-pane>

        <el-tab-pane label="退组申请" name="exit">
          <TablePageCard
            title="退出当前实验室"
            subtitle="成员流转"
            :count-label="`${exitPagination.total} 条`"
          >
            <template #header-extra>
              <el-button @click="fetchMyExitApplications">刷新</el-button>
            </template>

            <el-alert
              title="如果你希望退出当前实验室后再申请其他实验室，请先提交退组申请。审核通过后，账号会恢复为普通学生状态。"
              type="warning"
              :closable="false"
              show-icon
            />

            <el-form class="top-gap" label-position="top">
              <el-form-item label="原因">
                <el-input
                  v-model="exitReason"
                  type="textarea"
                  :rows="4"
                  maxlength="300"
                  show-word-limit
                  placeholder="请说明退组原因或下一步计划"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="danger"
                  :disabled="hasPendingExitApplication"
                  @click="submitExitRequest"
                >
                  提交退组申请
                </el-button>
                <span v-if="hasPendingExitApplication" class="hint-text">
                  当前已有待审核的退组申请，请等待处理后再提交新的申请。
                </span>
              </el-form-item>
            </el-form>
          </TablePageCard>

          <TablePageCard
            class="sub-card"
            title="我的退组记录"
            subtitle="审核进度"
            :count-label="`${exitPagination.total} 条`"
          >
            <el-table :data="exitApplications" border stripe>
              <el-table-column prop="labName" label="实验室" min-width="180" />
              <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label-map="exitStatusLabels" :type-map="exitStatusTypes" />
                </template>
              </el-table-column>
              <el-table-column prop="auditRemark" label="审核意见" min-width="180" show-overflow-tooltip />
              <el-table-column prop="createTime" label="提交时间" width="180" />
              <el-table-column prop="auditTime" label="审核时间" width="180" />
            </el-table>

            <template #pagination>
              <el-pagination
                v-model:current-page="exitPagination.current"
                v-model:page-size="exitPagination.size"
                :total="exitPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyExitApplications"
              />
            </template>
          </TablePageCard>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="borrowDialog.visible" title="借用设备" width="480px">
      <el-form label-position="top">
        <el-form-item label="设备">
          <el-input :model-value="borrowDialog.equipmentName" disabled />
        </el-form-item>
        <el-form-item label="借用原因">
          <el-input
            v-model="borrowDialog.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明借用该设备的用途"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="borrowDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitBorrow">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEquipmentList, borrowEquipment, getMyBorrowList } from '@/api/equipment'
import {
  getLabSpaceOverview,
  getMyAttendance,
  getMyExitApplications,
  submitExitApplication
} from '@/api/labSpace'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import {
  attendanceStatusLabels,
  attendanceStatusTypes,
  getAttendanceStatusText,
  getAttendanceStatusType
} from '@/utils/attendance-status'

const activeTab = ref('equipment')
const loadingOverview = ref(true)
const overview = reactive({
  lab: null,
  memberCount: 0,
  members: []
})

const equipmentSearch = reactive({
  name: ''
})
const equipmentStatusLabels = {
  0: '闲置',
  1: '借出中',
  2: '维修中'
}
const equipmentStatusTypes = {
  0: 'success',
  1: 'warning',
  2: 'danger'
}
const borrowStatusLabels = {
  0: '待审批',
  1: '已借出',
  2: '已驳回',
  3: '已归还'
}
const borrowStatusTypes = {
  0: 'info',
  1: 'primary',
  2: 'danger',
  3: 'success'
}
const exitStatusLabels = {
  0: '待审核',
  1: '已通过',
  2: '已驳回'
}
const exitStatusTypes = {
  0: 'warning',
  1: 'success',
  2: 'danger'
}
const equipmentList = ref([])
const equipmentMap = ref({})
const equipmentPagination = reactive({ current: 1, size: 10, total: 0 })

const myBorrowList = ref([])
const borrowPagination = reactive({ current: 1, size: 10, total: 0 })
const borrowDialog = reactive({
  visible: false,
  equipmentId: null,
  equipmentName: '',
  reason: ''
})

const attendanceList = ref([])
const attendancePagination = reactive({ current: 1, size: 10, total: 0 })

const exitReason = ref('')
const exitApplications = ref([])
const exitPagination = reactive({ current: 1, size: 10, total: 0 })

const getTodayString = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = computed(() => getTodayString())
const todayAttendance = computed(() =>
  attendanceList.value.find((item) => item.attendanceDate === today.value)
)
const todayAttendanceTitle = computed(() => {
  if (!todayAttendance.value) {
    return '今日状态：等待管理员确认'
  }
  return `今日状态：${getAttendanceStatusText(todayAttendance.value)}`
})
const todayAttendanceType = computed(() => {
  if (!todayAttendance.value) {
    return 'info'
  }
  return getAttendanceStatusType(todayAttendance.value)
})
const hasPendingExitApplication = computed(() =>
  exitApplications.value.some((item) => item.status === 0)
)

const fetchOverview = async () => {
  loadingOverview.value = true
  try {
    const res = await getLabSpaceOverview()
    overview.lab = res.data.lab || null
    overview.memberCount = res.data.memberCount || 0
    overview.members = res.data.members || []
  } catch (error) {
    overview.lab = null
    overview.memberCount = 0
    overview.members = []
  } finally {
    loadingOverview.value = false
  }
}

const fetchEquipment = async () => {
  if (!overview.lab?.id) {
    return
  }

  const res = await getEquipmentList({
    pageNum: equipmentPagination.current,
    pageSize: equipmentPagination.size,
    labId: overview.lab.id,
    name: equipmentSearch.name || undefined
  })

  equipmentList.value = res.data.records || []
  equipmentPagination.total = res.data.total || 0

  const nextMap = { ...equipmentMap.value }
  equipmentList.value.forEach((item) => {
    nextMap[item.id] = item.name
  })
  equipmentMap.value = nextMap
}

const fetchMyBorrowList = async () => {
  const res = await getMyBorrowList({
    pageNum: borrowPagination.current,
    pageSize: borrowPagination.size
  })
  myBorrowList.value = res.data.records || []
  borrowPagination.total = res.data.total || 0
}

const fetchMyAttendance = async () => {
  const res = await getMyAttendance({
    pageNum: attendancePagination.current,
    pageSize: attendancePagination.size
  })
  attendanceList.value = res.data.records || []
  attendancePagination.total = res.data.total || 0
}

const fetchMyExitApplications = async () => {
  const res = await getMyExitApplications({
    pageNum: exitPagination.current,
    pageSize: exitPagination.size
  })
  exitApplications.value = res.data.records || []
  exitPagination.total = res.data.total || 0
}

const handleEquipmentSearch = () => {
  equipmentPagination.current = 1
  fetchEquipment()
}

const openBorrowDialog = (row) => {
  borrowDialog.visible = true
  borrowDialog.equipmentId = row.id
  borrowDialog.equipmentName = row.name
  borrowDialog.reason = ''
}

const submitBorrow = async () => {
  if (!borrowDialog.reason.trim()) {
    ElMessage.warning('请先填写借用原因')
    return
  }

  await borrowEquipment({
    equipmentId: borrowDialog.equipmentId,
    reason: borrowDialog.reason.trim()
  })
  ElMessage.success('借用申请已提交')
  borrowDialog.visible = false
  fetchEquipment()
  fetchMyBorrowList()
}

const submitExitRequest = async () => {
  if (!exitReason.value.trim()) {
    ElMessage.warning('请先填写退组原因')
    return
  }

  await submitExitApplication({
    reason: exitReason.value.trim()
  })
  ElMessage.success('退组申请已提交')
  exitReason.value = ''
  fetchMyExitApplications()
}

const getEquipmentName = (equipmentId) => {
  return equipmentMap.value[equipmentId] || `设备 #${equipmentId}`
}

const getLabStatusText = (status) => {
  if (status === 1) return '招新中'
  if (status === 2) return '已关闭'
  return '未开始'
}

onMounted(async () => {
  await fetchOverview()
  await Promise.all([fetchEquipment(), fetchMyBorrowList(), fetchMyAttendance(), fetchMyExitApplications()])
})
</script>

<style scoped>
.lab-portal {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  border: 1px solid rgba(14, 116, 144, 0.18);
  background:
    radial-gradient(circle at right top, rgba(34, 211, 238, 0.16), transparent 35%),
    linear-gradient(135deg, #f0fdfa, #eff6ff);
}

.hero-content {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.hero-eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.hero-copy h2 {
  margin: 0;
  font-size: 30px;
  color: #0f172a;
}

.hero-desc {
  margin: 12px 0;
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #334155;
  font-size: 14px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
  min-width: 320px;
}

.stat-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.member-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.member-label {
  color: #0f172a;
  font-weight: 600;
}

.member-tag {
  margin-right: 0;
}

.portal-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.sub-card {
  margin-top: 16px;
}

.top-gap {
  margin-top: 16px;
}

.hint-text {
  margin-left: 12px;
  color: #d97706;
  font-size: 13px;
}

@media (max-width: 768px) {
  .hero-copy h2 {
    font-size: 24px;
  }

  .hero-stats {
    min-width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

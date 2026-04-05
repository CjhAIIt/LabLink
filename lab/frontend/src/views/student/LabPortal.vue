<template>
  <div class="lab-portal">
    <el-empty
      v-if="!loadingOverview && !overview.lab"
      description="你当前还没有加入实验室"
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
                  {{ overview.lab?.labDesc || '当前实验室暂未填写详细介绍。' }}
                </p>
                <div class="hero-meta">
                  <span>指导老师：{{ overview.lab?.advisors || '未填写' }}</span>
                  <span>当前管理员：{{ overview.lab?.currentAdmins || '未填写' }}</span>
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
              <span class="member-label">实验室成员</span>
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
        <el-tab-pane label="设备借用" name="equipment">
          <el-card shadow="never">
            <template #header>
              <div class="section-header">
                <span>实验室设备</span>
                <el-button type="primary" @click="fetchEquipment">刷新</el-button>
              </div>
            </template>

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

            <el-table :data="equipmentList" border stripe>
              <el-table-column prop="name" label="设备名称" min-width="180" />
              <el-table-column prop="type" label="类型" width="140" />
              <el-table-column prop="serialNumber" label="编号" width="180" />
              <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getEquipmentStatusType(row.status)">
                    {{ getEquipmentStatusText(row.status) }}
                  </el-tag>
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

            <div class="pagination">
              <el-pagination
                v-model:current-page="equipmentPagination.current"
                v-model:page-size="equipmentPagination.size"
                :total="equipmentPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchEquipment"
              />
            </div>
          </el-card>

          <el-card class="sub-card" shadow="never">
            <template #header>
              <div class="section-header">
                <span>我的设备借用记录</span>
                <el-button @click="fetchMyBorrowList">刷新</el-button>
              </div>
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
                  <el-tag :type="getBorrowStatusType(row.status)">
                    {{ getBorrowStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="申请时间" width="180" />
              <el-table-column prop="borrowTime" label="借出时间" width="180" />
              <el-table-column prop="returnTime" label="归还时间" width="180" />
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="borrowPagination.current"
                v-model:page-size="borrowPagination.size"
                :total="borrowPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyBorrowList"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="每日打卡" name="attendance">
          <el-card shadow="never">
            <template #header>
              <div class="section-header">
                <span>我的打卡记录</span>
                <el-button @click="fetchMyAttendance">刷新</el-button>
              </div>
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
                  <el-tag :type="getAttendanceStatusType(row.status)">
                    {{ getAttendanceStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="备注/原因" min-width="220" show-overflow-tooltip />
              <el-table-column prop="confirmTime" label="确认时间" width="180" />
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="attendancePagination.current"
                v-model:page-size="attendancePagination.size"
                :total="attendancePagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyAttendance"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="退出申请" name="exit">
          <el-card shadow="never">
            <template #header>
              <div class="section-header">
                <span>实验室退出申请</span>
                <el-button @click="fetchMyExitApplications">刷新</el-button>
              </div>
            </template>

            <el-alert
              title="正式成员如需退出当前实验室或准备申请其他实验室，请先提交退出申请，管理员审核通过后恢复为普通学生。"
              type="warning"
              :closable="false"
              show-icon
            />

            <el-form class="top-gap" label-position="top">
              <el-form-item label="申请原因">
                <el-input
                  v-model="exitReason"
                  type="textarea"
                  :rows="4"
                  maxlength="300"
                  show-word-limit
                  placeholder="请填写退出原因或后续计划"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="danger"
                  :disabled="hasPendingExitApplication"
                  @click="submitExitRequest"
                >
                  提交退出申请
                </el-button>
                <span v-if="hasPendingExitApplication" class="hint-text">
                  当前已有待审核申请，请等待管理员处理。
                </span>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="sub-card" shadow="never">
            <template #header>
              <span>我的退出申请记录</span>
            </template>

            <el-table :data="exitApplications" border stripe>
              <el-table-column prop="labName" label="实验室" min-width="180" />
              <el-table-column prop="reason" label="申请原因" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getExitStatusType(row.status)">
                    {{ getExitStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="auditRemark" label="审核备注" min-width="180" show-overflow-tooltip />
              <el-table-column prop="createTime" label="申请时间" width="180" />
              <el-table-column prop="auditTime" label="审核时间" width="180" />
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="exitPagination.current"
                v-model:page-size="exitPagination.size"
                :total="exitPagination.total"
                layout="total, prev, pager, next"
                @current-change="fetchMyExitApplications"
              />
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="borrowDialog.visible" title="设备借用申请" width="480px">
      <el-form label-position="top">
        <el-form-item label="设备名称">
          <el-input :model-value="borrowDialog.equipmentName" disabled />
        </el-form-item>
        <el-form-item label="借用原因">
          <el-input
            v-model="borrowDialog.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写借用原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="borrowDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitBorrow">提交申请</el-button>
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
    return '今日打卡状态：等待管理员确认'
  }
  return `今日打卡状态：${getAttendanceStatusText(todayAttendance.value.status)}`
})
const todayAttendanceType = computed(() => {
  if (!todayAttendance.value) {
    return 'info'
  }
  return getAttendanceStatusType(todayAttendance.value.status)
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
    ElMessage.warning('请填写借用原因')
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
    ElMessage.warning('请填写退出原因')
    return
  }

  await submitExitApplication({
    reason: exitReason.value.trim()
  })
  ElMessage.success('退出申请已提交，请等待管理员审核')
  exitReason.value = ''
  fetchMyExitApplications()
}

const getEquipmentName = (equipmentId) => {
  return equipmentMap.value[equipmentId] || `设备 #${equipmentId}`
}

const getEquipmentStatusText = (status) => {
  if (status === 1) return '借用中'
  if (status === 2) return '维修中'
  return '空闲'
}

const getEquipmentStatusType = (status) => {
  if (status === 1) return 'warning'
  if (status === 2) return 'danger'
  return 'success'
}

const getBorrowStatusText = (status) => {
  if (status === 1) return '已借出'
  if (status === 2) return '已拒绝'
  if (status === 3) return '已归还'
  return '申请中'
}

const getBorrowStatusType = (status) => {
  if (status === 1) return 'primary'
  if (status === 2) return 'danger'
  if (status === 3) return 'success'
  return 'info'
}

const getAttendanceStatusText = (status) => {
  if (status === 1) return '已到'
  if (status === 2) return '未到'
  return '待确认'
}

const getAttendanceStatusType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

const getExitStatusText = (status) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '待审核'
}

const getExitStatusType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

const getLabStatusText = (status) => {
  if (status === 1) return '招新中'
  if (status === 2) return '已结束'
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

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sub-card {
  margin-top: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
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

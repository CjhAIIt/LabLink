<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室管理</p>
          <h2>基础信息、管理员安排与资料审核</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="refreshPage">刷新</el-button>
          <el-button v-if="isSuperAdmin" type="primary" @click="openDialog()">新增实验室</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="实验室名称">
          <el-input v-model="filters.labName" clearable placeholder="输入名称搜索" @keyup.enter="loadLabs" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="开放" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLabs">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-alert
      class="admin-arrangement-tip"
      type="info"
      :closable="false"
      show-icon
      title="管理员安排规则"
      description="实验室暂无管理员时，由学校或学院管理员安排学生管理员；实验室已有管理员时，由当前管理员在实验室内部移交权限。"
    />

    <TablePageCard title="实验室列表" subtitle="基础信息与管理员安排" :count-label="`${pagination.total} 个实验室`">
      <el-table v-loading="loading" :data="labs" stripe>
        <el-table-column label="实验室" min-width="220">
          <template #default="{ row }">
            <div class="lab-cell">
              <div class="lab-cell__logo">
                <img v-if="row.logoUrl" :src="resolveMedia(row.logoUrl)" alt="实验室 Logo" />
                <span v-else>{{ (row.labName || 'L').charAt(0) }}</span>
              </div>
              <div class="lab-cell__copy">
                <strong>{{ row.labName }}</strong>
                <small>{{ row.labCode || `LAB-${row.id}` }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="labCode" label="编码" min-width="120" />
        <el-table-column prop="teacherName" label="指导教师" min-width="120" />
        <el-table-column prop="location" label="地点" min-width="140" />
        <el-table-column prop="recruitNum" label="计划容量" min-width="100" />
        <el-table-column prop="currentNum" label="当前成员" min-width="100" />
        <el-table-column label="当前管理员" min-width="180">
          <template #default="{ row }">
            <div v-if="row.currentAdmin">
              <div>{{ row.currentAdmin.realName || row.currentAdmin.username }}</div>
              <div class="admin-meta">{{ row.currentAdmin.studentId || row.currentAdmin.username }}</div>
            </div>
            <span v-else class="empty-text">待安排</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
              {{ Number(row.status) === 1 ? '开放' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="360">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/lab-info/${row.id}`)">查看</el-button>
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button
              v-if="(isSuperAdmin || isCollegeManager) && !row.currentAdmin"
              link
              type="success"
              @click="openAssignAdminDialog(row)"
            >
              安排学生管理员
            </el-button>
            <span v-else-if="isSuperAdmin || isCollegeManager" class="action-tip">已有管理员时由当前管理员移交</span>
            <el-button
              v-if="(isSuperAdmin || isCollegeManager) && row.currentAdmin"
              link
              type="danger"
              @click="handleRevokeAdmin(row)"
            >
              撤销管理员
            </el-button>
            <el-button v-if="canTransferAdmin" link type="warning" @click="openTransferDialog(row)">移交权限</el-button>
            <el-button v-if="isSuperAdmin" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          v-if="isSuperAdmin || isCollegeManager"
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </template>
    </TablePageCard>

    <TablePageCard
      v-if="isReviewAdmin"
      title="资料变更审核"
      subtitle="实验室管理员提交后，由学校管理员审核生效"
      :count-label="`${reviewPagination.total} 条`"
      class="review-card"
    >
      <template #filters>
        <div class="toolbar-actions compact">
          <el-input
            v-model="reviewFilters.keyword"
            clearable
            placeholder="按实验室或申请人搜索"
            style="width: 260px"
            @keyup.enter="loadPendingReviews"
          />
          <el-button @click="loadPendingReviews">查询</el-button>
        </div>
      </template>

      <el-table v-loading="reviewLoading" :data="pendingReviews" stripe>
        <el-table-column prop="labName" label="当前实验室" min-width="180" />
        <el-table-column prop="proposedLabName" label="申请后名称" min-width="180" />
        <el-table-column prop="collegeName" label="学院" min-width="160" />
        <el-table-column label="申请人" min-width="160">
          <template #default="{ row }">{{ row.applicantName || row.applicantUsername || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reviewStatusType(row.reviewStatus)">{{ reviewStatusText(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReviewDetail(row)">查看详情</el-button>
            <el-button link type="success" @click="handleReviewAction('approve', row)">通过</el-button>
            <el-button link type="danger" @click="handleReviewAction('reject', row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="reviewPagination.pageNum"
          :page-size="reviewPagination.pageSize"
          :total="reviewPagination.total"
          @current-change="handleReviewPageChange"
        />
      </template>
    </TablePageCard>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px">
      <div class="dialog-section">
        <el-alert
          v-if="!isSuperAdmin && latestManagedReview"
          :type="reviewStatusType(latestManagedReview.reviewStatus)"
          :closable="false"
          show-icon
          :title="`最近一次申请状态：${reviewStatusText(latestManagedReview.reviewStatus)}`"
          :description="buildLatestReviewDescription(latestManagedReview)"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="dialog-form">
          <div class="two-column-form">
            <el-form-item label="实验室名称" prop="labName"><el-input v-model="form.labName" /></el-form-item>
            <el-form-item label="实验室编码" prop="labCode"><el-input v-model="form.labCode" /></el-form-item>
            <el-form-item label="所属学院" prop="collegeId">
              <el-select v-model="form.collegeId" clearable placeholder="请选择学院" :disabled="!isSuperAdmin">
                <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="指导教师" prop="teacherName"><el-input v-model="form.teacherName" /></el-form-item>
            <el-form-item label="实验室地点" prop="location"><el-input v-model="form.location" /></el-form-item>
            <el-form-item label="联系邮箱" prop="contactEmail"><el-input v-model="form.contactEmail" /></el-form-item>
            <el-form-item label="计划容量" prop="recruitNum"><el-input-number v-model="form.recruitNum" :min="1" :max="999" /></el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status"><el-option label="开放" :value="1" /><el-option label="关闭" :value="0" /></el-select>
            </el-form-item>
          </div>
          <div class="media-upload-grid">
            <ImageUploadField
              v-model="form.logoUrl"
              title="实验室 Logo"
              description="用于实验室列表、详情页和品牌展示区。"
              scene="logo"
              accept=".jpg,.jpeg,.png,.svg,.webp"
              :size-limit="5"
            />
            <ImageUploadField
              v-model="form.coverImageUrl"
              title="实验室封面图"
              description="用于详情页头图和宣传展示，建议上传横向图片。"
              scene="image"
              accept=".jpg,.jpeg,.png,.webp"
              :size-limit="8"
            />
          </div>
          <el-form-item label="招新要求" prop="requireSkill"><el-input v-model="form.requireSkill" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="实验室简介" prop="labDesc"><el-input v-model="form.labDesc" type="textarea" :rows="4" /></el-form-item>
          <el-form-item label="基础信息" prop="basicInfo"><el-input v-model="form.basicInfo" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="获奖成果" prop="awards"><el-input v-model="form.awards" type="textarea" :rows="3" /></el-form-item>
        </el-form>

        <div v-if="!isSuperAdmin" class="history-block">
          <div class="history-header">
            <strong>最近申请记录</strong>
            <el-button text @click="loadCurrentLabChangeHistory(form.id)">刷新记录</el-button>
          </div>
          <div v-if="reviewHistoryLoading" class="history-empty">正在加载申请记录...</div>
          <el-timeline v-else-if="currentChangeHistory.length">
            <el-timeline-item
              v-for="item in currentChangeHistory"
              :key="item.reviewId"
              :timestamp="formatDateTime(item.submitTime)"
              :type="reviewStatusType(item.reviewStatus)"
            >
              <div class="timeline-title">{{ reviewStatusText(item.reviewStatus) }}</div>
              <div class="timeline-text">{{ item.reviewComment || '暂无审核意见' }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无资料变更申请记录" />
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ saveButtonLabel }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="安排学生管理员" width="760px">
      <div class="dialog-section">
        <el-alert type="info" :closable="false" show-icon title="学校/学院管理员安排" description="仅当实验室当前没有管理员时，才建议由上级管理员直接安排学生管理员。" />
        <div class="dialog-lab-card">
          <div class="dialog-lab-title">{{ currentAdminLab?.labName }}</div>
          <div class="dialog-lab-subtitle">实验室编码：{{ currentAdminLab?.labCode || '未设置' }} · 当前成员：{{ currentAdminLab?.currentNum ?? 0 }}</div>
        </div>
        <el-input v-model="assignFilters.keyword" clearable placeholder="按学号、姓名、用户名筛选学生" />
        <el-table v-loading="adminSelectionLoading" :data="filteredAssignCandidates" highlight-current-row height="360px" @current-change="handleStudentCurrentChange" @row-click="handleStudentCurrentChange">
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="major" label="专业" min-width="160" />
          <el-table-column prop="grade" label="年级" min-width="100" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigningAdmin" :disabled="!selectedAdminStudent" @click="submitAssignAdmin">确认安排</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDialogVisible" title="移交管理员权限" width="760px">
      <div class="dialog-section">
        <el-alert type="warning" :closable="false" show-icon title="当前管理员移交" description="移交成功后，当前账号会自动退出管理端，请先确认目标学生信息。" />
        <div class="dialog-lab-card">
          <div class="dialog-lab-title">{{ currentAdminLab?.labName }}</div>
          <div class="dialog-lab-subtitle">当前管理员：{{ userStore.realName || userStore.username || '当前账号' }}</div>
        </div>
        <el-form :inline="true" :model="transferFilters" class="toolbar-form transfer-form">
          <el-form-item label="学号/姓名"><el-input v-model="transferFilters.keyword" clearable placeholder="搜索本实验室学生" @keyup.enter="handleTransferSearch" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="transferSearchLoading" @click="handleTransferSearch">搜索</el-button></el-form-item>
        </el-form>
        <el-table v-loading="transferSearchLoading" :data="transferCandidates" highlight-current-row height="320px" @current-change="handleStudentCurrentChange" @row-click="handleStudentCurrentChange">
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="major" label="专业" min-width="160" />
          <el-table-column prop="grade" label="年级" min-width="100" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="assigningAdmin" :disabled="!selectedAdminStudent" @click="submitTransferAdmin">确认移交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="reviewDrawerVisible" title="实验室资料变更详情" size="760px">
      <template v-if="reviewDetail">
        <el-descriptions :column="2" border class="detail-card">
          <el-descriptions-item label="当前实验室">{{ reviewDetail.labName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请后名称">{{ reviewDetail.proposedLabName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ reviewDetail.collegeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ reviewDetail.applicantName || reviewDetail.applicantUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="reviewStatusType(reviewDetail.reviewStatus)">{{ reviewStatusText(reviewDetail.reviewStatus) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(reviewDetail.submitTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="reviewComparisonRows" stripe class="compare-table" :row-class-name="resolveCompareRowClass">
          <el-table-column prop="label" label="字段" min-width="140" />
          <el-table-column prop="currentValue" label="当前生效值" min-width="240" show-overflow-tooltip />
          <el-table-column prop="proposedValue" label="申请变更值" min-width="240" show-overflow-tooltip />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import ImageUploadField from '@/components/common/ImageUploadField.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { assignAdminToLab, getLabsWithAdmin, removeAdminFromLab } from '@/api/adminManagement'
import { getCollegeOptions } from '@/api/colleges'
import { approveLabInfoReview, getLabInfoReviewHistory, getPendingLabInfoReviewPage, rejectLabInfoReview } from '@/api/labInfoReview'
import { createLab, deleteLab, getLabById, getLabPage, updateLab, updateManagedLab } from '@/api/lab'
import { useUserStore } from '@/stores/user'
import { clearAuth } from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const reviewLoading = ref(false)
const reviewHistoryLoading = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const reviewDrawerVisible = ref(false)
const adminSelectionLoading = ref(false)
const transferSearchLoading = ref(false)
const assigningAdmin = ref(false)
const labs = ref([])
const colleges = ref([])
const assignCandidates = ref([])
const transferCandidates = ref([])
const labAdminMap = ref({})
const currentAdminLab = ref(null)
const selectedAdminStudent = ref(null)
const pendingReviews = ref([])
const reviewDetail = ref(null)
const currentChangeHistory = ref([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const reviewPagination = reactive({ pageNum: 1, pageSize: 8, total: 0 })
const filters = reactive({ labName: '', status: '' })
const reviewFilters = reactive({ keyword: '' })
const assignFilters = reactive({ keyword: '' })
const transferFilters = reactive({ keyword: '' })
const form = reactive({
  id: null,
  labName: '',
  labCode: '',
  collegeId: undefined,
  teacherName: '',
  location: '',
  contactEmail: '',
  requireSkill: '',
  recruitNum: 20,
  status: 1,
  labDesc: '',
  basicInfo: '',
  awards: '',
  logoUrl: '',
  coverImageUrl: ''
})
const rules = {
  labName: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  labCode: [{ required: true, message: '请输入实验室编码', trigger: 'blur' }],
  recruitNum: [{ required: true, message: '请输入计划容量', trigger: 'change' }]
}
const reviewFieldDefs = [
  { key: 'labName', label: '实验室名称' },
  { key: 'labCode', label: '实验室编码' },
  { key: 'teacherName', label: '指导教师' },
  { key: 'location', label: '地点' },
  { key: 'contactEmail', label: '联系邮箱' },
  { key: 'recruitNum', label: '计划容量' },
  { key: 'status', label: '状态' },
  { key: 'requireSkill', label: '招新要求' },
  { key: 'labDesc', label: '实验室简介' },
  { key: 'basicInfo', label: '基础信息' },
  { key: 'awards', label: '获奖成果' },
  { key: 'logoUrl', label: '实验室 Logo' },
  { key: 'coverImageUrl', label: '实验室封面图' }
]
const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isReviewAdmin = computed(() => isSuperAdmin.value || isCollegeManager.value)
const managedLabId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)
const canTransferAdmin = computed(() => !isSuperAdmin.value && Boolean(userStore.userInfo?.labManager && managedLabId.value))
const dialogTitle = computed(() => {
  if (!form.id) return '新增实验室'
  return isSuperAdmin.value ? '编辑实验室' : '提交实验室资料变更'
})
const saveButtonLabel = computed(() => (isSuperAdmin.value ? '保存' : '提交变更申请'))
const latestManagedReview = computed(() => currentChangeHistory.value[0] || null)
const hasPendingManagedReview = computed(() => !isSuperAdmin.value && latestManagedReview.value?.reviewStatus === 'PENDING')
const filteredAssignCandidates = computed(() => {
  const keyword = assignFilters.keyword.trim().toLowerCase()
  if (!keyword) return assignCandidates.value
  return assignCandidates.value.filter((item) =>
    [item.studentId, item.realName, item.username, item.major].some((value) => String(value || '').toLowerCase().includes(keyword))
  )
})
const reviewComparisonRows = computed(() => {
  const snapshot = reviewDetail.value?.snapshot || {}
  const official = reviewDetail.value?.official || {}
  return reviewFieldDefs.map((item) => {
    const currentValue = formatFieldValue(item.key, official[item.key])
    const proposedValue = formatFieldValue(item.key, snapshot[item.key])
    const changed = String(official[item.key] ?? '') !== String(snapshot[item.key] ?? '')
    return { ...item, currentValue, proposedValue, changed }
  })
})

const reviewStatusText = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'PENDING') return '待审核'
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  return value || '未知'
}
const reviewStatusType = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'PENDING') return 'warning'
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'info'
}
const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
const resolveMedia = (value) => resolveFileUrl(value)
const formatFieldValue = (key, value) => {
  if (value === null || value === undefined || value === '') return '-'
  if (key === 'status') return Number(value) === 1 ? '开放' : '关闭'
  if (key === 'logoUrl' || key === 'coverImageUrl') return value ? '已上传' : '-'
  return String(value)
}
const buildLatestReviewDescription = (review) => {
  if (!review) return ''
  const reviewTimeText = review.reviewTime ? `审核时间：${formatDateTime(review.reviewTime)}。` : ''
  const reviewCommentText = review.reviewComment ? `审核意见：${review.reviewComment}` : '暂无审核意见。'
  return `${reviewTimeText}${reviewCommentText}`
}
const resolveCompareRowClass = ({ row }) => (row.changed ? 'changed-row' : '')
const resetForm = () => {
  Object.assign(form, {
    id: null, labName: '', labCode: '', collegeId: undefined, teacherName: '', location: '', contactEmail: '',
    requireSkill: '', recruitNum: 20, status: 1, labDesc: '', basicInfo: '', awards: '', logoUrl: '', coverImageUrl: ''
  })
}
const fillForm = (data = {}) => {
  Object.assign(form, {
    id: data.id ?? null,
    labName: data.labName || '', labCode: data.labCode || '', collegeId: data.collegeId,
    teacherName: data.teacherName || '', location: data.location || '', contactEmail: data.contactEmail || '',
    requireSkill: data.requireSkill || '', recruitNum: data.recruitNum ?? 20, status: data.status ?? 1,
    labDesc: data.labDesc || '', basicInfo: data.basicInfo || '', awards: data.awards || '',
    logoUrl: data.logoUrl || '', coverImageUrl: data.coverImageUrl || ''
  })
}
const loadColleges = async () => {
  const response = await getCollegeOptions()
  colleges.value = response.data || []
}
const buildCurrentAdminMap = (items = []) => items.reduce((map, item) => {
  if (item?.lab?.id) map[item.lab.id] = item.admin || null
  return map
}, {})
const decorateLabRows = (items = []) => items.map((item) => ({ ...item, currentAdmin: labAdminMap.value[item.id] || null }))
const buildSelfAdminProfile = () => {
  if (!userStore.userInfo?.labManager) return null
  return {
    id: userStore.userInfo.id,
    realName: userStore.userInfo.realName,
    username: userStore.userInfo.username,
    studentId: userStore.userInfo.studentId,
    email: userStore.userInfo.email
  }
}
const loadLabs = async () => {
  loading.value = true
  try {
    if (isSuperAdmin.value) {
      const [response, adminResponse] = await Promise.all([
        getLabPage({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, labName: filters.labName || undefined, status: filters.status === '' ? undefined : filters.status }),
        getLabsWithAdmin()
      ])
      labAdminMap.value = buildCurrentAdminMap(adminResponse.data || [])
      labs.value = decorateLabRows(response.data.records || [])
      pagination.total = response.data.total || 0
      return
    }
    if (isCollegeManager.value) {
      const managedCollegeId = userStore.userInfo?.managedCollegeId
      if (!managedCollegeId) { labs.value = []; pagination.total = 0; return }
      const [response, adminResponse] = await Promise.all([
        getLabPage({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, collegeId: managedCollegeId, labName: filters.labName || undefined, status: filters.status === '' ? undefined : filters.status }),
        getLabsWithAdmin()
      ])
      labAdminMap.value = buildCurrentAdminMap(adminResponse.data || [])
      labs.value = decorateLabRows(response.data.records || [])
      pagination.total = response.data.total || 0
      return
    }
    if (!managedLabId.value) { labs.value = []; pagination.total = 0; return }
    const response = await getLabById(managedLabId.value)
    labs.value = response.data ? [{ ...response.data, currentAdmin: buildSelfAdminProfile() }] : []
    pagination.total = labs.value.length
  } finally {
    loading.value = false
  }
}
const loadPendingReviews = async () => {
  if (!isReviewAdmin.value) return
  reviewLoading.value = true
  try {
    const response = await getPendingLabInfoReviewPage({
      pageNum: reviewPagination.pageNum,
      pageSize: reviewPagination.pageSize,
      keyword: reviewFilters.keyword || undefined,
      collegeId: isCollegeManager.value ? userStore.userInfo?.managedCollegeId : undefined
    })
    pendingReviews.value = response.data.records || []
    reviewPagination.total = response.data.total || 0
  } finally {
    reviewLoading.value = false
  }
}
const loadCurrentLabChangeHistory = async (labId) => {
  if (!labId || isSuperAdmin.value) { currentChangeHistory.value = []; return }
  reviewHistoryLoading.value = true
  try {
    const response = await getLabInfoReviewHistory({ labId })
    currentChangeHistory.value = response.data || []
  } finally {
    reviewHistoryLoading.value = false
  }
}
const refreshPage = async () => { await Promise.all([loadLabs(), loadPendingReviews()]) }
const openDialog = async (row) => {
  resetForm()
  currentChangeHistory.value = []
  if (row) {
    fillForm(row)
  } else if (!isSuperAdmin.value && managedLabId.value) {
    form.id = managedLabId.value
    const response = await getLabById(managedLabId.value)
    if (response.data) fillForm(response.data)
  }
  dialogVisible.value = true
  if (!isSuperAdmin.value && form.id) await loadCurrentLabChangeHistory(form.id)
}
const openReviewDetail = (row) => { reviewDetail.value = row || null; reviewDrawerVisible.value = true }
const handleReviewAction = async (action, row) => {
  const actionText = action === 'approve' ? '通过' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(`请输入${actionText}意见`, `${actionText}变更申请`, {
      inputType: 'textarea', inputPattern: /[\s\S]{2,}/, inputErrorMessage: '审核意见至少输入 2 个字符', confirmButtonText: actionText, cancelButtonText: '取消'
    })
    const payload = { reviewComment: value }
    if (action === 'approve') { await approveLabInfoReview(row.reviewId, payload); ElMessage.success('资料变更已通过') }
    else { await rejectLabInfoReview(row.reviewId, payload); ElMessage.success('资料变更已驳回') }
    if (reviewDrawerVisible.value && reviewDetail.value?.reviewId === row.reviewId) { reviewDrawerVisible.value = false; reviewDetail.value = null }
    await Promise.all([loadLabs(), loadPendingReviews()])
  } catch (error) {
    if (error !== 'cancel' && error?.message) ElMessage.error(error.message)
  }
}
const loadAssignCandidates = async () => {
  adminSelectionLoading.value = true
  try {
    const labId = currentAdminLab.value?.id
    if (!labId) { assignCandidates.value = []; return }
    const response = await request.get('/api/user/list', { params: { pageNum: 1, pageSize: 200, role: 'student', labId } })
    assignCandidates.value = response.data.records || []
  } finally { adminSelectionLoading.value = false }
}
const handleStudentCurrentChange = (row) => { selectedAdminStudent.value = row || null }
const openAssignAdminDialog = async (row) => {
  currentAdminLab.value = row; selectedAdminStudent.value = null; assignFilters.keyword = ''; assignDialogVisible.value = true; await loadAssignCandidates()
}
const submitAssignAdmin = async () => {
  if (!currentAdminLab.value?.id || !selectedAdminStudent.value?.id) { ElMessage.warning('请选择要安排的学生管理员'); return }
  assigningAdmin.value = true
  try {
    await assignAdminToLab(currentAdminLab.value.id, selectedAdminStudent.value.id)
    ElMessage.success('学生管理员安排成功')
    assignDialogVisible.value = false
    await loadLabs()
  } catch (error) { ElMessage.error(error.message || '学生管理员安排失败') }
  finally { assigningAdmin.value = false }
}
const openTransferDialog = (row) => {
  currentAdminLab.value = row; selectedAdminStudent.value = null; transferFilters.keyword = ''; transferCandidates.value = []; transferDialogVisible.value = true
}
const handleTransferSearch = async () => {
  const keyword = transferFilters.keyword.trim()
  if (!keyword) { ElMessage.warning('请输入搜索关键词'); return }
  transferSearchLoading.value = true
  try {
    const labId = currentAdminLab.value?.id
    if (!labId) { transferCandidates.value = []; return }
    const response = await request.get('/api/user/list', { params: { pageNum: 1, pageSize: 20, role: 'student', keyword, labId } })
    transferCandidates.value = response.data.records || []
  } catch (error) { ElMessage.error(error.message || '搜索实验室学生失败') }
  finally { transferSearchLoading.value = false }
}
const handleRevokeAdmin = async (row) => {
  await ElMessageBox.confirm(`确认撤销实验室“${row.labName}”的管理员权限吗？`, '撤销确认', { type: 'warning' })
  try { await removeAdminFromLab(row.id); ElMessage.success('管理员已撤销'); await loadLabs() }
  catch (error) { ElMessage.error(error.message || '撤销管理员失败') }
}
const submitTransferAdmin = async () => {
  if (!currentAdminLab.value?.id || !selectedAdminStudent.value?.id) { ElMessage.warning('请选择要接收权限的学生'); return }
  const targetStudent = selectedAdminStudent.value
  await ElMessageBox.confirm(`确认将 ${currentAdminLab.value.labName} 的管理员权限移交给 ${targetStudent.realName}（${targetStudent.studentId || targetStudent.username}）吗？`, '移交确认', { type: 'warning', confirmButtonText: '确认移交', cancelButtonText: '取消' })
  assigningAdmin.value = true
  try {
    await assignAdminToLab(currentAdminLab.value.id, targetStudent.id)
    ElMessage.success('管理员权限移交成功，即将退出登录')
    transferDialogVisible.value = false
    setTimeout(() => { clearAuth(); router.push('/login') }, 1200)
  } catch (error) { ElMessage.error(error.message || '管理员权限移交失败') }
  finally { assigningAdmin.value = false }
}
const handleSave = async () => {
  await formRef.value.validate()
  if (!isSuperAdmin.value && hasPendingManagedReview.value) { ElMessage.warning('当前已有待审核变更，请等待学校管理员处理后再提交'); return }
  saving.value = true
  try {
    const payload = { ...form }
    if (isSuperAdmin.value) {
      if (form.id) await updateLab(form.id, payload)
      else await createLab(payload)
      ElMessage.success('实验室信息已保存')
    } else {
      await updateManagedLab(payload)
      ElMessage.success('资料变更申请已提交，等待学校管理员审核')
    }
    dialogVisible.value = false
    await Promise.all([loadLabs(), loadPendingReviews()])
  } finally { saving.value = false }
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除实验室“${row.labName}”吗？`, '删除确认', { type: 'warning' })
  await deleteLab(row.id)
  ElMessage.success('实验室已删除')
  await loadLabs()
}
const handlePageChange = (page) => { pagination.pageNum = page; loadLabs() }
const handleReviewPageChange = (page) => { reviewPagination.pageNum = page; loadPendingReviews() }

onMounted(async () => {
  await Promise.all([loadColleges(), loadLabs(), loadPendingReviews()])
})
</script>

<style scoped>
.admin-arrangement-tip,
.review-card { margin-bottom: 16px; }
.admin-meta { font-size: 12px; color: #909399; }
.empty-text,
.action-tip,
.history-empty { color: #909399; }
.dialog-section { display: flex; flex-direction: column; gap: 16px; }
.dialog-lab-card { padding: 16px; border-radius: 12px; background: #f5f7fa; }
.dialog-lab-title { font-size: 16px; font-weight: 600; color: #303133; }
.dialog-lab-subtitle { margin-top: 8px; color: #606266; }
.transfer-form { margin-bottom: 0; }
.lab-cell { display: flex; align-items: center; gap: 12px; }
.lab-cell__logo { width: 44px; height: 44px; border-radius: 14px; overflow: hidden; background: linear-gradient(135deg, #0f172a, #0f766e); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.lab-cell__logo img { width: 100%; height: 100%; object-fit: cover; }
.lab-cell__copy { display: grid; gap: 4px; }
.lab-cell__copy strong { color: #0f172a; }
.lab-cell__copy small { color: #64748b; }
.two-column-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.media-upload-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-bottom: 18px; }
.toolbar-actions.compact { display: flex; flex-wrap: wrap; gap: 10px; }
.history-block { border-top: 1px solid #ebeef5; padding-top: 16px; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.timeline-title { font-weight: 600; color: #303133; }
.timeline-text { margin-top: 6px; color: #606266; }
.detail-card,
.compare-table { margin-bottom: 16px; }
:deep(.changed-row) { --el-table-tr-bg-color: #fff7e6; }
@media (max-width: 768px) { .two-column-form, .media-upload-grid { grid-template-columns: 1fr; } }
</style>

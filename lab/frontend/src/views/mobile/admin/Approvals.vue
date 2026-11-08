<template>
  <div class="m-page">
    <section class="toolbar">
      <el-select v-model="typeFilter" class="type-select" @change="reload">
        <el-option label="全部审批" value="all" />
        <el-option label="成员申请" value="member" />
        <el-option label="实验室创建" value="create" />
        <el-option label="教师注册" value="teacher" />
      </el-select>
      <el-button plain :loading="loading" @click="reload">刷新</el-button>
    </section>

    <section v-loading="loading" class="list">
      <article v-for="item in items" :key="`${item.type}-${item.id}`" class="card">
        <div class="card-head">
          <div class="title">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subtitle }}</span>
          </div>
          <el-tag :type="statusTagType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
        </div>

        <div class="meta">
          <span>{{ item.typeLabel }}</span>
          <span>提交时间：{{ formatDateTime(item.createTime) }}</span>
        </div>

        <div v-if="item.detail" class="section">
          <label>申请说明</label>
          <p>{{ item.detail }}</p>
        </div>

        <div v-if="item.auditComment" class="section">
          <label>审核意见</label>
          <p>{{ item.auditComment }}</p>
        </div>

        <div v-if="item.extra" class="section">
          <label>{{ item.extraLabel }}</label>
          <p>{{ item.extra }}</p>
        </div>

        <div v-if="canAudit(item)" class="card-actions">
          <el-button
            v-for="action in actionsOf(item)"
            :key="action.value"
            :type="action.type"
            size="small"
            plain
            @click="handleAudit(item, action)"
          >
            {{ action.label }}
          </el-button>
        </div>
      </article>

      <el-empty v-if="!loading && items.length === 0" description="暂无待处理事项" :image-size="80" />
    </section>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { getLabApplyPage, auditLabApply } from '@/api/labApplies'
import { getLabCreateApplyPage, auditLabCreateApply } from '@/api/labCreateApplies'
import {
  getTeacherRegisterApplyPage,
  auditTeacherRegisterApply
} from '@/api/teacherRegisterApplies'

const loading = ref(false)
const typeFilter = ref('all')
const memberRows = ref([])
const createRows = ref([])
const teacherRows = ref([])

const TYPE_LABEL = {
  member: '成员申请',
  create: '实验室创建',
  teacher: '教师注册'
}

const STATUS_MAP = {
  submitted: '待审核',
  leader_approved: '初审通过',
  college_approved: '学院通过',
  approved: '已通过',
  rejected: '已驳回'
}

const STATUS_TAG = {
  submitted: 'warning',
  leader_approved: 'primary',
  college_approved: 'primary',
  approved: 'success',
  rejected: 'danger'
}

const statusLabel = (status) => STATUS_MAP[status] || status || '-'
const statusTagType = (status) => STATUS_TAG[status] || 'info'

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const memberItems = computed(() =>
  memberRows.value.map((row) => ({
    type: 'member',
    id: row.id,
    typeLabel: TYPE_LABEL.member,
    title: row.studentName || row.studentId || '学生申请',
    subtitle: row.labName || '未命名实验室',
    status: row.status,
    createTime: row.createTime,
    detail: row.applyReason,
    auditComment: row.auditComment,
    extra: row.planTitle,
    extraLabel: '招新计划'
  }))
)

const createItems = computed(() =>
  createRows.value.map((row) => ({
    type: 'create',
    id: row.id,
    typeLabel: TYPE_LABEL.create,
    title: row.labName || '实验室创建申请',
    subtitle: row.collegeName || '-',
    status: row.status,
    createTime: row.createTime,
    detail: row.applyReason,
    auditComment: row.collegeAuditComment || row.schoolAuditComment,
    extra: row.researchDirection,
    extraLabel: '研究方向'
  }))
)

const teacherItems = computed(() =>
  teacherRows.value.map((row) => ({
    type: 'teacher',
    id: row.id,
    typeLabel: TYPE_LABEL.teacher,
    title: row.realName || row.teacherNo || '教师申请',
    subtitle: `${row.teacherNo || '-'} · ${row.collegeName || '-'}`,
    status: row.status,
    createTime: row.createTime,
    detail: row.applyReason,
    auditComment: row.collegeAuditComment,
    extra: row.email,
    extraLabel: '联系方式'
  }))
)

const items = computed(() => {
  const all = [...memberItems.value, ...createItems.value, ...teacherItems.value]
  if (typeFilter.value === 'all') {
    return all
  }
  return all.filter((item) => item.type === typeFilter.value)
})

/** 不同审批链的可用动作不同：成员申请需两级审核，其余按当前所处阶段给出动作。 */
const actionsOf = (item) => {
  if (item.type === 'member') {
    if (item.status === 'submitted') {
      return [{ value: 'leaderApprove', label: '初审通过', type: 'primary' }]
    }
    if (item.status === 'leader_approved') {
      return [
        { value: 'approve', label: '终审通过', type: 'success' },
        { value: 'reject', label: '驳回', type: 'danger' }
      ]
    }
    return []
  }

  if (item.status === 'submitted') {
    return [{ value: 'collegeApprove', label: '学院通过', type: 'primary' }]
  }
  if (item.status === 'college_approved') {
    return [
      { value: 'schoolApprove', label: '学校通过', type: 'success' },
      { value: 'reject', label: '驳回', type: 'danger' }
    ]
  }
  return []
}

const canAudit = (item) => actionsOf(item).length > 0

const auditApiOf = (item) => {
  if (item.type === 'member') return auditLabApply
  if (item.type === 'create') return auditLabCreateApply
  return auditTeacherRegisterApply
}

const handleAudit = async (item, action) => {
  const result = await ElMessageBox.prompt(`请输入${action.label}备注`, '审核确认', {
    inputPlaceholder: '可选，留空也可直接提交',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  }).catch(() => null)

  if (!result) {
    return
  }

  await auditApiOf(item)(item.id, {
    action: action.value,
    auditComment: result.value
  })
  ElMessage.success('处理完成')
  await reload()
}

const loadAll = async () => {
  const [memberRes, createRes, teacherRes] = await Promise.all([
    getLabApplyPage({ pageNum: 1, pageSize: 50 }),
    getLabCreateApplyPage({ pageNum: 1, pageSize: 50 }),
    getTeacherRegisterApplyPage({ pageNum: 1, pageSize: 50 })
  ])
  memberRows.value = memberRes.data?.records || []
  createRows.value = createRes.data?.records || []
  teacherRows.value = teacherRes.data?.records || []
}

const reload = async () => {
  loading.value = true
  try {
    await loadAll()
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.type-select {
  flex: 1;
  min-width: 0;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ll-space-2);
  margin-top: var(--ll-space-1);
}

.card-actions :deep(.el-button) {
  margin-left: 0;
}
</style>

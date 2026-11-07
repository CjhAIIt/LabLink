<template>
  <div class="m-page">
    <section class="toolbar">
      <el-input v-model="filters.keyword" placeholder="搜索实验室名称 / 学院" clearable @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="openDialog">发起</el-button>
    </section>

    <section class="toolbar">
      <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 100%" @change="resetAndFetch">
        <el-option label="待学院审核" value="submitted" />
        <el-option label="待学校审核" value="college_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section v-loading="loading" class="list">
      <article v-for="row in records" :key="row.id" class="card">
        <div class="card-head">
          <div class="title">
            <strong>{{ row.labName || '实验室创建申请' }}</strong>
            <span>{{ row.collegeName || '-' }}</span>
          </div>
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </div>
        <div class="meta">
          <span>指导教师：{{ row.teacherName || '-' }}</span>
          <span>申请时间：{{ formatDateTime(row.createTime) }}</span>
        </div>
        <div class="section">
          <label>研究方向</label>
          <p>{{ row.researchDirection || '-' }}</p>
        </div>
        <div class="section">
          <label>学院审核</label>
          <p>{{ row.collegeAuditComment || '待处理' }}</p>
        </div>
        <div class="section">
          <label>学校审核</label>
          <p>{{ row.schoolAuditComment || '待处理' }}</p>
        </div>
      </article>

      <el-empty v-if="!loading && records.length === 0" description="暂无创建申请" :image-size="80" />

      <div class="load-more">
        <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
        <span v-else-if="records.length" class="no-more">已到底</span>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="发起实验室创建申请" width="92%">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="所属学院">
          <el-select v-model="form.collegeId" placeholder="请选择学院" :disabled="Boolean(lockedCollegeId)">
            <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室名称">
          <el-input v-model="form.labName" placeholder="请输入实验室名称" />
        </el-form-item>
        <el-form-item label="指导教师">
          <el-input v-model="form.teacherName" placeholder="请输入指导教师" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="例如：A楼 3-302" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="用于接收通知（可选）" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input v-model="form.researchDirection" type="textarea" :rows="3" placeholder="简要描述研究方向" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" placeholder="为什么需要创建该实验室" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { getCollegeOptions } from '@/api/colleges'
import { createLabCreateApply, getLabCreateApplyPage } from '@/api/labCreateApplies'
import { useUserStore } from '@/stores/user'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()

const loading = ref(false)
const loadingMore = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

const pageNum = ref(1)
const pageSize = 8
const total = ref(0)
const records = ref([])

const colleges = ref([])
const formRef = ref()

const filters = reactive({
  keyword: '',
  status: ''
})

const form = reactive({
  collegeId: undefined,
  labName: '',
  teacherName: '',
  location: '',
  contactEmail: '',
  researchDirection: '',
  applyReason: ''
})

const lockedCollegeId = computed(() => {
  const currentCollegeName = userStore.userInfo?.college
  if (!currentCollegeName) {
    return null
  }
  return colleges.value.find((item) => item.collegeName === currentCollegeName)?.id || null
})

const hasMore = computed(() => records.value.length < (total.value || 0))

const statusLabel = (value) => {
  const map = {
    submitted: '待学院审核',
    college_approved: '待学校审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[value] || value || '-'
}

const statusTagType = (value) => {
  const map = {
    submitted: 'warning',
    college_approved: 'primary',
    approved: 'success',
    rejected: 'danger'
  }
  return map[value] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const fetchPage = async (page) => {
  const res = await getLabCreateApplyPage({
    pageNum: page,
    pageSize,
    keyword: filters.keyword?.trim() || undefined,
    status: filters.status || undefined
  })
  const pageData = res.data || {}
  total.value = pageData.total || 0
  return pageData.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    records.value = await fetchPage(1)
  } finally {
    loading.value = false
  }
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const next = pageNum.value + 1
    const list = await fetchPage(next)
    pageNum.value = next
    records.value = records.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    collegeId: lockedCollegeId.value || undefined,
    labName: '',
    teacherName: userStore.realName || '',
    location: '',
    contactEmail: userStore.userInfo?.email || '',
    researchDirection: '',
    applyReason: ''
  })
}

const openDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const validateForm = () => {
  if (!form.collegeId) {
    ElMessage.warning('请选择所属学院')
    return false
  }
  if (!form.labName.trim()) {
    ElMessage.warning('请输入实验室名称')
    return false
  }
  if (!form.teacherName.trim()) {
    ElMessage.warning('请输入指导教师')
    return false
  }
  if (!form.researchDirection.trim()) {
    ElMessage.warning('请输入研究方向')
    return false
  }
  if (!form.applyReason.trim()) {
    ElMessage.warning('请输入申请说明')
    return false
  }
  return true
}

const submitApply = async () => {
  if (!validateForm()) return
  submitting.value = true
  try {
    await createLabCreateApply({ ...form })
    ElMessage.success('实验室创建申请已提交')
    dialogVisible.value = false
    resetForm()
    await resetAndFetch()
  } finally {
    submitting.value = false
  }
}

watch(
  () => lockedCollegeId.value,
  (value) => {
    if (value && !form.collegeId) {
      form.collegeId = value
    }
  }
)

onMounted(async () => {
  const res = await getCollegeOptions()
  colleges.value = res.data || []
  await resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.title {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.title strong {
  color: #0f172a;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.title span {
  color: #64748b;
  font-size: 12px;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
}

.section {
  display: grid;
  gap: 6px;
}

.section label {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.section p {
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.load-more {
  padding: 10px 0 2px 0;
  display: flex;
  justify-content: center;
}

.no-more {
  font-size: 12px;
  color: #94a3b8;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>

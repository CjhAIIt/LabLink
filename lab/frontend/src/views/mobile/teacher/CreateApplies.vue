<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="filters.keyword" clearable placeholder="搜索实验室名称或学院" @clear="resetAndFetch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="openDialog">发起申请</el-button>
    </section>

    <section class="toolbar-card compact">
      <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 100%" @change="resetAndFetch">
        <el-option label="待学院审核" value="submitted" />
        <el-option label="待学校审核" value="college_approved" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
    </section>

    <section class="card-list">
      <article v-for="row in records" :key="row.id" class="record-card">
        <div class="card-head">
          <div>
            <strong>{{ row.labName || '实验室创建申请' }}</strong>
            <p>{{ row.collegeName || '-' }}</p>
          </div>
          <span class="status-pill" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
        </div>
        <div class="info-block">
          <label>指导教师</label>
          <div>{{ row.teacherName || '-' }}</div>
        </div>
        <div class="info-block">
          <label>研究方向</label>
          <div>{{ row.researchDirection || '-' }}</div>
        </div>
        <div class="info-block">
          <label>学院审核</label>
          <div>{{ row.collegeAuditComment || '待处理' }}</div>
        </div>
        <div class="info-block">
          <label>学校审核</label>
          <div>{{ row.schoolAuditComment || '待处理' }}</div>
        </div>
      </article>

      <el-empty v-if="!loading && records.length === 0" description="暂无创建申请" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="发起实验室创建申请" width="92%">
      <el-form :model="form" label-position="top">
        <el-form-item label="所属学院">
          <el-select v-model="form.collegeId" placeholder="请选择学院" style="width: 100%" :disabled="Boolean(lockedCollegeId)">
            <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室名称">
          <el-input v-model="form.labName" placeholder="请输入实验室名称" />
        </el-form-item>
        <el-form-item label="指导教师">
          <el-input v-model="form.teacherName" placeholder="请输入指导教师" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="例如：A楼 3-302" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="用于接收审核通知" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input v-model="form.researchDirection" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" />
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
import { Search } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCollegeOptions } from '@/api/colleges'
import { createLabCreateApply, getLabCreateApplyPage } from '@/api/labCreateApplies'
import { useUserStore } from '@/stores/user'

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
  const collegeName = userStore.userInfo?.collegeName || userStore.userInfo?.college
  if (!collegeName) {
    return null
  }
  return colleges.value.find((item) => item.collegeName === collegeName)?.id || null
})

const hasMore = computed(() => records.value.length < total.value)

const fetchPage = async (page) => {
  const response = await getLabCreateApplyPage({
    pageNum: page,
    pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined
  })
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
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
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchPage(nextPage)
    pageNum.value = nextPage
    records.value = records.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const resetForm = () => {
  form.collegeId = lockedCollegeId.value || undefined
  form.labName = ''
  form.teacherName = userStore.realName || ''
  form.location = ''
  form.contactEmail = userStore.userInfo?.email || ''
  form.researchDirection = ''
  form.applyReason = ''
}

const openDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const submitApply = async () => {
  if (!form.collegeId || !form.labName.trim() || !form.teacherName.trim() || !form.researchDirection.trim() || !form.applyReason.trim()) {
    ElMessage.warning('请完整填写申请信息')
    return
  }
  submitting.value = true
  try {
    await createLabCreateApply({ ...form })
    ElMessage.success('创建申请已提交')
    dialogVisible.value = false
    await resetAndFetch()
  } finally {
    submitting.value = false
  }
}

const statusLabel = (value) => ({
  submitted: '待学院审核',
  college_approved: '待学校审核',
  approved: '已通过',
  rejected: '已驳回'
}[value] || value || '-')

const statusClass = (value) => ({
  submitted: 'pending',
  college_approved: 'progress',
  approved: 'success',
  rejected: 'danger'
}[value] || 'default')

onMounted(async () => {
  const response = await getCollegeOptions()
  colleges.value = response.data || []
  await resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.record-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.toolbar-card.compact {
  grid-template-columns: minmax(0, 1fr) auto;
}

.card-list {
  display: grid;
  gap: 10px;
}

.record-card {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head strong,
.info-block label {
  color: #0f172a;
}

.card-head p,
.info-block div {
  color: #64748b;
}

.card-head p {
  margin: 6px 0 0;
}

.info-block {
  display: grid;
  gap: 6px;
}

.info-block label {
  font-weight: 700;
  font-size: 13px;
}

.info-block div {
  line-height: 1.7;
  white-space: pre-wrap;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.pending {
  color: #b45309;
  background: rgba(254, 243, 199, 0.92);
}

.status-pill.progress {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.92);
}

.status-pill.success {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.danger {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.92);
}

.load-more {
  display: flex;
  justify-content: center;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

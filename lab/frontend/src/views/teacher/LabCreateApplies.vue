<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室创建申请</p>
          <h2>在线发起创建申请并跟踪学院、学校两级审批状态</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
          <el-button type="primary" @click="openDialog">发起申请</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="实验室名称 / 指导教师 / 学院" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 180px">
            <el-option label="待学院审核" value="submitted" />
            <el-option label="待学校审核" value="college_approved" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="collegeName" label="所属学院" min-width="150" />
        <el-table-column prop="labName" label="实验室名称" min-width="180" />
        <el-table-column prop="teacherName" label="指导教师" min-width="120" />
        <el-table-column prop="researchDirection" label="研究方向" min-width="220" show-overflow-tooltip />
        <el-table-column label="流程状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学院审核" min-width="220">
          <template #default="{ row }">
            <div class="audit-cell">
              <span>{{ row.collegeAuditComment || '待处理' }}</span>
              <small>{{ formatDateTime(row.collegeAuditTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学校审核" min-width="220">
          <template #default="{ row }">
            <div class="audit-cell">
              <span>{{ row.schoolAuditComment || '待处理' }}</span>
              <small>{{ formatDateTime(row.schoolAuditTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
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

    <el-dialog v-model="dialogVisible" title="发起实验室创建申请" width="760px">
      <el-form label-width="96px" class="dialog-form">
        <div class="two-column-form">
          <el-form-item label="所属学院">
            <el-select
              v-model="form.collegeId"
              placeholder="请选择学院"
              :disabled="Boolean(lockedCollegeId)"
            >
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验室名称">
            <el-input v-model="form.labName" />
          </el-form-item>
          <el-form-item label="指导教师">
            <el-input v-model="form.teacherName" />
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" />
          </el-form-item>
          <el-form-item label="联系邮箱">
            <el-input v-model="form.contactEmail" />
          </el-form-item>
        </div>

        <el-form-item label="研究方向">
          <el-input v-model="form.researchDirection" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
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

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const colleges = ref([])
const dialogVisible = ref(false)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

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

watch(lockedCollegeId, (value) => {
  if (value && !form.collegeId) {
    form.collegeId = value
  }
})

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

const loadPageData = async () => {
  loading.value = true
  try {
    const [applyRes, collegeRes] = await Promise.all([
      getLabCreateApplyPage({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        keyword: filters.keyword || undefined,
        status: filters.status || undefined
      }),
      getCollegeOptions()
    ])

    records.value = applyRes.data?.records || []
    pagination.total = applyRes.data?.total || 0
    colleges.value = collegeRes.data || []

    if (lockedCollegeId.value && !form.collegeId) {
      form.collegeId = lockedCollegeId.value
    }
  } finally {
    loading.value = false
  }
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
  if (!validateForm()) {
    return
  }

  await createLabCreateApply({ ...form })
  ElMessage.success('实验室创建申请已提交')
  dialogVisible.value = false
  resetForm()
  await loadPageData()
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadPageData()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadPageData()
}

const statusLabel = (status) => {
  const map = {
    submitted: '待学院审核',
    college_approved: '待学校审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || status || '-'
}

const statusTagType = (status) => {
  const map = {
    submitted: 'warning',
    college_approved: 'primary',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  resetForm()
  loadPageData()
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

.audit-cell {
  display: grid;
  gap: 6px;
}

.audit-cell small {
  color: #64748b;
}

@media (max-width: 768px) {
  .two-column-form {
    grid-template-columns: 1fr;
  }
}
</style>

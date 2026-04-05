<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">招新计划</p>
          <h2>实验室发布与周期配置</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPlans">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增计划</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="标题 / 实验室名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="草稿" value="draft" />
            <el-option label="开放" value="open" />
            <el-option label="关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPlans">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="plans" stripe>
        <el-table-column prop="title" label="计划标题" min-width="180" />
        <el-table-column prop="labName" label="实验室" min-width="150" />
        <el-table-column prop="quota" label="名额" min-width="90" />
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="requirement" label="申请要求" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" min-width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑招新计划' : '新增招新计划'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="two-column-form">
          <el-form-item label="所属实验室" prop="labId">
            <el-select v-model="form.labId" :disabled="!isSuperAdmin" placeholder="请选择实验室">
              <el-option v-for="lab in labOptions" :key="lab.id" :label="lab.labName" :value="lab.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="计划标题" prop="title">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="招募名额" prop="quota">
            <el-input-number v-model="form.quota" :min="1" :max="500" />
          </el-form-item>
          <el-form-item label="计划状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="开放" value="open" />
              <el-option label="关闭" value="closed" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="申请要求" prop="requirement">
          <el-input v-model="form.requirement" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { getLabPage } from '@/api/lab'
import {
  createRecruitPlan,
  deleteRecruitPlan,
  getRecruitPlanPage,
  updateRecruitPlan
} from '@/api/recruitPlans'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const plans = ref([])
const labOptions = ref([])

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
  id: null,
  labId: undefined,
  title: '',
  startTime: '',
  endTime: '',
  quota: 10,
  requirement: '',
  status: 'draft'
})

const rules = {
  labId: [{ required: true, message: '请选择实验室', trigger: 'change' }],
  title: [{ required: true, message: '请输入计划标题', trigger: 'blur' }],
  quota: [{ required: true, message: '请输入招募名额', trigger: 'change' }]
}

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')

const resetForm = () => {
  Object.assign(form, {
    id: null,
    labId: userStore.userInfo?.labId || undefined,
    title: '',
    startTime: '',
    endTime: '',
    quota: 10,
    requirement: '',
    status: 'draft'
  })
}

const loadPlans = async () => {
  loading.value = true
  try {
    const response = await getRecruitPlanPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      labId: isSuperAdmin.value ? undefined : userStore.userInfo?.labId
    })
    plans.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadLabOptions = async () => {
  const response = await getLabPage({ pageNum: 1, pageSize: 100 })
  labOptions.value = response.data.records || []
}

const openDialog = (row) => {
  resetForm()
  if (row) {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateRecruitPlan(form.id, payload)
    } else {
      await createRecruitPlan(payload)
    }
    ElMessage.success('招新计划已保存')
    dialogVisible.value = false
    await loadPlans()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除计划“${row.title}”吗？`, '删除确认', { type: 'warning' })
  await deleteRecruitPlan(row.id)
  ElMessage.success('招新计划已删除')
  await loadPlans()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadPlans()
}

const statusLabel = (value) => ({ draft: '草稿', open: '开放', closed: '关闭' })[value] || value
const statusTagType = (value) => ({ draft: 'info', open: 'success', closed: 'warning' })[value] || 'info'
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(async () => {
  await Promise.all([loadLabOptions(), loadPlans()])
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

@media (max-width: 768px) {
  .two-column-form {
    grid-template-columns: 1fr;
  }
}
</style>

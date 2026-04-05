<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">公告管理</p>
          <h2>学校、学院、实验室公告发布</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadNotices">刷新</el-button>
          <el-button type="primary" @click="openDialog()">发布公告</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="公告标题 / 实验室 / 学院" />
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="filters.publishScope" clearable placeholder="全部范围" style="width: 140px">
            <el-option label="学校" value="school" />
            <el-option label="学院" value="college" />
            <el-option label="实验室" value="lab" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadNotices">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="notices" stripe>
        <el-table-column prop="title" label="公告标题" min-width="180" />
        <el-table-column prop="publishScope" label="范围" min-width="100">
          <template #default="{ row }">{{ scopeLabel(row.publishScope) }}</template>
        </el-table-column>
        <el-table-column prop="collegeName" label="学院" min-width="160" />
        <el-table-column prop="labName" label="实验室" min-width="160" />
        <el-table-column prop="publisherName" label="发布人" min-width="120" />
        <el-table-column prop="content" label="内容摘要" min-width="260" show-overflow-tooltip />
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="160">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <div class="two-column-form">
          <el-form-item label="发布范围" prop="publishScope">
            <el-select v-model="form.publishScope" :disabled="!isSuperAdmin">
              <el-option label="学校" value="school" />
              <el-option label="学院" value="college" />
              <el-option label="实验室" value="lab" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属学院" prop="collegeId">
            <el-select v-model="form.collegeId" clearable :disabled="!isSuperAdmin || form.publishScope !== 'college'">
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属实验室" prop="labId">
            <el-select v-model="form.labId" clearable :disabled="!isSuperAdmin || form.publishScope !== 'lab'">
              <el-option v-for="item in labs" :key="item.id" :label="item.labName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="已发布" :value="1" />
              <el-option label="草稿" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" />
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
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import { createNotice, deleteNotice, getNoticePage, updateNotice } from '@/api/notices'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const notices = ref([])
const colleges = ref([])
const labs = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  publishScope: ''
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  publishScope: 'lab',
  collegeId: undefined,
  labId: undefined,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')

const resetForm = () => {
  Object.assign(form, {
    id: null,
    title: '',
    content: '',
    publishScope: isSuperAdmin.value ? 'school' : 'lab',
    collegeId: undefined,
    labId: isSuperAdmin.value ? undefined : userStore.userInfo?.labId,
    status: 1
  })
}

const loadOptions = async () => {
  const [collegeRes, labRes] = await Promise.all([
    getCollegeOptions(),
    getLabPage({ pageNum: 1, pageSize: 100 })
  ])
  colleges.value = collegeRes.data || []
  labs.value = labRes.data.records || []
}

const loadNotices = async () => {
  loading.value = true
  try {
    const response = await getNoticePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      publishScope: filters.publishScope || undefined
    })
    notices.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
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
      await updateNotice(form.id, payload)
    } else {
      await createNotice(payload)
    }
    ElMessage.success('公告已保存')
    dialogVisible.value = false
    await loadNotices()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除公告“${row.title}”吗？`, '删除确认', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('公告已删除')
  await loadNotices()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadNotices()
}

const scopeLabel = (scope) => ({ school: '学校', college: '学院', lab: '实验室' })[scope] || scope
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(async () => {
  await Promise.all([loadOptions(), loadNotices()])
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

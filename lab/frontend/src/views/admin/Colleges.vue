<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">学院管理</p>
          <h2>学院信息与管理员归口</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadColleges">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增学院</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="学院名称 / 编码" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadColleges">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="colleges" stripe>
        <el-table-column prop="collegeCode" label="学院编码" min-width="140" />
        <el-table-column prop="collegeName" label="学院名称" min-width="180" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="180">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学院' : '新增学院'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学院编码" prop="collegeCode">
          <el-input v-model="form.collegeCode" />
        </el-form-item>
        <el-form-item label="学院名称" prop="collegeName">
          <el-input v-model="form.collegeName" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="4" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { createCollege, deleteCollege, getCollegePage, updateCollege } from '@/api/colleges'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const colleges = ref([])
const formRef = ref()

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null,
  collegeCode: '',
  collegeName: '',
  status: 1,
  remark: ''
})

const rules = {
  collegeCode: [{ required: true, message: '请输入学院编码', trigger: 'blur' }],
  collegeName: [{ required: true, message: '请输入学院名称', trigger: 'blur' }]
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    collegeCode: '',
    collegeName: '',
    status: 1,
    remark: ''
  })
}

const loadColleges = async () => {
  loading.value = true
  try {
    const response = await getCollegePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status === '' ? undefined : filters.status
    })
    colleges.value = response.data.records || []
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
    if (form.id) {
      await updateCollege(form.id, form)
    } else {
      await createCollege(form)
    }
    ElMessage.success('学院信息已保存')
    dialogVisible.value = false
    await loadColleges()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除学院“${row.collegeName}”吗？`, '删除确认', { type: 'warning' })
  await deleteCollege(row.id)
  ElMessage.success('学院已删除')
  await loadColleges()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadColleges()
}

onMounted(() => {
  loadColleges()
})
</script>

<style scoped>
.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

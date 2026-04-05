<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室管理</p>
          <h2>基础信息与组织建档</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadLabs">刷新</el-button>
          <el-button v-if="isSuperAdmin" type="primary" @click="openDialog()">新增实验室</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="实验室名称">
          <el-input v-model="filters.labName" clearable placeholder="输入名称搜索" />
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

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="labs" stripe>
        <el-table-column prop="labName" label="实验室" min-width="180" />
        <el-table-column prop="labCode" label="编码" min-width="120" />
        <el-table-column prop="teacherName" label="指导教师" min-width="120" />
        <el-table-column prop="location" label="地点" min-width="140" />
        <el-table-column prop="recruitNum" label="计划容量" min-width="100" />
        <el-table-column prop="currentNum" label="当前成员" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="isSuperAdmin" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isSuperAdmin" class="pagination-row">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="dialog-form">
        <div class="two-column-form">
          <el-form-item label="实验室名称" prop="labName">
            <el-input v-model="form.labName" />
          </el-form-item>
          <el-form-item label="实验室编码" prop="labCode">
            <el-input v-model="form.labCode" />
          </el-form-item>
          <el-form-item label="所属学院" prop="collegeId">
            <el-select v-model="form.collegeId" clearable placeholder="请选择学院" :disabled="!isSuperAdmin">
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="指导教师" prop="teacherName">
            <el-input v-model="form.teacherName" />
          </el-form-item>
          <el-form-item label="实验室地点" prop="location">
            <el-input v-model="form.location" />
          </el-form-item>
          <el-form-item label="联系邮箱" prop="contactEmail">
            <el-input v-model="form.contactEmail" />
          </el-form-item>
          <el-form-item label="计划容量" prop="recruitNum">
            <el-input-number v-model="form.recruitNum" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="开放" :value="1" />
              <el-option label="关闭" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="招新要求" prop="requireSkill">
          <el-input v-model="form.requireSkill" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="实验室简介" prop="labDesc">
          <el-input v-model="form.labDesc" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="基础信息" prop="basicInfo">
          <el-input v-model="form.basicInfo" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="获奖成果" prop="awards">
          <el-input v-model="form.awards" type="textarea" :rows="3" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { getCollegeOptions } from '@/api/colleges'
import { createLab, deleteLab, getLabById, getLabPage, updateLab, updateManagedLab } from '@/api/lab'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const labs = ref([])
const colleges = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  labName: '',
  status: ''
})

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
  awards: ''
})

const rules = {
  labName: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  labCode: [{ required: true, message: '请输入实验室编码', trigger: 'blur' }],
  recruitNum: [{ required: true, message: '请输入计划容量', trigger: 'change' }]
}

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')
const dialogTitle = computed(() => (form.id ? '编辑实验室' : '新增实验室'))

const resetForm = () => {
  Object.assign(form, {
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
    awards: ''
  })
}

const loadColleges = async () => {
  const response = await getCollegeOptions()
  colleges.value = response.data || []
}

const loadLabs = async () => {
  loading.value = true
  try {
    if (isSuperAdmin.value) {
      const response = await getLabPage({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        labName: filters.labName || undefined,
        status: filters.status === '' ? undefined : filters.status
      })
      labs.value = response.data.records || []
      pagination.total = response.data.total || 0
      return
    }

    if (!userStore.userInfo?.labId) {
      labs.value = []
      return
    }
    const response = await getLabById(userStore.userInfo.labId)
    labs.value = response.data ? [response.data] : []
    pagination.total = labs.value.length
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  resetForm()
  if (row) {
    Object.assign(form, row)
  } else if (!isSuperAdmin.value && userStore.userInfo?.labId) {
    form.id = userStore.userInfo.labId
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (isSuperAdmin.value) {
      if (form.id) {
        await updateLab(form.id, payload)
      } else {
        await createLab(payload)
      }
    } else {
      await updateManagedLab(payload)
    }
    ElMessage.success('实验室信息已保存')
    dialogVisible.value = false
    await loadLabs()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除实验室“${row.labName}”吗？`, '删除确认', { type: 'warning' })
  await deleteLab(row.id)
  ElMessage.success('实验室已删除')
  await loadLabs()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadLabs()
}

onMounted(async () => {
  await Promise.all([loadColleges(), loadLabs()])
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

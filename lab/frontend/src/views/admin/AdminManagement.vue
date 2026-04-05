<template>
  <div class="admin-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理员管理</span>
          <el-button type="primary" @click="showAddDialog">添加管理员</el-button>
        </div>
      </template>
      
      <el-table :data="adminList" style="width: 100%">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="createTime" label="创建时间">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button 
              type="primary" 
              size="small" 
              :disabled="scope.row.canEdit === 0"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              :disabled="scope.row.canEdit === 0"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 添加/编辑管理员对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="500px"
    >
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="form.username" 
            :disabled="isEdit"
            placeholder="请输入管理员用户名"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="所属实验室" prop="labId">
          <el-select v-model="form.labId" placeholder="请选择实验室" style="width: 100%">
            <el-option 
              v-for="lab in labList" 
              :key="lab.id" 
              :label="lab.labName" 
              :value="lab.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllAdmins, addAdmin, updateAdmin, deleteAdmin } from '@/api/admin'
import { getAllLabs } from '@/api/lab'

// 数据定义
const adminList = ref([])
const labList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const dialogTitle = ref('添加管理员')

// 表单数据
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  labId: null
})

// 表单验证规则
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入电话', trigger: 'blur' }
  ],
  labId: [
    { required: true, message: '请选择实验室', trigger: 'change' }
  ]
})

// 获取管理员列表
const fetchAdminList = async () => {
  try {
    const response = await getAllAdmins()
    adminList.value = response.records || []
  } catch (error) {
    console.error('获取管理员列表失败:', error)
    ElMessage.error('获取管理员列表失败')
  }
}

// 获取实验室列表
const fetchLabList = async () => {
  try {
    const response = await getAllLabs()
    // 处理分页数据结构
    labList.value = response.records || response || []
  } catch (error) {
    console.error('获取实验室列表失败:', error)
    ElMessage.error('获取实验室列表失败')
  }
}

// 显示添加对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '添加管理员'
  resetForm()
  dialogVisible.value = true
}

// 处理编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑管理员'
  Object.assign(form, row)
  dialogVisible.value = true
}

// 处理删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除管理员 ${row.realName} 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteAdmin(row.id)
      ElMessage.success('删除成功')
      fetchAdminList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await updateAdmin(form)
          ElMessage.success('更新成功')
        } else {
          await addAdmin(form)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        fetchAdminList()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    id: null,
    username: '',
    password: '',
    realName: '',
    email: '',
    phone: '',
    labId: null
  })
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

// 组件挂载时获取数据
onMounted(() => {
  fetchAdminList()
  fetchLabList()
})
</script>

<style scoped>
.admin-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

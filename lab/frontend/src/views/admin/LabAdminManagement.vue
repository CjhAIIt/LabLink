<template>
  <div class="lab-admin-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验室管理员管理</span>
          <el-button type="primary" @click="showAssignDialog">指定管理员</el-button>
        </div>
      </template>
      
      <el-table :data="labList" style="width: 100%">
        <el-table-column prop="lab.labName" label="实验室名称" />
        <el-table-column prop="lab.labDesc" label="实验室描述" show-overflow-tooltip />
        <el-table-column label="当前管理员" width="180">
          <template #default="scope">
            <div v-if="scope.row.admin">
              <div>{{ scope.row.admin.realName }}</div>
              <div class="admin-info">{{ scope.row.admin.username }} ({{ scope.row.admin.email }})</div>
            </div>
            <span v-else class="no-admin">暂无管理员</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button 
              v-if="scope.row.admin"
              type="danger" 
              size="small" 
              @click="handleRemoveAdmin(scope.row)"
            >
              移除管理员
            </el-button>
            <el-button 
              v-else
              type="primary" 
              size="small" 
              @click="handleAssignAdmin(scope.row)"
            >
              指定管理员
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 指定管理员对话框 -->
    <el-dialog 
      v-model="assignDialogVisible" 
      title="指定实验室管理员"
      width="600px"
    >
      <div class="assign-content">
        <div class="lab-info">
          <h4>实验室信息</h4>
          <p><strong>名称:</strong> {{ currentLab?.lab?.labName }}</p>
          <p><strong>描述:</strong> {{ currentLab?.lab?.labDesc }}</p>
          <p v-if="currentLab?.admin" class="current-admin">
            <strong>当前管理员:</strong> {{ currentLab.admin.realName }} ({{ currentLab.admin.username }})
          </p>
        </div>
        
        <div class="student-selection">
          <h4>选择学生作为管理员</h4>
          <el-table 
            :data="availableStudents" 
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="phone" label="电话" />
          </el-table>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assignDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            :disabled="!selectedStudent"
            @click="handleAssignSubmit"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getLabsWithAdmin, 
  assignAdminToLab, 
  removeAdminFromLab,
  getAllAdminsWithLabs
} from '@/api/adminManagement'
import { getStudentList } from '@/api/admin'

// 数据定义
const labList = ref([])
const availableStudents = ref([])
const assignDialogVisible = ref(false)
const currentLab = ref(null)
const selectedStudent = ref(null)

// 获取实验室列表（包含管理员信息）
const fetchLabList = async () => {
  try {
    const response = await getLabsWithAdmin()
    labList.value = response.data || []
  } catch (error) {
    console.error('获取实验室列表失败:', error)
    ElMessage.error('获取实验室列表失败')
  }
}

// 获取可用的学生列表
const fetchAvailableStudents = async () => {
  try {
    const response = await getStudentList()
    // 过滤掉已经是管理员的学生
    const allAdminsResponse = await getAllAdminsWithLabs()
    const adminIds = allAdminsResponse.data?.map(admin => admin.id) || []
    
    availableStudents.value = response.data?.filter(student => 
      !adminIds.includes(student.id)
    ) || []
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  }
}

// 显示指定管理员对话框
const showAssignDialog = () => {
  currentLab.value = null
  selectedStudent.value = null
  fetchAvailableStudents()
  assignDialogVisible.value = true
}

// 处理为特定实验室指定管理员
const handleAssignAdmin = (lab) => {
  currentLab.value = lab
  selectedStudent.value = null
  fetchAvailableStudents()
  assignDialogVisible.value = true
}

// 处理移除管理员
const handleRemoveAdmin = (lab) => {
  const adminName = lab.admin?.realName || '未知'
  const labName = lab.lab?.name || '未知'
  
  ElMessageBox.confirm(
    `确定要移除 ${labName} 的管理员 ${adminName} 吗？移除后该学生将恢复为学生身份。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await removeAdminFromLab(lab.lab.id)
      ElMessage.success('移除管理员成功')
      fetchLabList()
    } catch (error) {
      console.error('移除管理员失败:', error)
      ElMessage.error(error.message || '移除管理员失败')
    }
  }).catch(() => {
    ElMessage.info('已取消移除')
  })
}

// 处理选择学生
const handleSelectionChange = (selection) => {
  selectedStudent.value = selection.length > 0 ? selection[0] : null
}

// 提交指定管理员
const handleAssignSubmit = async () => {
  if (!selectedStudent.value) {
    ElMessage.warning('请选择一个学生')
    return
  }
  
  const labId = currentLab.value?.lab?.id
  const userId = selectedStudent.value.id
  
  if (!labId) {
    ElMessage.error('实验室信息无效')
    return
  }
  
  try {
    await assignAdminToLab(labId, userId)
    ElMessage.success('指定管理员成功')
    assignDialogVisible.value = false
    fetchLabList()
  } catch (error) {
    console.error('指定管理员失败:', error)
    ElMessage.error(error.message || '指定管理员失败')
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchLabList()
})
</script>

<style scoped>
.lab-admin-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-info {
  font-size: 12px;
  color: #666;
}

.no-admin {
  color: #999;
  font-style: italic;
}

.current-admin {
  color: #409eff;
  margin-top: 10px;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
}

.assign-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.lab-info {
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.lab-info h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.lab-info p {
  margin: 5px 0;
}

.student-selection h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

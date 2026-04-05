<template>
  <div class="lab-admin-transfer">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理员权限转让</span>
        </div>
      </template>
      
      <div class="transfer-content">
        <el-alert
          title="权限转让说明"
          type="warning"
          description="将管理员权限转让给其他学生后，您将自动降级为普通学生，失去对实验室的管理权限。请谨慎操作！"
          show-icon
          :closable="false"
          class="alert-box"
        />
        
        <div class="current-info">
          <h3>当前实验室信息</h3>
          <el-descriptions border :column="1">
            <el-descriptions-item label="实验室名称">{{ labInfo.labName }}</el-descriptions-item>
            <el-descriptions-item label="当前管理员">{{ userInfo.realName }} ({{ userInfo.username }})</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="search-student">
          <h3>选择新管理员</h3>
          <el-form :model="searchForm" inline class="search-form">
            <el-form-item label="学号/姓名">
              <el-input 
                v-model="searchForm.keyword" 
                placeholder="请输入学号或姓名搜索" 
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="searching" @click="handleSearch">搜索</el-button>
            </el-form-item>
          </el-form>
          
          <el-table 
            v-if="students.length > 0" 
            :data="students" 
            stripe 
            style="width: 100%; margin-top: 20px;"
          >
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="grade" label="年级" width="100" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="handleTransfer(scope.row)"
                >
                  转让权限
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-else description="请输入关键词搜索学生" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { clearAuth } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()
const userInfo = userStore.userInfo

const labInfo = ref({})
const students = ref([])
const searching = ref(false)

const searchForm = reactive({
  keyword: ''
})

// 获取当前实验室信息
const fetchLabInfo = async () => {
  try {
    if (userInfo.labId) {
      const res = await request.get(`/api/labs/${userInfo.labId}`)
      labInfo.value = res.data
    }
  } catch (error) {
    console.error('获取实验室信息失败:', error)
  }
}

// 搜索学生
const handleSearch = async () => {
  if (!searchForm.keyword) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  try {
    const res = await request.get('/api/user/list', {
      params: {
        pageNum: 1,
        pageSize: 20,
        role: 'student',
        studentId: searchForm.keyword, // 后端支持按学号或姓名模糊搜索，这里简化传参
        // 也可以传 realName: searchForm.keyword，视后端实现而定
        // 假设后端UserController.list支持studentId作为关键词
      }
    })
    // 过滤掉当前已经是管理员的用户（虽然role=student已经过滤了，但双重保险）
    students.value = (res.data.records || []).filter(s => s.role === 'student')
  } catch (error) {
    console.error('搜索学生失败:', error)
    ElMessage.error('搜索学生失败')
  } finally {
    searching.value = false
  }
}

// 转让权限
const handleTransfer = (student) => {
  ElMessageBox.confirm(
    `确定要将管理员权限转让给学生 ${student.realName} (${student.studentId}) 吗？\n转让后您将失去管理权限并自动退出登录。`,
    '危险操作警告',
    {
      confirmButtonText: '确定转让',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await request.post('/api/admin-management/assign', {
        labId: userInfo.labId,
        userId: student.id
      })
      
      ElMessage.success('权限转让成功，即将退出登录...')
      
      // 延迟跳转，让用户看清提示
      setTimeout(() => {
        clearAuth()
        router.push('/login')
      }, 1500)
      
    } catch (error) {
      console.error('转让权限失败:', error)
      ElMessage.error(error.message || '转让权限失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchLabInfo()
})
</script>

<style scoped>
.lab-admin-transfer {
  padding: 0;
}

.alert-box {
  margin-bottom: 24px;
}

.current-info {
  margin-bottom: 32px;
}

.current-info h3, .search-student h3 {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.search-form {
  margin-bottom: 16px;
}
</style>

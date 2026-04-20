<template>
  <div class="admin-students-page">
    <el-alert
      v-if="isLabAdmin"
      class="scope-alert"
      type="info"
      :closable="false"
      show-icon
      title="当前页面仅展示投递过本实验室的学生，且只能查看本实验室的投递记录。"
    />

    <TablePageCard :title="pageTitle" subtitle="学生目录" :count-label="`${pagination.total} 条`">
      <template #header-extra>
        <div class="card-header">
          <el-button type="primary" @click="fetchStudents">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <template #filters>
        <div class="search-area">
        <el-form :model="searchForm" inline>
          <el-form-item label="学生姓名">
            <el-input
              v-model="searchForm.realName"
              placeholder="请输入学生姓名"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="学号">
            <el-input
              v-model="searchForm.studentId"
              placeholder="请输入学号"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="专业">
            <el-input
              v-model="searchForm.major"
              placeholder="请输入专业"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
        </div>
      </template>

      <el-table v-loading="loading" :data="students" stripe>
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="major" label="专业" width="180" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column label="申请统计" min-width="190">
          <template #default="{ row }">
            <div class="application-stats">
              <el-tag size="small" type="warning">待审核: {{ row.pendingCount || 0 }}</el-tag>
              <el-tag size="small" type="success">已通过: {{ row.approvedCount || 0 }}</el-tag>
              <el-tag size="small" type="danger">已拒绝: {{ row.rejectedCount || 0 }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="canDeleteStudents ? 220 : 140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewStudent(row)">查看</el-button>
            <el-button size="small" type="primary" @click="viewApplications(row)">申请记录</el-button>
            <el-button
              v-if="canDeleteStudents"
              size="small"
              type="danger"
              @click="deleteStudent(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </template>
    </TablePageCard>

    <el-dialog
      v-model="showDetailDialog"
      title="学生详情"
      width="60%"
      :close-on-click-modal="false"
    >
      <div v-if="selectedStudent" class="student-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">
            {{ selectedStudent.realName }}
          </el-descriptions-item>
          <el-descriptions-item label="学号">
            {{ selectedStudent.studentId }}
          </el-descriptions-item>
          <el-descriptions-item label="专业">
            {{ selectedStudent.major || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ selectedStudent.email || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            {{ selectedStudent.username }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDateTime(selectedStudent.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(selectedStudent.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="申请统计">
            <div class="application-stats">
              <el-tag size="small" type="warning">待审核: {{ selectedStudent.pendingCount || 0 }}</el-tag>
              <el-tag size="small" type="success">已通过: {{ selectedStudent.approvedCount || 0 }}</el-tag>
              <el-tag size="small" type="danger">已拒绝: {{ selectedStudent.rejectedCount || 0 }}</el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="个人简历">
            <el-link v-if="selectedStudent.resume" :href="resolveFileUrl(selectedStudent.resume)" target="_blank" type="primary">
              打开简历
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDetailDialog = false">关闭</el-button>
          <el-button type="primary" @click="viewApplications(selectedStudent)">申请记录</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showApplicationsDialog"
      title="学生申请记录"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="selectedStudent" class="student-applications">
        <el-table v-loading="applicationsLoading" :data="studentApplications" stripe>
          <el-table-column prop="labName" label="实验室名称" min-width="150" />
          <el-table-column prop="reason" label="申请理由" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <StatusTag
                :value="row.displayStatus ?? row.status"
                :label-map="deliveryStatusLabels"
                :type-map="deliveryStatusTypes"
              />
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="审核意见" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="viewApplicationDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="applicationsPagination.current"
            v-model:page-size="applicationsPagination.size"
            :page-sizes="[10, 20, 50]"
            :total="applicationsPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleApplicationsSizeChange"
            @current-change="handleApplicationsCurrentChange"
          />
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showApplicationsDialog = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import request from '@/utils/request'
import { getUserInfo } from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'

function normalizeDelivery(record) {
  let displayStatus = 0

  if (record.status === 2) {
    displayStatus = 2
  } else if (record.isAdmitted === 1) {
    displayStatus = 1
  } else if (record.isAdmitted === 2) {
    displayStatus = 3
  } else if (record.isAdmitted === 3) {
    displayStatus = 5
  } else if (record.status === 1) {
    displayStatus = 4
  }

  return {
    ...record,
    displayStatus
  }
}

export default {
  name: 'AdminStudents',
  components: {
    Refresh,
    StatusTag,
    TablePageCard
  },
  setup() {
    const router = useRouter()
    const currentUser = ref(getUserInfo() || {})

    const loading = ref(false)
    const applicationsLoading = ref(false)
    const showDetailDialog = ref(false)
    const showApplicationsDialog = ref(false)
    const selectedStudent = ref(null)
    const students = ref([])
    const studentApplications = ref([])

    const searchForm = reactive({
      realName: '',
      studentId: '',
      major: ''
    })

    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    const applicationsPagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    const deliveryStatusLabels = {
      0: '待审核',
      1: '待确认 offer',
      2: '已拒绝',
      3: '已加入',
      4: '审核通过',
      5: 'offer 已关闭',
      PENDING: '待审核',
      APPROVED: '已通过',
      REJECTED: '已拒绝'
    }

    const deliveryStatusTypes = {
      0: 'warning',
      1: 'success',
      2: 'danger',
      3: 'success',
      4: 'info',
      5: 'info',
      PENDING: 'warning',
      APPROVED: 'success',
      REJECTED: 'danger'
    }

    const isLabAdmin = computed(() => currentUser.value?.role === 'admin')
    const canDeleteStudents = computed(() => currentUser.value?.role === 'super_admin')
    const pageTitle = computed(() => (isLabAdmin.value ? '投递学生' : '学生管理'))

    const fetchStudents = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.size,
          ...searchForm
        }

        Object.keys(params).forEach((key) => {
          if (params[key] === '') {
            delete params[key]
          }
        })

        const response = await request.get('/api/admin/users', { params })
        students.value = response.data.records || []
        pagination.total = response.data.total || 0
      } catch (error) {
        console.error('获取学生列表失败:', error)
        ElMessage.error('获取学生列表失败')
      } finally {
        loading.value = false
      }
    }

    const fetchStudentApplications = async (studentId) => {
      applicationsLoading.value = true
      try {
        const params = {
          pageNum: applicationsPagination.current,
          pageSize: applicationsPagination.size,
          studentId
        }

        const response = await request.get('/api/delivery/list', { params })
        studentApplications.value = (response.data.records || []).map(normalizeDelivery)
        applicationsPagination.total = response.data.total || 0
      } catch (error) {
        console.error('获取学生申请记录失败:', error)
        ElMessage.error('获取学生申请记录失败')
      } finally {
        applicationsLoading.value = false
      }
    }

    const handleSearch = () => {
      pagination.current = 1
      fetchStudents()
    }

    const resetSearch = () => {
      searchForm.realName = ''
      searchForm.studentId = ''
      searchForm.major = ''
      pagination.current = 1
      fetchStudents()
    }

    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      fetchStudents()
    }

    const handleCurrentChange = (current) => {
      pagination.current = current
      fetchStudents()
    }

    const handleApplicationsSizeChange = (size) => {
      applicationsPagination.size = size
      applicationsPagination.current = 1
      if (selectedStudent.value) {
        fetchStudentApplications(selectedStudent.value.studentId)
      }
    }

    const handleApplicationsCurrentChange = (current) => {
      applicationsPagination.current = current
      if (selectedStudent.value) {
        fetchStudentApplications(selectedStudent.value.studentId)
      }
    }

    const viewStudent = (student) => {
      selectedStudent.value = student
      showDetailDialog.value = true
    }

    const viewApplications = (student) => {
      if (!student) {
        return
      }

      selectedStudent.value = student
      applicationsPagination.current = 1
      fetchStudentApplications(student.studentId)

      if (showDetailDialog.value) {
        showDetailDialog.value = false
      }

      showApplicationsDialog.value = true
    }

    const viewApplicationDetail = (application) => {
      showApplicationsDialog.value = false
      router.push(`/admin/delivery?id=${application.id}`)
    }

    const deleteStudent = async (student) => {
      if (!canDeleteStudents.value) {
        ElMessage.warning('实验室管理员不能删除学生账号')
        return
      }

      try {
        await ElMessageBox.confirm(
          `确定要删除学生“${student.realName}”吗？删除后将无法恢复。`,
          '删除学生',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        await request.delete(`/api/admin/users/${student.id}`)
        ElMessage.success('学生删除成功')
        fetchStudents()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除学生失败:', error)
          ElMessage.error('删除学生失败')
        }
      }
    }

    const getStatusType = (status) => {
      const typeMap = {
        0: 'warning',
        1: 'success',
        2: 'danger',
        3: 'success',
        4: 'info',
        5: 'info',
        PENDING: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger'
      }
      return typeMap[status] || 'info'
    }

    const getStatusText = (status) => {
      const textMap = {
        0: '待审核',
        1: '待确认 offer',
        2: '已拒绝',
        3: '已确认加入',
        4: '审核通过',
        5: 'offer 已关闭',
        PENDING: '待审核',
        APPROVED: '已通过',
        REJECTED: '已拒绝'
      }
      return textMap[status] || '未知'
    }

    const formatDateTime = (dateString) => {
      if (!dateString) {
        return ''
      }

      const date = new Date(dateString)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
        date.getDate()
      ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(
        date.getMinutes()
      ).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    }

    onMounted(() => {
      fetchStudents()
    })

    return {
      loading,
      applicationsLoading,
      searchForm,
      pagination,
      applicationsPagination,
      students,
      selectedStudent,
      studentApplications,
      showDetailDialog,
      showApplicationsDialog,
      isLabAdmin,
      canDeleteStudents,
      pageTitle,
      deliveryStatusLabels,
      deliveryStatusTypes,
      fetchStudents,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      handleApplicationsSizeChange,
      handleApplicationsCurrentChange,
      viewStudent,
      viewApplications,
      viewApplicationDetail,
      deleteStudent,
      getStatusType,
      getStatusText,
      formatDateTime,
      resolveFileUrl
    }
  }
}
</script>

<style scoped>
.admin-students-page {
  padding: 0;
}

.scope-alert {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-area {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.student-detail {
  margin-bottom: 20px;
}

.application-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.student-applications {
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

<template>
  <div class="admin-delivery-page">
    <el-alert
      v-if="!canAudit"
      type="info"
      :closable="false"
      show-icon
      class="readonly-alert"
      title="超级管理员仅可查看投递信息，审核与发放 offer 由各实验室管理员负责。"
    />

    <el-card>
      <template #header>
        <div class="card-header">
          <span>投递管理</span>
          <el-button type="primary" @click="fetchDeliveries">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <div class="search-area">
        <el-form :model="searchForm" inline>
          <el-form-item label="学生姓名">
            <el-input
              v-model="searchForm.studentName"
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
          <el-form-item label="审核状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="待审核" :value="0" />
              <el-option label="审核通过 / offer 处理中" :value="1" />
              <el-option label="已拒绝" :value="2" />
              <el-option label="已撤销" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="deliveries" stripe>
        <el-table-column prop="labName" label="实验室" min-width="160" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="reason" label="投递内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.displayStatus)">
              {{ getStatusText(row.displayStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="canAudit ? '操作' : '查看'" :width="canAudit ? 240 : 90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <template v-if="canAudit">
              <el-button
                size="small"
                type="success"
                :disabled="row.displayStatus !== 0"
                @click="openReviewDialog(row, 'approve')"
              >
                通过并发放 offer
              </el-button>
              <el-button
                size="small"
                type="danger"
                :disabled="row.displayStatus !== 0"
                @click="openReviewDialog(row, 'reject')"
              >
                拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetailDialog" title="投递详情" width="60%" :close-on-click-modal="false">
      <div v-if="selectedDelivery" class="delivery-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实验室">{{ selectedDelivery.labName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedDelivery.displayStatus)">
              {{ getStatusText(selectedDelivery.displayStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="学生姓名">{{ selectedDelivery.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ selectedDelivery.studentId }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ selectedDelivery.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selectedDelivery.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="投递时间" :span="2">
            {{ formatDateTime(selectedDelivery.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态更新时间" :span="2">
            {{ formatDateTime(selectedDelivery.updateTime || selectedDelivery.admitTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="累计投递次数">
            {{ selectedDelivery.deliveryAttemptCount }} / 2
          </el-descriptions-item>
          <el-descriptions-item label="已用撤销次数">
            {{ selectedDelivery.withdrawCount }} / 1
          </el-descriptions-item>
          <el-descriptions-item label="投递内容" :span="2">
            <div class="reason-content">{{ selectedDelivery.reason }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDelivery.attachments.length > 0" label="附件" :span="2">
            <div class="attachments-list">
              <div v-for="attachment in selectedDelivery.attachments" :key="attachment.url" class="attachment-item">
                <el-link :href="attachment.url" target="_blank" type="primary">
                  {{ attachment.name }}
                </el-link>
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDelivery.comment" label="备注" :span="2">
            <div class="comment-content">{{ selectedDelivery.comment }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDelivery.showProfileResume" label="个人简历" :span="2">
            <el-link :href="selectedDelivery.resumeUrl" target="_blank" type="primary">
              查看资料中的简历
            </el-link>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDetailDialog = false">关闭</el-button>
          <template v-if="canAudit">
            <el-button
              v-if="selectedDelivery?.displayStatus === 0"
              type="success"
              @click="openReviewDialog(selectedDelivery, 'approve')"
            >
              通过并发放 offer
            </el-button>
            <el-button
              v-if="selectedDelivery?.displayStatus === 0"
              type="danger"
              @click="openReviewDialog(selectedDelivery, 'reject')"
            >
              拒绝
            </el-button>
          </template>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showReviewDialog" :title="reviewDialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="90px">
        <el-form-item label="审核备注" prop="comment">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            :placeholder="reviewPlaceholder"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showReviewDialog = false">取消</el-button>
          <el-button type="primary" :loading="reviewing" @click="submitReview">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getUserInfo } from '@/utils/auth'
import { buildAttachmentList, resolveFileUrl } from '@/utils/file'

function normalizeDelivery(record) {
  let displayStatus = 0

  if (record.status === 3) {
    displayStatus = 6
  } else if (record.status === 2) {
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

  const attachments = buildAttachmentList(record.attachmentUrl || record.resumeUrl)
  const resumeUrl = record.resumeUrl ? resolveFileUrl(record.resumeUrl) : ''
  const showProfileResume = Boolean(resumeUrl) && !attachments.some((item) => item.rawUrl === record.resumeUrl)

  return {
    ...record,
    displayStatus,
    attachments,
    resumeUrl,
    showProfileResume,
    deliveryAttemptCount: Number(record.deliveryAttemptCount || 1),
    withdrawCount: Number(record.withdrawCount || 0)
  }
}

export default {
  name: 'AdminDelivery',
  components: {
    Refresh
  },
  setup() {
    const loading = ref(false)
    const reviewing = ref(false)
    const reviewFormRef = ref(null)
    const currentUser = ref(getUserInfo() || {})

    const searchForm = reactive({
      studentName: '',
      studentId: '',
      status: null
    })

    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })

    const deliveries = ref([])
    const selectedDelivery = ref(null)
    const showDetailDialog = ref(false)
    const showReviewDialog = ref(false)
    const reviewType = ref('approve')
    const reviewForm = reactive({
      id: null,
      comment: ''
    })

    const canAudit = computed(() => currentUser.value?.role === 'admin')

    const reviewDialogTitle = computed(() =>
      reviewType.value === 'approve' ? '通过投递并发放 offer' : '拒绝投递'
    )

    const reviewPlaceholder = computed(() =>
      reviewType.value === 'approve'
        ? '请输入通过理由，学生会收到 offer 通知'
        : '请输入拒绝原因'
    )

    const reviewRules = {
      comment: [
        { required: true, message: '请输入审核备注', trigger: 'blur' },
        { min: 2, message: '审核备注至少 2 个字', trigger: 'blur' }
      ]
    }

    const fetchDeliveries = async () => {
      loading.value = true
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.size,
          realName: searchForm.studentName || undefined,
          studentId: searchForm.studentId || undefined,
          auditStatus: searchForm.status
        }

        const response = await request.get('/api/delivery/list', { params })
        deliveries.value = (response.data.records || []).map(normalizeDelivery)
        pagination.total = response.data.total || 0
      } catch (error) {
        console.error('获取投递列表失败:', error)
        ElMessage.error('获取投递列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.current = 1
      fetchDeliveries()
    }

    const resetSearch = () => {
      searchForm.studentName = ''
      searchForm.studentId = ''
      searchForm.status = null
      pagination.current = 1
      fetchDeliveries()
    }

    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      fetchDeliveries()
    }

    const handleCurrentChange = (current) => {
      pagination.current = current
      fetchDeliveries()
    }

    const viewDetail = (delivery) => {
      selectedDelivery.value = delivery
      showDetailDialog.value = true
    }

    const openReviewDialog = (delivery, type) => {
      if (!canAudit.value) {
        ElMessage.warning('当前账号只有查看权限，不能审核投递')
        return
      }

      reviewType.value = type
      reviewForm.id = delivery.id
      reviewForm.comment = ''
      showDetailDialog.value = false
      showReviewDialog.value = true
    }

    const submitReview = async () => {
      if (!reviewFormRef.value || !canAudit.value) {
        return
      }

      try {
        await reviewFormRef.value.validate()
        reviewing.value = true

        if (reviewType.value === 'approve') {
          await request.post(`/api/delivery/audit/${reviewForm.id}`, null, {
            params: {
              auditStatus: 1,
              auditRemark: reviewForm.comment
            }
          })

          await request.post(`/api/delivery/admit/${reviewForm.id}`)
          ElMessage.success('已发放 offer，等待学生确认')
        } else {
          await request.post(`/api/delivery/audit/${reviewForm.id}`, null, {
            params: {
              auditStatus: 2,
              auditRemark: reviewForm.comment
            }
          })
          ElMessage.success('已拒绝该投递')
        }

        showReviewDialog.value = false
        fetchDeliveries()
      } catch (error) {
        console.error('处理投递失败:', error)
        ElMessage.error(error.message || '处理投递失败')
      } finally {
        reviewing.value = false
      }
    }

    const getStatusType = (status) => {
      const statusMap = {
        0: 'info',
        1: 'success',
        2: 'danger',
        3: 'warning',
        4: 'primary',
        5: '',
        6: 'warning'
      }
      return statusMap[status] || 'info'
    }

    const getStatusText = (status) => {
      const statusMap = {
        0: '待审核',
        1: '已加入',
        2: '已拒绝',
        3: '待学生确认',
        4: '审核通过',
        5: 'offer 已关闭',
        6: '已撤销'
      }
      return statusMap[status] || '未知状态'
    }

    const formatDateTime = (dateString) => {
      if (!dateString) {
        return '-'
      }
      const date = new Date(dateString)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
        date.getDate()
      ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(
        date.getMinutes()
      ).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    }

    onMounted(fetchDeliveries)

    return {
      loading,
      reviewing,
      reviewFormRef,
      searchForm,
      pagination,
      deliveries,
      selectedDelivery,
      showDetailDialog,
      showReviewDialog,
      reviewForm,
      reviewDialogTitle,
      reviewPlaceholder,
      reviewRules,
      canAudit,
      fetchDeliveries,
      handleSearch,
      resetSearch,
      handleSizeChange,
      handleCurrentChange,
      viewDetail,
      openReviewDialog,
      submitReview,
      getStatusType,
      getStatusText,
      formatDateTime
    }
  }
}
</script>

<style scoped>
.admin-delivery-page {
  padding: 0;
}

.readonly-alert {
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

.reason-content,
.comment-content {
  white-space: pre-wrap;
  line-height: 1.6;
  word-break: break-word;
}

.comment-content {
  color: #606266;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

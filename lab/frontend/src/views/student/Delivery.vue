<template>
  <div class="delivery-page">
    <el-card v-if="offerSummary.pendingOfferCount > 0" class="offer-notice-card" shadow="never">
      <template #header>
        <div class="offer-notice-header">
          <span>录取通知</span>
          <el-tag type="warning">待处理 {{ offerSummary.pendingOfferCount }} 项</el-tag>
        </div>
      </template>

      <div class="offer-list">
        <div v-for="offer in offerSummary.offers" :key="offer.id" class="offer-item">
          <div class="offer-main">
            <div class="offer-title">{{ offer.labName }}</div>
            <div class="offer-meta">
              <span>发放时间：{{ formatDateTime(offer.admitTime || offer.updateTime) }}</span>
            </div>
            <div v-if="offer.comment" class="offer-comment">{{ offer.comment }}</div>
          </div>
          <div class="offer-actions">
            <el-button size="small" @click="viewDetail(offer)">查看详情</el-button>
            <el-button size="small" type="success" @click="confirmOffer(offer)">接受</el-button>
            <el-button size="small" type="danger" plain @click="rejectOffer(offer)">拒绝</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="delivery-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>我的投递</span>
            <p class="card-subtitle">手机端已切换为卡片视图，桌面端保留表格视图。</p>
          </div>
          <el-button type="primary" @click="refreshPageData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <div class="search-area">
        <el-form :model="searchForm" inline class="search-form">
          <el-form-item label="实验室">
            <el-input
              v-model="searchForm.labName"
              placeholder="请输入实验室名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
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

      <div v-if="isMobile" v-loading="loading" class="mobile-delivery-list">
        <template v-if="deliveries.length">
          <div v-for="row in deliveries" :key="row.id" class="delivery-mobile-card">
            <div class="delivery-mobile-top">
              <div>
                <div class="delivery-mobile-title">{{ row.labName }}</div>
                <div class="delivery-mobile-time">{{ formatDateTime(row.createTime) }}</div>
              </div>
              <el-tag :type="getStatusType(row.displayStatus)">
                {{ getStatusText(row.displayStatus) }}
              </el-tag>
            </div>

            <div class="delivery-mobile-body">
              <div class="delivery-mobile-reason">{{ row.reason || '暂无投递说明' }}</div>
              <div class="delivery-mobile-meta">
                <span>状态更新时间：{{ formatDateTime(row.updateTime || row.admitTime) }}</span>
                <span>投递次数：{{ row.deliveryAttemptCount }} / 2</span>
              </div>
            </div>

            <div class="delivery-mobile-actions">
              <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
              <el-button v-if="row.canWithdraw" size="small" type="warning" plain @click="withdrawDelivery(row)">
                撤销投递
              </el-button>
              <el-button v-if="row.displayStatus === 3" size="small" type="success" @click="confirmOffer(row)">
                接受
              </el-button>
              <el-button v-if="row.displayStatus === 3" size="small" type="danger" plain @click="rejectOffer(row)">
                拒绝
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无投递记录" />
      </div>

      <el-table v-else v-loading="loading" :data="deliveries" stripe>
        <el-table-column prop="labName" label="实验室" min-width="160" />
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
        <el-table-column label="状态更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime || row.admitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.canWithdraw" size="small" type="warning" plain @click="withdrawDelivery(row)">
              撤销投递
            </el-button>
            <el-button v-if="row.displayStatus === 3" size="small" type="success" @click="confirmOffer(row)">
              接受
            </el-button>
            <el-button v-if="row.displayStatus === 3" size="small" type="danger" plain @click="rejectOffer(row)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          :layout="paginationLayout"
          :small="isMobile"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="showDetailDialog"
      title="投递详情"
      :width="dialogWidth"
      :fullscreen="isMobile"
      :close-on-click-modal="false"
    >
      <div v-if="selectedDelivery" class="delivery-detail">
        <el-descriptions :column="descriptionColumns" border>
          <el-descriptions-item label="实验室">{{ selectedDelivery.labName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedDelivery.displayStatus)">
              {{ getStatusText(selectedDelivery.displayStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="投递时间" :span="descriptionColumns === 1 ? 1 : 2">
            {{ formatDateTime(selectedDelivery.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态更新时间" :span="descriptionColumns === 1 ? 1 : 2">
            {{ formatDateTime(selectedDelivery.updateTime || selectedDelivery.admitTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="累计投递次数">
            {{ selectedDelivery.deliveryAttemptCount }} / 2
          </el-descriptions-item>
          <el-descriptions-item label="剩余撤销重投机会">
            {{ selectedDelivery.withdrawCount > 0 ? '已用完' : '1 次' }}
          </el-descriptions-item>
          <el-descriptions-item label="投递内容" :span="descriptionColumns === 1 ? 1 : 2">
            <div class="reason-content">{{ selectedDelivery.reason }}</div>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="selectedDelivery.attachments.length > 0"
            label="附件"
            :span="descriptionColumns === 1 ? 1 : 2"
          >
            <div class="attachments-list">
              <div v-for="attachment in selectedDelivery.attachments" :key="attachment.url" class="attachment-item">
                <el-link :href="attachment.url" target="_blank" type="primary">
                  {{ attachment.name }}
                </el-link>
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="selectedDelivery.comment"
            label="备注"
            :span="descriptionColumns === 1 ? 1 : 2"
          >
            <div class="comment-content">{{ selectedDelivery.comment }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <span class="dialog-footer" :class="{ mobile: isMobile }">
          <el-button @click="showDetailDialog = false">关闭</el-button>
          <el-button
            v-if="selectedDelivery?.canWithdraw"
            type="warning"
            plain
            @click="withdrawDelivery(selectedDelivery)"
          >
            撤销投递
          </el-button>
          <el-button v-if="selectedDelivery?.displayStatus === 3" type="success" @click="confirmOffer(selectedDelivery)">
            接受 offer
          </el-button>
          <el-button
            v-if="selectedDelivery?.displayStatus === 3"
            type="danger"
            plain
            @click="rejectOffer(selectedDelivery)"
          >
            拒绝 offer
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { buildAttachmentList } from '@/utils/file'

const loading = ref(false)
const isMobile = ref(false)
const showDetailDialog = ref(false)
const selectedDelivery = ref(null)
const lastNotifiedCount = ref(0)

const searchForm = reactive({
  labName: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const deliveries = ref([])
const offerSummary = reactive({
  pendingOfferCount: 0,
  offers: []
})

const dialogWidth = computed(() => (isMobile.value ? '100%' : '60%'))
const descriptionColumns = computed(() => (isMobile.value ? 1 : 2))
const paginationLayout = computed(() =>
  isMobile.value ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'
)

const updateIsMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const normalizeDelivery = (record) => {
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

  const deliveryAttemptCount = Number(record.deliveryAttemptCount || 1)
  const withdrawCount = Number(record.withdrawCount || 0)

  return {
    ...record,
    displayStatus,
    deliveryAttemptCount,
    withdrawCount,
    canWithdraw: withdrawCount < 1 && [0, 4].includes(displayStatus) && Number(record.isAdmitted || 0) === 0,
    attachments: buildAttachmentList(record.attachmentUrl)
  }
}

const fetchDeliveries = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.size,
      labName: searchForm.labName || undefined,
      auditStatus: searchForm.status
    }

    const response = await request.get('/api/delivery/my', { params })
    deliveries.value = (response.data.records || []).map(normalizeDelivery)
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error('获取投递列表失败')
  } finally {
    loading.value = false
  }
}

const fetchOfferSummary = async (notify = false) => {
  try {
    const response = await request.get('/api/delivery/offer-summary')
    offerSummary.pendingOfferCount = response.data.pendingOfferCount || 0
    offerSummary.offers = (response.data.offers || []).map(normalizeDelivery)

    if (
      notify &&
      offerSummary.pendingOfferCount > 0 &&
      offerSummary.pendingOfferCount !== lastNotifiedCount.value
    ) {
      ElNotification({
        title: '收到新的实验室 offer',
        message: `当前有 ${offerSummary.pendingOfferCount} 个实验室等待你确认是否加入`,
        type: 'success',
        duration: 5000
      })
    }

    lastNotifiedCount.value = offerSummary.pendingOfferCount
  } catch (error) {
    offerSummary.pendingOfferCount = 0
    offerSummary.offers = []
  }
}

const refreshPageData = async (notify = false) => {
  await Promise.all([fetchDeliveries(), fetchOfferSummary(notify)])
}

const handleSearch = () => {
  pagination.current = 1
  refreshPageData()
}

const resetSearch = () => {
  searchForm.labName = ''
  searchForm.status = null
  pagination.current = 1
  refreshPageData()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  refreshPageData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  refreshPageData()
}

const viewDetail = (delivery) => {
  selectedDelivery.value = delivery
  showDetailDialog.value = true
}

const emitOfferRefresh = () => {
  window.dispatchEvent(new Event('offer-summary-refresh'))
}

const emitUserInfoRefresh = () => {
  window.dispatchEvent(new Event('user-info-refresh'))
}

const confirmOffer = async (delivery) => {
  try {
    await ElMessageBox.confirm(
      `确认加入【${delivery.labName}】后，其他尚未处理的 offer 将自动关闭。`,
      '确认加入实验室',
      {
        confirmButtonText: '确认加入',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request.post(`/api/delivery/confirm/${delivery.id}`)
    ElMessage.success('已确认加入实验室')
    showDetailDialog.value = false
    emitOfferRefresh()
    emitUserInfoRefresh()
    refreshPageData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '确认加入失败')
    }
  }
}

const rejectOffer = async (delivery) => {
  try {
    await ElMessageBox.confirm(`确认拒绝【${delivery.labName}】的 offer 吗？`, '拒绝 offer', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await request.post(`/api/delivery/reject-offer/${delivery.id}`)
    ElMessage.success('已拒绝该 offer')
    showDetailDialog.value = false
    emitOfferRefresh()
    refreshPageData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '拒绝 offer 失败')
    }
  }
}

const withdrawDelivery = async (delivery) => {
  try {
    await ElMessageBox.confirm(
      `撤销【${delivery.labName}】本次投递后，你只剩 1 次重新投递机会。确定继续吗？`,
      '撤销投递',
      {
        confirmButtonText: '确认撤销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request.post(`/api/delivery/withdraw/${delivery.id}`)
    ElMessage.success('已撤销本次投递，你可以修改后重新提交')
    showDetailDialog.value = false
    refreshPageData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '撤销投递失败')
    }
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
    3: '待确认',
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
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(
    2,
    '0'
  )}:${String(date.getSeconds()).padStart(2, '0')}`
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
  refreshPageData(true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIsMobile)
})
</script>

<style scoped>
.delivery-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.delivery-card {
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.offer-notice-card {
  border: 1px solid rgba(245, 158, 11, 0.28);
  background: linear-gradient(135deg, rgba(255, 251, 235, 1), rgba(255, 247, 237, 1));
}

.offer-notice-header,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.offer-list,
.mobile-delivery-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.offer-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(251, 191, 36, 0.18);
}

.offer-main {
  flex: 1;
}

.offer-title {
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.offer-meta {
  margin-top: 6px;
  font-size: 13px;
  color: #9a3412;
}

.offer-comment {
  margin-top: 8px;
  line-height: 1.6;
  color: #7c2d12;
  white-space: pre-wrap;
}

.offer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-area {
  margin-bottom: 20px;
}

.delivery-mobile-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.04);
}

.delivery-mobile-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.delivery-mobile-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.delivery-mobile-time {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.delivery-mobile-body {
  margin-top: 14px;
}

.delivery-mobile-reason {
  line-height: 1.7;
  color: #475569;
}

.delivery-mobile-meta {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

.delivery-mobile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
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
  border-radius: 10px;
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

@media (max-width: 768px) {
  .offer-item,
  .offer-notice-header,
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form {
    display: grid;
    gap: 4px;
  }

  .search-form :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
    margin-bottom: 14px;
  }

  .offer-actions .el-button,
  .delivery-mobile-actions .el-button {
    flex: 1 1 120px;
  }

  .dialog-footer.mobile {
    width: 100%;
    flex-direction: column-reverse;
  }

  .dialog-footer.mobile .el-button {
    width: 100%;
  }
}
</style>

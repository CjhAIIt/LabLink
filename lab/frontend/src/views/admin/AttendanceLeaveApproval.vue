<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">请假审批管理</p>
          <h2>查看待审批请假申请，支持批量通过或拒绝</h2>
        </div>
        <div class="toolbar-actions">
          <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 220px" @change="loadLeaves">
            <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="状态筛选" style="width: 140px" clearable @change="loadLeaves">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
          <el-button @click="loadLeaves">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有可管理的实验室" />
    </div>

    <template v-else>
      <section class="table-card">
        <div class="card-head">
          <h3>请假列表</h3>
          <div class="batch-actions" v-if="selectedIds.length > 0">
            <el-input v-model="batchComment" placeholder="审批意见" style="width: 200px" size="small" />
            <el-button type="success" size="small" @click="handleBatch('APPROVE')">批量通过 ({{ selectedIds.length }})</el-button>
            <el-button type="danger" size="small" @click="handleBatch('REJECT')">批量拒绝 ({{ selectedIds.length }})</el-button>
          </div>
        </div>

        <el-table :data="leaveList" stripe style="width: 100%" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" :selectable="row => row.leaveStatus === 'PENDING'" />
          <el-table-column prop="userId" label="申请人ID" width="100" />
          <el-table-column prop="leaveReason" label="请假原因" />
          <el-table-column prop="leaveStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="leaveTagType(row.leaveStatus)" size="small">{{ leaveLabel(row.leaveStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewComment" label="审批意见" width="160" />
          <el-table-column prop="createdAt" label="申请时间" width="180" />
        </el-table>

        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadLeaves" style="margin-top: 16px" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { batchApproveLeave } from '@/api/attendance'
import { getAllLabs } from '@/api/lab'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const userStore = useUserStore()
const labOptions = ref([])
const selectedLabId = ref(null)
const leaveList = ref([])
const selectedIds = ref([])
const batchComment = ref('')
const statusFilter = ref('PENDING')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showLabSelector = computed(() => userStore.userInfo?.role === 'super_admin')
const labId = computed(() => selectedLabId.value || userStore.userInfo?.labId)

const leaveLabel = (status) => {
  const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[status] || status
}

const leaveTagType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || ''
}

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const loadLeaves = async () => {
  if (!labId.value) return
  try {
    const res = await request({
      url: '/api/attendance-workflow/lab/leaves',
      method: 'get',
      params: {
        labId: labId.value,
        leaveStatus: statusFilter.value || undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    const data = res.data?.data || res.data || {}
    leaveList.value = data.records || []
    total.value = data.total || 0
  } catch { /* ignore */ }
}

const handleBatch = async (action) => {
  if (selectedIds.value.length === 0) return
  try {
    await batchApproveLeave({
      leaveIds: selectedIds.value,
      action,
      comment: batchComment.value
    })
    ElMessage.success(action === 'APPROVE' ? '批量通过成功' : '批量拒绝成功')
    batchComment.value = ''
    selectedIds.value = []
    await loadLeaves()
  } catch {
    ElMessage.error('操作失败')
  }
}

const loadLabOptions = async () => {
  if (!showLabSelector.value) return
  try {
    const res = await getAllLabs()
    const data = res.data?.data || res.data || {}
    labOptions.value = data.records || data || []
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadLabOptions()
  if (labId.value) loadLeaves()
})
</script>

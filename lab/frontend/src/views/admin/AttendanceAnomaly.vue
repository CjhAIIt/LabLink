<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">异常出勤处理</p>
          <h2>批量处理缺勤记录，支持补假和补签</h2>
        </div>
        <div class="toolbar-actions">
          <el-select v-if="showLabSelector" v-model="selectedLabId" placeholder="选择实验室" style="width: 220px" @change="loadAnomalies">
            <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
          </el-select>
          <el-button @click="loadAnomalies">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!labId" class="empty-panel">
      <el-empty description="当前账号没有可管理的实验室" />
    </div>

    <template v-else>
      <section class="table-card">
        <div class="card-head">
          <h3>异常记录列表</h3>
          <div class="batch-actions" v-if="selectedIds.length > 0">
            <el-select v-model="batchStatus" placeholder="目标状态" style="width: 120px" size="small">
              <el-option label="补假" value="LEAVE" />
              <el-option label="补签" value="SIGNED" />
            </el-select>
            <el-input v-model="batchRemark" placeholder="备注说明" style="width: 200px" size="small" />
            <el-button type="primary" size="small" @click="handleBatch">批量处理 ({{ selectedIds.length }})</el-button>
          </div>
        </div>

        <el-table :data="anomalyList" stripe style="width: 100%" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="signStatus" label="当前状态" width="100">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.signStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createTime" label="记录时间" width="180" />
        </el-table>

        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadAnomalies" style="margin-top: 16px" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getAttendanceAnomalyList, handleAttendanceAnomaly } from '@/api/attendance'
import { getAllLabs } from '@/api/lab'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const labOptions = ref([])
const selectedLabId = ref(null)
const anomalyList = ref([])
const selectedIds = ref([])
const batchStatus = ref('')
const batchRemark = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showLabSelector = computed(() => userStore.userInfo?.role === 'super_admin')
const labId = computed(() => selectedLabId.value || userStore.userInfo?.labId)

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const loadAnomalies = async () => {
  if (!labId.value) return
  try {
    const res = await getAttendanceAnomalyList({
      labId: labId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    const data = res.data?.data || res.data || {}
    anomalyList.value = data.records || []
    total.value = data.total || 0
  } catch { /* ignore */ }
}

const handleBatch = async () => {
  if (!batchStatus.value) {
    ElMessage.warning('请选择目标状态')
    return
  }
  if (selectedIds.value.length === 0) return
  try {
    await handleAttendanceAnomaly({
      recordIds: selectedIds.value,
      targetStatus: batchStatus.value,
      remark: batchRemark.value
    })
    ElMessage.success('处理成功')
    batchStatus.value = ''
    batchRemark.value = ''
    selectedIds.value = []
    await loadAnomalies()
  } catch {
    ElMessage.error('处理失败')
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
  if (labId.value) loadAnomalies()
})
</script>

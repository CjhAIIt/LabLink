<template>
  <div class="page-shell equipment-apply">
    <TablePageCard title="设备借用申请" subtitle="实验室设备查询与借用" :count-label="`${pagination.total} 台`">
      <template #filters>
        <SearchToolbar
          v-model="searchForm.name"
          keyword-label="设备名称"
          placeholder="请输入设备名称"
          search-text="搜索"
          reset-text="重置"
          @search="handleSearch"
          @reset="resetSearch"
        >
          <el-form-item label="实验室">
            <el-select v-model="searchForm.labId" placeholder="选择实验室" @change="handleLabChange">
              <el-option v-for="lab in labs" :key="lab.id" :label="lab.labName" :value="lab.id" />
            </el-select>
          </el-form-item>
        </SearchToolbar>
      </template>

      <el-table :data="equipmentList" border stripe>
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <el-image 
              v-if="scope.row.imageUrl" 
              :src="scope.row.imageUrl" 
              :preview-src-list="[scope.row.imageUrl]"
              style="width: 50px; height: 50px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <StatusTag :value="scope.row.status" :label-map="equipmentStatusLabels" :type-map="equipmentStatusTypes" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              :disabled="scope.row.status !== 0"
              @click="showApplyDialog(scope.row)"
            >
              申请借用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchEquipment"
        />
      </template>
    </TablePageCard>

    <el-dialog v-model="dialog.visible" title="借用申请" width="30%">
      <el-form ref="formRef" :model="form" label-width="80px" :rules="rules">
        <el-form-item label="设备名称">
          <el-input v-model="dialog.equipmentName" disabled />
        </el-form-item>
        <el-form-item label="借用理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入借用理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialog.visible = false">取消</el-button>
          <el-button type="primary" @click="submitApply">提交申请</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEquipmentList, borrowEquipment } from '@/api/equipment'
import { getAllLabs } from '@/api/lab'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'

const searchForm = reactive({
  labId: '',
  name: ''
})
const equipmentStatusLabels = {
  0: '空闲',
  1: '借用中',
  2: '维修中'
}
const equipmentStatusTypes = {
  0: 'success',
  1: 'warning',
  2: 'danger'
}

const labs = ref([])
const equipmentList = ref([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialog = reactive({ visible: false, equipmentName: '' })
const form = reactive({ equipmentId: '', reason: '' })
const rules = {
  reason: [{ required: true, message: '请输入借用理由', trigger: 'blur' }]
}
const formRef = ref(null)

onMounted(async () => {
  const res = await getAllLabs()
  if (res.code === 200) {
    labs.value = res.data.records
    if (labs.value.length > 0) {
      searchForm.labId = labs.value[0].id
      fetchEquipment()
    }
  }
})

const fetchEquipment = async () => {
  const res = await getEquipmentList({
    pageNum: pagination.current,
    pageSize: pagination.size,
    labId: searchForm.labId,
    name: searchForm.name
  })
  if (res.code === 200) {
    equipmentList.value = res.data.records
    pagination.total = res.data.total
  }
}

const handleLabChange = () => {
  pagination.current = 1
  fetchEquipment()
}

const handleSearch = () => {
  pagination.current = 1
  fetchEquipment()
}

const resetSearch = () => {
  searchForm.name = ''
  pagination.current = 1
  fetchEquipment()
}

const showApplyDialog = (row) => {
  dialog.visible = true
  dialog.equipmentName = row.name
  form.equipmentId = row.id
  form.reason = ''
}

const submitApply = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const res = await borrowEquipment(form)
      if (res.code === 200) {
        ElMessage.success('申请提交成功')
        dialog.visible = false
        fetchEquipment()
      } else {
        ElMessage.error(res.message || '申请失败')
      }
    }
  })
}

</script>

<style scoped>
.equipment-apply :deep(.el-image) {
  border-radius: 10px;
}
</style>

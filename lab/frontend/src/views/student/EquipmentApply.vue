<template>
  <div class="equipment-apply">
    <el-card>
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="实验室">
            <el-select v-model="searchForm.labId" placeholder="选择实验室" @change="fetchEquipment">
              <el-option v-for="lab in labs" :key="lab.id" :label="lab.labName" :value="lab.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备名称">
            <el-input v-model="searchForm.name" placeholder="请输入设备名称" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchEquipment">搜索</el-button>
          </el-form-item>
        </el-form>
      </div>

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
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
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

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchEquipment"
        />
      </div>
    </el-card>

    <!-- 申请对话框 -->
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

const searchForm = reactive({
  labId: '',
  name: ''
})

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

const getStatusType = (status) => {
  return status === 0 ? 'success' : status === 1 ? 'warning' : 'danger'
}
const getStatusText = (status) => {
  return status === 0 ? '空闲' : status === 1 ? '借用中' : '维修中'
}
</script>

<style scoped>
.equipment-apply { padding: 20px; }
.search-bar { margin-bottom: 20px; }
.pagination { margin-top: 20px; text-align: right; }
</style>

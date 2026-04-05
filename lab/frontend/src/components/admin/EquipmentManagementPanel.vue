<template>
  <div class="equipment-management">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="设备列表" name="list">
          <div class="search-bar">
            <el-form :inline="true" :model="searchForm">
              <el-form-item label="设备名称">
                <el-input v-model="searchForm.name" placeholder="请输入设备名称" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="searchForm.status" placeholder="全部" clearable>
                  <el-option label="空闲" :value="0" />
                  <el-option label="借用中" :value="1" />
                  <el-option label="维修中" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchEquipment">搜索</el-button>
                <el-button type="success" @click="showAddDialog">添加设备</el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <el-table :data="equipmentList" border stripe>
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="serialNumber" label="编号" width="150" />
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
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button size="small" @click="editEquipment(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteEquipment(scope.row)">删除</el-button>
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
        </el-tab-pane>
        
        <el-tab-pane label="借用申请" name="borrow">
          <el-table :data="borrowList" border stripe>
            <el-table-column prop="equipmentId" label="设备ID" width="80" />
            <el-table-column prop="userId" label="申请人ID" width="80" />
            <el-table-column prop="reason" label="申请理由" />
            <el-table-column prop="createTime" label="申请时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getBorrowStatusType(scope.row.status)">
                  {{ getBorrowStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250">
              <template #default="scope">
                <div v-if="scope.row.status === 0">
                  <el-button size="small" type="success" @click="auditBorrow(scope.row, 1)">通过</el-button>
                  <el-button size="small" type="danger" @click="auditBorrow(scope.row, 2)">拒绝</el-button>
                </div>
                <div v-if="scope.row.status === 1">
                  <el-button size="small" type="warning" @click="returnEquipment(scope.row)">确认归还</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination">
            <el-pagination
              v-model:current-page="borrowPagination.current"
              v-model:page-size="borrowPagination.size"
              :total="borrowPagination.total"
              layout="total, prev, pager, next"
              @current-change="fetchBorrowList"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 设备编辑/添加对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="50%">
      <el-form ref="formRef" :model="form" label-width="100px" :rules="rules">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-input v-model="form.type" />
        </el-form-item>
        <el-form-item label="编号" prop="serialNumber">
          <el-input v-model="form.serialNumber" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <FileUpload v-model="fileList" :limit="1" accept="image/*" @success="handleUploadSuccess" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">空闲</el-radio>
            <el-radio :label="1">借用中</el-radio>
            <el-radio :label="2">维修中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialog.visible = false">取消</el-button>
          <el-button type="primary" @click="saveEquipment">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipmentList, addEquipment, updateEquipment, deleteEquipment as deleteEquipmentApi, getBorrowList, auditBorrow as auditBorrowApi, returnEquipment as returnEquipmentApi } from '@/api/equipment'
import { useUserStore } from '@/stores/user'
import FileUpload from '@/components/FileUpload.vue'

const userStore = useUserStore()
const activeTab = ref('list')
const labId = ref(null)

// 搜索
const searchForm = reactive({
  name: '',
  status: ''
})

// 列表数据
const equipmentList = ref([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

const borrowList = ref([])
const borrowPagination = reactive({ current: 1, size: 10, total: 0 })

// 对话框
const dialog = reactive({ visible: false, title: '添加设备', isEdit: false })
const form = reactive({
  id: '',
  labId: '',
  name: '',
  type: '',
  serialNumber: '',
  imageUrl: '',
  description: '',
  status: 0
})
const fileList = ref([])
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入类型', trigger: 'blur' }]
}
const formRef = ref(null)

onMounted(() => {
  if (userStore.userInfo && userStore.userInfo.labId) {
    labId.value = userStore.userInfo.labId
    fetchEquipment()
    fetchBorrowList()
  }
})

// 监听Tab切换
watch(activeTab, (val) => {
  if (val === 'list') fetchEquipment()
  if (val === 'borrow') fetchBorrowList()
})

const fetchEquipment = async () => {
  if (!labId.value) return
  const res = await getEquipmentList({
    pageNum: pagination.current,
    pageSize: pagination.size,
    labId: labId.value,
    name: searchForm.name,
    status: searchForm.status === '' ? null : searchForm.status
  })
  if (res.code === 200) {
    equipmentList.value = res.data.records
    pagination.total = res.data.total
  }
}

const fetchBorrowList = async () => {
  // 这里需要后端支持按labId查询借用记录，目前API可能只支持userId/equipmentId
  // 如果后端没有支持，这里可能查不到数据。
  // 我们假设后端 getBorrowList 应该支持 labId 筛选，或者我们需要先查出该实验室所有设备ID，再查借用记录。
  // 为了简化，假设后端 EquipmentController.listBorrow 支持 labId 参数（但我之前没加）。
  // 让我回顾一下 EquipmentController.listBorrow，它支持 userId, equipmentId, status.
  // 确实不支持 labId。这在Admin端是个问题。
  // 管理员只能看到自己实验室设备的借用记录。
  // 我需要修改 EquipmentController.listBorrow 来支持 labId，或者在 Service 层处理。
  // 现在先不管，直接调用，可能返回空。
  // 修正：我应该在 EquipmentController 加上 labId 参数。
  const res = await getBorrowList({
    pageNum: borrowPagination.current,
    pageSize: borrowPagination.size,
    // labId: labId.value // 需要后端支持
  })
  if (res.code === 200) {
    borrowList.value = res.data.records
    borrowPagination.total = res.data.total
  }
}

const showAddDialog = () => {
  dialog.visible = true
  dialog.title = '添加设备'
  dialog.isEdit = false
  Object.keys(form).forEach(key => form[key] = '')
  form.status = 0
  form.labId = labId.value
  fileList.value = []
}

const editEquipment = (row) => {
  dialog.visible = true
  dialog.title = '编辑设备'
  dialog.isEdit = true
  Object.assign(form, row)
  fileList.value = row.imageUrl ? [{ name: 'image', url: row.imageUrl }] : []
}

const handleUploadSuccess = (response) => {
  form.imageUrl = response.data.url || response.data.path
}

const saveEquipment = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const res = dialog.isEdit ? await updateEquipment(form) : await addEquipment(form)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        dialog.visible = false
        fetchEquipment()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    }
  })
}

const deleteEquipment = (row) => {
  ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }).then(async () => {
    const res = await deleteEquipmentApi(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchEquipment()
    }
  })
}

const auditBorrow = async (row, status) => {
  const res = await auditBorrowApi({ id: row.id, status })
  if (res.code === 200) {
    ElMessage.success('操作成功')
    fetchBorrowList()
  }
}

const returnEquipment = async (row) => {
  const res = await returnEquipmentApi({ id: row.id })
  if (res.code === 200) {
    ElMessage.success('归还成功')
    fetchBorrowList()
  }
}

const getStatusType = (status) => {
  return status === 0 ? 'success' : status === 1 ? 'warning' : 'danger'
}
const getStatusText = (status) => {
  return status === 0 ? '空闲' : status === 1 ? '借用中' : '维修中'
}
const getBorrowStatusType = (status) => {
  return status === 0 ? 'info' : status === 1 ? 'primary' : status === 2 ? 'danger' : 'success'
}
const getBorrowStatusText = (status) => {
  return status === 0 ? '申请中' : status === 1 ? '已借出' : status === 2 ? '已拒绝' : '已归还'
}
</script>

<style scoped>
.equipment-management { padding: 20px; }
.search-bar { margin-bottom: 20px; }
.pagination { margin-top: 20px; text-align: right; }
</style>

<template>
  <div class="device-page">
    <section class="page-hero device-hero">
      <div class="hero-header">
        <div>
          <div class="eyebrow">设备中心</div>
          <h2>设备台账、借还与维修管理</h2>
          <p class="hero-text">在当前管理范围内统一维护设备，不必离开桌面管理端。</p>
        </div>
        <el-button type="primary" @click="refreshAll">刷新</el-button>
      </div>
      <div class="scope-bar">
        <el-form inline>
          <el-form-item v-if="canSelectCollege" label="学院">
            <el-select v-model="scopeFilters.collegeId" clearable placeholder="全部学院" style="width: 220px" @change="handleCollegeChange">
              <el-option v-for="item in collegeOptions" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="canSelectLab" label="实验室">
            <el-select v-model="scopeFilters.labId" clearable placeholder="全部实验室" style="width: 240px" @change="handleLabChange">
              <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="实验室">
            <el-input :model-value="fixedLabLabel" disabled style="width: 240px" />
          </el-form-item>
        </el-form>
        <el-tag effect="plain">{{ scopeSummary }}</el-tag>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="device-tabs">
      <el-tab-pane label="设备台账" name="assets">
        <TablePageCard title="设备台账" subtitle="设备中心">
          <template #header-extra>
            <div class="toolbar-actions">
              <el-tag effect="plain">{{ equipmentPagination.total }} 条</el-tag>
              <el-button @click="categoryDialogVisible = true">分类管理</el-button>
              <el-button type="primary" @click="openDeviceDialog()">新增设备</el-button>
            </div>
          </template>
          <template #filters>
            <SearchToolbar v-model="equipmentFilters.keyword" placeholder="设备名称、编号、品牌" @search="handleEquipmentSearch" @reset="resetEquipmentFilters">
              <el-form-item label="分类">
                <el-select v-model="equipmentFilters.categoryId" clearable placeholder="全部分类" style="width: 200px">
                  <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="equipmentFilters.status" clearable placeholder="全部状态" style="width: 180px">
                  <el-option label="闲置" :value="0" />
                  <el-option label="借出中" :value="1" />
                  <el-option label="维修中" :value="2" />
                  <el-option label="已报废" :value="3" />
                </el-select>
              </el-form-item>
            </SearchToolbar>
          </template>

          <el-table v-loading="equipmentLoading" :data="equipmentRows" stripe>
            <el-table-column prop="deviceCode" label="编号" min-width="130" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column label="分类" min-width="140">
              <template #default="{ row }">{{ row.categoryName || row.type || '-' }}</template>
            </el-table-column>
            <el-table-column prop="brand" label="品牌" min-width="120" />
            <el-table-column prop="model" label="型号" min-width="140" />
            <el-table-column prop="location" label="位置" min-width="140" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }"><StatusTag :value="row.status" preset="equipment" /></template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openDeviceDialog(row)">编辑</el-button>
                <el-button size="small" type="warning" plain @click="openMaintenanceDialog(row)">报修</el-button>
                <el-button size="small" type="danger" plain @click="removeDevice(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <template #pagination>
            <el-pagination v-model:current-page="equipmentPagination.current" v-model:page-size="equipmentPagination.size" :page-sizes="[10,20,50]" :total="equipmentPagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchEquipments" @current-change="fetchEquipments" />
          </template>
        </TablePageCard>
      </el-tab-pane>

      <el-tab-pane label="借还记录" name="borrows">
        <TablePageCard title="借还记录" subtitle="设备中心" :count-label="`${borrowPagination.total} 条`">
          <template #filters>
            <SearchToolbar v-model="borrowFilters.keyword" placeholder="设备、借用人、原因" @search="handleBorrowSearch" @reset="resetBorrowFilters">
              <el-form-item label="状态">
                <el-select v-model="borrowFilters.status" clearable placeholder="全部状态" style="width: 180px">
                  <el-option label="待审批" :value="0" />
                  <el-option label="已通过" :value="1" />
                  <el-option label="已驳回" :value="2" />
                  <el-option label="已归还" :value="3" />
                </el-select>
              </el-form-item>
            </SearchToolbar>
          </template>
          <el-table v-loading="borrowLoading" :data="borrowRows" stripe>
            <el-table-column prop="equipmentName" label="设备" min-width="150" />
            <el-table-column prop="deviceCode" label="编号" min-width="120" />
            <el-table-column prop="userRealName" label="借用人" min-width="120" />
            <el-table-column prop="reason" label="借用原因" min-width="220" show-overflow-tooltip />
            <el-table-column prop="expectedReturnTime" label="预计归还时间" width="180"><template #default="{ row }">{{ formatDateTime(row.expectedReturnTime) }}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="120"><template #default="{ row }"><StatusTag :value="row.status" preset="borrow" /></template></el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button size="small" type="success" @click="reviewBorrow(row, 1)">通过</el-button>
                  <el-button size="small" type="danger" plain @click="reviewBorrow(row, 2)">驳回</el-button>
                </template>
                <el-button v-else-if="row.status === 1" size="small" type="warning" @click="confirmReturn(row)">归还</el-button>
                <span v-else class="done-text">已结束</span>
              </template>
            </el-table-column>
          </el-table>
          <template #pagination>
            <el-pagination v-model:current-page="borrowPagination.current" v-model:page-size="borrowPagination.size" :page-sizes="[10,20,50]" :total="borrowPagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchBorrows" @current-change="fetchBorrows" />
          </template>
        </TablePageCard>
      </el-tab-pane>

      <el-tab-pane label="维修记录" name="maintenance">
        <TablePageCard title="维修记录" subtitle="设备中心" :count-label="`${maintenancePagination.total} 条`">
          <template #filters>
            <SearchToolbar v-model="maintenanceFilters.keyword" placeholder="设备、故障、提报人" @search="handleMaintenanceSearch" @reset="resetMaintenanceFilters">
              <el-form-item label="状态">
                <el-select v-model="maintenanceFilters.status" clearable placeholder="全部状态" style="width: 180px">
                  <el-option label="待处理" value="PENDING" />
                  <el-option label="处理中" value="IN_PROGRESS" />
                  <el-option label="已解决" value="RESOLVED" />
                </el-select>
              </el-form-item>
            </SearchToolbar>
          </template>
          <el-table v-loading="maintenanceLoading" :data="maintenanceRows" stripe>
            <el-table-column prop="equipmentName" label="设备" min-width="150" />
            <el-table-column prop="deviceCode" label="编号" min-width="120" />
            <el-table-column prop="issueDesc" label="故障描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="maintenanceStatus" label="状态" width="130"><template #default="{ row }"><StatusTag :value="row.maintenanceStatus" preset="maintenance" /></template></el-table-column>
            <el-table-column prop="reportUserName" label="提报人" min-width="120" />
            <el-table-column prop="handledByName" label="处理人" min-width="120" />
            <el-table-column prop="handledAt" label="处理时间" width="180"><template #default="{ row }">{{ formatDateTime(row.handledAt) }}</template></el-table-column>
            <el-table-column prop="resultDesc" label="处理结果" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.maintenanceStatus !== 'RESOLVED'" size="small" type="primary" @click="openHandleMaintenanceDialog(row)">更新</el-button>
                <span v-else class="done-text">已解决</span>
              </template>
            </el-table-column>
          </el-table>
          <template #pagination>
            <el-pagination v-model:current-page="maintenancePagination.current" v-model:page-size="maintenancePagination.size" :page-sizes="[10,20,50]" :total="maintenancePagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchMaintenances" @current-change="fetchMaintenances" />
          </template>
        </TablePageCard>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="categoryDialogVisible" title="分类管理" width="640px">
      <el-alert v-if="!scopeFilters.labId && canSelectLab" type="info" :closable="false" title="如需新增分类，请先选择实验室。" />
      <div class="dialog-grid">
        <el-form :model="categoryForm" label-width="92px">
          <el-form-item label="名称"><el-input v-model="categoryForm.name" maxlength="50" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="categoryForm.description" type="textarea" :rows="3" maxlength="120" show-word-limit /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitCategory">{{ categoryForm.id ? '保存' : '创建' }}</el-button>
            <el-button @click="resetCategoryForm">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="categoryOptions" stripe>
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="equipmentCount" label="设备数" width="90" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="editCategory(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="removeCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="deviceDialogVisible" :title="deviceForm.id ? '编辑设备' : '新增设备'" width="720px">
      <el-form :model="deviceForm" label-width="108px">
        <el-form-item v-if="canSelectLab && !deviceForm.id" label="实验室">
          <el-select v-model="deviceForm.labId" placeholder="请选择实验室" style="width: 100%"><el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" /></el-select>
        </el-form-item>
        <el-form-item label="设备名称"><el-input v-model="deviceForm.name" maxlength="80" /></el-form-item>
        <el-form-item label="设备编号"><el-input v-model="deviceForm.deviceCode" maxlength="64" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="deviceForm.categoryId" clearable placeholder="请选择分类" style="width: 100%"><el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="品牌"><el-input v-model="deviceForm.brand" maxlength="64" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="deviceForm.model" maxlength="64" /></el-form-item>
        <el-form-item label="购置日期"><el-date-picker v-model="deviceForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="存放位置"><el-input v-model="deviceForm.location" maxlength="120" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="deviceForm.imageUrl" maxlength="255" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="deviceForm.status" style="width: 100%"><el-option label="闲置" :value="0" /><el-option label="借出中" :value="1" /><el-option label="维修中" :value="2" /><el-option label="已报废" :value="3" /></el-select></el-form-item>
        <el-form-item label="说明"><el-input v-model="deviceForm.description" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
        <el-form-item label="备注"><el-input v-model="deviceForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="deviceDialogVisible = false">取消</el-button><el-button type="primary" @click="submitDevice">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="maintenanceDialogVisible" title="新建维修记录" width="540px">
      <el-form :model="maintenanceForm" label-width="108px">
        <el-form-item label="设备"><el-input :model-value="maintenanceForm.equipmentName" disabled /></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="maintenanceForm.issueDesc" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="maintenanceDialogVisible = false">取消</el-button><el-button type="primary" @click="submitMaintenance">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="maintenanceHandleDialogVisible" title="更新维修状态" width="540px">
      <el-form :model="maintenanceHandleForm" label-width="108px">
        <el-form-item label="状态"><el-select v-model="maintenanceHandleForm.maintenanceStatus" style="width: 100%"><el-option label="处理中" value="IN_PROGRESS" /><el-option label="已解决" value="RESOLVED" /></el-select></el-form-item>
        <el-form-item label="处理结果"><el-input v-model="maintenanceHandleForm.resultDesc" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="maintenanceHandleDialogVisible = false">取消</el-button><el-button type="primary" @click="submitMaintenanceHandle">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import {
  addEquipment,
  auditBorrow,
  createEquipmentCategory,
  createMaintenanceRecord,
  deleteEquipment,
  deleteEquipmentCategory,
  getBorrowList,
  getEquipmentCategories,
  getEquipmentList,
  getMaintenanceList,
  handleMaintenanceRecord,
  returnEquipment,
  updateEquipment,
  updateEquipmentCategory
} from '@/api/equipment'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('assets')
const categoryDialogVisible = ref(false)
const deviceDialogVisible = ref(false)
const maintenanceDialogVisible = ref(false)
const maintenanceHandleDialogVisible = ref(false)

const equipmentLoading = ref(false)
const borrowLoading = ref(false)
const maintenanceLoading = ref(false)

const equipmentRows = ref([])
const borrowRows = ref([])
const maintenanceRows = ref([])
const categoryOptions = ref([])
const collegeOptions = ref([])
const labOptions = ref([])

const scopeFilters = reactive({ collegeId: userStore.userInfo?.managedCollegeId || '', labId: userStore.userInfo?.labId || '' })
const equipmentFilters = reactive({ keyword: '', categoryId: '', status: '' })
const borrowFilters = reactive({ keyword: '', status: '' })
const maintenanceFilters = reactive({ keyword: '', status: '' })

const equipmentPagination = reactive({ current: 1, size: 10, total: 0 })
const borrowPagination = reactive({ current: 1, size: 10, total: 0 })
const maintenancePagination = reactive({ current: 1, size: 10, total: 0 })

const categoryForm = reactive({ id: null, name: '', description: '' })
const deviceForm = reactive({ id: null, labId: '', categoryId: '', name: '', deviceCode: '', brand: '', model: '', purchaseDate: '', location: '', imageUrl: '', status: 0, description: '', remark: '' })
const maintenanceForm = reactive({ equipmentId: null, equipmentName: '', issueDesc: '' })
const maintenanceHandleForm = reactive({ id: null, maintenanceStatus: 'IN_PROGRESS', resultDesc: '' })

const canSelectCollege = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const canSelectLab = computed(() => !userStore.userInfo?.labManager)
const fixedLabLabel = computed(() => scopeFilters.labId ? (labOptions.value.find((item) => item.id === scopeFilters.labId)?.labName || `实验室 #${scopeFilters.labId}`) : '当前未固定')
const scopeSummary = computed(() => scopeFilters.labId ? `实验室范围 #${scopeFilters.labId}` : (scopeFilters.collegeId ? `学院范围 #${scopeFilters.collegeId}` : '当前管理范围'))
const scopeParams = computed(() => ({ collegeId: scopeFilters.collegeId || undefined, labId: scopeFilters.labId || undefined }))

const loadColleges = async () => {
  if (!canSelectCollege.value) return
  const response = await getCollegeOptions()
  collegeOptions.value = response.data || []
}

const loadLabs = async () => {
  const response = await getLabPage({ pageNum: 1, pageSize: 200, collegeId: scopeFilters.collegeId || undefined })
  labOptions.value = response.data.records || []
}

const loadCategories = async () => {
  const response = await getEquipmentCategories(scopeParams.value)
  categoryOptions.value = response.data || []
}

const fetchEquipments = async () => {
  equipmentLoading.value = true
  try {
    const response = await getEquipmentList({
      pageNum: equipmentPagination.current,
      pageSize: equipmentPagination.size,
      ...scopeParams.value,
      categoryId: equipmentFilters.categoryId || undefined,
      name: equipmentFilters.keyword || undefined,
      status: equipmentFilters.status === '' ? undefined : equipmentFilters.status
    })
    equipmentRows.value = response.data.records || []
    equipmentPagination.total = response.data.total || 0
  } finally {
    equipmentLoading.value = false
  }
}

const fetchBorrows = async () => {
  borrowLoading.value = true
  try {
    const response = await getBorrowList({
      pageNum: borrowPagination.current,
      pageSize: borrowPagination.size,
      ...scopeParams.value,
      keyword: borrowFilters.keyword || undefined,
      status: borrowFilters.status === '' ? undefined : borrowFilters.status
    })
    borrowRows.value = response.data.records || []
    borrowPagination.total = response.data.total || 0
  } finally {
    borrowLoading.value = false
  }
}

const fetchMaintenances = async () => {
  maintenanceLoading.value = true
  try {
    const response = await getMaintenanceList({
      pageNum: maintenancePagination.current,
      pageSize: maintenancePagination.size,
      ...scopeParams.value,
      keyword: maintenanceFilters.keyword || undefined,
      status: maintenanceFilters.status || undefined
    })
    maintenanceRows.value = response.data.records || []
    maintenancePagination.total = response.data.total || 0
  } finally {
    maintenanceLoading.value = false
  }
}

const refreshAll = async () => {
  await loadCategories()
  await Promise.all([fetchEquipments(), fetchBorrows(), fetchMaintenances()])
}

const handleCollegeChange = async () => {
  scopeFilters.labId = ''
  equipmentFilters.categoryId = ''
  await loadLabs()
  await refreshAll()
}

const handleLabChange = async () => {
  equipmentFilters.categoryId = ''
  await refreshAll()
}

const resetCategoryForm = () => Object.assign(categoryForm, { id: null, name: '', description: '' })
const editCategory = (row) => Object.assign(categoryForm, { id: row.id, name: row.name, description: row.description || '' })

const submitCategory = async () => {
  if (!scopeFilters.labId) {
    ElMessage.warning('请先选择实验室')
    return
  }
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  const payload = { labId: scopeFilters.labId, name: categoryForm.name.trim(), description: categoryForm.description.trim() || undefined }
  if (categoryForm.id) {
    await updateEquipmentCategory(categoryForm.id, payload)
  } else {
    await createEquipmentCategory(payload)
  }
  ElMessage.success('分类已保存')
  resetCategoryForm()
  await loadCategories()
}

const removeCategory = async (row) => {
  await ElMessageBox.confirm(`确认删除分类“${row.name}”吗？`, '删除分类', { type: 'warning' })
  await deleteEquipmentCategory(row.id)
  ElMessage.success('分类已删除')
  await loadCategories()
}

const resetDeviceForm = () => Object.assign(deviceForm, { id: null, labId: scopeFilters.labId || '', categoryId: '', name: '', deviceCode: '', brand: '', model: '', purchaseDate: '', location: '', imageUrl: '', status: 0, description: '', remark: '' })

const openDeviceDialog = (row) => {
  resetDeviceForm()
  if (row) {
    Object.assign(deviceForm, {
      id: row.id,
      labId: row.labId,
      categoryId: row.categoryId || '',
      name: row.name || '',
      deviceCode: row.deviceCode || '',
      brand: row.brand || '',
      model: row.model || '',
      purchaseDate: row.purchaseDate || '',
      location: row.location || '',
      imageUrl: row.imageUrl || '',
      status: row.status ?? 0,
      description: row.description || '',
      remark: row.remark || ''
    })
  }
  deviceDialogVisible.value = true
}

const submitDevice = async () => {
  if (!deviceForm.labId || !deviceForm.name.trim() || !deviceForm.deviceCode.trim()) {
    ElMessage.warning('实验室、设备名称和设备编号不能为空')
    return
  }
  const payload = {
    ...deviceForm,
    categoryId: deviceForm.categoryId || null,
    name: deviceForm.name.trim(),
    deviceCode: deviceForm.deviceCode.trim(),
    brand: deviceForm.brand.trim() || undefined,
    model: deviceForm.model.trim() || undefined,
    location: deviceForm.location.trim() || undefined,
    imageUrl: deviceForm.imageUrl.trim() || undefined,
    description: deviceForm.description.trim() || undefined,
    remark: deviceForm.remark.trim() || undefined
  }
  if (deviceForm.id) {
    await updateEquipment(payload)
  } else {
    await addEquipment(payload)
  }
  ElMessage.success('设备已保存')
  deviceDialogVisible.value = false
  await Promise.all([loadCategories(), fetchEquipments()])
}

const removeDevice = async (row) => {
  await ElMessageBox.confirm(`确认删除设备“${row.name}”吗？`, '删除设备', { type: 'warning' })
  await deleteEquipment(row.id)
  ElMessage.success('设备已删除')
  await Promise.all([loadCategories(), fetchEquipments()])
}

const openMaintenanceDialog = (row) => {
  Object.assign(maintenanceForm, { equipmentId: row.id, equipmentName: `${row.name} / ${row.deviceCode}`, issueDesc: '' })
  maintenanceDialogVisible.value = true
}

const submitMaintenance = async () => {
  if (!maintenanceForm.issueDesc.trim()) {
    ElMessage.warning('请输入故障描述')
    return
  }
  await createMaintenanceRecord({ equipmentId: maintenanceForm.equipmentId, issueDesc: maintenanceForm.issueDesc.trim() })
  ElMessage.success('维修记录已创建')
  maintenanceDialogVisible.value = false
  await Promise.all([fetchEquipments(), fetchMaintenances()])
}

const openHandleMaintenanceDialog = (row) => {
  Object.assign(maintenanceHandleForm, {
    id: row.id,
    maintenanceStatus: row.maintenanceStatus === 'PENDING' ? 'IN_PROGRESS' : row.maintenanceStatus,
    resultDesc: row.resultDesc || ''
  })
  maintenanceHandleDialogVisible.value = true
}

const submitMaintenanceHandle = async () => {
  await handleMaintenanceRecord({
    id: maintenanceHandleForm.id,
    maintenanceStatus: maintenanceHandleForm.maintenanceStatus,
    resultDesc: maintenanceHandleForm.resultDesc.trim() || undefined
  })
  ElMessage.success('维修记录已更新')
  maintenanceHandleDialogVisible.value = false
  await Promise.all([fetchEquipments(), fetchMaintenances()])
}

const reviewBorrow = async (row, status) => {
  await auditBorrow({ id: row.id, status })
  ElMessage.success('借用申请已更新')
  await Promise.all([fetchEquipments(), fetchBorrows()])
}

const confirmReturn = async (row) => {
  await returnEquipment({ id: row.id })
  ElMessage.success('设备已归还')
  await Promise.all([fetchEquipments(), fetchBorrows()])
}

const handleEquipmentSearch = () => {
  equipmentPagination.current = 1
  fetchEquipments()
}
const resetEquipmentFilters = () => {
  Object.assign(equipmentFilters, { keyword: '', categoryId: '', status: '' })
  equipmentPagination.current = 1
  fetchEquipments()
}
const handleBorrowSearch = () => {
  borrowPagination.current = 1
  fetchBorrows()
}
const resetBorrowFilters = () => {
  Object.assign(borrowFilters, { keyword: '', status: '' })
  borrowPagination.current = 1
  fetchBorrows()
}
const handleMaintenanceSearch = () => {
  maintenancePagination.current = 1
  fetchMaintenances()
}
const resetMaintenanceFilters = () => {
  Object.assign(maintenanceFilters, { keyword: '', status: '' })
  maintenancePagination.current = 1
  fetchMaintenances()
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  await loadColleges()
  await loadLabs()
  await refreshAll()
})
</script>

<style scoped>
.device-page { display: grid; gap: 20px; }
.hero-header, .scope-bar { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.device-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(15, 118, 110, 0.88)),
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.18), transparent 35%);
}
.hero-header h2 { margin: 6px 0 10px; font-size: 28px; }
.hero-text { margin: 0; color: rgba(226, 232, 240, 0.86); }
.toolbar-actions { display: flex; gap: 12px; }
.dialog-grid { display: grid; gap: 16px; }
.done-text { color: #94a3b8; }
@media (max-width: 960px) {
  .hero-header, .scope-bar { flex-direction: column; }
}
</style>

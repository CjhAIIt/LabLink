<template>
  <div class="m-page">
    <section class="toolbar-card scope-toolbar">
      <el-select
        v-if="canSelectCollege"
        v-model="scopeFilters.collegeId"
        clearable
        placeholder="学院"
        style="width: 120px"
        @change="handleCollegeChange"
      >
        <el-option v-for="item in collegeOptions" :key="item.id" :label="item.collegeName" :value="item.id" />
      </el-select>
      <el-select
        v-if="canSelectLab"
        v-model="scopeFilters.labId"
        clearable
        placeholder="实验室"
        style="width: 140px"
        @change="handleLabChange"
      >
        <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
      </el-select>
      <el-input v-else :model-value="fixedLabLabel" disabled />
      <el-button plain :loading="refreshing" @click="refreshAll">刷新</el-button>
    </section>

    <section class="metric-grid">
      <article v-for="card in summaryCards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.tip }}</small>
      </article>
    </section>

    <section class="tab-row">
      <button class="tab-btn" :class="{ active: activeTab === 'assets' }" type="button" @click="activeTab = 'assets'">设备台账</button>
      <button class="tab-btn" :class="{ active: activeTab === 'borrows' }" type="button" @click="activeTab = 'borrows'">借还记录</button>
      <button class="tab-btn" :class="{ active: activeTab === 'maintenance' }" type="button" @click="activeTab = 'maintenance'">维修记录</button>
    </section>

    <section v-if="activeTab === 'assets'" class="panel-card">
      <div class="toolbar-card compact">
        <el-input v-model="equipmentFilters.keyword" clearable placeholder="搜索设备名称、编号或品牌" @keyup.enter="resetEquipments" />
        <el-select v-model="equipmentFilters.categoryId" clearable placeholder="分类" style="width: 110px" @change="resetEquipments">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="equipmentFilters.status" clearable placeholder="状态" style="width: 110px" @change="resetEquipments">
          <el-option label="闲置" :value="0" />
          <el-option label="借出中" :value="1" />
          <el-option label="维修中" :value="2" />
          <el-option label="已报废" :value="3" />
        </el-select>
      </div>
      <div class="action-row top-gap">
        <el-button plain @click="categoryDialogVisible = true">分类管理</el-button>
        <el-button type="primary" @click="openDeviceDialog()">新增设备</el-button>
      </div>

      <div class="card-list top-gap" v-loading="equipmentLoading">
        <article v-for="row in equipmentRows" :key="row.id" class="record-card">
          <div class="card-head">
            <div>
              <strong>{{ row.name || '-' }}</strong>
              <p>{{ row.deviceCode || '-' }} · {{ row.categoryName || row.type || '未分类' }}</p>
            </div>
            <span class="status-pill" :class="equipmentStatusClass(row.status)">{{ equipmentStatusLabel(row.status) }}</span>
          </div>
          <div class="meta-grid">
            <div>
              <span>品牌 / 型号</span>
              <strong>{{ [row.brand, row.model].filter(Boolean).join(' / ') || '-' }}</strong>
            </div>
            <div>
              <span>存放位置</span>
              <strong>{{ row.location || '-' }}</strong>
            </div>
            <div>
              <span>更新时间</span>
              <strong>{{ formatDateTime(row.updateTime) }}</strong>
            </div>
            <div>
              <span>实验室</span>
              <strong>{{ row.labName || fixedLabLabel }}</strong>
            </div>
          </div>
          <div class="section-block" v-if="row.description || row.remark">
            <label>说明</label>
            <div>{{ row.description || row.remark }}</div>
          </div>
          <div class="action-row">
            <el-button text type="primary" @click="openDeviceDialog(row)">编辑</el-button>
            <el-button text type="warning" @click="openMaintenanceDialog(row)">报修</el-button>
            <el-button text type="danger" @click="removeDevice(row)">删除</el-button>
          </div>
        </article>
        <el-empty v-if="!equipmentLoading && equipmentRows.length === 0" description="暂无设备数据" :image-size="80" />
      </div>
      <div class="load-more">
        <el-button v-if="hasMoreAssets" plain :loading="equipmentLoadingMore" @click="loadMoreEquipments">加载更多</el-button>
        <span v-else-if="equipmentRows.length" class="muted">已经到底了</span>
      </div>
    </section>

    <section v-if="activeTab === 'borrows'" class="panel-card">
      <div class="toolbar-card compact">
        <el-input v-model="borrowFilters.keyword" clearable placeholder="搜索设备、借用人或原因" @keyup.enter="resetBorrows" />
        <el-select v-model="borrowFilters.status" clearable placeholder="状态" style="width: 120px" @change="resetBorrows">
          <el-option label="待审批" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="已归还" :value="3" />
        </el-select>
      </div>

      <div class="card-list top-gap" v-loading="borrowLoading">
        <article v-for="row in borrowRows" :key="row.id" class="record-card">
          <div class="card-head">
            <div>
              <strong>{{ row.equipmentName || '-' }}</strong>
              <p>{{ row.deviceCode || '-' }} · {{ row.userRealName || '-' }}</p>
            </div>
            <span class="status-pill" :class="borrowStatusClass(row.status)">{{ borrowStatusLabel(row.status) }}</span>
          </div>
          <div class="meta-grid">
            <div>
              <span>借用原因</span>
              <strong>{{ row.reason || '-' }}</strong>
            </div>
            <div>
              <span>预计归还</span>
              <strong>{{ formatDateTime(row.expectedReturnTime) }}</strong>
            </div>
            <div>
              <span>提交时间</span>
              <strong>{{ formatDateTime(row.createTime) }}</strong>
            </div>
            <div>
              <span>实验室</span>
              <strong>{{ row.labName || fixedLabLabel }}</strong>
            </div>
          </div>
          <div class="action-row">
            <el-button v-if="Number(row.status) === 0" text type="success" @click="reviewBorrow(row, 1)">通过</el-button>
            <el-button v-if="Number(row.status) === 0" text type="danger" @click="reviewBorrow(row, 2)">驳回</el-button>
            <el-button v-if="Number(row.status) === 1" text type="warning" @click="confirmReturn(row)">归还</el-button>
          </div>
        </article>
        <el-empty v-if="!borrowLoading && borrowRows.length === 0" description="暂无借还记录" :image-size="80" />
      </div>
      <div class="load-more">
        <el-button v-if="hasMoreBorrows" plain :loading="borrowLoadingMore" @click="loadMoreBorrows">加载更多</el-button>
        <span v-else-if="borrowRows.length" class="muted">已经到底了</span>
      </div>
    </section>

    <section v-if="activeTab === 'maintenance'" class="panel-card">
      <div class="toolbar-card compact">
        <el-input v-model="maintenanceFilters.keyword" clearable placeholder="搜索设备、故障或提报人" @keyup.enter="resetMaintenances" />
        <el-select v-model="maintenanceFilters.status" clearable placeholder="状态" style="width: 120px" @change="resetMaintenances">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="IN_PROGRESS" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
      </div>

      <div class="card-list top-gap" v-loading="maintenanceLoading">
        <article v-for="row in maintenanceRows" :key="row.id" class="record-card">
          <div class="card-head">
            <div>
              <strong>{{ row.equipmentName || '-' }}</strong>
              <p>{{ row.deviceCode || '-' }} · {{ row.reportUserName || '-' }}</p>
            </div>
            <span class="status-pill" :class="maintenanceStatusClass(row.maintenanceStatus)">{{ maintenanceStatusLabel(row.maintenanceStatus) }}</span>
          </div>
          <div class="meta-grid">
            <div>
              <span>故障描述</span>
              <strong>{{ row.issueDesc || '-' }}</strong>
            </div>
            <div>
              <span>处理人</span>
              <strong>{{ row.handledByName || '-' }}</strong>
            </div>
            <div>
              <span>处理时间</span>
              <strong>{{ formatDateTime(row.handledAt) }}</strong>
            </div>
            <div>
              <span>处理结果</span>
              <strong>{{ row.resultDesc || '-' }}</strong>
            </div>
          </div>
          <div class="action-row">
            <el-button v-if="row.maintenanceStatus !== 'RESOLVED'" text type="primary" @click="openHandleMaintenanceDialog(row)">更新状态</el-button>
          </div>
        </article>
        <el-empty v-if="!maintenanceLoading && maintenanceRows.length === 0" description="暂无维修记录" :image-size="80" />
      </div>
      <div class="load-more">
        <el-button v-if="hasMoreMaintenances" plain :loading="maintenanceLoadingMore" @click="loadMoreMaintenances">加载更多</el-button>
        <span v-else-if="maintenanceRows.length" class="muted">已经到底了</span>
      </div>
    </section>

    <el-dialog v-model="categoryDialogVisible" title="分类管理" width="92%">
      <el-alert v-if="!scopeFilters.labId && canSelectLab" type="info" :closable="false" title="新增分类前，请先选择实验室。" />
      <div class="drawer-body top-gap">
        <el-form label-position="top">
          <el-form-item label="分类名称">
            <el-input v-model="categoryForm.name" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="分类说明">
            <el-input v-model="categoryForm.description" type="textarea" :rows="3" maxlength="120" show-word-limit />
          </el-form-item>
          <div class="action-row">
            <el-button @click="resetCategoryForm">重置</el-button>
            <el-button type="primary" @click="submitCategory">{{ categoryForm.id ? '保存' : '创建' }}</el-button>
          </div>
        </el-form>
        <div class="card-list">
          <article v-for="row in categoryOptions" :key="row.id" class="record-card">
            <div class="card-head">
              <div>
                <strong>{{ row.name }}</strong>
                <p>{{ row.description || '暂无说明' }}</p>
              </div>
              <span class="status-pill default">{{ row.equipmentCount || 0 }} 台</span>
            </div>
            <div class="action-row">
              <el-button text type="primary" @click="editCategory(row)">编辑</el-button>
              <el-button text type="danger" @click="removeCategory(row)">删除</el-button>
            </div>
          </article>
          <el-empty v-if="categoryOptions.length === 0" description="暂无分类" :image-size="72" />
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="deviceDialogVisible" :title="deviceForm.id ? '编辑设备' : '新增设备'" size="92%">
      <div class="drawer-body">
        <el-form label-position="top">
          <el-form-item v-if="canSelectLab && !deviceForm.id" label="所属实验室">
            <el-select v-model="deviceForm.labId" placeholder="请选择实验室" style="width: 100%">
              <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="设备名称">
            <el-input v-model="deviceForm.name" maxlength="80" show-word-limit />
          </el-form-item>
          <el-form-item label="设备编号">
            <el-input v-model="deviceForm.deviceCode" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="设备分类">
            <el-select v-model="deviceForm.categoryId" clearable placeholder="请选择分类" style="width: 100%">
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="品牌">
            <el-input v-model="deviceForm.brand" maxlength="64" />
          </el-form-item>
          <el-form-item label="型号">
            <el-input v-model="deviceForm.model" maxlength="64" />
          </el-form-item>
          <el-form-item label="购置日期">
            <el-date-picker v-model="deviceForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="存放位置">
            <el-input v-model="deviceForm.location" maxlength="120" />
          </el-form-item>
          <el-form-item label="图片地址">
            <el-input v-model="deviceForm.imageUrl" maxlength="255" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="deviceForm.status" style="width: 100%">
              <el-option label="闲置" :value="0" />
              <el-option label="借出中" :value="1" />
              <el-option label="维修中" :value="2" />
              <el-option label="已报废" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="说明">
            <el-input v-model="deviceForm.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="deviceForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="action-row">
          <el-button @click="deviceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitDevice">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="maintenanceDialogVisible" title="新建维修记录" width="92%">
      <el-form label-position="top">
        <el-form-item label="设备">
          <el-input :model-value="maintenanceForm.equipmentName" disabled />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="maintenanceForm.issueDesc" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="action-row">
          <el-button @click="maintenanceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitMaintenance">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="maintenanceHandleDialogVisible" title="更新维修状态" width="92%">
      <el-form label-position="top">
        <el-form-item label="状态">
          <el-select v-model="maintenanceHandleForm.maintenanceStatus" style="width: 100%">
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="maintenanceHandleForm.resultDesc" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="action-row">
          <el-button @click="maintenanceHandleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitMaintenanceHandle">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const pageSize = 8

const activeTab = ref('assets')
const refreshing = ref(false)
const categoryDialogVisible = ref(false)
const deviceDialogVisible = ref(false)
const maintenanceDialogVisible = ref(false)
const maintenanceHandleDialogVisible = ref(false)

const equipmentLoading = ref(false)
const equipmentLoadingMore = ref(false)
const borrowLoading = ref(false)
const borrowLoadingMore = ref(false)
const maintenanceLoading = ref(false)
const maintenanceLoadingMore = ref(false)

const equipmentRows = ref([])
const borrowRows = ref([])
const maintenanceRows = ref([])
const collegeOptions = ref([])
const labOptions = ref([])
const categoryOptions = ref([])

const equipmentTotal = ref(0)
const borrowTotal = ref(0)
const maintenanceTotal = ref(0)
const equipmentPageNum = ref(1)
const borrowPageNum = ref(1)
const maintenancePageNum = ref(1)

const scopeFilters = reactive({
  collegeId: userStore.userInfo?.managedCollegeId || '',
  labId: userStore.userInfo?.managedLabId || userStore.userInfo?.labId || ''
})
const equipmentFilters = reactive({ keyword: '', categoryId: '', status: '' })
const borrowFilters = reactive({ keyword: '', status: '' })
const maintenanceFilters = reactive({ keyword: '', status: '' })

const categoryForm = reactive({ id: null, name: '', description: '' })
const deviceForm = reactive({ id: null, labId: '', categoryId: '', name: '', deviceCode: '', brand: '', model: '', purchaseDate: '', location: '', imageUrl: '', status: 0, description: '', remark: '' })
const maintenanceForm = reactive({ equipmentId: null, equipmentName: '', issueDesc: '' })
const maintenanceHandleForm = reactive({ id: null, maintenanceStatus: 'IN_PROGRESS', resultDesc: '' })

const canSelectCollege = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const canSelectLab = computed(() => !userStore.userInfo?.labManager)
const fixedLabLabel = computed(() => {
  if (scopeFilters.labId) {
    return labOptions.value.find((item) => Number(item.id) === Number(scopeFilters.labId))?.labName || userStore.userInfo?.labName || `实验室#${scopeFilters.labId}`
  }
  return userStore.userInfo?.labName || '当前未固定实验室'
})
const scopeParams = computed(() => ({
  collegeId: scopeFilters.collegeId || undefined,
  labId: scopeFilters.labId || undefined
}))
const summaryCards = computed(() => [
  { label: '设备数', value: equipmentTotal.value, tip: '当前范围内设备总数' },
  { label: '借还数', value: borrowTotal.value, tip: '当前范围内借还记录' },
  { label: '维修数', value: maintenanceTotal.value, tip: '当前范围内维修记录' },
  { label: '分类数', value: categoryOptions.value.length, tip: '当前实验室已配置分类' }
])
const hasMoreAssets = computed(() => equipmentRows.value.length < equipmentTotal.value)
const hasMoreBorrows = computed(() => borrowRows.value.length < borrowTotal.value)
const hasMoreMaintenances = computed(() => maintenanceRows.value.length < maintenanceTotal.value)

const loadColleges = async () => {
  if (!canSelectCollege.value) {
    return
  }
  const response = await getCollegeOptions()
  collegeOptions.value = response.data || []
}

const loadLabs = async () => {
  const response = await getLabPage({
    pageNum: 1,
    pageSize: 200,
    collegeId: scopeFilters.collegeId || undefined
  })
  labOptions.value = response.data?.records || []
  if (scopeFilters.labId && !labOptions.value.some((item) => Number(item.id) === Number(scopeFilters.labId))) {
    labOptions.value.unshift({
      id: scopeFilters.labId,
      labName: fixedLabLabel.value
    })
  }
}

const loadCategories = async () => {
  const response = await getEquipmentCategories(scopeParams.value)
  categoryOptions.value = response.data || []
}

const fetchEquipmentPage = async (page) => {
  const response = await getEquipmentList({
    pageNum: page,
    pageSize,
    ...scopeParams.value,
    categoryId: equipmentFilters.categoryId || undefined,
    name: equipmentFilters.keyword || undefined,
    status: equipmentFilters.status === '' ? undefined : equipmentFilters.status
  })
  equipmentTotal.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const fetchBorrowPage = async (page) => {
  const response = await getBorrowList({
    pageNum: page,
    pageSize,
    ...scopeParams.value,
    keyword: borrowFilters.keyword || undefined,
    status: borrowFilters.status === '' ? undefined : borrowFilters.status
  })
  borrowTotal.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const fetchMaintenancePage = async (page) => {
  const response = await getMaintenanceList({
    pageNum: page,
    pageSize,
    ...scopeParams.value,
    keyword: maintenanceFilters.keyword || undefined,
    status: maintenanceFilters.status || undefined
  })
  maintenanceTotal.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetEquipments = async () => {
  equipmentLoading.value = true
  try {
    equipmentPageNum.value = 1
    equipmentRows.value = await fetchEquipmentPage(1)
  } finally {
    equipmentLoading.value = false
  }
}

const loadMoreEquipments = async () => {
  if (equipmentLoadingMore.value || !hasMoreAssets.value) {
    return
  }
  equipmentLoadingMore.value = true
  try {
    const nextPage = equipmentPageNum.value + 1
    const list = await fetchEquipmentPage(nextPage)
    equipmentPageNum.value = nextPage
    equipmentRows.value = equipmentRows.value.concat(list)
  } finally {
    equipmentLoadingMore.value = false
  }
}

const resetBorrows = async () => {
  borrowLoading.value = true
  try {
    borrowPageNum.value = 1
    borrowRows.value = await fetchBorrowPage(1)
  } finally {
    borrowLoading.value = false
  }
}

const loadMoreBorrows = async () => {
  if (borrowLoadingMore.value || !hasMoreBorrows.value) {
    return
  }
  borrowLoadingMore.value = true
  try {
    const nextPage = borrowPageNum.value + 1
    const list = await fetchBorrowPage(nextPage)
    borrowPageNum.value = nextPage
    borrowRows.value = borrowRows.value.concat(list)
  } finally {
    borrowLoadingMore.value = false
  }
}

const resetMaintenances = async () => {
  maintenanceLoading.value = true
  try {
    maintenancePageNum.value = 1
    maintenanceRows.value = await fetchMaintenancePage(1)
  } finally {
    maintenanceLoading.value = false
  }
}

const loadMoreMaintenances = async () => {
  if (maintenanceLoadingMore.value || !hasMoreMaintenances.value) {
    return
  }
  maintenanceLoadingMore.value = true
  try {
    const nextPage = maintenancePageNum.value + 1
    const list = await fetchMaintenancePage(nextPage)
    maintenancePageNum.value = nextPage
    maintenanceRows.value = maintenanceRows.value.concat(list)
  } finally {
    maintenanceLoadingMore.value = false
  }
}

const refreshAll = async () => {
  refreshing.value = true
  try {
    await loadCategories()
    await Promise.all([resetEquipments(), resetBorrows(), resetMaintenances()])
  } finally {
    refreshing.value = false
  }
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

const resetCategoryForm = () => {
  Object.assign(categoryForm, { id: null, name: '', description: '' })
}

const editCategory = (row) => {
  Object.assign(categoryForm, {
    id: row.id,
    name: row.name,
    description: row.description || ''
  })
}

const submitCategory = async () => {
  if (!scopeFilters.labId) {
    ElMessage.warning('请先选择实验室')
    return
  }
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }

  const payload = {
    labId: scopeFilters.labId,
    name: categoryForm.name.trim(),
    description: categoryForm.description.trim() || undefined
  }

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

const resetDeviceForm = () => {
  Object.assign(deviceForm, {
    id: null,
    labId: scopeFilters.labId || '',
    categoryId: '',
    name: '',
    deviceCode: '',
    brand: '',
    model: '',
    purchaseDate: '',
    location: '',
    imageUrl: '',
    status: 0,
    description: '',
    remark: ''
  })
}

const openDeviceDialog = (row = null) => {
  resetDeviceForm()
  if (row) {
    Object.assign(deviceForm, {
      id: row.id,
      labId: row.labId || scopeFilters.labId || '',
      categoryId: row.categoryId || '',
      name: row.name || '',
      deviceCode: row.deviceCode || '',
      brand: row.brand || '',
      model: row.model || '',
      purchaseDate: row.purchaseDate || '',
      location: row.location || '',
      imageUrl: row.imageUrl || '',
      status: Number(row.status ?? 0),
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
  await Promise.all([loadCategories(), resetEquipments()])
}

const removeDevice = async (row) => {
  await ElMessageBox.confirm(`确认删除设备“${row.name}”吗？`, '删除设备', { type: 'warning' })
  await deleteEquipment(row.id)
  ElMessage.success('设备已删除')
  await Promise.all([loadCategories(), resetEquipments()])
}

const openMaintenanceDialog = (row) => {
  Object.assign(maintenanceForm, {
    equipmentId: row.id,
    equipmentName: `${row.name || '-'} / ${row.deviceCode || '-'}`,
    issueDesc: ''
  })
  maintenanceDialogVisible.value = true
}

const submitMaintenance = async () => {
  if (!maintenanceForm.issueDesc.trim()) {
    ElMessage.warning('请输入故障描述')
    return
  }
  await createMaintenanceRecord({
    equipmentId: maintenanceForm.equipmentId,
    issueDesc: maintenanceForm.issueDesc.trim()
  })
  ElMessage.success('维修记录已创建')
  maintenanceDialogVisible.value = false
  await Promise.all([resetEquipments(), resetMaintenances()])
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
  await Promise.all([resetEquipments(), resetMaintenances()])
}

const reviewBorrow = async (row, status) => {
  await auditBorrow({ id: row.id, status })
  ElMessage.success('借用申请已更新')
  await Promise.all([resetEquipments(), resetBorrows()])
}

const confirmReturn = async (row) => {
  await returnEquipment({ id: row.id })
  ElMessage.success('设备已归还')
  await Promise.all([resetEquipments(), resetBorrows()])
}

const equipmentStatusLabel = (value) => ({ 0: '闲置', 1: '借出中', 2: '维修中', 3: '已报废' }[Number(value)] || '未知')
const equipmentStatusClass = (value) => ({ 0: 'default', 1: 'progress', 2: 'pending', 3: 'danger' }[Number(value)] || 'default')
const borrowStatusLabel = (value) => ({ 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已归还' }[Number(value)] || '未知')
const borrowStatusClass = (value) => ({ 0: 'pending', 1: 'success', 2: 'danger', 3: 'default' }[Number(value)] || 'default')
const maintenanceStatusLabel = (value) => ({ PENDING: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已解决' }[value] || value || '-')
const maintenanceStatusClass = (value) => ({ PENDING: 'pending', IN_PROGRESS: 'progress', RESOLVED: 'success' }[value] || 'default')
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(async () => {
  await loadColleges()
  await loadLabs()
  await refreshAll()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.metric-grid,
.card-list,
.meta-grid {
  display: grid;
  gap: 10px;
}

.metric-grid,
.meta-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.toolbar-card,
.metric-card,
.panel-card,
.record-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card,
.metric-card,
.panel-card,
.record-card {
  padding: 14px;
}

.scope-toolbar {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.compact {
  padding: 0;
  border: 0;
  background: transparent;
  grid-template-columns: minmax(0, 1fr) 110px 110px;
}

.metric-card {
  display: grid;
  gap: 6px;
}

.metric-card span,
.metric-card small,
.card-head p,
.meta-grid span,
.section-block div,
.muted {
  color: #64748b;
}

.metric-card strong,
.card-head strong,
.meta-grid strong,
.section-block label {
  color: #0f172a;
}

.metric-card strong {
  font-size: 24px;
}

.tab-row,
.card-head,
.action-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tab-row {
  overflow-x: auto;
}

.tab-btn {
  border: 0;
  background: rgba(241, 245, 249, 0.92);
  color: #475569;
  padding: 10px 14px;
  border-radius: 999px;
  font-weight: 700;
  white-space: nowrap;
}

.tab-btn.active {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.panel-card,
.record-card {
  display: grid;
  gap: 10px;
}

.card-head,
.action-row {
  justify-content: space-between;
  align-items: flex-start;
}

.card-head p {
  margin: 6px 0 0;
}

.meta-grid span {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.section-block {
  display: grid;
  gap: 6px;
}

.section-block label {
  font-size: 13px;
  font-weight: 700;
}

.section-block div {
  line-height: 1.7;
  white-space: pre-wrap;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.pending {
  color: #b45309;
  background: rgba(254, 243, 199, 0.92);
}

.status-pill.progress {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.92);
}

.status-pill.success {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.danger {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.92);
}

.status-pill.default {
  color: #475569;
  background: rgba(241, 245, 249, 0.92);
}

.top-gap {
  margin-top: 12px;
}

.load-more {
  display: flex;
  justify-content: center;
}

.drawer-body {
  display: grid;
  gap: 12px;
  padding-bottom: 20px;
}

@media (max-width: 640px) {
  .metric-grid,
  .meta-grid,
  .scope-toolbar,
  .compact {
    grid-template-columns: 1fr;
  }
}
</style>

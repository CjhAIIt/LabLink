<template>
  <div class="page-shell">
    <section class="toolbar-card selector-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室工作台</p>
          <h2>{{ scopeTitle }}</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="reloadWorkspace">刷新</el-button>
        </div>
      </div>

      <div class="selector-grid">
        <el-form-item v-if="canSelectCollege" label="学院">
          <el-select v-model="selectedCollegeId" placeholder="请选择学院" @change="handleCollegeChange">
            <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canSelectLab" label="实验室">
          <el-select v-model="selectedLabId" placeholder="请选择实验室" @change="handleLabChange">
            <el-option v-for="item in labOptions" :key="item.id" :label="item.labName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="实验室">
          <el-input :model-value="fixedLabName" disabled />
        </el-form-item>
      </div>

      <el-alert
        :closable="false"
        :type="workspaceEditable ? 'success' : 'info'"
        :title="workspaceEditable ? '当前为实验室管理模式，可维护资料空间并处理退组申请。' : '当前为只读模式，可查看并导出实验室资料。'"
      />
    </section>

    <div v-if="!selectedLabId" class="empty-panel">
      <el-empty description="请选择实验室后查看工作台数据" />
    </div>

    <template v-else>
      <section v-if="overview.lab" class="page-hero workspace-hero">
        <div>
          <p class="eyebrow">实验室工作台</p>
          <h1>{{ overview.lab.labName }}</h1>
          <p class="hero-subtitle">
            聚焦资料空间与成员流转，统一查看当前实验室的文件沉淀和退出申请处理情况。
          </p>
        </div>
        <div class="hero-side">
          <div class="hero-chip">指导老师：{{ overview.lab.teacherName || '未维护' }}</div>
          <div class="hero-chip muted">地点：{{ overview.lab.location || '未维护' }}</div>
        </div>
      </section>

      <section class="metric-grid">
        <article v-for="card in metricCards" :key="card.label" class="metric-card">
          <span class="metric-label">{{ card.label }}</span>
          <strong class="metric-value">{{ card.value }}</strong>
          <span class="metric-tip">{{ card.tip }}</span>
        </article>
      </section>

      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane label="资料空间" name="space">
          <div class="content-grid space-grid">
            <TablePageCard
              class="panel-card"
              title="目录树"
              subtitle="资料空间"
              :count-label="`${flatFolders.length} 项`"
            >
              <template #header-extra>
                <el-button v-if="workspaceEditable" type="primary" link @click="openFolderDialog()">新建目录</el-button>
              </template>

              <el-tree
                v-if="folderTree.length"
                :data="folderTree"
                node-key="id"
                default-expand-all
                highlight-current
                :expand-on-click-node="false"
                @node-click="handleFolderSelect"
              >
                <template #default="{ data }">
                  <div class="tree-node">
                    <span>{{ data.folderName }}</span>
                    <el-button v-if="workspaceEditable" type="primary" link @click.stop="openFolderDialog(data)">编辑</el-button>
                  </div>
                </template>
              </el-tree>
              <el-empty v-else description="暂无目录" />
            </TablePageCard>

            <TablePageCard
              class="panel-card"
              title="文件列表"
              subtitle="资料空间"
              :count-label="`${filePagination.total} 份`"
            >
              <template #filters>
                <div class="toolbar-actions compact">
                  <el-input v-model="fileFilters.keyword" clearable placeholder="搜索文件名 / 上传人" style="width: 220px" />
                  <el-select v-model="fileFilters.archiveFlag" clearable placeholder="归档状态" style="width: 140px">
                    <el-option label="全部" :value="null" />
                    <el-option label="未归档" :value="0" />
                    <el-option label="已归档" :value="1" />
                  </el-select>
                  <el-button @click="loadFiles">查询</el-button>
                  <el-button type="primary" plain @click="exportFiles">导出清单</el-button>
                  <el-upload v-if="workspaceEditable" :show-file-list="false" :http-request="handleUpload" accept="*">
                    <el-button type="primary">上传文件</el-button>
                  </el-upload>
                </div>
              </template>

              <el-table v-loading="fileLoading" :data="files" stripe>
                <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
                <el-table-column prop="folderName" label="目录" min-width="120" />
                <el-table-column prop="uploadUserName" label="上传人" min-width="110" />
                <el-table-column label="大小" min-width="100">
                  <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
                </el-table-column>
                <el-table-column label="上传时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
                <el-table-column label="归档" min-width="100">
                  <template #default="{ row }">
                    <StatusTag :value="row.archiveFlag" :label-map="archiveFlagLabels" :type-map="archiveFlagTypes" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openFile(row)">查看</el-button>
                    <el-button v-if="workspaceEditable" link type="success" @click="toggleArchive(row)">
                      {{ row.archiveFlag === 1 ? '取消归档' : '归档' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <template #pagination>
                <el-pagination
                  background
                  layout="prev, pager, next, total"
                  :current-page="filePagination.pageNum"
                  :page-size="filePagination.pageSize"
                  :total="filePagination.total"
                  @current-change="handleFilePageChange"
                />
              </template>
            </TablePageCard>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="workspaceEditable" label="退组申请" name="exit">
          <TablePageCard
            class="panel-card"
            title="实验室退组申请"
            subtitle="成员流转"
            :count-label="`${exitPagination.total} 条`"
          >
            <template #filters>
              <div class="toolbar-actions compact">
                <el-input
                  v-model="exitSearch.realName"
                  clearable
                  placeholder="搜索学生姓名"
                  style="width: 220px"
                  @keyup.enter="handleExitSearch"
                />
                <el-select v-model="exitSearch.status" clearable placeholder="状态" style="width: 140px">
                  <el-option label="待审核" :value="0" />
                  <el-option label="已通过" :value="1" />
                  <el-option label="已驳回" :value="2" />
                </el-select>
                <el-button @click="handleExitSearch">查询</el-button>
              </div>
            </template>

            <el-table :data="exitApplications" border stripe>
              <el-table-column prop="realName" label="姓名" min-width="110" />
              <el-table-column prop="studentId" label="学号" min-width="120" />
              <el-table-column prop="major" label="专业" min-width="150" />
              <el-table-column prop="reason" label="退组原因" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label-map="exitStatusLabels" :type-map="exitStatusTypes" />
                </template>
              </el-table-column>
              <el-table-column label="审核备注" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.auditRemarkDraft" maxlength="120" show-word-limit :disabled="row.status !== 0" />
                </template>
              </el-table-column>
              <el-table-column label="申请时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="审核时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.auditTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 0">
                    <el-button size="small" type="success" @click="auditExit(row, 1)">同意</el-button>
                    <el-button size="small" type="danger" plain @click="auditExit(row, 2)">驳回</el-button>
                  </template>
                  <span v-else class="done-text">已处理</span>
                </template>
              </el-table-column>
            </el-table>

            <template #pagination>
              <el-pagination
                background
                layout="prev, pager, next, total"
                :current-page="exitPagination.current"
                :page-size="exitPagination.size"
                :total="exitPagination.total"
                @current-change="fetchExitApplications"
              />
            </template>
          </TablePageCard>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="folderDialogVisible" :title="folderForm.id ? '编辑目录' : '新建目录'" width="520px">
      <el-form label-width="88px">
        <el-form-item label="目录名称">
          <el-input v-model="folderForm.folderName" maxlength="30" />
        </el-form-item>
        <el-form-item label="目录分类">
          <el-input v-model="folderForm.category" maxlength="20" />
        </el-form-item>
        <el-form-item label="上级目录">
          <el-select v-model="folderForm.parentId" clearable placeholder="顶级目录">
            <el-option :value="0" label="顶级目录" />
            <el-option v-for="item in flatFolders" :key="item.id" :label="item.folderName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFolder">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import {
  auditExitApplication,
  createSpaceFolder,
  getLabExitApplications,
  getLabSpaceOverview,
  getSpaceFiles,
  getSpaceFolders,
  updateSpaceFileArchive,
  updateSpaceFolder,
  uploadSpaceFile
} from '@/api/labSpace'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { useUserStore } from '@/stores/user'
import { downloadCsv } from '@/utils/export'

const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('space')
const overview = reactive({ lab: null, memberCount: 0, recentFiles: [] })
const folderTree = ref([])
const selectedFolderId = ref(null)
const files = ref([])
const fileLoading = ref(false)
const fileFilters = reactive({
  keyword: '',
  archiveFlag: null
})
const filePagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const exitSearch = reactive({
  realName: '',
  status: null
})
const exitApplications = ref([])
const exitPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const folderDialogVisible = ref(false)
const folderForm = reactive({
  id: null,
  folderName: '',
  category: '',
  parentId: 0
})

const colleges = ref([])
const allLabs = ref([])
const selectedCollegeId = ref(null)
const selectedLabId = ref(null)

const isTeacherPortal = computed(() => route.path.startsWith('/teacher'))
const isSchoolDirector = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager) && !isTeacherPortal.value)
const workspaceEditable = computed(() => isLabManager.value || isSchoolDirector.value)
const canSelectCollege = computed(() => isSchoolDirector.value)
const canSelectLab = computed(() => isSchoolDirector.value || isCollegeManager.value)

const archiveFlagLabels = {
  0: '未归档',
  1: '已归档'
}
const archiveFlagTypes = {
  0: 'info',
  1: 'success'
}
const exitStatusLabels = {
  0: '待审核',
  1: '已通过',
  2: '已驳回'
}
const exitStatusTypes = {
  0: 'warning',
  1: 'success',
  2: 'danger'
}

const scopeTitle = computed(() => {
  if (isTeacherPortal.value) {
    return '指导老师实验室工作台'
  }
  if (isSchoolDirector.value) {
    return '总负责人可按学院与实验室切换查看'
  }
  if (isCollegeManager.value) {
    return '学院负责人可查看本学院实验室工作台'
  }
  return '实验室管理员工作台'
})

const labOptions = computed(() => {
  let rows = allLabs.value.slice()
  if (isSchoolDirector.value && selectedCollegeId.value) {
    rows = rows.filter((item) => item.collegeId === selectedCollegeId.value)
  } else if (isCollegeManager.value) {
    rows = rows.filter((item) => item.collegeId === userStore.userInfo?.managedCollegeId)
  } else if (userStore.userInfo?.labId) {
    rows = rows.filter((item) => item.id === userStore.userInfo.labId)
  }
  return rows
})

const fixedLabName = computed(() => {
  const currentLab = allLabs.value.find((item) => item.id === selectedLabId.value)
  return currentLab?.labName || overview.lab?.labName || ''
})

const flatFolders = computed(() => {
  const result = []
  const walk = (nodes = []) => {
    nodes.forEach((node) => {
      result.push({ id: node.id, folderName: node.folderName })
      walk(node.children || [])
    })
  }
  walk(folderTree.value)
  return result
})

const metricCards = computed(() => [
  { label: '正式成员', value: overview.memberCount || 0, tip: '当前实验室有效成员数' },
  { label: '资料目录', value: flatFolders.value.length, tip: '当前实验室资料目录数量' },
  { label: '资料文件', value: filePagination.total || 0, tip: '当前实验室资料文件数量' },
  { label: '退组申请', value: workspaceEditable.value ? exitPagination.total || 0 : 0, tip: '待查看的退组申请记录数' }
])

const resetFolderForm = () => {
  Object.assign(folderForm, {
    id: null,
    folderName: '',
    category: '',
    parentId: 0
  })
}

const initializeScope = () => {
  if (isSchoolDirector.value) {
    selectedCollegeId.value = selectedCollegeId.value || colleges.value[0]?.id || null
    selectedLabId.value = selectedLabId.value || labOptions.value[0]?.id || null
    return
  }
  if (isCollegeManager.value) {
    selectedCollegeId.value = userStore.userInfo?.managedCollegeId || null
    selectedLabId.value = selectedLabId.value || labOptions.value[0]?.id || null
    return
  }
  selectedLabId.value = userStore.userInfo?.labId || null
}

const loadSelectors = async () => {
  const [collegeRes, labRes] = await Promise.all([
    getCollegeOptions(),
    getLabPage({ pageNum: 1, pageSize: 500 })
  ])
  colleges.value = collegeRes.data || []
  allLabs.value = labRes.data?.records || []
  initializeScope()
}

const loadOverview = async () => {
  if (!selectedLabId.value) {
    Object.assign(overview, { lab: null, memberCount: 0, recentFiles: [] })
    return
  }
  const res = await getLabSpaceOverview({ labId: selectedLabId.value })
  Object.assign(overview, res.data || { lab: null, memberCount: 0, recentFiles: [] })
}

const loadFolders = async () => {
  if (!selectedLabId.value) {
    folderTree.value = []
    selectedFolderId.value = null
    return
  }
  const res = await getSpaceFolders({ labId: selectedLabId.value })
  folderTree.value = res.data || []
  if (!folderTree.value.length) {
    selectedFolderId.value = null
    return
  }
  const exists = flatFolders.value.some((item) => item.id === selectedFolderId.value)
  if (!exists) {
    selectedFolderId.value = folderTree.value[0]?.id || null
  }
}

const loadFiles = async () => {
  if (!selectedLabId.value) {
    files.value = []
    filePagination.total = 0
    return
  }
  fileLoading.value = true
  try {
    const res = await getSpaceFiles({
      pageNum: filePagination.pageNum,
      pageSize: filePagination.pageSize,
      labId: selectedLabId.value,
      folderId: selectedFolderId.value || undefined,
      archiveFlag: fileFilters.archiveFlag === null ? undefined : fileFilters.archiveFlag,
      keyword: fileFilters.keyword || undefined
    })
    files.value = res.data?.records || []
    filePagination.total = res.data?.total || 0
  } finally {
    fileLoading.value = false
  }
}

const exportFiles = async () => {
  if (!selectedLabId.value) {
    return
  }
  const res = await getSpaceFiles({
    pageNum: 1,
    pageSize: 1000,
    labId: selectedLabId.value,
    folderId: selectedFolderId.value || undefined,
    archiveFlag: fileFilters.archiveFlag === null ? undefined : fileFilters.archiveFlag,
    keyword: fileFilters.keyword || undefined
  })
  downloadCsv(
    `lab-files-${selectedLabId.value}-${dayjs().format('YYYYMMDD-HHmmss')}.csv`,
    [
      ['文件名', '目录', '上传人', '大小', '上传时间', '归档状态'],
      ...(res.data?.records || []).map((item) => [
        item.fileName || '',
        item.folderName || '',
        item.uploadUserName || '',
        formatFileSize(item.fileSize),
        formatDateTime(item.createTime),
        item.archiveFlag === 1 ? '已归档' : '未归档'
      ])
    ]
  )
  ElMessage.success('资料清单已导出')
}

const handleFolderSelect = (node) => {
  selectedFolderId.value = node.id
  filePagination.pageNum = 1
  loadFiles()
}

const handleFilePageChange = (page) => {
  filePagination.pageNum = page
  loadFiles()
}

const openFolderDialog = (folder) => {
  resetFolderForm()
  if (folder) {
    Object.assign(folderForm, {
      id: folder.id,
      folderName: folder.folderName,
      category: folder.category || '',
      parentId: folder.parentId || 0
    })
  } else if (selectedFolderId.value) {
    folderForm.parentId = selectedFolderId.value
  }
  folderDialogVisible.value = true
}

const saveFolder = async () => {
  if (!folderForm.folderName) {
    ElMessage.warning('请先填写目录名称')
    return
  }
  const payload = {
    labId: selectedLabId.value,
    folderName: folderForm.folderName,
    category: folderForm.category,
    parentId: folderForm.parentId || 0
  }
  if (folderForm.id) {
    await updateSpaceFolder(folderForm.id, payload)
  } else {
    await createSpaceFolder(payload)
  }
  folderDialogVisible.value = false
  ElMessage.success('目录已保存')
  await loadFolders()
  await loadFiles()
}

const handleUpload = async ({ file }) => {
  if (!selectedFolderId.value) {
    ElMessage.warning('请先选择上传目录')
    return
  }
  const formData = new FormData()
  formData.append('labId', selectedLabId.value)
  formData.append('folderId', selectedFolderId.value)
  formData.append('archiveFlag', 0)
  formData.append('file', file)
  await uploadSpaceFile(formData)
  ElMessage.success('文件上传成功')
  await Promise.all([loadFiles(), loadOverview()])
}

const toggleArchive = async (row) => {
  await updateSpaceFileArchive(row.id, { archiveFlag: row.archiveFlag === 1 ? 0 : 1 })
  ElMessage.success(row.archiveFlag === 1 ? '已取消归档' : '已归档')
  await loadFiles()
}

const openFile = (row) => {
  if (row?.fileUrl) {
    window.open(row.fileUrl, '_blank')
  }
}

const fetchExitApplications = async (page = exitPagination.current) => {
  if (!workspaceEditable.value || !selectedLabId.value) {
    exitApplications.value = []
    exitPagination.total = 0
    return
  }
  exitPagination.current = page
  const res = await getLabExitApplications({
    pageNum: exitPagination.current,
    pageSize: exitPagination.size,
    labId: selectedLabId.value,
    realName: exitSearch.realName || undefined,
    status: exitSearch.status
  })
  exitApplications.value = (res.data?.records || []).map((item) => ({
    ...item,
    auditRemarkDraft: item.auditRemark || ''
  }))
  exitPagination.total = res.data?.total || 0
}

const handleExitSearch = () => {
  fetchExitApplications(1)
}

const auditExit = async (row, status) => {
  await auditExitApplication({
    id: row.id,
    status,
    auditRemark: row.auditRemarkDraft
  })
  ElMessage.success(status === 1 ? '已同意退组申请' : '已驳回退组申请')
  await Promise.all([fetchExitApplications(exitPagination.current), loadOverview()])
}

const handleCollegeChange = () => {
  selectedLabId.value = labOptions.value[0]?.id || null
  handleLabChange()
}

const handleLabChange = async () => {
  selectedFolderId.value = null
  filePagination.pageNum = 1
  exitPagination.current = 1
  await reloadWorkspace()
}

const resetWorkspaceData = () => {
  Object.assign(overview, { lab: null, memberCount: 0, recentFiles: [] })
  folderTree.value = []
  files.value = []
  exitApplications.value = []
  filePagination.total = 0
  exitPagination.total = 0
  selectedFolderId.value = null
}

const reloadWorkspace = async () => {
  if (!selectedLabId.value) {
    resetWorkspaceData()
    return
  }
  const tasks = [loadOverview(), loadFolders()]
  if (workspaceEditable.value) {
    tasks.push(fetchExitApplications(1))
  }
  await Promise.all(tasks)
  await loadFiles()
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const formatFileSize = (size) => {
  const value = Number(size) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(async () => {
  await loadSelectors()
  await reloadWorkspace()
})
</script>

<style scoped>
.selector-card {
  display: grid;
  gap: 16px;
}

.selector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 320px));
  gap: 12px 16px;
}

.workspace-hero {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(13, 148, 136, 0.86)),
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.22), transparent 30%);
}

.hero-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.hero-chip {
  padding: 10px 14px;
  border-radius: 999px;
  color: #ecfeff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.hero-chip.muted {
  color: rgba(236, 254, 255, 0.9);
}

.space-grid {
  grid-template-columns: 320px minmax(0, 1fr);
}

.toolbar-actions.compact {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.done-text {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 960px) {
  .space-grid {
    grid-template-columns: 1fr;
  }
}
</style>

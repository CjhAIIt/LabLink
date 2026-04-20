<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室空间</p>
          <h2>浏览目录、上传文件，并查看实验室成员信息</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!userStore.userInfo?.labId" class="empty-panel">
      <el-empty description="请先加入实验室后再访问共享空间。" />
    </div>

    <template v-else>
      <TablePageCard title="实验室成员" subtitle="当前实验室基础信息" :count-label="`${members.length} 人`" class="member-card">
        <el-table :data="members" stripe>
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="major" label="专业" min-width="160" />
          <el-table-column label="角色" min-width="120">
            <template #default="{ row }">
              <StatusTag :value="row.memberRole" :label-map="memberRoleLabels" :type-map="memberRoleTypes" />
            </template>
          </el-table-column>
          <el-table-column prop="joinDate" label="加入日期" min-width="120" />
        </el-table>
      </TablePageCard>

      <section class="content-grid space-grid">
        <TablePageCard title="目录" subtitle="实验室空间" :count-label="`${folderCount} 项`">
          <el-tree
            v-if="folderTree.length"
            :data="folderTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            highlight-current
            @node-click="handleFolderSelect"
          />
          <el-empty v-else description="暂无目录" />
        </TablePageCard>

        <TablePageCard title="文件" subtitle="实验室空间" :count-label="`${pagination.total} 项`">
          <template #header-extra>
            <el-upload :show-file-list="false" :http-request="handleUpload">
              <el-button type="primary">上传文件</el-button>
            </el-upload>
          </template>

          <template #filters>
            <div class="toolbar-actions compact">
              <el-input v-model="fileFilters.keyword" clearable placeholder="搜索文件名" style="width: 220px" @keyup.enter="handleSearch" />
              <el-select v-model="fileFilters.archiveFlag" clearable placeholder="归档状态" style="width: 160px">
                <el-option label="全部" :value="null" />
                <el-option label="当前文件" :value="0" />
                <el-option label="已归档" :value="1" />
              </el-select>
              <el-button @click="handleSearch">搜索</el-button>
            </div>
          </template>

          <el-table v-loading="loading" :data="files" stripe>
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column prop="folderName" label="目录" min-width="120" />
            <el-table-column prop="uploadUserName" label="上传人" min-width="110" />
            <el-table-column label="大小" min-width="100"><template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="上传时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
            <el-table-column label="归档状态" min-width="100">
              <template #default="{ row }"><StatusTag :value="row.archiveFlag" :label-map="archiveStatusLabels" :type-map="archiveStatusTypes" /></template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openFile(row)">打开</el-button></template></el-table-column>
          </el-table>

          <template #pagination>
            <el-pagination background layout="prev, pager, next, total" :current-page="pagination.pageNum" :page-size="pagination.pageSize" :total="pagination.total" @current-change="handlePageChange" />
          </template>
        </TablePageCard>
      </section>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getActiveLabMembers } from '@/api/labMembers'
import { getSpaceFiles, getSpaceFolders, uploadSpaceFile } from '@/api/labSpace'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const folderTree = ref([])
const selectedFolderId = ref(null)
const files = ref([])
const members = ref([])
const loading = ref(false)
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const fileFilters = reactive({ keyword: '', archiveFlag: null })
const archiveStatusLabels = { 0: '当前文件', 1: '已归档' }
const archiveStatusTypes = { 0: 'info', 1: 'success' }
const memberRoleLabels = { lab_admin: '管理员', lab_leader: '负责人', leader: '负责人', member: '成员' }
const memberRoleTypes = { lab_admin: 'danger', lab_leader: 'success', leader: 'success', member: 'info' }
const treeProps = { children: 'children', label: 'folderName' }

const folderCount = computed(() => {
  let total = 0
  const walk = (nodes) => {
    ;(nodes || []).forEach((node) => {
      total += 1
      walk(node.children)
    })
  }
  walk(folderTree.value)
  return total
})

const loadFolders = async () => {
  const res = await getSpaceFolders()
  folderTree.value = res.data || []
  if (!selectedFolderId.value && folderTree.value.length) {
    selectedFolderId.value = folderTree.value[0].id
  }
}

const loadFiles = async () => {
  loading.value = true
  try {
    const res = await getSpaceFiles({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      folderId: selectedFolderId.value || undefined,
      archiveFlag: fileFilters.archiveFlag === null ? undefined : fileFilters.archiveFlag,
      keyword: fileFilters.keyword || undefined
    })
    files.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadMembers = async () => {
  const res = await getActiveLabMembers({ labId: userStore.userInfo?.labId })
  members.value = res.data || []
}

const loadPageData = async () => {
  await Promise.all([loadFolders(), loadMembers()])
  await loadFiles()
}

const handleFolderSelect = (node) => {
  selectedFolderId.value = node.id
  pagination.pageNum = 1
  loadFiles()
}
const handleSearch = () => { pagination.pageNum = 1; loadFiles() }
const handleUpload = async ({ file }) => {
  if (!selectedFolderId.value) { ElMessage.warning('请先选择目录后再上传文件'); return }
  const formData = new FormData()
  formData.append('folderId', selectedFolderId.value)
  formData.append('archiveFlag', 0)
  formData.append('file', file)
  await uploadSpaceFile(formData)
  ElMessage.success('文件已上传')
  loadFiles()
}
const handlePageChange = (page) => { pagination.pageNum = page; loadFiles() }
const openFile = (row) => { window.open(row.fileUrl, '_blank') }
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')
const formatFileSize = (size) => {
  const value = Number(size) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(() => { loadPageData() })
</script>

<style scoped>
.member-card { margin-bottom: 16px; }
.space-grid { grid-template-columns: 320px minmax(0, 1fr); }
.toolbar-actions.compact { display: flex; flex-wrap: wrap; gap: 10px; }
@media (max-width: 960px) { .space-grid { grid-template-columns: 1fr; } }
</style>
<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室资料空间</p>
          <h2>分类浏览、上传与归档资料</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadPageData">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!userStore.userInfo?.labId" class="empty-panel">
      <el-empty description="当前尚未加入实验室，无法访问资料空间。" />
    </div>

    <template v-else>
      <section class="content-grid space-grid">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>目录树</span>
              <el-tag effect="plain">{{ folderCount }} 个目录</el-tag>
            </div>
          </template>

          <el-tree
            v-if="folderTree.length"
            :data="folderTree"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleFolderSelect"
          />
          <el-empty v-else description="暂无目录" />
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>文件列表</span>
              <div class="toolbar-actions compact">
                <el-input v-model="fileFilters.keyword" clearable placeholder="搜索文件名" style="width: 220px" />
                <el-select v-model="fileFilters.archiveFlag" clearable placeholder="归档状态" style="width: 140px">
                  <el-option label="全部" :value="undefined" />
                  <el-option label="未归档" :value="0" />
                  <el-option label="已归档" :value="1" />
                </el-select>
                <el-button @click="loadFiles">查询</el-button>
                <el-upload :show-file-list="false" :http-request="handleUpload">
                  <el-button type="primary">上传文件</el-button>
                </el-upload>
              </div>
            </div>
          </template>

          <el-table v-loading="loading" :data="files" stripe>
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
                <el-tag :type="row.archiveFlag === 1 ? 'success' : 'info'">
                  {{ row.archiveFlag === 1 ? '已归档' : '未归档' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openFile(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :current-page="pagination.pageNum"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </section>
    </template>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { getSpaceFiles, getSpaceFolders, uploadSpaceFile } from '@/api/labSpace'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const folderTree = ref([])
const selectedFolderId = ref(null)
const files = ref([])
const loading = ref(false)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const fileFilters = reactive({
  keyword: '',
  archiveFlag: undefined
})

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
      archiveFlag: fileFilters.archiveFlag,
      keyword: fileFilters.keyword || undefined
    })
    files.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadPageData = async () => {
  await loadFolders()
  await loadFiles()
}

const handleFolderSelect = (node) => {
  selectedFolderId.value = node.id
  pagination.pageNum = 1
  loadFiles()
}

const handleUpload = async ({ file }) => {
  if (!selectedFolderId.value) {
    ElMessage.warning('请先选择上传目录')
    return
  }
  const formData = new FormData()
  formData.append('folderId', selectedFolderId.value)
  formData.append('archiveFlag', 0)
  formData.append('file', file)
  await uploadSpaceFile(formData)
  ElMessage.success('文件上传成功')
  loadFiles()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadFiles()
}

const openFile = (row) => {
  window.open(row.fileUrl, '_blank')
}

const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const formatFileSize = (size) => {
  const value = Number(size) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(() => {
  loadPageData()
})
</script>

<style scoped>
.space-grid {
  grid-template-columns: 320px minmax(0, 1fr);
}

.toolbar-actions.compact {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 960px) {
  .space-grid {
    grid-template-columns: 1fr;
  }
}
</style>

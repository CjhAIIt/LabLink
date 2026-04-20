<template>
  <div class="m-page">
    <section v-if="!userStore.userInfo?.labId" class="empty-card">
      <el-empty description="加入实验室后才能访问资料空间。" :image-size="88" />
    </section>

    <template v-else>
      <section class="toolbar-card">
        <el-input v-model="keyword" clearable placeholder="搜索文件名称" @keyup.enter="loadFiles" />
        <el-select v-model="archiveFlag" clearable placeholder="状态" style="width: 120px" @change="loadFiles">
          <el-option label="当前文件" :value="0" />
          <el-option label="已归档" :value="1" />
        </el-select>
      </section>

      <section class="folder-strip">
        <button
          v-for="item in flattenedFolders"
          :key="item.id"
          class="folder-chip"
          :class="{ active: selectedFolderId === item.id }"
          type="button"
          @click="selectFolder(item.id)"
        >
          {{ item.folderName }}
        </button>
      </section>

      <section class="panel-card upload-card">
        <div>
          <strong>上传文件</strong>
          <p>当前目录：{{ currentFolderName || '未选择' }}</p>
        </div>
        <el-upload :show-file-list="false" :http-request="handleUpload">
          <el-button type="primary">选择文件</el-button>
        </el-upload>
      </section>

      <section class="panel-card">
        <header class="panel-head">
          <h2>文件列表</h2>
          <span>{{ total }} 个</span>
        </header>
        <div v-if="files.length" class="file-list">
          <article v-for="item in files" :key="item.id" class="file-card">
            <div>
              <strong>{{ item.fileName || fileName(item.fileUrl) }}</strong>
              <p>{{ item.folderName || currentFolderName || '未分类' }}</p>
            </div>
            <div class="file-meta">
              <span>{{ formatSize(item.fileSize) }}</span>
              <small>{{ formatDateTime(item.createTime) }}</small>
            </div>
            <div class="file-actions">
              <el-button text @click="openFile(item)">打开</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无文件" :image-size="80" />
      </section>

      <div class="load-more">
        <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSpaceFiles, getSpaceFolders, uploadSpaceFile } from '@/api/labSpace'
import { useUserStore } from '@/stores/user'
import { getFileNameFromUrl, resolveFileUrl } from '@/utils/file'

const userStore = useUserStore()
const folders = ref([])
const selectedFolderId = ref(null)
const files = ref([])
const keyword = ref('')
const archiveFlag = ref(null)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loadingMore = ref(false)

const flattenedFolders = computed(() => {
  const result = []
  const walk = (items = []) => {
    items.forEach((item) => {
      result.push(item)
      walk(item.children)
    })
  }
  walk(folders.value)
  return result
})

const hasMore = computed(() => files.value.length < total.value)
const currentFolderName = computed(() =>
  flattenedFolders.value.find((item) => item.id === selectedFolderId.value)?.folderName || ''
)

const loadFolders = async () => {
  const response = await getSpaceFolders()
  folders.value = response.data || []
  if (!selectedFolderId.value && flattenedFolders.value.length) {
    selectedFolderId.value = flattenedFolders.value[0].id
  }
}

const fetchFiles = async (page) => {
  const response = await getSpaceFiles({
    pageNum: page,
    pageSize,
    folderId: selectedFolderId.value || undefined,
    keyword: keyword.value || undefined,
    archiveFlag: archiveFlag.value === null ? undefined : archiveFlag.value
  })
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const loadFiles = async () => {
  pageNum.value = 1
  files.value = await fetchFiles(1)
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchFiles(nextPage)
    pageNum.value = nextPage
    files.value = files.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const selectFolder = async (folderId) => {
  selectedFolderId.value = folderId
  await loadFiles()
}

const handleUpload = async ({ file }) => {
  if (!selectedFolderId.value) {
    ElMessage.warning('请先选择目录')
    return
  }
  const formData = new FormData()
  formData.append('folderId', selectedFolderId.value)
  formData.append('archiveFlag', 0)
  formData.append('file', file)
  await uploadSpaceFile(formData)
  ElMessage.success('上传成功')
  await loadFiles()
}

const openFile = (row) => {
  window.open(resolveFileUrl(row.fileUrl), '_blank')
}

const fileName = (url) => getFileNameFromUrl(url, '附件')
const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
const formatSize = (size) => {
  const value = Number(size) || 0
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(async () => {
  await loadFolders()
  await loadFiles()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.panel-card,
.empty-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 10px;
}

.folder-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.folder-chip {
  white-space: nowrap;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.94);
  border-radius: 999px;
  padding: 8px 12px;
  color: #475569;
}

.folder-chip.active {
  color: #2563eb;
  border-color: rgba(37, 99, 235, 0.24);
  background: rgba(219, 234, 254, 0.92);
}

.panel-card {
  padding: 14px;
}

.upload-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.upload-card strong,
.panel-head h2,
.file-card strong {
  color: #0f172a;
}

.upload-card p,
.panel-head span,
.file-card p,
.file-meta {
  color: #64748b;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.file-list {
  display: grid;
  gap: 10px;
}

.file-card {
  border-radius: 16px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  display: grid;
  gap: 8px;
}

.file-card p {
  margin: 6px 0 0;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
}

.file-actions {
  display: flex;
  justify-content: flex-end;
}

.load-more {
  display: flex;
  justify-content: center;
}
</style>


<template>
  <div class="file-upload">
    <el-upload
      ref="uploadRef"
      class="upload-component"
      :action="uploadUrl"
      :headers="headers"
      :before-upload="beforeUpload"
      :on-preview="handlePreview"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
      :file-list="fileList"
      :limit="limit"
      :accept="accept"
      :multiple="multiple"
      :disabled="disabled"
      :data="data"
      :drag="drag"
      :list-type="listType"
      :auto-upload="autoUpload"
    >
      <el-button v-if="!drag && listType !== 'picture-card'" type="primary">
        <el-icon><upload /></el-icon>
        <span>上传文件</span>
      </el-button>

      <div v-if="drag" class="drag-area">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          <div class="drag-title">拖拽文件到此处，或<em>点击上传</em></div>
          <div class="drag-subtitle">
            支持 {{ accept || '所有文件' }}，单个不超过 {{ sizeLimit }}MB
          </div>
        </div>
      </div>

      <el-icon v-if="listType === 'picture-card'"><plus /></el-icon>

      <template #tip>
        <div v-if="tip" class="el-upload__tip">{{ tip }}</div>
      </template>
    </el-upload>

    <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="80%" top="5vh">
      <div v-if="previewMode === 'image'" class="preview-body preview-image">
        <img :src="previewUrl" :alt="previewTitle" />
      </div>
      <div v-else-if="previewMode === 'pdf'" class="preview-body">
        <iframe :src="previewUrl" class="preview-frame" />
      </div>
      <div v-else-if="previewMode === 'office'" class="preview-body">
        <iframe :src="previewUrl" class="preview-frame" />
      </div>
      <div v-else class="preview-empty">
        当前文件暂不支持在线预览，已尝试为你打开新窗口。
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, genFileId } from 'element-plus'
import { Plus, Upload, UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import {
  buildOfficePreviewUrl,
  getFileExtension,
  getFileNameFromUrl,
  getFilePreviewType,
  resolveFileUrl
} from '@/utils/file'

const props = defineProps({
  action: { type: String, default: '/api/files/upload' },
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 5 },
  accept: { type: String, default: '' },
  multiple: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  data: { type: Object, default: () => ({}) },
  drag: { type: Boolean, default: false },
  listType: { type: String, default: 'text' },
  autoUpload: { type: Boolean, default: true },
  sizeLimit: { type: Number, default: 10 },
  tip: { type: String, default: '' },
  previewable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'change', 'remove'])

const uploadRef = ref()
const fileList = ref([])

const previewDialogVisible = ref(false)
const previewTitle = ref('')
const previewUrl = ref('')
const previewMode = ref('download')

const uploadUrl = computed(() => props.action)

const headers = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

/* ---------- 工具函数 ---------- */

function normalizeFileList(items = []) {
  return items.map((item, index) => {
    if (typeof item === 'string') {
      const resolvedUrl = resolveFileUrl(item)
      return {
        name: getFileNameFromUrl(item, `file-${index + 1}`),
        rawUrl: item,
        previewType: getFilePreviewType(item),
        url: resolvedUrl
      }
    }

    const rawUrl = item.rawUrl || item.url || ''
    const resolvedUrl = resolveFileUrl(rawUrl)

    return {
      ...item,
      name: item.name || getFileNameFromUrl(rawUrl, `file-${index + 1}`),
      rawUrl,
      previewType: item.previewType || getFilePreviewType(rawUrl),
      url: resolvedUrl
    }
  })
}

function normalizeUploadFiles(uploadFiles = []) {
  return uploadFiles
    .map((file, index) => {
      const rawUrl =
        file.rawUrl ||
        file.response?.data?.url ||
        file.response?.data?.path ||
        file.url ||
        ''

      if (!rawUrl) return null

      return {
        ...file,
        name:
          file.response?.data?.fileName ||
          file.name ||
          getFileNameFromUrl(rawUrl, `file-${index + 1}`),
        rawUrl,
        previewType: getFilePreviewType(rawUrl),
        url: resolveFileUrl(rawUrl)
      }
    })
    .filter(Boolean)
}

function parseAcceptRules(accept) {
  return (accept || '')
    .split(',')
    .map((i) => i.trim().toLowerCase())
    .filter(Boolean)
}

function matchesAcceptRule(file, rule) {
  const fileName = (file.name || '').toLowerCase()
  const fileType = (file.type || '').toLowerCase()
  const fileExtension = getFileExtension(fileName)

  if (rule.startsWith('.')) return fileExtension === rule

  if (rule.endsWith('/*')) {
    if (fileType.startsWith(rule.slice(0, -1))) return true
    if (rule === 'image/*') {
      return ['.jpg', '.jpeg', '.png', '.gif', '.webp'].some((e) =>
        fileName.endsWith(e)
      )
    }
    return false
  }

  return fileType === rule
}

/* ---------- watch ---------- */

watch(
  () => props.modelValue,
  (val) => {
    fileList.value = normalizeFileList(val || [])
  },
  { immediate: true }
)

/* ---------- 上传逻辑 ---------- */

const beforeUpload = (file) => {
  const isLtSize = file.size / 1024 / 1024 < props.sizeLimit
  if (!isLtSize) {
    ElMessage.error(`文件大小不能超过 ${props.sizeLimit}MB`)
    return false
  }

  const rules = parseAcceptRules(props.accept)
  if (rules.length && !rules.some((r) => matchesAcceptRule(file, r))) {
    ElMessage.error(`不支持的文件类型：${props.accept}`)
    return false
  }

  return true
}

const handleSuccess = (response, uploadFile, uploadFiles) => {
  if (response.code !== 200 && response.code !== 0) {
    ElMessage.error(response.message || '上传失败')
    emit('error', response)
    return
  }

  fileList.value = normalizeUploadFiles(uploadFiles)

  emit('update:modelValue', fileList.value)
  emit('success', response)
  emit('change', fileList.value)
}

const handleError = () => {
  ElMessage.error('上传失败')
}

const handleRemove = (uploadFile, uploadFiles) => {
  fileList.value = normalizeUploadFiles(uploadFiles)

  emit('update:modelValue', fileList.value)
  emit('change', fileList.value)
  emit('remove', uploadFile)
}

const handleExceed = (files) => {
  if (props.limit === 1 && files.length) {
    const file = files[0]
    file.uid = genFileId()
    uploadRef.value.clearFiles()
    uploadRef.value.handleStart(file)
    if (props.autoUpload) uploadRef.value.submit()
    return
  }

  ElMessage.warning(`最多上传 ${props.limit} 个文件`)
}

const handlePreview = (file) => {
  if (!props.previewable) return

  const rawUrl =
    file.rawUrl || file.response?.data?.url || file.response?.data?.path || ''
  const url = resolveFileUrl(rawUrl || file.url || '')

  const previewType = getFilePreviewType(url)

  previewTitle.value = file.name

  if (previewType === 'image') {
    previewMode.value = 'image'
    previewUrl.value = url
    previewDialogVisible.value = true
    return
  }

  if (previewType === 'pdf') {
    previewMode.value = 'pdf'
    previewUrl.value = url
    previewDialogVisible.value = true
    return
  }

  if (previewType === 'office') {
    const officeUrl = buildOfficePreviewUrl(url)
    if (officeUrl) {
      previewMode.value = 'office'
      previewUrl.value = officeUrl
      previewDialogVisible.value = true
      return
    }
  }

  window.open(url)
}

defineExpose({
  submit: () => uploadRef.value?.submit(),
  clearFiles: () => uploadRef.value?.clearFiles()
})
</script>

<style scoped>
.file-upload {
  width: 100%;
}

.drag-area {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  width: 100%;
  height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: #fafafa;
}

.preview-frame {
  width: 100%;
  min-height: 70vh;
  border: none;
}

.preview-image img {
  max-width: 100%;
  max-height: 70vh;
}
</style>

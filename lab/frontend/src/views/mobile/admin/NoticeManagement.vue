<template>
  <div class="m-page">
    <section class="metric-grid">
      <article v-for="card in summaryCards" :key="card.label" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.tip }}</small>
      </article>
    </section>

    <section class="toolbar-card">
      <el-input v-model="filters.keyword" clearable placeholder="搜索公告标题" @keyup.enter="resetAndFetch" />
      <el-select v-model="filters.publishScope" clearable placeholder="范围" style="width: 110px" @change="resetAndFetch">
        <el-option label="学校" value="school" />
        <el-option label="学院" value="college" />
        <el-option label="实验室" value="lab" />
      </el-select>
      <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
      <el-button type="primary" @click="openEditor()">发布</el-button>
    </section>

    <section class="card-list" v-loading="loading">
      <article v-for="row in rows" :key="row.id" class="record-card">
        <div class="card-head">
          <div>
            <strong>{{ row.title || '未命名公告' }}</strong>
            <p>{{ scopeLabel(row.publishScope) }}<template v-if="row.collegeName"> · {{ row.collegeName }}</template><template v-if="row.labName"> · {{ row.labName }}</template></p>
          </div>
          <span class="status-pill" :class="Number(row.status) === 1 ? 'success' : 'default'">
            {{ Number(row.status) === 1 ? '已发布' : '草稿' }}
          </span>
        </div>

        <div class="meta-grid">
          <div>
            <span>发布人</span>
            <strong>{{ row.publisherName || '-' }}</strong>
          </div>
          <div>
            <span>发布时间</span>
            <strong>{{ formatDateTime(row.publishTime || row.createTime) }}</strong>
          </div>
        </div>

        <div class="section-block">
          <label>公告内容</label>
          <div>{{ row.content || '暂无内容' }}</div>
        </div>

        <div class="action-row">
          <el-button text type="primary" @click="openEditor(row)">编辑</el-button>
          <el-button text type="danger" @click="removeNotice(row)">删除</el-button>
        </div>
      </article>

      <el-empty v-if="!loading && rows.length === 0" description="暂无公告数据" :image-size="84" />
    </section>

    <div class="load-more">
      <el-button v-if="hasMore" plain :loading="loadingMore" @click="fetchMore">加载更多</el-button>
      <span v-else-if="rows.length" class="muted">已经到底了</span>
    </div>

    <el-drawer v-model="editorVisible" :title="form.id ? '编辑公告' : '发布公告'" size="92%">
      <div class="drawer-body">
        <el-form label-position="top">
          <el-form-item label="发布范围">
            <el-segmented v-model="form.publishScope" :options="scopeOptions" :disabled="!canSelectScope" />
          </el-form-item>
          <el-form-item v-if="form.publishScope !== 'school'" label="所属学院">
            <el-select v-model="form.collegeId" :disabled="!canSelectCollegeInForm" placeholder="请选择学院" style="width: 100%" @change="handleFormCollegeChange">
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.publishScope === 'lab'" label="所属实验室">
            <el-select v-model="form.labId" :disabled="!canSelectLabInForm" placeholder="请选择实验室" style="width: 100%">
              <el-option v-for="item in labs" :key="item.id" :label="item.labName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="已发布" :value="1" />
              <el-option label="草稿" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="公告标题">
            <el-input v-model="form.title" maxlength="80" show-word-limit />
          </el-form-item>
          <el-form-item label="公告内容">
            <el-input v-model="form.content" type="textarea" :rows="8" maxlength="2000" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveNoticeAction">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import { createNotice, deleteNotice, getNoticePage, updateNotice } from '@/api/notices'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const pageSize = 10

const loading = ref(false)
const loadingMore = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const colleges = ref([])
const labs = ref([])

const filters = reactive({
  keyword: '',
  publishScope: ''
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  publishScope: 'school',
  collegeId: undefined,
  labId: undefined,
  status: 1
})

const isSchoolManager = computed(() => Boolean(userStore.userInfo?.schoolDirector || userStore.userRole === 'super_admin'))
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager))
const managedCollegeId = computed(() => userStore.userInfo?.managedCollegeId || null)
const managedLabId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)
const hasMore = computed(() => rows.value.length < total.value)
const canSelectScope = computed(() => isSchoolManager.value || isCollegeManager.value)
const canSelectCollegeInForm = computed(() => isSchoolManager.value)
const canSelectLabInForm = computed(() => !isLabManager.value)

const scopeOptions = computed(() => {
  const options = []
  if (isSchoolManager.value) {
    options.push({ label: '学校', value: 'school' })
  }
  if (isSchoolManager.value || isCollegeManager.value) {
    options.push({ label: '学院', value: 'college' })
  }
  options.push({ label: '实验室', value: 'lab' })
  return options
})

const summaryCards = computed(() => [
  { label: '公告总数', value: total.value, tip: '当前筛选结果' },
  { label: '已发布', value: rows.value.filter((item) => Number(item.status) === 1).length, tip: '当前页已发布公告' },
  { label: '草稿', value: rows.value.filter((item) => Number(item.status) !== 1).length, tip: '当前页草稿数量' },
  { label: '默认范围', value: scopeLabel(defaultScope()), tip: '新建公告默认范围' }
])

const defaultScope = () => {
  if (isSchoolManager.value) {
    return 'school'
  }
  if (isCollegeManager.value) {
    return 'college'
  }
  return 'lab'
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    title: '',
    content: '',
    publishScope: defaultScope(),
    collegeId: isSchoolManager.value ? undefined : managedCollegeId.value || undefined,
    labId: isLabManager.value ? managedLabId.value || undefined : undefined,
    status: 1
  })
}

const buildQuery = (page) => ({
  pageNum: page,
  pageSize,
  keyword: filters.keyword || undefined,
  publishScope: filters.publishScope || undefined
})

const fetchPage = async (page) => {
  const response = await getNoticePage(buildQuery(page))
  total.value = Number(response.data?.total || 0)
  return response.data?.records || []
}

const resetAndFetch = async () => {
  loading.value = true
  try {
    pageNum.value = 1
    rows.value = await fetchPage(1)
  } finally {
    loading.value = false
  }
}

const fetchMore = async () => {
  if (loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const nextPage = pageNum.value + 1
    const list = await fetchPage(nextPage)
    pageNum.value = nextPage
    rows.value = rows.value.concat(list)
  } finally {
    loadingMore.value = false
  }
}

const loadColleges = async () => {
  if (isSchoolManager.value) {
    const response = await getCollegeOptions()
    colleges.value = response.data || []
    return
  }

  if (managedCollegeId.value) {
    colleges.value = [
      {
        id: managedCollegeId.value,
        collegeName: userStore.userInfo?.collegeName || userStore.userInfo?.college || `学院#${managedCollegeId.value}`
      }
    ]
  } else {
    colleges.value = []
  }
}

const loadLabs = async (collegeId = undefined) => {
  const params = {
    pageNum: 1,
    pageSize: 200,
    collegeId: collegeId || (isSchoolManager.value ? undefined : managedCollegeId.value || undefined)
  }
  const response = await getLabPage(params)
  labs.value = response.data?.records || []
  if (isLabManager.value && managedLabId.value && !labs.value.some((item) => Number(item.id) === Number(managedLabId.value))) {
    labs.value.unshift({
      id: managedLabId.value,
      labName: userStore.userInfo?.labName || `实验室#${managedLabId.value}`
    })
  }
}

const ensureFormScope = async () => {
  if (form.publishScope === 'school') {
    form.collegeId = undefined
    form.labId = undefined
    return
  }

  if (!isSchoolManager.value) {
    form.collegeId = managedCollegeId.value || form.collegeId
  }

  if (form.publishScope === 'college') {
    form.labId = undefined
    return
  }

  if (isLabManager.value) {
    form.labId = managedLabId.value || form.labId
    return
  }

  await loadLabs(form.collegeId)
}

const handleFormCollegeChange = async () => {
  form.labId = undefined
  if (form.publishScope === 'lab') {
    await loadLabs(form.collegeId)
  }
}

const openEditor = async (row = null) => {
  resetForm()
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title || '',
      content: row.content || '',
      publishScope: row.publishScope || defaultScope(),
      collegeId: row.collegeId || managedCollegeId.value || undefined,
      labId: row.labId || undefined,
      status: Number(row.status ?? 1)
    })
  }
  await ensureFormScope()
  editorVisible.value = true
}

const buildPayload = () => {
  const payload = {
    title: form.title.trim(),
    content: form.content.trim(),
    publishScope: form.publishScope,
    status: Number(form.status)
  }

  if (form.publishScope === 'college') {
    payload.collegeId = form.collegeId || managedCollegeId.value || undefined
  }

  if (form.publishScope === 'lab') {
    payload.collegeId = form.collegeId || managedCollegeId.value || undefined
    payload.labId = form.labId || managedLabId.value || undefined
  }

  return payload
}

const saveNoticeAction = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  if (form.publishScope === 'college' && !(form.collegeId || managedCollegeId.value)) {
    ElMessage.warning('请选择所属学院')
    return
  }
  if (form.publishScope === 'lab' && !(form.labId || managedLabId.value)) {
    ElMessage.warning('请选择所属实验室')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await updateNotice(form.id, payload)
    } else {
      await createNotice(payload)
    }
    ElMessage.success('公告已保存')
    editorVisible.value = false
    await resetAndFetch()
  } finally {
    saving.value = false
  }
}

const removeNotice = async (row) => {
  await ElMessageBox.confirm(`确认删除公告“${row.title || '未命名公告'}”吗？`, '删除公告', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('公告已删除')
  await resetAndFetch()
}

const scopeLabel = (scope) => ({ school: '学校', college: '学院', lab: '实验室' }[scope] || scope || '-')
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

watch(
  () => form.publishScope,
  () => {
    ensureFormScope()
  }
)

onMounted(async () => {
  resetForm()
  await Promise.all([loadColleges(), loadLabs(managedCollegeId.value || undefined), resetAndFetch()])
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.metric-grid,
.meta-grid,
.card-list {
  display: grid;
  gap: 10px;
}

.metric-grid,
.meta-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.toolbar-card,
.record-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.metric-card,
.record-card {
  padding: 14px;
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

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px auto auto;
  gap: 10px;
}

.record-card {
  display: grid;
  gap: 10px;
}

.card-head,
.action-row,
.drawer-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
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

.status-pill.success {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.default {
  color: #475569;
  background: rgba(241, 245, 249, 0.92);
}

.load-more {
  display: flex;
  justify-content: center;
}

.drawer-body {
  padding-bottom: 20px;
}

@media (max-width: 560px) {
  .metric-grid,
  .meta-grid,
  .toolbar-card {
    grid-template-columns: 1fr;
  }
}
</style>

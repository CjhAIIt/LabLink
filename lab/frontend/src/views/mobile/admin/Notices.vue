<template>
  <div class="m-page">
    <section class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索公告标题" @clear="reload">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="openCreate">发布</el-button>
    </section>

    <section v-loading="loading" class="list">
      <button v-for="item in notices" :key="item.id" class="card" type="button" @click="openDetail(item)">
        <div class="card-head">
          <div class="title">
            <strong>{{ item.title || '公告' }}</strong>
            <span>{{ scopeLabel(item.publishScope) }}</span>
          </div>
          <span class="time">{{ formatDate(item.publishTime || item.createTime) }}</span>
        </div>
        <p class="preview">{{ item.content || '暂无内容' }}</p>
      </button>

      <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="80" />
    </section>

    <el-drawer v-model="detailVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">{{ active?.title || '公告详情' }}</strong>
          <button class="drawer-close" type="button" @click="detailVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ formatDate(active?.publishTime || active?.createTime) }}</p>
        <div class="drawer-body">{{ active?.content || '暂无内容' }}</div>
      </div>
    </el-drawer>

    <el-drawer v-model="createVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">发布公告</strong>
          <button class="drawer-close" type="button" @click="createVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="notice-form">
          <el-form-item label="公告标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="80" show-word-limit />
          </el-form-item>
          <el-form-item label="公告内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="2000"
              show-word-limit
              placeholder="请输入公告内容"
            />
          </el-form-item>
          <el-form-item label="发布范围">
            <el-select v-model="form.publishScope" style="width: 100%">
              <el-option label="全校" value="school" />
              <el-option label="本学院" value="college" />
              <el-option label="本实验室" value="lab" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.publishScope === 'college'" label="所属学院">
            <el-select v-model="form.collegeId" placeholder="请选择学院" style="width: 100%" filterable>
              <el-option
                v-for="item in colleges"
                :key="item.id"
                :label="item.collegeName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.publishScope === 'lab'" label="所属实验室">
            <el-select v-model="form.labId" placeholder="请选择实验室" style="width: 100%" filterable>
              <el-option v-for="item in labs" :key="item.id" :label="item.labName" :value="item.id" />
            </el-select>
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="saving" @click="submitForm">发布</el-button>
          </div>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { Close, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { getCollegeOptions } from '@/api/colleges'
import { getLabPage } from '@/api/lab'
import { createNotice, getNoticePage } from '@/api/notices'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const notices = ref([])
const colleges = ref([])
const labs = ref([])

const detailVisible = ref(false)
const createVisible = ref(false)
const active = ref(null)

const formRef = ref()
const form = reactive({
  title: '',
  content: '',
  publishScope: 'school',
  collegeId: undefined,
  labId: undefined,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const SCOPE_LABEL = {
  school: '全校',
  college: '学院',
  lab: '实验室'
}

const scopeLabel = (scope) => SCOPE_LABEL[scope] || scope || '-'

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const openDetail = (notice) => {
  active.value = notice
  detailVisible.value = true
}

const openCreate = () => {
  form.title = ''
  form.content = ''
  form.publishScope = 'school'
  form.collegeId = undefined
  form.labId = undefined
  createVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await createNotice({ ...form })
    ElMessage.success('公告已发布')
    createVisible.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

const loadOptions = async () => {
  const [collegeRes, labRes] = await Promise.all([
    getCollegeOptions(),
    getLabPage({ pageNum: 1, pageSize: 100 })
  ])
  colleges.value = collegeRes.data || []
  labs.value = labRes.data?.records || []
}

const reload = async () => {
  loading.value = true
  try {
    const res = await getNoticePage({
      pageNum: 1,
      pageSize: 50,
      keyword: keyword.value?.trim() || undefined
    })
    notices.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo?.schoolDirector) {
    form.publishScope = 'lab'
    form.labId = userStore.userInfo?.labId
  }
  await Promise.all([loadOptions(), reload()])
})
</script>

<style scoped>
.notice-form {
  margin-top: var(--ll-space-3);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-bottom: var(--ll-space-6);
}
</style>

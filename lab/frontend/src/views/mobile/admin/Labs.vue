<template>
  <div class="m-page">
    <section class="toolbar-card">
      <el-input v-model="filters.keyword" clearable placeholder="搜索实验室名称" @keyup.enter="loadLabs" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 120px" @change="loadLabs">
        <el-option label="开放" :value="1" />
        <el-option label="关闭" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadLabs">查询</el-button>
      <el-button v-if="isSuperAdmin" plain @click="openEditor()">新建</el-button>
    </section>

    <section v-if="isLabManager && latestReview" class="status-card">
      <strong>最近一次资料变更</strong>
      <p>{{ reviewStatusLabel(latestReview.reviewStatus) }}</p>
      <span>{{ latestReview.reviewComment || '暂无审核意见' }}</span>
    </section>

    <section class="card-list">
      <article v-for="row in labs" :key="row.id" class="lab-card">
        <div class="card-head">
          <div>
            <h2>{{ row.labName || '未命名实验室' }}</h2>
            <p>{{ row.labCode || `#${row.id}` }}</p>
          </div>
          <span class="status-pill" :class="Number(row.status) === 1 ? 'online' : 'offline'">
            {{ Number(row.status) === 1 ? '开放' : '关闭' }}
          </span>
        </div>

        <div class="info-grid">
          <div><span>指导教师</span><strong>{{ row.teacherName || '-' }}</strong></div>
          <div><span>位置</span><strong>{{ row.location || '-' }}</strong></div>
          <div><span>计划容量</span><strong>{{ row.recruitNum ?? 0 }}</strong></div>
          <div><span>学院</span><strong>{{ row.collegeName || userStore.userInfo?.collegeName || userStore.userInfo?.college || '-' }}</strong></div>
        </div>

        <p class="description">{{ row.labDesc || row.basicInfo || '暂无实验室介绍' }}</p>

        <div class="card-actions">
          <el-button text @click="router.push(`/lab-info/${row.id}`)">查看详情</el-button>
          <el-button v-if="canEditRow(row)" type="primary" text @click="openEditor(row)">
            {{ isSuperAdmin ? '编辑' : '提交变更' }}
          </el-button>
        </div>
      </article>

      <el-empty v-if="!loading && labs.length === 0" description="暂无实验室数据" :image-size="92" />
    </section>

    <el-drawer v-model="editorVisible" :title="editorTitle" size="92%">
      <div class="drawer-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="实验室名称" prop="labName">
            <el-input v-model="form.labName" placeholder="请输入实验室名称" />
          </el-form-item>
          <el-form-item label="实验室编码" prop="labCode">
            <el-input v-model="form.labCode" placeholder="请输入实验室编码" :disabled="!isSuperAdmin" />
          </el-form-item>
          <el-form-item v-if="isSuperAdmin" label="所属学院" prop="collegeId">
            <el-select v-model="form.collegeId" placeholder="请选择学院" style="width: 100%">
              <el-option v-for="item in colleges" :key="item.id" :label="item.collegeName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="指导教师" prop="teacherName">
            <el-input v-model="form.teacherName" placeholder="请输入指导教师" />
          </el-form-item>
          <el-form-item label="位置">
            <el-input v-model="form.location" placeholder="请输入实验室位置" />
          </el-form-item>
          <el-form-item label="联系邮箱">
            <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
          </el-form-item>
          <el-form-item label="计划容量">
            <el-input-number v-model="form.recruitNum" :min="1" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="isSuperAdmin" label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="开放" :value="1" />
              <el-option label="关闭" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="招新要求">
            <el-input v-model="form.requireSkill" type="textarea" :rows="3" maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item label="实验室简介">
            <el-input v-model="form.labDesc" type="textarea" :rows="4" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item label="基础信息">
            <el-input v-model="form.basicInfo" type="textarea" :rows="4" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item label="成果与荣誉">
            <el-input v-model="form.awards" type="textarea" :rows="4" maxlength="1000" show-word-limit />
          </el-form-item>
        </el-form>

        <section v-if="!isSuperAdmin && reviewHistory.length" class="history-card">
          <header>
            <strong>变更审核记录</strong>
            <span>{{ reviewHistory.length }} 条</span>
          </header>
          <div class="history-list">
            <article v-for="item in reviewHistory" :key="item.reviewId" class="history-item">
              <strong>{{ reviewStatusLabel(item.reviewStatus) }}</strong>
              <p>{{ item.reviewComment || '暂无审核意见' }}</p>
              <span>{{ formatDateTime(item.submitTime) }}</span>
            </article>
          </div>
        </section>
      </div>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="saveForm">
            {{ isSuperAdmin ? '保存' : '提交变更' }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { getLabInfoReviewHistory } from '@/api/labInfoReview'
import { createLab, getLabById, getLabPage, updateLab, updateManagedLab } from '@/api/lab'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const editorVisible = ref(false)
const labs = ref([])
const colleges = ref([])
const reviewHistory = ref([])

const filters = reactive({
  keyword: '',
  status: ''
})

const form = reactive({
  id: null,
  collegeId: undefined,
  labName: '',
  labCode: '',
  teacherName: '',
  location: '',
  contactEmail: '',
  requireSkill: '',
  recruitNum: 20,
  status: 1,
  labDesc: '',
  basicInfo: '',
  awards: ''
})

const rules = {
  labName: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  labCode: [{ required: true, message: '请输入实验室编码', trigger: 'blur' }]
}

const isSuperAdmin = computed(() => userStore.userRole === 'super_admin')
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager))
const managedLabId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)
const latestReview = computed(() => reviewHistory.value[0] || null)
const editorTitle = computed(() => {
  if (!form.id) {
    return '新建实验室'
  }
  return isSuperAdmin.value ? '编辑实验室' : '提交实验室资料变更'
})

const resetForm = () => {
  Object.assign(form, {
    id: null,
    collegeId: undefined,
    labName: '',
    labCode: '',
    teacherName: '',
    location: '',
    contactEmail: '',
    requireSkill: '',
    recruitNum: 20,
    status: 1,
    labDesc: '',
    basicInfo: '',
    awards: ''
  })
}

const fillForm = (row = {}) => {
  Object.assign(form, {
    id: row.id ?? null,
    collegeId: row.collegeId ?? undefined,
    labName: row.labName || '',
    labCode: row.labCode || '',
    teacherName: row.teacherName || '',
    location: row.location || '',
    contactEmail: row.contactEmail || '',
    requireSkill: row.requireSkill || '',
    recruitNum: row.recruitNum ?? 20,
    status: row.status ?? 1,
    labDesc: row.labDesc || '',
    basicInfo: row.basicInfo || '',
    awards: row.awards || ''
  })
}

const loadColleges = async () => {
  if (!isSuperAdmin.value) {
    return
  }
  const response = await getCollegeOptions()
  colleges.value = response.data || []
}

const loadLabs = async () => {
  loading.value = true
  try {
    if (isSuperAdmin.value) {
      const response = await getLabPage({
        pageNum: 1,
        pageSize: 50,
        labName: filters.keyword || undefined,
        status: filters.status === '' ? undefined : filters.status
      })
      labs.value = response.data?.records || []
      return
    }

    if (isCollegeManager.value) {
      const response = await getLabPage({
        pageNum: 1,
        pageSize: 50,
        collegeId: userStore.userInfo?.managedCollegeId,
        labName: filters.keyword || undefined,
        status: filters.status === '' ? undefined : filters.status
      })
      labs.value = response.data?.records || []
      return
    }

    if (!managedLabId.value) {
      labs.value = []
      return
    }

    const response = await getLabById(managedLabId.value)
    labs.value = response.data ? [response.data] : []
  } finally {
    loading.value = false
  }
}

const loadReviewHistory = async (labId) => {
  if (!labId || isSuperAdmin.value) {
    reviewHistory.value = []
    return
  }
  const response = await getLabInfoReviewHistory({ labId })
  reviewHistory.value = response.data || []
}

const openEditor = async (row = null) => {
  resetForm()
  if (row?.id) {
    fillForm(row)
    await loadReviewHistory(row.id)
  } else {
    reviewHistory.value = []
  }
  editorVisible.value = true
}

const canEditRow = (row) => {
  if (isSuperAdmin.value) {
    return true
  }
  return Boolean(isLabManager.value && managedLabId.value && Number(managedLabId.value) === Number(row.id))
}

const saveForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = { ...form }
    if (isSuperAdmin.value) {
      if (form.id) {
        await updateLab(form.id, payload)
      } else {
        await createLab(payload)
      }
      ElMessage.success('实验室信息已保存')
    } else {
      await updateManagedLab(payload)
      ElMessage.success('资料变更已提交，等待学校管理员审核')
    }
    editorVisible.value = false
    await loadLabs()
    if (managedLabId.value) {
      await loadReviewHistory(managedLabId.value)
    }
  } finally {
    submitting.value = false
  }
}

const reviewStatusLabel = (status) => {
  const value = String(status || '').toUpperCase()
  if (value === 'PENDING') return '待审核'
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已驳回'
  return status || '-'
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  await Promise.all([loadColleges(), loadLabs()])
  if (managedLabId.value) {
    await loadReviewHistory(managedLabId.value)
  }
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.status-card,
.lab-card,
.history-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px auto auto;
  gap: 10px;
}

.status-card,
.lab-card,
.history-card {
  padding: 14px;
}

.status-card strong,
.lab-card h2,
.history-card strong {
  color: #0f172a;
}

.status-card p {
  margin: 8px 0 4px;
  color: #d97706;
  font-weight: 700;
}

.status-card span,
.description,
.history-item p,
.history-item span {
  color: #64748b;
  line-height: 1.6;
}

.card-list,
.history-list {
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head h2 {
  margin: 0;
  font-size: 16px;
}

.card-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.info-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.info-grid strong {
  color: #0f172a;
}

.description {
  margin: 12px 0 0;
}

.card-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.online {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.offline {
  color: #475569;
  background: rgba(241, 245, 249, 0.92);
}

.drawer-body {
  padding-bottom: 18px;
}

.history-card {
  margin-top: 14px;
}

.history-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.history-item {
  border-radius: 14px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 560px) {
  .toolbar-card,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>

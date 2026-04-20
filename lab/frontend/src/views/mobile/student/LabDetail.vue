<template>
  <div class="m-page">
    <section v-if="lab" class="hero-card">
      <p class="eyebrow">Lab Detail</p>
      <h1>{{ lab.labName }}</h1>
      <p>{{ lab.labDesc || '暂无实验室简介' }}</p>
      <div class="meta-row">
        <span>{{ lab.teacherName || '指导教师待完善' }}</span>
        <span>{{ lab.location || '位置待完善' }}</span>
      </div>
      <el-button type="primary" size="large" :disabled="Boolean(userStore.userInfo?.labId)" @click="openApplyDialog">
        {{ userStore.userInfo?.labId ? '已加入实验室' : '申请加入' }}
      </el-button>
    </section>

    <section v-if="lab" class="panel-card">
      <header class="panel-head"><h2>基础信息</h2></header>
      <div class="content-text">{{ lab.basicInfo || '暂无基础信息' }}</div>
    </section>

    <section v-if="lab" class="panel-card">
      <header class="panel-head"><h2>招新要求</h2></header>
      <div v-if="skillList.length" class="tag-list">
        <span v-for="item in skillList" :key="item" class="skill-tag">{{ item }}</span>
      </div>
      <el-empty v-else description="暂未设置招新要求" :image-size="72" />
    </section>

    <section v-if="lab" class="panel-card">
      <header class="panel-head"><h2>成果与荣誉</h2></header>
      <div class="content-text">{{ lab.awards || '暂无成果展示' }}</div>
    </section>

    <el-skeleton v-if="loading" animated :rows="6" />
    <el-empty v-else-if="!lab" description="未找到实验室详情" :image-size="88" />

    <el-dialog v-model="dialogVisible" title="申请加入实验室" width="92%">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="申请理由" prop="applyReason">
          <el-input v-model="form.applyReason" type="textarea" :rows="3" placeholder="简要说明你为什么希望加入该实验室" />
        </el-form-item>
        <el-form-item label="研究兴趣">
          <el-input v-model="form.researchInterest" placeholder="例如：前端 / 算法 / 嵌入式 / AI" />
        </el-form-item>
        <el-form-item label="技能概述">
          <el-input v-model="form.skillSummary" type="textarea" :rows="3" placeholder="可以填写项目经历或技能栈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createLabApply } from '@/api/labApplies'
import { getLabById } from '@/api/lab'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const lab = ref(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  applyReason: '',
  researchInterest: '',
  skillSummary: ''
})

const rules = {
  applyReason: [{ required: true, message: '请输入申请理由', trigger: 'blur' }]
}

const skillList = computed(() => {
  if (!lab.value?.requireSkill) {
    return []
  }
  return String(lab.value.requireSkill)
    .split(/[，,、\s/]+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const loadDetail = async () => {
  loading.value = true
  try {
    const response = await getLabById(route.params.id)
    lab.value = response.data || null
  } finally {
    loading.value = false
  }
}

const openApplyDialog = () => {
  if (userStore.userInfo?.labId) {
    ElMessage.warning('你已经加入实验室，不能重复申请')
    return
  }
  if (!userStore.userInfo?.resume) {
    ElMessage.warning('请先到个人资料页上传简历后再申请实验室')
    router.push('/m/student/profile')
    return
  }
  dialogVisible.value = true
}

const submitApply = async () => {
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
    await createLabApply({
      labId: lab.value.id,
      applyReason: form.applyReason.trim(),
      researchInterest: form.researchInterest || undefined,
      skillSummary: form.skillSummary || undefined
    })
    form.applyReason = ''
    form.researchInterest = ''
    form.skillSummary = ''
    dialogVisible.value = false
    ElMessage.success('申请已提交，请等待审核')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.hero-card,
.panel-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.hero-card {
  padding: 18px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.94), rgba(29, 78, 216, 0.88));
  color: #f8fafc;
  display: grid;
  gap: 12px;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.82;
}

.hero-card h1 {
  margin: 0;
  font-size: 24px;
}

.hero-card p,
.meta-row {
  margin: 0;
  color: rgba(226, 232, 240, 0.92);
  line-height: 1.6;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.panel-card {
  padding: 14px;
}

.panel-head h2 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #0f172a;
}

.content-text {
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(219, 234, 254, 0.92);
  color: #1d4ed8;
  font-weight: 700;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

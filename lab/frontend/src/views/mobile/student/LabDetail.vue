<template>
  <div class="m-page">
    <section v-if="lab" class="hero">
      <p class="hero-code">{{ lab.labCode || `#${lab.id}` }}</p>
      <h1 class="hero-title">{{ lab.labName }}</h1>
      <p class="hero-desc">{{ lab.labDesc || '暂无简介' }}</p>
      <div class="hero-meta">
        <span class="meta-chip">
          <el-icon :size="16"><User /></el-icon>
          <span>{{ lab.teacherName || '指导教师待维护' }}</span>
        </span>
        <span v-if="lab.location" class="meta-chip">
          <el-icon :size="16"><Location /></el-icon>
          <span>{{ lab.location }}</span>
        </span>
      </div>
      <div class="hero-actions">
        <el-button
          type="primary"
          size="large"
          style="width: 100%"
          :disabled="Boolean(userStore.userInfo?.labId)"
          @click="openApply"
        >
          {{ userStore.userInfo?.labId ? '已加入实验室' : '申请加入' }}
        </el-button>
      </div>
    </section>

    <section v-if="lab" class="panel">
      <header class="panel-header">
        <h2>实验室介绍</h2>
      </header>
      <div class="panel-body">{{ lab.basicInfo || '暂无详细介绍' }}</div>
    </section>

    <section v-if="lab" class="panel">
      <header class="panel-header">
        <h2>所需技能</h2>
      </header>
      <div class="tags">
        <el-tag v-for="item in skillList" :key="item" effect="plain" class="tag">{{ item }}</el-tag>
        <span v-if="!skillList.length" class="muted">暂未明确技能要求</span>
      </div>
    </section>

    <section v-if="lab" class="panel">
      <header class="panel-header">
        <h2>荣誉展示</h2>
      </header>
      <div class="panel-body">{{ lab.awards || '暂无荣誉展示' }}</div>
    </section>

    <el-skeleton v-if="loading" animated :rows="6" />
    <el-empty v-else-if="!lab" description="未找到实验室详情" :image-size="90" />

    <el-dialog v-model="applyVisible" title="申请加入实验室" width="92%">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-position="top">
        <el-form-item label="申请理由" prop="applyReason">
          <el-input v-model="applyForm.applyReason" type="textarea" :rows="3" placeholder="简单说明你为什么想加入" />
        </el-form-item>
        <el-form-item label="研究兴趣（可选）" prop="researchInterest">
          <el-input v-model="applyForm.researchInterest" placeholder="例如：前端 / 算法 / 嵌入式 / AI..." />
        </el-form-item>
        <el-form-item label="技能概述（可选）" prop="skillSummary">
          <el-input v-model="applyForm.skillSummary" type="textarea" :rows="3" placeholder="你掌握的技能与项目经历" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="applyVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Location, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getLabById } from '@/api/lab'
import { createLabApply } from '@/api/labApplies'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const lab = ref(null)
const loading = ref(false)

const applyVisible = ref(false)
const submitting = ref(false)
const applyFormRef = ref()

const applyForm = reactive({
  applyReason: '',
  researchInterest: '',
  skillSummary: ''
})

const applyRules = reactive({
  applyReason: [{ required: true, message: '请输入申请理由', trigger: 'blur' }]
})

const skillList = computed(() => {
  if (!lab.value?.requireSkill) return []
  return String(lab.value.requireSkill)
    .split(/[，,、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getLabById(id)
    lab.value = res.data || null
  } finally {
    loading.value = false
  }
}

const openApply = () => {
  if (userStore.userInfo?.labId) {
    ElMessage.warning('你已加入实验室，不能重复申请')
    return
  }
  applyVisible.value = true
}

const submitApply = async () => {
  if (!lab.value?.id) return
  await applyFormRef.value?.validate?.()
  submitting.value = true
  try {
    await createLabApply({
      labId: lab.value.id,
      applyReason: applyForm.applyReason.trim(),
      researchInterest: applyForm.researchInterest?.trim() || undefined,
      skillSummary: applyForm.skillSummary?.trim() || undefined
    })
    applyVisible.value = false
    applyForm.applyReason = ''
    applyForm.researchInterest = ''
    applyForm.skillSummary = ''
    ElMessage.success('申请已提交，请等待审核')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(135deg, #0f172a 0%, #1e40af 100%);
  color: #ffffff;
}

.hero-code {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.hero-title {
  font-size: 20px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.hero-desc {
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.6;
  margin-bottom: 12px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
}

.panel {
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.panel-header {
  margin-bottom: 8px;
}

.panel-header h2 {
  font-size: 15px;
  color: #0f172a;
}

.panel-body {
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  border-radius: 999px;
}

.muted {
  color: #94a3b8;
  font-size: 13px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>

<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室查询</p>
          <h2>浏览实验室与开放招新计划</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
        </div>
      </div>

      <div class="status-notice-group">
        <el-alert
          v-if="!hasResume"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #title>
            <div class="alert-title-row">
              <span>未提交简历，请先在个人资料提交个人简历。</span>
              <el-button link type="warning" @click="router.push('/student/profile')">前往个人资料</el-button>
            </div>
          </template>
        </el-alert>

        <el-alert
          v-if="userStore.userInfo?.labId"
          type="success"
          :closable="false"
          title="你已加入实验室，仍可继续浏览实验室信息，但不能重复提交入组申请。"
        />
      </div>
    </section>

    <section class="college-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">学院导航</p>
          <h3>先选择学院，再查看对应实验室</h3>
        </div>
        <el-tag type="info" effect="plain">{{ collegeSections.length }} 个学院分组</el-tag>
      </div>

      <div v-if="collegeSections.length" class="college-grid">
        <button
          v-for="college in collegeSections"
          :key="college.id"
          type="button"
          class="college-card"
          :class="{ active: String(college.id) === String(activeCollege?.id) }"
          @click="activeCollegeId = college.id"
        >
          <span class="college-card-code">{{ college.code || 'COLLEGE' }}</span>
          <strong>{{ college.name }}</strong>
          <span>{{ college.labs.length }} 个实验室</span>
        </button>
      </div>
      <el-empty v-else description="当前暂无可浏览的实验室" />
    </section>

    <section v-if="activeCollege" class="college-labs-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">当前学院</p>
          <h3>{{ activeCollege.name }}</h3>
        </div>
        <el-tag type="primary" effect="plain">{{ visibleLabs.length }} 个实验室</el-tag>
      </div>

      <div class="lab-grid">
        <el-card v-for="lab in visibleLabs" :key="lab.id" shadow="never" class="lab-card">
          <div class="lab-card-head">
            <div>
              <p class="lab-code">{{ lab.labCode || '未配置编码' }}</p>
              <h3>{{ lab.labName }}</h3>
            </div>
            <el-tag :type="lab.status === 1 ? 'success' : 'info'">{{ lab.status === 1 ? '开放' : '关闭' }}</el-tag>
          </div>

          <p class="lab-desc">{{ lab.labDesc || '暂无实验室简介' }}</p>

          <div class="lab-meta">
            <span>指导教师：{{ lab.teacherName || '待维护' }}</span>
            <span>地点：{{ lab.location || '待维护' }}</span>
            <span>计划容量：{{ lab.recruitNum || 0 }}</span>
          </div>

          <div class="plan-chip-group">
            <el-tag
              v-for="plan in plansByLab[lab.id] || []"
              :key="plan.id"
              type="primary"
              effect="plain"
            >
              {{ plan.title }}
            </el-tag>
          </div>

          <div class="lab-card-actions">
            <el-button @click="router.push(`/lab-info/${lab.id}`)">查看详情</el-button>
            <el-button type="primary" :disabled="applyState(lab).disabled" @click="openApplyDialog(lab)">
              {{ applyState(lab).label }}
            </el-button>
          </div>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="提交实验室申请" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="实验室">
          <el-input :model-value="selectedLab?.labName || ''" disabled />
        </el-form-item>
        <el-form-item label="招新计划" prop="recruitPlanId">
          <el-select v-model="form.recruitPlanId" placeholder="请选择开放计划">
            <el-option
              v-for="plan in selectedPlans"
              :key="plan.id"
              :label="plan.title"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由" prop="applyReason">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="研究兴趣">
          <el-input v-model="form.researchInterest" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="技能概述">
          <el-input v-model="form.skillSummary" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { createLabApply } from '@/api/labApplies'
import { getLabPage } from '@/api/lab'
import { getActiveRecruitPlans } from '@/api/recruitPlans'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const dialogVisible = ref(false)
const submitting = ref(false)
const labs = ref([])
const activePlans = ref([])
const colleges = ref([])
const selectedLab = ref(null)
const activeCollegeId = ref(null)
const hasResume = computed(() => Boolean(userStore.userInfo?.resume))

const form = reactive({
  labId: null,
  recruitPlanId: null,
  applyReason: '',
  researchInterest: '',
  skillSummary: ''
})

const rules = {
  recruitPlanId: [{ required: true, message: '请选择招新计划', trigger: 'change' }],
  applyReason: [{ required: true, message: '请输入申请理由', trigger: 'blur' }]
}

const plansByLab = computed(() =>
  activePlans.value.reduce((acc, item) => {
    if (!acc[item.labId]) {
      acc[item.labId] = []
    }
    acc[item.labId].push(item)
    return acc
  }, {})
)

const selectedPlans = computed(() => {
  if (!selectedLab.value) {
    return []
  }
  return plansByLab.value[selectedLab.value.id] || []
})

const collegeSections = computed(() => {
  const groupedLabs = labs.value.reduce((acc, lab) => {
    const key = lab.collegeId ?? 'unassigned'
    if (!acc[key]) {
      acc[key] = []
    }
    acc[key].push(lab)
    return acc
  }, {})

  const sections = colleges.value
    .map((college) => ({
      id: college.id,
      name: college.collegeName,
      code: college.collegeCode,
      labs: groupedLabs[college.id] || []
    }))
    .filter((college) => college.labs.length > 0)

  if (groupedLabs.unassigned?.length) {
    sections.push({
      id: 'unassigned',
      name: '未归属学院',
      code: 'UNASSIGNED',
      labs: groupedLabs.unassigned
    })
  }

  return sections
})

const activeCollege = computed(() =>
  collegeSections.value.find((item) => String(item.id) === String(activeCollegeId.value)) || collegeSections.value[0] || null
)

const visibleLabs = computed(() => activeCollege.value?.labs || [])

const loadData = async () => {
  const [labRes, planRes, collegeRes] = await Promise.all([
    getLabPage({ pageNum: 1, pageSize: 100, status: 1 }),
    getActiveRecruitPlans(),
    getCollegeOptions()
  ])
  labs.value = labRes.data.records || []
  activePlans.value = planRes.data || []
  colleges.value = collegeRes.data || []

  if (!collegeSections.value.some((item) => String(item.id) === String(activeCollegeId.value))) {
    activeCollegeId.value = collegeSections.value[0]?.id || null
  }
}

const applyState = (lab) => {
  if (userStore.userInfo?.labId) {
    return {
      disabled: true,
      label: '已加入其他实验室'
    }
  }

  if (!hasResume.value) {
    return {
      disabled: true,
      label: '请先上传简历'
    }
  }

  if (lab.status !== 1) {
    return {
      disabled: true,
      label: '实验室未开放'
    }
  }

  if ((plansByLab.value[lab.id] || []).length === 0) {
    return {
      disabled: true,
      label: '暂无开放计划'
    }
  }

  return {
    disabled: false,
    label: '申请加入'
  }
}

const openApplyDialog = (lab) => {
  if (!hasResume.value) {
    ElMessage.warning('请先在个人信息中上传简历后再申请加入实验室')
    return
  }
  selectedLab.value = lab
  Object.assign(form, {
    labId: lab.id,
    recruitPlanId: plansByLab.value[lab.id]?.[0]?.id || null,
    applyReason: '',
    researchInterest: '',
    skillSummary: ''
  })
  dialogVisible.value = true
}

const submitApply = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    await createLabApply({ ...form })
    ElMessage.success('申请已提交，请等待审核')
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.status-notice-group,
.college-section,
.college-labs-section {
  display: grid;
  gap: 16px;
}

.status-notice-group {
  margin-top: 20px;
}

.alert-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-heading h3 {
  margin: 4px 0 0;
  color: #0f172a;
}

.college-section,
.college-labs-section {
  margin-top: 24px;
}

.college-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.college-card {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.98), rgba(236, 254, 255, 0.92));
  padding: 20px 22px;
  display: grid;
  gap: 8px;
  text-align: left;
  cursor: pointer;
  color: #0f172a;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.college-card:hover,
.college-card.active {
  border-color: rgba(15, 118, 110, 0.45);
  box-shadow: 0 14px 28px -22px rgba(15, 118, 110, 0.7);
  transform: translateY(-2px);
}

.college-card-code {
  color: #0f766e;
  font-weight: 700;
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.lab-card {
  border-radius: 24px;
}

.lab-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.lab-card-head h3 {
  margin-top: 4px;
  color: #0f172a;
}

.lab-code {
  color: #0f766e;
  font-weight: 700;
}

.lab-desc {
  color: #475569;
  line-height: 1.7;
  min-height: 68px;
}

.lab-meta {
  display: grid;
  gap: 8px;
  margin: 16px 0;
  color: #64748b;
}

.plan-chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}

.lab-card-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .alert-title-row,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

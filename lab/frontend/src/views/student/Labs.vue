<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">实验室广场</p>
          <h2>查看实验室信息与开放招新入口</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
        </div>
      </div>

      <div class="status-notice-group">
        <el-alert
          v-if="userStore.userInfo?.labId"
          type="success"
          :closable="false"
          title="你已经加入实验室，当前仍可查看实验室信息，但不能再次提交加入申请。"
        />
        <el-alert
          v-else
          class="resume-tip"
          :type="hasResume ? 'success' : 'warning'"
          :closable="false"
          show-icon
          title="温馨提示：申请实验室前请先提交个人简历，未提交简历将无法申请"
          :description="hasResume ? '已检测到你的简历，可以正常提交实验室申请。' : '请先到个人资料页完善并提交个人简历，系统会在后端再次校验。'"
        >
          <template #default>
            <el-button v-if="!hasResume" size="small" type="warning" @click="router.push('/student/profile')">
              去完善简历
            </el-button>
          </template>
        </el-alert>
      </div>
    </section>

    <TablePageCard
      class="college-section"
      title="学院导航"
      subtitle="先选择学院，再查看对应实验室"
      :count-label="`${collegeSections.length} 组`"
    >
      <div v-if="collegeSections.length" class="college-grid">
        <button
          v-for="college in collegeSections"
          :key="college.id"
          type="button"
          class="college-card"
          :class="{ active: String(college.id) === String(activeCollege?.id) }"
          @click="activeCollegeId = college.id"
        >
          <span class="college-card-code">{{ college.code || '学院' }}</span>
          <strong>{{ college.name }}</strong>
          <span>{{ college.labs.length }} 个实验室</span>
        </button>
      </div>
      <el-empty v-else description="当前暂无可见实验室。" />
    </TablePageCard>

    <TablePageCard
      v-if="activeCollege"
      class="college-labs-section"
      :title="activeCollege.name"
      subtitle="所选学院下的实验室"
      :count-label="`${visibleLabs.length} 个实验室`"
      count-tag-type="primary"
    >
      <div class="lab-grid">
        <el-card v-for="lab in visibleLabs" :key="lab.id" shadow="never" class="lab-card">
          <div class="lab-card-head">
            <div>
              <p class="lab-code">{{ lab.labCode || '未分配编号' }}</p>
              <h3>{{ lab.labName }}</h3>
            </div>
            <StatusTag :value="lab.status" :label-map="labStatusLabels" :type-map="labStatusTypes" />
          </div>

          <p class="lab-desc">{{ lab.labDesc || '暂无实验室介绍。' }}</p>

          <div class="lab-meta">
            <span>指导教师：{{ lab.teacherName || '待分配' }}</span>
            <span>地点：{{ lab.location || '待补全' }}</span>
            <span>计划名额：{{ lab.recruitNum || 0 }}</span>
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
    </TablePageCard>

    <el-dialog v-model="dialogVisible" title="申请加入实验室" width="620px">
      <el-alert
        class="dialog-resume-tip"
        type="info"
        :closable="false"
        show-icon
        title="提交前请确认个人简历已完善，未提交简历的申请会被系统拦截。"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="实验室">
          <el-input :model-value="selectedLab?.labName || ''" disabled />
        </el-form-item>
        <el-form-item v-if="selectedPlans.length" label="计划" prop="recruitPlanId">
          <el-select v-model="form.recruitPlanId" placeholder="请选择开放中的计划">
            <el-option
              v-for="plan in selectedPlans"
              :key="plan.id"
              :label="plan.title"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="计划">
          <el-alert
            type="info"
            :closable="false"
            title="当前未单独配置招新计划"
            description="这次会按实验室通用入组申请提交。"
          />
        </el-form-item>
        <el-form-item label="申请原因" prop="applyReason">
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
        <el-button type="primary" :loading="submitting" @click="submitApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCollegeOptions } from '@/api/colleges'
import { createLabApply } from '@/api/labApplies'
import { getLabPage } from '@/api/lab'
import { getActiveRecruitPlans } from '@/api/recruitPlans'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const dialogVisible = ref(false)
const submitting = ref(false)
const labs = ref([])
const activePlans = ref([])
const colleges = ref([])
const selectedLab = ref(null)
const activeCollegeId = ref(null)
const hasResume = computed(() => Boolean(String(userStore.userInfo?.resume || '').trim()))

const labStatusLabels = {
  0: '已关闭',
  1: '开放中'
}

const labStatusTypes = {
  0: 'info',
  1: 'success'
}

const form = reactive({
  labId: null,
  recruitPlanId: null,
  applyReason: '',
  researchInterest: '',
  skillSummary: ''
})

const selectedPlans = computed(() => {
  if (!selectedLab.value) {
    return []
  }
  return plansByLab.value[selectedLab.value.id] || []
})

const validateRecruitPlan = (_rule, value, callback) => {
  if (selectedPlans.value.length > 0 && !value) {
    callback(new Error('请先选择招新计划'))
    return
  }
  callback()
}

const rules = {
  recruitPlanId: [{ validator: validateRecruitPlan, trigger: 'change' }],
  applyReason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }]
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
      code: '未归属',
      labs: groupedLabs.unassigned
    })
  }

  return sections
})

const activeCollege = computed(() =>
  collegeSections.value.find((item) => String(item.id) === String(activeCollegeId.value)) || collegeSections.value[0] || null
)

const visibleLabs = computed(() => activeCollege.value?.labs || [])

const getLabPlans = (labId) => plansByLab.value[labId] || []

const applyState = (lab) => {
  if (userStore.userInfo?.labId) {
    return {
      disabled: true,
      label: '已加入其他实验室'
    }
  }

  if (!hasResume.value) {
    return {
      disabled: false,
      label: '先提交简历'
    }
  }

  if (Number(lab.status) !== 1) {
    return {
      disabled: true,
      label: '当前未开放招新'
    }
  }

  return {
    disabled: false,
    label: getLabPlans(lab.id).length > 0 ? '立即申请' : '直接申请'
  }
}

const openApplyDialog = (lab) => {
  if (!hasResume.value) {
    ElMessageBox.confirm('请先完善并提交个人简历，再申请实验室。现在去个人资料页吗？', '需要先提交简历', {
      type: 'warning',
      confirmButtonText: '去完善简历',
      cancelButtonText: '稍后再说'
    }).then(() => {
      router.push('/student/profile')
    }).catch(() => {})
    return
  }
  selectedLab.value = lab
  Object.assign(form, {
    labId: lab.id,
    recruitPlanId: getLabPlans(lab.id)[0]?.id || null,
    applyReason: '',
    researchInterest: '',
    skillSummary: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const consumeApplyLabQuery = async () => {
  const rawLabId = route.query.applyLabId
  if (!rawLabId) {
    return
  }

  const targetLabId = Number(rawLabId)
  const nextQuery = { ...route.query }
  delete nextQuery.applyLabId
  await router.replace({ path: route.path, query: nextQuery })

  if (!Number.isFinite(targetLabId)) {
    return
  }

  const targetLab = labs.value.find((item) => Number(item.id) === targetLabId)
  if (!targetLab) {
    ElMessage.warning('未找到目标实验室')
    return
  }

  const state = applyState(targetLab)
  if (state.disabled) {
    ElMessage.warning(state.label)
    return
  }

  activeCollegeId.value = targetLab.collegeId ?? 'unassigned'
  openApplyDialog(targetLab)
}

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

  await consumeApplyLabQuery()
}

const submitApply = async () => {
  if (!hasResume.value) {
    ElMessage.warning('请先完善并提交个人简历，再申请实验室')
    await router.push('/student/profile')
    return
  }
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
      ...form,
      recruitPlanId: form.recruitPlanId || undefined,
      applyReason: form.applyReason.trim(),
      researchInterest: form.researchInterest || undefined,
      skillSummary: form.skillSummary || undefined
    })
    ElMessage.success('申请已提交')
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
.status-notice-group {
  margin-top: 20px;
}

.resume-tip,
.dialog-resume-tip {
  margin-top: 12px;
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
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .alert-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .alert-actions {
    gap: 6px;
  }

  .college-grid,
  .lab-grid {
    grid-template-columns: 1fr;
  }

  .college-card {
    padding: 18px;
    border-radius: 20px;
  }

  .lab-card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .lab-desc {
    min-height: 0;
  }

  .lab-card-actions .el-button {
    flex: 1 1 140px;
    margin-left: 0;
  }
}
</style>

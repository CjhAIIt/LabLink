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

      <el-alert
        v-if="userStore.userInfo?.labId"
        type="success"
        :closable="false"
        title="你已加入实验室，仍可继续浏览实验室信息，但不能重复提交入组申请。"
      />
    </section>

    <section class="lab-grid">
      <el-card v-for="lab in labs" :key="lab.id" shadow="never" class="lab-card">
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
          <el-button @click="$router.push(`/lab-info/${lab.id}`)">查看详情</el-button>
          <el-button
            type="primary"
            :disabled="!canApply(lab)"
            @click="openApplyDialog(lab)"
          >
            {{ canApply(lab) ? '申请加入' : '当前不可申请' }}
          </el-button>
        </div>
      </el-card>
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
import { createLabApply } from '@/api/labApplies'
import { getLabPage } from '@/api/lab'
import { getActiveRecruitPlans } from '@/api/recruitPlans'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const dialogVisible = ref(false)
const submitting = ref(false)
const labs = ref([])
const activePlans = ref([])
const selectedLab = ref(null)
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

const loadData = async () => {
  const [labRes, planRes] = await Promise.all([
    getLabPage({ pageNum: 1, pageSize: 100, status: 1 }),
    getActiveRecruitPlans()
  ])
  labs.value = labRes.data.records || []
  activePlans.value = planRes.data || []
}

const canApply = (lab) =>
  !userStore.userInfo?.labId && hasResume.value && lab.status === 1 && (plansByLab.value[lab.id] || []).length > 0

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
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
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
</style>

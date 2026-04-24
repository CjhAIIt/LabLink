<template>
  <MobilePageContainer>
    <template v-if="lab">
      <section class="lab-detail-cover" :style="heroStyle">
        <div class="lab-detail-cover__content">
          <div class="lab-detail-cover__logo">
            <img v-if="resolvedLogo" :src="resolvedLogo" alt="实验室 Logo" />
            <el-icon v-else><OfficeBuilding /></el-icon>
          </div>
          <span class="mobile-kicker">{{ lab.collegeName || lab.college || '学院待完善' }}</span>
          <h1>{{ lab.labName }}</h1>
          <p>{{ lab.labDesc || lab.basicInfo || '该实验室正在完善介绍资料。' }}</p>
          <div class="lab-detail-cover__tags">
            <MobileStatusTag type="active" :label="userStore.userInfo?.labId ? '已加入实验室' : '可申请加入'" />
            <span>{{ lab.teacherName || lab.advisors || '指导教师待完善' }}</span>
            <span>{{ lab.location || '位置待完善' }}</span>
          </div>
        </div>
      </section>

      <section class="lab-detail-stats">
        <MobileStatCard label="成员规模" :value="`${lab.currentNum ?? 0} / ${lab.recruitNum ?? '不限'}`" description="当前成员 / 招新名额" />
        <MobileStatCard label="成立时间" :value="lab.foundingDate || '待完善'" description="实验室建设周期" accent="#19A7B8" />
      </section>

      <section v-if="!userStore.userInfo?.labId" class="resume-guide-card" :class="{ success: hasResume }">
        <div>
          <strong>{{ hasResume ? '已检测到个人简历' : '申请前请先提交个人简历' }}</strong>
          <p>{{ hasResume ? '可以直接提交申请，建议在申请理由中补充你与实验室方向的匹配点。' : '完善简历后再申请，老师能更快了解你的经历和技能。' }}</p>
        </div>
        <el-button :type="hasResume ? 'success' : 'primary'" plain @click="router.push('/m/student/profile')">
          {{ hasResume ? '查看简历' : '去提交简历' }}
        </el-button>
      </section>

      <section class="lab-detail-section">
        <header><h2>实验室简介</h2></header>
        <p>{{ lab.basicInfo || lab.labDesc || '暂无基础信息。' }}</p>
      </section>

      <section class="lab-detail-section">
        <header><h2>招新方向</h2></header>
        <div v-if="skillList.length" class="lab-detail-tags">
          <span v-for="item in skillList" :key="item">{{ item }}</span>
        </div>
        <MobileEmptyState
          v-else
          icon="CollectionTag"
          title="暂未设置招新要求"
          description="可以先关注实验室介绍，或联系指导老师了解更多。"
        />
      </section>

      <section class="lab-detail-section">
        <header><h2>成果与荣誉</h2></header>
        <p>{{ lab.awards || '暂无成果展示。' }}</p>
      </section>

      <section class="lab-detail-section">
        <header>
          <h2>优秀学长</h2>
          <span>{{ graduateCards.length }} 位</span>
        </header>
        <div v-if="graduateCards.length" class="graduate-list">
          <article v-for="item in graduateCards" :key="item.id || item.name" class="graduate-card">
            <div class="graduate-card__head">
              <el-avatar :size="44" :src="resolveMedia(item.avatarUrl)">
                {{ (item.name || 'G').charAt(0) }}
              </el-avatar>
              <div>
                <strong>{{ item.name || '优秀学长' }}</strong>
                <p>{{ item.major || '专业待完善' }}</p>
              </div>
            </div>
            <div v-if="item.coverImageUrl" class="graduate-card__cover">
              <img :src="resolveMedia(item.coverImageUrl)" alt="展示图" />
            </div>
            <p>{{ item.description || item.achievement || '暂无介绍。' }}</p>
          </article>
        </div>
        <MobileEmptyState
          v-else
          icon="UserFilled"
          title="暂未展示优秀学长"
          description="实验室资料完善后，这里会展示优秀成员和成长故事。"
        />
      </section>
    </template>

    <el-skeleton v-if="loading" animated :rows="7" />
    <MobileEmptyState
      v-else-if="!lab"
      icon="OfficeBuilding"
      title="未找到实验室详情"
      description="该实验室可能已下线或你暂无查看权限。"
    />

    <div v-if="lab" class="lab-detail-sticky-apply">
      <div>
        <span>{{ userStore.userInfo?.labId ? '当前状态' : '准备好加入？' }}</span>
        <strong>{{ userStore.userInfo?.labId ? '你已加入实验室' : lab.labName }}</strong>
      </div>
      <el-button type="primary" :disabled="Boolean(userStore.userInfo?.labId)" @click="openApplyDialog">
        {{ userStore.userInfo?.labId ? '已加入' : '申请加入' }}
      </el-button>
    </div>

    <MobileActionSheet
      v-model="dialogVisible"
      title="申请加入实验室"
      description="请用简洁、具体的内容说明你为什么适合这个实验室。"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="申请理由" prop="applyReason">
          <el-input v-model="form.applyReason" type="textarea" :rows="4" placeholder="例如：相关课程、项目经历、研究兴趣与实验室方向的匹配点" />
        </el-form-item>
        <el-form-item label="研究兴趣">
          <el-input v-model="form.researchInterest" placeholder="例如：前端 / 算法 / 嵌入式 / AI" />
        </el-form-item>
        <el-form-item label="技能概述">
          <el-input v-model="form.skillSummary" type="textarea" :rows="3" placeholder="可以填写项目经历或技能栈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="sheet-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
        </div>
      </template>
    </MobileActionSheet>
  </MobilePageContainer>
</template>

<script setup>
import { OfficeBuilding } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createLabApply } from '@/api/labApplies'
import { getGraduateList } from '@/api/graduate'
import { getLabById } from '@/api/lab'
import MobileActionSheet from '@/components/mobile/MobileActionSheet.vue'
import MobileEmptyState from '@/components/mobile/MobileEmptyState.vue'
import MobilePageContainer from '@/components/mobile/MobilePageContainer.vue'
import MobileStatCard from '@/components/mobile/MobileStatCard.vue'
import MobileStatusTag from '@/components/mobile/MobileStatusTag.vue'
import { useUserStore } from '@/stores/user'
import { resolveFileUrl } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const lab = ref(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const graduates = ref([])
const formRef = ref()

const form = reactive({
  applyReason: '',
  researchInterest: '',
  skillSummary: ''
})

const rules = {
  applyReason: [{ required: true, message: '请输入申请理由', trigger: 'blur' }]
}

const resolvedLogo = computed(() => resolveMedia(lab.value?.logoUrl))
const resolvedCover = computed(() => resolveMedia(lab.value?.coverImageUrl))
const hasResume = computed(() => Boolean(String(userStore.userInfo?.resume || '').trim()))
const heroStyle = computed(() => ({
  backgroundImage: resolvedCover.value
    ? `linear-gradient(135deg, rgba(15, 35, 55, 0.82), rgba(23, 107, 154, 0.46)), url(${resolvedCover.value})`
    : 'linear-gradient(135deg, #15324b, #176b9a 58%, #19a7b8)'
}))

const skillList = computed(() => {
  if (!lab.value?.requireSkill && !lab.value?.researchDirection) {
    return []
  }
  return String(lab.value.requireSkill || lab.value.researchDirection)
    .split(/[，,、/\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const graduateCards = computed(() => {
  if (graduates.value.length) {
    return graduates.value
  }
  if (!lab.value?.outstandingSeniors) {
    return []
  }
  try {
    return JSON.parse(lab.value.outstandingSeniors).map((item, index) => ({
      id: `legacy-${index}`,
      ...item,
      description: item.achievement || '',
      avatarUrl: '',
      coverImageUrl: ''
    }))
  } catch {
    return []
  }
})

const resolveMedia = (value) => resolveFileUrl(value)

const loadDetail = async () => {
  loading.value = true
  try {
    const [labRes, graduateRes] = await Promise.allSettled([
      getLabById(route.params.id),
      getGraduateList({ pageNum: 1, pageSize: 6, labId: route.params.id })
    ])

    if (labRes.status === 'fulfilled') {
      lab.value = labRes.value.data || null
    }
    graduates.value = graduateRes.status === 'fulfilled' ? graduateRes.value.data?.records || [] : []
  } finally {
    loading.value = false
  }
}

const openApplyDialog = () => {
  if (userStore.userInfo?.labId) {
    ElMessage.warning('你已经加入实验室，不能重复申请')
    return
  }
  if (!hasResume.value) {
    ElMessageBox.confirm('请先完善并提交个人简历，再申请实验室。现在去个人资料页吗？', '需要先提交简历', {
      type: 'warning',
      confirmButtonText: '去完善简历',
      cancelButtonText: '稍后再说'
    }).then(() => {
      router.push('/m/student/profile')
    }).catch(() => {})
    return
  }
  dialogVisible.value = true
}

const submitApply = async () => {
  if (!hasResume.value) {
    ElMessage.warning('请先完善并提交个人简历，再申请实验室')
    await router.push('/m/student/profile')
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
.lab-detail-cover {
  min-height: 300px;
  overflow: hidden;
  border-radius: 28px;
  background-size: cover;
  background-position: center;
  color: #ffffff;
  box-shadow: 0 22px 48px rgba(23, 107, 154, 0.22);
}

.lab-detail-cover__content {
  min-height: 300px;
  padding: 22px;
  display: grid;
  align-content: end;
  gap: 12px;
  background: linear-gradient(180deg, rgba(15, 35, 55, 0.04), rgba(15, 35, 55, 0.58));
}

.lab-detail-cover__logo {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  font-size: 28px;
}

.lab-detail-cover__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mobile-kicker {
  width: fit-content;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.13);
  font-size: 12px;
  font-weight: 900;
}

.lab-detail-cover h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.12;
}

.lab-detail-cover p {
  margin: 0;
  color: rgba(240, 249, 255, 0.9);
  line-height: 1.7;
}

.lab-detail-cover__tags,
.lab-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.lab-detail-cover__tags span:not(.mobile-status-tag),
.lab-detail-tags span {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.lab-detail-cover__tags span:not(.mobile-status-tag) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.13);
}

.lab-detail-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.resume-guide-card,
.lab-detail-section,
.graduate-card {
  border-radius: 24px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
}

.resume-guide-card {
  padding: 16px;
  display: grid;
  gap: 13px;
  border-color: rgba(217, 134, 31, 0.2);
  background: linear-gradient(180deg, #fff8ed, #ffffff);
}

.resume-guide-card.success {
  border-color: rgba(22, 169, 122, 0.2);
  background: linear-gradient(180deg, #effdf8, #ffffff);
}

.resume-guide-card strong {
  color: #172033;
}

.resume-guide-card p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.65;
}

.lab-detail-section {
  padding: 17px;
  display: grid;
  gap: 12px;
}

.lab-detail-section header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.lab-detail-section h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.lab-detail-section header span {
  color: #3388bb;
  font-size: 12px;
  font-weight: 900;
}

.lab-detail-section > p,
.graduate-card p {
  margin: 0;
  color: #475569;
  line-height: 1.78;
  white-space: pre-wrap;
}

.lab-detail-tags span {
  color: #176b9a;
  background: #eaf6fc;
}

.graduate-list {
  display: grid;
  gap: 12px;
}

.graduate-card {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.graduate-card__head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.graduate-card__head strong {
  color: #172033;
}

.graduate-card__head p {
  margin: 4px 0 0;
  color: #64748b;
}

.graduate-card__cover {
  height: 150px;
  border-radius: 18px;
  overflow: hidden;
  background: #f8fafc;
}

.graduate-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lab-detail-sticky-apply {
  position: fixed;
  left: 50%;
  bottom: calc(86px + env(safe-area-inset-bottom));
  z-index: 72;
  width: min(720px, calc(100vw - 24px));
  transform: translateX(-50%);
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.76);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(18px);
}

.lab-detail-sticky-apply span {
  display: block;
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.lab-detail-sticky-apply strong {
  display: block;
  color: #172033;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sheet-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 430px) {
  .lab-detail-stats,
  .lab-detail-sticky-apply {
    grid-template-columns: 1fr;
  }

  .lab-detail-sticky-apply .el-button {
    width: 100%;
  }
}
</style>

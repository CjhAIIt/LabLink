<template>
  <div class="page-shell lab-detail-page">
    <section v-if="lab" class="page-hero detail-hero" :style="heroStyle">
      <div class="hero-mask">
        <div class="hero-content">
          <div class="hero-brand">
            <div class="hero-logo">
              <img v-if="resolvedLogo" :src="resolvedLogo" alt="实验室 Logo" />
              <el-icon v-else><OfficeBuilding /></el-icon>
            </div>
            <div class="hero-text">
              <p class="eyebrow">{{ lab.labCode || 'LAB-CODE' }}</p>
              <h1>{{ lab.labName }}</h1>
              <p class="hero-subtitle">{{ lab.labDesc || lab.basicInfo || '暂无实验室简介' }}</p>
              <div class="hero-meta">
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>指导教师：{{ lab.teacherName || lab.advisors || '待完善' }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><Location /></el-icon>
                  <span>地点：{{ lab.location || '待完善' }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="hero-actions">
            <el-button type="primary" size="large" :disabled="!canApply" @click="handleApply">
              {{ applyButtonText }}
            </el-button>
            <el-button size="large" @click="$router.back()">返回列表</el-button>
          </div>
        </div>
      </div>
    </section>

    <div v-if="lab" class="detail-container">
      <section class="summary-grid">
        <article class="summary-card">
          <span>实验室 Logo</span>
          <div class="summary-logo">
            <img v-if="resolvedLogo" :src="resolvedLogo" alt="实验室 Logo" />
            <el-icon v-else><OfficeBuilding /></el-icon>
          </div>
        </article>
        <article class="summary-card">
          <span>成员规模</span>
          <strong>{{ lab.currentNum ?? 0 }} / {{ lab.recruitNum ?? '-' }}</strong>
          <small>当前成员数 / 计划容量</small>
        </article>
        <article class="summary-card">
          <span>成立时间</span>
          <strong>{{ lab.foundingDate || '待完善' }}</strong>
          <small>实验室发展时间线</small>
        </article>
      </section>

      <el-row :gutter="24">
        <el-col :xs="24" :sm="24" :md="16">
          <TablePageCard class="panel-card info-section" title="实验室介绍" subtitle="基础说明">
            <div class="rich-content">
              <p>{{ lab.basicInfo || lab.labDesc || '暂无详细介绍' }}</p>
            </div>
          </TablePageCard>

          <TablePageCard class="panel-card honor-section" title="成果与荣誉" subtitle="成果沉淀">
            <div class="honor-list">
              <div v-if="lab.awards" class="honor-item">
                <p>{{ lab.awards }}</p>
              </div>
              <el-empty v-else description="暂无荣誉展示" :image-size="60" />
            </div>
          </TablePageCard>

          <TablePageCard
            class="panel-card graduate-section"
            title="优秀学长"
            subtitle="成长样本"
            :count-label="`${graduateCards.length} 位`"
            count-tag-type="warning"
          >
            <div v-if="graduateCards.length" class="graduate-grid">
              <article v-for="item in graduateCards" :key="item.id || item.name" class="graduate-card">
                <div class="graduate-cover">
                  <img v-if="item.coverImageUrl" :src="resolveMedia(item.coverImageUrl)" alt="展示图" />
                  <div v-else class="graduate-cover__fallback">
                    <el-avatar :size="56" :src="resolveMedia(item.avatarUrl)">
                      {{ (item.name || 'G').charAt(0) }}
                    </el-avatar>
                  </div>
                </div>
                <div class="graduate-head">
                  <el-avatar :size="44" :src="resolveMedia(item.avatarUrl)">
                    {{ (item.name || 'G').charAt(0) }}
                  </el-avatar>
                  <div>
                    <strong>{{ item.name || '优秀学长' }}</strong>
                    <p>{{ item.major || '专业待完善' }}<template v-if="item.graduationYear"> · {{ item.graduationYear }}</template></p>
                  </div>
                </div>
                <div class="graduate-body">
                  <div class="graduate-line">
                    <span>去向</span>
                    <strong>{{ item.company || '-' }} {{ item.position || item.achievement || '' }}</strong>
                  </div>
                  <p>{{ item.description || item.achievement || '暂无介绍' }}</p>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无优秀学长信息" :image-size="60" />
          </TablePageCard>
        </el-col>

        <el-col :xs="24" :sm="24" :md="8">
          <TablePageCard class="panel-card skill-section" title="所需技能" subtitle="能力要求">
            <div class="skill-tags">
              <el-tag v-for="skill in skillList" :key="skill" effect="plain" class="skill-tag">
                {{ skill }}
              </el-tag>
              <span v-if="!skillList.length" class="empty-text">暂未明确技能要求</span>
            </div>
          </TablePageCard>

          <TablePageCard class="panel-card brand-section" title="品牌资料" subtitle="展示素材">
            <div class="brand-preview">
              <div class="brand-preview__cover">
                <img v-if="resolvedCover" :src="resolvedCover" alt="实验室封面图" />
                <div v-else class="brand-preview__empty">暂无封面图</div>
              </div>
            </div>
          </TablePageCard>
        </el-col>
      </el-row>
    </div>

    <div v-else-if="!loading" class="empty-panel">
      <el-empty description="未找到实验室详情" />
    </div>
  </div>
</template>

<script setup>
import { Location, OfficeBuilding, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getGraduateList } from '@/api/graduate'
import { getLabById } from '@/api/lab'
import { useUserStore } from '@/stores/user'
import { resolveFileUrl } from '@/utils/file'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const lab = ref(null)
const loading = ref(true)
const graduates = ref([])

const labId = computed(() => route.params.id)
const resolvedLogo = computed(() => resolveMedia(lab.value?.logoUrl))
const resolvedCover = computed(() => resolveMedia(lab.value?.coverImageUrl))
const heroStyle = computed(() => ({
  backgroundImage: resolvedCover.value
    ? `linear-gradient(135deg, rgba(15, 23, 42, 0.84), rgba(15, 118, 110, 0.44)), url(${resolvedCover.value})`
    : 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)'
}))

const canApply = computed(() => Boolean(lab.value) && !userStore.userInfo?.labId && Number(lab.value?.status) === 1)

const applyButtonText = computed(() => {
  if (userStore.userInfo?.labId) {
    return '已加入实验室'
  }
  if (lab.value && Number(lab.value.status) !== 1) {
    return '当前未开放招新'
  }
  return '立即申请加入'
})

const skillList = computed(() => {
  if (!lab.value?.requireSkill) return []
  return String(lab.value.requireSkill).split(/[，,、\s/]+/).filter(Boolean)
})

const parsedSeniors = computed(() => {
  if (!lab.value?.outstandingSeniors) return []
  try {
    return JSON.parse(lab.value.outstandingSeniors)
  } catch (error) {
    return []
  }
})

const graduateCards = computed(() => {
  if (graduates.value.length) {
    return graduates.value
  }
  return parsedSeniors.value.map((item, index) => ({
    id: `legacy-${index}`,
    name: item.name,
    major: item.major,
    achievement: item.achievement,
    description: item.achievement,
    avatarUrl: '',
    coverImageUrl: ''
  }))
})

const resolveMedia = (value) => resolveFileUrl(value)

const fetchLabDetail = async () => {
  loading.value = true
  try {
    const [labRes, graduateRes] = await Promise.allSettled([
      getLabById(labId.value),
      getGraduateList({ pageNum: 1, pageSize: 8, labId: labId.value })
    ])

    if (labRes.status === 'fulfilled') {
      lab.value = labRes.value.data || null
    }

    if (graduateRes.status === 'fulfilled') {
      graduates.value = graduateRes.value.data?.records || []
    } else {
      graduates.value = []
    }
  } catch (error) {
    ElMessage.error('获取实验室详情失败')
  } finally {
    loading.value = false
  }
}

const handleApply = () => {
  if (userStore.userInfo?.labId) {
    ElMessage.warning('你已经加入实验室，不能重复申请')
    return
  }
  if (!lab.value || Number(lab.value.status) !== 1) {
    ElMessage.warning('当前实验室未开放招新')
    return
  }
  router.push(`/student/labs?applyLabId=${labId.value}`)
}

onMounted(() => {
  fetchLabDetail()
})
</script>

<style scoped>
.lab-detail-page {
  background-color: #f8fafc;
}

.detail-hero {
  background-size: cover;
  background-position: center;
  color: white;
  margin: -24px -24px 24px -24px;
}

.hero-mask {
  padding: 48px 60px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.22), rgba(15, 23, 42, 0.46));
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-logo {
  width: 92px;
  height: 92px;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36px;
  flex-shrink: 0;
}

.hero-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-text h1 {
  font-size: 34px;
  margin: 12px 0;
  color: white;
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(226, 232, 240, 0.9);
  max-width: 640px;
  margin-bottom: 20px;
  line-height: 1.8;
}

.hero-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #cbd5e1;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 8px;
}

.summary-card span,
.summary-card small,
.graduate-head p,
.graduate-body p,
.empty-text {
  color: #64748b;
}

.summary-card strong,
.graduate-head strong {
  color: #0f172a;
}

.summary-logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a, #0f766e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.summary-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.panel-card {
  margin-bottom: 24px;
}

.rich-content {
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

.honor-item {
  padding: 12px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 4px;
  color: #92400e;
}

.graduate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.graduate-card {
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.96);
  overflow: hidden;
}

.graduate-cover {
  height: 150px;
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);
}

.graduate-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.graduate-cover__fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graduate-head,
.graduate-body {
  padding: 16px;
}

.graduate-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.graduate-body {
  display: grid;
  gap: 10px;
}

.graduate-line {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.skill-tag {
  margin: 0;
}

.brand-preview__cover {
  min-height: 220px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.brand-preview__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-preview__empty {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.empty-panel {
  padding: 48px 0;
}

@media (max-width: 768px) {
  .detail-hero {
    margin: -16px -16px 16px -16px;
  }

  .hero-mask {
    padding: 32px 20px;
  }

  .hero-content,
  .hero-brand {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions,
  .summary-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .hero-meta {
    flex-direction: column;
    gap: 10px;
  }
}
</style>

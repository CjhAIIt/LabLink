<template>
  <div v-loading="loading" class="growth-page">
    <GradPathNav
      title="学习路线"
      description="每条成长路径都拆成阶段式路线，并补充课程、书籍、竞赛和证书资源。你不需要再自己拼凑资料，可以直接按阶段推进。"
    >
      <template #actions>
        <el-select v-model="selectedTrackCode" style="width: 240px" @change="handleTrackChange">
          <el-option
            v-for="item in tracks"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
      </template>
    </GradPathNav>

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
      <el-button type="primary" plain @click="loadData(true)">重新加载</el-button>
    </div>

    <section v-if="track" class="learn-grid">
      <section class="overview-card">
        <p class="eyebrow">ROADMAP</p>
        <h2>{{ track.name }}</h2>
        <p class="overview-text">{{ track.fitScene }}</p>

        <div class="summary-metrics">
          <MetricCard label="阶段任务" :value="stages.length" tip="按阶段推进" compact />
          <MetricCard label="推荐练题关键词" :value="track.recommendedKeyword || '-'" tip="配合练题入口使用" compact />
          <MetricCard label="推荐面试岗位" :value="track.interviewPosition || '-'" tip="可直接跳转 AI 面试" compact />
        </div>
      </section>

      <TablePageCard class="aside-card" title="资源导航" subtitle="配套课程与资料" count-label="数据库驱动" count-tag-type="success">

        <div class="resource-stack">
          <div class="resource-group">
            <strong>推荐课程</strong>
            <span v-for="item in track.courses || []" :key="item">{{ item }}</span>
          </div>
          <div class="resource-group">
            <strong>推荐书籍</strong>
            <span v-for="item in track.books || []" :key="item">{{ item }}</span>
          </div>
          <div class="resource-group">
            <strong>适合参与的竞赛</strong>
            <span v-for="item in track.competitions || []" :key="item">{{ item }}</span>
          </div>
          <div class="resource-group">
            <strong>可关注的证书</strong>
            <span v-for="item in track.certificates || []" :key="item">{{ item }}</span>
          </div>
        </div>
      </TablePageCard>
    </section>

    <div class="timeline">
      <TablePageCard
        v-for="item in stages"
        :key="item.id"
        class="stage-card"
        :title="item.title"
        :subtitle="item.phaseCode"
        :count-label="`Stage ${item.stageNo}`"
      >
        <div class="meta-line">{{ item.duration }}</div>
        <p class="stage-desc">{{ item.goal }}</p>

        <div class="focus-row">
          <el-tag
            v-for="skill in item.focusSkills || []"
            :key="`${item.id}-${skill}`"
            effect="plain"
            type="info"
          >
            {{ skill }}
          </el-tag>
        </div>

        <div class="resource-box">
          <span>推荐资源</span>
          <a :href="item.resourceUrl" target="_blank" rel="noreferrer">{{ item.resourceName }}</a>
          <small>{{ item.actionHint }}</small>
        </div>

        <div class="stage-actions">
          <el-button @click="openPractice(item.practiceKeyword)">按这个阶段练题</el-button>
          <el-button type="primary" plain @click="openInterview">转到 AI 面试</el-button>
        </div>
      </TablePageCard>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GradPathNav from '@/components/GradPathNav.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { loadGrowthDashboard } from '@/utils/growthCenterCache'
import { getGrowthTrackDetail } from '@/api/growthCenter'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tracks = ref([])
const track = ref(null)
const stages = ref([])
const selectedTrackCode = ref(route.query.track || '')
const loadError = ref('')

const applyDashboardData = (dashboardData = {}) => {
  tracks.value = dashboardData.tracks || []
  if (!selectedTrackCode.value) {
    selectedTrackCode.value =
      dashboardData.latestResult?.topTracks?.[0]?.code || dashboardData.tracks?.[0]?.code || ''
  }
}

const loadTrackDetail = async (trackCode = selectedTrackCode.value) => {
  if (!trackCode) {
    track.value = null
    stages.value = []
    return
  }

  const response = await getGrowthTrackDetail(trackCode)
  track.value = response.data.track
  stages.value = response.data.stages || []
}

const syncTrackRoute = async (trackCode) => {
  if (!trackCode || trackCode === route.query.track) {
    return
  }
  await router.replace(`/student/guide/learn?track=${trackCode}`)
}

const loadData = async (force = false) => {
  loading.value = true
  loadError.value = ''
  try {
    const dashboardData = await loadGrowthDashboard({ force })
    applyDashboardData(dashboardData)
    await syncTrackRoute(selectedTrackCode.value)
    await loadTrackDetail(selectedTrackCode.value)
  } catch (error) {
    loadError.value = '成长中心学习路线加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const handleTrackChange = async (trackCode) => {
  if (!trackCode) {
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    await syncTrackRoute(trackCode)
    await loadTrackDetail(trackCode)
  } catch (error) {
    loadError.value = '成长中心学习路线加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const openPractice = (keyword) => {
  const query = new URLSearchParams({
    track: track.value.code,
    keyword: keyword || track.value.recommendedKeyword || ''
  })
  router.push(`/student/guide/practice?${query.toString()}`)
}

const openInterview = () => {
  router.push(`/student/guide/interview?track=${track.value.code}`)
}

watch(
  () => route.query.track,
  async (value) => {
    if (value && value !== selectedTrackCode.value) {
      selectedTrackCode.value = value
      loading.value = true
      loadError.value = ''
      try {
        await loadTrackDetail(value)
      } catch (error) {
        loadError.value = '成长中心学习路线加载失败，请稍后重试'
      } finally {
        loading.value = false
      }
    }
  }
)

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.growth-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1360px;
  margin: 0 auto;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.learn-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.overview-card,
.aside-card,
.stage-card {
  border: 1px solid rgba(14, 165, 233, 0.14);
  border-radius: 24px;
}

.overview-card {
  padding: 20px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 25%),
    linear-gradient(135deg, #082f49, #155e75);
  color: #f8fafc;
}

.eyebrow,
.phase {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
}

.eyebrow {
  color: rgba(186, 230, 253, 0.92);
}

.overview-card h2,
.stage-card h3 {
  margin: 0;
  color: inherit;
}

.overview-text {
  margin: 14px 0 0;
  line-height: 1.8;
  color: rgba(226, 232, 240, 0.92);
}

.summary-metrics {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.summary-metrics :deep(.metric-card) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.12);
}

.summary-metrics :deep(.metric-card__label),
.summary-metrics :deep(.metric-card__tip),
.summary-metrics :deep(.metric-card__value) {
  color: #e2e8f0;
}

.resource-stack,
.timeline {
  display: grid;
  gap: 16px;
}

.resource-group,
.stage-card :deep(.table-page-card__body) {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.resource-group {
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.resource-group strong,
.stage-card h3 {
  color: #0f172a;
}

.resource-group span {
  display: block;
  margin-top: 10px;
  line-height: 1.8;
  color: #475569;
}

.stage-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.phase {
  color: #0284c7;
}

.stage-card h3 {
  color: #0f172a;
}

.meta-line {
  margin-top: 14px;
  font-weight: 700;
  color: #0f766e;
}

.stage-desc {
  margin: 10px 0 0;
  line-height: 1.8;
  color: #475569;
}

.focus-row {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.resource-box {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eff6ff;
  border: 1px solid rgba(14, 165, 233, 0.16);
}

.resource-box span,
.resource-box a,
.resource-box small {
  display: block;
}

.resource-box span {
  font-size: 13px;
  color: #0369a1;
}

.resource-box a {
  margin-top: 8px;
  color: #0f172a;
  text-decoration: none;
  font-weight: 700;
}

.resource-box small {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.7;
}

.stage-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .load-error {
    flex-direction: column;
    align-items: stretch;
  }

  .learn-grid,
  .summary-metrics {
    grid-template-columns: 1fr;
  }
}
</style>

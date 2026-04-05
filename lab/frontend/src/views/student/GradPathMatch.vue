<template>
  <div v-loading="loading" class="growth-page">
    <GradPathNav
      title="能力匹配"
      description="测评完成后，这里会根据你的结果给出动态推荐度，再结合路径能力模型拆出当前最值得优先补的短板。"
    >
      <template #actions>
        <el-select v-model="selectedTrackCode" style="width: 240px" @change="handleTrackChange">
          <el-option
            v-for="trackOption in tracks"
            :key="trackOption.code"
            :label="trackOption.name"
            :value="trackOption.code"
          />
        </el-select>
      </template>
    </GradPathNav>

    <el-alert
      v-if="!dashboard.hasResult"
      title="你还没有完成成长测评。当前可以先浏览路径详情，但推荐度会在完成 20 题后才动态生成。"
      type="warning"
      :closable="false"
      show-icon
    />

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
      <el-button type="primary" plain @click="loadData(true)">重新加载</el-button>
    </div>

    <section v-if="track" class="match-grid">
      <el-card class="score-card" shadow="never">
        <p class="eyebrow">DYNAMIC MATCH</p>
        <div class="score-shell">
          <div class="score-circle">
            <strong>{{ track.matchScore || 0 }}</strong>
            <span>当前推荐度</span>
          </div>
          <div class="score-copy">
            <h3>{{ track.name }}</h3>
            <p>{{ track.subtitle }}</p>
            <div class="focus-row">
              <el-tag
                v-for="skill in track.tags || []"
                :key="`${track.code}-${skill}`"
                type="info"
                effect="plain"
              >
                {{ skill }}
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="summary-card" shadow="never">
        <template #header>
          <div class="section-head">
            <span>行动建议</span>
            <el-tag type="warning" effect="plain">按先后顺序推进</el-tag>
          </div>
        </template>

        <div class="advice-list">
          <div class="advice-item">
            <strong>先补最短板</strong>
            <p>{{ weakestSkill }}</p>
          </div>
          <div class="advice-item">
            <strong>再做针对性练题</strong>
            <p>先用“{{ track.recommendedKeyword || '基础题型' }}”做一轮定向练习，把核心手感补起来。</p>
          </div>
          <div class="advice-item">
            <strong>最后转到表达训练</strong>
            <p>去 AI 面试页把你的思路讲出来，验证是否真的理解，而不是只会做题。</p>
          </div>
        </div>

        <div class="summary-actions">
          <el-button @click="router.push(`/student/guide/learn?track=${track.code}`)">去学习路线</el-button>
          <el-button type="primary" @click="openPractice">去练题</el-button>
        </div>
      </el-card>
    </section>

    <el-card v-if="track" class="table-card" shadow="never">
      <template #header>
        <div class="section-head">
          <span>{{ track.name }} 能力模型</span>
          <el-tag effect="plain">根据测评结果估算</el-tag>
        </div>
      </template>

      <div class="gap-list">
        <div v-for="item in competencies" :key="item.skill" class="gap-item">
          <div class="gap-head">
            <strong>{{ item.skill }}</strong>
            <span>目标 {{ item.target }}</span>
          </div>
          <el-progress :percentage="item.percentage" :stroke-width="10" />
          <p>{{ item.description }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GradPathNav from '@/components/GradPathNav.vue'
import { loadGrowthDashboard } from '@/utils/growthCenterCache'
import { getGrowthTrackDetail } from '@/api/growthCenter'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tracks = ref([])
const track = ref(null)
const dashboard = ref({
  hasResult: false,
  latestResult: null
})
const selectedTrackCode = ref(route.query.track || '')
const loadError = ref('')

const competencies = computed(() => {
  const list = track.value?.competencies || []
  const baseScore = Number(track.value?.matchScore || 0)
  return list.map((item, index) => {
    const percentage = Math.max(18, Math.min(96, baseScore + 8 - index * 10))
    return {
      ...item,
      percentage
    }
  })
})

const weakestSkill = computed(() => {
  if (!competencies.value.length) {
    return '当前暂无能力模型数据'
  }
  const weakest = [...competencies.value].sort((left, right) => left.percentage - right.percentage)[0]
  return `${weakest.skill}：${weakest.description}`
})

const applyDashboardData = (dashboardData = {}) => {
  dashboard.value = {
    hasResult: Boolean(dashboardData.hasResult),
    latestResult: dashboardData.latestResult || null
  }
  tracks.value = dashboardData.tracks || []
  if (!selectedTrackCode.value) {
    selectedTrackCode.value =
      dashboardData.latestResult?.topTracks?.[0]?.code || dashboardData.tracks?.[0]?.code || ''
  }
}

const loadTrackDetail = async (trackCode = selectedTrackCode.value) => {
  if (!trackCode) {
    track.value = null
    return
  }
  const response = await getGrowthTrackDetail(trackCode)
  track.value = response.data.track
}

const syncTrackRoute = async (trackCode) => {
  if (!trackCode || trackCode === route.query.track) {
    return
  }
  await router.replace(`/student/guide/match?track=${trackCode}`)
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
    loadError.value = '成长中心能力匹配加载失败，请稍后重试'
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
    loadError.value = '成长中心能力匹配加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const openPractice = () => {
  const query = new URLSearchParams({
    track: track.value.code,
    keyword: track.value.recommendedKeyword || ''
  })
  router.push(`/student/guide/practice?${query.toString()}`)
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
        loadError.value = '成长中心能力匹配加载失败，请稍后重试'
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

.match-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.score-card,
.summary-card,
.table-card {
  border: 1px solid rgba(14, 165, 233, 0.14);
}

.score-card {
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 25%),
    linear-gradient(135deg, #082f49, #0f766e);
  color: #f8fafc;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: rgba(186, 230, 253, 0.92);
}

.score-shell {
  display: flex;
  gap: 24px;
  align-items: center;
}

.score-circle {
  width: 180px;
  height: 180px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.score-circle strong {
  display: block;
  font-size: 56px;
  line-height: 1;
}

.score-circle span {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.9);
}

.score-copy h3 {
  margin: 0;
  font-size: 28px;
}

.score-copy p {
  margin: 14px 0 0;
  line-height: 1.8;
  color: rgba(226, 232, 240, 0.92);
}

.focus-row {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.advice-list,
.gap-list {
  display: grid;
  gap: 14px;
}

.advice-item,
.gap-item {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.advice-item strong,
.gap-item strong {
  color: #0f172a;
}

.advice-item p,
.gap-item p {
  margin: 8px 0 0;
  line-height: 1.8;
  color: #475569;
}

.summary-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.gap-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #0f172a;
}

@media (max-width: 960px) {
  .load-error {
    flex-direction: column;
    align-items: stretch;
  }

  .match-grid {
    grid-template-columns: 1fr;
  }

  .score-shell {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

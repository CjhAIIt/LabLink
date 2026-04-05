<template>
  <div v-loading="loading" class="growth-page">
    <GradPathNav
      title="路径广场"
      description="这里展示成长中心当前可选的全部成长路径。数量已经从原先的少量模板扩展到多方向路径，测评完成后会带上你的动态推荐度。"
    >
      <template #actions>
        <el-button type="primary" @click="router.push('/student/guide')">返回成长测评</el-button>
      </template>
    </GradPathNav>

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
      <el-button type="primary" plain @click="loadData(true)">重新加载</el-button>
    </div>

    <section class="jobs-grid">
      <el-card
        v-for="track in tracks"
        :key="track.code"
        class="job-card"
        shadow="never"
      >
        <div class="card-top">
          <div>
            <p class="eyebrow">{{ track.shortName }}</p>
            <h3>{{ track.name }}</h3>
          </div>
          <el-tag effect="plain" :type="track.matchScore >= 80 ? 'success' : 'info'">
            {{ track.difficultyLabel || '成长路径' }}
          </el-tag>
        </div>

        <div class="salary">{{ track.salaryRange || '持续成长方向' }}</div>
        <p class="subtitle">{{ track.subtitle }}</p>
        <p class="summary">{{ track.description }}</p>
        <p class="scene">{{ track.fitScene }}</p>

        <div class="focus-row">
          <el-tag
            v-for="skill in track.tags || []"
            :key="`${track.code}-${skill}`"
            effect="plain"
            type="info"
          >
            {{ skill }}
          </el-tag>
        </div>

        <div class="score-box">
          <span>{{ track.matchScore == null ? '尚未完成测评' : '当前动态推荐度' }}</span>
          <el-progress :percentage="track.matchScore || 0" :stroke-width="10" />
        </div>

        <div class="resource-grid">
          <div class="resource-item">
            <strong>课程</strong>
            <span>{{ track.courses?.[0] || '待补充' }}</span>
          </div>
          <div class="resource-item">
            <strong>竞赛</strong>
            <span>{{ track.competitions?.[0] || '待补充' }}</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button @click="openMatch(track.code)">看匹配</el-button>
          <el-button @click="openLearn(track.code)">看路线</el-button>
          <el-button type="primary" @click="openPractice(track)">去练题</el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import GradPathNav from '@/components/GradPathNav.vue'
import { loadGrowthDashboard } from '@/utils/growthCenterCache'

const router = useRouter()
const loading = ref(false)
const tracks = ref([])
const loadError = ref('')

const loadData = async (force = false) => {
  loading.value = true
  loadError.value = ''
  try {
    const dashboardData = await loadGrowthDashboard({ force })
    tracks.value = dashboardData.tracks || []
  } catch (error) {
    tracks.value = []
    loadError.value = '成长中心路径数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const openMatch = (trackCode) => {
  router.push(`/student/guide/match?track=${trackCode}`)
}

const openLearn = (trackCode) => {
  router.push(`/student/guide/learn?track=${trackCode}`)
}

const openPractice = (track) => {
  const query = new URLSearchParams({
    track: track.code,
    keyword: track.recommendedKeyword || ''
  })
  router.push(`/student/guide/practice?${query.toString()}`)
}

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

.jobs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.job-card {
  border: 1px solid rgba(14, 165, 233, 0.14);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.08), transparent 24%),
    linear-gradient(180deg, #ffffff, #f8fbff);
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: #0284c7;
}

.job-card h3 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}

.salary {
  margin-top: 18px;
  font-size: 22px;
  font-weight: 800;
  color: #0f766e;
}

.subtitle,
.summary,
.scene {
  line-height: 1.8;
  color: #475569;
}

.subtitle {
  margin: 14px 0 0;
  font-weight: 700;
  color: #0f172a;
}

.summary,
.scene {
  margin: 10px 0 0;
}

.focus-row {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.score-box {
  margin-top: 18px;
  display: grid;
  gap: 10px;
  color: #334155;
  font-weight: 600;
}

.resource-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.resource-item {
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.resource-item strong,
.resource-item span {
  display: block;
}

.resource-item strong {
  color: #0f172a;
}

.resource-item span {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.card-actions {
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

  .jobs-grid {
    grid-template-columns: 1fr;
  }

  .resource-grid {
    grid-template-columns: 1fr;
  }

  .job-card h3 {
    font-size: 22px;
  }
}
</style>

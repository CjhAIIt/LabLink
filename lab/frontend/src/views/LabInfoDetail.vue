<template>
  <div class="lab-info-detail-page">
    <div class="bg-grid"></div>
    <div class="ambient-glow"></div>

    <div v-if="lab" class="lab-content">
      <header class="brand-hero" :style="heroStyle">
        <div class="brand-hero__overlay">
          <div class="hero-main">
            <div class="hero-brand">
              <div class="hero-logo">
                <img v-if="resolvedLogo" :src="resolvedLogo" alt="实验室 Logo" />
                <el-icon v-else><OfficeBuilding /></el-icon>
              </div>
              <div class="hero-copy">
                <span class="hero-tag">Lab Showcase</span>
                <h1>{{ lab.labName }}</h1>
                <p>{{ lab.labDesc || '暂无实验室简介' }}</p>
                <div class="hero-meta">
                  <span><el-icon><User /></el-icon>{{ lab.teacherName || lab.advisors || '指导教师待完善' }}</span>
                  <span><el-icon><Location /></el-icon>{{ lab.location || '地点待完善' }}</span>
                </div>
              </div>
            </div>

            <router-link class="ghost-btn" to="/intro">
              <el-icon><Back /></el-icon>
              返回列表
            </router-link>
          </div>
        </div>
      </header>

      <main class="main-content">
        <section class="overview-grid">
          <article class="overview-card">
            <span>实验室编码</span>
            <strong>{{ lab.labCode || `LAB-${lab.id}` }}</strong>
            <small>统一识别当前实验室品牌档案</small>
          </article>
          <article class="overview-card">
            <span>成员规模</span>
            <strong>{{ lab.currentNum ?? 0 }} / {{ lab.recruitNum ?? '-' }}</strong>
            <small>当前成员数与计划容量</small>
          </article>
          <article class="overview-card">
            <span>成立时间</span>
            <strong>{{ lab.foundingDate || '待完善' }}</strong>
            <small>实验室发展时间线</small>
          </article>
        </section>

        <div class="content-grid">
          <div class="info-grid">
            <div class="info-card">
              <div class="card-header">
                <el-icon><InfoFilled /></el-icon>
                <h3>基础信息</h3>
              </div>
              <div class="info-list">
                <div class="info-item">
                  <span class="label">指导老师</span>
                  <span class="value">{{ lab.teacherName || lab.advisors || '暂无' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">当前管理员</span>
                  <span class="value">{{ lab.currentAdmins || '暂无' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">联系邮箱</span>
                  <span class="value">{{ lab.contactEmail || '暂无' }}</span>
                </div>
              </div>
            </div>

            <div class="info-card">
              <div class="card-header">
                <el-icon><Trophy /></el-icon>
                <h3>成果与荣誉</h3>
              </div>
              <div class="text-content">{{ lab.awards || '暂无成果与荣誉信息' }}</div>
            </div>

            <div class="info-card full-width">
              <div class="card-header">
                <el-icon><DataBoard /></el-icon>
                <h3>实验室介绍</h3>
              </div>
              <div class="text-content">{{ lab.basicInfo || lab.labDesc || '暂无详细介绍' }}</div>
            </div>
          </div>

          <aside class="side-panel">
            <div class="side-card">
              <div class="card-header">
                <el-icon><PictureFilled /></el-icon>
                <h3>品牌信息</h3>
              </div>
              <div class="brand-stack">
                <div class="brand-stack__item">
                  <span>Logo</span>
                  <div class="brand-thumb logo">
                    <img v-if="resolvedLogo" :src="resolvedLogo" alt="实验室 Logo" />
                    <el-icon v-else><OfficeBuilding /></el-icon>
                  </div>
                </div>
                <div class="brand-stack__item">
                  <span>封面图</span>
                  <div class="brand-thumb cover">
                    <img v-if="resolvedCover" :src="resolvedCover" alt="实验室封面图" />
                    <div v-else class="brand-thumb__empty">暂无封面图</div>
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <TablePageCard
          class="panel-card"
          title="优秀学长"
          subtitle="成长样本与去向展示"
          :count-label="`${graduates.length} 位`"
          count-tag-type="warning"
        >
          <div v-if="graduates.length" class="graduate-grid">
            <div v-for="item in graduates" :key="item.id" class="graduate-card">
              <div class="graduate-cover">
                <img v-if="item.coverImageUrl" :src="resolveMedia(item.coverImageUrl)" alt="展示图" />
                <div v-else class="graduate-cover__fallback">
                  <el-avatar :size="56" :src="resolveMedia(item.avatarUrl)">
                    {{ (item.name || 'G').charAt(0) }}
                  </el-avatar>
                </div>
              </div>
              <div class="graduate-head">
                <el-avatar :size="46" :src="resolveMedia(item.avatarUrl)">
                  {{ (item.name || 'G').charAt(0) }}
                </el-avatar>
                <div>
                  <div class="graduate-name">{{ item.name }}</div>
                  <div class="graduate-meta">
                    <span>{{ item.major || '-' }}</span>
                    <span v-if="item.graduationYear">· {{ item.graduationYear }}</span>
                  </div>
                </div>
              </div>
              <div class="graduate-body">
                <div class="graduate-line"><span class="label">去向</span><span>{{ item.company || '-' }} {{ item.position || '' }}</span></div>
                <div class="graduate-desc">{{ item.description || '暂无介绍' }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无优秀学长数据" />
        </TablePageCard>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Back, DataBoard, InfoFilled, Location, OfficeBuilding, PictureFilled, Trophy, User } from '@element-plus/icons-vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getLabById } from '@/api/lab'
import { getGraduateList } from '@/api/graduate'
import { resolveFileUrl } from '@/utils/file'

const route = useRoute()
const lab = ref(null)
const graduates = ref([])

const resolvedLogo = computed(() => resolveMedia(lab.value?.logoUrl))
const resolvedCover = computed(() => resolveMedia(lab.value?.coverImageUrl))
const heroStyle = computed(() => ({
  backgroundImage: resolvedCover.value
    ? `linear-gradient(135deg, rgba(15, 23, 42, 0.84), rgba(14, 116, 144, 0.56)), url(${resolvedCover.value})`
    : 'linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(14, 116, 144, 0.86), rgba(20, 184, 166, 0.76))'
}))

const resolveMedia = (value) => resolveFileUrl(value)

const loadLabDetail = async () => {
  const labId = route.params.id
  if (!labId) {
    return
  }

  const [labRes, graduateRes] = await Promise.allSettled([
    getLabById(labId),
    getGraduateList({ pageNum: 1, pageSize: 12, labId })
  ])

  if (labRes.status === 'fulfilled' && labRes.value.code === 200) {
    lab.value = labRes.value.data
  }

  if (graduateRes.status === 'fulfilled' && graduateRes.value.code === 200) {
    graduates.value = graduateRes.value.data.records || []
  } else {
    graduates.value = []
  }
}

const handleWindowFocus = () => {
  loadLabDetail()
}

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    loadLabDetail()
  }
}

onMounted(async () => {
  await loadLabDetail()
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.lab-info-detail-page {
  --bg: #f8fafc;
  --panel: rgba(255, 255, 255, 0.96);
  --line: rgba(226, 232, 240, 0.92);
  --text: #0f172a;
  --muted: #64748b;
  --shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
  min-height: 100vh;
  background: var(--bg);
  color: var(--text);
}

.bg-grid {
  position: fixed;
  inset: 0;
  pointer-events: none;
  opacity: 0.6;
  z-index: 0;
  background-image:
    linear-gradient(#e2e8f0 1px, transparent 1px),
    linear-gradient(90deg, #e2e8f0 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0));
}

.ambient-glow {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(14, 165, 233, 0.12) 0%, transparent 45%),
    radial-gradient(circle at 80% 60%, rgba(20, 184, 166, 0.12) 0%, transparent 45%);
}

.lab-content {
  position: relative;
  z-index: 1;
}

.brand-hero {
  margin: 0 auto;
  min-height: 360px;
  background-size: cover;
  background-position: center;
}

.brand-hero__overlay {
  min-height: 360px;
  padding: 72px 24px 54px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.18), rgba(15, 23, 42, 0.46));
}

.hero-main {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
}

.hero-brand {
  display: flex;
  gap: 22px;
  align-items: center;
}

.hero-logo {
  width: 96px;
  height: 96px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #fff;
  font-size: 38px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.18);
  flex-shrink: 0;
  backdrop-filter: blur(12px);
}

.hero-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-copy {
  color: #f8fafc;
}

.hero-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.04;
}

.hero-copy p {
  margin: 0;
  max-width: 760px;
  color: rgba(248, 250, 252, 0.88);
  line-height: 1.75;
}

.hero-meta {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  font-weight: 700;
}

.main-content {
  max-width: 1240px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card,
.info-card,
.side-card {
  border-radius: 24px;
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: var(--shadow);
}

.overview-card {
  padding: 20px;
  display: grid;
  gap: 8px;
}

.overview-card span,
.overview-card small,
.label {
  color: var(--muted);
}

.overview-card strong {
  color: var(--text);
  font-size: 26px;
  font-weight: 800;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 24px;
}

.info-grid {
  display: grid;
  gap: 24px;
}

.info-card {
  padding: 24px;
}

.info-card.full-width {
  min-height: 240px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  color: #0f766e;
}

.card-header h3 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
}

.info-list {
  display: grid;
  gap: 14px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.76);
}

.info-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.value,
.text-content {
  color: #334155;
}

.text-content {
  line-height: 1.8;
  white-space: pre-wrap;
}

.side-card {
  padding: 24px;
  position: sticky;
  top: 24px;
}

.brand-stack {
  display: grid;
  gap: 18px;
}

.brand-stack__item {
  display: grid;
  gap: 10px;
}

.brand-stack__item span {
  font-weight: 700;
  color: var(--muted);
}

.brand-thumb {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.98));
}

.brand-thumb.logo {
  width: 108px;
  height: 108px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0f766e;
  font-size: 40px;
}

.brand-thumb.cover {
  min-height: 160px;
}

.brand-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-thumb__empty {
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.panel-card {
  margin-top: 28px;
}

.graduate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.graduate-card {
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(255, 255, 255, 0.96);
  overflow: hidden;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.graduate-cover {
  height: 160px;
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
  padding: 18px;
}

.graduate-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.graduate-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
}

.graduate-meta {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 600;
}

.graduate-body {
  display: grid;
  gap: 10px;
  color: #334155;
}

.graduate-line {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.graduate-desc {
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 14px;
}

@media (max-width: 960px) {
  .hero-main,
  .hero-brand,
  .content-grid {
    flex-direction: column;
    grid-template-columns: 1fr;
  }

  .side-card {
    position: static;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>

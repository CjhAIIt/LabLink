<template>
  <div v-loading="loading" class="page-shell statistics-dashboard">
    <section class="page-hero statistics-hero">
      <div class="statistics-hero__content">
        <p class="eyebrow">Statistics Console</p>
        <h1>统计分析</h1>
        <p class="hero-subtitle">
          查看实验室、成员、角色、专业等数据概况，快速识别组织结构、成员差异与资料缺口。
        </p>

        <div class="statistics-hero__pills">
          <span class="statistics-hero__pill">
            <el-icon><OfficeBuilding /></el-icon>
            {{ scopeLabel }}
          </span>
          <span class="statistics-hero__pill">
            <el-icon><Calendar /></el-icon>
            {{ rangeLabel }}
          </span>
          <span class="statistics-hero__pill statistics-hero__pill--ghost">
            <el-icon><Refresh /></el-icon>
            数据更新于 {{ lastUpdatedLabel }}
          </span>
          <span v-if="filterSummaryLabel" class="statistics-hero__pill statistics-hero__pill--muted">
            {{ filterSummaryLabel }}
          </span>
        </div>
      </div>

      <div class="statistics-hero__actions">
        <el-button class="statistics-hero__button" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button
          type="primary"
          class="statistics-hero__button statistics-hero__button--primary"
          :loading="exportLoading"
          :disabled="loading"
          @click="handleExport"
        >
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>

        <div class="statistics-hero__status">
          <span>筛选精度</span>
          <strong>{{ precisionLabel }}</strong>
          <small>{{ precisionHint }}</small>
        </div>
      </div>
    </section>

    <section class="toolbar-card statistics-filter-panel">
      <SectionHeading
        :icon="DataAnalysis"
        title="分析视角筛选"
        description="支持按实验室、角色、专业切换看板视角，时间范围和实验室会触发重新拉取统计数据。"
        badge="可扩展筛选层"
      >
        <template #extra>
          <el-tag v-if="filterNotice" type="warning" effect="plain">{{ filterNotice }}</el-tag>
        </template>
      </SectionHeading>

      <div class="statistics-filter-panel__body">
        <el-form :inline="true" class="toolbar-form statistics-filter-panel__form">
          <el-form-item label="统计时间">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>

          <el-form-item label="实验室">
            <el-select v-model="filters.labId" clearable filterable placeholder="全部实验室" style="width: 220px">
              <el-option
                v-for="item in labOptions"
                :key="item.id"
                :label="item.labName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="角色">
            <el-select v-model="filters.memberRole" clearable placeholder="全部角色" style="width: 180px">
              <el-option
                v-for="item in roleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="专业">
            <el-select v-model="filters.major" clearable filterable placeholder="全部专业" style="width: 220px">
              <el-option
                v-for="item in majorOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="applyFilters">应用筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="statistics-filter-panel__meta">
          <span class="statistics-filter-panel__meta-chip">
            当前范围：{{ scopeLabel }}
          </span>
          <span class="statistics-filter-panel__meta-chip">
            明细样本：{{ sampleCoverageLabel }}
          </span>
          <span class="statistics-filter-panel__meta-chip statistics-filter-panel__meta-chip--success">
            数据更新时间：{{ lastUpdatedLabel }}
          </span>
        </div>
      </div>
    </section>

    <section class="statistics-section">
      <SectionHeading
        :icon="DataBoard"
        title="核心指标总览"
        description="把最重要的实验室与成员指标前置展示，让进入页面后的第一眼就能抓住重点。"
        badge="第一屏"
      />

      <div class="statistics-metrics-grid">
        <StatCard
          v-for="(card, index) in metricCards"
          :key="card.label"
          :icon="card.icon"
          :label="card.label"
          :value="card.value"
          :description="card.description"
          :trend-label="card.trendLabel"
          :trend-value="card.trendValue"
          :trend-type="card.trendType"
          :accent="card.accent"
          :accent-soft="card.accentSoft"
          :index="index"
        />
      </div>
    </section>

    <section class="statistics-section">
      <SectionHeading
        :icon="TrendCharts"
        title="核心图表分析"
        description="用图表代替列表和进度条，快速看清成员集中在哪些实验室，以及角色结构如何分布。"
        badge="第二屏"
      />

      <div class="statistics-grid statistics-grid--primary">
        <ChartCard
          :icon="Histogram"
          title="实验室成员人数分布"
          description="定位成员规模最高的实验室，帮助管理者快速识别重点实验室与成员重心。"
          :badge="sampleDrivenBadge"
          :accent="DASHBOARD_COLORS.blue"
          class="statistics-grid__item statistics-grid__item--wide"
        >
          <template #extra>
            <el-tag effect="plain">{{ labDistributionRows.length }} 个分组</el-tag>
          </template>

          <DashboardChart
            v-if="labDistributionRows.length"
            :option="labDistributionOption"
            :height="330"
          />
          <EmptyState
            v-else
            title="暂无实验室分布数据"
            description="当前条件下还没有实验室成员分布结果，请尝试放宽筛选条件或刷新数据。"
            caption="该图表会优先展示成员规模最高的实验室。"
            :action-label="hasMemberLens ? '清空成员筛选' : '刷新数据'"
            @action="hasMemberLens ? clearMemberLens() : handleRefresh()"
          />
        </ChartCard>

        <ChartCard
          :icon="PieChart"
          title="成员角色分布"
          description="展示管理员、负责人和普通成员的结构占比，帮助快速判断管理层与执行层配比。"
          :badge="sampleDrivenBadge"
          :accent="DASHBOARD_COLORS.emerald"
          class="statistics-grid__item"
        >
          <template #extra>
            <el-tag type="success" effect="plain">{{ formatCount(roleDistributionTotal, '0') }} 人</el-tag>
          </template>

          <DashboardChart
            v-if="roleDistributionRows.length"
            :option="roleDistributionOption"
            :height="330"
          />
          <EmptyState
            v-else
            title="暂无角色分布数据"
            description="当前还无法绘制成员角色结构，请先确认成员数据是否已同步。"
            caption="角色分布会随专业和实验室筛选联动更新。"
            :action-label="hasMemberLens ? '清空成员筛选' : '刷新数据'"
            @action="hasMemberLens ? clearMemberLens() : handleRefresh()"
          />
        </ChartCard>
      </div>
    </section>

    <section class="statistics-section">
      <SectionHeading
        :icon="Grid"
        title="补充分析"
        description="继续从专业、年级和新增趋势三个维度补全画像，让页面不仅能看总量，也能看结构和变化。"
        badge="第三屏"
      />

      <div class="statistics-grid statistics-grid--secondary">
        <ChartCard
          :icon="Management"
          title="专业分布"
          description="识别成员主要来源专业，便于后续招新宣传和培养方向调整。"
          :badge="majorBadge"
          :accent="DASHBOARD_COLORS.cyan"
          class="statistics-grid__item"
        >
          <DashboardChart
            v-if="majorDistributionRows.length"
            :option="majorDistributionOption"
            :height="310"
          />
          <EmptyState
            v-else
            title="暂无专业分布数据"
            description="请先完善成员专业信息，系统会自动生成专业分布图。"
            caption="可前往成员管理页面补充成员资料。"
            action-label="前往成员管理"
            @action="goToMembers"
          />
        </ChartCard>

        <ChartCard
          :icon="UserFilled"
          title="年级分布"
          description="观察不同年级成员的组成情况，帮助安排培养节奏与梯队建设。"
          :badge="sampleDrivenBadge"
          :accent="DASHBOARD_COLORS.violet"
          class="statistics-grid__item"
        >
          <DashboardChart
            v-if="gradeDistributionRows.length"
            :option="gradeDistributionOption"
            :height="310"
          />
          <EmptyState
            v-else
            :title="gradeEmptyTitle"
            :description="gradeEmptyDescription"
            caption="选择具体实验室后，年级分析会更准确。"
            :action-label="gradeEmptyActionLabel"
            @action="handleGradeEmptyAction"
          />
        </ChartCard>

        <ChartCard
          :icon="TrendCharts"
          title="最近新增成员趋势"
          description="按加入时间展示新增趋势，帮助判断近期增长节奏和招新成效。"
          :badge="sampleDrivenBadge"
          :accent="DASHBOARD_COLORS.amber"
          class="statistics-grid__item statistics-grid__item--wide"
        >
          <DashboardChart
            v-if="newMemberTrendRows.length"
            :option="newMemberTrendOption"
            :height="320"
          />
          <EmptyState
            v-else
            :title="trendEmptyTitle"
            :description="trendEmptyDescription"
            caption="当成员加入日期逐步完善后，这里会自动切换为趋势分析图。"
            :action-label="trendEmptyActionLabel"
            @action="handleTrendEmptyAction"
          />
        </ChartCard>
      </div>
    </section>

    <section class="statistics-section">
      <InsightCard
        :icon="Opportunity"
        title="数据洞察"
        description="系统基于当前统计结果自动生成结论，帮助你更快读懂整体情况与数据缺口。"
        :badge="insightBadge"
        :updated-at="lastUpdatedLabel"
        :items="insightItems"
      />
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import {
  Calendar,
  DataAnalysis,
  DataBoard,
  Download,
  Grid,
  Histogram,
  Management,
  OfficeBuilding,
  Opportunity,
  PieChart,
  Refresh,
  TrendCharts,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChartCard from '@/components/statistics/ChartCard.vue'
import DashboardChart from '@/components/statistics/DashboardChart.vue'
import EmptyState from '@/components/statistics/EmptyState.vue'
import InsightCard from '@/components/statistics/InsightCard.vue'
import SectionHeading from '@/components/statistics/SectionHeading.vue'
import StatCard from '@/components/statistics/StatCard.vue'
import { getAllLabs } from '@/api/lab'
import { getLabMemberPage } from '@/api/labMembers'
import { exportStatisticsDashboard, getStatisticsDashboard, getStatisticsLabs, getStatisticsMembers } from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import { downloadBlob } from '@/utils/export'
import {
  DASHBOARD_COLORS,
  ROLE_LABELS,
  buildBarOption,
  buildDonutOption,
  buildInsightItems,
  buildJoinTrendRows,
  buildLineOption,
  buildMemberDistribution,
  buildRoleRows,
  filterMemberRows,
  findTopRow,
  formatCount,
  formatDateTime,
  formatPercent,
  getGroupRows,
  normalizeMemberRows,
  normalizeRows,
  resolveRoleLabel,
  sumRows
} from '@/utils/statistics-dashboard'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const exportLoading = ref(false)
const dashboard = ref({})
const labGroups = ref([])
const memberGroups = ref([])
const labOptions = ref([])
const memberSample = ref([])
const memberSampleTotal = ref(0)
const memberSampleError = ref('')
const lastUpdatedAt = ref('')

const filters = reactive({
  dateRange: [
    dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD')
  ],
  labId: '',
  memberRole: '',
  major: ''
})

const summary = computed(() => dashboard.value.summary || {})
const scopeLabel = computed(() => dashboard.value.scopeName || userStore.userInfo?.labName || '当前统计范围')
const lastUpdatedLabel = computed(() => formatDateTime(lastUpdatedAt.value, '待同步'))
const startDate = computed(() => filters.dateRange?.[0] || dayjs().subtract(29, 'day').format('YYYY-MM-DD'))
const endDate = computed(() => filters.dateRange?.[1] || dayjs().format('YYYY-MM-DD'))
const rangeLabel = computed(() => `${startDate.value} 至 ${endDate.value}`)

const selectedLabName = computed(() => {
  const target = labOptions.value.find((item) => Number(item.id) === Number(filters.labId))
  return target?.labName || ''
})

const resolveScopedLabName = (labId) => {
  const normalizedLabId = Number(labId)
  const target = labOptions.value.find((item) => Number(item.id) === normalizedLabId)
  return target?.labName
    || dashboard.value.scopeName
    || userStore.userInfo?.labName
    || '实验室名称待同步'
}

const sampleRows = computed(() => normalizeMemberRows(memberSample.value))
const filteredMemberRows = computed(() => filterMemberRows(sampleRows.value, filters))
const sampleLoaded = computed(() => sampleRows.value.length > 0)
const sampleLimited = computed(() => memberSampleTotal.value > sampleRows.value.length)
const hasMemberLens = computed(() => Boolean(filters.memberRole || filters.major))

const aggregateTopLabRows = computed(() => getGroupRows(labGroups.value, 'topLabs').slice(0, 8))
const aggregateRoleRows = computed(() => buildRoleRows(getGroupRows(memberGroups.value, 'memberRoles')).slice(0, 6))
const aggregateMajorRows = computed(() => normalizeRows(getGroupRows(memberGroups.value, 'majorDistribution')).slice(0, 8))

const roleOptions = computed(() => {
  if (sampleLoaded.value) {
    const rows = filterMemberRows(sampleRows.value, {
      labId: filters.labId,
      memberRole: '',
      major: filters.major
    })
    const bucket = new Map()
    rows.forEach((row) => {
      const key = row.memberRole || 'unknown'
      bucket.set(key, (bucket.get(key) || 0) + 1)
    })

    return Array.from(bucket.entries())
      .map(([value, count]) => ({
        value,
        label: resolveRoleLabel(value),
        count
      }))
      .sort((left, right) => right.count - left.count)
  }

  return aggregateRoleRows.value.map((row) => ({
    value: row.rawName || row.name,
    label: row.name,
    count: row.value
  }))
})

const majorOptions = computed(() => {
  if (sampleLoaded.value) {
    const rows = filterMemberRows(sampleRows.value, {
      labId: filters.labId,
      memberRole: filters.memberRole,
      major: ''
    })
    const bucket = new Map()
    rows.forEach((row) => {
      if (!row.major) {
        return
      }
      bucket.set(row.major, (bucket.get(row.major) || 0) + 1)
    })

    return Array.from(bucket.entries())
      .map(([value, count]) => ({
        value,
        label: value,
        count
      }))
      .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, 'zh-CN'))
  }

  return aggregateMajorRows.value.map((row) => ({
    value: row.name,
    label: row.name,
    count: row.value
  }))
})

const selectedRoleLabel = computed(() => roleOptions.value.find((item) => item.value === filters.memberRole)?.label || '')

const filterSummaryLabel = computed(() => {
  const parts = []
  if (selectedLabName.value) {
    parts.push(selectedLabName.value)
  }
  if (selectedRoleLabel.value) {
    parts.push(selectedRoleLabel.value)
  }
  if (filters.major) {
    parts.push(filters.major)
  }
  return parts.length ? `当前视角：${parts.join(' / ')}` : ''
})

const sampleCoverageLabel = computed(() => {
  if (!sampleLoaded.value) {
    return '仅聚合统计'
  }
  if (sampleLimited.value) {
    return `${formatCount(sampleRows.value.length, '0')} / ${formatCount(memberSampleTotal.value, '0')} 条活跃成员`
  }
  return `${formatCount(sampleRows.value.length, '0')} 条活跃成员`
})

const precisionLabel = computed(() => {
  if (sampleLoaded.value && !sampleLimited.value) {
    return '精细筛选可用'
  }
  if (sampleLoaded.value) {
    return '明细样本模式'
  }
  return '聚合统计模式'
})

const precisionHint = computed(() => {
  if (sampleLoaded.value && !sampleLimited.value) {
    return '角色、专业、趋势图均可做联动分析'
  }
  if (sampleLoaded.value) {
    return '图表已联动成员明细，整体总览仍以聚合结果为准'
  }
  return '建议选择具体实验室后查看更细致的交叉分析'
})

const filterNotice = computed(() => {
  if (sampleLimited.value) {
    return '当前明细分析基于已加载的活跃成员样本，整体总览仍优先采用聚合统计结果。'
  }
  if (hasMemberLens.value && !sampleLoaded.value) {
    return '角色 / 专业筛选已启用，但年级与新增趋势仍建议先按实验室细分后查看。'
  }
  if (memberSampleError.value) {
    return memberSampleError.value
  }
  return ''
})

const totalMembers = computed(() => {
  if (hasMemberLens.value) {
    if (sampleLoaded.value) {
      return filteredMemberRows.value.length
    }
    if (filters.memberRole && !filters.major) {
      return roleOptions.value.find((item) => item.value === filters.memberRole)?.count ?? 0
    }
    if (!filters.memberRole && filters.major) {
      return majorOptions.value.find((item) => item.value === filters.major)?.count ?? 0
    }
    return null
  }

  const number = Number(summary.value.memberCount)
  return Number.isFinite(number) ? number : sumRows(aggregateRoleRows.value)
})

const managerCount = computed(() => {
  if (sampleLoaded.value) {
    return filteredMemberRows.value.filter((row) => row.memberRole === 'lab_admin').length
  }
  if (filters.major) {
    return null
  }
  if (filters.memberRole) {
    return filters.memberRole === 'lab_admin'
      ? roleOptions.value.find((item) => item.value === 'lab_admin')?.count ?? 0
      : 0
  }
  return aggregateRoleRows.value.find((row) => row.rawName === 'lab_admin')?.value ?? 0
})

const ordinaryCount = computed(() => {
  if (sampleLoaded.value) {
    return filteredMemberRows.value.filter((row) => row.memberRole === 'member').length
  }
  if (filters.major) {
    return null
  }
  if (filters.memberRole) {
    return filters.memberRole === 'member'
      ? roleOptions.value.find((item) => item.value === 'member')?.count ?? 0
      : 0
  }
  return aggregateRoleRows.value.find((row) => row.rawName === 'member')?.value ?? 0
})

const activeLabCount = computed(() => {
  if (sampleLoaded.value) {
    return new Set(filteredMemberRows.value.map((row) => row.labId || row.labName)).size
  }

  const number = Number(summary.value.labCount)
  return Number.isFinite(number) ? number : aggregateTopLabRows.value.length
})

const missingMajorCount = computed(() => {
  if (sampleLoaded.value) {
    return filteredMemberRows.value.filter((row) => !row.major).length
  }

  if (filters.memberRole && filters.major) {
    return null
  }

  if (filters.major) {
    return 0
  }

  if (filters.memberRole) {
    return null
  }

  const total = Number(summary.value.memberCount ?? 0)
  return Math.max(total - sumRows(aggregateMajorRows.value), 0)
})

const majorCompletionRate = computed(() => {
  if (sampleLoaded.value) {
    const total = filteredMemberRows.value.length
    if (!total) {
      return 0
    }
    return ((total - missingMajorCount.value) / total) * 100
  }

  if (filters.memberRole) {
    return null
  }

  const total = Number(totalMembers.value)
  if (!Number.isFinite(total) || total <= 0) {
    return 0
  }

  const missing = Number(missingMajorCount.value)
  if (!Number.isFinite(missing)) {
    return null
  }

  return ((total - missing) / total) * 100
})

const monthlyNewCount = computed(() => {
  if (!sampleLoaded.value) {
    return null
  }

  const monthStart = dayjs().startOf('month')
  return filteredMemberRows.value.filter((row) => {
    const date = dayjs(row.joinDate)
    return date.isValid() && !date.isBefore(monthStart, 'day')
  }).length
})

const labDistributionRows = computed(() => {
  if (sampleLoaded.value) {
    return buildMemberDistribution(filteredMemberRows.value, 'labName').slice(0, 8)
  }
  return aggregateTopLabRows.value
})

const roleDistributionRows = computed(() => {
  if (sampleLoaded.value) {
    return buildRoleRows(
      buildMemberDistribution(filteredMemberRows.value, 'memberRole', {
        labelMap: ROLE_LABELS
      }).slice(0, 6)
    )
  }
  return aggregateRoleRows.value
})

const majorDistributionRows = computed(() => {
  if (sampleLoaded.value) {
    return buildMemberDistribution(filteredMemberRows.value, 'major').slice(0, 8)
  }
  if (filters.major) {
    return aggregateMajorRows.value.filter((row) => row.name === filters.major)
  }
  return aggregateMajorRows.value
})

const gradeDistributionRows = computed(() => {
  if (!sampleLoaded.value) {
    return []
  }
  return buildMemberDistribution(filteredMemberRows.value, 'grade').slice(0, 8)
})

const newMemberTrendRows = computed(() => {
  if (!sampleLoaded.value) {
    return []
  }
  return buildJoinTrendRows(filteredMemberRows.value, startDate.value, endDate.value)
})

const roleDistributionTotal = computed(() => roleDistributionRows.value.reduce((total, row) => total + Number(row.value || 0), 0))
const topRole = computed(() => findTopRow(roleDistributionRows.value))
const topLab = computed(() => findTopRow(labDistributionRows.value))

const safePercent = (count, total) => {
  if (count === null || count === undefined || total === null || total === undefined) {
    return null
  }
  const number = Number(total)
  if (!Number.isFinite(number) || number <= 0) {
    return 0
  }
  return (Number(count) / number) * 100
}

const displayCount = (value) => (value === null || value === undefined ? '--' : formatCount(value))
const displayPercent = (value) => (value === null || value === undefined ? '--' : formatPercent(value, 0))

const metricCards = computed(() => {
  const labCount = Number(summary.value.labCount ?? 0)
  const managerShare = safePercent(managerCount.value, totalMembers.value)
  const memberShare = safePercent(ordinaryCount.value, totalMembers.value)

  return [
    {
      label: '实验室总数',
      value: displayCount(labCount),
      description: '当前统计范围内纳入看板的实验室总量。',
      trendLabel: '活跃视角',
      trendValue: scopeLabel.value,
      trendType: 'neutral',
      icon: OfficeBuilding,
      accent: DASHBOARD_COLORS.blue,
      accentSoft: 'rgba(37, 99, 235, 0.18)'
    },
    {
      label: '成员总数',
      value: displayCount(totalMembers.value),
      description: hasMemberLens.value
        ? '已按当前成员筛选视角重新计算。'
        : '展示当前范围内的成员总体规模。',
      trendLabel: '当前范围',
      trendValue: rangeLabel.value,
      trendType: 'neutral',
      icon: UserFilled,
      accent: DASHBOARD_COLORS.cyan,
      accentSoft: 'rgba(6, 182, 212, 0.18)'
    },
    {
      label: '实验室管理员',
      value: displayCount(managerCount.value),
      description: managerCount.value === null
        ? '选择具体实验室后可得到更精确的管理员人数。'
        : '仅统计实验室管理员角色，不含负责人。',
      trendLabel: '角色占比',
      trendValue: displayPercent(managerShare),
      trendType: 'up',
      icon: Management,
      accent: DASHBOARD_COLORS.emerald,
      accentSoft: 'rgba(16, 185, 129, 0.18)'
    },
    {
      label: '普通成员',
      value: displayCount(ordinaryCount.value),
      description: ordinaryCount.value === null
        ? '当前筛选下暂无精确的普通成员结构结果。'
        : '快速判断执行层成员是否占据主要比重。',
      trendLabel: '结构占比',
      trendValue: displayPercent(memberShare),
      trendType: 'up',
      icon: Grid,
      accent: DASHBOARD_COLORS.amber,
      accentSoft: 'rgba(245, 158, 11, 0.18)'
    },
    {
      label: '专业资料完整率',
      value: displayPercent(majorCompletionRate.value),
      description: majorCompletionRate.value === null
        ? '当前筛选需要更多成员明细才能精确计算。'
        : '用于判断成员画像字段是否足够支撑后续分析。',
      trendLabel: '待补资料',
      trendValue: missingMajorCount.value === null ? '--' : `${displayCount(missingMajorCount.value)} 人`,
      trendType: missingMajorCount.value > 0 ? 'down' : 'up',
      icon: DataAnalysis,
      accent: DASHBOARD_COLORS.violet,
      accentSoft: 'rgba(124, 58, 237, 0.16)'
    },
    {
      label: '活跃实验室数',
      value: displayCount(activeLabCount.value),
      description: sampleLoaded.value
        ? '已按当前成员视角统计存在成员的实验室数量。'
        : '没有细分成员视角时默认按当前范围展示。',
      trendLabel: '本月新增',
      trendValue: monthlyNewCount.value === null ? '待细分' : `${displayCount(monthlyNewCount.value)} 人`,
      trendType: monthlyNewCount.value > 0 ? 'up' : 'neutral',
      icon: TrendCharts,
      accent: DASHBOARD_COLORS.rose,
      accentSoft: 'rgba(244, 63, 94, 0.16)'
    }
  ]
})

const sampleDrivenBadge = computed(() => {
  if (sampleLoaded.value && !sampleLimited.value) {
    return '筛选联动'
  }
  if (sampleLoaded.value) {
    return '样本联动'
  }
  return '聚合统计'
})

const majorBadge = computed(() => {
  if (filters.major) {
    return `已聚焦 ${filters.major}`
  }
  return sampleDrivenBadge.value
})

const labDistributionOption = computed(() => buildBarOption({
  data: labDistributionRows.value,
  color: DASHBOARD_COLORS.blue
}))

const roleDistributionOption = computed(() => buildDonutOption({
  data: roleDistributionRows.value,
  colors: [
    DASHBOARD_COLORS.emerald,
    DASHBOARD_COLORS.blue,
    DASHBOARD_COLORS.amber,
    DASHBOARD_COLORS.violet,
    DASHBOARD_COLORS.rose
  ]
}))

const majorDistributionOption = computed(() => buildBarOption({
  data: majorDistributionRows.value,
  color: DASHBOARD_COLORS.cyan
}))

const gradeDistributionOption = computed(() => buildBarOption({
  data: gradeDistributionRows.value,
  color: DASHBOARD_COLORS.violet,
  horizontal: true
}))

const newMemberTrendOption = computed(() => buildLineOption({
  data: newMemberTrendRows.value,
  color: DASHBOARD_COLORS.amber
}))

const gradeEmptyTitle = computed(() => {
  if (!sampleLoaded.value) {
    return '暂无年级分布数据'
  }
  return '当前筛选下暂无年级数据'
})

const gradeEmptyDescription = computed(() => {
  if (!sampleLoaded.value) {
    return '当前范围缺少可用于年级分析的成员明细，建议先按实验室细分查看。'
  }
  return '当前筛选结果中没有可展示的年级信息，请尝试切换角色或专业筛选条件。'
})

const gradeEmptyActionLabel = computed(() => {
  if (!sampleLoaded.value) {
    return filters.labId ? '前往成员管理' : '刷新数据'
  }
  return hasMemberLens.value ? '清空成员筛选' : '前往成员管理'
})

const trendEmptyTitle = computed(() => {
  if (!sampleLoaded.value) {
    return '暂无新增趋势数据'
  }
  return '当前时间范围内暂无新增成员'
})

const trendEmptyDescription = computed(() => {
  if (!sampleLoaded.value) {
    return '当前范围缺少成员加入日期明细，建议选择具体实验室后查看新增趋势。'
  }
  return '当前所选时间范围内没有新增成员记录，趋势图会在后续有新成员加入时自动生成。'
})

const trendEmptyActionLabel = computed(() => {
  if (!sampleLoaded.value) {
    return filters.labId ? '前往成员管理' : '刷新数据'
  }
  return hasMemberLens.value ? '清空成员筛选' : '前往成员管理'
})

const insightItems = computed(() => buildInsightItems({
  scopeLabel: scopeLabel.value,
  labCount: Number(summary.value.labCount ?? activeLabCount.value ?? 0),
  memberCount: totalMembers.value ?? Number(summary.value.memberCount ?? 0),
  topRole: topRole.value,
  roleTotal: roleDistributionTotal.value,
  topLab: topLab.value,
  missingMajorCount: missingMajorCount.value ?? 0,
  selectedRoleLabel: selectedRoleLabel.value,
  selectedMajor: filters.major,
  preciseMode: sampleLoaded.value,
  monthlyNewCount: monthlyNewCount.value
}))

const insightBadge = computed(() => {
  if (missingMajorCount.value > 0) {
    return `待补资料 ${displayCount(missingMajorCount.value)} 人`
  }
  return '结构状态良好'
})

const buildParams = () => ({
  startDate: startDate.value,
  endDate: endDate.value,
  labId: filters.labId || undefined
})

const normalizeLabOptions = (response) => {
  const rows = response?.data?.records || response?.data || []
  return Array.isArray(rows) ? rows : []
}

const ensureScopeLabOption = () => {
  const currentLabId = Number(filters.labId || dashboard.value.labId || userStore.userInfo?.labId)
  if (!Number.isFinite(currentLabId) || currentLabId <= 0) {
    return
  }

  const exists = labOptions.value.some((item) => Number(item.id) === currentLabId)
  if (!exists) {
    labOptions.value.unshift({
      id: currentLabId,
      labName: resolveScopedLabName(currentLabId)
    })
  }
}

const sanitizeFilters = () => {
  if (filters.labId && !labOptions.value.some((item) => Number(item.id) === Number(filters.labId))) {
    filters.labId = ''
  }
  if (filters.memberRole && !roleOptions.value.some((item) => item.value === filters.memberRole)) {
    filters.memberRole = ''
  }
  if (filters.major && !majorOptions.value.some((item) => item.value === filters.major)) {
    filters.major = ''
  }
}

const loadMemberSample = async (params) => {
  memberSample.value = []
  memberSampleTotal.value = 0
  memberSampleError.value = ''

  const scopedLabId = params.labId || dashboard.value.labId || userStore.userInfo?.labId || undefined

  try {
    const response = await getLabMemberPage({
      pageNum: 1,
      pageSize: 1000,
      status: 'active',
      labId: scopedLabId
    })

    memberSample.value = response?.data?.records || []
    memberSampleTotal.value = Number(response?.data?.total ?? memberSample.value.length)
  } catch (error) {
    memberSample.value = []
    memberSampleTotal.value = 0
    memberSampleError.value = scopedLabId
      ? '当前角色下无法读取该实验室的成员明细，已自动回退为聚合统计模式。'
      : '当前范围暂未返回成员明细，涉及交叉筛选的模块会回退为聚合展示。'
  }
}

const loadStatistics = async () => {
  loading.value = true
  try {
    const params = buildParams()

    const [dashboardResult, labsResult, membersResult, labOptionsResult] = await Promise.allSettled([
      getStatisticsDashboard(params),
      getStatisticsLabs(params),
      getStatisticsMembers(params),
      getAllLabs({
        collegeId: userStore.userInfo?.managedCollegeId || undefined
      })
    ])

    if (dashboardResult.status !== 'fulfilled') {
      throw dashboardResult.reason
    }

    dashboard.value = dashboardResult.value?.data || {}
    labGroups.value = labsResult.status === 'fulfilled' ? labsResult.value?.data || [] : []
    memberGroups.value = membersResult.status === 'fulfilled' ? membersResult.value?.data || [] : []
    labOptions.value = labOptionsResult.status === 'fulfilled' ? normalizeLabOptions(labOptionsResult.value) : []

    ensureScopeLabOption()
    await loadMemberSample(params)
    sanitizeFilters()
    lastUpdatedAt.value = dayjs().toISOString()

    if (labsResult.status !== 'fulfilled' || membersResult.status !== 'fulfilled') {
      ElMessage.warning('部分图表数据未完整返回，页面已优先展示可用结果。')
    }
  } catch (error) {
    ElMessage.error(error?.message || '统计数据加载失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  loadStatistics()
}

const clearMemberLens = () => {
  filters.memberRole = ''
  filters.major = ''
}

const resetFilters = () => {
  filters.dateRange = [
    dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD')
  ]
  filters.labId = ''
  filters.memberRole = ''
  filters.major = ''
  loadStatistics()
}

const handleRefresh = () => {
  loadStatistics()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const response = await exportStatisticsDashboard(buildParams())
    downloadBlob(
      `lablink-statistics-${startDate.value}-${endDate.value}.xlsx`,
      response.data,
      response.headers?.['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    )
    ElMessage.success('统计报表已导出为 Excel 文件。')
  } catch (error) {
    ElMessage.error(error?.message || '导出统计报表失败，请稍后重试。')
  } finally {
    exportLoading.value = false
  }
}

const goToMembers = () => {
  router.push(route.path.startsWith('/m/') ? '/m/admin/members' : '/admin/members')
}

const handleGradeEmptyAction = () => {
  if (!sampleLoaded.value && !filters.labId) {
    handleRefresh()
    return
  }

  if (hasMemberLens.value) {
    clearMemberLens()
    return
  }

  goToMembers()
}

const handleTrendEmptyAction = () => {
  if (!sampleLoaded.value && !filters.labId) {
    handleRefresh()
    return
  }

  if (hasMemberLens.value) {
    clearMemberLens()
    return
  }

  goToMembers()
}

watch(() => filters.memberRole, () => {
  if (filters.major && !majorOptions.value.some((item) => item.value === filters.major)) {
    filters.major = ''
  }
})

watch(() => filters.major, () => {
  if (filters.memberRole && !roleOptions.value.some((item) => item.value === filters.memberRole)) {
    filters.memberRole = ''
  }
})

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-dashboard {
  padding-bottom: 8px;
}

.statistics-section {
  display: grid;
  gap: 18px;
}

.statistics-hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 0% 0%, rgba(56, 189, 248, 0.22), transparent 28%),
    radial-gradient(circle at 100% 0%, rgba(45, 212, 191, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(12, 74, 110, 0.92));
}

.statistics-hero::after {
  content: '';
  position: absolute;
  inset: auto -20% -35% auto;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.18), transparent 72%);
  pointer-events: none;
}

.statistics-hero__content,
.statistics-hero__actions {
  position: relative;
  z-index: 1;
}

.statistics-hero__content {
  display: grid;
  gap: 18px;
}

.statistics-hero__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.statistics-hero__pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  color: rgba(240, 249, 255, 0.92);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.statistics-hero__pill--ghost {
  background: rgba(15, 23, 42, 0.22);
}

.statistics-hero__pill--muted {
  color: #bfdbfe;
}

.statistics-hero__actions {
  min-width: 240px;
  display: grid;
  gap: 12px;
  justify-items: end;
}

.statistics-hero__button {
  min-width: 144px;
  border-radius: 14px;
  border-color: rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.1);
  color: #eff6ff;
}

.statistics-hero__button--primary {
  background: linear-gradient(135deg, #38bdf8, #14b8a6);
  border-color: transparent;
}

.statistics-hero__status {
  width: 100%;
  padding: 16px 18px;
  display: grid;
  gap: 4px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(226, 232, 240, 0.9);
}

.statistics-hero__status strong {
  font-size: 18px;
  color: #fff;
}

.statistics-hero__status small {
  color: rgba(226, 232, 240, 0.8);
  line-height: 1.65;
}

.statistics-filter-panel {
  display: grid;
  gap: 18px;
}

.statistics-filter-panel__body {
  display: grid;
  gap: 14px;
}

.statistics-filter-panel__form {
  margin-top: 0;
}

.statistics-filter-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.statistics-filter-panel__meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  color: #334155;
  background: rgba(148, 163, 184, 0.12);
}

.statistics-filter-panel__meta-chip--success {
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
}

.statistics-metrics-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 18px;
}

.statistics-grid__item {
  grid-column: span 6;
}

.statistics-grid__item--wide {
  grid-column: span 8;
}

.statistics-grid--primary .statistics-grid__item:last-child {
  grid-column: span 4;
}

.statistics-grid--secondary .statistics-grid__item:last-child {
  grid-column: span 12;
}

:deep(.statistics-grid .el-card__body) {
  padding: 22px;
}

:deep(.statistics-grid .el-card__header),
:deep(.insight-card .el-card__header) {
  padding: 22px 22px 0;
  border-bottom: 0;
}

:deep(.insight-card .el-card__body) {
  padding: 22px;
}

@media (max-width: 1280px) {
  .statistics-metrics-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .statistics-grid__item,
  .statistics-grid__item--wide,
  .statistics-grid--primary .statistics-grid__item:last-child,
  .statistics-grid--secondary .statistics-grid__item:last-child {
    grid-column: span 12;
  }
}

@media (max-width: 900px) {
  .statistics-hero__actions {
    width: 100%;
    justify-items: stretch;
  }

  .statistics-hero__button {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .statistics-dashboard {
    padding: 0;
    gap: 16px;
  }

  .statistics-hero {
    border-radius: 26px;
    padding: 22px;
    background: linear-gradient(135deg, #15324b, #197a78 58%, #2aa3a1);
  }

  .statistics-filter-panel {
    border-radius: 24px;
    padding: 16px;
  }

  .statistics-filter-panel__form {
    display: grid;
    grid-template-columns: 1fr;
  }

  .statistics-filter-panel__form :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 0;
  }

  .statistics-filter-panel__form :deep(.el-date-editor),
  .statistics-filter-panel__form :deep(.el-select) {
    width: 100% !important;
  }

  .statistics-metrics-grid {
    grid-template-columns: 1fr;
  }

  .statistics-grid {
    grid-template-columns: 1fr;
  }

  .statistics-grid__item,
  .statistics-grid__item--wide,
  .statistics-grid--primary .statistics-grid__item:last-child,
  .statistics-grid--secondary .statistics-grid__item:last-child {
    grid-column: auto;
  }

  .statistics-hero__pills {
    flex-direction: column;
  }

  .statistics-filter-panel__meta {
    flex-direction: column;
  }

  :deep(.statistics-grid .el-card__body),
  :deep(.insight-card .el-card__body) {
    padding: 16px;
  }

  :deep(.statistics-grid .el-card__header),
  :deep(.insight-card .el-card__header) {
    padding: 16px 16px 0;
  }
}
</style>

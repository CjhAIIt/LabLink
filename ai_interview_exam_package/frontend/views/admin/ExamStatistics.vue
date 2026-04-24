<template>
  <div class="exam-statistics-page">
    <div class="page-header">
      <h2>成绩统计</h2>
      <el-select v-model="selectedExamId" placeholder="选择考试" style="width: 240px" @change="loadStatistics">
        <el-option v-for="e in examList" :key="e.id" :label="e.title" :value="e.id" />
      </el-select>
    </div>

    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card"><div class="stat-value">{{ stats.totalCount ?? '-' }}</div><div class="stat-label">参考人数</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.submitRate ?? '-' }}%</div><div class="stat-label">提交率</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.avgScore ?? '-' }}</div><div class="stat-label">平均分</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.passRate ?? '-' }}%</div><div class="stat-label">通过率</div></div>
    </div>

    <!-- Charts -->
    <div class="charts-row">
      <div class="chart-card"><div class="chart-title">分数段分布</div><div ref="barChartRef" class="chart-container"></div></div>
      <div class="chart-card"><div class="chart-title">各题型得分占比</div><div ref="pieChartRef" class="chart-container"></div></div>
    </div>

    <!-- Top/Bottom Performers -->
    <div class="table-card">
      <div class="card-title">成绩排名</div>
      <el-table :data="stats.ranking || []" border stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="160" />
        <el-table-column prop="totalScore" label="总分" width="100" align="center" />
        <el-table-column prop="submitTime" label="提交时间" />
      </el-table>
    </div>

    <!-- Cheat Logs -->
    <div class="table-card">
      <div class="card-title">作弊记录</div>
      <el-table v-loading="loadingCheats" :data="cheatLogs" border stripe>
        <el-table-column prop="cheatType" label="类型" width="140" />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getAdminExamList, getAdminExamStatistics, getAdminExamCheatLogs } from '@/api/writtenExam'

const selectedExamId = ref('')
const examList = ref([])
const stats = ref({})
const cheatLogs = ref([])
const loadingCheats = ref(false)
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null, pieChart = null

async function loadExams() {
  try { const { data } = await getAdminExamList(); examList.value = data?.records || data || [] } catch { /* ignore */ }
}

async function loadStatistics() {
  if (!selectedExamId.value) return
  try {
    const { data } = await getAdminExamStatistics(selectedExamId.value)
    stats.value = data || {}
    await nextTick()
    renderBarChart(data?.scoreDistribution || [])
    renderPieChart(data?.typeScoreRatio || [])
  } catch { /* ignore */ }
  loadingCheats.value = true
  try { const { data } = await getAdminExamCheatLogs({ examId: selectedExamId.value }); cheatLogs.value = data?.records || data || [] }
  finally { loadingCheats.value = false }
}

function renderBarChart(distribution) {
  if (!barChartRef.value) return
  barChart?.dispose()
  barChart = echarts.init(barChartRef.value)
  const labels = ['0-59', '60-69', '70-79', '80-89', '90-100']
  const values = labels.map((_, i) => distribution[i] ?? 0)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#606266' } },
    yAxis: { type: 'value', axisLabel: { color: '#606266' } },
    series: [{ type: 'bar', data: values, itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#79bbff' }]) } }],
    grid: { top: 20, right: 20, bottom: 30, left: 40 }
  })
}

function renderPieChart(typeData) {
  if (!pieChartRef.value) return
  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)
  const seriesData = (typeData.length ? typeData : [{ name: '暂无数据', value: 1 }])
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{ type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: true, itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, label: { show: false }, emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } }, data: seriesData }]
  })
}

function handleResize() { barChart?.resize(); pieChart?.resize() }
onMounted(() => { loadExams(); window.addEventListener('resize', handleResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); barChart?.dispose(); pieChart?.dispose() })
</script>

<style scoped>
.exam-statistics-page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 600; color: #1a1a2e; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 24px; text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; color: #409eff; margin-bottom: 6px; }
.stat-label { font-size: 14px; color: #909399; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.chart-container { width: 100%; height: 320px; }
.table-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 20px; margin-bottom: 20px; }
.card-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 14px; }
</style>

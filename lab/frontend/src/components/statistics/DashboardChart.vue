<template>
  <div ref="chartRef" class="dashboard-chart" :style="{ height: normalizedHeight }" />
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  option: {
    type: Object,
    default: () => ({})
  },
  height: {
    type: [String, Number],
    default: 320
  }
})

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const normalizedHeight = computed(() => (typeof props.height === 'number' ? `${props.height}px` : props.height))

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) {
    return
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  chartInstance.setOption(props.option || {}, true)
  chartInstance.resize()
}

onMounted(async () => {
  await renderChart()

  if (typeof ResizeObserver !== 'undefined' && chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize()
    })
    resizeObserver.observe(chartRef.value)
  }

  window.addEventListener('resize', renderChart)
})

watch(
  () => props.option,
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', renderChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.dashboard-chart {
  width: 100%;
}
</style>

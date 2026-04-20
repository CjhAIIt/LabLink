<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <div class="chart-card__header">
        <div class="chart-card__title-wrap">
          <span class="chart-card__title">{{ title }}</span>
          <small v-if="subtitle" class="chart-card__subtitle">{{ subtitle }}</small>
        </div>
        <div class="chart-card__extra">
          <slot name="header-extra">
            <el-tag v-if="tagLabel" :type="tagType" :effect="tagEffect">{{ tagLabel }}</el-tag>
          </slot>
        </div>
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  tagLabel: {
    type: [String, Number],
    default: ''
  },
  tagType: {
    type: String,
    default: 'info'
  },
  tagEffect: {
    type: String,
    default: 'plain'
  }
})
</script>

<style scoped>
.chart-card {
  border-radius: 20px;
}

.chart-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.chart-card__title-wrap {
  display: grid;
  gap: 4px;
}

.chart-card__title {
  font-weight: 700;
  color: #0f172a;
}

.chart-card__subtitle {
  color: #64748b;
}

.chart-card__extra {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>

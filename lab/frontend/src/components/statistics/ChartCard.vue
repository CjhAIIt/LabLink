<template>
  <el-card shadow="never" class="chart-card" :style="styleVars">
    <template #header>
      <div class="chart-card__header">
        <div class="chart-card__title-group">
          <div class="chart-card__icon">
            <el-icon :size="18">
              <component :is="icon" />
            </el-icon>
          </div>
          <div class="chart-card__copy">
            <div class="chart-card__title-row">
              <h3>{{ title }}</h3>
              <span v-if="badge" class="chart-card__badge">{{ badge }}</span>
            </div>
            <p v-if="description">{{ description }}</p>
          </div>
        </div>
        <div v-if="$slots.extra" class="chart-card__extra">
          <slot name="extra" />
        </div>
      </div>
    </template>
    <div class="chart-card__body">
      <slot />
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  },
  accent: {
    type: String,
    default: '#2563eb'
  }
})

const styleVars = computed(() => ({
  '--chart-accent': props.accent
}))
</script>

<style scoped>
.chart-card {
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.98));
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
  backdrop-filter: blur(16px);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.chart-card:hover {
  transform: translateY(-4px);
  border-color: rgba(96, 165, 250, 0.26);
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.1);
}

.chart-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.chart-card__title-group {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.chart-card__icon {
  width: 42px;
  height: 42px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #eff6ff;
  background:
    linear-gradient(135deg, var(--chart-accent), rgba(14, 165, 233, 0.95));
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.18);
}

.chart-card__copy {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.chart-card__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.chart-card__title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.chart-card__copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.chart-card__badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--chart-accent);
  background: color-mix(in srgb, var(--chart-accent) 12%, #ffffff);
}

.chart-card__extra {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.chart-card__body {
  min-height: 240px;
}

@media (max-width: 768px) {
  .chart-card {
    border-radius: 24px;
  }

  .chart-card__header {
    flex-direction: column;
  }

  .chart-card__icon {
    width: 38px;
    height: 38px;
    border-radius: 12px;
  }
}
</style>

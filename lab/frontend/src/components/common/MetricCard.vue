<template>
  <article class="metric-card" :class="{ 'metric-card--compact': compact }">
    <span class="metric-card__label">{{ label }}</span>
    <strong class="metric-card__value">{{ value }}</strong>
    <span v-if="tip" class="metric-card__tip">{{ tip }}</span>
  </article>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: '-'
  },
  tip: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.metric-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(241, 245, 249, 0.95));
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.metric-card--compact {
  padding: 16px 18px;
}

.metric-card__label,
.metric-card__tip {
  color: #64748b;
}

.metric-card__value {
  color: #0f172a;
  font-size: 24px;
  line-height: 1.1;
}

.metric-card--compact .metric-card__value {
  font-size: 20px;
}
</style>

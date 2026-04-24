<template>
  <article class="stat-card" :style="styleVars">
    <div class="stat-card__glow" />
    <div class="stat-card__top">
      <div class="stat-card__icon">
        <el-icon :size="20">
          <component :is="icon" />
        </el-icon>
      </div>
      <div v-if="trendLabel || trendValue" class="stat-card__trend" :class="trendClass">
        <span v-if="trendLabel" class="stat-card__trend-label">{{ trendLabel }}</span>
        <strong v-if="trendValue" class="stat-card__trend-value">{{ trendValue }}</strong>
      </div>
    </div>

    <div class="stat-card__body">
      <span class="stat-card__label">{{ label }}</span>
      <strong class="stat-card__value">{{ value }}</strong>
      <p class="stat-card__description">{{ description }}</p>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  label: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: '--'
  },
  description: {
    type: String,
    default: ''
  },
  trendLabel: {
    type: String,
    default: ''
  },
  trendValue: {
    type: String,
    default: ''
  },
  trendType: {
    type: String,
    default: 'neutral'
  },
  accent: {
    type: String,
    default: '#2563eb'
  },
  accentSoft: {
    type: String,
    default: 'rgba(37, 99, 235, 0.16)'
  },
  index: {
    type: Number,
    default: 0
  }
})

const styleVars = computed(() => ({
  '--stat-accent': props.accent,
  '--stat-accent-soft': props.accentSoft,
  '--stat-delay': `${Math.max(0, props.index) * 80}ms`
}))

const trendClass = computed(() => `stat-card__trend--${props.trendType}`)
</script>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 20px;
  padding: 22px;
  min-height: 190px;
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.98));
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
  transform: translateY(0);
  transition:
    transform 0.26s ease,
    box-shadow 0.26s ease,
    border-color 0.26s ease;
  animation: stat-card-enter 0.55s ease both;
  animation-delay: var(--stat-delay);
}

.stat-card:hover {
  transform: translateY(-6px);
  border-color: rgba(96, 165, 250, 0.32);
  box-shadow: 0 26px 58px rgba(15, 23, 42, 0.13);
}

.stat-card__glow {
  position: absolute;
  inset: -32% auto auto -8%;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--stat-accent-soft), transparent 70%);
  pointer-events: none;
  transition: transform 0.35s ease;
}

.stat-card:hover .stat-card__glow {
  transform: scale(1.12);
}

.stat-card__top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--stat-accent), rgba(15, 23, 42, 0.92));
  box-shadow: 0 14px 26px color-mix(in srgb, var(--stat-accent) 22%, transparent);
}

.stat-card__trend {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  backdrop-filter: blur(10px);
}

.stat-card__trend--up {
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
}

.stat-card__trend--down {
  color: #b91c1c;
  background: rgba(244, 63, 94, 0.12);
}

.stat-card__trend--neutral {
  color: #334155;
  background: rgba(148, 163, 184, 0.12);
}

.stat-card__trend-label {
  color: inherit;
  opacity: 0.82;
}

.stat-card__trend-value {
  font-weight: 700;
}

.stat-card__body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
}

.stat-card__label {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #64748b;
}

.stat-card__value {
  color: #0f172a;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.stat-card__description {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

@keyframes stat-card-enter {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .stat-card {
    min-height: auto;
    padding: 20px 18px;
    border-radius: 22px;
  }

  .stat-card__value {
    font-size: 30px;
  }
}
</style>

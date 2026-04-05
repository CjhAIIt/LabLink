<template>
  <div class="brand-logo" :class="[sizeClass, `tone-${tone}`]" :aria-label="title">
    <div class="brand-mark" aria-hidden="true">
      <span class="brand-mark-text">AIIT</span>
    </div>

    <div v-if="showText" class="brand-copy">
      <div class="brand-title">{{ title }}</div>
      <div v-if="subtitle" class="brand-subtitle">{{ subtitle }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'dark'
  },
  size: {
    type: String,
    default: 'md'
  },
  showText: {
    type: Boolean,
    default: true
  }
})

const sizeClass = computed(() => `size-${props.size}`)
</script>

<style scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-mark {
  position: relative;
  flex-shrink: 0;
  border-radius: 20px;
  background:
    radial-gradient(circle at 22% 18%, rgba(191, 219, 254, 0.42), transparent 30%),
    linear-gradient(135deg, #1d4ed8, #38bdf8 58%, #22d3ee);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 14px 30px rgba(29, 78, 216, 0.18);
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.18);
  pointer-events: none;
}

.brand-mark::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent 48%);
  pointer-events: none;
}

.brand-mark-text {
  position: relative;
  z-index: 1;
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #f8fafc;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-indent: 0.18em;
  text-transform: uppercase;
  text-shadow: 0 4px 18px rgba(8, 15, 35, 0.28);
}

.brand-copy {
  min-width: 0;
}

.brand-title,
.brand-subtitle {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-title {
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 4px;
  font-weight: 500;
}

.tone-dark .brand-title {
  color: #0f172a;
}

.tone-dark .brand-subtitle {
  color: #64748b;
}

.tone-light .brand-title {
  color: #f8fafc;
}

.tone-light .brand-subtitle {
  color: rgba(203, 213, 225, 0.84);
}

.size-sm .brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 16px;
}

.size-sm .brand-mark-text {
  font-size: 11px;
}

.size-sm .brand-title {
  font-size: 16px;
  line-height: 1.1;
}

.size-sm .brand-subtitle {
  font-size: 12px;
  line-height: 1.25;
}

.size-md .brand-mark {
  width: 54px;
  height: 54px;
}

.size-md .brand-mark-text {
  font-size: 13px;
}

.size-md .brand-title {
  font-size: 18px;
  line-height: 1.1;
}

.size-md .brand-subtitle {
  font-size: 12px;
  line-height: 1.3;
}

.size-lg .brand-mark {
  width: 62px;
  height: 62px;
  border-radius: 22px;
}

.size-lg .brand-mark-text {
  font-size: 15px;
}

.size-lg .brand-title {
  font-size: 24px;
  line-height: 1.05;
}

.size-lg .brand-subtitle {
  font-size: 13px;
  line-height: 1.35;
}

@media (max-width: 768px) {
  .size-lg .brand-title {
    font-size: 20px;
  }
}
</style>

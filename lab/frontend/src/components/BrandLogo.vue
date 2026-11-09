<template>
  <div class="brand-logo" :class="[sizeClass, `tone-${tone}`]" :aria-label="title">
    <div class="brand-mark" aria-hidden="true">
      <BrandMark />
    </div>

    <div v-if="showText" class="brand-copy">
      <div class="brand-title">{{ title }}</div>
      <div v-if="subtitle" class="brand-subtitle">{{ subtitle }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BrandMark from '@/components/BrandMark.vue'

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
  border-radius: var(--ll-radius-md);
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(30, 64, 175, 0.22);
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.24);
  pointer-events: none;
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
  color: var(--ll-text-primary);
}

.tone-dark .brand-subtitle {
  color: var(--ll-text-secondary);
}

.tone-light .brand-title {
  color: var(--ll-text-primary);
}

.tone-light .brand-subtitle {
  color: rgba(203, 213, 225, 0.84);
}

.size-sm .brand-mark {
  width: 46px;
  height: 46px;
  border-radius: var(--ll-radius-md);
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
  border-radius: var(--ll-radius-lg);
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

<template>
  <button class="mobile-fab" type="button" @click="$emit('click')">
    <el-icon :size="22"><component :is="resolveIcon(icon)" /></el-icon>
    <span v-if="label">{{ label }}</span>
  </button>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineEmits(['click'])

const ICON_MAP = {
  Plus
}

const resolveIcon = (icon) => (typeof icon === 'string' ? ICON_MAP[icon] || Plus : icon || Plus)

defineProps({
  icon: {
    type: [String, Object],
    default: 'Plus'
  },
  label: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <MobileActionSheet
    :model-value="modelValue"
    :title="title"
    :description="description"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="mobile-filter-sheet__body">
      <slot />
    </div>
    <template #footer>
      <div class="mobile-filter-sheet__footer">
        <el-button plain @click="$emit('reset')">重置</el-button>
        <el-button type="primary" @click="$emit('apply')">应用筛选</el-button>
      </div>
    </template>
  </MobileActionSheet>
</template>

<script setup>
import MobileActionSheet from '@/components/mobile/MobileActionSheet.vue'

defineEmits(['update:modelValue', 'reset', 'apply'])

defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '筛选'
  },
  description: {
    type: String,
    default: '选择条件后应用到当前列表。'
  }
})
</script>

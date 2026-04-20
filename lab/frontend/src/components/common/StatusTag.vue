<template>
  <el-tag :type="resolved.type" :effect="effect" :size="size" :round="round">
    {{ resolved.label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { resolveStatusPresentation } from '@/utils/status-presenters'

const props = defineProps({
  value: {
    type: [String, Number],
    default: ''
  },
  preset: {
    type: String,
    default: 'generic'
  },
  fallbackLabel: {
    type: String,
    default: '-'
  },
  effect: {
    type: String,
    default: 'light'
  },
  size: {
    type: String,
    default: 'default'
  },
  round: {
    type: Boolean,
    default: false
  },
  labelMap: {
    type: Object,
    default: null
  },
  typeMap: {
    type: Object,
    default: null
  }
})

const resolved = computed(() =>
  resolveStatusPresentation(props.value, props.preset, props.fallbackLabel, props.labelMap, props.typeMap)
)
</script>

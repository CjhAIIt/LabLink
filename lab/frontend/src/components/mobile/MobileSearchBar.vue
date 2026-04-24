<template>
  <div class="mobile-search-bar">
    <el-input
      :model-value="modelValue"
      clearable
      :placeholder="placeholder"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('search')"
      @clear="$emit('search')"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <button v-if="filterable" class="mobile-search-bar__filter" type="button" @click="$emit('filter')">
      <el-icon :size="18"><Filter /></el-icon>
      <span v-if="filterText">{{ filterText }}</span>
    </button>
    <button class="mobile-search-bar__search" type="button" @click="$emit('search')">
      <el-icon :size="18"><Search /></el-icon>
    </button>
  </div>
</template>

<script setup>
import { Filter, Search } from '@element-plus/icons-vue'

defineEmits(['update:modelValue', 'search', 'filter'])

defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '搜索'
  },
  filterable: {
    type: Boolean,
    default: true
  },
  filterText: {
    type: String,
    default: ''
  }
})
</script>

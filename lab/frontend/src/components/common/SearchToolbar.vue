<template>
  <div class="search-toolbar">
    <el-form :inline="true" class="search-toolbar__form" @submit.prevent>
      <el-form-item v-if="showKeyword" :label="keywordLabel">
        <el-input
          :model-value="modelValue"
          :placeholder="placeholder"
          :clearable="clearable"
          :style="{ width: keywordWidth }"
          @update:model-value="$emit('update:modelValue', $event)"
          @keyup.enter="$emit('search')"
        />
      </el-form-item>
      <slot />
      <el-form-item v-if="showDefaultActions">
        <el-button type="primary" @click="$emit('search')">{{ searchText }}</el-button>
        <el-button v-if="showReset" @click="$emit('reset')">{{ resetText }}</el-button>
      </el-form-item>
    </el-form>
    <div v-if="$slots.actions" class="search-toolbar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  showKeyword: {
    type: Boolean,
    default: true
  },
  keywordLabel: {
    type: String,
    default: '关键词'
  },
  placeholder: {
    type: String,
    default: '请输入关键词'
  },
  keywordWidth: {
    type: String,
    default: '240px'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  showDefaultActions: {
    type: Boolean,
    default: true
  },
  showReset: {
    type: Boolean,
    default: true
  },
  searchText: {
    type: String,
    default: '搜索'
  },
  resetText: {
    type: String,
    default: '重置'
  }
})

defineEmits(['update:modelValue', 'search', 'reset'])
</script>

<style scoped>
.search-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.search-toolbar__form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
}

.search-toolbar__actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
</style>

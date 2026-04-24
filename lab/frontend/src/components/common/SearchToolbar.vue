<template>
  <div class="search-toolbar">
    <div v-if="collapsibleOnMobile && isPhoneWide" class="mobile-filter-shell">
      <div class="mobile-filter-shell__header">
        <div>
          <strong>{{ collapseTitle }}</strong>
          <div v-if="collapseSummary" class="mobile-filter-shell__summary">{{ collapseSummary }}</div>
        </div>
        <button class="mobile-filter-shell__toggle" type="button" @click="mobileExpanded = !mobileExpanded">
          {{ mobileExpanded ? '收起筛选' : '展开筛选' }}
        </button>
      </div>
      <div class="mobile-filter-shell__body" :hidden="!mobileExpanded">
        <el-form :inline="true" class="search-toolbar__form" @submit.prevent>
          <el-form-item v-if="showKeyword" :label="keywordLabel">
            <el-input
              :model-value="modelValue"
              :placeholder="placeholder"
              :clearable="clearable"
              :style="{ width: keywordWidth }"
              @update:model-value="updateModelValue"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <slot />
          <el-form-item v-if="showDefaultActions">
            <el-button type="primary" @click="handleSearch">{{ searchText }}</el-button>
            <el-button v-if="showReset" @click="handleReset">{{ resetText }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <el-form v-else :inline="true" class="search-toolbar__form" @submit.prevent>
      <el-form-item v-if="showKeyword" :label="keywordLabel">
        <el-input
          :model-value="modelValue"
          :placeholder="placeholder"
          :clearable="clearable"
          :style="{ width: keywordWidth }"
          @update:model-value="updateModelValue"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <slot />
      <el-form-item v-if="showDefaultActions">
        <el-button type="primary" @click="handleSearch">{{ searchText }}</el-button>
        <el-button v-if="showReset" @click="handleReset">{{ resetText }}</el-button>
      </el-form-item>
    </el-form>
    <div v-if="$slots.actions" class="search-toolbar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useViewport } from '@/composables/useViewport'

const props = defineProps({
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
    default: '请输入关键字'
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
  collapsibleOnMobile: {
    type: Boolean,
    default: false
  },
  collapseTitle: {
    type: String,
    default: '筛选条件'
  },
  collapseSummary: {
    type: String,
    default: ''
  },
  defaultCollapsed: {
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

const emit = defineEmits(['update:modelValue', 'search', 'reset'])
const { isPhoneWide } = useViewport()
const mobileExpanded = ref(!props.defaultCollapsed)

const updateModelValue = (value) => emit('update:modelValue', value)
const handleSearch = () => emit('search')
const handleReset = () => emit('reset')
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

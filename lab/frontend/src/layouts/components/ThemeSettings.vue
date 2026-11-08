<template>
  <el-drawer
    :model-value="modelValue"
    title="界面设置"
    direction="rtl"
    size="320px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="setting-block">
      <span class="setting-label">主题模式</span>
      <el-radio-group :model-value="themeMode" size="small" @change="onThemeChange">
        <el-radio-button :value="THEME_MODES.LIGHT">浅色</el-radio-button>
        <el-radio-button :value="THEME_MODES.DARK">深色</el-radio-button>
        <el-radio-button :value="THEME_MODES.AUTO">跟随系统</el-radio-button>
      </el-radio-group>
    </div>

    <div class="setting-block">
      <span class="setting-label">界面密度</span>
      <el-radio-group :model-value="density" size="small" @change="onDensityChange">
        <el-radio-button :value="DENSITIES.DEFAULT">默认</el-radio-button>
        <el-radio-button :value="DENSITIES.COMPACT">紧凑</el-radio-button>
      </el-radio-group>
      <p class="setting-hint">紧凑模式收紧表格行高、控件尺寸与页面留白。</p>
    </div>

    <div class="setting-block">
      <span class="setting-label">主色调</span>
      <div class="preset-list">
        <button
          v-for="preset in THEME_PRESETS"
          :key="preset.id"
          class="preset-item"
          :class="{ active: accent === preset.id && !customAccent }"
          type="button"
          @click="onPresetChange(preset.id)"
        >
          <span class="preset-swatches">
            <span
              v-for="(color, key) in preset.colors"
              :key="key"
              class="preset-swatch"
              :style="{ background: color }"
            />
          </span>
          <span class="preset-name">{{ preset.name }}</span>
        </button>
      </div>

      <div class="custom-row">
        <span class="custom-label">自定义</span>
        <el-color-picker
          :model-value="customAccent || resolvedAccent"
          size="small"
          @change="onCustomAccentChange"
        />
        <button v-if="customAccent" class="custom-reset" type="button" @click="onPresetChange(accent)">
          恢复预设
        </button>
      </div>
    </div>

    <div class="setting-block">
      <div class="setting-row">
        <span class="setting-label">收起侧边栏</span>
        <el-switch
          :model-value="sidebarCollapsed"
          size="small"
          @change="onSidebarChange"
        />
      </div>
      <div class="setting-row">
        <span class="setting-label">显示页签栏</span>
        <el-switch
          :model-value="showTagsView"
          size="small"
          @change="onTagsViewChange"
        />
      </div>
    </div>

    <template #footer>
      <el-button class="reset-button" @click="resetAll">恢复默认设置</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import {
  DENSITIES,
  THEME_MODES,
  THEME_PRESETS,
  useSettingsStore
} from '@/stores/settings'

defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const settingsStore = useSettingsStore()
const { themeMode, density, accent, customAccent, resolvedAccent, sidebarCollapsed, showTagsView } =
  storeToRefs(settingsStore)

const onThemeChange = (mode) => settingsStore.setThemeMode(mode)
const onDensityChange = (value) => settingsStore.setDensity(value)
const onPresetChange = (id) => settingsStore.setAccent(id)
const onCustomAccentChange = (color) => settingsStore.setCustomAccent(color)
const onSidebarChange = (value) => settingsStore.setSidebarCollapsed(value)
const onTagsViewChange = (value) => settingsStore.setShowTagsView(value)

const resetAll = () => {
  settingsStore.setThemeMode(THEME_MODES.AUTO)
  settingsStore.setDensity(DENSITIES.DEFAULT)
  settingsStore.setAccent(THEME_PRESETS[0].id)
  settingsStore.setSidebarCollapsed(false)
  settingsStore.setShowTagsView(true)
}
</script>

<style scoped>
.setting-block {
  padding-bottom: var(--ll-space-5);
  margin-bottom: var(--ll-space-5);
  border-bottom: 1px solid var(--ll-border-subtle);
}

.setting-block:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
}

.setting-label {
  display: block;
  margin-bottom: var(--ll-space-3);
  font-size: var(--ll-text-sm);
  font-weight: 500;
  color: var(--ll-text-primary);
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ll-space-3);
}

.setting-row .setting-label {
  margin-bottom: 0;
}

.setting-hint {
  margin-top: var(--ll-space-2);
  font-size: var(--ll-text-xs);
  line-height: 1.6;
  color: var(--ll-text-tertiary);
}

.preset-list {
  display: grid;
  gap: var(--ll-space-2);
}

.preset-item {
  display: flex;
  align-items: center;
  gap: var(--ll-space-3);
  width: 100%;
  padding: var(--ll-space-2) var(--ll-space-3);
  border: 1px solid var(--ll-border-subtle);
  border-radius: var(--ll-radius-md);
  background: var(--ll-bg-surface);
  cursor: pointer;
  transition: border-color var(--ll-transition-fast), background-color var(--ll-transition-fast);
}

.preset-item:hover {
  border-color: var(--ll-border-default);
}

.preset-item.active {
  border-color: var(--ll-accent-600);
  background: var(--ll-bg-selected);
}

.preset-swatches {
  display: inline-flex;
  gap: 3px;
}

.preset-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  border: 1px solid rgba(9, 9, 11, 0.08);
}

.preset-name {
  font-size: var(--ll-text-sm);
  color: var(--ll-text-primary);
}

.custom-row {
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  margin-top: var(--ll-space-3);
}

.custom-label {
  font-size: var(--ll-text-sm);
  color: var(--ll-text-secondary);
}

.custom-reset {
  border: none;
  background: transparent;
  padding: 0;
  font-size: var(--ll-text-xs);
  color: var(--ll-text-link);
  cursor: pointer;
}

.reset-button {
  width: 100%;
}
</style>

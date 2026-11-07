<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    @click="toggleTheme"
  >
    <el-icon :size="16"><component :is="isDark ? Moon : Sunny" /></el-icon>
  </button>
</template>

<script setup>
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()
</script>

<style scoped>
.theme-toggle {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--ll-border-subtle);
  border-radius: var(--ll-radius-md);
  background: var(--ll-bg-surface);
  color: var(--ll-text-secondary);
  transition: color var(--ll-transition-fast), background-color var(--ll-transition-fast),
    border-color var(--ll-transition-fast);
}

.theme-toggle:hover {
  color: var(--ll-text-brand);
  border-color: var(--ll-border-default);
  background: var(--ll-bg-muted);
}
</style>

<template>
  <div class="m-shell">
    <header class="m-topbar">
      <button v-if="canGoBack" class="m-icon-btn" type="button" @click="router.back()">
        <el-icon :size="18"><ArrowLeft /></el-icon>
      </button>
      <div class="m-title">
        <span class="m-title-text">{{ title }}</span>
      </div>
      <div class="m-topbar-actions">
        <ThemeToggle />
        <el-dropdown placement="bottom-end" @command="$emit('command', $event)">
          <button class="m-user-btn" type="button">
            <el-avatar :size="30">{{ userInitial }}</el-avatar>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="m-content">
      <router-view />
    </main>

    <nav class="m-tabbar" aria-label="导航">
      <button
        v-for="item in tabItems"
        :key="item.path"
        class="m-tab"
        :class="{ active: isActive(item.path) }"
        type="button"
        @click="router.push(item.path)"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span class="m-tab-label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ThemeToggle from '@/components/ThemeToggle.vue'

const props = defineProps({
  /** 底部标签 [{ path, label, icon }] */
  tabItems: { type: Array, default: () => [] },
  /** 头像文字 */
  userInitial: { type: String, default: '' },
  /** 根路径，用于判断是否展示返回按钮 */
  rootPath: { type: String, default: '' }
})

defineEmits(['command'])

const route = useRoute()
const router = useRouter()

const title = computed(() => route.meta.title || '')
const canGoBack = computed(() => props.rootPath && route.path !== props.rootPath)

const isActive = (path) => route.path === path || (path !== props.rootPath && route.path.startsWith(path))
</script>

<style scoped>
.m-shell {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--ll-bg-body);
}

/* ---------- 顶栏 ---------- */
.m-topbar {
  position: sticky;
  top: 0;
  z-index: var(--ll-z-topbar);
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  height: 52px;
  padding: env(safe-area-inset-top) 12px 0 12px;
  background: var(--ll-bg-chrome);
  border-bottom: 1px solid var(--ll-border-subtle);
  backdrop-filter: blur(12px);
}

.m-icon-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--ll-radius-sm);
  background: transparent;
  color: var(--ll-text-secondary);
}

.m-icon-btn:active {
  background: var(--ll-bg-hover);
}

.m-title {
  flex: 1;
  min-width: 0;
}

.m-title-text {
  font-size: var(--ll-text-lg);
  font-weight: 600;
  color: var(--ll-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  flex-shrink: 0;
}

.m-user-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--ll-radius-full);
  background: transparent;
  padding: 0;
}

/* ---------- 内容 ---------- */
.m-content {
  flex: 1;
  padding: var(--ll-space-3) var(--ll-space-3) calc(72px + env(safe-area-inset-bottom));
}

/* ---------- 底部标签栏 ---------- */
.m-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--ll-z-sticky);
  display: grid;
  grid-template-columns: repeat(v-bind('tabItems.length'), minmax(0, 1fr));
  gap: 2px;
  padding: 6px 8px calc(6px + env(safe-area-inset-bottom));
  background: var(--ll-bg-chrome);
  border-top: 1px solid var(--ll-border-subtle);
  backdrop-filter: blur(14px);
}

.m-tab {
  border: none;
  background: transparent;
  padding: 6px 4px;
  border-radius: var(--ll-radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  color: var(--ll-text-tertiary);
  transition: color var(--ll-transition-fast), background-color var(--ll-transition-fast);
}

.m-tab.active {
  color: var(--ll-brand-600);
  background: var(--ll-brand-50);
}

.m-tab-label {
  font-size: 11px;
  line-height: 1;
  font-weight: 500;
}
</style>

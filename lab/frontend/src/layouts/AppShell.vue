<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ 'is-rail': isRail, 'is-open': isDrawerOpen }">
      <div class="sidebar-inner">
        <div class="sidebar-brand">
          <BrandLogo title="LabLink" subtitle="高校实验室管理平台" tone="dark" size="sm" />
        </div>

        <el-menu
          :default-active="activePath"
          :collapse="isRail"
          :collapse-transition="false"
          router
          class="sidebar-menu"
        >
          <template v-for="group in menuGroups" :key="group.label">
            <div v-if="!isRail" class="menu-group-label">{{ group.label }}</div>
            <template v-for="item in group.items" :key="item.path">
              <el-sub-menu v-if="item.children" :index="item.path">
                <template #title>
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.label }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                  {{ child.label }}
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="item.path">
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>{{ item.label }}</template>
              </el-menu-item>
            </template>
          </template>
        </el-menu>
      </div>
    </aside>

    <div v-if="isMobile && isDrawerOpen" class="sidebar-mask" @click="closeDrawer"></div>

    <div class="main-shell">
      <header class="topbar">
        <div class="topbar-left">
          <el-button class="collapse-btn" text @click="toggleSidebar">
            <el-icon :size="18">
              <component :is="isMobile ? Expand : isRail ? Expand : Fold" />
            </el-icon>
          </el-button>
          <nav class="topbar-heading" aria-label="面包屑">
            <span class="topbar-role">{{ roleLabel }}</span>
            <el-icon class="topbar-sep"><ArrowRight /></el-icon>
            <h2 class="topbar-title">{{ pageTitle }}</h2>
          </nav>
        </div>

        <div class="topbar-actions">
          <ThemeToggle />
          <button class="icon-btn" type="button" title="界面设置" @click="settingsVisible = true">
            <el-icon :size="16"><Setting /></el-icon>
          </button>
          <el-dropdown @command="$emit('command', $event)">
            <button class="user-chip" type="button">
              <el-avatar :size="30">{{ userInitial }}</el-avatar>
              <span class="user-chip-text">
                <strong>{{ userName }}</strong>
                <small>{{ userMeta }}</small>
              </span>
              <el-icon class="user-chip-caret"><ArrowDown /></el-icon>
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

      <TagsView v-if="showTagsView" />

      <main class="content">
        <div class="content-inner">
          <router-view />
        </div>
      </main>
    </div>

    <ThemeSettings v-model="settingsVisible" />
  </div>
</template>

<script setup>
import { ArrowDown, ArrowRight, Expand, Fold, Setting } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BrandLogo from '@/components/BrandLogo.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import TagsView from '@/layouts/components/TagsView.vue'
import ThemeSettings from '@/layouts/components/ThemeSettings.vue'
import { useSettingsStore } from '@/stores/settings'

defineProps({
  /** 顶栏左侧的角色标签，例如「管理端」 */
  roleLabel: { type: String, default: '' },
  /** 导航分组 [{ label, items: [{ path, label, icon, children? }] }] */
  menuGroups: { type: Array, default: () => [] },
  /** 用户显示名 */
  userName: { type: String, default: '' },
  /** 用户副标题，通常是角色说明 */
  userMeta: { type: String, default: '' },
  /** 头像文字 */
  userInitial: { type: String, default: '' }
})

defineEmits(['command'])

const route = useRoute()
const settingsStore = useSettingsStore()
const { sidebarCollapsed, showTagsView } = storeToRefs(settingsStore)

const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 960 : false)
/** 桌面端收成图标栏，与设置面板中的「收起侧边栏」联动 */
const isRail = computed(() => sidebarCollapsed.value && !isMobile.value)
/** 移动端抽屉是否展开 */
const isDrawerOpen = ref(false)
/** 界面设置抽屉 */
const settingsVisible = ref(false)

const activePath = computed(() => route.fullPath)
const pageTitle = computed(() => route.meta.title || '工作台')

const updateViewport = () => {
  const mobile = window.innerWidth <= 960
  if (mobile === isMobile.value) {
    return
  }
  isMobile.value = mobile
  if (mobile) {
    isDrawerOpen.value = false
  }
}

const toggleSidebar = () => {
  if (isMobile.value) {
    isDrawerOpen.value = !isDrawerOpen.value
    return
  }
  settingsStore.setSidebarCollapsed(!sidebarCollapsed.value)
}

const closeDrawer = () => {
  if (isMobile.value) {
    isDrawerOpen.value = false
  }
}

watch(
  () => route.fullPath,
  () => {
    closeDrawer()
  }
)

onMounted(() => {
  updateViewport()
  settingsStore.applyToDocument()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  background: var(--ll-bg-body);
  overflow: hidden;
  position: relative;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: var(--ll-sidebar-width);
  flex-shrink: 0;
  background: var(--ll-bg-surface);
  border-right: 1px solid var(--ll-border-subtle);
  transition: width var(--ll-transition-base);
}

.sidebar.is-rail {
  width: var(--ll-sidebar-width-collapsed);
}

.sidebar-inner {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-brand {
  height: var(--ll-topbar-height);
  display: flex;
  align-items: center;
  padding: 0 var(--ll-space-4);
  border-bottom: 1px solid var(--ll-border-subtle);
  flex-shrink: 0;
  overflow: hidden;
}

.sidebar.is-rail .sidebar-brand {
  padding: 0;
  justify-content: center;
}

.sidebar.is-rail .sidebar-brand :deep(.brand-copy) {
  display: none;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--ll-space-3) var(--ll-space-3) var(--ll-space-6);
  border-right: none;
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: var(--ll-gray-200);
  border: none;
}

.menu-group-label {
  padding: var(--ll-space-4) var(--ll-space-3) var(--ll-space-2);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ll-text-tertiary);
  white-space: nowrap;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 38px;
  line-height: 38px;
  margin-bottom: 2px;
  border-radius: var(--ll-radius-md);
  color: var(--ll-text-secondary);
  font-size: var(--ll-text-base);
  transition: background-color var(--ll-transition-fast), color var(--ll-transition-fast);
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--ll-bg-hover);
  color: var(--ll-text-primary);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--ll-bg-selected);
  color: var(--ll-accent-600);
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 38px !important;
  height: 34px;
  line-height: 34px;
  font-size: var(--ll-text-sm);
}

.sidebar.is-rail .sidebar-menu {
  padding-left: var(--ll-space-2);
  padding-right: var(--ll-space-2);
}

.sidebar-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: var(--ll-z-drawer-mask);
}

/* ---------- 主区 ---------- */
.main-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.topbar {
  height: var(--ll-topbar-height);
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--ll-space-4);
  padding: 0 var(--ll-space-6);
  background: var(--ll-bg-surface);
  border-bottom: 1px solid var(--ll-border-subtle);
  position: sticky;
  top: 0;
  z-index: var(--ll-z-topbar);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  min-width: 0;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  flex-shrink: 0;
}

.topbar-heading {
  display: flex;
  align-items: baseline;
  gap: var(--ll-space-2);
  min-width: 0;
}

.topbar-role {
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
  white-space: nowrap;
}

.topbar-sep {
  font-size: 11px;
  color: var(--ll-text-disabled);
}

.icon-btn {
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

.icon-btn:hover {
  color: var(--ll-text-primary);
  border-color: var(--ll-border-default);
  background: var(--ll-bg-muted);
}

.topbar-title {
  font-size: var(--ll-text-lg);
  font-weight: 600;
  color: var(--ll-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collapse-btn {
  color: var(--ll-text-secondary);
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: var(--ll-radius-sm);
}

.collapse-btn:hover {
  background: var(--ll-bg-hover);
  color: var(--ll-text-primary);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: var(--ll-space-2);
  padding: 4px 10px 4px 4px;
  border-radius: var(--ll-radius-full);
  border: 1px solid var(--ll-border-subtle);
  background: var(--ll-bg-surface);
  cursor: pointer;
  transition: background-color var(--ll-transition-fast), border-color var(--ll-transition-fast);
}

.user-chip:hover {
  background: var(--ll-bg-muted);
  border-color: var(--ll-border-default);
}

.user-chip-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  text-align: left;
  min-width: 0;
}

.user-chip-text strong {
  font-size: var(--ll-text-sm);
  font-weight: 600;
  color: var(--ll-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-chip-text small {
  font-size: var(--ll-text-xs);
  color: var(--ll-text-tertiary);
  white-space: nowrap;
}

.user-chip-caret {
  font-size: 12px;
  color: var(--ll-text-tertiary);
}

.content {
  flex: 1;
  overflow-y: auto;
  background: var(--ll-bg-body);
}

.content-inner {
  max-width: var(--ll-content-max-width);
  margin: 0 auto;
  padding: var(--ll-space-6);
}

/* ---------- 响应式 ---------- */
@media (max-width: 960px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: var(--ll-z-sidebar);
    width: var(--ll-sidebar-width);
    transform: translateX(-100%);
    transition: transform var(--ll-transition-base);
    box-shadow: var(--ll-shadow-lg);
  }

  .sidebar.is-open {
    transform: translateX(0);
  }

  .sidebar.is-rail {
    width: var(--ll-sidebar-width);
  }

  .topbar {
    padding: 0 var(--ll-space-4);
  }
}

@media (max-width: 768px) {
  .main-shell {
    height: 100dvh;
  }

  .topbar {
    padding: 0 var(--ll-space-3);
    gap: var(--ll-space-2);
  }

  .topbar-heading {
    gap: var(--ll-space-1);
  }

  .topbar-role {
    display: none;
  }

  .topbar-title {
    font-size: var(--ll-text-base);
  }

  .user-chip-text {
    display: none;
  }

  .user-chip {
    padding: 3px;
  }

  .content-inner {
    padding: var(--ll-space-3);
  }
}
</style>

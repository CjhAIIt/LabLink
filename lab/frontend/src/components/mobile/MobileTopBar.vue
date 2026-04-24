<template>
  <header class="mobile-topbar">
    <button
      class="mobile-topbar__back"
      :class="{ 'is-hidden': !canGoBack }"
      type="button"
      :aria-hidden="!canGoBack"
      :disabled="!canGoBack"
      @click="$emit('back')"
    >
      <el-icon :size="20"><ArrowLeft /></el-icon>
    </button>

    <div class="mobile-topbar__title">
      <strong>{{ title }}</strong>
      <span>{{ subtitle }}</span>
      <small v-if="contextLabel">{{ contextLabel }}</small>
    </div>

    <div class="mobile-topbar__actions">
      <MobileMenuDrawer
        v-if="menuItems.length"
        :items="menuItems"
        :portal-role="portalRole"
        :title="menuTitle"
        :eyebrow="menuEyebrow"
      />
      <MobileMessageBadge :path="notificationPath" />
      <el-dropdown placement="bottom-end" @command="$emit('command', $event)">
        <button class="mobile-topbar__avatar" type="button" aria-label="账号菜单">
          <el-avatar :size="34">{{ userInitial }}</el-avatar>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="desktop">切换桌面端</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
import MobileMenuDrawer from '@/components/common/MobileMenuDrawer.vue'
import MobileMessageBadge from '@/components/mobile/MobileMessageBadge.vue'

defineEmits(['back', 'command'])

defineProps({
  title: {
    type: String,
    default: 'LabLink'
  },
  subtitle: {
    type: String,
    default: '移动工作台'
  },
  contextLabel: {
    type: String,
    default: ''
  },
  canGoBack: {
    type: Boolean,
    default: false
  },
  notificationPath: {
    type: String,
    required: true
  },
  userInitial: {
    type: String,
    default: 'U'
  },
  menuItems: {
    type: Array,
    default: () => []
  },
  portalRole: {
    type: String,
    default: 'student'
  },
  menuTitle: {
    type: String,
    default: '全部功能'
  },
  menuEyebrow: {
    type: String,
    default: 'LabLink'
  }
})
</script>

<style scoped>
.mobile-topbar {
  position: sticky;
  top: 0;
  z-index: 70;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: calc(12px + env(safe-area-inset-top)) 12px 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.86));
  border-bottom: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(20px);
}

.mobile-topbar__back,
.mobile-topbar__avatar {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  color: #172033;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  cursor: pointer;
}

.mobile-topbar__back.is-hidden {
  visibility: hidden;
  pointer-events: none;
}

.mobile-topbar__title {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.mobile-topbar__title strong,
.mobile-topbar__title span,
.mobile-topbar__title small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-topbar__title strong {
  color: #172033;
  font-size: 17px;
  line-height: 1.15;
}

.mobile-topbar__title span {
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
}

.mobile-topbar__title small {
  width: fit-content;
  max-width: 100%;
  margin-top: 3px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(51, 136, 187, 0.1);
  color: var(--mobile-primary, #3388bb);
  font-size: 11px;
  font-weight: 800;
}

.mobile-topbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-topbar__avatar {
  padding: 0;
  border-radius: 999px;
}

.mobile-topbar__avatar :deep(.el-avatar) {
  background: linear-gradient(135deg, var(--mobile-primary, #3388bb), var(--mobile-accent, #22b8cf));
  color: #ffffff;
  font-weight: 800;
}

@media (max-width: 380px) {
  .mobile-topbar {
    gap: 8px;
    padding-left: 10px;
    padding-right: 10px;
  }

  .mobile-topbar__actions {
    gap: 6px;
  }
}
</style>

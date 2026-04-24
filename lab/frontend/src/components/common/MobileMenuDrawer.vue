<template>
  <div class="mobile-menu-entry">
    <button class="m-nav-btn mobile-menu-trigger" type="button" aria-label="打开全部功能" @click="visible = true">
      <el-icon :size="20"><Menu /></el-icon>
    </button>

    <el-drawer
      v-model="visible"
      custom-class="mobile-menu-drawer"
      direction="rtl"
      :size="drawerSize"
      :with-header="false"
    >
      <div class="mobile-menu-panel">
        <header class="mobile-menu-head">
          <div>
            <span>{{ eyebrow }}</span>
            <strong>{{ title }}</strong>
          </div>
          <button class="mobile-menu-close" type="button" aria-label="关闭全部功能" @click="visible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </header>

        <div class="mobile-menu-groups">
          <section v-for="group in menuGroups" :key="group.title" class="mobile-menu-group">
            <h3>{{ group.title }}</h3>
            <button
              v-for="item in group.items"
              :key="item.path"
              class="mobile-menu-item"
              :class="{ active: isActive(item.path) }"
              type="button"
              @click="go(item.path)"
            >
              <el-icon :size="19"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </button>
          </section>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { Close, Menu } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useViewport } from '@/composables/useViewport'
import { resolveDesktopMenuGroups } from '@/utils/portal-menu'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  portalRole: {
    type: String,
    default: 'student'
  },
  title: {
    type: String,
    default: '全部功能'
  },
  eyebrow: {
    type: String,
    default: 'LabLink'
  }
})

const visible = ref(false)
const route = useRoute()
const router = useRouter()
const { isPhoneWide } = useViewport()

const drawerSize = computed(() => (isPhoneWide.value ? '92%' : '380px'))
const menuGroups = computed(() => resolveDesktopMenuGroups(props.items || [], props.portalRole))

const isActive = (path) => route.path === path || (path !== '/' && route.path.startsWith(path))

const go = async (path) => {
  if (!path) return
  visible.value = false
  await router.push(path)
}
</script>

<style scoped>
.mobile-menu-entry {
  display: contents;
}

.mobile-menu-trigger {
  flex-shrink: 0;
}

:global(.mobile-menu-drawer) {
  border-top-left-radius: 24px;
  border-bottom-left-radius: 24px;
  overflow: hidden;
}

.mobile-menu-panel {
  min-height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.14), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.mobile-menu-head {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: calc(16px + env(safe-area-inset-top)) 16px 14px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(226, 232, 240, 0.86);
  backdrop-filter: blur(14px);
}

.mobile-menu-head span {
  display: block;
  margin-bottom: 3px;
  color: #0f766e;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.mobile-menu-head strong {
  display: block;
  color: #0f172a;
  font-size: 18px;
}

.mobile-menu-close {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 14px;
  background: #ffffff;
  color: #334155;
}

.mobile-menu-groups {
  display: grid;
  gap: 14px;
  padding: 14px 14px calc(22px + env(safe-area-inset-bottom));
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-menu-group {
  display: grid;
  gap: 8px;
}

.mobile-menu-group h3 {
  margin: 6px 4px 0;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.mobile-menu-item {
  width: 100%;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 13px;
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  text-align: left;
  font-weight: 700;
}

.mobile-menu-item.active {
  color: #0f766e;
  background: rgba(15, 118, 110, 0.09);
  border-color: rgba(15, 118, 110, 0.2);
}

@media (max-width: 420px) {
  :global(.mobile-menu-drawer) {
    border-top-left-radius: 20px;
    border-bottom-left-radius: 20px;
  }
}
</style>

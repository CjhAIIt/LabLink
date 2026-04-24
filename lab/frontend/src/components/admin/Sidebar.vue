<template>
  <aside class="admin-sidebar" :class="{ 'admin-sidebar--mobile': mobile, 'is-open': open, 'is-collapsed': compact }">
    <div class="admin-sidebar__surface">
      <button
        v-if="!mobile"
        class="admin-sidebar__collapse"
        type="button"
        :title="compact ? '展开侧边栏' : '收起侧边栏'"
        :aria-label="compact ? '展开侧边栏' : '收起侧边栏'"
        @click="$emit('toggle-collapse')"
      >
        <el-icon>
          <component :is="compact ? Expand : Fold" />
        </el-icon>
      </button>

      <div class="admin-sidebar__brand">
        <div class="admin-sidebar__brand-main">
          <div v-if="compact" class="admin-sidebar__brand-mark">L</div>
          <template v-else>
            <BrandLogo title="LabLink" subtitle="高校实验室管理平台" tone="light" size="sm" />
            <span class="admin-sidebar__badge">Admin Suite</span>
          </template>
        </div>
        <p v-if="!compact" class="admin-sidebar__brand-note">统一管理实验室、成员、审批、资料与统计分析。</p>
      </div>

      <div class="admin-sidebar__nav">
        <SidebarGroup
          v-for="section in sections"
          :key="section.path"
          :group="section"
          :active-path="activePath"
          :open="openedMap[section.path]"
          :collapsed="compact"
          @toggle="toggleGroup"
          @select="$emit('navigate', $event)"
        />
      </div>

      <div class="admin-sidebar__footer" :title="compact ? roleLabel : undefined">
        <span class="admin-sidebar__status-dot" />
        <div v-if="!compact">
          <strong>{{ roleLabel }}</strong>
          <span>当前权限环境已就绪</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { Expand, Fold } from '@element-plus/icons-vue'
import { computed, reactive, watch } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import SidebarGroup from './SidebarGroup.vue'

const props = defineProps({
  sections: {
    type: Array,
    default: () => []
  },
  activePath: {
    type: String,
    default: ''
  },
  open: {
    type: Boolean,
    default: true
  },
  mobile: {
    type: Boolean,
    default: false
  },
  collapsed: {
    type: Boolean,
    default: false
  },
  roleLabel: {
    type: String,
    default: '管理角色'
  }
})

defineEmits(['navigate', 'toggle-collapse'])

const openedMap = reactive({})
const compact = computed(() => props.collapsed && !props.mobile)

const isActivePath = (path) => props.activePath === path || props.activePath.startsWith(`${path}/`)

const syncOpenedMap = () => {
  props.sections.forEach((section) => {
    const hasActiveChild = section.children.some((child) => isActivePath(child.path))
    if (openedMap[section.path] === undefined) {
      openedMap[section.path] = hasActiveChild
      return
    }
    if (hasActiveChild) {
      openedMap[section.path] = true
    }
  })
}

const toggleGroup = (path) => {
  openedMap[path] = !openedMap[path]
}

watch(
  () => props.sections,
  () => {
    syncOpenedMap()
  },
  { immediate: true, deep: true }
)

watch(
  () => props.activePath,
  () => {
    syncOpenedMap()
  },
  { immediate: true }
)
</script>

<style scoped>
.admin-sidebar {
  width: 272px;
  height: 100vh;
  flex-shrink: 0;
  padding: 14px 0 14px 14px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.16), transparent 26%),
    linear-gradient(180deg, #091222 0%, #0d1b31 50%, #0c2433 100%);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.03);
  transition: width 0.22s ease, padding 0.22s ease;
}

.admin-sidebar.is-collapsed {
  width: 88px;
  padding-left: 10px;
}

.admin-sidebar__surface {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px 14px 14px;
  border-radius: 28px 0 0 28px;
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(10, 19, 37, 0.98), rgba(10, 26, 44, 0.96));
  border: 1px solid rgba(125, 211, 252, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    28px 0 44px rgba(8, 15, 35, 0.18);
  transition: padding 0.22s ease, gap 0.22s ease, border-radius 0.22s ease;
}

.admin-sidebar.is-collapsed .admin-sidebar__surface {
  gap: 14px;
  padding: 18px 10px 14px;
}

.admin-sidebar__collapse {
  position: absolute;
  top: 20px;
  right: -14px;
  z-index: 2;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(248, 250, 252, 0.96);
  box-shadow: 0 14px 30px rgba(8, 15, 35, 0.24);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.admin-sidebar__collapse:hover {
  color: #0f172a;
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(8, 15, 35, 0.32);
}

.admin-sidebar__brand {
  padding: 2px 6px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.admin-sidebar.is-collapsed .admin-sidebar__brand {
  display: flex;
  justify-content: center;
  padding: 0 0 14px;
}

.admin-sidebar__brand-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.admin-sidebar__brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 17px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ecfeff;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  box-shadow: 0 16px 30px rgba(20, 184, 166, 0.22);
}

.admin-sidebar__badge {
  padding: 7px 10px;
  border-radius: 999px;
  color: #dbeafe;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-sidebar__brand-note {
  margin-top: 12px;
  color: rgba(148, 163, 184, 0.92);
  line-height: 1.65;
  font-size: 13px;
}

.admin-sidebar__nav {
  flex: 1;
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 12px;
  padding-right: 6px;
  overflow-y: auto;
}

.admin-sidebar.is-collapsed .admin-sidebar__nav {
  gap: 8px;
  padding-right: 0;
}

.admin-sidebar__nav::-webkit-scrollbar {
  width: 6px;
}

.admin-sidebar__nav::-webkit-scrollbar-track {
  background: transparent;
}

.admin-sidebar__nav::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.28);
}

.admin-sidebar__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px;
  border-radius: 18px;
  color: rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.admin-sidebar.is-collapsed .admin-sidebar__footer {
  justify-content: center;
  padding: 16px 0;
}

.admin-sidebar__status-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #34d399, #22d3ee);
  box-shadow: 0 0 0 6px rgba(52, 211, 153, 0.08);
}

.admin-sidebar__footer strong {
  display: block;
  color: #f8fafc;
  font-size: 14px;
}

.admin-sidebar__footer span {
  display: block;
  color: rgba(148, 163, 184, 0.88);
  font-size: 12px;
  margin-top: 2px;
}

.admin-sidebar--mobile {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 120;
  padding: 10px 0 10px 10px;
  transform: translateX(-110%);
  transition: transform 0.24s ease;
}

.admin-sidebar--mobile.is-open {
  transform: translateX(0);
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: min(300px, 92vw);
  }

  .admin-sidebar__surface {
    border-radius: 24px 0 0 24px;
  }
}
</style>

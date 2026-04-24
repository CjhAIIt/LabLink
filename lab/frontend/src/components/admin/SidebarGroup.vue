<template>
  <section class="sidebar-group" :class="{ 'is-open': open && !collapsed, 'is-active': hasActiveChild, 'is-collapsed': collapsed }">
    <button
      class="sidebar-group__header"
      type="button"
      :title="collapsed ? group.label : undefined"
      @click="handleHeaderClick"
    >
      <span class="sidebar-group__icon">
        <component :is="group.icon" />
      </span>

      <div v-if="!collapsed" class="sidebar-group__copy">
        <strong>{{ group.label }}</strong>
        <span>{{ group.children.length }} 项导航</span>
      </div>

      <span v-if="!collapsed" class="sidebar-group__arrow" />
    </button>

    <el-collapse-transition>
      <div v-show="open && !collapsed" class="sidebar-group__items">
        <SidebarMenuItem
          v-for="child in group.children"
          :key="child.path"
          :item="child"
          :active="isActivePath(child.path)"
          @select="$emit('select', $event)"
        />
      </div>
    </el-collapse-transition>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import SidebarMenuItem from './SidebarMenuItem.vue'

const props = defineProps({
  group: {
    type: Object,
    required: true
  },
  activePath: {
    type: String,
    default: ''
  },
  open: {
    type: Boolean,
    default: false
  },
  collapsed: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle', 'select'])

const isActivePath = (path) => props.activePath === path || props.activePath.startsWith(`${path}/`)

const hasActiveChild = computed(() => props.group.children.some((child) => isActivePath(child.path)))

const handleHeaderClick = () => {
  if (props.collapsed) {
    const firstChild = props.group.children?.[0]
    if (firstChild?.path) {
      emit('select', firstChild.path)
    }
    return
  }
  emit('toggle', props.group.path)
}
</script>

<style scoped>
.sidebar-group {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(18px);
  transition: background-color 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease, padding 0.22s ease;
}

.sidebar-group.is-active,
.sidebar-group:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(125, 211, 252, 0.14);
  box-shadow: 0 16px 32px rgba(8, 15, 35, 0.14);
}

.sidebar-group.is-collapsed {
  gap: 0;
  padding: 7px;
  border-radius: 18px;
}

.sidebar-group__header {
  width: 100%;
  border: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 16px;
  color: rgba(226, 232, 240, 0.94);
  background: transparent;
  transition: background-color 0.18s ease;
}

.sidebar-group.is-collapsed .sidebar-group__header {
  justify-content: center;
  gap: 0;
  padding: 7px;
}

.sidebar-group__header:hover {
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-group__icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #dff7ff;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.34), rgba(20, 184, 166, 0.22)),
    rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.sidebar-group.is-collapsed .sidebar-group__icon {
  width: 42px;
  height: 42px;
}

.sidebar-group__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.sidebar-group__copy {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 2px;
  text-align: left;
}

.sidebar-group__copy strong {
  color: #f8fafc;
  font-size: 14px;
  font-weight: 700;
}

.sidebar-group__copy span {
  color: rgba(148, 163, 184, 0.88);
  font-size: 12px;
}

.sidebar-group__arrow {
  width: 10px;
  height: 10px;
  border-right: 2px solid rgba(148, 163, 184, 0.9);
  border-bottom: 2px solid rgba(148, 163, 184, 0.9);
  transform: rotate(45deg);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.sidebar-group.is-open .sidebar-group__arrow {
  transform: rotate(225deg);
  border-color: #7dd3fc;
}

.sidebar-group__items {
  display: grid;
  gap: 4px;
  padding: 4px 4px 2px;
}
</style>

<template>
  <button class="sidebar-menu-item" :class="{ 'is-active': active }" type="button" @click="$emit('select', item.path)">
    <span class="sidebar-menu-item__indicator" />
    <span class="sidebar-menu-item__dot" />
    <span class="sidebar-menu-item__label">{{ item.label }}</span>
  </button>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  },
  active: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select'])
</script>

<style scoped>
.sidebar-menu-item {
  position: relative;
  width: 100%;
  border: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 0 14px 0 18px;
  border-radius: 14px;
  color: rgba(215, 244, 247, 0.76);
  background: transparent;
  transition: transform 0.18s ease, background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.sidebar-menu-item:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(2px);
}

.sidebar-menu-item.is-active {
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.2), rgba(20, 184, 166, 0.16)),
    rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(125, 211, 252, 0.18);
}

.sidebar-menu-item__indicator {
  position: absolute;
  inset: 8px auto 8px 4px;
  width: 4px;
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.18s ease;
}

.sidebar-menu-item.is-active .sidebar-menu-item__indicator {
  background: linear-gradient(180deg, #7dd3fc, #2dd4bf);
}

.sidebar-menu-item__dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.72);
  transition: background-color 0.18s ease, transform 0.18s ease;
}

.sidebar-menu-item:hover .sidebar-menu-item__dot,
.sidebar-menu-item.is-active .sidebar-menu-item__dot {
  background: #7dd3fc;
  transform: scale(1.15);
}

.sidebar-menu-item__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
}
</style>

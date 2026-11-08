<template>
  <nav class="tags-view" aria-label="页签导航">
    <router-link
      v-for="tag in visitedRoutes"
      :key="tag.path"
      class="tag-item"
      :class="{ active: tag.path === activePath }"
      :to="tag.fullPath"
    >
      <span class="tag-title">{{ tag.title }}</span>
      <el-icon
        v-if="!tag.affix"
        class="tag-close"
        :size="12"
        @click.prevent.stop="closeTag(tag)"
      >
        <Close />
      </el-icon>
    </router-link>
  </nav>
</template>

<script setup>
import { Close } from '@element-plus/icons-vue'
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsView } from '@/composables/useTagsView'

const route = useRoute()
const router = useRouter()
const { visitedRoutes, addRoute, removeRoute } = useTagsView()

const activePath = computed(() => route.path)

watch(
  () => route.fullPath,
  () => addRoute(route),
  { immediate: true }
)

const closeTag = (tag) => {
  const wasActive = tag.path === activePath.value
  removeRoute(tag.path)
  if (wasActive) {
    const fallback = visitedRoutes.value[visitedRoutes.value.length - 1]
    router.push(fallback ? fallback.fullPath : '/')
  }
}
</script>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  gap: var(--ll-space-1);
  padding: 6px var(--ll-space-6);
  background: var(--ll-bg-surface);
  border-bottom: 1px solid var(--ll-border-subtle);
  overflow-x: auto;
  scrollbar-width: none;
  flex-shrink: 0;
}

.tags-view::-webkit-scrollbar {
  display: none;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  height: 26px;
  padding: 0 8px 0 10px;
  border: 1px solid var(--ll-border-subtle);
  border-radius: var(--ll-radius-sm);
  background: var(--ll-bg-surface);
  font-size: var(--ll-text-sm);
  color: var(--ll-text-secondary);
  transition: color var(--ll-transition-fast), border-color var(--ll-transition-fast),
    background-color var(--ll-transition-fast);
}

.tag-item:hover {
  color: var(--ll-text-primary);
  border-color: var(--ll-border-default);
}

.tag-item.active {
  color: var(--ll-accent-600);
  border-color: var(--ll-accent-600);
  background: var(--ll-bg-selected);
  font-weight: 500;
}

.tag-title {
  white-space: nowrap;
}

.tag-close {
  border-radius: var(--ll-radius-full);
  padding: 2px;
  color: var(--ll-text-disabled);
  cursor: pointer;
}

.tag-close:hover {
  color: var(--ll-text-primary);
  background: var(--ll-bg-hover);
}

@media (max-width: 768px) {
  .tags-view {
    display: none;
  }
}
</style>

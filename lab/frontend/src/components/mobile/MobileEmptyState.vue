<template>
  <section class="mobile-empty-state">
    <div class="mobile-empty-state__art">
      <el-icon :size="34"><component :is="resolveIcon(icon)" /></el-icon>
    </div>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <slot />
  </section>
</template>

<script setup>
import {
  Bell,
  Calendar,
  CollectionTag,
  Document,
  OfficeBuilding,
  Search,
  Tickets,
  User,
  UserFilled
} from '@element-plus/icons-vue'

const ICON_MAP = {
  Bell,
  Calendar,
  CollectionTag,
  Document,
  OfficeBuilding,
  Search,
  Tickets,
  User,
  UserFilled
}

const resolveIcon = (icon) => (typeof icon === 'string' ? ICON_MAP[icon] || Document : icon || Document)

defineProps({
  icon: {
    type: [String, Object],
    default: 'Document'
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前还没有可展示的内容。'
  }
})
</script>

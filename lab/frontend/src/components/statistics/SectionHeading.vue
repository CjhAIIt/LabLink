<template>
  <header class="section-heading">
    <div class="section-heading__icon">
      <el-icon :size="18">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="section-heading__copy">
      <div class="section-heading__title-row">
        <h2>{{ title }}</h2>
        <span v-if="badge" class="section-heading__badge">{{ badge }}</span>
      </div>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.extra" class="section-heading__extra">
      <slot name="extra" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.section-heading__icon {
  width: 42px;
  height: 42px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #eff6ff;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(14, 165, 233, 0.95));
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.2);
}

.section-heading__copy {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 6px;
}

.section-heading__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.section-heading__title-row h2 {
  margin: 0;
  color: #0f172a;
  font-size: 21px;
  line-height: 1.2;
}

.section-heading__copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.section-heading__badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  background: rgba(16, 185, 129, 0.12);
}

.section-heading__extra {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .section-heading {
    flex-direction: column;
  }

  .section-heading__title-row h2 {
    font-size: 18px;
  }

  .section-heading__icon {
    width: 38px;
    height: 38px;
    border-radius: 12px;
  }
}
</style>

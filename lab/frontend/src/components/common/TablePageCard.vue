<template>
  <el-card shadow="never" class="table-page-card">
    <template #header>
      <div class="table-page-card__header">
        <div class="table-page-card__title-wrap">
          <span class="table-page-card__title">{{ title }}</span>
          <small v-if="subtitle" class="table-page-card__subtitle">{{ subtitle }}</small>
        </div>
        <div class="table-page-card__header-extra">
          <slot name="header-extra">
            <el-tag v-if="countLabel !== ''" :type="countTagType" :effect="countTagEffect">
              {{ countLabel }}
            </el-tag>
          </slot>
        </div>
      </div>
    </template>

    <div v-if="$slots.filters" class="table-page-card__filters">
      <slot name="filters" />
    </div>

    <div class="table-page-card__body">
      <slot />
    </div>

    <div v-if="$slots.pagination" class="table-page-card__pagination">
      <slot name="pagination" />
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  countLabel: {
    type: [String, Number],
    default: ''
  },
  countTagType: {
    type: String,
    default: 'info'
  },
  countTagEffect: {
    type: String,
    default: 'plain'
  }
})
</script>

<style scoped>
.table-page-card {
  border-radius: 20px;
}

.table-page-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.table-page-card__title-wrap {
  display: grid;
  gap: 4px;
}

.table-page-card__title {
  font-weight: 700;
  color: #0f172a;
}

.table-page-card__subtitle {
  color: #64748b;
}

.table-page-card__header-extra {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.table-page-card__filters {
  margin-bottom: 16px;
}

.table-page-card__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .table-page-card__pagination {
    justify-content: center;
  }
}
</style>

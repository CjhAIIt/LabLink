<template>
  <el-card shadow="never" class="insight-card">
    <template #header>
      <div class="insight-card__header">
        <div class="insight-card__title-group">
          <div class="insight-card__icon">
            <el-icon :size="18">
              <component :is="icon" />
            </el-icon>
          </div>
          <div class="insight-card__copy">
            <div class="insight-card__title-row">
              <h3>{{ title }}</h3>
              <span v-if="badge" class="insight-card__badge">{{ badge }}</span>
            </div>
            <p v-if="description">{{ description }}</p>
          </div>
        </div>
        <div class="insight-card__meta">
          <span class="insight-card__meta-label">数据更新时间</span>
          <strong>{{ updatedAt }}</strong>
        </div>
      </div>
    </template>

    <div class="insight-card__body">
      <article v-for="(item, index) in items" :key="`${index}-${item}`" class="insight-card__item">
        <span class="insight-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
        <p>{{ item }}</p>
      </article>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  title: {
    type: String,
    default: '数据洞察'
  },
  description: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  },
  updatedAt: {
    type: String,
    default: '--'
  },
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.insight-card {
  border-radius: 30px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.insight-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.insight-card__title-group {
  min-width: 0;
  display: flex;
  gap: 14px;
}

.insight-card__icon {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #ecfeff;
  background: linear-gradient(135deg, #0f766e, #22c55e);
  box-shadow: 0 16px 30px rgba(16, 185, 129, 0.18);
}

.insight-card__copy {
  display: grid;
  gap: 6px;
}

.insight-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.insight-card__title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.insight-card__copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.65;
}

.insight-card__badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0f766e;
  background: rgba(16, 185, 129, 0.12);
}

.insight-card__meta {
  min-width: 188px;
  padding: 14px 16px;
  border-radius: 18px;
  display: grid;
  gap: 4px;
  background: rgba(15, 23, 42, 0.04);
}

.insight-card__meta-label {
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.insight-card__meta strong {
  color: #0f172a;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}

.insight-card__body {
  display: grid;
  gap: 14px;
}

.insight-card__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: flex-start;
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.insight-card__index {
  min-width: 44px;
  padding: 9px 10px;
  display: inline-flex;
  justify-content: center;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #0f766e;
  background: rgba(45, 212, 191, 0.12);
}

.insight-card__item p {
  margin: 0;
  color: #0f172a;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .insight-card {
    border-radius: 24px;
  }

  .insight-card__header {
    flex-direction: column;
  }

  .insight-card__meta {
    min-width: 0;
    width: 100%;
  }

  .insight-card__item {
    grid-template-columns: 1fr;
  }
}
</style>

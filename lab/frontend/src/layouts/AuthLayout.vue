<template>
  <div class="auth-page" :style="{ '--auth-width': width }">
    <div class="auth-column">
      <header class="auth-brand">
        <BrandLogo size="lg" title="LabLink" subtitle="高校实验室管理平台" />
      </header>

      <section class="auth-card">
        <div class="auth-card-head">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>

        <slot />
      </section>

      <section v-if="features.length" class="auth-features">
        <div v-for="item in features" :key="item.label" class="feature-row">
          <span class="feature-label">{{ item.label }}</span>
          <span class="feature-text">{{ item.text }}</span>
        </div>
      </section>

      <footer class="auth-footer">
        <slot name="footer">
          <router-link to="/intro">查看平台介绍</router-link>
        </slot>
      </footer>
    </div>
  </div>
</template>

<script setup>
import BrandLogo from '@/components/BrandLogo.vue'

defineProps({
  /** 卡片标题 */
  title: { type: String, default: '' },
  /** 卡片副标题 */
  subtitle: { type: String, default: '' },
  /** 卡片下方的能力说明 [{ label, text }] */
  features: { type: Array, default: () => [] },
  /** 卡片最大宽度 */
  width: { type: String, default: '440px' }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ll-space-10) var(--ll-space-4);
  background: var(--ll-bg-body);
  position: relative;
}

/* 顶部一层极淡的品牌色晕染，替代原先多色径向渐变 */
.auth-page::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 420px;
  background: radial-gradient(ellipse 60% 100% at 50% 0%, rgba(99, 102, 241, 0.09), transparent 70%);
  pointer-events: none;
}

.auth-column {
  position: relative;
  width: min(var(--auth-width, 440px), 100%);
  display: flex;
  flex-direction: column;
  gap: var(--ll-space-6);
}

.auth-brand {
  display: flex;
  justify-content: center;
}

.auth-card {
  background: var(--ll-bg-surface);
  border: 1px solid var(--ll-border-subtle);
  border-radius: var(--ll-radius-xl);
  box-shadow: var(--ll-shadow-md);
  padding: var(--ll-space-8);
}

.auth-card-head {
  margin-bottom: var(--ll-space-6);
}

.auth-card-head h1 {
  font-size: var(--ll-text-xl);
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ll-text-primary);
}

.auth-card-head p {
  margin-top: var(--ll-space-2);
  font-size: var(--ll-text-sm);
  line-height: 1.6;
  color: var(--ll-text-secondary);
}

.auth-features {
  display: grid;
  gap: var(--ll-space-1);
}

.feature-row {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: var(--ll-space-3);
  align-items: start;
  padding: var(--ll-space-3) 0;
  border-top: 1px solid var(--ll-border-subtle);
}

.feature-label {
  font-size: var(--ll-text-xs);
  font-weight: 600;
  color: var(--ll-text-brand);
  background: var(--ll-brand-50);
  border-radius: var(--ll-radius-xs);
  padding: 2px 8px;
  text-align: center;
  white-space: nowrap;
}

.feature-text {
  font-size: var(--ll-text-sm);
  line-height: 1.6;
  color: var(--ll-text-secondary);
}

.auth-footer {
  text-align: center;
  font-size: var(--ll-text-sm);
}

.auth-footer :deep(a) {
  color: var(--ll-text-secondary);
}

.auth-footer :deep(a:hover) {
  color: var(--ll-text-brand);
}

@media (max-width: 768px) {
  .auth-page {
    padding: var(--ll-space-6) var(--ll-space-4);
    align-items: flex-start;
  }

  .auth-card {
    padding: var(--ll-space-6) var(--ll-space-5);
    border-radius: var(--ll-radius-lg);
  }

  .feature-row {
    grid-template-columns: 1fr;
    gap: var(--ll-space-1);
  }

  .feature-label {
    justify-self: start;
  }
}
</style>

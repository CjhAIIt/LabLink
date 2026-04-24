<template>
  <div class="empty-state">
    <div class="empty-state__art">
      <span class="empty-state__orb empty-state__orb--one" />
      <span class="empty-state__orb empty-state__orb--two" />
      <span class="empty-state__ring" />
      <span class="empty-state__bar empty-state__bar--one" />
      <span class="empty-state__bar empty-state__bar--two" />
      <span class="empty-state__bar empty-state__bar--three" />
      <span class="empty-state__dot empty-state__dot--one" />
      <span class="empty-state__dot empty-state__dot--two" />
    </div>

    <div class="empty-state__copy">
      <h4>{{ title }}</h4>
      <p>{{ description }}</p>
      <small v-if="caption">{{ caption }}</small>
    </div>

    <el-button v-if="actionLabel" text type="primary" class="empty-state__action" @click="$emit('action')">
      {{ actionLabel }}
    </el-button>
  </div>
</template>

<script setup>
defineEmits(['action'])

defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前条件下还没有可展示的数据。'
  },
  caption: {
    type: String,
    default: ''
  },
  actionLabel: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.empty-state {
  min-height: 260px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 18px;
  padding: 20px 0;
  text-align: center;
}

.empty-state__art {
  position: relative;
  width: 180px;
  height: 120px;
}

.empty-state__orb,
.empty-state__ring,
.empty-state__bar,
.empty-state__dot {
  position: absolute;
}

.empty-state__orb {
  border-radius: 50%;
  filter: blur(2px);
}

.empty-state__orb--one {
  top: 6px;
  left: 18px;
  width: 68px;
  height: 68px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.22), transparent 72%);
}

.empty-state__orb--two {
  right: 18px;
  bottom: 18px;
  width: 74px;
  height: 74px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.18), transparent 74%);
}

.empty-state__ring {
  inset: 18px 42px auto;
  height: 70px;
  border-radius: 999px;
  border: 1px dashed rgba(96, 165, 250, 0.32);
}

.empty-state__bar {
  bottom: 18px;
  width: 18px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.16), rgba(59, 130, 246, 0.42));
}

.empty-state__bar--one {
  left: 54px;
  height: 30px;
}

.empty-state__bar--two {
  left: 82px;
  height: 52px;
}

.empty-state__bar--three {
  left: 110px;
  height: 40px;
}

.empty-state__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 0 8px rgba(56, 189, 248, 0.12);
}

.empty-state__dot--one {
  top: 28px;
  right: 34px;
}

.empty-state__dot--two {
  bottom: 30px;
  left: 24px;
  background: #14b8a6;
  box-shadow: 0 0 0 8px rgba(20, 184, 166, 0.12);
}

.empty-state__copy {
  display: grid;
  gap: 8px;
  max-width: 320px;
}

.empty-state__copy h4 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.empty-state__copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.empty-state__copy small {
  color: #94a3b8;
  line-height: 1.7;
}

.empty-state__action {
  font-weight: 700;
}
</style>

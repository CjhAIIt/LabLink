<template>
  <article class="mobile-stat-card" :style="{ '--stat-accent': accent }">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small v-if="description">{{ description }}</small>
  </article>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: 0
  },
  description: {
    type: String,
    default: ''
  },
  accent: {
    type: String,
    default: '#3388BB'
  }
})
</script>

<template>
  <span class="mobile-status-tag" :class="`mobile-status-tag--${type}`">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'default'
  }
})
</script>

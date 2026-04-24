<template>
  <section class="mobile-card" :class="[`mobile-card--${variant}`]">
    <slot />
  </section>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'default'
  }
})
</script>

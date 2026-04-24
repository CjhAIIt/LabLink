<template>
  <div class="mobile-page-container" :class="{ 'mobile-page-container--flush': flush }">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  flush: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <svg class="brand-mark-svg" viewBox="0 0 128 128" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <defs>
      <linearGradient :id="gradientId" x1="16" y1="10" x2="112" y2="120" gradientUnits="userSpaceOnUse">
        <stop stop-color="var(--ll-logo-from)" />
        <stop offset="1" stop-color="var(--ll-logo-to)" />
      </linearGradient>
    </defs>
    <rect width="128" height="128" rx="28" :fill="`url(#${gradientId})`" />
    <g
      stroke="var(--ll-on-logo)"
      stroke-width="8"
      stroke-linecap="butt"
      stroke-linejoin="miter"
      fill="none"
    >
      <path d="M64 20L108 64" />
      <path d="M108 64L64 108" />
      <path d="M64 108L20 64" />
      <path d="M20 64L64 20" />
      <path d="M42 76L62 56" />
      <path d="M54 88L78 64" />
      <path d="M66 100L94 72" />
    </g>
  </svg>
</template>

<script setup>
import { ref } from 'vue'

/**
 * 品牌标识图形。
 *
 * 与 `public/favicon.svg` 是同一套图形：蓝色圆角方块 + 白色菱形与三条平行斜线。
 * 单独抽成组件是为了让侧边栏、登录页与平台介绍页共用同一份矢量，
 * 颜色跟随品牌标识令牌，暗色主题下也保持一致。
 */

// 渐变 id 必须唯一：页面上可能同时存在多个标识，重复 id 会让引用指向错误的定义
const gradientId = ref(`lablinkMarkBg-${Math.random().toString(36).slice(2, 10)}`)
</script>

<style scoped>
.brand-mark-svg {
  display: block;
  width: 100%;
  height: 100%;
}
</style>

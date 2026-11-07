<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
#app {
  font-family: "HarmonyOS Sans SC", "MiSans", "Source Han Sans SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1b1f2a;
  min-height: 100vh;
  background: #f6f7fb;
}
</style>
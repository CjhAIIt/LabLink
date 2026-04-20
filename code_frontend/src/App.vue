<template>
  <div class="common-layout">
    <el-container>
      <el-header class="header">
        <div class="logo">
          <el-icon :size="28" color="#409EFF"><Cpu /></el-icon>
          <span class="logo-text">GradPath 智途</span>
        </div>
        <el-menu
            :default-active="activeIndex"
            mode="horizontal"
            router
            class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/jobs">岗位雷达</el-menu-item>
          <el-menu-item index="/match">智能匹配</el-menu-item>
          <el-menu-item index="/learn">学习路径</el-menu-item>
          <el-menu-item index="/algo">算法训练</el-menu-item>
          <el-menu-item index="/interview">AI面试</el-menu-item>
        </el-menu>
        <div class="user-avatar">
          <router-link to="/profile">
            <el-avatar src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          </router-link>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
const activeIndex = ref('/')
// 图标需在 main.js 全局注册或按需引入
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  background: #fff;
  z-index: 100;
}
.logo { display: flex; align-items: center; gap: 10px; width: 200px; }
.logo-text { font-size: 20px; font-weight: bold; color: #303133; }
.nav-menu { flex: 1; justify-content: center; border-bottom: none !important; }
.main-content { background-color: #f5f7fa; min-height: calc(100vh - 60px); padding: 20px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
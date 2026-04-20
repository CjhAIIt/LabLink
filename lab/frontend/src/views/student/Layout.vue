<template>
  <div class="student-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="200px">
        <div class="logo">
          <h2>实验室招募</h2>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/student/dashboard">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/student/labs">
            <el-icon><Document /></el-icon>
            <span>实验室列表</span>
          </el-menu-item>
          <el-menu-item index="/student/delivery">
            <el-icon><Files /></el-icon>
            <span>我的申请</span>
          </el-menu-item>
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <!-- 顶部导航 -->
        <el-header>
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/student' }">学生系统</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="30" :src="userAvatar">
                  {{ userInitial }}
                </el-avatar>
                <span class="username">{{ currentUser?.realName || '用户' }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 主内容区 -->
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { House, Document, Files, User, ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'StudentLayout',
  components: {
    House,
    Document,
    Files,
    User,
    ArrowDown
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    
    // 当前用户信息
    const currentUser = computed(() => store.getters.currentUser)
    
    // 用户头像
    const userAvatar = computed(() => {
      return currentUser.value?.avatar || ''
    })
    
    // 用户名首字
    const userInitial = computed(() => {
      const name = currentUser.value?.realName || '用'
      return name.charAt(0)
    })
    
    // 当前页面标题
    const currentPageTitle = computed(() => {
      return route.meta.title || '首页'
    })
    
    // 处理下拉菜单命令
    const handleCommand = async (command) => {
      switch (command) {
        case 'profile':
          router.push('/student/profile')
          break
        case 'logout':
          try {
            await ElMessageBox.confirm(
              '确定要退出登录吗？',
              '提示',
              {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              }
            )
            
            store.dispatch('logout')
            ElMessage.success('已退出登录')
            router.push('/login')
          } catch (error) {
            // 用户取消操作
          }
          break
      }
    }
    
    return {
      currentUser,
      userAvatar,
      userInitial,
      currentPageTitle,
      handleCommand
    }
  }
}
</script>

<style scoped>
.student-layout {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #304156;
  color: #bfcbd9;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  color: #fff;
}

.logo h2 {
  margin: 0;
  font-size: 16px;
  font-weight: normal;
}

.el-menu {
  border-right: none;
}

.el-header {
  background-color: #fff;
  color: #333;
  line-height: 60px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin: 0 8px;
  color: #606266;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
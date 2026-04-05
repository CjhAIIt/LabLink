<template>
  <div class="shell admin-shell">
    <aside class="sidebar" :class="{ 'is-collapsed': isCollapse }">
      <div class="sidebar-inner">
        <div class="sidebar-brand">
          <BrandLogo title="安徽信息工程学院" subtitle="实验室管理平台" tone="dark" size="sm" />
        </div>

        <el-menu :default-active="$route.fullPath" router class="sidebar-menu" :collapse="false">
          <template v-for="item in menuItems" :key="item.path">
            <el-sub-menu v-if="item.children" :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                <span>{{ child.label }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
    </aside>

    <div class="main-shell">
      <header class="topbar">
        <div class="topbar-left">
          <el-button class="collapse-btn" text @click="toggleCollapse">
            <el-icon :size="20">
              <component :is="isCollapse ? Expand : Fold" />
            </el-icon>
          </el-button>
          <div>
            <p class="topbar-label">管理端</p>
            <h2>{{ $route.meta.title || '工作台' }}</h2>
          </div>
        </div>
        <el-dropdown @command="handleCommand">
          <div class="user-chip">
            <el-avatar :size="34">{{ userInitial }}</el-avatar>
            <div>
              <strong>{{ userStore.realName || '管理员' }}</strong>
              <span>{{ roleLabel }}</span>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import {
  Bell,
  Calendar,
  DataBoard,
  DocumentChecked,
  DocumentCopy,
  Files,
  FolderOpened,
  OfficeBuilding,
  Search,
  TrendCharts,
  User,
  UserFilled,
  Expand,
  Fold
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import BrandLogo from '@/components/BrandLogo.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const isSchoolDirector = computed(() => Boolean(userStore.userInfo?.schoolDirector))
const isCollegeManager = computed(() => Boolean(userStore.userInfo?.collegeManager))
const isLabManager = computed(() => Boolean(userStore.userInfo?.labManager))

const roleLabel = computed(() => {
  if (isSchoolDirector.value) return '学校管理员'
  if (isCollegeManager.value) return '学院管理员'
  if (isLabManager.value) return '实验室管理员'
  return userStore.userRole === 'super_admin' ? '学校管理员' : '管理员'
})

const userInitial = computed(() => userStore.realName?.charAt(0) || 'A')

const menuItems = computed(() =>
  [
    { path: '/admin/dashboard', label: '工作台', icon: DataBoard },
    isSchoolDirector.value ? { path: '/admin/search', label: '综合检索', icon: Search } : null,
    isSchoolDirector.value ? { path: '/admin/colleges', label: '学院管理', icon: OfficeBuilding } : null,
    isSchoolDirector.value ? { path: '/admin/labs', label: '实验室管理', icon: FolderOpened } : null,
    isSchoolDirector.value || isCollegeManager.value ? { path: '/admin/create-applies', label: '实验室创建审批', icon: DocumentCopy } : null,
    isSchoolDirector.value || isCollegeManager.value
      ? { path: '/admin/teacher-register-applies', label: '教师注册审批', icon: DocumentChecked }
      : null,
    isSchoolDirector.value || isCollegeManager.value
      ? { path: '/admin/attendance-tasks', label: '考勤任务', icon: Calendar }
      : null,
    isSchoolDirector.value || isCollegeManager.value || isLabManager.value
      ? { path: '/admin/workspace', label: '实验室工作台', icon: Files }
      : null,
    isLabManager.value ? { path: '/admin/plans', label: '招新计划', icon: Calendar } : null,
    isLabManager.value ? { path: '/admin/applications', label: '成员申请审核', icon: UserFilled } : null,
    isLabManager.value ? { path: '/admin/members', label: '成员管理', icon: User } : null,
    isLabManager.value ? { path: '/admin/notices', label: '公告管理', icon: Bell } : null,
    isSchoolDirector.value || isLabManager.value
      ? {
          path: '/admin/statistics',
          label: '统计分析',
          icon: TrendCharts,
          children: [
            { path: '/admin/statistics?tab=overview', label: '运营概览' },
            { path: '/admin/statistics?tab=trends', label: '趋势分析' },
            { path: '/admin/statistics?tab=activity', label: '实时动态' }
          ]
        }
      : null,
    { path: '/admin/profile', label: '个人信息', icon: UserFilled }
  ].filter(Boolean)
)

const ensureProfile = async () => {
  if (userStore.userInfo?.id) return
  const response = await request.get('/api/access/profile')
  userStore.setUserInfo(response.data || {})
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push('/admin/profile')
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
    userStore.clearUserInfo()
    await router.push('/login')
  }
}

onMounted(() => {
  ensureProfile()
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  background-color: #ffffff;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: #f9f9f9;
  color: #0f172a;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1), transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid #e5e5e5;
}

.sidebar.is-collapsed {
  margin-left: -260px;
}

.sidebar-inner {
  width: 260px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.sidebar-brand {
  margin-bottom: 24px;
  padding: 0 8px;
}

.sidebar-menu {
  border: 0;
  background: transparent;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 8px;
  color: #333333;
  margin-bottom: 4px;
  font-size: 14px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: #ececec;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #000000;
  background: #ececec;
  font-weight: 600;
}

.main-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  color: #475569;
  padding: 8px;
}

.collapse-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
}

.topbar-label {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.topbar h2 {
  margin-top: 2px;
  margin-bottom: 0;
  color: #0f172a;
  font-size: 20px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.user-chip:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.user-chip span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.user-chip strong {
  font-size: 14px;
  color: #1e293b;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px 24px;
  background-color: #ffffff;
}

@media (max-width: 960px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    height: 100vh;
    border-right: 1px solid #e5e5e5;
  }

  .sidebar.is-collapsed {
    transform: translateX(-100%);
    opacity: 0;
    margin-left: 0;
  }

  .content {
    padding: 0 16px 24px;
  }

  .topbar {
    padding: 16px;
  }
}
</style>

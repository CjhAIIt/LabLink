<template>
  <div
    class="mobile-app-shell"
    :class="[`mobile-app-shell--${portalRole}`, { 'mobile-app-shell--fullscreen': fullScreen }]"
  >
    <MobileTopBar
      v-if="!fullScreen"
      :title="title"
      :subtitle="subtitle"
      :context-label="contextLabel"
      :can-go-back="canGoBack"
      :notification-path="notificationPath"
      :user-initial="userInitial"
      :menu-items="menuItems"
      :portal-role="portalRole"
      :menu-title="menuTitle"
      :menu-eyebrow="menuEyebrow"
      @back="router.back()"
      @command="handleCommand"
    />

    <main class="mobile-app-content" :class="{ 'mobile-app-content--fullscreen': fullScreen }">
      <slot />
    </main>

    <MobileBottomNav v-if="!fullScreen" :items="tabItems" :home-path="homePath" />
  </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MobileBottomNav from '@/components/mobile/MobileBottomNav.vue'
import MobileTopBar from '@/components/mobile/MobileTopBar.vue'
import { ensureAuthContext } from '@/utils/auth-context'
import { resolvePortalLogin, setPortalSurface } from '@/utils/portal'

const props = defineProps({
  portalRole: {
    type: String,
    default: 'student'
  },
  title: {
    type: String,
    default: 'LabLink'
  },
  subtitle: {
    type: String,
    default: '移动工作台'
  },
  contextLabel: {
    type: String,
    default: ''
  },
  homePath: {
    type: String,
    required: true
  },
  profilePath: {
    type: String,
    required: true
  },
  notificationPath: {
    type: String,
    required: true
  },
  tabItems: {
    type: Array,
    default: () => []
  },
  menuItems: {
    type: Array,
    default: () => []
  },
  fullScreen: {
    type: Boolean,
    default: false
  },
  forceAuthContext: {
    type: Boolean,
    default: false
  },
  menuTitle: {
    type: String,
    default: '全部功能'
  },
  menuEyebrow: {
    type: String,
    default: 'LabLink'
  }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const canGoBack = computed(() => route.path !== props.homePath)
const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || props.portalRole.charAt(0).toUpperCase())

const handleCommand = async (command) => {
  if (command === 'profile') {
    await router.push(props.profilePath)
    return
  }

  if (command === 'desktop') {
    setPortalSurface('desktop')
    await router.push('/login')
    return
  }

  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
    setPortalSurface('mobile')
    userStore.clearUserInfo()
    await router.push(resolvePortalLogin({ surface: 'mobile' }))
  }
}

onMounted(() => {
  setPortalSurface('mobile')
  ensureAuthContext(userStore, { force: props.forceAuthContext })
})
</script>

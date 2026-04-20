<template>
  <div class="mobile-auth-page login-theme">
    <div class="mobile-auth-shell">
      <header class="mobile-auth-header">
        <BrandLogo size="md" tone="dark" title="LabLink" subtitle="统一移动入口" />
        <router-link class="mobile-auth-desktop-link" :to="{ path: '/login', query: { surface: 'desktop' } }">
          桌面版
        </router-link>
      </header>

      <section class="mobile-auth-banner">
        <span class="mobile-auth-badge">统一登录</span>
        <h1>进入你的移动工作台</h1>
        <p>学生、教师、管理员共用同一入口，系统会根据身份自动跳转到对应的移动端工作台。</p>

        <div class="mobile-auth-pill-row">
          <span class="mobile-auth-pill">学生</span>
          <span class="mobile-auth-pill">教师</span>
          <span class="mobile-auth-pill">管理员</span>
          <span class="mobile-auth-pill">支持找回密码</span>
        </div>

        <div class="mobile-auth-summary">
          <div class="mobile-auth-summary-item">
            <span>登录账号</span>
            <strong>账号 / 学号 / 工号</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>登录结果</span>
            <strong>自动进入对应角色首页</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>首次使用</span>
            <strong>可直接从底部入口注册</strong>
          </div>
        </div>
      </section>

      <section class="mobile-auth-card">
        <div class="mobile-auth-card-head">
          <div>
            <h2>账号登录</h2>
            <p>请输入正确的账号和密码，登录后可直接继续处理消息、申请和审批。</p>
          </div>
        </div>

        <el-form
          ref="loginFormRef"
          class="mobile-auth-form"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          size="large"
        >
          <div class="mobile-auth-section">
            <div class="mobile-auth-section-head">
              <strong>登录信息</strong>
              <span>支持学生学号、教师工号和管理员账号统一登录。</span>
            </div>

            <el-form-item label="账号 / 学号 / 工号" prop="username">
              <el-input
                v-model="loginForm.username"
                :prefix-icon="User"
                placeholder="请输入账号、学号或工号"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                :prefix-icon="Lock"
                type="password"
                placeholder="请输入密码"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
          </div>

          <div class="mobile-auth-primary-actions">
            <div class="mobile-auth-inline-action">
              <button class="mobile-auth-text-button" type="button" @click="openForgotDialog">忘记密码？</button>
            </div>

            <el-button class="mobile-auth-submit" type="primary" :loading="loading" @click="handleLogin">
              登录移动端
            </el-button>
          </div>
        </el-form>
      </section>

      <footer class="mobile-auth-footer">
        <div class="mobile-auth-footer-links">
          <router-link class="mobile-auth-footer-link" to="/m/register">学生注册</router-link>
          <router-link class="mobile-auth-footer-link" to="/m/teacher-register">教师注册</router-link>
          <router-link class="mobile-auth-footer-link" to="/intro">平台介绍</router-link>
        </div>
        <div class="mobile-auth-note">
          首次使用建议先完成注册。若已注册但忘记密码，可直接通过上方“忘记密码”使用邮箱验证码重置。
        </div>
      </footer>
    </div>

    <el-dialog v-model="forgotDialogVisible" title="找回密码" width="460px" @closed="resetForgotForm">
      <p class="mobile-auth-dialog-tip">请输入账号和注册邮箱，校验通过后即可直接重置密码。</p>

      <el-form
        ref="forgotFormRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-position="top"
        size="default"
      >
        <el-form-item label="账号 / 学号 / 工号" prop="username">
          <el-input v-model="forgotForm.username" :prefix-icon="User" placeholder="请输入账号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="forgotForm.email" :prefix-icon="Message" placeholder="请输入注册邮箱" />
        </el-form-item>

        <el-form-item label="邮箱验证码" prop="code">
          <div class="mobile-auth-code-row">
            <el-input
              v-model="forgotForm.code"
              maxlength="6"
              placeholder="请输入 6 位验证码"
              @input="sanitizeResetCode"
            />
            <el-button
              type="primary"
              plain
              :loading="resetCodeLoading"
              :disabled="resetCodeCountdown > 0"
              @click="sendResetCode"
            >
              {{ resetCodeCountdown > 0 ? `${resetCodeCountdown}s 后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="请输入新密码"
            show-password
            @keyup.enter="handleResetPassword"
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="forgotForm.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            @keyup.enter="handleResetPassword"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="forgotDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">重置密码</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { login, resetPassword, sendPasswordResetCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { ensureAuthContext } from '@/utils/auth-context'
import { resolvePortalHome, setPortalSurface } from '@/utils/portal'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref()
const loading = ref(false)
const forgotDialogVisible = ref(false)
const forgotFormRef = ref()
const resetCodeLoading = ref(false)
const resetLoading = ref(false)
const resetCodeCountdown = ref(0)

let resetCodeTimer = null

const loginForm = reactive({
  username: '',
  password: ''
})

const forgotForm = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateResetConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const forgotRules = reactive({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度需在 6 到 20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateResetConfirmPassword, trigger: 'blur' }]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const normalizedForgotEmail = () => forgotForm.email.trim().toLowerCase()

const sanitizeResetCode = (value) => {
  forgotForm.code = String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 6)
}

const startResetCodeCountdown = () => {
  clearInterval(resetCodeTimer)
  resetCodeCountdown.value = 60
  resetCodeTimer = window.setInterval(() => {
    if (resetCodeCountdown.value <= 1) {
      clearInterval(resetCodeTimer)
      resetCodeTimer = null
      resetCodeCountdown.value = 0
      return
    }
    resetCodeCountdown.value -= 1
  }, 1000)
}

const resetForgotForm = () => {
  forgotForm.username = loginForm.username.trim()
  forgotForm.email = ''
  forgotForm.code = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  clearInterval(resetCodeTimer)
  resetCodeTimer = null
  resetCodeCountdown.value = 0
  forgotFormRef.value?.clearValidate()
}

const openForgotDialog = () => {
  forgotDialogVisible.value = true
  if (!forgotForm.username) {
    forgotForm.username = loginForm.username.trim()
  }
}

const sendResetCode = async () => {
  const username = loginForm.username.trim() || forgotForm.username.trim()
  const email = normalizedForgotEmail()

  if (!username) {
    ElMessage.error('请先输入账号')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.error('请先输入正确的邮箱')
    return
  }

  forgotForm.username = username
  resetCodeLoading.value = true
  try {
    await sendPasswordResetCode({
      username,
      email
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startResetCodeCountdown()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '验证码发送失败，请稍后重试'))
  } finally {
    resetCodeLoading.value = false
  }
}

const handleResetPassword = async () => {
  if (!forgotFormRef.value) {
    return
  }

  try {
    await forgotFormRef.value.validate()
  } catch {
    return
  }

  resetLoading.value = true
  try {
    const username = forgotForm.username.trim()
    await resetPassword({
      username,
      email: normalizedForgotEmail(),
      code: forgotForm.code,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    loginForm.username = username
    loginForm.password = ''
    forgotDialogVisible.value = false
    resetForgotForm()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '密码重置失败，请稍后重试'))
  } finally {
    resetLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) {
    return
  }

  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const response = await login(loginForm)
    const userData = response.data
    setPortalSurface('mobile')
    userStore.setToken(userData.token)
    userStore.setUserInfo(userData)
    await ensureAuthContext(userStore, { force: true, fallbackUserInfo: userData })
    ElMessage.success('登录成功')
    await router.push(resolvePortalHome(userStore.userInfo || userData, { surface: 'mobile' }))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '登录失败，请检查账号和密码'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setPortalSurface('mobile')
})

onBeforeUnmount(() => {
  clearInterval(resetCodeTimer)
})
</script>

<style scoped>
.login-theme {
  --mobile-auth-start: #0f172a;
  --mobile-auth-end: #2563eb;
  --mobile-auth-bg-top: #edf4ff;
  --mobile-auth-bg-mid: #f6f8fd;
  --mobile-auth-glow: rgba(59, 130, 246, 0.18);
  --mobile-auth-glow-soft: rgba(34, 211, 238, 0.1);
  --mobile-auth-accent: #2563eb;
  --mobile-auth-accent-dark: #1d4ed8;
  --mobile-auth-accent-light: #60a5fa;
  --mobile-auth-accent-soft-light: #93c5fd;
  --mobile-auth-accent-soft: rgba(37, 99, 235, 0.1);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

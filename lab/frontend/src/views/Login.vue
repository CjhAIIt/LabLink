<template>
  <AuthLayout
    title="账号登录"
    subtitle="请输入管理员账号、学生学号或教师工号登录平台"
    :features="features"
  >
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      size="large"
    >
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

      <div class="aux-links">
        <button class="link-button" type="button" @click="openForgotDialog">忘记密码？</button>
      </div>

      <el-form-item class="form-actions">
        <el-button type="primary" :loading="loading" class="submit-button" @click="handleLogin">
          登录
        </el-button>
      </el-form-item>

      <el-form-item>
        <div class="register-link">
          <router-link to="/register">学生注册</router-link>
          <span class="divider">|</span>
          <router-link to="/teacher-register">教师注册</router-link>
        </div>
      </el-form-item>
    </el-form>
  </AuthLayout>

  <el-dialog v-model="forgotDialogVisible" title="找回密码" width="min(460px, 92vw)">
    <p class="dialog-tip">请输入账号和注册邮箱，校验通过后即可重置密码。</p>

    <el-form
      ref="forgotFormRef"
      :model="forgotForm"
      :rules="forgotRules"
      label-position="top"
    >
      <el-form-item label="账号" prop="username">
        <el-input
          v-model="forgotForm.username"
          :prefix-icon="User"
          placeholder="请输入账号、学号或工号"
        />
      </el-form-item>

      <el-form-item label="注册邮箱" prop="email">
        <el-input
          v-model="forgotForm.email"
          :prefix-icon="Message"
          placeholder="请输入注册时使用的邮箱"
        />
      </el-form-item>

      <el-form-item label="邮箱验证码" prop="code">
        <div class="code-row">
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
        <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">
          重置密码
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import {
  getUserInfo as fetchCurrentUserApi,
  login,
  resetPassword,
  sendPasswordResetCode
} from '@/api/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useUserStore } from '@/stores/user'
import { resolvePortalHome } from '@/utils/portal'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginFormRef = ref()

const features = [
  { label: '学生入口', text: '注册后可浏览实验室、提交入组申请并跟踪审核结果' },
  { label: '教师入口', text: '通过注册审批后可发起实验室创建申请并查看流程状态' },
  { label: '管理侧', text: '支持学院初审、学校终审、统一检索与统计分析' }
]

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

const loginRules = reactive({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

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
    userStore.setToken(userData.token)

    let mergedUserInfo = userData
    try {
      const profileResponse = await fetchCurrentUserApi()
      mergedUserInfo = {
        ...userData,
        ...(profileResponse.data || {})
      }
    } catch {
      mergedUserInfo = userData
    }

    userStore.setUserInfo(mergedUserInfo)
    ElMessage.success('登录成功')
    await router.push(resolvePortalHome(mergedUserInfo))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '登录失败，请检查账号和密码'))
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  clearInterval(resetCodeTimer)
})
</script>

<style scoped>
.aux-links {
  display: flex;
  justify-content: flex-end;
  margin: calc(var(--ll-space-2) * -1) 0 var(--ll-space-4);
}

.link-button {
  border: none;
  background: transparent;
  padding: 0;
  font-size: var(--ll-text-sm);
  color: var(--ll-text-secondary);
  transition: color var(--ll-transition-fast);
}

.link-button:hover {
  color: var(--ll-text-brand);
}

.form-actions {
  margin-bottom: var(--ll-space-2);
}

.submit-button {
  width: 100%;
  height: 38px;
  font-size: var(--ll-text-base);
  font-weight: 500;
}

.register-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--ll-space-2);
  font-size: var(--ll-text-sm);
}

.register-link .divider {
  color: var(--ll-border-default);
}

.code-row {
  display: flex;
  gap: var(--ll-space-2);
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-row .el-button {
  flex-shrink: 0;
}

.dialog-tip {
  margin-bottom: var(--ll-space-4);
  font-size: var(--ll-text-sm);
  line-height: 1.6;
  color: var(--ll-text-secondary);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--ll-space-2);
}
</style>

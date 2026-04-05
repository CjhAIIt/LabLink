<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <BrandLogo
          class="brand"
          size="lg"
          title="安徽信息工程学院"
          subtitle="实验室管理平台"
        />

        <div class="hero-card">
          <h2>面向全校的实验室管理与协同平台</h2>
          <p>
            平台覆盖学校、学院、实验室、教师与学生等多角色场景，支持实验室信息管理、
            教师注册审批、实验室创建申请、成员申请、公告发布和统计分析。
          </p>
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hero-label">学生入口</span>
              <strong>学生注册后可浏览实验室、提交申请并跟踪审核结果</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">教师入口</span>
              <strong>教师通过注册审批后可发起实验室创建申请并查看流程状态</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">管理侧</span>
              <strong>支持学院初审、学校终审、统一检索与统计分析</strong>
            </div>
          </div>
        </div>

        <div class="hero-footer">
          <router-link class="ghost-link" to="/intro">查看平台介绍</router-link>
        </div>
      </section>

      <section class="auth-panel">
        <el-card class="auth-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div>
                <h2>账号登录</h2>
                <p>请输入管理员账号、学生学号或教师工号登录平台</p>
              </div>
            </div>
          </template>

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
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
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
        </el-card>
      </section>
    </div>

    <el-dialog v-model="forgotDialogVisible" title="找回密码" width="460px">
      <p class="dialog-tip">请输入账号和注册邮箱，校验通过后即可重置密码。</p>

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
  </div>
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
import BrandLogo from '@/components/BrandLogo.vue'
import { useUserStore } from '@/stores/user'
import { resolvePortalHome } from '@/utils/portal'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginFormRef = ref()

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
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: radial-gradient(circle at top left, #e8f0ff 0%, #f6f7fb 45%, #f7f0ff 100%);
}

.auth-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 28px;
  align-items: stretch;
}

.auth-hero {
  position: relative;
  padding: 32px;
  border-radius: 24px;
  color: #0f1b36;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(243, 247, 255, 0.9));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

.auth-hero::before {
  content: '';
  position: absolute;
  width: 220px;
  height: 220px;
  right: -80px;
  top: -80px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.25), transparent 70%);
}

.brand {
  margin-bottom: 24px;
}

.hero-card {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  padding: 22px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.hero-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.hero-card p {
  margin: 0 0 16px;
  color: #475569;
  line-height: 1.6;
}

.hero-grid {
  display: grid;
  gap: 12px;
}

.hero-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.hero-label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.hero-footer {
  margin-top: 18px;
}

.ghost-link {
  color: #1d4ed8;
  text-decoration: none;
  font-weight: 600;
}

.ghost-link:hover {
  text-decoration: underline;
}

.auth-panel {
  display: flex;
  align-items: center;
}

.auth-card {
  width: 100%;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.card-header h2 {
  margin: 0 0 6px;
  color: #0f172a;
}

.card-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.aux-links {
  display: flex;
  justify-content: flex-end;
  margin-top: -6px;
  margin-bottom: 12px;
}

.link-button {
  border: 0;
  background: transparent;
  padding: 0;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.link-button:hover {
  text-decoration: underline;
}

.form-actions {
  margin-top: 6px;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.register-link a {
  color: #1d4ed8;
  text-decoration: none;
  font-weight: 600;
}

.register-link a:hover {
  text-decoration: underline;
}

.divider {
  color: #cbd5e1;
}

.dialog-tip {
  margin: 0 0 16px;
  color: #64748b;
  line-height: 1.6;
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .auth-hero {
    padding: 24px;
  }

  .auth-panel {
    padding-bottom: 12px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }

  .register-link {
    flex-direction: column;
    gap: 6px;
  }
}
</style>

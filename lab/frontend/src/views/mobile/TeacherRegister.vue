<template>
  <div class="mobile-auth-page teacher-theme">
    <div class="mobile-auth-shell">
      <header class="mobile-auth-header">
        <BrandLogo size="md" tone="dark" title="LabLink" subtitle="教师注册 · 移动端" />
        <router-link
          class="mobile-auth-desktop-link"
          :to="{ path: '/teacher-register', query: { surface: 'desktop' } }"
        >
          桌面版
        </router-link>
      </header>

      <section class="mobile-auth-banner">
        <span class="mobile-auth-badge">教师注册申请</span>
        <h1>提交教师账号开通申请</h1>
        <p>填写工号、所属学院、联系方式和申请说明后进入审批流程，通过后即可登录教师移动端。</p>

        <div class="mobile-auth-pill-row">
          <span class="mobile-auth-pill">学院初审</span>
          <span class="mobile-auth-pill">学校终审</span>
          <span class="mobile-auth-pill">状态可追踪</span>
        </div>

        <div class="mobile-auth-summary">
          <div class="mobile-auth-summary-item">
            <span>账号主体</span>
            <strong>工号作为身份标识</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>申请材料</span>
            <strong>学院、联系方式</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>审批通过后</span>
            <strong>直接登录教师工作台</strong>
          </div>
        </div>
      </section>

      <section class="mobile-auth-card">
        <div class="mobile-auth-card-head">
          <div>
            <h2>教师注册申请</h2>
            <p>请填写真实资料。提交后会进入审批，审批通过前无法使用该账号登录。</p>
          </div>
        </div>

        <el-form
          ref="registerFormRef"
          class="mobile-auth-form"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          size="large"
        >
          <div class="mobile-auth-section">
            <div class="mobile-auth-section-head">
              <strong>账号信息</strong>
              <span>工号将作为后续登录的重要标识，请按学校实际工号填写。</span>
            </div>

            <el-form-item label="工号" prop="teacherNo">
              <el-input
                v-model="registerForm.teacherNo"
                :prefix-icon="Postcard"
                maxlength="32"
                placeholder="请输入工号，可包含字母、数字、下划线和中划线"
                @input="sanitizeTeacherNo"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                :prefix-icon="Lock"
                type="password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                :prefix-icon="Lock"
                type="password"
                placeholder="请再次输入密码"
                show-password
              />
            </el-form-item>
          </div>

          <div class="mobile-auth-section">
            <div class="mobile-auth-section-head">
              <strong>身份资料</strong>
              <span>这些信息将用于审批流转、教师资料展示和后续工作台权限确认。</span>
            </div>

            <el-form-item label="真实姓名" prop="realName">
              <el-input
                v-model="registerForm.realName"
                :prefix-icon="UserFilled"
                placeholder="请输入真实姓名"
              />
            </el-form-item>

            <el-form-item label="所属学院" prop="collegeId">
              <el-select
                v-model="registerForm.collegeId"
                placeholder="请选择所属学院"
                filterable
                :loading="collegeLoading"
              >
                <el-option
                  v-for="college in collegeOptions"
                  :key="college.id"
                  :label="college.collegeName"
                  :value="college.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="职称" prop="title">
              <el-input v-model="registerForm.title" placeholder="如讲师、副教授、实验师，可选填" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号，可选填" maxlength="11" />
            </el-form-item>
          </div>

          <div class="mobile-auth-section">
            <div class="mobile-auth-section-head">
              <strong>验证与说明</strong>
              <span>请补充简要申请说明，方便学院和学校审核。</span>
            </div>

            <el-form-item label="申请说明" prop="applyReason">
              <el-input
                v-model="registerForm.applyReason"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请简要说明申请注册教师账号及后续实验室建设计划"
              />
            </el-form-item>
          </div>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="registerForm.email"
              :prefix-icon="Message"
              placeholder="请输入常用邮箱"
            />
          </el-form-item>

          <el-form-item label="邮箱验证码" prop="emailCode">
            <el-input
              v-model="registerForm.emailCode"
              maxlength="6"
              placeholder="请输入 6 位邮箱验证码"
              @input="sanitizeEmailCode"
            >
              <template #append>
                <el-button
                  :loading="sendCodeLoading"
                  :disabled="sendCodeLoading || codeCountdown > 0 || !registerForm.teacherNo || !emailPattern.test(normalizedEmail())"
                  @click="sendCode"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>

          <div class="mobile-auth-primary-actions">
            <el-button class="mobile-auth-submit" type="primary" :loading="loading" @click="submitForm">
              提交申请
            </el-button>
          </div>
        </el-form>
      </section>

      <footer class="mobile-auth-footer">
        <div class="mobile-auth-footer-links">
          <router-link class="mobile-auth-footer-link" to="/m/login">返回登录</router-link>
          <router-link class="mobile-auth-footer-link" to="/m/register">学生注册</router-link>
        </div>
        <div class="mobile-auth-note">
          审批通过后即可用工号和密码登录移动端教师工作台，继续发起实验室创建申请并处理相关通知。
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Postcard, UserFilled } from '@element-plus/icons-vue'
import { getCollegeOptions } from '@/api/colleges'
import { registerTeacher, sendTeacherRegisterCode } from '@/api/auth'
import BrandLogo from '@/components/BrandLogo.vue'
import { setPortalSurface } from '@/utils/portal'

const router = useRouter()
const loading = ref(false)
const collegeLoading = ref(false)
const registerFormRef = ref()
const collegeOptions = ref([])
const codeCountdown = ref(0)
const sendCodeLoading = ref(false)
let registerCodeTimer = null

const registerForm = reactive({
  teacherNo: '',
  password: '',
  confirmPassword: '',
  realName: '',
  collegeId: undefined,
  title: '',
  phone: '',
  email: '',
  emailCode: '',
  applyReason: ''
})

const chineseRealNamePattern = /^[\u4e00-\u9fff·]{2,50}$/
const teacherNoPattern = /^[A-Za-z0-9_-]{3,32}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const sanitizeTeacherNo = (value) => {
  registerForm.teacherNo = String(value ?? '')
    .replace(/[^A-Za-z0-9_-]/g, '')
    .slice(0, 32)
    .toUpperCase()
}

const sanitizeEmailCode = (value) => {
  registerForm.emailCode = String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 6)
}

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }

  if (registerForm.confirmPassword) {
    registerFormRef.value?.validateField('confirmPassword')
  }
  callback()
}

const validatePass2 = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const validateTeacherNo = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入工号'))
    return
  }
  if (!teacherNoPattern.test(value)) {
    callback(new Error('工号需为 3 到 32 位字母、数字、下划线或中划线'))
    return
  }
  callback()
}

const validateRealName = (rule, value, callback) => {
  const normalized = String(value ?? '').trim()
  if (!normalized) {
    callback(new Error('请输入真实姓名'))
    return
  }
  if (!chineseRealNamePattern.test(normalized)) {
    callback(new Error('真实姓名必须为中文'))
    return
  }
  callback()
}

const normalizedEmail = () => registerForm.email.trim().toLowerCase()

const validateEmail = (rule, value, callback) => {
  const normalized = String(value ?? '').trim().toLowerCase()
  if (!normalized) {
    callback(new Error('请输入邮箱'))
    return
  }
  if (!emailPattern.test(normalized)) {
    callback(new Error('请输入正确的邮箱格式'))
    return
  }
  callback()
}

const validateEmailCode = (rule, value, callback) => {
  const normalized = String(value ?? '').trim()
  if (!normalized) {
    callback(new Error('请输入邮箱验证码'))
    return
  }
  if (!/^\d{6}$/.test(normalized)) {
    callback(new Error('请输入 6 位数字验证码'))
    return
  }
  callback()
}

const validatePhone = (rule, value, callback) => {
  const normalized = String(value ?? '').trim()
  if (!normalized) {
    callback()
    return
  }
  if (!/^1[3-9]\d{9}$/.test(normalized)) {
    callback(new Error('请输入正确的手机号'))
    return
  }
  callback()
}

const registerRules = reactive({
  teacherNo: [{ required: true, validator: validateTeacherNo, trigger: 'blur' }],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
  realName: [{ required: true, validator: validateRealName, trigger: 'blur' }],
  email: [{ required: true, validator: validateEmail, trigger: 'blur' }],
  emailCode: [{ required: true, validator: validateEmailCode, trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  applyReason: [
    { required: true, message: '请输入申请说明', trigger: 'blur' },
    { min: 10, max: 500, message: '申请说明需在 10 到 500 个字符之间', trigger: 'blur' }
  ]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const normalizedTeacherNo = () => registerForm.teacherNo.trim().toUpperCase()

const startCodeCountdown = () => {
  clearInterval(registerCodeTimer)
  codeCountdown.value = 60
  registerCodeTimer = window.setInterval(() => {
    if (codeCountdown.value <= 1) {
      clearInterval(registerCodeTimer)
      registerCodeTimer = null
      codeCountdown.value = 0
      return
    }
    codeCountdown.value -= 1
  }, 1000)
}

const sendCode = async () => {
  if (!registerFormRef.value) {
    return
  }

  try {
    await registerFormRef.value.validateField(['teacherNo', 'email'])
  } catch {
    return
  }

  sendCodeLoading.value = true
  try {
    await sendTeacherRegisterCode({
      teacherNo: normalizedTeacherNo(),
      email: normalizedEmail()
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCodeCountdown()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '验证码发送失败，请稍后重试'))
  } finally {
    sendCodeLoading.value = false
  }
}

const loadColleges = async () => {
  collegeLoading.value = true
  try {
    const response = await getCollegeOptions()
    collegeOptions.value = response.data || []
  } finally {
    collegeLoading.value = false
  }
}

const submitForm = async () => {
  if (!registerFormRef.value) {
    return
  }

  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await registerTeacher({
      ...registerForm,
      teacherNo: normalizedTeacherNo(),
      realName: registerForm.realName.trim(),
      title: registerForm.title.trim(),
      phone: registerForm.phone.trim(),
      email: normalizedEmail(),
      applyReason: registerForm.applyReason.trim()
    })
    ElMessage.success('教师注册申请已提交，请等待学院和学校审批')
    await router.push('/m/login')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '教师注册申请提交失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setPortalSurface('mobile')
  loadColleges()
})

onBeforeUnmount(() => {
  clearInterval(registerCodeTimer)
})
</script>

<style scoped>
.teacher-theme {
  --mobile-auth-start: #1f2937;
  --mobile-auth-end: #d97706;
  --mobile-auth-bg-top: #fff5ea;
  --mobile-auth-bg-mid: #fffaf5;
  --mobile-auth-glow: rgba(249, 115, 22, 0.14);
  --mobile-auth-glow-soft: rgba(251, 191, 36, 0.1);
  --mobile-auth-accent: #d97706;
  --mobile-auth-accent-dark: #b45309;
  --mobile-auth-accent-light: #f59e0b;
  --mobile-auth-accent-soft-light: #fbbf24;
  --mobile-auth-accent-soft: rgba(217, 119, 6, 0.12);
}
</style>

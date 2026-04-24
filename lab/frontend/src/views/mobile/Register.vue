<template>
  <div class="mobile-auth-page">
    <div class="mobile-auth-shell">
      <header class="mobile-auth-header">
        <BrandLogo size="md" tone="dark" title="LabLink" subtitle="学生注册 · 移动端" />
        <router-link class="mobile-auth-desktop-link" :to="{ path: '/register', query: { surface: 'desktop' } }">
          桌面版
        </router-link>
      </header>

      <section class="mobile-auth-banner">
        <span class="mobile-auth-badge">学生注册</span>
        <h1>使用学号完成账号开通</h1>
        <p>请填写真实姓名和有效邮箱，注册成功后即可在移动端查看实验室信息与申请进度。</p>
      </section>

      <section class="mobile-auth-card">
        <div class="mobile-auth-card-head">
          <div>
            <h2>注册信息</h2>
            <p>邮箱用于接收验证码和后续通知</p>
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
          <el-form-item label="学号" prop="studentId">
            <el-input
              v-model="registerForm.studentId"
              :prefix-icon="Postcard"
              maxlength="14"
              placeholder="请输入3开头的8到14位纯数字学号"
              @input="sanitizeStudentId"
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

          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model="registerForm.realName"
              :prefix-icon="UserFilled"
              placeholder="请输入真实姓名"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="registerForm.email"
              :prefix-icon="Message"
              placeholder="请输入常用邮箱"
            />
          </el-form-item>

          <el-form-item label="所属学院" prop="college">
            <el-select
              v-model="registerForm.college"
              placeholder="请选择所属学院"
              filterable
              :loading="collegeLoading"
            >
              <el-option
                v-for="college in collegeOptions"
                :key="college.id"
                :label="college.collegeName"
                :value="college.collegeName"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="专业" prop="major">
            <el-select v-model="registerForm.major" placeholder="请选择专业" filterable>
              <el-option v-for="major in majorOptions" :key="major" :label="major" :value="major" />
            </el-select>
          </el-form-item>

          <el-form-item label="年级" prop="grade">
            <el-select v-model="registerForm.grade" placeholder="请选择年级">
              <el-option v-for="year in yearOptions" :key="year" :label="`${year}级`" :value="`${year}级`" />
            </el-select>
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
                  :disabled="sendCodeLoading || codeCountdown > 0 || !registerForm.studentId || !emailPattern.test(normalizedEmail())"
                  @click="sendCode"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>

          <div class="mobile-auth-primary-actions">
            <el-button class="mobile-auth-submit" type="primary" :loading="loading" @click="submitForm">
              完成注册
            </el-button>
          </div>
        </el-form>
      </section>

      <footer class="mobile-auth-footer">
        <div class="mobile-auth-footer-links">
          <router-link class="mobile-auth-footer-link" to="/m/login">返回登录</router-link>
          <router-link class="mobile-auth-footer-link" to="/m/teacher-register">教师注册</router-link>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Postcard, UserFilled } from '@element-plus/icons-vue'
import { register, sendRegisterCode } from '@/api/auth'
import { getCollegeOptions } from '@/api/colleges'
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

const majorOptions = [
  '计算机科学与技术',
  '软件工程',
  '网络工程',
  '数据科学与大数据技术',
  '人工智能',
  '数字媒体技术',
  '电子信息工程',
  '通信工程',
  '电气工程及其自动化',
  '自动化',
  '机器人工程',
  '机械设计制造及其自动化',
  '市场营销',
  '财务管理',
  '供应链管理',
  '英语'
]

const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let year = currentYear; year >= currentYear - 6; year -= 1) {
    years.push(year)
  }
  return years
})

const registerForm = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  studentId: '',
  email: '',
  emailCode: '',
  college: '',
  major: '',
  grade: '',
  role: 'student'
})

const chineseRealNamePattern = /^[\u4e00-\u9fff·]{2,5}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const deriveGradeFromStudentId = (studentId) => {
  const value = String(studentId || '').trim()
  if (!/^3\d{7,13}$/.test(value)) {
    return ''
  }
  const gradeCode = Number(value.slice(1, 3))
  if (gradeCode < 20 || gradeCode > 40) {
    return ''
  }
  return `20${value.slice(1, 3)}级`
}

const sanitizeStudentId = (value) => {
  registerForm.studentId = String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 14)
  const grade = deriveGradeFromStudentId(registerForm.studentId)
  if (grade) {
    registerForm.grade = grade
  }
}

const sanitizeEmailCode = (value) => {
  registerForm.emailCode = String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 6)
}

const normalizedEmail = () => registerForm.email.trim().toLowerCase()

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

const validateStudentId = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入学号'))
    return
  }
  if (!/^3\d{7,13}$/.test(value)) {
    callback(new Error('学号必须为3开头的8到14位纯数字'))
    return
  }
  if (!deriveGradeFromStudentId(value)) {
    callback(new Error('学号第2~3位需为有效年级，如23表示2023级'))
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
    callback(new Error('真实姓名需为2到5个中文字符，不能全是空格'))
    return
  }
  callback()
}

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

const registerRules = reactive({
  studentId: [{ required: true, validator: validateStudentId, trigger: 'blur' }],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
  realName: [{ required: true, validator: validateRealName, trigger: 'blur' }],
  email: [{ required: true, validator: validateEmail, trigger: 'blur' }],
  emailCode: [{ required: true, validator: validateEmailCode, trigger: 'blur' }],
  college: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

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
    await registerFormRef.value.validateField(['studentId', 'email'])
  } catch {
    return
  }

  sendCodeLoading.value = true
  try {
    await sendRegisterCode({
      studentId: registerForm.studentId.trim(),
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
    const studentId = registerForm.studentId.trim()
    await register({
      ...registerForm,
      realName: registerForm.realName.trim(),
      studentId,
      grade: deriveGradeFromStudentId(studentId) || registerForm.grade,
      email: normalizedEmail(),
      username: studentId
    })
    ElMessage.success('注册成功，请登录')
    await router.push('/m/login')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '注册失败，请稍后重试'))
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
.mobile-auth-page {
  min-height: 100vh;
  padding: 20px 14px 28px;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.16), transparent 34%),
    linear-gradient(180deg, #eef4ff 0%, #f8fbff 42%, #f2f7ff 100%);
}

.mobile-auth-shell {
  width: min(100%, 560px);
  margin: 0 auto;
}

.mobile-auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.mobile-auth-desktop-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}

.mobile-auth-banner {
  margin-bottom: 16px;
  padding: 20px;
  border-radius: 24px;
  color: #0f172a;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(239, 246, 255, 0.92));
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.mobile-auth-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.mobile-auth-banner h1 {
  margin: 14px 0 8px;
  font-size: 24px;
  line-height: 1.2;
}

.mobile-auth-banner p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
  font-size: 14px;
}

.mobile-auth-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.mobile-auth-card-head {
  margin-bottom: 12px;
}

.mobile-auth-card-head h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.mobile-auth-card-head p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.mobile-auth-form :deep(.el-select) {
  width: 100%;
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.mobile-auth-primary-actions {
  margin-top: 10px;
}

.mobile-auth-submit {
  width: 100%;
  height: 46px;
}

.mobile-auth-footer {
  margin-top: 16px;
}

.mobile-auth-footer-links {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.mobile-auth-footer-link {
  color: #0f766e;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}

@media (max-width: 480px) {
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>

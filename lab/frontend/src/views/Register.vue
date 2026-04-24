<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <BrandLogo
          class="brand"
          size="lg"
          title="LabLink"
          subtitle="高校实验室管理平台 · 学生注册"
        />

        <div class="hero-card">
          <h2>学生注册后可直接进入实验室信息管理场景</h2>
          <p>
            完成注册后，学生可以浏览实验室信息、查看招新计划、提交申请，并持续跟踪审核结果。
          </p>
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hero-label">注册对象</span>
              <strong>在校学生</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">必填信息</span>
              <strong>学号、真实姓名、邮箱、学院、专业、年级</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">教师入口</span>
              <strong>教师请使用独立注册入口并走审批流程</strong>
            </div>
          </div>
        </div>

        <div class="hero-footer">
          <router-link class="ghost-link" to="/teacher-register">前往教师注册</router-link>
        </div>
      </section>

      <section class="auth-panel">
        <el-card class="auth-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div>
                <h2>学生注册</h2>
                <p>请使用学号和本人真实信息完成注册</p>
              </div>
            </div>
          </template>

          <el-form
            ref="registerFormRef"
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
                style="width: 100%"
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
              <el-select v-model="registerForm.major" placeholder="请选择专业" style="width: 100%" filterable>
                <el-option v-for="major in majorOptions" :key="major" :label="major" :value="major" />
              </el-select>
            </el-form-item>

            <el-form-item label="年级" prop="grade">
              <el-select v-model="registerForm.grade" placeholder="请选择年级" style="width: 100%">
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

            <el-form-item class="form-actions">
              <el-button type="primary" :loading="loading" style="width: 100%" @click="submitForm">
                立即注册
              </el-button>
            </el-form-item>

            <el-form-item>
              <div class="login-link">
                <router-link to="/login">返回登录</router-link>
                <span class="divider">|</span>
                <router-link to="/teacher-register">教师注册</router-link>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </section>
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
    await router.push('/login')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '注册失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadColleges()
})

onBeforeUnmount(() => {
  clearInterval(registerCodeTimer)
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 32px 16px;
  background: radial-gradient(circle at top right, #e8f0ff 0%, #f6f7fb 45%, #f2f8ff 100%);
}

.auth-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 28px;
  align-items: start;
}

.auth-hero {
  position: sticky;
  top: 32px;
  padding: 32px;
  border-radius: 24px;
  color: #0f1b36;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95), rgba(240, 249, 255, 0.95));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

.auth-hero::before {
  content: '';
  position: absolute;
  width: 220px;
  height: 220px;
  left: -80px;
  top: -80px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.25), transparent 70%);
}

.brand {
  margin-bottom: 24px;
}

.hero-card {
  background: rgba(255, 255, 255, 0.72);
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
  color: #0f766e;
  text-decoration: none;
  font-weight: 600;
}

.ghost-link:hover {
  text-decoration: underline;
}

.auth-panel {
  display: flex;
  align-items: flex-start;
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

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.form-actions {
  margin-top: 6px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.login-link a {
  color: #0f766e;
  text-decoration: none;
  font-weight: 600;
}

.login-link a:hover {
  text-decoration: underline;
}

.divider {
  color: #cbd5e1;
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

  .login-link {
    flex-direction: column;
    gap: 6px;
  }
}
</style>

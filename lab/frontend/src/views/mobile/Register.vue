<template>
  <div class="mobile-auth-page student-theme">
    <div class="mobile-auth-shell">
      <header class="mobile-auth-header">
        <BrandLogo size="md" tone="dark" title="LabLink" subtitle="学生注册 · 移动端" />
        <router-link class="mobile-auth-desktop-link" :to="{ path: '/register', query: { surface: 'desktop' } }">
          桌面版
        </router-link>
      </header>

      <section class="mobile-auth-banner">
        <span class="mobile-auth-badge">学生注册</span>
        <h1>用学号完成账号开通</h1>
        <p>注册后可直接在手机端浏览实验室、投递申请、查看通知和处理日常信息。</p>

        <div class="mobile-auth-pill-row">
          <span class="mobile-auth-pill">学号作为账号</span>
          <span class="mobile-auth-pill">实名注册</span>
        </div>

        <div class="mobile-auth-summary">
          <div class="mobile-auth-summary-item">
            <span>账号主体</span>
            <strong>学号即登录账号</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>必填信息</span>
            <strong>学院、专业、年级</strong>
          </div>
          <div class="mobile-auth-summary-item">
            <span>注册完成后</span>
            <strong>直接回到移动端登录页</strong>
          </div>
        </div>
      </section>

      <section class="mobile-auth-card">
        <div class="mobile-auth-card-head">
          <div>
            <h2>学生注册</h2>
            <p>请确保姓名、学号和邮箱均为真实有效信息，便于后续审核与联系。</p>
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
              <span>用于后续登录，请设置可长期记忆的密码。</span>
            </div>

            <el-form-item label="学号" prop="studentId">
              <el-input
                v-model="registerForm.studentId"
                :prefix-icon="Postcard"
                maxlength="20"
                placeholder="请输入纯数字学号"
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
          </div>

          <div class="mobile-auth-section">
            <div class="mobile-auth-section-head">
              <strong>个人资料</strong>
              <span>这些信息会用于学院维度统计、实验室申请和个人资料展示。</span>
            </div>

            <el-form-item label="真实姓名" prop="realName">
              <el-input
                v-model="registerForm.realName"
                :prefix-icon="UserFilled"
                placeholder="请输入真实姓名"
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
          </div>

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
        <div class="mobile-auth-note">
          注册成功后可直接使用学号登录移动端，实验室浏览、消息通知和申请进度都会在手机端持续同步。
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Postcard, UserFilled } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { getCollegeOptions } from '@/api/colleges'
import BrandLogo from '@/components/BrandLogo.vue'
import { setPortalSurface } from '@/utils/portal'

const router = useRouter()
const loading = ref(false)
const collegeLoading = ref(false)
const registerFormRef = ref()
const collegeOptions = ref([])

let codeTimer = null

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
  college: '',
  major: '',
  grade: '',
  role: 'student'
})

const chineseRealNamePattern = /^[\u4e00-\u9fff·]{2,50}$/

const sanitizeStudentId = (value) => {
  registerForm.studentId = String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 20)
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

const validateStudentId = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入学号'))
    return
  }
  if (!/^\d{3,20}$/.test(value)) {
    callback(new Error('学号必须为 3 到 20 位纯数字'))
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

const registerRules = reactive({
  studentId: [{ required: true, validator: validateStudentId, trigger: 'blur' }],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
  realName: [{ required: true, validator: validateRealName, trigger: 'blur' }],
  college: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const loadColleges = async () => {
  collegeLoading.value = true
  try {
    const response = await getCollegeOptions()
    collegeOptions.value = response.data || []
  } finally {
    collegeLoading.value = false
  }
}

const sendEmailCode = async () => {
  const studentId = registerForm.studentId.trim()
  const email = normalizedEmail()

  if (!/^\d{3,20}$/.test(studentId)) {
    ElMessage.error('请先输入正确的学号')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.error('请先输入正确的邮箱')
    return
  }

  codeLoading.value = true
  try {
    await sendRegisterCode({
      studentId,
      email
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCodeCountdown()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '验证码发送失败，请稍后重试'))
  } finally {
    codeLoading.value = false
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
</script>

<style scoped>
.student-theme {
  --mobile-auth-start: #0f172a;
  --mobile-auth-end: #2563eb;
  --mobile-auth-bg-top: #eef4ff;
  --mobile-auth-bg-mid: #f6f9fd;
  --mobile-auth-glow: rgba(59, 130, 246, 0.18);
  --mobile-auth-glow-soft: rgba(34, 211, 238, 0.1);
  --mobile-auth-accent: #2563eb;
  --mobile-auth-accent-dark: #1d4ed8;
  --mobile-auth-accent-light: #60a5fa;
  --mobile-auth-accent-soft-light: #93c5fd;
  --mobile-auth-accent-soft: rgba(37, 99, 235, 0.1);
}
</style>

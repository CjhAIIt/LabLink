<template>
  <AuthLayout
    title="学生注册"
    subtitle="请使用学号、邮箱验证码和本人真实信息完成注册"
    width="520px"
    :features="features"
  >
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

      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="registerForm.email"
          :prefix-icon="Message"
          placeholder="请输入常用邮箱"
        />
      </el-form-item>

      <el-form-item label="邮箱验证码" prop="emailCode">
        <div class="code-row">
          <el-input
            v-model="registerForm.emailCode"
            maxlength="6"
            placeholder="请输入 6 位验证码"
            @input="sanitizeEmailCode"
          />
          <el-button
            type="primary"
            plain
            :loading="codeLoading"
            :disabled="codeCountdown > 0"
            @click="sendEmailCode"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '发送验证码' }}
          </el-button>
        </div>
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

      <el-form-item class="form-actions">
        <el-button type="primary" :loading="loading" class="submit-button" @click="submitForm">
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

    <template #footer>
      <router-link to="/teacher-register">前往教师注册</router-link>
    </template>
  </AuthLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { ElMessage } from 'element-plus'
import { Lock, Message, Postcard, UserFilled } from '@element-plus/icons-vue'
import { register, sendRegisterCode } from '@/api/auth'
import { getCollegeOptions } from '@/api/colleges'

const features = [
  { label: '注册对象', text: '在校学生，注册后即可浏览实验室并提交入组申请' },
  { label: '所属学院', text: '注册时必须选择，便于学院维度统计与管理' },
  { label: '教师入口', text: '教师请使用独立注册入口并走审批流程' }
]

const router = useRouter()
const loading = ref(false)
const codeLoading = ref(false)
const collegeLoading = ref(false)
const codeCountdown = ref(0)
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
  email: '',
  emailCode: '',
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
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const normalizedEmail = () => registerForm.email.trim().toLowerCase()

const startCodeCountdown = () => {
  clearInterval(codeTimer)
  codeCountdown.value = 60
  codeTimer = window.setInterval(() => {
    if (codeCountdown.value <= 1) {
      clearInterval(codeTimer)
      codeTimer = null
      codeCountdown.value = 0
      return
    }
    codeCountdown.value -= 1
  }, 1000)
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
      email: normalizedEmail(),
      studentId,
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
  clearInterval(codeTimer)
})
</script>

<style scoped>
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

.form-actions {
  margin-bottom: var(--ll-space-2);
  margin-top: var(--ll-space-2);
}

.submit-button {
  width: 100%;
  height: 38px;
  font-size: var(--ll-text-base);
  font-weight: 500;
}

.login-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--ll-space-2);
  font-size: var(--ll-text-sm);
}

.login-link .divider {
  color: var(--ll-border-default);
}
</style>

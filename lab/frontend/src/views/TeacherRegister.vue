<template>
  <AuthLayout
    title="教师注册申请"
    subtitle="提交后进入审批流程，审批通过后方可登录平台"
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
          style="width: 100%"
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

      <el-form-item class="form-actions">
        <el-button type="primary" :loading="loading" class="submit-button" @click="submitForm">
          提交注册申请
        </el-button>
      </el-form-item>

      <el-form-item>
        <div class="login-link">
          <router-link to="/login">返回登录</router-link>
          <span class="divider">|</span>
          <router-link to="/register">学生注册</router-link>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <router-link to="/register">前往学生注册</router-link>
    </template>
  </AuthLayout>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { ElMessage } from 'element-plus'
import { Lock, Message, Postcard, UserFilled } from '@element-plus/icons-vue'
import { getCollegeOptions } from '@/api/colleges'
import { registerTeacher, sendTeacherRegisterCode } from '@/api/auth'

const features = [
  { label: '提交材料', text: '工号、真实姓名、所属学院、联系方式与申请说明' },
  { label: '审批机制', text: '学院管理员初审，学校管理员终审，状态全程可追踪' },
  { label: '通过后能力', text: '登录教师工作台、发起实验室创建申请、查看公告与个人信息' }
]

const router = useRouter()
const loading = ref(false)
const codeLoading = ref(false)
const collegeLoading = ref(false)
const codeCountdown = ref(0)
const registerFormRef = ref()
const collegeOptions = ref([])

let codeTimer = null

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
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' }
  ],
  applyReason: [
    { required: true, message: '请输入申请说明', trigger: 'blur' },
    { min: 10, max: 500, message: '申请说明需在 10 到 500 个字符之间', trigger: 'blur' }
  ]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const normalizedTeacherNo = () => registerForm.teacherNo.trim().toUpperCase()
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
  const teacherNo = normalizedTeacherNo()
  const email = normalizedEmail()

  if (!teacherNoPattern.test(teacherNo)) {
    ElMessage.error('请先输入正确的工号')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.error('请先输入正确的邮箱')
    return
  }

  codeLoading.value = true
  try {
    await sendTeacherRegisterCode({
      teacherNo,
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
    await router.push('/login')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '教师注册申请提交失败，请稍后重试'))
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

<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-hero">
        <BrandLogo
          class="brand"
          size="lg"
          title="LabLink"
          subtitle="高校实验室管理平台 · 教师注册"
        />

        <div class="hero-card">
          <h2>教师注册采用“学院初审 + 学校终审”流程</h2>
          <p>
            教师提交工号和基本信息后，学院管理员先完成初审，学校管理员再进行终审。
            审批通过后，教师可登录平台发起实验室创建申请并查看全流程状态。
          </p>
          <div class="hero-grid">
            <div class="hero-item">
              <span class="hero-label">提交材料</span>
              <strong>工号、真实姓名、所属学院、联系方式、申请说明</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">审批机制</span>
              <strong>学院管理员初审，学校管理员终审，状态全程可追踪</strong>
            </div>
            <div class="hero-item">
              <span class="hero-label">通过后能力</span>
              <strong>登录教师工作台、发起实验室创建申请、查看公告与个人信息</strong>
            </div>
          </div>
        </div>

        <div class="hero-footer">
          <router-link class="ghost-link" to="/register">前往学生注册</router-link>
        </div>
      </section>

      <section class="auth-panel">
        <el-card class="auth-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div>
                <h2>教师注册申请</h2>
                <p>提交后进入审批流程，审批通过后方可登录平台</p>
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
              <el-button type="primary" :loading="loading" style="width: 100%" @click="submitForm">
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
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Postcard, UserFilled } from '@element-plus/icons-vue'
import { getCollegeOptions } from '@/api/colleges'
import { registerTeacher } from '@/api/auth'
import BrandLogo from '@/components/BrandLogo.vue'

const router = useRouter()
const loading = ref(false)
const collegeLoading = ref(false)
const registerFormRef = ref()
const collegeOptions = ref([])

const registerForm = reactive({
  teacherNo: '',
  password: '',
  confirmPassword: '',
  realName: '',
  collegeId: undefined,
  title: '',
  phone: '',
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
  applyReason: [
    { required: true, message: '请输入申请说明', trigger: 'blur' },
    { min: 10, max: 500, message: '申请说明需在 10 到 500 个字符之间', trigger: 'blur' }
  ]
})

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data || error?.message || fallback

const normalizedTeacherNo = () => registerForm.teacherNo.trim().toUpperCase()

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
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 32px 16px;
  background: radial-gradient(circle at top left, #e6f4f1 0%, #f7fbfb 46%, #eef8f7 100%);
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
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(237, 250, 247, 0.95));
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
  background: radial-gradient(circle, rgba(15, 118, 110, 0.22), transparent 70%);
}

.brand {
  margin-bottom: 24px;
}

.hero-card {
  background: rgba(255, 255, 255, 0.74);
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

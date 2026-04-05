<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
              <el-button type="primary" @click="editProfile">
                <el-icon><Edit /></el-icon>
                编辑信息
              </el-button>
            </div>
          </template>

          <div class="profile-info">
            <div class="avatar-section">
              <el-avatar :size="100" :src="resolvedAvatar">
                {{ userInitial }}
              </el-avatar>
              <div class="username">{{ userInfo.realName || '-' }}</div>
              <div class="user-id">{{ accountLabel }}：{{ accountValue }}</div>
              <div class="user-role">{{ roleLabel }}</div>
              <el-button link type="primary" @click="openAvatarUpload">更换头像</el-button>
            </div>

            <div class="info-section">
              <div class="info-item">
                <div class="info-label">用户名</div>
                <div class="info-value">{{ userInfo.username || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">邮箱</div>
                <div class="info-value">{{ userInfo.email || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">手机号</div>
                <div class="info-value">{{ userInfo.phone || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">所属学院</div>
                <div class="info-value">{{ userInfo.college || '-' }}</div>
              </div>
              <div v-if="isStudent" class="info-item">
                <div class="info-label">专业</div>
                <div class="info-value">{{ userInfo.major || '-' }}</div>
              </div>
              <div v-if="isStudent" class="info-item">
                <div class="info-label">简历</div>
                <div class="info-value">
                  <el-link v-if="userInfo.resume" :href="resolveFileUrl(userInfo.resume)" target="_blank" type="primary">
                    查看已上传简历
                  </el-link>
                  <span v-else>-</span>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">注册时间</div>
                <div class="info-value">{{ formatDate(userInfo.createTime) || '-' }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="password-card">
          <template #header>
            <span>修改密码</span>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="110px"
            class="password-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="changePassword">
                保存密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showEditDialog" title="编辑个人信息" width="50%" :close-on-click-modal="false">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="账号">
          <el-input :model-value="accountValue" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item v-if="isStudent" label="专业" prop="major">
          <el-input v-model="profileForm.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="isStudent" label="简历">
          <FileUpload
            v-model="resumeFiles"
            action="/api/file/upload"
            accept=".pdf,.doc,.docx"
            :data="{ scene: 'resume' }"
            :limit="1"
            :tip="'支持 PDF、DOC、DOCX，单个文件不超过 10MB。点击文件名可直接预览。'"
            @change="handleResumeFileChange"
            @remove="handleResumeRemove"
          />
          <el-link
            v-if="profileForm.resume"
            class="resume-link"
            :href="resolveFileUrl(profileForm.resume)"
            target="_blank"
            type="primary"
          >
            查看当前简历
          </el-link>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" :loading="profileLoading" @click="updateProfile">
            保存修改
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showAvatarUpload" title="更换头像" width="420px" :close-on-click-modal="false">
      <div class="avatar-dialog-content">
        <div class="avatar-preview-box">
          <el-avatar :size="120" :src="avatarPreviewUrl">
            {{ userInitial }}
          </el-avatar>
          <div class="avatar-preview-text">
            {{ tempAvatarUrl ? '新头像预览' : '当前头像' }}
          </div>
        </div>

        <FileUpload
          v-model="avatarFiles"
          action="/api/file/upload"
          accept=".jpg,.jpeg,.png"
          :data="{ scene: 'avatar' }"
          :limit="1"
          list-type="picture-card"
          :tip="'支持 JPG、JPEG、PNG，单个文件不超过 10MB。上传后可删除重选。'"
          @change="handleAvatarFileChange"
          @remove="handleAvatarRemove"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeAvatarDialog">取消</el-button>
          <el-button type="primary" :loading="avatarLoading" @click="saveAvatar">
            保存头像
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import request from '@/utils/request'
import FileUpload from '@/components/FileUpload.vue'
import { useUserStore } from '@/stores/user'
import { resolveFileUrl } from '@/utils/file'
import { resolvePortalRole } from '@/utils/portal'

const store = useUserStore()

const passwordLoading = ref(false)
const profileLoading = ref(false)
const avatarLoading = ref(false)
const passwordFormRef = ref(null)
const profileFormRef = ref(null)
const showEditDialog = ref(false)
const showAvatarUpload = ref(false)
const avatarFiles = ref([])
const resumeFiles = ref([])
const tempAvatarUrl = ref('')
const userInfo = ref({})

const portalRole = computed(() => resolvePortalRole(userInfo.value))
const isStudent = computed(() => portalRole.value === 'student')
const isTeacher = computed(() => portalRole.value === 'teacher')
const resolvedAvatar = computed(() => resolveFileUrl(userInfo.value.avatar))
const avatarPreviewUrl = computed(() => resolveFileUrl(tempAvatarUrl.value || userInfo.value.avatar))
const userInitial = computed(() => userInfo.value?.realName?.charAt(0) || 'U')

const accountLabel = computed(() => {
  if (isTeacher.value) {
    return '教师工号'
  }
  if (isStudent.value) {
    return '学号'
  }
  return '管理员账号'
})

const accountValue = computed(() => {
  if (isStudent.value) {
    return userInfo.value.studentId || '-'
  }
  return userInfo.value.username || '-'
})

const roleLabel = computed(() => {
  if (portalRole.value === 'teacher') {
    return '教师账号'
  }
  if (portalRole.value === 'student') {
    return '学生账号'
  }
  return userInfo.value.role === 'super_admin' ? '学校管理员' : '学院管理员'
})

const profileRules = computed(() => {
  const rules = {
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ]
  }

  if (isStudent.value) {
    rules.major = [{ required: true, message: '请输入专业', trigger: 'blur' }]
  }

  return rules
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileForm = reactive({
  realName: '',
  major: '',
  email: '',
  resume: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const toStoredUrl = (file) => file?.rawUrl || file?.response?.data?.url || file?.response?.data?.path || file?.url || ''

const fetchUserInfo = async () => {
  try {
    const response = await request.get('/api/access/profile')
    userInfo.value = {
      ...(store.userInfo || {}),
      ...(response.data || {})
    }
    store.setUserInfo(userInfo.value)
  } catch {
    ElMessage.error('加载个人信息失败')
  }
}

const editProfile = () => {
  if (!userInfo.value?.id) {
    ElMessage.error('个人信息尚未加载完成')
    return
  }

  profileForm.realName = userInfo.value.realName || ''
  profileForm.major = userInfo.value.major || ''
  profileForm.email = userInfo.value.email || ''
  profileForm.resume = userInfo.value.resume || ''

  resumeFiles.value = profileForm.resume
    ? [
        {
          name: 'resume',
          rawUrl: profileForm.resume,
          url: resolveFileUrl(profileForm.resume)
        }
      ]
    : []

  showEditDialog.value = true
}

const updateProfile = async () => {
  if (!profileFormRef.value) {
    return
  }

  try {
    await profileFormRef.value.validate()
    profileLoading.value = true

    const payload = {
      realName: profileForm.realName,
      email: profileForm.email
    }

    if (isStudent.value) {
      payload.major = profileForm.major
      payload.resume = profileForm.resume
    }

    await request.put('/api/user/info', payload)

    ElMessage.success('个人信息已更新')
    showEditDialog.value = false
    await fetchUserInfo()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    profileLoading.value = false
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) {
    return
  }

  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true

    await request.put('/api/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    passwordLoading.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const formatDate = (dateString) => {
  if (!dateString) {
    return ''
  }
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`
}

const handleAvatarFileChange = (files) => {
  tempAvatarUrl.value = files.length ? toStoredUrl(files[files.length - 1]) : ''
}

const handleAvatarRemove = () => {
  tempAvatarUrl.value = ''
}

const openAvatarUpload = () => {
  tempAvatarUrl.value = ''
  avatarFiles.value = []
  showAvatarUpload.value = true
}

const closeAvatarDialog = () => {
  tempAvatarUrl.value = ''
  avatarFiles.value = []
  showAvatarUpload.value = false
}

const saveAvatar = async () => {
  if (!tempAvatarUrl.value) {
    ElMessage.warning('请先上传头像')
    return
  }

  try {
    avatarLoading.value = true

    await request.put('/api/user/avatar', {
      avatar: tempAvatarUrl.value
    })

    ElMessage.success('头像更新成功')
    closeAvatarDialog()
    userInfo.value.avatar = tempAvatarUrl.value
    store.setUserInfo(userInfo.value)
    await fetchUserInfo()
  } catch (error) {
    ElMessage.error(error.message || '头像更新失败')
  } finally {
    avatarLoading.value = false
  }
}

const handleResumeFileChange = (files) => {
  profileForm.resume = files.length ? toStoredUrl(files[files.length - 1]) : ''
}

const handleResumeRemove = () => {
  profileForm.resume = ''
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 0;
}

.profile-card,
.password-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  gap: 8px;
}

.username {
  font-size: 18px;
  font-weight: bold;
  margin-top: 10px;
  color: #303133;
}

.user-id,
.user-role {
  font-size: 14px;
  color: #909399;
}

.info-section {
  width: 100%;
}

.info-item {
  display: flex;
  margin-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 10px;
}

.info-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.info-label {
  width: 88px;
  color: #909399;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #303133;
  font-size: 14px;
  word-break: break-word;
}

.password-form {
  max-width: 560px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.avatar-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avatar-preview-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-preview-text {
  color: #606266;
  font-size: 14px;
}

.resume-link {
  margin-top: 12px;
}
</style>

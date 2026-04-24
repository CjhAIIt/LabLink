<template>
  <div class="m-page profile-page">
    <section class="profile-hero">
      <div class="hero-copy">
        <span class="hero-badge">Profile Center</span>
        <div class="hero-main">
          <el-avatar :size="72" class="hero-avatar">{{ userInitial }}</el-avatar>
          <div class="hero-meta">
            <h1>{{ userStore.realName || userStore.userName || '用户' }}</h1>
            <p>{{ roleLabel }}</p>
            <div class="hero-tags">
              <span class="hero-tag">{{ collegeLabel }}</span>
              <span class="hero-tag accent">{{ labLabel }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-button class="hero-refresh" plain :loading="loading" @click="refresh()">刷新资料</el-button>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <span>账号</span>
        <strong>{{ userStore.userName || '-' }}</strong>
        <small>当前登录账号</small>
      </article>
      <article class="summary-card">
        <span>身份</span>
        <strong>{{ roleLabel }}</strong>
        <small>{{ hasResume ? '简历已完善' : '建议补充简历' }}</small>
      </article>
      <article class="summary-card">
        <span>学院</span>
        <strong>{{ collegeLabel }}</strong>
        <small>用于资料归档与权限范围</small>
      </article>
      <article class="summary-card">
        <span>实验室</span>
        <strong>{{ labLabel }}</strong>
        <small>{{ userStore.userInfo?.labId ? '已同步实验室身份' : '暂未加入实验室' }}</small>
      </article>
    </section>

    <section class="panel-card detail-card">
      <div class="section-head">
        <div>
          <strong>个人资料</strong>
          <p>这里会展示当前账号的最新基础信息和实验室归属。</p>
        </div>
      </div>
      <div class="detail-list">
        <div class="detail-item">
          <span>姓名</span>
          <strong>{{ userStore.realName || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>账号</span>
          <strong>{{ userStore.userName || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>学院</span>
          <strong>{{ collegeLabel }}</strong>
        </div>
        <div class="detail-item">
          <span>实验室</span>
          <strong>{{ labLabel }}</strong>
        </div>
      </div>
    </section>

    <section class="panel-card resume-card">
      <div class="section-head">
        <div>
          <strong>简历材料</strong>
          <p>实验室报名、成员升级等场景会使用这份简历材料。</p>
        </div>
        <el-button size="small" type="primary" @click="openResumeDialog">{{ hasResume ? '更新' : '上传' }}</el-button>
      </div>

      <div class="resume-banner">
        <div>
          <span class="resume-state">{{ hasResume ? '已上传' : '待完善' }}</span>
          <strong>{{ hasResume ? currentResumeName : '还没有上传简历文件' }}</strong>
          <p>{{ hasResume ? '如需替换，请重新上传并保存。' : '建议先下载模板填写，再上传 PDF / DOC / DOCX。' }}</p>
        </div>
        <a class="file-link ghost" :href="resumeTemplateUrl" download>下载模板</a>
      </div>

      <div class="info-row">
        <span>当前文件</span>
        <a v-if="hasResume" class="file-link" :href="currentResumeUrl" target="_blank" rel="noreferrer">{{ currentResumeName }}</a>
        <strong v-else>未上传</strong>
      </div>
    </section>

    <section v-if="isStudent" class="panel-card member-profile-card">
      <div class="section-head">
        <div>
          <strong>成员资料审核</strong>
          <p>上传成员资料附件并提交后，管理员会在“资料审核”中处理。</p>
        </div>
        <span class="resume-state">{{ profileStatusLabel }}</span>
      </div>

      <div class="resume-banner profile-audit-banner">
        <div>
          <strong>{{ requiresProfileReview ? '加入实验室后需要提交成员资料审核' : '暂未加入实验室' }}</strong>
          <p>{{ requiresProfileReview ? '请补全基础信息，上传成员资料附件后提交审核。' : '请先通过实验室申请，加入后再提交成员资料审核。' }}</p>
        </div>
      </div>

      <div class="mobile-profile-form">
        <label>
          <span>学号</span>
          <el-input v-model="studentProfileForm.studentNo" :disabled="memberProfileLocked" placeholder="请输入学号" />
        </label>
        <label>
          <span>真实姓名</span>
          <el-input v-model="studentProfileForm.realName" :disabled="memberProfileLocked" placeholder="请输入真实姓名" />
        </label>
        <label>
          <span>专业</span>
          <el-input v-model="studentProfileForm.major" :disabled="memberProfileLocked" placeholder="请输入专业" />
        </label>
        <label>
          <span>班级</span>
          <el-input v-model="studentProfileForm.className" :disabled="memberProfileLocked" placeholder="请输入班级" />
        </label>
        <label>
          <span>电话</span>
          <el-input v-model="studentProfileForm.phone" :disabled="memberProfileLocked" placeholder="请输入联系电话" />
        </label>
        <label>
          <span>邮箱</span>
          <el-input v-model="studentProfileForm.email" :disabled="memberProfileLocked" placeholder="请输入邮箱" />
        </label>
        <label>
          <span>研究方向</span>
          <el-input v-model="studentProfileForm.direction" :disabled="memberProfileLocked" placeholder="可填写兴趣方向" />
        </label>
        <label class="form-wide">
          <span>个人介绍</span>
          <el-input
            v-model="studentProfileForm.introduction"
            :disabled="memberProfileLocked"
            :rows="4"
            type="textarea"
            placeholder="简要介绍背景、技能和阶段目标"
          />
        </label>
      </div>

      <div class="upload-panel member-upload-panel">
        <el-upload
          class="resume-upload"
          :show-file-list="false"
          :http-request="uploadProfileAttachment"
          :disabled="memberProfileLocked"
          accept=".pdf,.doc,.docx"
        >
          <el-button type="primary" plain :disabled="memberProfileLocked" :loading="memberProfileUploading">
            上传成员资料附件
          </el-button>
        </el-upload>
        <div class="upload-result">
          <span>当前附件</span>
          <a
            v-if="studentProfileForm.attachmentUrl"
            class="file-link"
            :href="profileAttachmentUrl"
            target="_blank"
            rel="noreferrer"
          >
            {{ profileAttachmentName }}
          </a>
          <strong v-else>未上传</strong>
        </div>
      </div>

      <div class="dialog-actions profile-actions">
        <el-button :loading="memberProfileSaving" :disabled="memberProfileLocked" @click="saveMemberProfile">保存草稿</el-button>
        <el-button
          type="primary"
          :loading="memberProfileSubmitting"
          :disabled="memberProfileLocked || !requiresProfileReview"
          @click="submitMemberProfile"
        >
          提交审核
        </el-button>
      </div>
    </section>

    <section class="action-card">
      <button class="action-row" type="button" @click="router.push(resolvePortalHome(userStore.userInfo, { surface: 'mobile' }))">
        <div>
          <strong>返回首页</strong>
          <p>继续使用移动端功能</p>
        </div>
        <el-icon :size="18"><ArrowRight /></el-icon>
      </button>
      <button class="action-row" type="button" @click="switchDesktop">
        <div>
          <strong>切换桌面端</strong>
          <p>进入原有 Web 管理界面</p>
        </div>
        <el-icon :size="18"><ArrowRight /></el-icon>
      </button>
      <button class="action-row danger" type="button" @click="logout">
        <div>
          <strong>退出登录</strong>
          <p>清除本地登录状态</p>
        </div>
        <el-icon :size="18"><ArrowRight /></el-icon>
      </button>
    </section>

    <el-dialog v-model="dialogVisible" title="上传简历" width="92%">
      <div class="dialog-copy">
        <p>请先下载《成员入驻申请表》模板，完善内容后上传 PDF、DOC 或 DOCX 文件。</p>
        <a class="file-link" :href="resumeTemplateUrl" download>下载成员入驻申请表模板</a>
      </div>
      <div class="upload-panel">
        <el-upload class="resume-upload" :show-file-list="false" :http-request="uploadResumeFile" accept=".pdf,.doc,.docx">
          <el-button type="primary" plain :loading="uploading">选择简历文件</el-button>
        </el-upload>
        <div class="upload-result">
          <span>待保存文件</span>
          <strong>{{ tempResumeName || '未选择' }}</strong>
        </div>
      </div>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="closeResumeDialog">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveResume">保存简历</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyStudentProfile, saveMyStudentProfile, submitMyStudentProfile } from '@/api/profile'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { getFileNameFromUrl, resolveFileUrl } from '@/utils/file'
import { resolvePortalHome, resolvePortalLogin, resolvePortalRole, setPortalSurface } from '@/utils/portal'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dialogVisible = ref(false)
const uploading = ref(false)
const saving = ref(false)
const memberProfileUploading = ref(false)
const memberProfileSaving = ref(false)
const memberProfileSubmitting = ref(false)
const tempResumeUrl = ref('')
const tempResumeName = ref('')
const resumeTemplateUrl = '/templates/member-application-template.docx'
const studentProfile = ref({})

const studentProfileForm = reactive({
  studentNo: '',
  realName: '',
  gender: '',
  collegeId: null,
  major: '',
  className: '',
  phone: '',
  email: '',
  direction: '',
  introduction: '',
  attachmentUrl: ''
})

const userInitial = computed(() => userStore.realName?.charAt(0) || userStore.userName?.charAt(0) || 'U')
const portalRole = computed(() => resolvePortalRole(userStore.userInfo))
const isStudent = computed(() => portalRole.value === 'student')
const hasResume = computed(() => Boolean(userStore.userInfo?.resume))
const currentResumeUrl = computed(() => resolveFileUrl(userStore.userInfo?.resume))
const currentResumeName = computed(() => getFileNameFromUrl(userStore.userInfo?.resume, '已上传简历'))
const roleLabel = computed(() => ({ admin: '管理员', teacher: '教师', student: '学生' }[portalRole.value] || '用户'))
const collegeLabel = computed(() => userStore.userInfo?.collegeName || userStore.userInfo?.college || '未设置学院')
const labLabel = computed(() => {
  if (userStore.userInfo?.labName) {
    return userStore.userInfo.labName
  }
  return '暂无实验室'
})
const requiresProfileReview = computed(() => Boolean(studentProfile.value?.reviewRequired ?? userStore.userInfo?.labId))
const memberProfileLocked = computed(() => requiresProfileReview.value && studentProfile.value?.status === 'PENDING')
const profileAttachmentUrl = computed(() => resolveFileUrl(studentProfileForm.attachmentUrl))
const profileAttachmentName = computed(() => getFileNameFromUrl(studentProfileForm.attachmentUrl, '成员资料附件'))
const profileStatusLabel = computed(() => ({
  DRAFT: '草稿',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  ARCHIVED: '已归档'
}[studentProfile.value?.status] || '草稿')
)

const applyStudentProfile = (payload = {}) => {
  studentProfile.value = payload || {}
  studentProfileForm.studentNo = payload.studentNo || userStore.userInfo?.studentId || ''
  studentProfileForm.realName = payload.realName || userStore.userInfo?.realName || ''
  studentProfileForm.gender = payload.gender || ''
  studentProfileForm.collegeId = payload.collegeId || null
  studentProfileForm.major = payload.major || userStore.userInfo?.major || ''
  studentProfileForm.className = payload.className || ''
  studentProfileForm.phone = payload.phone || userStore.userInfo?.phone || ''
  studentProfileForm.email = payload.email || userStore.userInfo?.email || ''
  studentProfileForm.direction = payload.direction || ''
  studentProfileForm.introduction = payload.introduction || ''
  studentProfileForm.attachmentUrl = payload.attachmentUrl || payload.resumeUrl || userStore.userInfo?.resume || ''
}

const fetchStudentProfile = async () => {
  if (!isStudent.value) {
    return
  }
  const response = await getMyStudentProfile()
  applyStudentProfile(response.data || {})
}

const refresh = async (options = {}) => {
  const { silent = false } = options
  loading.value = true
  try {
    const response = await request.get('/api/access/profile')
    userStore.setUserInfo(response.data || {})
    if (resolvePortalRole(userStore.userInfo) === 'student') {
      await fetchStudentProfile()
    }
    if (!silent) {
      ElMessage.success('资料已刷新')
    }
  } catch (error) {
    if (!silent || !userStore.userInfo?.id) {
      ElMessage.error(error.message || '资料刷新失败')
    }
  } finally {
    loading.value = false
  }
}

const openResumeDialog = () => {
  tempResumeUrl.value = userStore.userInfo?.resume || ''
  tempResumeName.value = userStore.userInfo?.resume ? currentResumeName.value : ''
  dialogVisible.value = true
}

const closeResumeDialog = () => {
  dialogVisible.value = false
  tempResumeUrl.value = ''
  tempResumeName.value = ''
}

const uploadResumeFile = async ({ file }) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('scene', 'resume')
    const response = await request.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    tempResumeUrl.value = response.data?.url || response.data?.path || response.data || ''
    tempResumeName.value = file.name
    ElMessage.success('文件上传成功，点击保存后生效')
  } finally {
    uploading.value = false
  }
}

const uploadProfileAttachment = async ({ file }) => {
  memberProfileUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('scene', 'resume')
    const response = await request.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    studentProfileForm.attachmentUrl = response.data?.url || response.data?.path || response.data || ''
    ElMessage.success('成员资料附件上传成功，保存或提交后生效')
  } catch (error) {
    ElMessage.error(error.message || '成员资料附件上传失败')
  } finally {
    memberProfileUploading.value = false
  }
}

const validateMemberProfile = ({ requireAttachment = false } = {}) => {
  const requiredFields = [
    ['studentNo', '请填写学号'],
    ['realName', '请填写真实姓名'],
    ['major', '请填写专业'],
    ['className', '请填写班级'],
    ['phone', '请填写联系电话'],
    ['email', '请填写邮箱']
  ]
  const missing = requiredFields.find(([key]) => !String(studentProfileForm[key] || '').trim())
  if (missing) {
    ElMessage.warning(missing[1])
    return false
  }
  if (requireAttachment && !studentProfileForm.attachmentUrl) {
    ElMessage.warning('请先上传成员资料附件')
    return false
  }
  return true
}

const saveMemberProfile = async () => {
  if (!validateMemberProfile()) {
    return
  }
  memberProfileSaving.value = true
  try {
    const response = await saveMyStudentProfile({ ...studentProfileForm })
    applyStudentProfile(response.data || {})
    ElMessage.success('成员资料草稿已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存成员资料失败')
  } finally {
    memberProfileSaving.value = false
  }
}

const submitMemberProfile = async () => {
  if (!requiresProfileReview.value) {
    ElMessage.warning('请先加入实验室，再提交成员资料审核')
    return
  }
  if (!validateMemberProfile({ requireAttachment: true })) {
    return
  }
  const result = await ElMessageBox.confirm('确认提交成员资料进入审核吗？审核中暂时不能修改。', '提交资料审核', {
    type: 'warning'
  }).catch(() => null)
  if (!result) {
    return
  }
  memberProfileSubmitting.value = true
  try {
    const saveResponse = await saveMyStudentProfile({ ...studentProfileForm })
    applyStudentProfile(saveResponse.data || {})
    const response = await submitMyStudentProfile()
    applyStudentProfile(response.data || {})
    ElMessage.success('成员资料已提交审核')
  } catch (error) {
    ElMessage.error(error.message || '提交成员资料审核失败')
  } finally {
    memberProfileSubmitting.value = false
  }
}

const saveResume = async () => {
  if (!tempResumeUrl.value) {
    ElMessage.warning('请先上传简历文件')
    return
  }
  saving.value = true
  try {
    await request.put('/api/user/info', { resume: tempResumeUrl.value })
    userStore.setUserInfo({ ...(userStore.userInfo || {}), resume: tempResumeUrl.value })
    if (!studentProfileForm.attachmentUrl) {
      studentProfileForm.attachmentUrl = tempResumeUrl.value
    }
    ElMessage.success('简历已保存')
    closeResumeDialog()
  } finally {
    saving.value = false
  }
}

const switchDesktop = async () => {
  setPortalSurface('desktop')
  await router.push('/login')
}

const logout = async () => {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type: 'warning' })
  setPortalSurface('mobile')
  userStore.clearUserInfo()
  await router.push(resolvePortalLogin({ surface: 'mobile' }))
}

onMounted(() => {
  refresh({ silent: Boolean(userStore.userInfo?.id) })
})
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 14px;
}

.profile-hero,
.panel-card,
.action-card,
.summary-card {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.profile-hero {
  padding: 22px 18px;
  background:
    radial-gradient(circle at top right, rgba(191, 219, 254, 0.42), transparent 28%),
    linear-gradient(145deg, #0f172a, #1d4ed8 60%, #38bdf8);
  color: #f8fafc;
  display: grid;
  gap: 18px;
}

.profile-hero::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  right: -70px;
  bottom: -82px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 72%);
}

.hero-copy,
.hero-refresh {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-main {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0.08));
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.18);
}

.hero-meta {
  min-width: 0;
}

.hero-meta h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.08;
  word-break: break-all;
}

.hero-meta p {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.84);
  line-height: 1.6;
}

.hero-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 700;
}

.hero-tag.accent {
  background: rgba(125, 211, 252, 0.16);
}

.hero-refresh {
  min-height: 42px;
  border-radius: 14px;
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  padding: 16px 14px;
  background: rgba(255, 255, 255, 0.94);
  display: grid;
  gap: 6px;
}

.summary-card span,
.summary-card small,
.section-head p,
.detail-item span,
.resume-banner p,
.info-row span,
.action-row p,
.dialog-copy p,
.upload-result span,
.mobile-profile-form label > span {
  color: #64748b;
}

.summary-card strong,
.section-head strong,
.detail-item strong,
.resume-banner strong,
.info-row strong,
.action-row strong {
  color: #0f172a;
}

.summary-card strong {
  font-size: 18px;
  line-height: 1.3;
}

.panel-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.95);
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.section-head p,
.resume-banner p,
.action-row p,
.dialog-copy p {
  margin: 6px 0 0;
  line-height: 1.65;
}

.detail-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.detail-item {
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.82);
  display: grid;
  gap: 8px;
}

.resume-banner {
  margin-top: 14px;
  padding: 16px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(191, 219, 254, 0.28), transparent 28%),
    linear-gradient(180deg, rgba(239, 246, 255, 0.96), rgba(248, 250, 252, 0.98));
  border: 1px solid rgba(191, 219, 254, 0.7);
  display: grid;
  gap: 14px;
}

.resume-state {
  display: inline-flex;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.profile-audit-banner {
  background:
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.18), transparent 30%),
    linear-gradient(180deg, rgba(240, 253, 250, 0.96), rgba(248, 250, 252, 0.98));
  border-color: rgba(94, 234, 212, 0.52);
}

.mobile-profile-form {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mobile-profile-form label {
  display: grid;
  gap: 7px;
}

.mobile-profile-form label > span {
  font-size: 12px;
  font-weight: 700;
}

.mobile-profile-form .form-wide {
  grid-column: 1 / -1;
}

.member-upload-panel,
.profile-actions {
  margin-top: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.82);
}

.file-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 700;
}

.file-link.ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 9px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(191, 219, 254, 0.72);
}

.action-card {
  background: rgba(255, 255, 255, 0.95);
}

.action-row {
  width: 100%;
  padding: 15px 16px;
  border: 0;
  background: transparent;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  text-align: left;
  border-top: 1px solid rgba(226, 232, 240, 0.82);
}

.action-row:first-child {
  border-top: 0;
}

.action-row.danger strong,
.action-row.danger .el-icon {
  color: #b91c1c;
}

.dialog-copy {
  margin-bottom: 14px;
}

.upload-panel {
  padding: 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px dashed rgba(148, 163, 184, 0.44);
}

.resume-upload {
  margin-bottom: 12px;
}

.upload-result {
  display: grid;
  gap: 4px;
  color: #0f172a;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 480px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .mobile-profile-form {
    grid-template-columns: 1fr;
  }

  .hero-main,
  .info-row {
    align-items: flex-start;
  }

  .info-row {
    flex-direction: column;
  }
}
</style>

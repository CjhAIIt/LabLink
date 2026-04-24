<template>
  <div class="page-shell profile-page">
    <el-row :gutter="20">
      <el-col :xs="24" :md="8">
        <TablePageCard class="account-card" title="账户信息" subtitle="资料概览">
          <template #header-extra>
            <el-button v-if="!isStudent" type="primary" @click="openBasicDialog">编辑</el-button>
          </template>

          <div class="account-panel">
            <el-avatar :size="96" :src="resolvedAvatar">
              {{ userInitial }}
            </el-avatar>
            <div class="account-name">{{ userInfo.realName || '-' }}</div>
            <div class="account-role">{{ roleLabel }}</div>
            <div class="account-id">{{ accountLabel }}: {{ accountValue }}</div>
            <el-button link type="primary" @click="openAvatarDialog">更换头像</el-button>
          </div>

          <div class="info-list">
            <div class="info-row">
              <span>邮箱</span>
              <strong>{{ userInfo.email || '-' }}</strong>
            </div>
            <div class="info-row">
              <span>电话</span>
              <strong>{{ userInfo.phone || '-' }}</strong>
            </div>
            <div class="info-row">
              <span>学院</span>
              <strong>{{ userInfo.college || userInfo.collegeName || '-' }}</strong>
            </div>
            <div class="info-row">
              <span>实验室</span>
              <strong>{{ resolvedLabName }}</strong>
            </div>
          </div>
        </TablePageCard>
      </el-col>

      <el-col :xs="24" :md="16">
        <TablePageCard title="密码管理" subtitle="账号凭证">

          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="changePassword">保存密码</el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </TablePageCard>
      </el-col>
    </el-row>

    <TablePageCard
      v-if="isStudent"
      class="resume-card"
      title="简历材料"
      subtitle="加入实验室前，请先下载模板并上传完整简历。"
    >
      <template #header-extra>
        <div class="header-actions">
          <el-link :href="resumeTemplateUrl" download type="primary">下载模板</el-link>
          <el-button type="primary" @click="openResumeDialog">
            {{ hasResume ? '更新简历' : '简历提交' }}
          </el-button>
        </div>
      </template>

      <el-alert
        class="status-alert"
        :closable="false"
        show-icon
        :type="hasResume ? 'success' : 'warning'"
        :title="hasResume ? '简历已提交，可继续申请加入实验室。' : '当前尚未提交简历，暂时不能申请加入实验室。'"
      />

      <div class="resume-body">
        <div class="resume-row">
          <span>当前状态</span>
          <strong>{{ hasResume ? '已上传' : '未上传' }}</strong>
        </div>
        <div class="resume-row">
          <span>当前文件</span>
          <el-link v-if="hasResume" :href="resolvedResumeUrl" target="_blank" type="primary">
            {{ currentResumeName }}
          </el-link>
          <strong v-else>-</strong>
        </div>
      </div>
    </TablePageCard>

    <TablePageCard
      v-if="isStudent"
      class="profile-workspace"
      :title="profileWorkspaceTitle"
      :subtitle="profileWorkspaceSubtitle"
    >
      <template #header-extra>
        <div class="header-actions">
          <StatusTag v-if="requiresProfileReview" :value="studentProfile.status || 'DRAFT'" preset="profile" />
          <el-button @click="refreshStudentProfile">刷新</el-button>
        </div>
      </template>

      <el-alert
        v-if="requiresProfileReview"
        class="status-alert"
        :closable="false"
        show-icon
        :type="statusAlertType(studentProfile.status)"
        :title="statusHint(studentProfile.status)"
      />
      <el-alert
        v-else
        class="status-alert"
        :closable="false"
        show-icon
        type="info"
        title="当前账号未加入实验室，资料保存后直接生效，无需提交审核。"
      />

      <div v-if="requiresProfileReview" class="summary-box summary-card-grid">
        <MetricCard label="当前版本" :value="studentProfile.currentVersion ?? 0" tip="当前草稿版本" compact />
        <MetricCard label="提交时间" :value="formatDateTime(studentProfile.submittedAt) || '-'" tip="最近一次提交时间" compact />
        <MetricCard label="最近审核" :value="formatDateTime(studentProfile.lastReviewTime) || '-'" tip="最近一次审核时间" compact />
        <MetricCard label="归档数量" :value="archiveHistory.length" tip="已归档的通过快照" compact />
      </div>

      <el-form
        ref="studentProfileFormRef"
        :model="studentProfileForm"
        :rules="studentProfileRules"
        label-width="120px"
        class="student-profile-form"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="studentProfileForm.studentNo" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="studentProfileForm.realName" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="性别">
              <el-select v-model="studentProfileForm.gender" :disabled="studentProfileLocked" clearable>
                <el-option label="男" value="male" />
                <el-option label="女" value="female" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="专业" prop="major">
              <el-input v-model="studentProfileForm.major" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="studentProfileForm.className" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="方向">
              <el-input v-model="studentProfileForm.direction" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="studentProfileForm.phone" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="studentProfileForm.email" :disabled="studentProfileLocked" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="学院" prop="collegeId">
              <el-select
                v-if="!requiresProfileReview"
                v-model="studentProfileForm.collegeId"
                :disabled="studentProfileLocked"
                clearable
                filterable
                placeholder="请选择学院"
              >
                <el-option
                  v-for="item in collegeOptions"
                  :key="item.id"
                  :label="item.collegeName"
                  :value="item.id"
                />
              </el-select>
              <el-input v-else :model-value="resolvedCollegeName" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="实验室">
              <el-input :model-value="resolvedLabName" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="资料附件" prop="attachmentUrl">
              <div class="profile-attachment-box">
                <FileUpload
                  v-model="profileAttachmentFiles"
                  action="/api/files/upload"
                  accept=".pdf,.doc,.docx"
                  :data="{ scene: 'resume' }"
                  :limit="1"
                  :disabled="studentProfileLocked"
                  :tip="'请上传成员资料表、承诺书或补充证明，支持 PDF、DOC、DOCX。提交审核前必须上传。'"
                  @change="handleProfileAttachmentChange"
                  @remove="handleProfileAttachmentRemove"
                />
                <el-link
                  v-if="studentProfileForm.attachmentUrl"
                  :href="resolvedProfileAttachmentUrl"
                  target="_blank"
                  type="primary"
                >
                  查看当前资料附件：{{ currentProfileAttachmentName }}
                </el-link>
                <div class="profile-upload-tip">
                  上传后请点击“保存草稿”或“提交审核”，管理员会在资料审核中心看到该附件。
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="个人介绍">
              <el-input
                v-model="studentProfileForm.introduction"
                type="textarea"
                :rows="5"
                :disabled="studentProfileLocked"
                placeholder="请简要介绍你的背景、兴趣方向和阶段目标。"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" :loading="studentProfileSaving" :disabled="studentProfileLocked" @click="saveStudentProfile">
            {{ requiresProfileReview ? '保存草稿' : '保存资料' }}
          </el-button>
          <el-button
            v-if="requiresProfileReview"
            type="success"
            :loading="studentProfileSubmitting"
            :disabled="studentProfileLocked"
            @click="submitStudentProfile"
          >
            提交审核
          </el-button>
        </el-form-item>
      </el-form>

      <el-row v-if="requiresProfileReview" :gutter="20" class="history-section">
        <el-col :xs="24" :lg="12">
          <TablePageCard title="审核记录" subtitle="历史审核意见" :count-label="`${reviewHistory.length} 条`">
            <el-timeline v-if="reviewHistory.length">
              <el-timeline-item
                v-for="item in reviewHistory"
                :key="item.id"
                :timestamp="formatDateTime(item.reviewTime || item.createTime)"
                :type="timelineType(item.reviewStatus)"
              >
                <div class="timeline-title">版本 {{ item.versionNo }} · {{ formatReviewStatus(item.reviewStatus) }}</div>
                <div class="timeline-text">{{ item.reviewComment || '无审核意见' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审核记录" />
          </TablePageCard>
        </el-col>

        <el-col :xs="24" :lg="12">
          <TablePageCard title="归档记录" subtitle="通过后的资料快照" :count-label="`${archiveHistory.length} 条`">
            <el-timeline v-if="archiveHistory.length">
              <el-timeline-item
                v-for="item in archiveHistory"
                :key="item.id"
                :timestamp="formatDateTime(item.archivedAt || item.createTime)"
                type="success"
              >
                <div class="timeline-title">版本 {{ item.versionNo }} 已归档</div>
                <div class="timeline-text">
                  {{ item.snapshot?.direction || item.snapshot?.major || '已生成资料快照' }}
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无归档记录" />
          </TablePageCard>
        </el-col>
      </el-row>
    </TablePageCard>

    <el-dialog v-model="showBasicDialog" title="编辑基础信息" width="520px" :close-on-click-modal="false">
      <el-form ref="basicFormRef" :model="basicForm" :rules="basicRules" label-width="120px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="basicForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="basicForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBasicDialog = false">取消</el-button>
        <el-button type="primary" :loading="basicSaving" @click="saveBasicInfo">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAvatarDialog" title="更换头像" width="420px" :close-on-click-modal="false">
      <FileUpload
        v-model="avatarFiles"
        action="/api/files/upload"
        accept=".jpg,.jpeg,.png"
        :data="{ scene: 'avatar' }"
        :limit="1"
        list-type="picture-card"
        :tip="'仅支持上传 JPG 或 PNG 图片。'"
        @change="handleAvatarFileChange"
        @remove="handleAvatarRemove"
      />
      <template #footer>
        <el-button @click="closeAvatarDialog">取消</el-button>
        <el-button type="primary" :loading="avatarSaving" @click="saveAvatar">保存头像</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showResumeDialog" title="简历提交" width="520px" :close-on-click-modal="false">
      <div class="resume-dialog-copy">
        <p>请先下载《成员入驻申请表》模板，补全内容后再上传 PDF、DOC 或 DOCX 文件。</p>
        <el-link :href="resumeTemplateUrl" download type="primary">下载成员入驻申请表模板</el-link>
      </div>

      <FileUpload
        v-model="resumeFiles"
        action="/api/files/upload"
        accept=".pdf,.doc,.docx"
        :data="{ scene: 'resume' }"
        :limit="1"
        :tip="'支持 PDF、DOC、DOCX，单个文件不超过 10MB。'"
        @change="handleResumeFileChange"
        @remove="handleResumeRemove"
      />

      <template #footer>
        <el-button @click="closeResumeDialog">取消</el-button>
        <el-button type="primary" :loading="resumeSaving" @click="saveResume">保存简历</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FileUpload from '@/components/FileUpload.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import {
  getMyStudentProfile,
  getProfileArchives,
  getProfileReviews,
  saveMyStudentProfile,
  submitMyStudentProfile
} from '@/api/profile'
import { getCollegeOptions } from '@/api/colleges'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { getFileNameFromUrl, resolveFileUrl } from '@/utils/file'
import { resolvePortalRole } from '@/utils/portal'

const store = useUserStore()

const userInfo = ref({})
const studentProfile = ref({})
const reviewHistory = ref([])
const archiveHistory = ref([])
const collegeOptions = ref([])

const passwordLoading = ref(false)
const basicSaving = ref(false)
const avatarSaving = ref(false)
const resumeSaving = ref(false)
const studentProfileSaving = ref(false)
const studentProfileSubmitting = ref(false)
const showBasicDialog = ref(false)
const showAvatarDialog = ref(false)
const showResumeDialog = ref(false)
const avatarFiles = ref([])
const resumeFiles = ref([])
const profileAttachmentFiles = ref([])
const tempAvatarUrl = ref('')
const tempResumeUrl = ref('')

const passwordFormRef = ref(null)
const basicFormRef = ref(null)
const studentProfileFormRef = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const basicForm = reactive({
  realName: '',
  email: ''
})

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

const portalRole = computed(() => resolvePortalRole(userInfo.value))
const isStudent = computed(() => portalRole.value === 'student')
const requiresProfileReview = computed(() => Boolean(studentProfile.value?.reviewRequired ?? userInfo.value.labId))
const resolvedAvatar = computed(() => resolveFileUrl(userInfo.value.avatar))
const hasResume = computed(() => Boolean(userInfo.value?.resume))
const resolvedResumeUrl = computed(() => resolveFileUrl(userInfo.value?.resume))
const currentResumeName = computed(() => getFileNameFromUrl(userInfo.value?.resume, '已上传简历'))
const resolvedProfileAttachmentUrl = computed(() => resolveFileUrl(studentProfileForm.attachmentUrl))
const currentProfileAttachmentName = computed(() =>
  getFileNameFromUrl(studentProfileForm.attachmentUrl, '成员资料附件')
)
const userInitial = computed(() => userInfo.value?.realName?.charAt(0) || '用')
const studentProfileLocked = computed(() => requiresProfileReview.value && studentProfile.value?.status === 'PENDING')
const profileWorkspaceTitle = computed(() => (requiresProfileReview.value ? '成员资料流程' : '个人资料'))
const profileWorkspaceSubtitle = computed(() =>
  (requiresProfileReview.value ? '请先完善资料，再提交管理员审核。' : '未加入实验室前，资料保存后立即生效。')
)
const resumeTemplateUrl = '/templates/member-application-template.docx'
const resolvedLabName = computed(() => studentProfile.value.labName || userInfo.value.labName || '暂无实验室')
const resolvedCollegeName = computed(() => {
  if (studentProfile.value?.collegeName) {
    return studentProfile.value.collegeName
  }
  if (studentProfileForm.collegeId) {
    return findCollegeNameById(studentProfileForm.collegeId) || userInfo.value.college || userInfo.value.collegeName || '-'
  }
  return userInfo.value.college || userInfo.value.collegeName || '-'
})

const accountLabel = computed(() => {
  if (portalRole.value === 'student') {
    return '学号'
  }
  if (portalRole.value === 'teacher') {
    return '教师账号'
  }
  return '管理员账号'
})

const accountValue = computed(() => {
  if (portalRole.value === 'student') {
    return userInfo.value.studentId || '-'
  }
  return userInfo.value.username || '-'
})

const roleLabel = computed(() => {
  if (portalRole.value === 'student') return '学生端'
  if (portalRole.value === 'teacher') return '教师端'
  return userInfo.value.role === 'super_admin' ? '学校管理员' : '管理端'
})

const basicRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const studentProfileRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  collegeId: [
    {
      validator: (rule, value, callback) => {
        if (requiresProfileReview.value || value) {
          callback()
          return
        }
        callback(new Error('请选择学院'))
      },
      trigger: 'change'
    }
  ],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  attachmentUrl: [
    {
      validator: (rule, value, callback) => {
        if (!requiresProfileReview.value || value) {
          callback()
          return
        }
        callback(new Error('请先上传成员资料附件'))
      },
      trigger: 'change'
    }
  ]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const fetchUserInfo = async () => {
  const response = await request.get('/api/access/profile')
  userInfo.value = {
    ...(store.userInfo || {}),
    ...(response.data || {})
  }
  store.setUserInfo(userInfo.value)
}

const findCollegeIdByName = (collegeName) => {
  const normalized = collegeName?.trim()
  if (!normalized) {
    return null
  }
  return collegeOptions.value.find((item) => item.collegeName === normalized)?.id || null
}

const findCollegeNameById = (collegeId) => {
  if (!collegeId) {
    return ''
  }
  return collegeOptions.value.find((item) => item.id === collegeId)?.collegeName || ''
}

const syncCollegeSelection = (candidateName) => {
  if (studentProfileForm.collegeId) {
    return
  }
  studentProfileForm.collegeId = findCollegeIdByName(candidateName || userInfo.value.college || userInfo.value.collegeName)
}

const fetchCollegeOptionList = async () => {
  const response = await getCollegeOptions()
  collegeOptions.value = response.data || []
  syncCollegeSelection(studentProfile.value?.collegeName)
}

const applyStudentProfile = (payload = {}) => {
  studentProfile.value = payload || {}
  studentProfileForm.studentNo = payload.studentNo || userInfo.value.studentId || ''
  studentProfileForm.realName = payload.realName || userInfo.value.realName || ''
  studentProfileForm.gender = payload.gender || ''
  studentProfileForm.collegeId = payload.collegeId || findCollegeIdByName(payload.collegeName || userInfo.value.college || userInfo.value.collegeName)
  studentProfileForm.major = payload.major || userInfo.value.major || ''
  studentProfileForm.className = payload.className || ''
  studentProfileForm.phone = payload.phone || userInfo.value.phone || ''
  studentProfileForm.email = payload.email || userInfo.value.email || ''
  studentProfileForm.direction = payload.direction || ''
  studentProfileForm.introduction = payload.introduction || ''
  studentProfileForm.attachmentUrl = payload.attachmentUrl || payload.resumeUrl || userInfo.value.resume || ''
  profileAttachmentFiles.value = studentProfileForm.attachmentUrl ? [studentProfileForm.attachmentUrl] : []
}

const fetchStudentProfile = async () => {
  if (!isStudent.value) {
    return
  }
  const profileRes = await getMyStudentProfile()
  applyStudentProfile(profileRes.data || {})

  if (requiresProfileReview.value && profileRes.data?.id) {
    const [reviewRes, archiveRes] = await Promise.all([
      getProfileReviews(profileRes.data.id),
      getProfileArchives(profileRes.data.id)
    ])
    reviewHistory.value = reviewRes.data || []
    archiveHistory.value = archiveRes.data || []
  } else {
    reviewHistory.value = []
    archiveHistory.value = []
  }
}

const refreshStudentProfile = async () => {
  try {
    await fetchStudentProfile()
  } catch (error) {
    ElMessage.error(error.message || '加载成员资料失败')
  }
}

const saveStudentProfile = async () => {
  if (!studentProfileFormRef.value) {
    return
  }
  try {
    await studentProfileFormRef.value.validate()
    studentProfileSaving.value = true
    const response = await saveMyStudentProfile({ ...studentProfileForm })
    applyStudentProfile(response.data || {})
    syncStoreFromProfile(response.data || {})
    await fetchStudentProfile()
    ElMessage.success(requiresProfileReview.value ? '资料草稿已保存' : '资料已保存')
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    studentProfileSaving.value = false
  }
}

const submitStudentProfile = async () => {
  if (!studentProfileFormRef.value) {
    return
  }
  if (!requiresProfileReview.value) {
    ElMessage.info('当前账号未加入实验室，资料保存后直接生效，无需提交审核。')
    return
  }
  try {
    await studentProfileFormRef.value.validate()
    await ElMessageBox.confirm(
      '确认提交当前草稿进入审核吗？审核中将暂时不能修改。',
      '提交资料',
      { type: 'warning' }
    )
    studentProfileSubmitting.value = true
    const saveResponse = await saveMyStudentProfile({ ...studentProfileForm })
    applyStudentProfile(saveResponse.data || {})
    syncStoreFromProfile(saveResponse.data || {})
    const response = await submitMyStudentProfile()
    applyStudentProfile(response.data || {})
    await fetchStudentProfile()
    ElMessage.success('资料已提交审核')
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    studentProfileSubmitting.value = false
  }
}

const syncStoreFromProfile = (payload = {}) => {
  const collegeName = payload.collegeName || findCollegeNameById(studentProfileForm.collegeId) || userInfo.value.college || ''
  const resumeUrl = payload.resumeUrl || payload.attachmentUrl || userInfo.value.resume || ''
  const merged = {
    ...(userInfo.value || {}),
    realName: studentProfileForm.realName,
    studentId: studentProfileForm.studentNo,
    college: collegeName,
    collegeName,
    major: studentProfileForm.major,
    phone: studentProfileForm.phone,
    email: studentProfileForm.email,
    resume: resumeUrl
  }
  userInfo.value = merged
  store.setUserInfo(merged)
}

const openBasicDialog = () => {
  basicForm.realName = userInfo.value.realName || ''
  basicForm.email = userInfo.value.email || ''
  showBasicDialog.value = true
}

const saveBasicInfo = async () => {
  if (!basicFormRef.value) {
    return
  }
  try {
    await basicFormRef.value.validate()
    basicSaving.value = true
    await request.put('/api/user/info', {
      realName: basicForm.realName,
      email: basicForm.email
    })
    userInfo.value = {
      ...(userInfo.value || {}),
      realName: basicForm.realName,
      email: basicForm.email
    }
    store.setUserInfo(userInfo.value)
    showBasicDialog.value = false
    ElMessage.success('基础信息已更新')
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    basicSaving.value = false
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
    ElMessage.success('密码已更新')
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

const toStoredUrl = (file) => file?.rawUrl || file?.response?.data?.url || file?.response?.data?.path || file?.url || ''

const handleAvatarFileChange = (files) => {
  tempAvatarUrl.value = files.length ? toStoredUrl(files[files.length - 1]) : ''
}

const handleAvatarRemove = () => {
  tempAvatarUrl.value = ''
}

const openAvatarDialog = () => {
  avatarFiles.value = []
  tempAvatarUrl.value = ''
  showAvatarDialog.value = true
}

const closeAvatarDialog = () => {
  avatarFiles.value = []
  tempAvatarUrl.value = ''
  showAvatarDialog.value = false
}

const openResumeDialog = () => {
  resumeFiles.value = userInfo.value?.resume ? [userInfo.value.resume] : []
  tempResumeUrl.value = userInfo.value?.resume || ''
  showResumeDialog.value = true
}

const closeResumeDialog = () => {
  resumeFiles.value = []
  tempResumeUrl.value = ''
  showResumeDialog.value = false
}

const saveAvatar = async () => {
  if (!tempAvatarUrl.value) {
    ElMessage.warning('请先上传头像')
    return
  }
  try {
    avatarSaving.value = true
    await request.put('/api/user/avatar', { avatar: tempAvatarUrl.value })
    userInfo.value = {
      ...(userInfo.value || {}),
      avatar: tempAvatarUrl.value
    }
    store.setUserInfo(userInfo.value)
    closeAvatarDialog()
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新头像失败')
  } finally {
    avatarSaving.value = false
  }
}

const handleResumeFileChange = (files) => {
  tempResumeUrl.value = files.length ? toStoredUrl(files[files.length - 1]) : ''
}

const handleResumeRemove = () => {
  tempResumeUrl.value = ''
}

const handleProfileAttachmentChange = (files) => {
  studentProfileForm.attachmentUrl = files.length ? toStoredUrl(files[files.length - 1]) : ''
  studentProfileFormRef.value?.validateField?.('attachmentUrl', () => {})
}

const handleProfileAttachmentRemove = () => {
  studentProfileForm.attachmentUrl = ''
  studentProfileFormRef.value?.validateField?.('attachmentUrl', () => {})
}

const saveResume = async () => {
  if (!tempResumeUrl.value) {
    ElMessage.warning('请先上传简历')
    return
  }
  try {
    resumeSaving.value = true
    await request.put('/api/user/info', { resume: tempResumeUrl.value })
    userInfo.value = {
      ...(userInfo.value || {}),
      resume: tempResumeUrl.value
    }
    store.setUserInfo(userInfo.value)
    if (isStudent.value && !studentProfileForm.attachmentUrl) {
      studentProfileForm.attachmentUrl = tempResumeUrl.value
      profileAttachmentFiles.value = [tempResumeUrl.value]
    }
    closeResumeDialog()
    ElMessage.success('简历已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存简历失败')
  } finally {
    resumeSaving.value = false
  }
}

const statusAlertType = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'ARCHIVED') return 'success'
  if (status === 'REJECTED') return 'error'
  return 'info'
}

const statusHint = (status) => {
  if (status === 'PENDING') {
    return '资料正在审核中，暂时无法编辑。'
  }
  if (status === 'REJECTED') {
    return '上一次提交已被驳回，请修改后重新提交。'
  }
  if (status === 'ARCHIVED') {
    return '最新通过版本已成功归档。'
  }
  return '当前为草稿状态，请完善资料后提交审核。'
}

const formatReviewStatus = (status) => {
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'PENDING') return '待审核'
  return status || '草稿'
}

const timelineType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

const formatDateTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(
    2,
    '0'
  )}`
}

onMounted(async () => {
  try {
    await fetchUserInfo()
    if (isStudent.value) {
      await fetchCollegeOptionList()
      await fetchStudentProfile()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载资料数据失败')
  }
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.account-card,
.resume-card,
.profile-workspace {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 20px;
}

.account-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.account-role,
.account-id {
  color: #64748b;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.workspace-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.workspace-subtitle {
  margin-top: 6px;
  color: #64748b;
}

.summary-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.status-alert,
.resume-body,
.student-profile-form,
.history-section {
  margin-top: 18px;
}

.resume-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-attachment-box {
  width: 100%;
  display: grid;
  gap: 10px;
}

.profile-upload-tip {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.resume-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.resume-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.resume-dialog-copy {
  margin-bottom: 14px;
  color: #475569;
}

.resume-dialog-copy p {
  margin-bottom: 8px;
}

.timeline-title {
  font-weight: 600;
  color: #0f172a;
}

.timeline-text {
  margin-top: 6px;
  color: #475569;
}

@media (max-width: 768px) {
  .summary-card-grid {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: space-between;
  }

  .info-row {
    flex-direction: column;
  }

  .resume-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

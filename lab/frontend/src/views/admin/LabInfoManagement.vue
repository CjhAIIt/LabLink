<template>
  <div class="lab-info-management">
    <TablePageCard title="实验室资料管理" subtitle="维护实验室品牌信息、简介内容与优秀学长展示">
      <el-tabs v-model="activeTab" class="management-tabs">
        <el-tab-pane label="实验室资料" name="info">
          <section class="brand-panel">
            <div class="brand-cover" :style="brandCoverStyle">
              <div class="brand-cover__mask">
                <div class="brand-header">
                  <div class="brand-logo">
                    <img v-if="labLogoPreview" :src="labLogoPreview" alt="实验室 Logo" />
                    <el-icon v-else><OfficeBuilding /></el-icon>
                  </div>
                  <div class="brand-copy">
                    <p class="brand-copy__eyebrow">Lab Brand</p>
                    <h2>{{ labInfo.labName || '实验室品牌展示' }}</h2>
                    <p>{{ labInfo.labDesc || '可在下方完善实验室 Logo、封面图和介绍文案。' }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <el-form ref="labFormRef" :model="labInfo" :rules="labRules" label-width="120px" class="lab-form">
            <div class="media-grid">
              <ImageUploadField
                v-model="labInfo.logoUrl"
                title="实验室 Logo"
                description="支持 JPG、PNG、SVG、WEBP，建议上传透明底 Logo，便于详情页和品牌卡片展示。"
                scene="logo"
                accept=".jpg,.jpeg,.png,.svg,.webp"
                :size-limit="5"
              />
              <ImageUploadField
                v-model="labInfo.coverImageUrl"
                title="实验室封面图"
                description="用于实验室介绍页头图和品牌横幅展示，建议横向海报比例。"
                scene="image"
                accept=".jpg,.jpeg,.png,.webp"
                :size-limit="8"
              />
            </div>

            <div class="form-grid">
              <el-form-item label="实验室名称">
                <el-input v-model="labInfo.labName" disabled />
              </el-form-item>
              <el-form-item label="招新状态" prop="status">
                <el-select v-model="labInfo.status" placeholder="请选择招新状态">
                  <el-option label="招新中" :value="1" />
                  <el-option label="已关闭" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="成员上限" prop="recruitNum">
                <el-input-number
                  v-model="labInfo.recruitNum"
                  :min="Math.max(1, Number(labInfo.currentNum || 0))"
                  :max="500"
                />
              </el-form-item>
              <el-form-item label="当前成员数">
                <el-input :model-value="String(labInfo.currentNum || 0)" disabled />
              </el-form-item>
              <el-form-item label="成立时间" prop="foundingDate">
                <el-date-picker
                  v-model="labInfo.foundingDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item label="指导老师">
                <el-input :model-value="actualTeacherName" disabled />
                <div class="sync-hint">由实验室管理中的“指导教师”实时同步，这里不再手动维护。</div>
              </el-form-item>
              <el-form-item label="当前管理员">
                <el-input :model-value="actualCurrentAdmins" disabled />
                <div class="sync-hint">由管理员安排、移交或撤销动作实时同步，确保展示当前真实管理员。</div>
              </el-form-item>
            </div>

            <el-form-item label="招新要求" prop="requireSkill">
              <el-input
                v-model="labInfo.requireSkill"
                type="textarea"
                :rows="3"
                placeholder="例如：Java、算法、嵌入式、前端开发"
              />
            </el-form-item>
            <el-form-item label="获奖成果" prop="awards">
              <el-input v-model="labInfo.awards" type="textarea" :rows="4" placeholder="填写实验室荣誉、竞赛成果与代表项目" />
            </el-form-item>
            <el-form-item label="基础信息" prop="basicInfo">
              <el-input v-model="labInfo.basicInfo" type="textarea" :rows="4" placeholder="填写实验室定位、研究方向与日常工作说明" />
            </el-form-item>
            <el-form-item label="实验室简介" prop="labDesc">
              <el-input v-model="labInfo.labDesc" type="textarea" :rows="5" placeholder="填写实验室简介，建议突出特色、方向与培养成果" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingLabInfo" @click="saveLabInfo">保存修改</el-button>
              <el-button @click="fetchLabInfo">重新加载</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="优秀学长" name="graduates">
          <div class="operation-bar">
            <div class="operation-copy">
              <h3>优秀学长展示</h3>
              <p>支持上传头像与展示图片，用于实验室详情页、作品集展示页和品牌宣传页回显。</p>
            </div>
            <el-button type="primary" @click="showAddGraduateDialog">新增学长</el-button>
          </div>

          <el-table :data="graduates" border stripe class="graduate-table">
            <el-table-column label="形象图" width="160">
              <template #default="{ row }">
                <div class="graduate-media">
                  <el-avatar :size="46" :src="resolveMedia(row.avatarUrl)">
                    {{ (row.name || 'G').charAt(0) }}
                  </el-avatar>
                  <el-image
                    v-if="row.coverImageUrl"
                    :src="resolveMedia(row.coverImageUrl)"
                    fit="cover"
                    class="graduate-media__cover"
                    :preview-src-list="[resolveMedia(row.coverImageUrl)]"
                    preview-teleported
                  />
                  <div v-else class="graduate-media__placeholder">无展示图</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" width="150" />
            <el-table-column prop="graduationYear" label="毕业年份" width="110" />
            <el-table-column prop="company" label="就业单位" min-width="180" />
            <el-table-column prop="position" label="职位" width="150" />
            <el-table-column label="介绍" min-width="220">
              <template #default="{ row }">
                <span class="graduate-description">{{ row.description || '暂无介绍' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="editGraduate(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteGraduate(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="graduatePagination.current"
              v-model:page-size="graduatePagination.size"
              :total="graduatePagination.total"
              layout="total, prev, pager, next"
              @current-change="fetchGraduates"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </TablePageCard>

    <el-dialog v-model="graduateDialog.visible" :title="graduateDialog.title" width="760px" :close-on-click-modal="false">
      <el-form ref="graduateFormRef" :model="graduateForm" :rules="graduateRules" label-width="100px">
        <div class="media-grid compact">
          <ImageUploadField
            v-model="graduateForm.avatarUrl"
            title="头像图片"
            description="用于列表卡片和详情页头像展示。"
            scene="avatar"
            accept=".jpg,.jpeg,.png,.webp"
            :size-limit="5"
          />
          <ImageUploadField
            v-model="graduateForm.coverImageUrl"
            title="展示图片"
            description="可选，用于详情页海报或展示材料回显。"
            scene="image"
            accept=".jpg,.jpeg,.png,.webp"
            :size-limit="8"
          />
        </div>

        <div class="form-grid">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="graduateForm.name" />
          </el-form-item>
          <el-form-item label="专业" prop="major">
            <el-input v-model="graduateForm.major" />
          </el-form-item>
          <el-form-item label="毕业年份" prop="graduationYear">
            <el-date-picker
              v-model="graduateForm.graduationYear"
              type="year"
              value-format="YYYY"
              placeholder="选择年份"
            />
          </el-form-item>
          <el-form-item label="就业单位" prop="company">
            <el-input v-model="graduateForm.company" />
          </el-form-item>
          <el-form-item label="职位" prop="position">
            <el-input v-model="graduateForm.position" />
          </el-form-item>
        </div>

        <el-form-item label="介绍" prop="description">
          <el-input
            v-model="graduateForm.description"
            type="textarea"
            :rows="4"
            placeholder="填写毕业去向、成长经历或对实验室的贡献亮点"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="graduateDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="graduateSaving" @click="saveGraduate">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OfficeBuilding } from '@element-plus/icons-vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import ImageUploadField from '@/components/common/ImageUploadField.vue'
import request from '@/utils/request'
import { getLabById, updateManagedLab } from '@/api/lab'
import { addGraduate, deleteGraduate as deleteGraduateApi, getGraduateList, updateGraduate } from '@/api/graduate'
import { useUserStore } from '@/stores/user'
import { resolveFileUrl } from '@/utils/file'

const userStore = useUserStore()

const activeTab = ref('info')
const labId = ref(null)
const savingLabInfo = ref(false)
const graduateSaving = ref(false)
const labFormRef = ref(null)
const graduateFormRef = ref(null)

const labInfo = reactive({
  id: '',
  labName: '',
  labDesc: '',
  requireSkill: '',
  recruitNum: 1,
  currentNum: 0,
  status: 1,
  foundingDate: '',
  teacherName: '',
  advisors: '',
  currentAdmins: '',
  awards: '',
  basicInfo: '',
  logoUrl: '',
  coverImageUrl: ''
})

const labRules = {
  status: [{ required: true, message: '请选择招新状态', trigger: 'change' }],
  recruitNum: [{ required: true, message: '请输入成员上限', trigger: 'blur' }],
  requireSkill: [{ required: true, message: '请输入招新要求', trigger: 'blur' }],
  labDesc: [{ required: true, message: '请输入实验室简介', trigger: 'blur' }]
}

const graduates = ref([])
const graduatePagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const graduateDialog = reactive({
  visible: false,
  title: '新增优秀学长',
  isEdit: false
})

const graduateForm = reactive({
  id: '',
  labId: '',
  name: '',
  major: '',
  graduationYear: '',
  company: '',
  position: '',
  description: '',
  avatarUrl: '',
  coverImageUrl: ''
})

const graduateRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const labLogoPreview = computed(() => resolveMedia(labInfo.logoUrl))
const actualTeacherName = computed(() => labInfo.teacherName || labInfo.advisors || '暂无')
const actualCurrentAdmins = computed(() => labInfo.currentAdmins || '暂无')
const brandCoverStyle = computed(() => {
  const coverUrl = resolveMedia(labInfo.coverImageUrl)
  return {
    backgroundImage: coverUrl
      ? `linear-gradient(135deg, rgba(15, 23, 42, 0.72), rgba(15, 118, 110, 0.58)), url(${coverUrl})`
      : 'linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(14, 116, 144, 0.86), rgba(20, 184, 166, 0.76))'
  }
})

const resolveMedia = (value) => resolveFileUrl(value)

const resolveLabId = async () => {
  if (userStore.userInfo?.managedLabId || userStore.userInfo?.labId) {
    return userStore.userInfo.managedLabId || userStore.userInfo.labId
  }

  const response = await request.get('/api/access/profile')
  const nextUserInfo = {
    ...(userStore.userInfo || {}),
    ...response.data
  }
  userStore.setUserInfo(nextUserInfo)
  return nextUserInfo.managedLabId || nextUserInfo.labId || null
}

const fetchLabInfo = async () => {
  if (!labId.value) {
    return
  }

  const response = await getLabById(labId.value)
  Object.assign(labInfo, {
    id: response.data.id || '',
    labName: response.data.labName || '',
    labDesc: response.data.labDesc || '',
    requireSkill: response.data.requireSkill || '',
    recruitNum: Number(response.data.recruitNum || 1),
    currentNum: Number(response.data.currentNum || 0),
    status: Number(response.data.status || 1),
    foundingDate: response.data.foundingDate || '',
    teacherName: response.data.teacherName || '',
    advisors: response.data.advisors || '',
    currentAdmins: response.data.currentAdmins || '',
    awards: response.data.awards || '',
    basicInfo: response.data.basicInfo || '',
    logoUrl: response.data.logoUrl || '',
    coverImageUrl: response.data.coverImageUrl || ''
  })
}

const saveLabInfo = async () => {
  if (!labFormRef.value) {
    return
  }

  await labFormRef.value.validate()

  try {
    savingLabInfo.value = true
    await updateManagedLab({
      id: labInfo.id,
      labDesc: labInfo.labDesc,
      requireSkill: labInfo.requireSkill,
      recruitNum: Number(labInfo.recruitNum || 0),
      status: Number(labInfo.status || 2),
      foundingDate: labInfo.foundingDate || null,
      awards: labInfo.awards || '',
      basicInfo: labInfo.basicInfo || '',
      logoUrl: labInfo.logoUrl || '',
      coverImageUrl: labInfo.coverImageUrl || ''
    })
    ElMessage.success('实验室信息已提交更新')
    await fetchLabInfo()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingLabInfo.value = false
  }
}

const fetchGraduates = async () => {
  if (!labId.value) {
    return
  }

  const response = await getGraduateList({
    pageNum: graduatePagination.current,
    pageSize: graduatePagination.size,
    labId: labId.value
  })
  graduates.value = response.data.records || []
  graduatePagination.total = response.data.total || 0
}

const resetGraduateForm = () => {
  Object.assign(graduateForm, {
    id: '',
    labId: labId.value,
    name: '',
    major: '',
    graduationYear: '',
    company: '',
    position: '',
    description: '',
    avatarUrl: '',
    coverImageUrl: ''
  })
  graduateFormRef.value?.clearValidate()
}

const showAddGraduateDialog = () => {
  graduateDialog.visible = true
  graduateDialog.title = '新增优秀学长'
  graduateDialog.isEdit = false
  resetGraduateForm()
}

const editGraduate = (row) => {
  graduateDialog.visible = true
  graduateDialog.title = '编辑优秀学长'
  graduateDialog.isEdit = true
  Object.assign(graduateForm, {
    id: row.id || '',
    labId: row.labId || labId.value,
    name: row.name || '',
    major: row.major || '',
    graduationYear: row.graduationYear || '',
    company: row.company || '',
    position: row.position || '',
    description: row.description || '',
    avatarUrl: row.avatarUrl || '',
    coverImageUrl: row.coverImageUrl || ''
  })
  graduateFormRef.value?.clearValidate()
}

const saveGraduate = async () => {
  if (!graduateFormRef.value) {
    return
  }

  try {
    await graduateFormRef.value.validate()
    graduateSaving.value = true

    const payload = {
      ...graduateForm,
      labId: labId.value,
      avatarUrl: graduateForm.avatarUrl || '',
      coverImageUrl: graduateForm.coverImageUrl || ''
    }

    if (graduateDialog.isEdit) {
      await updateGraduate(payload)
    } else {
      await addGraduate(payload)
    }

    ElMessage.success('保存成功')
    graduateDialog.visible = false
    await fetchGraduates()
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    graduateSaving.value = false
  }
}

const deleteGraduate = async (row) => {
  await ElMessageBox.confirm(`确定删除优秀学长“${row.name}”吗？`, '提示', { type: 'warning' })
  await deleteGraduateApi(row.id)
  ElMessage.success('删除成功')
  await fetchGraduates()
}

onMounted(async () => {
  try {
    labId.value = await resolveLabId()
    if (!labId.value) {
      ElMessage.error('当前管理员未绑定实验室')
      return
    }

    await fetchLabInfo()
    try {
      await fetchGraduates()
    } catch (error) {
      graduates.value = []
      graduatePagination.total = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载实验室信息失败')
  }
})
</script>

<style scoped>
.lab-info-management {
  padding: 0;
}

.management-tabs {
  min-height: 640px;
}

.brand-panel {
  margin-bottom: 24px;
}

.brand-cover {
  min-height: 220px;
  border-radius: 28px;
  overflow: hidden;
  background-position: center;
  background-size: cover;
  border: 1px solid rgba(226, 232, 240, 0.72);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
}

.brand-cover__mask {
  min-height: 220px;
  padding: 28px;
  display: flex;
  align-items: flex-end;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.brand-logo {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #fff;
  font-size: 32px;
  backdrop-filter: blur(14px);
  flex-shrink: 0;
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-copy {
  color: #f8fafc;
}

.brand-copy__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.brand-copy h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
}

.brand-copy p {
  margin: 10px 0 0;
  max-width: 720px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.84);
}

.lab-form {
  max-width: 960px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.media-grid.compact {
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.sync-hint {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.operation-copy h3 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.operation-copy p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.graduate-table {
  border-radius: 18px;
  overflow: hidden;
}

.graduate-media {
  display: flex;
  align-items: center;
  gap: 10px;
}

.graduate-media__cover {
  width: 72px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid rgba(226, 232, 240, 0.86);
}

.graduate-media__placeholder {
  min-width: 72px;
  height: 48px;
  padding: 0 10px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed rgba(148, 163, 184, 0.44);
  color: #94a3b8;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graduate-description {
  color: #475569;
  line-height: 1.6;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .media-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .operation-bar,
  .brand-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<template>
  <div class="lab-info-management">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="实验室信息" name="info">
          <el-form ref="labFormRef" :model="labInfo" :rules="labRules" label-width="120px" class="lab-form">
            <el-form-item label="实验室名称">
              <el-input v-model="labInfo.labName" disabled />
            </el-form-item>
            <el-form-item label="招新状态" prop="status">
              <el-select v-model="labInfo.status" placeholder="请选择招新状态" style="width: 220px">
                <el-option label="招新中" :value="1" />
                <el-option label="已关闭" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="成员总数上限" prop="recruitNum">
              <el-input-number
                v-model="labInfo.recruitNum"
                :min="Math.max(1, Number(labInfo.currentNum || 0))"
                :max="500"
              />
              <span class="field-hint">当前正式成员数不能超过上限</span>
            </el-form-item>
            <el-form-item label="当前正式成员数">
              <el-input :model-value="String(labInfo.currentNum || 0)" disabled />
            </el-form-item>
            <el-form-item label="技能要求" prop="requireSkill">
              <el-input
                v-model="labInfo.requireSkill"
                type="textarea"
                :rows="3"
                placeholder="例如：Java、算法、嵌入式、前端开发"
              />
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
            <el-form-item label="指导老师" prop="advisors">
              <el-input v-model="labInfo.advisors" placeholder="多个老师请用顿号或逗号分隔" />
            </el-form-item>
            <el-form-item label="当前管理员" prop="currentAdmins">
              <el-input v-model="labInfo.currentAdmins" placeholder="多个管理员请用顿号或逗号分隔" />
            </el-form-item>
            <el-form-item label="获奖情况" prop="awards">
              <el-input v-model="labInfo.awards" type="textarea" :rows="4" placeholder="填写实验室获奖情况" />
            </el-form-item>
            <el-form-item label="基础信息" prop="basicInfo">
              <el-input v-model="labInfo.basicInfo" type="textarea" :rows="4" placeholder="填写实验室基础信息" />
            </el-form-item>
            <el-form-item label="实验室简介" prop="labDesc">
              <el-input v-model="labInfo.labDesc" type="textarea" :rows="5" placeholder="填写实验室简介" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingLabInfo" @click="saveLabInfo">保存修改</el-button>
              <el-button @click="fetchLabInfo">重新加载</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="优秀毕业生" name="graduates">
          <div class="operation-bar">
            <el-button type="primary" @click="showAddGraduateDialog">添加毕业生</el-button>
          </div>

          <el-table :data="graduates" border stripe>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" width="150" />
            <el-table-column prop="graduationYear" label="毕业年份" width="110" />
            <el-table-column prop="company" label="就业单位" min-width="180" />
            <el-table-column prop="position" label="职位" width="150" />
            <el-table-column label="操作" width="160">
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
    </el-card>

    <el-dialog v-model="graduateDialog.visible" :title="graduateDialog.title" width="50%">
      <el-form ref="graduateFormRef" :model="graduateForm" :rules="graduateRules" label-width="100px">
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
        <el-form-item label="介绍" prop="description">
          <el-input v-model="graduateForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="graduateDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="saveGraduate">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getLabById, updateLabInfo } from '@/api/lab'
import { addGraduate, deleteGraduate as deleteGraduateApi, getGraduateList, updateGraduate } from '@/api/graduate'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const activeTab = ref('info')
const labId = ref(null)
const savingLabInfo = ref(false)
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
  advisors: '',
  currentAdmins: '',
  awards: '',
  basicInfo: ''
})

const labRules = {
  status: [{ required: true, message: '请选择招新状态', trigger: 'change' }],
  recruitNum: [{ required: true, message: '请输入成员总数上限', trigger: 'blur' }],
  requireSkill: [{ required: true, message: '请输入技能要求', trigger: 'blur' }],
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
  title: '添加毕业生',
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
  avatarUrl: ''
})

const graduateRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const resolveLabId = async () => {
  if (userStore.userInfo?.labId) {
    return userStore.userInfo.labId
  }

  const response = await request.get('/api/access/profile')
  const nextUserInfo = {
    ...(userStore.userInfo || {}),
    ...response.data
  }
  userStore.setUserInfo(nextUserInfo)
  return nextUserInfo.labId || null
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
    advisors: response.data.advisors || '',
    currentAdmins: response.data.currentAdmins || '',
    awards: response.data.awards || '',
    basicInfo: response.data.basicInfo || ''
  })
}

const saveLabInfo = async () => {
  if (!labFormRef.value) {
    return
  }

  await labFormRef.value.validate()

  try {
    savingLabInfo.value = true
    await updateLabInfo({
      id: labInfo.id,
      labDesc: labInfo.labDesc,
      requireSkill: labInfo.requireSkill,
      recruitNum: Number(labInfo.recruitNum || 0),
      status: Number(labInfo.status || 2),
      foundingDate: labInfo.foundingDate || null,
      advisors: labInfo.advisors || '',
      currentAdmins: labInfo.currentAdmins || '',
      awards: labInfo.awards || '',
      basicInfo: labInfo.basicInfo || ''
    })
    ElMessage.success('实验室信息已更新')
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

const showAddGraduateDialog = () => {
  graduateDialog.visible = true
  graduateDialog.title = '添加毕业生'
  graduateDialog.isEdit = false
  Object.assign(graduateForm, {
    id: '',
    labId: labId.value,
    name: '',
    major: '',
    graduationYear: '',
    company: '',
    position: '',
    description: '',
    avatarUrl: ''
  })
  graduateFormRef.value?.clearValidate()
}

const editGraduate = (row) => {
  graduateDialog.visible = true
  graduateDialog.title = '编辑毕业生'
  graduateDialog.isEdit = true
  Object.assign(graduateForm, {
    ...row
  })
  graduateFormRef.value?.clearValidate()
}

const saveGraduate = async () => {
  if (!graduateFormRef.value) {
    return
  }

  await graduateFormRef.value.validate()

  const payload = {
    ...graduateForm,
    labId: labId.value
  }

  if (graduateDialog.isEdit) {
    await updateGraduate(payload)
  } else {
    await addGraduate(payload)
  }

  ElMessage.success('保存成功')
  graduateDialog.visible = false
  fetchGraduates()
}

const deleteGraduate = async (row) => {
  await ElMessageBox.confirm(`确定删除毕业生“${row.name}”吗？`, '提示', {
    type: 'warning'
  })

  await deleteGraduateApi(row.id)
  ElMessage.success('删除成功')
  fetchGraduates()
}

onMounted(async () => {
  try {
    labId.value = await resolveLabId()
    if (!labId.value) {
      ElMessage.error('当前管理员未绑定实验室')
      return
    }

    await Promise.all([fetchLabInfo(), fetchGraduates()])
  } catch (error) {
    ElMessage.error(error.message || '加载实验室信息失败')
  }
})
</script>

<style scoped>
.lab-info-management {
  padding: 0;
}

.lab-form {
  max-width: 860px;
}

.field-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.operation-bar {
  margin-bottom: 20px;
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
</style>

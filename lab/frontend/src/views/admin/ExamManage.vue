<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">笔试管理</p>
          <h2>笔试创建与发布管理</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadExams">刷新</el-button>
          <el-button type="primary" @click="openDialog()">创建笔试</el-button>
        </div>
      </div>
    </section>

    <TablePageCard title="笔试列表" subtitle="笔试中心" :count-label="`${pagination.total} 条`">
      <el-table v-loading="loading" :data="exams" stripe>
        <el-table-column prop="title" label="笔试标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="labName" label="所属实验室" min-width="140" />
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">{{ fmtDT(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="170">
          <template #default="{ row }">{{ fmtDT(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分)" min-width="100" />
        <el-table-column prop="totalScore" label="总分" min-width="80" />
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <StatusTag
              :value="row.status"
              :label-map="statusLabel"
              :type-map="statusType"
            />
          </template>
        </el-table-column>
        <el-table-column prop="participantCount" label="参与人数" min-width="100" />
        <el-table-column label="操作" fixed="right" min-width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 'draft'" link type="success" @click="handlePublish(row)">发布</el-button>
            <el-button link type="primary" @click="handleViewStats(row)">统计</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </template>
    </TablePageCard>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑笔试' : '创建笔试'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="笔试标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入笔试标题" />
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="所属实验室" prop="labId">
            <el-select v-model="form.labId" placeholder="请选择实验室">
              <el-option v-for="lab in labOptions" :key="lab.id" :label="lab.labName" :value="lab.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="考试时长" prop="duration">
            <el-input-number v-model="form.duration" :min="1" :max="600" placeholder="分钟" style="width:100%" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width:100%" />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width:100%" />
          </el-form-item>
          <el-form-item label="总分" prop="totalScore">
            <el-input-number v-model="form.totalScore" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="及格分" prop="passScore">
            <el-input-number v-model="form.passScore" :min="0" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="笔试描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入笔试描述" />
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="防作弊">
            <el-switch v-model="form.enableAntiCheat" />
          </el-form-item>
          <el-form-item label="签名确认">
            <el-switch v-model="form.enableSignature" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getAdminExamList, createAdminExam, updateAdminExam, deleteAdminExam, publishAdminExam } from '@/api/writtenExam'
import { getLabPage } from '@/api/lab'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const exams = ref([])
const labOptions = ref([])

const statusLabel = { draft: '草稿', published: '已发布', ongoing: '进行中', ended: '已结束' }
const statusType = { draft: 'info', published: 'success', ongoing: 'warning', ended: '' }

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const defaultForm = () => ({
  id: null, title: '', labId: undefined, description: '', startTime: '', endTime: '',
  duration: 60, totalScore: 100, passScore: 60, enableAntiCheat: false, enableSignature: false
})
const form = reactive(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入笔试标题', trigger: 'blur' }],
  labId: [{ required: true, message: '请选择实验室', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请输入总分', trigger: 'blur' }]
}

const fmtDT = (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-')

const loadLabOptions = async () => {
  const res = await getLabPage({ pageNum: 1, pageSize: 200 })
  labOptions.value = res.data?.records || []
}

const loadExams = async () => {
  loading.value = true
  try {
    const res = await getAdminExamList({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    exams.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally { loading.value = false }
}
const openDialog = (row) => {
  Object.assign(form, defaultForm())
  if (row) Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateAdminExam(form.id, payload)
    } else {
      await createAdminExam(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadExams()
  } finally { saving.value = false }
}

const handlePublish = async (row) => {
  await ElMessageBox.confirm(`确认发布笔试"${row.title}"吗？发布后学生可见。`, '发布确认', { type: 'warning' })
  await publishAdminExam(row.id)
  ElMessage.success('发布成功')
  await loadExams()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除笔试"${row.title}"吗？`, '删除确认', { type: 'warning' })
  await deleteAdminExam(row.id)
  ElMessage.success('删除成功')
  await loadExams()
}

const handleViewStats = (row) => {
  ElMessage.info(`查看笔试"${row.title}"的统计数据`)
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadExams()
}

onMounted(() => Promise.all([loadLabOptions(), loadExams()]))
</script>

<style scoped>
.two-column-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
@media (max-width: 768px) {
  .two-column-form { grid-template-columns: 1fr; }
}
</style>

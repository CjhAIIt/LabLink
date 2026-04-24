<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">题库管理</p>
          <h2>笔试题目维护与管理</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadQuestions">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增题目</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="题型">
          <el-select v-model="filters.questionType" clearable placeholder="全部题型" style="width:130px">
            <el-option v-for="(lbl, key) in typeLabels" :key="key" :label="lbl" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="filters.difficulty" clearable placeholder="全部难度" style="width:120px">
            <el-option v-for="(lbl, key) in diffLabels" :key="key" :label="lbl" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="题目标题" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadQuestions">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <TablePageCard title="题目列表" subtitle="题库中心" :count-label="`${pagination.total} 条`">
      <div v-loading="loading" class="exam-mobile-list mobile-only">
        <article v-for="row in questions" :key="row.id" class="exam-mobile-card">
          <div class="exam-mobile-card__head">
            <div>
              <strong>{{ row.title || '未命名题目' }}</strong>
              <span>{{ typeLabels[row.questionType] || row.questionType }} · {{ diffLabels[row.difficulty] || row.difficulty }}</span>
            </div>
            <el-tag :type="diffType[row.difficulty] || ''" size="small">{{ diffLabels[row.difficulty] || row.difficulty }}</el-tag>
          </div>
          <div class="exam-mobile-card__meta">
            <span>分值 {{ row.score ?? '-' }}</span>
            <span>归属 {{ row.labName || '公共' }}</span>
            <span>标签 {{ row.tags || '未设置' }}</span>
          </div>
          <p>{{ row.content || '暂无题目内容' }}</p>
          <div class="exam-mobile-card__actions">
            <el-button plain type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button plain type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </article>
        <el-empty v-if="!loading && !questions.length" description="暂无题目" />
      </div>
      <el-table v-loading="loading" :data="questions" stripe class="desktop-only">
        <el-table-column prop="title" label="题目标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="题型" min-width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabels[row.questionType] || row.questionType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" min-width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="diffType[row.difficulty] || ''">{{ diffLabels[row.difficulty] || row.difficulty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" min-width="80" />
        <el-table-column label="归属" min-width="120">
          <template #default="{ row }">{{ row.labName || '公共' }}</template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" min-width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑题目' : '新增题目'" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" />
        </el-form-item>
        <el-form-item label="题目内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入题目内容" />
        </el-form-item>
        <div class="two-column-form">
          <el-form-item label="题型" prop="questionType">
            <el-select v-model="form.questionType" placeholder="请选择题型">
              <el-option v-for="(lbl, key) in typeLabels" :key="key" :label="lbl" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="form.difficulty" placeholder="请选择难度">
              <el-option v-for="(lbl, key) in diffLabels" :key="key" :label="lbl" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item label="分值" prop="score">
            <el-input-number v-model="form.score" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </div>
        <!-- 单选题选项 -->
        <template v-if="form.questionType === 'single_choice'">
          <el-form-item v-for="opt in optionKeys" :key="opt" :label="`选项${opt}`">
            <el-input v-model="form.options[opt]" :placeholder="`请输入选项${opt}内容`" />
          </el-form-item>
          <el-form-item label="正确答案" prop="correctAnswer">
            <el-select v-model="form.correctAnswer" placeholder="请选择正确答案">
              <el-option v-for="opt in optionKeys" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
        </template>
        <!-- 填空题 -->
        <template v-if="form.questionType === 'fill_blank'">
          <el-form-item label="正确答案" prop="correctAnswer">
            <el-input v-model="form.correctAnswer" placeholder="请输入正确答案" />
          </el-form-item>
        </template>
        <!-- 编程题 -->
        <template v-if="form.questionType === 'programming'">
          <el-form-item label="允许语言">
            <el-checkbox-group v-model="form.allowedLanguages">
              <el-checkbox label="java">Java</el-checkbox>
              <el-checkbox label="python">Python</el-checkbox>
              <el-checkbox label="cpp">C++</el-checkbox>
              <el-checkbox label="javascript">JavaScript</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <div class="two-column-form">
            <el-form-item label="示例输入">
              <el-input v-model="form.sampleInput" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="示例输出">
              <el-input v-model="form.sampleOutput" type="textarea" :rows="3" />
            </el-form-item>
          </div>
          <el-form-item label="测试用例">
            <el-input v-model="form.testCases" type="textarea" :rows="4" placeholder="JSON格式的测试用例" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getAdminQuestionList, createAdminQuestion, updateAdminQuestion, deleteAdminQuestion } from '@/api/writtenExam'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const questions = ref([])

const typeLabels = { single_choice: '单选题', fill_blank: '填空题', programming: '编程题' }
const diffLabels = { easy: '简单', medium: '中等', hard: '困难' }
const diffType = { easy: 'success', medium: 'warning', hard: 'danger' }
const optionKeys = ['A', 'B', 'C', 'D']

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive({ questionType: '', difficulty: '', keyword: '' })

const defaultForm = () => ({
  id: null, title: '', content: '', questionType: 'single_choice', difficulty: 'easy',
  score: 5, tags: '', correctAnswer: '', options: { A: '', B: '', C: '', D: '' },
  allowedLanguages: [], sampleInput: '', sampleOutput: '', testCases: ''
})
const form = reactive(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  questionType: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
}

const loadQuestions = async () => {
  loading.value = true
  try {
    const res = await getAdminQuestionList({
      pageNum: pagination.pageNum, pageSize: pagination.pageSize,
      questionType: filters.questionType || undefined,
      difficulty: filters.difficulty || undefined,
      keyword: filters.keyword || undefined
    })
    questions.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally { loading.value = false }
}

const openDialog = (row) => {
  Object.assign(form, defaultForm())
  if (row) Object.assign(form, { ...row, options: row.options ? { ...row.options } : { A: '', B: '', C: '', D: '' } })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) { await updateAdminQuestion(form.id, payload) }
    else { await createAdminQuestion(payload) }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadQuestions()
  } finally { saving.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除题目"${row.title}"吗？`, '删除确认', { type: 'warning' })
  await deleteAdminQuestion(row.id)
  ElMessage.success('删除成功')
  await loadQuestions()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadQuestions()
}

onMounted(() => loadQuestions())
</script>

<style scoped>
.exam-mobile-list {
  display: grid;
  gap: 12px;
}

.exam-mobile-card {
  padding: 15px;
  display: grid;
  gap: 12px;
  border-radius: 22px;
  border: 1px solid rgba(51, 136, 187, 0.12);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(23, 32, 51, 0.075);
}

.exam-mobile-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.exam-mobile-card__head strong {
  display: block;
  color: #172033;
  font-size: 16px;
}

.exam-mobile-card__head span,
.exam-mobile-card p,
.exam-mobile-card__meta {
  color: #64748b;
}

.exam-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.exam-mobile-card__meta span {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fafc;
}

.exam-mobile-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.exam-mobile-card__actions .el-button {
  flex: 1 1 0;
}

.two-column-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
@media (max-width: 768px) {
  .two-column-form { grid-template-columns: 1fr; }
}
</style>

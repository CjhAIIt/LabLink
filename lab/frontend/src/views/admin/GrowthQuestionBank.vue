<template>
  <div class="growth-bank-page">
    <TablePageCard title="Growth Question Bank" subtitle="Shared Practice Questions" :count-label="`${pagination.total} items`">
      <template #header-extra>
        <div class="header-row">
          <div>
            <strong>成长中心共享题库</strong>
            <p>这里维护练习题库，正式笔试的题目也从这里选择。</p>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="openCreateDialog">新增题目</el-button>
          </div>
        </div>
      </template>

      <template #filters>
      <div class="toolbar">
        <el-select v-model="filters.trackCode" clearable placeholder="方向" style="width: 180px">
          <el-option
            v-for="track in trackOptions"
            :key="track.id"
            :label="track.name"
            :value="track.id"
          />
        </el-select>
        <el-select v-model="filters.questionType" clearable placeholder="题型" style="width: 160px">
          <el-option label="单选题" value="single_choice" />
          <el-option label="填空题" value="fill_blank" />
          <el-option label="编程题" value="programming" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索标题或描述"
          style="width: 280px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      </template>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="title" label="题目" min-width="260" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="120">
          <template #default="{ row }">
            {{ questionTypeLabelMap[row.questionType] || row.questionType }}
          </template>
        </el-table-column>
        <el-table-column label="方向" width="180">
          <template #default="{ row }">
            {{ getTrackLabel(row.trackCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column label="标签" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="tag in normalizeTags(row.tags).slice(0, 4)"
              :key="`${row.id}-${tag}`"
              size="small"
              effect="plain"
              style="margin-right: 6px"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row.id)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="removeQuestion(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </template>
    </TablePageCard>

    <el-dialog v-model="editor.visible" :title="editor.id ? '编辑题目' : '新增题目'" width="980px">
      <el-form label-position="top" class="editor-form">
        <el-form-item label="题型">
          <el-radio-group v-model="editor.form.questionType">
            <el-radio-button label="single_choice">单选题</el-radio-button>
            <el-radio-button label="fill_blank">填空题</el-radio-button>
            <el-radio-button label="programming">编程题</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="方向">
          <el-select v-model="editor.form.trackCode" placeholder="请选择方向">
            <el-option
              v-for="track in trackOptions"
              :key="track.id"
              :label="track.name"
              :value="track.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="难度">
          <el-select v-model="editor.form.difficulty" placeholder="请选择难度">
            <el-option label="简单" value="简单" />
            <el-option label="中等" value="中等" />
            <el-option label="困难" value="困难" />
          </el-select>
        </el-form-item>

        <el-form-item label="题目标题">
          <el-input v-model="editor.form.title" placeholder="请输入题目标题" />
        </el-form-item>

        <el-form-item label="题目描述">
          <el-input
            v-model="editor.form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题干、背景或补充说明"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-input
            v-model="editor.form.tagsText"
            placeholder="使用英文逗号分隔，例如：数组, 双指针, C语言"
          />
        </el-form-item>

        <el-form-item label="解题提示">
          <el-input
            v-model="editor.form.analysisHint"
            type="textarea"
            :rows="3"
            placeholder="可选，用于成长中心题目详情和 AI 分析提示"
          />
        </el-form-item>

        <template v-if="editor.form.questionType === 'single_choice'">
          <div class="block">
            <div class="block-head">
              <strong>选项设置</strong>
              <el-button size="small" @click="addChoiceOption">新增选项</el-button>
            </div>
            <div
              v-for="(option, index) in editor.form.options"
              :key="`option-${index}`"
              class="option-row"
            >
              <div class="option-label">{{ option.label }}</div>
              <el-input v-model="option.text" :placeholder="`请输入选项 ${option.label}`" />
              <el-radio v-model="editor.form.correctAnswer" :label="option.label">正确答案</el-radio>
              <el-button
                v-if="editor.form.options.length > 2"
                type="danger"
                link
                @click="removeChoiceOption(index)"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>

        <template v-else-if="editor.form.questionType === 'fill_blank'">
          <el-form-item label="可接受答案">
            <el-select
              v-model="editor.form.acceptableAnswers"
              multiple
              allow-create
              filterable
              default-first-option
              placeholder="输入一个或多个可接受答案"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="允许语言">
            <el-checkbox-group v-model="editor.form.allowedLanguages">
              <el-checkbox label="c">C</el-checkbox>
              <el-checkbox label="cpp">C++</el-checkbox>
              <el-checkbox label="java">Java</el-checkbox>
              <el-checkbox label="python">Python</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="输入格式">
            <el-input v-model="editor.form.inputFormat" type="textarea" :rows="3" />
          </el-form-item>

          <el-form-item label="输出格式">
            <el-input v-model="editor.form.outputFormat" type="textarea" :rows="3" />
          </el-form-item>

          <div class="sample-grid">
            <el-form-item label="样例输入">
              <el-input v-model="editor.form.sampleInput" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item label="样例输出">
              <el-input v-model="editor.form.sampleOutput" type="textarea" :rows="4" />
            </el-form-item>
          </div>

          <div class="block">
            <div class="block-head">
              <strong>判题用例</strong>
              <el-button size="small" @click="addJudgeCase">新增用例</el-button>
            </div>
            <div
              v-for="(judgeCase, index) in editor.form.judgeCases"
              :key="`case-${index}`"
              class="judge-case-row"
            >
              <el-input v-model="judgeCase.input" type="textarea" :rows="3" placeholder="输入" />
              <el-input v-model="judgeCase.output" type="textarea" :rows="3" placeholder="期望输出" />
              <el-button
                v-if="editor.form.judgeCases.length > 1"
                type="danger"
                link
                @click="removeJudgeCase(index)"
              >
                删除
              </el-button>
            </div>
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="editor.visible = false">取消</el-button>
        <el-button type="primary" :loading="editor.saving" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TablePageCard from '@/components/common/TablePageCard.vue'
import {
  deleteAdminGrowthQuestion,
  getAdminGrowthQuestionBank,
  getAdminGrowthQuestionDetail,
  saveAdminGrowthQuestion
} from '@/api/growthCenter'
import { getTrackById, gradPathTracks } from '@/constants/gradPath'

const trackOptions = [{ id: 'common', name: '公共题库' }, ...gradPathTracks]
const questionTypeLabelMap = {
  single_choice: '单选题',
  fill_blank: '填空题',
  programming: '编程题'
}

const loading = ref(false)
const rows = ref([])
let listRequestId = 0
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const filters = reactive({
  trackCode: '',
  questionType: '',
  keyword: ''
})
const editor = reactive({
  visible: false,
  id: null,
  saving: false,
  form: createEmptyQuestion()
})

function createEmptyQuestion() {
  return {
    questionType: 'programming',
    trackCode: 'common',
    difficulty: '中等',
    title: '',
    content: '',
    tagsText: '',
    analysisHint: '',
    options: [
      { label: 'A', text: '' },
      { label: 'B', text: '' },
      { label: 'C', text: '' },
      { label: 'D', text: '' }
    ],
    correctAnswer: 'A',
    acceptableAnswers: [],
    allowedLanguages: ['c', 'cpp', 'java', 'python'],
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    judgeCases: [{ input: '', output: '' }]
  }
}

const fetchList = async () => {
  const requestId = ++listRequestId
  const keyword = filters.keyword.trim()
  loading.value = true
  try {
    const res = await getAdminGrowthQuestionBank({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      trackCode: filters.trackCode || undefined,
      questionType: filters.questionType || undefined,
      keyword: keyword || undefined
    })
    if (requestId !== listRequestId) {
      return
    }
    rows.value = res.data.records || []
    pagination.total = res.data.total || 0
  } finally {
    if (requestId === listRequestId) {
      loading.value = false
    }
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const openCreateDialog = () => {
  editor.id = null
  editor.form = createEmptyQuestion()
  editor.visible = true
}

const openEditDialog = async (questionId) => {
  const res = await getAdminGrowthQuestionDetail(questionId)
  const data = res.data || {}
  const sampleCase = parseSampleCase(data.sampleCase)
  editor.id = questionId
  editor.form = {
    questionType: data.questionType || 'programming',
    trackCode: data.trackCode || 'common',
    difficulty: data.difficulty || '中等',
    title: data.title || '',
    content: data.content || '',
    tagsText: normalizeTags(data.tags).join(', '),
    analysisHint: data.analysisHint || '',
    options: normalizeOptions(data.options),
    correctAnswer: data.correctAnswer || 'A',
    acceptableAnswers: normalizeArray(data.acceptableAnswers),
    allowedLanguages: normalizeArray(data.allowedLanguages, ['c', 'cpp', 'java', 'python']),
    inputFormat: data.inputFormat || '',
    outputFormat: data.outputFormat || '',
    sampleInput: sampleCase.input,
    sampleOutput: sampleCase.output,
    judgeCases: normalizeJudgeCases(data.judgeCases)
  }
  editor.visible = true
}

const saveQuestion = async () => {
  if (!editor.form.title.trim()) {
    ElMessage.warning('请输入题目标题')
    return
  }

  if (editor.form.questionType === 'single_choice') {
    const hasEmptyOption = editor.form.options.some((item) => !item.text.trim())
    if (hasEmptyOption) {
      ElMessage.warning('请补全单选题选项内容')
      return
    }
    if (!editor.form.correctAnswer) {
      ElMessage.warning('请选择单选题正确答案')
      return
    }
  }

  if (editor.form.questionType === 'fill_blank' && !editor.form.acceptableAnswers.length) {
    ElMessage.warning('请至少填写一个可接受答案')
    return
  }

  if (editor.form.questionType === 'programming') {
    if (!editor.form.allowedLanguages.length) {
      ElMessage.warning('请至少选择一种允许语言')
      return
    }
    const validJudgeCases = editor.form.judgeCases.filter(
      (item) => item.output && item.output.trim()
    )
    if (!validJudgeCases.length) {
      ElMessage.warning('请至少配置一个判题用例')
      return
    }
  }

  editor.saving = true
  try {
    await saveAdminGrowthQuestion({
      id: editor.id || undefined,
      questionType: editor.form.questionType,
      trackCode: editor.form.trackCode,
      title: editor.form.title,
      content: editor.form.content,
      difficulty: editor.form.difficulty,
      tags: normalizeTags(editor.form.tagsText),
      analysisHint: editor.form.analysisHint,
      options: editor.form.questionType === 'single_choice'
        ? editor.form.options.map((item) => ({ label: item.label, text: item.text.trim() }))
        : [],
      correctAnswer: editor.form.questionType === 'single_choice' ? editor.form.correctAnswer : '',
      acceptableAnswers: editor.form.questionType === 'fill_blank'
        ? editor.form.acceptableAnswers.map((item) => String(item).trim()).filter(Boolean)
        : [],
      allowedLanguages: editor.form.questionType === 'programming' ? editor.form.allowedLanguages : [],
      inputFormat: editor.form.questionType === 'programming' ? editor.form.inputFormat : '',
      outputFormat: editor.form.questionType === 'programming' ? editor.form.outputFormat : '',
      sampleCase: editor.form.questionType === 'programming'
        ? JSON.stringify({
            input: editor.form.sampleInput || '',
            output: editor.form.sampleOutput || ''
          })
        : '',
      judgeCases: editor.form.questionType === 'programming'
        ? editor.form.judgeCases
            .map((item) => ({
              input: item.input || '',
              output: item.output || ''
            }))
            .filter((item) => item.output.trim())
        : []
    })
    ElMessage.success('题目已保存')
    editor.visible = false
    fetchList()
  } finally {
    editor.saving = false
  }
}

const removeQuestion = async (questionId) => {
  await ElMessageBox.confirm(
    '删除后，该题目将不再出现在成长中心练习题库中，正式笔试的历史快照不会受影响。是否继续？',
    '提示',
    { type: 'warning' }
  )
  await deleteAdminGrowthQuestion(questionId)
  ElMessage.success('题目已删除')
  fetchList()
}

const addChoiceOption = () => {
  editor.form.options.push({ label: 'A', text: '' })
  relabelOptions()
}

const removeChoiceOption = (index) => {
  editor.form.options.splice(index, 1)
  relabelOptions()
  if (!editor.form.options.some((item) => item.label === editor.form.correctAnswer)) {
    editor.form.correctAnswer = editor.form.options[0]?.label || 'A'
  }
}

const addJudgeCase = () => {
  editor.form.judgeCases.push({ input: '', output: '' })
}

const removeJudgeCase = (index) => {
  editor.form.judgeCases.splice(index, 1)
}

const relabelOptions = () => {
  editor.form.options = editor.form.options.map((item, index) => ({
    ...item,
    label: String.fromCharCode(65 + index)
  }))
}

const normalizeOptions = (value) => {
  const options = Array.isArray(value) && value.length
    ? value.map((item, index) => ({
        label: item.label || String.fromCharCode(65 + index),
        text: item.text || ''
      }))
    : createEmptyQuestion().options
  return options.map((item, index) => ({
    ...item,
    label: String.fromCharCode(65 + index)
  }))
}

const normalizeJudgeCases = (value) => {
  if (!Array.isArray(value) || !value.length) {
    return [{ input: '', output: '' }]
  }
  return value.map((item) => ({
    input: item.input || '',
    output: item.output || ''
  }))
}

const normalizeArray = (value, fallback = []) => {
  return Array.isArray(value) && value.length ? value : [...fallback]
}

const normalizeTags = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value.map((item) => String(item).trim()).filter(Boolean)
  if (typeof value === 'string') {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return []
}

const parseSampleCase = (value) => {
  if (!value) return { input: '', output: '' }
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch (error) {
    return { input: '', output: '' }
  }
}

const getTrackLabel = (trackCode) => {
  if (!trackCode || trackCode === 'common') return '公共题库'
  return getTrackById(trackCode).name
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.growth-bank-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.header-row,
.header-actions,
.toolbar,
.block-head {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.header-row > div:first-child {
  display: none;
}

.toolbar {
  justify-content: flex-start;
}

.editor-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.editor-form :deep(.el-form-item:nth-child(1)),
.editor-form :deep(.el-form-item:nth-child(4)),
.editor-form :deep(.el-form-item:nth-child(5)),
.editor-form :deep(.el-form-item:nth-child(6)),
.block,
.sample-grid {
  grid-column: 1 / -1;
}

.option-row,
.judge-case-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
}

.option-label {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  display: grid;
  place-items: center;
  font-weight: 700;
  flex-shrink: 0;
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .editor-form,
  .sample-grid {
    grid-template-columns: 1fr;
  }

  .option-row,
  .judge-case-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

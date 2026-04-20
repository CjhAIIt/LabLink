<template>
  <div class="paper-compose-page">
    <div class="page-header">
      <h2>手动组卷</h2>
      <el-button type="primary" :loading="saving" @click="handleSave">保存试卷</el-button>
    </div>

    <div class="compose-layout">
      <!-- Left: Question Bank -->
      <div class="panel question-bank-panel">
        <div class="panel-title">题库列表</div>
        <div class="panel-filters">
          <el-input v-model="keyword" placeholder="搜索题目" clearable style="width: 100%" @keyup.enter="loadQuestions">
            <template #append><el-button @click="loadQuestions">搜索</el-button></template>
          </el-input>
          <el-select v-model="typeFilter" clearable placeholder="题型筛选" style="width: 100%; margin-top: 10px" @change="loadQuestions">
            <el-option label="单选题" value="single_choice" />
            <el-option label="填空题" value="fill_blank" />
            <el-option label="编程题" value="programming" />
          </el-select>
        </div>
        <div v-loading="loadingQuestions" class="question-list">
          <div v-for="q in questionList" :key="q.id" class="question-item">
            <div class="question-info">
              <div class="question-title">{{ q.title }}</div>
              <div class="question-meta">
                <el-tag size="small" type="info">{{ typeLabel(q.questionType) }}</el-tag>
                <span class="score-text">{{ q.score ?? '-' }}分</span>
              </div>
            </div>
            <el-button size="small" type="primary" :disabled="isSelected(q.id)" @click="addQuestion(q)">添加</el-button>
          </div>
          <el-empty v-if="!loadingQuestions && questionList.length === 0" description="暂无题目" />
        </div>
      </div>

      <!-- Right: Selected Questions -->
      <div class="panel selected-panel">
        <div class="panel-title">已选题目 ({{ selectedQuestions.length }})</div>
        <div class="selected-list">
          <div v-for="(sq, index) in selectedQuestions" :key="sq.questionId" class="selected-item">
            <div class="item-order">
              <el-button :disabled="index === 0" link size="small" @click="moveUp(index)">↑</el-button>
              <span class="order-num">{{ index + 1 }}</span>
              <el-button :disabled="index === selectedQuestions.length - 1" link size="small" @click="moveDown(index)">↓</el-button>
            </div>
            <div class="item-info">
              <div class="question-title">{{ sq.title }}</div>
              <el-tag size="small" type="info">{{ typeLabel(sq.questionType) }}</el-tag>
            </div>
            <div class="item-score">
              <el-input-number v-model="sq.score" :min="0" :max="100" size="small" style="width: 100px" />
              <span class="score-unit">分</span>
            </div>
            <el-button type="danger" link size="small" @click="removeQuestion(index)">移除</el-button>
          </div>
          <el-empty v-if="selectedQuestions.length === 0" description="请从左侧添加题目" />
        </div>
        <div class="total-bar">
          总分：<span class="total-score">{{ totalScore }}</span> 分
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminQuestionList, getAdminPaperQuestions, saveAdminPaperQuestions } from '@/api/writtenExam'

const route = useRoute()
const examId = computed(() => route.query.examId)

const keyword = ref('')
const typeFilter = ref('')
const loadingQuestions = ref(false)
const saving = ref(false)
const questionList = ref([])
const selectedQuestions = ref([])

const typeLabel = (t) => ({ single_choice: '单选题', fill_blank: '填空题', programming: '编程题' }[t] || t)
const totalScore = computed(() => selectedQuestions.value.reduce((s, q) => s + (q.score || 0), 0))
const isSelected = (id) => selectedQuestions.value.some((q) => q.questionId === id)

async function loadQuestions() {
  loadingQuestions.value = true
  try {
    const { data } = await getAdminQuestionList({ keyword: keyword.value, questionType: typeFilter.value })
    questionList.value = data?.records || data || []
  } finally { loadingQuestions.value = false }
}

async function loadPaper() {
  if (!examId.value) return
  try {
    const { data } = await getAdminPaperQuestions(examId.value)
    selectedQuestions.value = (data || []).map((q) => ({ questionId: q.questionId || q.id, title: q.title, questionType: q.questionType, score: q.score ?? 0 }))
  } catch { /* empty paper */ }
}

function addQuestion(q) {
  if (isSelected(q.id)) return
  selectedQuestions.value.push({ questionId: q.id, title: q.title, questionType: q.questionType, score: q.score ?? 10 })
}
function removeQuestion(i) { selectedQuestions.value.splice(i, 1) }
function moveUp(i) { if (i > 0) { const t = selectedQuestions.value.splice(i, 1)[0]; selectedQuestions.value.splice(i - 1, 0, t) } }
function moveDown(i) { if (i < selectedQuestions.value.length - 1) { const t = selectedQuestions.value.splice(i, 1)[0]; selectedQuestions.value.splice(i + 1, 0, t) } }

async function handleSave() {
  if (!examId.value) return ElMessage.warning('缺少考试ID')
  if (selectedQuestions.value.length === 0) return ElMessage.warning('请至少添加一道题目')
  saving.value = true
  try {
    await saveAdminPaperQuestions(examId.value, { questions: selectedQuestions.value.map((q, i) => ({ questionId: q.questionId, score: q.score, orderIndex: i })) })
    ElMessage.success('试卷保存成功')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

onMounted(() => { loadQuestions(); loadPaper() })
</script>

<style scoped>
.paper-compose-page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 600; color: #1a1a2e; }
.compose-layout { display: flex; gap: 20px; height: calc(100vh - 160px); }
.panel { background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 20px; display: flex; flex-direction: column; }
.question-bank-panel { flex: 1; min-width: 0; }
.selected-panel { flex: 1; min-width: 0; }
.panel-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 16px; }
.panel-filters { margin-bottom: 12px; }
.question-list, .selected-list { flex: 1; overflow-y: auto; }
.question-item { display: flex; align-items: center; justify-content: space-between; padding: 12px; border-radius: 10px; background: #f8f9fc; margin-bottom: 8px; transition: background .2s; }
.question-item:hover { background: #eef1f8; }
.question-info { flex: 1; min-width: 0; }
.question-title { font-size: 14px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 4px; }
.question-meta { display: flex; align-items: center; gap: 8px; }
.score-text { font-size: 12px; color: #909399; }
.selected-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 10px; background: #f8f9fc; margin-bottom: 8px; }
.item-order { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.order-num { font-weight: 600; font-size: 14px; color: #409eff; }
.item-info { flex: 1; min-width: 0; }
.item-score { display: flex; align-items: center; gap: 4px; }
.score-unit { font-size: 13px; color: #606266; }
.total-bar { margin-top: 12px; padding: 14px 16px; background: #f0f5ff; border-radius: 10px; text-align: right; font-size: 15px; color: #303133; font-weight: 500; }
.total-score { font-size: 22px; font-weight: 700; color: #409eff; }
</style>

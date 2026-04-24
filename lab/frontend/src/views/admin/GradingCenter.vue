<template>
  <div class="grading-center-page">
    <div class="page-header">
      <h2>阅卷中心</h2>
      <div class="header-actions">
        <el-select v-model="selectedExamId" placeholder="选择考试" style="width: 240px" @change="loadGradingList">
          <el-option v-for="e in examList" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
        <el-button type="success" :disabled="!selectedExamId" @click="handlePublish">发布成绩</el-button>
        <el-button type="warning" :disabled="!selectedExamId" @click="inviteDialogVisible = true">发送面试邀请</el-button>
      </div>
    </div>

    <div class="table-card">
      <div v-loading="loading" class="exam-mobile-list mobile-only">
        <article v-for="row in gradingList" :key="row.id || row.attemptId" class="exam-mobile-card">
          <div class="exam-mobile-card__head">
            <div>
              <strong>{{ row.studentName || '未命名学生' }}</strong>
              <span>{{ row.studentId || '-' }} · {{ row.submitTime }}</span>
            </div>
            <el-tag :type="row.status === 'graded' ? 'success' : 'warning'" size="small">
              {{ row.status === 'graded' ? '已阅卷' : '待阅卷' }}
            </el-tag>
          </div>
          <div class="exam-mobile-card__score">
            <div><span>自动评分</span><strong>{{ row.autoScore ?? '-' }}</strong></div>
            <div><span>人工评分</span><strong>{{ row.manualScore ?? '-' }}</strong></div>
            <div><span>总分</span><strong>{{ row.totalScore ?? '-' }}</strong></div>
            <div><span>作弊次数</span><strong>{{ row.cheatCount ?? 0 }}</strong></div>
          </div>
          <div class="exam-mobile-card__actions">
            <el-button type="primary" @click="openGrading(row)">阅卷</el-button>
          </div>
        </article>
        <el-empty v-if="!loading && !gradingList.length" description="暂无阅卷记录" />
      </div>
      <el-table v-loading="loading" :data="gradingList" border stripe class="desktop-only">
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="140" />
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column prop="autoScore" label="自动评分" width="100" align="center" />
        <el-table-column prop="manualScore" label="人工评分" width="100" align="center" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center">
          <template #default="{ row }"><span class="total-score-cell">{{ row.totalScore ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="cheatCount" label="作弊次数" width="100" align="center">
          <template #default="{ row }"><el-tag v-if="row.cheatCount > 0" type="danger" size="small">{{ row.cheatCount }}</el-tag><span v-else>0</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 'graded' ? 'success' : 'warning'" size="small">{{ row.status === 'graded' ? '已阅卷' : '待阅卷' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }"><el-button type="primary" link @click="openGrading(row)">阅卷</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Grading Drawer -->
    <el-drawer v-model="drawerVisible" title="阅卷详情" size="720px" direction="rtl">
      <div v-if="currentStudent" v-loading="loadingAnswer" class="drawer-content">
        <div class="student-info-bar">
          <span>{{ currentStudent.studentName }}</span><span class="sep">|</span>
          <span>{{ currentStudent.studentId }}</span><span class="sep">|</span>
          <span>提交于 {{ currentStudent.submitTime }}</span>
        </div>
        <div v-for="(q, idx) in answerList" :key="q.questionId" class="answer-block">
          <div class="answer-header">
            <span class="q-index">第{{ idx + 1 }}题</span>
            <span class="q-title">{{ q.title }}</span>
            <el-tag size="small" type="info">{{ typeLabel(q.questionType) }}</el-tag>
          </div>
          <div class="answer-body">
            <pre v-if="q.questionType === 'programming'" class="code-block">{{ q.answer }}</pre>
            <div v-else class="text-answer">{{ q.answer || '未作答' }}</div>
          </div>
          <div class="score-row">
            <span>评分：</span>
            <el-input-number v-model="q.gradingScore" :min="0" :max="q.maxScore || 100" size="small" style="width: 120px" />
            <span class="max-score">/ {{ q.maxScore ?? '?' }}</span>
          </div>
          <el-input v-model="q.remark" type="textarea" :rows="2" placeholder="评语（可选）" style="margin-top: 8px" />
        </div>
        <div class="drawer-footer">
          <el-button type="primary" :loading="submittingGrade" @click="handleSubmitGrading">保存评分</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- Interview Invitation Dialog -->
    <el-dialog v-model="inviteDialogVisible" title="发送面试邀请" width="500px">
      <p>选择通过的学生发送面试邀请：</p>
      <el-checkbox-group v-model="selectedStudentIds">
        <el-checkbox v-for="s in passedStudents" :key="s.studentId" :label="s.studentId">{{ s.studentName }} ({{ s.totalScore }}分)</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendingInvite" @click="handleSendInvitations">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminExamList, getAdminGradingList, getAdminStudentAnswer, submitAdminGrading, publishAdminScores, sendInterviewInvitations } from '@/api/writtenExam'

const selectedExamId = ref('')
const examList = ref([])
const gradingList = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const loadingAnswer = ref(false)
const submittingGrade = ref(false)
const currentStudent = ref(null)
const answerList = ref([])
const inviteDialogVisible = ref(false)
const selectedStudentIds = ref([])
const sendingInvite = ref(false)

const typeLabel = (t) => ({ single_choice: '单选题', fill_blank: '填空题', programming: '编程题' }[t] || t)
const passedStudents = computed(() => gradingList.value.filter((s) => s.status === 'graded' && (s.totalScore ?? 0) >= 60))

async function loadExams() {
  try { const { data } = await getAdminExamList(); examList.value = data?.records || data || [] } catch { /* ignore */ }
}
async function loadGradingList() {
  if (!selectedExamId.value) return
  loading.value = true
  try { const { data } = await getAdminGradingList({ examId: selectedExamId.value }); gradingList.value = data?.records || data || [] }
  finally { loading.value = false }
}
async function openGrading(row) {
  currentStudent.value = row; drawerVisible.value = true; loadingAnswer.value = true
  try {
    const { data } = await getAdminStudentAnswer(row.attemptId || row.id)
    answerList.value = (data?.questions || data || []).map((q) => ({ ...q, gradingScore: q.gradingScore ?? q.score ?? 0, remark: q.remark || '' }))
  } finally { loadingAnswer.value = false }
}
async function handleSubmitGrading() {
  submittingGrade.value = true
  try {
    await submitAdminGrading(currentStudent.value.attemptId || currentStudent.value.id, { questions: answerList.value.map((q) => ({ questionId: q.questionId, score: q.gradingScore, remark: q.remark })) })
    ElMessage.success('评分已保存'); drawerVisible.value = false; loadGradingList()
  } catch { ElMessage.error('保存失败') } finally { submittingGrade.value = false }
}
async function handlePublish() {
  try { await publishAdminScores(selectedExamId.value); ElMessage.success('成绩已发布') } catch { ElMessage.error('发布失败') }
}
async function handleSendInvitations() {
  if (selectedStudentIds.value.length === 0) return ElMessage.warning('请选择学生')
  sendingInvite.value = true
  try {
    await sendInterviewInvitations({ examId: selectedExamId.value, studentIds: selectedStudentIds.value })
    ElMessage.success('邀请已发送'); inviteDialogVisible.value = false
  } catch { ElMessage.error('发送失败') } finally { sendingInvite.value = false }
}
onMounted(loadExams)
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
.exam-mobile-card__score span {
  color: #64748b;
}

.exam-mobile-card__score {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.exam-mobile-card__score div {
  padding: 10px;
  border-radius: 14px;
  background: #f8fafc;
  display: grid;
  gap: 4px;
}

.exam-mobile-card__score strong {
  color: #172033;
  font-size: 18px;
}

.exam-mobile-card__actions {
  display: flex;
}

.exam-mobile-card__actions .el-button {
  width: 100%;
}

.grading-center-page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 600; color: #1a1a2e; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.table-card { background: #fff; border-radius: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); padding: 20px; }
.total-score-cell { font-weight: 700; color: #409eff; }
.drawer-content { padding: 0 4px; }
.student-info-bar { padding: 12px 16px; background: #f0f5ff; border-radius: 10px; margin-bottom: 20px; font-size: 14px; color: #303133; }
.sep { margin: 0 10px; color: #dcdfe6; }
.answer-block { background: #f8f9fc; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.answer-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.q-index { font-weight: 600; color: #409eff; }
.q-title { flex: 1; font-size: 14px; color: #303133; }
.code-block { background: #1e1e2e; color: #cdd6f4; padding: 14px; border-radius: 8px; font-size: 13px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
.text-answer { padding: 10px; background: #fff; border-radius: 8px; font-size: 14px; color: #606266; min-height: 36px; }
.score-row { display: flex; align-items: center; gap: 8px; margin-top: 10px; font-size: 14px; color: #303133; }
.max-score { color: #909399; font-size: 13px; }
.drawer-footer { text-align: right; margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebeef5; }
</style>

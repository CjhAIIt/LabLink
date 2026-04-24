<template>
  <div class="exam-config-panel">
    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <strong>正式笔试配置</strong>
            <p>正式笔试题目从成长中心共享题库中选择，保存时会固化为当前快照。</p>
          </div>
          <div class="panel-actions">
            <el-button @click="goToQuestionBank">成长中心题库</el-button>
            <el-button @click="openSelector">从题库选题</el-button>
            <el-button type="primary" :loading="savingExamConfig" @click="saveExamConfig">
              保存笔试配置
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="config-form">
        <el-form-item label="是否开放招新">
          <el-switch
            v-model="examConfig.recruitmentOpen"
            active-text="开放"
            inactive-text="关闭"
          />
        </el-form-item>

        <el-form-item label="笔试标题">
          <el-input v-model="examConfig.title" placeholder="例如：2026 春季实验室笔试" />
        </el-form-item>

        <el-form-item label="笔试说明">
          <el-input
            v-model="examConfig.description"
            type="textarea"
            :rows="4"
            placeholder="说明考试要求、作答规则和注意事项"
          />
        </el-form-item>

        <el-form-item label="笔试时间">
          <el-date-picker
            v-model="examConfig.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="通过分数">
          <el-input-number v-model="examConfig.passScore" :min="1" :max="100" />
        </el-form-item>
      </el-form>

      <div class="env-panel">
        <div class="env-header">
          <strong>当前判题环境</strong>
          <div class="env-tags">
            <el-tag
              v-for="(available, key) in environmentStatus"
              :key="key"
              :type="available ? 'success' : 'info'"
              effect="plain"
            >
              {{ languageLabelMap[key] }} {{ available ? '可用' : '未就绪' }}
            </el-tag>
          </div>
        </div>

        <div class="env-grid">
          <div
            v-for="item in environmentDetailList"
            :key="item.key"
            class="env-card"
            :class="{ unavailable: !item.available }"
          >
            <div class="env-card-head">
              <strong>{{ item.label }}</strong>
              <el-tag :type="item.available ? 'success' : 'warning'" effect="plain">
                {{ item.available ? '已就绪' : '未就绪' }}
              </el-tag>
            </div>
            <div class="env-card-line">配置命令：{{ item.configuredCommand || '-' }}</div>
            <div class="env-card-line">解析命令：{{ item.resolvedCommand || '-' }}</div>
            <div class="env-card-tip">{{ item.message || '请检查服务端编译环境' }}</div>
          </div>
        </div>
      </div>

      <div class="question-toolbar">
        <div>
          <strong>正式笔试题目</strong>
          <p>学生进入正式笔试时，只会看到这里配置的几道题，不会看到成长中心全部练习题。</p>
        </div>
        <el-button type="primary" plain @click="openSelector">添加题目</el-button>
      </div>

      <el-empty
        v-if="examConfig.questions.length === 0"
        description="当前还没有配置正式笔试题目"
      />

      <div v-else class="question-list">
        <div
          v-for="(question, index) in examConfig.questions"
          :key="question.bankQuestionId || question.id || index"
          class="question-card"
        >
          <div class="question-head">
            <div>
              <p class="question-index">第 {{ index + 1 }} 题</p>
              <h4>{{ question.title }}</h4>
            </div>
            <div class="question-tags">
              <el-tag effect="plain">{{ questionTypeLabelMap[question.questionType] || question.questionType }}</el-tag>
              <el-tag effect="plain" type="success">{{ getTrackLabel(question.trackCode) }}</el-tag>
              <el-tag v-if="question.difficulty" effect="plain" type="warning">{{ question.difficulty }}</el-tag>
            </div>
          </div>

          <p class="question-content">{{ question.content || '暂无题目描述' }}</p>

          <div class="question-meta">
            <el-form-item label="分值" class="score-item">
              <el-input-number v-model="question.score" :min="1" :max="100" />
            </el-form-item>

            <div class="question-ops">
              <el-button :disabled="index === 0" @click="moveQuestion(index, -1)">上移</el-button>
              <el-button :disabled="index === examConfig.questions.length - 1" @click="moveQuestion(index, 1)">下移</el-button>
              <el-button type="danger" link @click="removeQuestion(index)">移除</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <strong>笔试提交记录</strong>
            <p>自动评分后仍保留管理员审核入口，可查看答卷并最终确认结果。</p>
          </div>
          <div class="filter-row">
            <el-input
              v-model="submissionSearch.realName"
              clearable
              placeholder="搜索学生姓名"
              style="width: 200px"
              @keyup.enter="handleSubmissionSearch"
            />
            <el-select v-model="submissionSearch.status" clearable placeholder="全部状态" style="width: 160px">
              <el-option label="待审核" :value="1" />
              <el-option label="已通过" :value="2" />
              <el-option label="未通过" :value="3" />
            </el-select>
            <el-button type="primary" @click="handleSubmissionSearch">搜索</el-button>
          </div>
        </div>
      </template>

      <el-table :data="examSubmissions" border stripe>
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="studentId" label="学号" width="150" />
        <el-table-column prop="major" label="专业" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalScore" label="总分" width="100" />
        <el-table-column prop="aiRemark" label="评分说明" min-width="280" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getSubmissionStatusType(row.status)">
              {{ getSubmissionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核备注" min-width="220">
          <template #default="{ row }">
            <el-input
              v-model="row.reviewRemarkDraft"
              placeholder="可填写审核说明"
              maxlength="120"
              show-word-limit
              :disabled="row.status !== 1"
            />
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showSubmissionDetail(row)">查看答卷</el-button>
            <template v-if="row.status === 1">
              <el-button size="small" type="success" @click="reviewSubmission(row, 2)">通过</el-button>
              <el-button size="small" type="danger" plain @click="reviewSubmission(row, 3)">不通过</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="submissionPagination.current"
          v-model:page-size="submissionPagination.size"
          :total="submissionPagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchExamSubmissions"
        />
      </div>
    </el-card>

    <el-dialog v-model="selector.visible" title="从成长中心题库选题" width="1080px">
      <div class="selector-toolbar">
        <el-select v-model="selector.filters.trackCode" clearable placeholder="方向" style="width: 180px">
          <el-option
            v-for="track in trackOptions"
            :key="track.id"
            :label="track.name"
            :value="track.id"
          />
        </el-select>
        <el-select v-model="selector.filters.questionType" clearable placeholder="题型" style="width: 160px">
          <el-option label="单选题" value="single_choice" />
          <el-option label="填空题" value="fill_blank" />
          <el-option label="编程题" value="programming" />
        </el-select>
        <el-input
          v-model="selector.filters.keyword"
          clearable
          placeholder="搜索标题或描述"
          style="width: 260px"
          @keyup.enter="fetchQuestionBank"
        />
        <el-button type="primary" @click="fetchQuestionBank">搜索</el-button>
      </div>

      <el-table
        v-loading="selector.loading"
        :data="selector.rows"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
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
        <el-table-column label="标签" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="tag in normalizeTags(row.tags).slice(0, 3)"
              :key="`${row.id}-${tag}`"
              size="small"
              effect="plain"
              style="margin-right: 6px"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="selector.pagination.pageNum"
          v-model:page-size="selector.pagination.pageSize"
          :total="selector.pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchQuestionBank"
        />
      </div>

      <template #footer>
        <el-button @click="selector.visible = false">取消</el-button>
        <el-button type="primary" @click="appendSelectedQuestions">加入正式笔试</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submissionDetail.visible" title="笔试答卷详情" width="920px">
      <el-table v-if="submissionDetail.answerSheet.length" :data="submissionDetail.answerSheet" border stripe>
        <el-table-column prop="title" label="题目" min-width="220" />
        <el-table-column prop="fullScore" label="满分" width="80" />
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column prop="language" label="语言" width="100" />
        <el-table-column prop="resultMessage" label="判题说明" min-width="280" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="当前没有答卷明细" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminGrowthQuestionBank } from '@/api/growthCenter'
import {
  getAdminExamConfig,
  getAdminExamSubmissions,
  reviewAdminExamSubmission,
  saveAdminExamConfig
} from '@/api/writtenExam'
import { getTrackById, gradPathTracks } from '@/constants/gradPath'

const router = useRouter()

const trackOptions = [{ id: 'common', name: '公共题库' }, ...gradPathTracks]
const questionTypeLabelMap = {
  single_choice: '单选题',
  fill_blank: '填空题',
  programming: '编程题'
}
const languageLabelMap = {
  c: 'C',
  cpp: 'C++',
  java: 'Java',
  python: 'Python'
}

const savingExamConfig = ref(false)
const environmentStatus = reactive({})
const environmentDetails = reactive({})
const examConfig = reactive({
  recruitmentOpen: false,
  title: '',
  description: '',
  timeRange: [],
  passScore: 60,
  questions: []
})
const submissionSearch = reactive({
  realName: '',
  status: null
})
const examSubmissions = reactive([])
const submissionPagination = reactive({ current: 1, size: 10, total: 0 })
const submissionDetail = reactive({
  visible: false,
  answerSheet: []
})
const selector = reactive({
  visible: false,
  loading: false,
  rows: [],
  selectedRows: [],
  filters: {
    trackCode: '',
    questionType: '',
    keyword: ''
  },
  pagination: {
    pageNum: 1,
    pageSize: 8,
    total: 0
  }
})

const environmentDetailList = computed(() =>
  Object.entries(environmentDetails).map(([key, value]) => ({ key, ...(value || {}) }))
)

const fetchExamConfig = async () => {
  const res = await getAdminExamConfig()
  const data = res.data || {}
  examConfig.recruitmentOpen = !!data.recruitmentOpen
  examConfig.title = data.exam?.title || ''
  examConfig.description = data.exam?.description || ''
  examConfig.timeRange = data.exam?.startTime && data.exam?.endTime
    ? [data.exam.startTime, data.exam.endTime]
    : []
  examConfig.passScore = data.exam?.passScore || 60
  examConfig.questions = (data.questions || []).map((item, index) => ({
    ...item,
    score: item.score ?? (item.questionType === 'programming' ? 30 : 10),
    sortOrder: index + 1
  }))

  Object.keys(environmentStatus).forEach((key) => delete environmentStatus[key])
  Object.entries(data.environmentStatus || {}).forEach(([key, value]) => {
    environmentStatus[key] = value
  })

  Object.keys(environmentDetails).forEach((key) => delete environmentDetails[key])
  Object.entries(data.environmentDetails || {}).forEach(([key, value]) => {
    environmentDetails[key] = value
  })
}

const saveExamConfig = async () => {
  if (!examConfig.title.trim()) {
    ElMessage.warning('请先填写笔试标题')
    return
  }
  if (!examConfig.timeRange.length) {
    ElMessage.warning('请先设置笔试时间')
    return
  }
  if (examConfig.questions.length === 0) {
    ElMessage.warning('请至少选择一道正式笔试题目')
    return
  }

  savingExamConfig.value = true
  try {
    await saveAdminExamConfig({
      recruitmentOpen: examConfig.recruitmentOpen,
      title: examConfig.title,
      description: examConfig.description,
      startTime: examConfig.timeRange[0],
      endTime: examConfig.timeRange[1],
      passScore: examConfig.passScore,
      questions: examConfig.questions.map((item, index) => ({
        bankQuestionId: item.bankQuestionId || item.id,
        questionType: item.questionType,
        trackCode: item.trackCode,
        title: item.title,
        content: item.content,
        difficulty: item.difficulty,
        inputFormat: item.inputFormat,
        outputFormat: item.outputFormat,
        sampleCase: item.sampleCase,
        tags: normalizeTags(item.tags),
        analysisHint: item.analysisHint,
        options: item.options,
        correctAnswer: item.correctAnswer,
        acceptableAnswers: item.acceptableAnswers,
        allowedLanguages: item.allowedLanguages,
        judgeCases: item.judgeCases,
        score: item.score,
        sortOrder: index + 1
      }))
    })
    ElMessage.success('笔试配置已保存')
    await fetchExamConfig()
  } finally {
    savingExamConfig.value = false
  }
}

const fetchExamSubmissions = async () => {
  const res = await getAdminExamSubmissions({
    pageNum: submissionPagination.current,
    pageSize: submissionPagination.size,
    realName: submissionSearch.realName || undefined,
    status: submissionSearch.status
  })
  examSubmissions.splice(
    0,
    examSubmissions.length,
    ...(res.data.records || []).map((item) => ({
      ...item,
      reviewRemarkDraft: item.adminRemark || ''
    }))
  )
  submissionPagination.total = res.data.total || 0
}

const handleSubmissionSearch = () => {
  submissionPagination.current = 1
  fetchExamSubmissions()
}

const reviewSubmission = async (row, status) => {
  await reviewAdminExamSubmission({
    submissionId: row.id,
    status,
    adminRemark: row.reviewRemarkDraft
  })
  ElMessage.success(status === 2 ? '已通过该学生笔试' : '已标记为笔试未通过')
  fetchExamSubmissions()
}

const showSubmissionDetail = (row) => {
  submissionDetail.answerSheet = row.answerSheet || []
  submissionDetail.visible = true
}

const moveQuestion = (index, offset) => {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= examConfig.questions.length) return
  const current = examConfig.questions[index]
  examConfig.questions[index] = examConfig.questions[targetIndex]
  examConfig.questions[targetIndex] = current
}

const removeQuestion = (index) => {
  examConfig.questions.splice(index, 1)
}

const openSelector = async () => {
  selector.visible = true
  selector.selectedRows = []
  selector.pagination.pageNum = 1
  await fetchQuestionBank()
}

const fetchQuestionBank = async () => {
  selector.loading = true
  try {
    const res = await getAdminGrowthQuestionBank({
      pageNum: selector.pagination.pageNum,
      pageSize: selector.pagination.pageSize,
      trackCode: selector.filters.trackCode || undefined,
      questionType: selector.filters.questionType || undefined,
      keyword: selector.filters.keyword || undefined
    })
    selector.rows = res.data.records || []
    selector.pagination.total = res.data.total || 0
  } finally {
    selector.loading = false
  }
}

const handleSelectionChange = (rows) => {
  selector.selectedRows = rows || []
}

const appendSelectedQuestions = () => {
  if (!selector.selectedRows.length) {
    ElMessage.warning('请先选择题目')
    return
  }

  const existedIds = new Set(examConfig.questions.map((item) => item.bankQuestionId || item.id))
  selector.selectedRows.forEach((row) => {
    const questionId = row.bankQuestionId || row.id
    if (existedIds.has(questionId)) return
    examConfig.questions.push({
      ...row,
      bankQuestionId: questionId,
      score: row.questionType === 'programming' ? 30 : 10
    })
  })

  selector.visible = false
  ElMessage.success('已加入正式笔试')
}

const normalizeTags = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value
  if (typeof value === 'string') {
    try {
      return JSON.parse(value)
    } catch (error) {
      return value.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }
  return []
}

const getTrackLabel = (trackCode) => {
  if (!trackCode || trackCode === 'common') return '公共题库'
  return getTrackById(trackCode).name
}

const getSubmissionStatusText = (status) => {
  if (status === 2) return '已通过'
  if (status === 3) return '未通过'
  return '待审核'
}

const getSubmissionStatusType = (status) => {
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  return 'warning'
}

const goToQuestionBank = () => router.push('/admin/growth-question-bank')

onMounted(async () => {
  await Promise.all([fetchExamConfig(), fetchExamSubmissions()])
})
</script>

<style scoped>
.exam-config-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header,
.panel-actions,
.question-head,
.question-tags,
.question-meta,
.question-ops,
.selector-toolbar,
.filter-row,
.env-header,
.env-tags {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.panel-header,
.env-header,
.question-head,
.question-meta {
  justify-content: space-between;
}

.panel-header p,
.question-toolbar p,
.question-content {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.config-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.config-form :deep(.el-form-item:nth-child(2)),
.config-form :deep(.el-form-item:nth-child(3)),
.config-form :deep(.el-form-item:nth-child(4)) {
  grid-column: 1 / -1;
}

.env-panel {
  margin-top: 12px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff, #f8fafc);
  border: 1px solid rgba(14, 165, 233, 0.14);
}

.env-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.env-card {
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.env-card.unavailable {
  background: #fffbeb;
  border-color: rgba(245, 158, 11, 0.3);
}

.env-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.env-card-line,
.env-card-tip {
  color: #475569;
  line-height: 1.7;
  word-break: break-all;
}

.question-toolbar {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.question-list {
  margin-top: 14px;
  display: grid;
  gap: 14px;
}

.question-card {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.question-index {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.question-card h4 {
  margin: 0;
  color: #0f172a;
}

.score-item {
  margin-bottom: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .config-form {
    grid-template-columns: 1fr;
  }

  .panel-header,
  .question-toolbar,
  .question-head,
  .question-meta {
    align-items: flex-start;
  }
}
</style>

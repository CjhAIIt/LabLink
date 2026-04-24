<template>
  <div class="hub-page">
    <!-- 顶部概览 -->
    <header class="hub-header">
      <div class="hub-header__info">
        <h1>笔试中心</h1>
        <p>管理笔试、题库、阅卷与成绩，一站式完成招新考核流程</p>
      </div>
      <div class="hub-header__stats">
        <div class="hub-stat"><strong>{{ overview.examCount ?? 0 }}</strong><span>笔试场次</span></div>
        <div class="hub-stat"><strong>{{ overview.questionCount ?? 0 }}</strong><span>题库总量</span></div>
        <div class="hub-stat"><strong>{{ overview.submissionCount ?? 0 }}</strong><span>答卷数</span></div>
        <div class="hub-stat"><strong>{{ overview.avgScore ?? '-' }}</strong><span>平均分</span></div>
      </div>
    </header>

    <!-- Tab 导航 -->
    <nav class="hub-tabs">
      <button v-for="tab in tabs" :key="tab.key" class="hub-tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        <component :is="tab.icon" class="hub-tab__icon" />
        <span>{{ tab.label }}</span>
      </button>
    </nav>

    <!-- ==================== 笔试管理 ==================== -->
    <section v-if="activeTab === 'exam'" class="hub-section">
      <div class="section-bar">
        <h2>笔试列表</h2>
        <div class="section-bar__actions">
          <el-input v-model="examSearch" placeholder="搜索笔试名称" clearable style="width: 200px" />
          <el-button type="primary" @click="showExamDialog()"><el-icon class="el-icon--left"><Plus /></el-icon>创建笔试</el-button>
        </div>
      </div>
      <div v-loading="examLoading" class="card-grid">
        <el-empty v-if="!examLoading && !filteredExams.length" description="暂无笔试" />
        <article v-for="exam in filteredExams" :key="exam.id" class="exam-card">
          <div class="exam-card__head">
            <el-tag :type="examStatusType(exam.status)" size="small" effect="dark" round>{{ examStatusLabel(exam.status) }}</el-tag>
            <el-dropdown trigger="click" @command="(cmd) => handleExamAction(cmd, exam)">
              <el-button text size="small"><el-icon><MoreFilled /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="paper">组卷</el-dropdown-item>
                  <el-dropdown-item command="publish" :disabled="exam.status !== 'draft'">发布</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <h3 class="exam-card__title">{{ exam.title }}</h3>
          <p class="exam-card__desc">{{ exam.description || '暂无说明' }}</p>
          <div class="exam-card__meta">
            <span>{{ exam.duration }}分钟</span>
            <span>满分{{ exam.totalScore }}</span>
            <span>及格{{ exam.passScore }}</span>
          </div>
          <div class="exam-card__time">
            <el-icon><Clock /></el-icon>
            {{ formatDate(exam.startTime) }} ~ {{ formatDate(exam.endTime) }}
          </div>
        </article>
      </div>
    </section>

    <!-- ==================== 题库管理 ==================== -->
    <section v-if="activeTab === 'question'" class="hub-section">
      <div class="section-bar">
        <h2>题库</h2>
        <div class="section-bar__actions">
          <el-select v-model="qFilter.type" clearable placeholder="题型" style="width: 130px">
            <el-option label="单选题" value="single_choice" />
            <el-option label="填空题" value="fill_blank" />
            <el-option label="编程题" value="programming" />
            <el-option label="简答题" value="short_answer" />
          </el-select>
          <el-select v-model="qFilter.difficulty" clearable placeholder="难度" style="width: 110px">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
          <el-input v-model="qFilter.keyword" placeholder="搜索题目" clearable style="width: 180px" @keyup.enter="loadQuestions" />
          <el-button @click="loadQuestions">搜索</el-button>
          <el-button type="primary" @click="showQuestionDialog()"><el-icon class="el-icon--left"><Plus /></el-icon>新增题目</el-button>
        </div>
      </div>
      <div v-loading="qLoading" class="exam-mobile-list mobile-only">
        <article v-for="row in questions" :key="row.id" class="exam-mobile-card">
          <div class="exam-mobile-card__head">
            <div>
              <strong>{{ row.title || '未命名题目' }}</strong>
              <span>{{ qTypeLabel(row.questionType) }} · {{ diffLabel(row.difficulty) }}</span>
            </div>
            <el-tag :type="diffType(row.difficulty)" size="small">{{ diffLabel(row.difficulty) }}</el-tag>
          </div>
          <p>{{ row.content || row.description || '暂无题目内容摘要' }}</p>
          <div class="exam-mobile-card__meta">
            <span>分值 {{ row.score ?? '-' }}</span>
            <span>{{ row.tags || '未设置标签' }}</span>
          </div>
          <div class="exam-mobile-card__actions">
            <el-button plain type="primary" @click="showQuestionDialog(row)">编辑</el-button>
            <el-button plain type="danger" @click="handleDeleteQuestion(row)">删除</el-button>
          </div>
        </article>
        <el-empty v-if="!qLoading && !questions.length" description="暂无题目" />
      </div>
      <el-table v-loading="qLoading" :data="questions" stripe class="hub-table desktop-only">
        <el-table-column prop="title" label="题目" min-width="240" show-overflow-tooltip />
        <el-table-column label="题型" width="100">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ qTypeLabel(row.questionType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }"><el-tag :type="diffType(row.difficulty)" size="small">{{ diffLabel(row.difficulty) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" align="center" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showQuestionDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- ==================== 阅卷中心 ==================== -->
    <section v-if="activeTab === 'grading'" class="hub-section">
      <div class="section-bar">
        <h2>阅卷中心</h2>
        <div class="section-bar__actions grading-actions">
          <el-select v-model="gradingExamId" clearable placeholder="全部笔试" class="grading-actions__select" @change="loadGradingList">
            <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
          </el-select>
          <el-input v-model="gradingKeyword" clearable placeholder="姓名/学号/实验室" style="width: 180px" @keyup.enter="loadGradingList" />
          <el-button @click="loadGradingList">查询</el-button>
          <el-button :disabled="!gradingExamId" @click="handlePublishScores">发布成绩</el-button>
          <el-button type="primary" :disabled="!gradingExamId" @click="showInviteDialog = true">发送面试邀请</el-button>
        </div>
      </div>
      <div v-loading="gradingLoading" class="exam-mobile-list mobile-only">
        <article v-for="row in gradingList" :key="row.id || row.attemptId" class="exam-mobile-card">
          <div class="exam-mobile-card__head">
            <div>
              <strong>{{ row.studentName || '未命名学生' }}</strong>
              <span>{{ row.studentNo || row.studentId || '-' }} · {{ row.labName || '实验室待定' }}</span>
            </div>
            <el-tag :type="attemptStatusType(row.statusKey)" size="small">{{ row.statusLabel || attemptStatusLabel(row.status) }}</el-tag>
          </div>
          <div class="exam-mobile-card__score">
            <div><span>客观分</span><strong>{{ row.autoScore ?? '-' }}</strong></div>
            <div><span>主观分</span><strong>{{ row.manualScore ?? '-' }}</strong></div>
            <div><span>总分</span><strong>{{ row.totalScore ?? '-' }}</strong></div>
          </div>
          <p>提交时间：{{ formatDate(row.submitTime) }}；切屏 {{ row.switchCount ?? 0 }} 次</p>
          <div class="exam-mobile-card__actions">
            <el-button type="primary" @click="openGradingDrawer(row)">查看详情</el-button>
          </div>
        </article>
        <el-empty v-if="!gradingLoading && !gradingList.length" description="暂无阅卷记录" />
      </div>
      <el-table v-loading="gradingLoading" :data="gradingList" stripe class="hub-table desktop-only">
        <el-table-column prop="examTitle" label="笔试" min-width="160" show-overflow-tooltip />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="college" label="学院" min-width="130" show-overflow-tooltip />
        <el-table-column prop="labName" label="申请实验室" min-width="150" show-overflow-tooltip />
        <el-table-column label="提交时间" width="170"><template #default="{ row }">{{ formatDate(row.submitTime) }}</template></el-table-column>
        <el-table-column prop="autoScore" label="客观分" width="80" align="center" />
        <el-table-column prop="manualScore" label="主观分" width="80" align="center" />
        <el-table-column prop="totalScore" label="总分" width="70" align="center" />
        <el-table-column prop="switchCount" label="切屏" width="60" align="center" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="attemptStatusType(row.statusKey)" size="small">{{ row.statusLabel || attemptStatusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }"><el-button link type="primary" size="small" @click="openGradingDrawer(row)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <!-- ==================== 成绩统计 ==================== -->
    <section v-if="activeTab === 'stats'" class="hub-section">
      <div class="section-bar">
        <h2>成绩统计</h2>
        <el-select v-model="statsExamId" placeholder="选择笔试" style="width: 220px" @change="loadStatistics">
          <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
      </div>
      <div class="stats-cards">
        <div class="stats-card stats-card--purple"><strong>{{ statsData.totalCount ?? 0 }}</strong><span>参考人数</span></div>
        <div class="stats-card stats-card--blue"><strong>{{ statsData.submitRate ?? '0%' }}</strong><span>提交率</span></div>
        <div class="stats-card stats-card--green"><strong>{{ statsData.avgScore ?? '-' }}</strong><span>平均分</span></div>
        <div class="stats-card stats-card--amber"><strong>{{ statsData.passRate ?? '0%' }}</strong><span>通过率</span></div>
        <div class="stats-card stats-card--rose"><strong>{{ statsData.maxScore ?? '-' }}</strong><span>最高分</span></div>
        <div class="stats-card stats-card--slate"><strong>{{ statsData.minScore ?? '-' }}</strong><span>最低分</span></div>
      </div>
      <div class="stats-charts">
        <div class="chart-card"><h3>分数段分布</h3><div ref="barChartRef" class="chart-canvas"></div></div>
        <div class="chart-card"><h3>题型得分占比</h3><div ref="pieChartRef" class="chart-canvas"></div></div>
      </div>
    </section>

    <!-- ==================== 创建/编辑笔试弹窗 ==================== -->
    <el-dialog v-model="examDialogVisible" :title="editingExam ? '编辑笔试' : '创建笔试'" width="600px" :close-on-click-modal="false">
      <el-form label-width="100px" class="hub-form">
        <el-form-item label="笔试名称" required><el-input v-model="examForm.title" placeholder="如：2024秋季招新笔试" /></el-form-item>
        <el-form-item label="考试说明"><el-input v-model="examForm.description" type="textarea" :rows="3" placeholder="考试注意事项、范围等" /></el-form-item>
        <el-form-item label="考试时间">
          <div style="display:flex;gap:8px;width:100%">
            <el-date-picker v-model="examForm.startTime" type="datetime" placeholder="开始时间" value-format="YYYY-MM-DD HH:mm:ss" style="flex:1" />
            <el-date-picker v-model="examForm.endTime" type="datetime" placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="考试时长"><el-input-number v-model="examForm.duration" :min="5" :max="300" :step="5" /><span style="margin-left:8px;color:#94a3b8">分钟</span></el-form-item>
        <el-form-item label="分数设置">
          <div style="display:flex;gap:16px">
            <div>满分 <el-input-number v-model="examForm.totalScore" :min="10" :max="1000" /></div>
            <div>及格 <el-input-number v-model="examForm.passScore" :min="0" :max="examForm.totalScore" /></div>
          </div>
        </el-form-item>
        <el-form-item label="防作弊"><el-switch v-model="examForm.enableAntiCheat" active-text="开启" inactive-text="关闭" /></el-form-item>
        <el-form-item label="签名确认"><el-switch v-model="examForm.enableSignature" active-text="开启" inactive-text="关闭" /></el-form-item>
        <el-form-item v-if="examForm.enableAntiCheat" label="最大切屏"><el-input-number v-model="examForm.maxSwitchCount" :min="1" :max="20" /><span style="margin-left:8px;color:#94a3b8">次后强制交卷</span></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="examDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveExam">{{ editingExam ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 新增/编辑题目弹窗 ==================== -->
    <el-dialog v-model="qDialogVisible" :title="editingQuestion ? '编辑题目' : '新增题目'" width="780px" :close-on-click-modal="false" top="4vh">
      <el-form label-width="100px" class="hub-form">
        <el-form-item label="题型" required>
          <el-radio-group v-model="qForm.questionType">
            <el-radio-button value="single_choice">单选题</el-radio-button>
            <el-radio-button value="fill_blank">填空题</el-radio-button>
            <el-radio-button value="programming">编程题</el-radio-button>
            <el-radio-button value="short_answer">简答题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目标题" required><el-input v-model="qForm.title" placeholder="简要描述题目" /></el-form-item>
        <el-form-item label="题目内容"><el-input v-model="qForm.content" type="textarea" :rows="4" placeholder="详细题目描述（支持Markdown）" /></el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="qForm.difficulty">
            <el-radio-button value="easy">简单</el-radio-button>
            <el-radio-button value="medium">中等</el-radio-button>
            <el-radio-button value="hard">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分值"><el-input-number v-model="qForm.score" :min="1" :max="100" /></el-form-item>

        <!-- 单选题选项 -->
        <template v-if="qForm.questionType === 'single_choice'">
          <el-form-item label="选项">
            <div class="options-editor">
              <div v-for="(opt, i) in qForm.options" :key="i" class="option-row">
                <span class="option-label">{{ opt.label }}</span>
                <el-input v-model="opt.text" :placeholder="`选项${opt.label}内容`" />
                <el-button v-if="qForm.options.length > 2" text type="danger" @click="removeOption(i)"><el-icon><Close /></el-icon></el-button>
              </div>
              <el-button text type="primary" @click="addOption">+ 添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案"><el-select v-model="qForm.correctAnswer" placeholder="选择正确选项">
            <el-option v-for="opt in qForm.options" :key="opt.label" :label="`${opt.label}. ${opt.text}`" :value="opt.label" />
          </el-select></el-form-item>
        </template>

        <!-- 填空题 -->
        <template v-if="qForm.questionType === 'fill_blank'">
          <el-form-item label="标准答案"><el-input v-model="qForm.correctAnswer" placeholder="精确匹配答案" /></el-form-item>
        </template>

        <!-- 编程题 -->
        <template v-if="qForm.questionType === 'programming'">
          <el-form-item label="输入格式"><el-input v-model="qForm.inputFormat" type="textarea" :rows="2" placeholder="描述输入数据格式" /></el-form-item>
          <el-form-item label="输出格式"><el-input v-model="qForm.outputFormat" type="textarea" :rows="2" placeholder="描述输出数据格式" /></el-form-item>
          <el-form-item label="样例输入"><el-input v-model="qForm.sampleInput" type="textarea" :rows="2" placeholder="示例输入数据" /></el-form-item>
          <el-form-item label="样例输出"><el-input v-model="qForm.sampleOutput" type="textarea" :rows="2" placeholder="示例输出数据" /></el-form-item>
          <el-form-item label="支持语言">
            <el-checkbox-group v-model="qForm.allowedLanguages">
              <el-checkbox value="java" label="Java" />
              <el-checkbox value="python" label="Python" />
              <el-checkbox value="c" label="C" />
              <el-checkbox value="cpp" label="C++" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="测试用例">
            <div class="testcase-editor">
              <div v-for="(tc, i) in qForm.testCases" :key="i" class="testcase-row">
                <div class="testcase-row__head">
                  <strong>用例 {{ i + 1 }}</strong>
                  <div><span>分值</span><el-input-number v-model="tc.score" :min="0" :max="100" size="small" />
                  <el-button v-if="qForm.testCases.length > 1" text type="danger" size="small" @click="removeTestCase(i)">删除</el-button></div>
                </div>
                <div class="testcase-row__body">
                  <el-input v-model="tc.input" type="textarea" :rows="2" placeholder="输入" />
                  <el-input v-model="tc.output" type="textarea" :rows="2" placeholder="期望输出" />
                </div>
              </div>
              <el-button text type="primary" @click="addTestCase">+ 添加测试用例</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 简答题 -->
        <template v-if="qForm.questionType === 'short_answer'">
          <el-form-item label="参考答案"><el-input v-model="qForm.correctAnswer" type="textarea" :rows="3" placeholder="参考答案（供阅卷参考）" /></el-form-item>
        </template>

        <el-form-item label="解析"><el-input v-model="qForm.analysis" type="textarea" :rows="2" placeholder="题目解析（可选）" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="qForm.tags" placeholder="逗号分隔，如：数组,排序,动态规划" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">{{ editingQuestion ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 阅卷抽屉 ==================== -->
    <el-drawer v-model="gradingDrawerVisible" title="阅卷详情" size="720px">
      <div v-if="currentAttempt" class="grading-drawer">
        <div class="grading-student">
          <strong>{{ currentAttempt.studentName }}</strong>
          <span>{{ currentAttempt.studentNo }}</span>
          <span>{{ currentAttempt.college || '-' }}</span>
          <span>{{ currentAttempt.labName || '-' }}</span>
          <el-tag size="small">切屏 {{ currentAttempt.switchCount }} 次</el-tag>
        </div>
        <div v-for="(ans, i) in currentAnswers" :key="i" class="grading-item">
          <div class="grading-item__head">
            <strong>第 {{ i + 1 }} 题</strong>
            <el-tag size="small" effect="plain">{{ qTypeLabel(ans.questionType) }}</el-tag>
          </div>
          <div v-if="ans.questionType === 'programming'" class="grading-code"><pre>{{ ans.code || '未作答' }}</pre><div v-if="ans.language" class="grading-code__lang">{{ ans.language }}</div></div>
          <div v-else class="grading-answer">{{ ans.answer || '未作答' }}</div>
          <div v-if="ans.correctAnswer" class="grading-correct">参考答案：{{ ans.correctAnswer }}</div>
          <div class="grading-item__score">
            <span>评分</span>
            <el-input-number v-model="ans.score" :min="0" :max="ans.maxScore || 100" size="small" />
            <el-input v-model="ans.graderRemark" placeholder="备注" size="small" style="flex:1" />
          </div>
        </div>
        <div class="grading-footer">
          <el-button type="primary" @click="submitGrading">保存阅卷</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- ==================== 面试邀请弹窗 ==================== -->
    <el-dialog v-model="showInviteDialog" title="发送面试邀请" width="480px">
      <p style="color:#64748b;margin:0 0 16px">将向所有通过笔试的学生发送面试邀请通知。</p>
      <el-form label-width="80px">
        <el-form-item label="面试标题"><el-input v-model="inviteForm.title" placeholder="如：2024秋季面试" /></el-form-item>
        <el-form-item label="面试说明"><el-input v-model="inviteForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSendInvitations">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Close, DataLine, EditPen, Files, MoreFilled, Plus, Tickets, TrendCharts } from '@element-plus/icons-vue'
import {
  createAdminExam, createAdminQuestion, deleteAdminExam, deleteAdminQuestion,
  getAdminExamList, getAdminExamStatistics, getAdminGradingList, getAdminQuestionList,
  getAdminStudentAnswer, publishAdminExam, publishAdminScores, sendInterviewInvitations,
  submitAdminGrading, updateAdminExam, updateAdminQuestion
} from '@/api/writtenExam'

// ========== Tabs ==========
const tabs = [
  { key: 'exam', label: '笔试管理', icon: EditPen },
  { key: 'question', label: '题库管理', icon: Files },
  { key: 'grading', label: '阅卷中心', icon: Tickets },
  { key: 'stats', label: '成绩统计', icon: TrendCharts }
]
const activeTab = ref('exam')
const overview = reactive({ examCount: 0, questionCount: 0, submissionCount: 0, avgScore: '-' })

// ========== Exam Management ==========
const exams = ref([])
const examLoading = ref(false)
const examSearch = ref('')
const examDialogVisible = ref(false)
const editingExam = ref(null)
const examForm = reactive({
  title: '', description: '', startTime: '', endTime: '', duration: 60,
  totalScore: 100, passScore: 60, enableAntiCheat: true, enableSignature: true, allowRetry: false, maxSwitchCount: 5
})

const filteredExams = computed(() => {
  if (!examSearch.value) return exams.value
  return exams.value.filter(e => e.title?.includes(examSearch.value))
})

const loadExams = async () => {
  examLoading.value = true
  try {
    const res = await getAdminExamList({ pageNum: 1, pageSize: 100 })
    exams.value = res.data?.records || res.data?.list || res.data || []
    overview.examCount = exams.value.length
  } finally { examLoading.value = false }
}

const showExamDialog = (exam = null) => {
  editingExam.value = exam
  if (exam) {
    Object.assign(examForm, { title: exam.title, description: exam.description, startTime: exam.startTime, endTime: exam.endTime, duration: exam.duration, totalScore: exam.totalScore, passScore: exam.passScore, enableAntiCheat: exam.enableAntiCheat, enableSignature: exam.enableSignature, allowRetry: exam.allowRetry, maxSwitchCount: exam.maxSwitchCount })
  } else {
    Object.assign(examForm, { title: '', description: '', startTime: '', endTime: '', duration: 60, totalScore: 100, passScore: 60, enableAntiCheat: true, enableSignature: true, allowRetry: false, maxSwitchCount: 5 })
  }
  examDialogVisible.value = true
}

const saveExam = async () => {
  if (!examForm.title) return ElMessage.warning('请填写笔试名称')
  try {
    if (editingExam.value) { await updateAdminExam(editingExam.value.id, examForm) }
    else { await createAdminExam(examForm) }
    ElMessage.success(editingExam.value ? '已更新' : '已创建')
    examDialogVisible.value = false
    await loadExams()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

const handleExamAction = async (cmd, exam) => {
  if (cmd === 'edit') { showExamDialog(exam); return }
  if (cmd === 'paper') { activeTab.value = 'question'; return }
  if (cmd === 'publish') {
    await ElMessageBox.confirm('发布后学生即可看到此笔试，确认发布？', '发布笔试')
    await publishAdminExam(exam.id); ElMessage.success('已发布'); await loadExams()
  }
  if (cmd === 'delete') {
    await ElMessageBox.confirm('确认删除此笔试？', '删除').catch(() => null)
    await deleteAdminExam(exam.id); ElMessage.success('已删除'); await loadExams()
  }
}

// ========== Question Bank ==========
const questions = ref([])
const qLoading = ref(false)
const qFilter = reactive({ type: '', difficulty: '', keyword: '' })
const qDialogVisible = ref(false)
const editingQuestion = ref(null)
const qForm = reactive({
  title: '', content: '', questionType: 'single_choice', difficulty: 'medium', score: 10,
  options: [{ label: 'A', text: '' }, { label: 'B', text: '' }, { label: 'C', text: '' }, { label: 'D', text: '' }],
  correctAnswer: '', analysis: '',
  inputFormat: '', outputFormat: '', sampleInput: '', sampleOutput: '',
  testCases: [{ input: '', output: '', score: 10 }],
  allowedLanguages: ['java', 'python', 'c', 'cpp'], tags: ''
})

const loadQuestions = async () => {
  qLoading.value = true
  try {
    const res = await getAdminQuestionList({ questionType: qFilter.type || undefined, difficulty: qFilter.difficulty || undefined, keyword: qFilter.keyword || undefined })
    questions.value = res.data?.records || res.data?.list || res.data || []
    overview.questionCount = questions.value.length
  } finally { qLoading.value = false }
}

const showQuestionDialog = (q = null) => {
  editingQuestion.value = q
  if (q) {
    const opts = typeof q.options === 'string' ? JSON.parse(q.options || '[]') : (q.options || [])
    const sc = typeof q.sampleCase === 'string' ? JSON.parse(q.sampleCase || '{}') : (q.sampleCase || {})
    const tc = typeof q.testCases === 'string' ? JSON.parse(q.testCases || '[]') : (q.testCases || [])
    const al = typeof q.allowedLanguages === 'string' ? JSON.parse(q.allowedLanguages || '[]') : (q.allowedLanguages || [])
    Object.assign(qForm, {
      title: q.title, content: q.content, questionType: q.questionType, difficulty: q.difficulty, score: q.score,
      options: opts.length ? opts : [{ label: 'A', text: '' }, { label: 'B', text: '' }, { label: 'C', text: '' }, { label: 'D', text: '' }],
      correctAnswer: q.correctAnswer || '', analysis: q.analysis || '',
      inputFormat: q.inputFormat || '', outputFormat: q.outputFormat || '',
      sampleInput: sc.input || '', sampleOutput: sc.output || '',
      testCases: tc.length ? tc : [{ input: '', output: '', score: 10 }],
      allowedLanguages: al.length ? al : ['java', 'python', 'c', 'cpp'],
      tags: Array.isArray(q.tags) ? q.tags.join(',') : (q.tags || '')
    })
  } else {
    Object.assign(qForm, {
      title: '', content: '', questionType: 'single_choice', difficulty: 'medium', score: 10,
      options: [{ label: 'A', text: '' }, { label: 'B', text: '' }, { label: 'C', text: '' }, { label: 'D', text: '' }],
      correctAnswer: '', analysis: '', inputFormat: '', outputFormat: '', sampleInput: '', sampleOutput: '',
      testCases: [{ input: '', output: '', score: 10 }], allowedLanguages: ['java', 'python', 'c', 'cpp'], tags: ''
    })
  }
  qDialogVisible.value = true
}

const addOption = () => { const next = String.fromCharCode(65 + qForm.options.length); qForm.options.push({ label: next, text: '' }) }
const removeOption = (i) => { qForm.options.splice(i, 1) }
const addTestCase = () => { qForm.testCases.push({ input: '', output: '', score: 10 }) }
const removeTestCase = (i) => { qForm.testCases.splice(i, 1) }

const saveQuestion = async () => {
  if (!qForm.title) return ElMessage.warning('请填写题目标题')
  const payload = {
    ...qForm,
    options: JSON.stringify(qForm.options),
    sampleCase: JSON.stringify({ input: qForm.sampleInput, output: qForm.sampleOutput }),
    testCases: JSON.stringify(qForm.testCases),
    allowedLanguages: JSON.stringify(qForm.allowedLanguages),
    tags: qForm.tags
  }
  delete payload.sampleInput; delete payload.sampleOutput
  try {
    if (editingQuestion.value) { await updateAdminQuestion(editingQuestion.value.id, payload) }
    else { await createAdminQuestion(payload) }
    ElMessage.success(editingQuestion.value ? '已更新' : '已创建')
    qDialogVisible.value = false; await loadQuestions()
  } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

const handleDeleteQuestion = async (q) => {
  await ElMessageBox.confirm(`确认删除题目「${q.title}」？`, '删除').catch(() => null)
  await deleteAdminQuestion(q.id); ElMessage.success('已删除'); await loadQuestions()
}

// ========== Grading ==========
const gradingExamId = ref(null)
const gradingKeyword = ref('')
const gradingList = ref([])
const gradingLoading = ref(false)
const gradingDrawerVisible = ref(false)
const currentAttempt = ref(null)
const currentAnswers = ref([])
const showInviteDialog = ref(false)
const inviteForm = reactive({ title: '', description: '' })

const handleSendInvitations = async () => {
  if (!inviteForm.title) return ElMessage.warning('请填写面试标题')
  try {
    const passedIds = gradingList.value.filter(r => r.totalScore >= (exams.value.find(e => e.id === gradingExamId.value)?.passScore || 60)).map(r => r.studentUserId || r.studentId)
    if (!passedIds.length) return ElMessage.warning('暂无通过的学生')
    await sendInterviewInvitations({ examId: gradingExamId.value, studentIds: passedIds, title: inviteForm.title, description: inviteForm.description })
    ElMessage.success(`已向 ${passedIds.length} 名学生发送面试邀请`)
    showInviteDialog.value = false
  } catch (e) { ElMessage.error(e?.response?.data?.message || '发送失败') }
}

const loadGradingList = async () => {
  gradingLoading.value = true
  try {
    const res = await getAdminGradingList({ examId: gradingExamId.value || undefined, keyword: gradingKeyword.value || undefined, pageNum: 1, pageSize: 200 })
    gradingList.value = res.data?.records || res.data?.list || res.data || []
    overview.submissionCount = gradingList.value.length
  } finally { gradingLoading.value = false }
}

const openGradingDrawer = async (row) => {
  currentAttempt.value = row
  try {
    const res = await getAdminStudentAnswer(row.id)
    currentAttempt.value = { ...row, ...(res.data || {}) }
    currentAnswers.value = res.data?.answers || []
  } catch { currentAnswers.value = [] }
  gradingDrawerVisible.value = true
}

const submitGrading = async () => {
  if (!currentAttempt.value) return
  const scores = currentAnswers.value.map(a => ({ id: a.id, questionId: a.questionId, score: a.score || 0, graderRemark: a.graderRemark || '' }))
  await submitAdminGrading(currentAttempt.value.id, { scores })
  ElMessage.success('阅卷已保存'); gradingDrawerVisible.value = false; await loadGradingList()
}

const handlePublishScores = async () => {
  await ElMessageBox.confirm('发布后学生可查看成绩，确认发布？', '发布成绩')
  await publishAdminScores(gradingExamId.value); ElMessage.success('成绩已发布')
}

// ========== Statistics ==========
const statsExamId = ref(null)
const statsData = reactive({ totalCount: 0, submitRate: '0%', avgScore: '-', passRate: '0%', maxScore: '-', minScore: '-', distribution: [], typeScores: [] })
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null, pieChart = null

const loadStatistics = async () => {
  if (!statsExamId.value) return
  try {
    const res = await getAdminExamStatistics(statsExamId.value)
    Object.assign(statsData, res.data || {})
    overview.avgScore = statsData.avgScore
    await nextTick(); buildCharts()
  } catch { /* ignore */ }
}

const buildCharts = () => {
  // Bar chart
  if (barChartRef.value) {
    if (barChart) barChart.dispose()
    barChart = echarts.init(barChartRef.value)
    const dist = statsData.distribution || [
      { range: '0-59', count: 0 }, { range: '60-69', count: 0 }, { range: '70-79', count: 0 },
      { range: '80-89', count: 0 }, { range: '90-100', count: 0 }
    ]
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 20, left: 50, right: 20, bottom: 30 },
      xAxis: { type: 'category', data: dist.map(d => d.range), axisLabel: { color: '#64748b' } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
      series: [{
        type: 'bar', data: dist.map(d => d.count), barWidth: 32, itemStyle: { borderRadius: [6, 6, 0, 0] },
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#818cf8' }, { offset: 1, color: '#6366f1' }])
      }]
    })
  }
  // Pie chart
  if (pieChartRef.value) {
    if (pieChart) pieChart.dispose()
    pieChart = echarts.init(pieChartRef.value)
    const ts = statsData.typeScores?.length ? statsData.typeScores : [{ name: '客观题', value: 60 }, { name: '编程题', value: 40 }]
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#64748b' } },
      series: [{
        type: 'pie', radius: ['40%', '70%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { formatter: '{b}\n{d}%', color: '#334155', fontSize: 12 },
        data: ts, color: ['#6366f1', '#f43f5e', '#f59e0b', '#10b981', '#06b6d4']
      }]
    })
  }
}

const handleResize = () => { barChart?.resize(); pieChart?.resize() }

// ========== Helpers ==========
const examStatusLabel = (s) => ({ draft: '草稿', published: '已发布', ongoing: '进行中', ended: '已结束' }[s] || s)
const examStatusType = (s) => ({ draft: 'info', published: 'success', ongoing: 'warning', ended: 'danger' }[s] || 'info')
const qTypeLabel = (t) => ({ single_choice: '单选', fill_blank: '填空', programming: '编程', short_answer: '简答', multi_choice: '多选', judge: '判断' }[t] || t)
const diffLabel = (d) => ({ easy: '简单', medium: '中等', hard: '困难' }[d] || d)
const diffType = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')
const attemptStatusLabel = (s) => ({ 0: '未开始', 1: '进行中', 2: '已提交', 3: '已批改', 4: '成绩已发布' }[s] || s || '未开始')
const attemptStatusType = (s) => ({ in_progress: 'warning', submitted: 'primary', graded: 'success', published: 'success', not_started: 'info' }[s] || 'info')
const formatDate = (v) => v ? dayjs(v).format('MM-DD HH:mm') : '-'

// ========== Lifecycle ==========
onMounted(async () => {
  await Promise.all([loadExams(), loadQuestions()])
  await loadGradingList()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  barChart?.dispose(); pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.hub-page { display: flex; flex-direction: column; gap: 20px; padding: 24px 28px; max-width: 1400px; margin: 0 auto; }

/* Header */
.hub-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px; padding: 28px 32px; border-radius: 20px; background: linear-gradient(135deg, #1e1b4b, #312e81, #4338ca); color: #fff; }
.hub-header h1 { margin: 0; font-size: 24px; font-weight: 700; }
.hub-header p { margin: 6px 0 0; color: rgba(255,255,255,.7); font-size: 13px; }
.hub-header__stats { display: flex; gap: 24px; }
.hub-stat { text-align: center; }
.hub-stat strong { display: block; font-size: 26px; font-weight: 700; }
.hub-stat span { font-size: 12px; color: rgba(255,255,255,.6); }

/* Tabs */
.hub-tabs { display: flex; gap: 6px; padding: 4px; background: #fff; border-radius: 14px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.hub-tab { display: flex; align-items: center; gap: 8px; padding: 10px 20px; border: none; border-radius: 10px; background: transparent; cursor: pointer; font-size: 14px; font-weight: 500; color: #64748b; transition: all .2s; }
.hub-tab:hover { background: #f8fafc; color: #334155; }
.hub-tab.active { background: #6366f1; color: #fff; box-shadow: 0 4px 12px rgba(99,102,241,.3); }
.hub-tab__icon { width: 18px; height: 18px; }

/* Section */
.hub-section { background: #fff; border-radius: 18px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,.04); padding: 24px; }
.section-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.section-bar h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1e293b; }
.section-bar__actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.grading-actions { justify-content: flex-end; gap: 12px; }
.grading-actions__select { width: 280px; }
.grading-actions :deep(.el-button) { min-width: 104px; }

/* Exam Cards */
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.exam-card { padding: 20px; border-radius: 16px; border: 1px solid #f1f5f9; background: linear-gradient(180deg, #fafbff, #fff); transition: transform .2s, box-shadow .2s; }
.exam-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,.06); }
.exam-card__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.exam-card__title { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0 0 6px; }
.exam-card__desc { font-size: 13px; color: #64748b; line-height: 1.5; margin: 0 0 10px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.exam-card__meta { display: flex; gap: 12px; font-size: 12px; color: #94a3b8; margin-bottom: 8px; }
.exam-card__time { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #94a3b8; }

/* Table */
.hub-table :deep(th) { background: #f8fafc !important; color: #475569 !important; font-weight: 600; }

/* Stats */
.stats-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; margin-bottom: 20px; }
.stats-card { padding: 18px; border-radius: 14px; text-align: center; border: 1px solid #f1f5f9; }
.stats-card strong { display: block; font-size: 28px; font-weight: 700; line-height: 1.2; }
.stats-card span { font-size: 12px; color: #64748b; }
.stats-card--purple { background: #faf5ff; } .stats-card--purple strong { color: #7c3aed; }
.stats-card--blue { background: #eff6ff; } .stats-card--blue strong { color: #2563eb; }
.stats-card--green { background: #f0fdf4; } .stats-card--green strong { color: #16a34a; }
.stats-card--amber { background: #fffbeb; } .stats-card--amber strong { color: #d97706; }
.stats-card--rose { background: #fff1f2; } .stats-card--rose strong { color: #e11d48; }
.stats-card--slate { background: #f8fafc; } .stats-card--slate strong { color: #475569; }
.stats-charts { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; }
.chart-card { padding: 20px; border-radius: 16px; border: 1px solid #f1f5f9; background: #fff; }
.chart-card h3 { margin: 0 0 12px; font-size: 15px; font-weight: 600; color: #1e293b; }
.chart-canvas { width: 100%; height: 280px; }

/* Form */
.hub-form .el-form-item { margin-bottom: 18px; }

/* Question dialog */
.options-editor { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.option-row { display: flex; align-items: center; gap: 8px; }
.option-label { width: 28px; height: 28px; border-radius: 8px; background: #6366f1; color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; flex-shrink: 0; }
.testcase-editor { display: flex; flex-direction: column; gap: 12px; width: 100%; }
.testcase-row { padding: 14px; border-radius: 12px; background: #f8fafc; border: 1px solid #f1f5f9; }
.testcase-row__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.testcase-row__head strong { color: #1e293b; font-size: 13px; }
.testcase-row__head div { display: flex; align-items: center; gap: 8px; }
.testcase-row__head span { font-size: 12px; color: #64748b; }
.testcase-row__body { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }

/* Grading drawer */
.grading-drawer { display: flex; flex-direction: column; gap: 16px; }
.grading-student { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: 12px; background: #f8fafc; }
.grading-student strong { font-size: 16px; color: #1e293b; }
.grading-student span { color: #64748b; font-size: 13px; }
.grading-item { padding: 16px; border-radius: 14px; border: 1px solid #f1f5f9; }
.grading-item__head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.grading-item__head strong { color: #1e293b; }
.grading-code { position: relative; border-radius: 10px; overflow: hidden; }
.grading-code pre { margin: 0; padding: 14px; background: #0f172a; color: #d1fae5; font-family: Consolas, 'Courier New', monospace; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; max-height: 240px; overflow-y: auto; }
.grading-code__lang { position: absolute; top: 8px; right: 10px; font-size: 11px; color: #6366f1; background: rgba(99,102,241,.15); padding: 2px 8px; border-radius: 6px; }
.grading-answer { padding: 12px 14px; border-radius: 10px; background: #f8fafc; color: #334155; }
.grading-correct { padding: 10px 14px; border-radius: 10px; background: #f0fdf4; color: #16a34a; font-size: 13px; margin-top: 8px; }
.grading-item__score { display: flex; align-items: center; gap: 10px; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f1f5f9; }
.grading-item__score span { font-size: 13px; color: #64748b; flex-shrink: 0; }
.grading-footer { padding-top: 12px; border-top: 1px solid #f1f5f9; display: flex; justify-content: flex-end; }

@media (max-width: 960px) {
  .hub-page { padding: 16px; }
  .hub-header { flex-direction: column; align-items: flex-start; }
  .hub-tabs { flex-wrap: wrap; }
  .card-grid { grid-template-columns: 1fr; }
  .stats-charts { grid-template-columns: 1fr; }
  .stats-cards { grid-template-columns: repeat(2, 1fr); }
  .testcase-row__body { grid-template-columns: 1fr; }
  .grading-actions { width: 100%; justify-content: flex-start; }
  .grading-actions__select { width: min(100%, 280px); }
}

@media (max-width: 768px) {
  .hub-page {
    padding: 0;
    gap: 16px;
  }

  .hub-header {
    padding: 22px;
    border-radius: 26px;
    color: #ffffff;
    background: linear-gradient(135deg, #15324b, #197a78 58%, #2aa3a1);
    box-shadow: 0 22px 48px rgba(15, 118, 110, 0.2);
  }

  .hub-header__info h1,
  .hub-header__info p {
    color: #ffffff;
  }

  .hub-header__stats {
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hub-stat {
    background: rgba(255, 255, 255, 0.13);
    border-color: rgba(255, 255, 255, 0.16);
  }

  .hub-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
    border-radius: 22px;
    scrollbar-width: none;
  }

  .hub-tabs::-webkit-scrollbar {
    display: none;
  }

  .hub-tab {
    flex: 0 0 auto;
    min-height: 44px;
    border-radius: 16px;
  }

  .hub-tab.active {
    background: #197a78;
    box-shadow: 0 10px 22px rgba(15, 118, 110, 0.18);
  }

  .hub-section {
    padding: 16px;
    border-radius: 24px;
  }

  .section-bar {
    align-items: stretch;
  }

  .section-bar__actions,
  .grading-actions {
    display: grid;
    grid-template-columns: 1fr;
    width: 100%;
  }

  .section-bar__actions :deep(.el-input),
  .section-bar__actions :deep(.el-select),
  .section-bar__actions :deep(.el-button),
  .grading-actions__select {
    width: 100% !important;
  }

  .exam-card,
  .exam-mobile-card,
  .stats-card,
  .chart-card {
    border-radius: 22px;
    border: 1px solid rgba(15, 118, 110, 0.12);
    box-shadow: 0 14px 34px rgba(23, 32, 51, 0.075);
  }

  .exam-card:hover {
    transform: none;
  }

  .exam-mobile-list {
    display: grid;
    gap: 12px;
  }

  .exam-mobile-card {
    padding: 15px;
    display: grid;
    gap: 12px;
    background: rgba(255, 255, 255, 0.96);
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

  .exam-mobile-card p {
    margin: 0;
    line-height: 1.7;
  }

  .exam-mobile-card__meta,
  .exam-mobile-card__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .exam-mobile-card__score {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .exam-mobile-card__score div {
    padding: 10px;
    border-radius: 14px;
    background: #f8fafc;
    display: grid;
    gap: 4px;
  }

  .exam-mobile-card__score span {
    color: #94a3b8;
    font-size: 12px;
    font-weight: 700;
  }

  .exam-mobile-card__score strong {
    color: #172033;
    font-size: 18px;
  }

  .stats-charts,
  .stats-cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 430px) {
  .hub-header__stats,
  .exam-mobile-card__score {
    grid-template-columns: 1fr;
  }

  .exam-mobile-card__head {
    flex-direction: column;
  }

  .exam-mobile-card__actions .el-button {
    flex: 1 1 0;
  }
}
</style>

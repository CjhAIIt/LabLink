<template>
  <div class="exam-result-wrapper">
    <div class="exam-result-card" v-loading="loading">
      <template v-if="submission">
        <div class="result-header" :class="resultClass">
          <div class="score-display">
            <span class="score-number">{{ submission.score ?? '--' }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="result-status">
            <el-tag :type="statusTagType" size="large" effect="dark" round>
              {{ statusText }}
            </el-tag>
          </div>
          <div class="pass-indicator" v-if="submission.status === 'GRADED'">
            <span :class="['pass-text', passed ? 'passed' : 'failed']">
              {{ passed ? '恭喜通过！' : '未达到及格分数' }}
            </span>
          </div>
        </div>

        <div class="result-details">
          <div class="detail-item">
            <span class="detail-label">提交时间</span>
            <span class="detail-value">{{ submission.submitTime }}</span>
          </div>
          <div class="detail-item" v-if="submission.totalScore">
            <span class="detail-label">试卷总分</span>
            <span class="detail-value">{{ submission.totalScore }} 分</span>
          </div>
          <div class="detail-item" v-if="submission.passScore">
            <span class="detail-label">及格分数</span>
            <span class="detail-value">{{ submission.passScore }} 分</span>
          </div>
        </div>

        <div class="interview-notice" v-if="submission.interviewInvitation">
          <el-alert
            title="面试通知"
            type="success"
            :closable="false"
            show-icon
          >
            <p>{{ submission.interviewInvitation }}</p>
          </el-alert>
        </div>

        <div class="action-section">
          <el-button type="primary" size="large" @click="goBack">
            返回考试中心
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentExamSubmission } from '@/api/writtenExam'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId
const examCenterPath = computed(() => resolveSurfacePathByRoute(route.path, '/student/exam-center'))

const loading = ref(false)
const submission = ref(null)

const passed = computed(() => {
  if (!submission.value || submission.value.status !== 'GRADED') return false
  return submission.value.score >= submission.value.passScore
})

const statusText = computed(() => {
  if (!submission.value) return ''
  return submission.value.status === 'GRADED' ? '已出分' : '待阅卷'
})

const statusTagType = computed(() => {
  if (!submission.value) return 'info'
  return submission.value.status === 'GRADED' ? 'success' : 'warning'
})

const resultClass = computed(() => {
  if (!submission.value || submission.value.status !== 'GRADED') return ''
  return passed.value ? 'result-passed' : 'result-failed'
})

const fetchResult = async () => {
  loading.value = true
  try {
    const res = await getStudentExamSubmission(examId)
    submission.value = res.data
  } catch (e) {
    ElMessage.error('获取考试结果失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push(examCenterPath.value)
}

onMounted(fetchResult)
</script>

<style scoped>
.exam-result-wrapper {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.exam-result-card {
  width: 100%;
  max-width: 600px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  padding: 48px 40px;
}

.result-header {
  text-align: center;
  margin-bottom: 36px;
  padding-bottom: 32px;
  border-bottom: 1px solid #f0f0f0;
}

.result-passed .score-number {
  color: #67c23a;
}

.result-failed .score-number {
  color: #f56c6c;
}

.score-display {
  margin-bottom: 16px;
}

.score-number {
  font-size: 72px;
  font-weight: 800;
  color: #409eff;
  line-height: 1;
}

.score-unit {
  font-size: 20px;
  color: #909399;
  margin-left: 4px;
  vertical-align: top;
}

.result-status {
  margin-bottom: 12px;
}

.pass-text {
  font-size: 18px;
  font-weight: 600;
}

.pass-text.passed {
  color: #67c23a;
}

.pass-text.failed {
  color: #f56c6c;
}

.result-details {
  margin-bottom: 28px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-label {
  font-size: 14px;
  color: #909399;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.interview-notice {
  margin-bottom: 28px;
}

.interview-notice p {
  margin: 4px 0 0;
  font-size: 14px;
}

.action-section {
  text-align: center;
}

.action-section .el-button {
  min-width: 200px;
  border-radius: 10px;
  font-size: 16px;
  height: 48px;
}

@media (max-width: 768px) {
  .exam-result-wrapper {
    padding: 0;
    min-height: auto;
    background: transparent;
  }

  .exam-result-card {
    padding: 28px 18px;
    border-radius: 26px;
    border: 1px solid rgba(51, 136, 187, 0.12);
    box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
  }

  .score-number {
    font-size: 56px;
  }

  .detail-item {
    flex-direction: column;
    gap: 6px;
    align-items: flex-start;
  }

  .action-section .el-button {
    width: 100%;
    min-width: 0;
    border-radius: 16px;
  }
}
</style>

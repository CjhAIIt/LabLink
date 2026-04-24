<template>
  <div class="exam-detail-wrapper">
    <div class="exam-detail-card" v-loading="loading">
      <template v-if="exam">
        <h1 class="exam-title">{{ exam.title }}</h1>
        <div class="exam-meta">
          <div class="meta-item">
            <span class="meta-label">所属实验室</span>
            <span class="meta-value">{{ exam.labName }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">考试时长</span>
            <span class="meta-value">{{ exam.duration }} 分钟</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">总分</span>
            <span class="meta-value">{{ exam.totalScore }} 分</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">及格分数</span>
            <span class="meta-value">{{ exam.passScore }} 分</span>
          </div>
        </div>

        <div class="exam-description" v-if="exam.description">
          <h3>考试说明</h3>
          <p>{{ exam.description }}</p>
        </div>

        <div class="anti-cheat-notice" v-if="exam.antiCheatEnabled">
          <el-alert
            title="防作弊提醒"
            type="warning"
            :closable="false"
            show-icon
          >
            <p>本场考试已开启防作弊功能，考试过程中请注意以下事项：</p>
            <ul>
              <li>请勿切换浏览器标签页或窗口</li>
              <li>请勿使用其他应用程序</li>
              <li>违规操作将被系统记录并可能影响成绩</li>
            </ul>
          </el-alert>
        </div>

        <div class="eligibility-section">
          <template v-if="eligibility">
            <el-alert
              v-if="eligibility.eligible"
              title="您已满足参加考试的条件"
              type="success"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else
              title="您暂不满足参加考试的条件"
              :description="eligibility.reason"
              type="error"
              :closable="false"
              show-icon
            />
          </template>
        </div>

        <div class="action-section" v-if="eligibility?.eligible">
          <el-button
            v-if="exam.signatureRequired"
            type="primary"
            size="large"
            @click="goToSign"
          >
            签名确认并开始
          </el-button>
          <el-button
            v-else
            type="primary"
            size="large"
            @click="goToTake"
          >
            直接开始
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getExamDetail, checkExamEligibility } from '@/api/writtenExam'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId
const examBasePath = computed(() => resolveSurfacePathByRoute(route.path, '/student/exam-center'))

const loading = ref(false)
const exam = ref(null)
const eligibility = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const [examRes, eligibilityRes] = await Promise.all([
      getExamDetail(examId),
      checkExamEligibility(examId)
    ])
    exam.value = examRes.data
    eligibility.value = eligibilityRes.data
  } catch (e) {
    ElMessage.error('获取考试信息失败')
  } finally {
    loading.value = false
  }
}

const goToSign = () => {
  router.push(`${examBasePath.value}/${examId}/sign`)
}

const goToTake = () => {
  router.push(`${examBasePath.value}/${examId}/take`)
}

onMounted(fetchData)
</script>

<style scoped>
.exam-detail-wrapper {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.exam-detail-card {
  width: 100%;
  max-width: 800px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 40px 36px;
}

.exam-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 28px;
  text-align: center;
}

.exam-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 28px;
}

.meta-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label {
  font-size: 13px;
  color: #909399;
}

.meta-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.exam-description {
  margin-bottom: 24px;
}

.exam-description h3 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 10px;
}

.exam-description p {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin: 0;
}

.anti-cheat-notice {
  margin-bottom: 24px;
}

.anti-cheat-notice ul {
  margin: 8px 0 0;
  padding-left: 18px;
}

.anti-cheat-notice li {
  font-size: 13px;
  line-height: 1.8;
}

.eligibility-section {
  margin-bottom: 32px;
}

.action-section {
  text-align: center;
}

.action-section .el-button {
  min-width: 200px;
  border-radius: 4px;
  font-size: 15px;
  height: 44px;
}

@media (max-width: 768px) {
  .exam-detail-wrapper {
    padding: 0;
    min-height: auto;
    background: transparent;
  }

  .exam-detail-card {
    padding: 22px 18px;
    border-radius: 26px;
    border: 1px solid rgba(51, 136, 187, 0.12);
    box-shadow: 0 16px 38px rgba(23, 32, 51, 0.08);
  }

  .exam-meta {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .exam-title {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .action-section .el-button {
    width: 100%;
    min-width: 0;
    border-radius: 16px;
  }
}
</style>

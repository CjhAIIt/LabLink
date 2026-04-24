<template>
  <div class="exam-sign-wrapper">
    <div class="exam-sign-card">
      <h1 class="sign-title">诚信考试承诺书</h1>

      <div class="pledge-card">
        <p>本人自愿参加本次考试，并郑重承诺：</p>
        <ol>
          <li>本人将严格遵守考试纪律和相关规定，诚信应考。</li>
          <li>考试过程中不以任何方式作弊或协助他人作弊，包括但不限于：抄袭、传递答案、使用未经允许的辅助工具等。</li>
          <li>不将考试内容以任何形式泄露或传播给他人。</li>
          <li>本人理解并接受，如有违反上述承诺的行为，愿意承担相应的纪律处分和后果。</li>
          <li>本人保证所提交的答案均为本人独立完成。</li>
        </ol>
        <p class="pledge-footer">本承诺书自签署之日起生效。</p>
      </div>

      <div class="agree-section">
        <el-checkbox v-model="agreed" label="我已阅读并同意以上承诺" />
      </div>

      <div class="signature-section">
        <label class="signature-label">请输入您的真实姓名作为电子签名</label>
        <el-input
          v-model="signatureName"
          placeholder="请输入真实姓名"
          size="large"
          :maxlength="20"
          clearable
        />
      </div>

      <div class="action-section">
        <el-button
          type="primary"
          size="large"
          :disabled="!canSubmit"
          :loading="submitting"
          @click="handleSubmit"
        >
          签名并开始考试
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitExamSignature } from '@/api/writtenExam'
import { resolveSurfacePathByRoute } from '@/utils/portal'

const route = useRoute()
const router = useRouter()
const examId = route.params.examId
const examBasePath = computed(() => resolveSurfacePathByRoute(route.path, '/student/exam-center'))

const agreed = ref(false)
const signatureName = ref('')
const submitting = ref(false)

const canSubmit = computed(() => agreed.value && signatureName.value.trim().length > 0)

const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await submitExamSignature({ examId, signature: signatureName.value.trim() })
    ElMessage.success('签名成功，即将进入考试')
    router.push(`${examBasePath.value}/${examId}/take`)
  } catch (e) {
    ElMessage.error('签名提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.exam-sign-wrapper {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.exam-sign-card {
  width: 100%;
  max-width: 600px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  padding: 48px 40px;
}

.sign-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
  margin: 0 0 32px;
}

.pledge-card {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 28px 24px;
  margin-bottom: 28px;
  background: #fafbfd;
}

.pledge-card p {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  margin: 0 0 12px;
}

.pledge-card ol {
  padding-left: 20px;
  margin: 0 0 16px;
}

.pledge-card li {
  font-size: 14px;
  color: #606266;
  line-height: 2;
}

.pledge-footer {
  color: #909399 !important;
  font-size: 13px !important;
  margin-bottom: 0 !important;
}

.agree-section {
  margin-bottom: 24px;
}

.signature-section {
  margin-bottom: 32px;
}

.signature-label {
  display: block;
  font-size: 14px;
  color: #303133;
  margin-bottom: 10px;
  font-weight: 500;
}

.signature-section .el-input {
  font-size: 16px;
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
  .exam-sign-wrapper {
    padding: 16px 12px;
    min-height: auto;
  }

  .exam-sign-card {
    padding: 24px 18px;
    border-radius: 16px;
  }

  .sign-title {
    font-size: 20px;
    margin-bottom: 22px;
  }

  .pledge-card {
    padding: 18px 16px;
  }

  .action-section .el-button {
    width: 100%;
    min-width: 0;
  }
}
</style>

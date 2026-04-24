<template>
  <div class="record-detail">
    <button class="back-btn" @click="$router.push('/admin/ai-interview-records')">
      <el-icon><ArrowLeft /></el-icon> 返回列表
    </button>

    <div v-loading="loading" v-if="record">
      <!-- Header Card -->
      <div class="detail-header">
        <div class="detail-header-bg"></div>
        <div class="detail-header-content">
          <div class="header-row">
            <div class="student-info">
              <h2>{{ record.studentName }}</h2>
              <div class="meta-tags">
                <span class="meta-tag">{{ record.moduleName }}</span>
                <span class="meta-tag">第{{ record.attemptNo }}次</span>
                <el-tag :type="record.status === '已完成' ? 'success' : 'danger'" size="small">{{ record.status }}</el-tag>
              </div>
            </div>
            <div class="header-score" :class="scoreLevel">
              <span class="big-score">{{ record.score }}</span>
              <span class="score-sub">/ 100</span>
            </div>
          </div>
          <div class="time-row">
            <span>开始：{{ record.startTime }}</span>
            <span>结束：{{ record.endTime }}</span>
          </div>
        </div>
      </div>

      <!-- Tags -->
      <div class="section" v-if="tags.length">
        <h3>能力标签</h3>
        <div class="tag-list">
          <span class="ability-tag" v-for="(t, i) in tags" :key="i">{{ t }}</span>
        </div>
      </div>

      <!-- Summary -->
      <div class="section" v-if="record.summary">
        <h3>AI 综合评价</h3>
        <div class="summary-box">{{ record.summary }}</div>
      </div>

      <!-- Conversation -->
      <div class="section" v-if="conversation.length">
        <h3>对话记录</h3>
        <div class="conversation">
          <div v-for="(msg, i) in conversation" :key="i" class="conv-msg" :class="msg.role">
            <div class="conv-avatar">{{ msg.role === 'assistant' ? '🤖' : '🧑‍💻' }}</div>
            <div class="conv-bubble">{{ msg.content }}</div>
          </div>
        </div>
      </div>

      <!-- Admin Actions -->
      <div class="admin-actions">
        <el-button type="warning" plain @click="handleInvalidate" v-if="record.status !== '作废'">作废此记录</el-button>
        <el-button type="primary" plain @click="handleResetChance">补偿该学生 1 次机会</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminRecordDetail, invalidateRecord, resetStudentChance } from '@/api/aiInterview'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const record = ref(null)

const tags = computed(() => { try { return JSON.parse(record.value?.tagsJson || '[]') } catch { return [] } })
const conversation = computed(() => { try { return JSON.parse(record.value?.conversationJson || '[]') } catch { return [] } })
const scoreLevel = computed(() => { const s = record.value?.score || 0; return s >= 80 ? 'high' : s >= 60 ? 'mid' : 'low' })

onMounted(async () => {
  try { const r = await getAdminRecordDetail(route.params.id); record.value = r?.data || null } catch {}
  loading.value = false
})

async function handleInvalidate() {
  try {
    await ElMessageBox.confirm('确定作废此记录？', '确认', { type: 'warning' })
    await invalidateRecord(record.value.id)
    ElMessage.success('已作废')
    record.value.status = '作废'
  } catch {}
}

async function handleResetChance() {
  try {
    await ElMessageBox.confirm(`确定为 ${record.value.studentName} 补偿 1 次面试机会？`, '确认')
    await resetStudentChance(record.value.studentId)
    ElMessage.success('已补偿')
  } catch {}
}
</script>

<style scoped>
.record-detail { max-width: 820px; margin: 0 auto; padding: 0 4px 40px; }
.back-btn { display: inline-flex; align-items: center; gap: 4px; background: none; border: 1px solid #e2e8f0; border-radius: 10px; padding: 8px 14px; font-size: 13px; color: #64748b; cursor: pointer; margin-bottom: 20px; transition: all .2s; }
.back-btn:hover { border-color: #0ea5e9; color: #0ea5e9; }

.detail-header { position: relative; border-radius: 18px; overflow: hidden; margin-bottom: 28px; background: linear-gradient(135deg, #0f172a, #1e293b); }
.detail-header-bg { position: absolute; inset: 0; background: radial-gradient(ellipse at 70% 20%, rgba(14,165,233,.2) 0%, transparent 60%); }
.detail-header-content { position: relative; padding: 32px 28px 24px; }
.header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.student-info h2 { font-size: 20px; font-weight: 700; color: #f1f5f9; margin: 0 0 8px; }
.meta-tags { display: flex; gap: 8px; align-items: center; }
.meta-tag { font-size: 12px; padding: 3px 10px; border-radius: 6px; background: rgba(255,255,255,.08); color: #94a3b8; }
.header-score { text-align: right; }
.big-score { font-size: 40px; font-weight: 800; }
.header-score.high .big-score { color: #34d399; }
.header-score.mid .big-score { color: #fbbf24; }
.header-score.low .big-score { color: #f87171; }
.score-sub { font-size: 14px; color: #64748b; }
.time-row { display: flex; gap: 24px; font-size: 12px; color: #64748b; }

.section { margin-bottom: 24px; }
.section h3 { font-size: 15px; font-weight: 600; color: #1e293b; margin: 0 0 12px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
.ability-tag { font-size: 13px; padding: 5px 14px; border-radius: 20px; background: rgba(14,165,233,.08); color: #0369a1; border: 1px solid rgba(14,165,233,.12); }
.summary-box { padding: 18px 20px; border-radius: 14px; background: #f8fafc; border: 1px solid #f1f5f9; font-size: 14px; color: #475569; line-height: 1.7; }

.conversation { display: flex; flex-direction: column; gap: 14px; max-height: 500px; overflow-y: auto; padding: 16px; border-radius: 14px; background: #fafbfc; border: 1px solid #f1f5f9; }
.conv-msg { display: flex; gap: 10px; max-width: 85%; }
.conv-msg.user { flex-direction: row-reverse; align-self: flex-end; }
.conv-avatar { width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; background: #f1f5f9; flex-shrink: 0; }
.conv-bubble { padding: 12px 16px; border-radius: 14px; font-size: 13px; line-height: 1.65; color: #334155; }
.conv-msg.assistant .conv-bubble { background: #fff; border: 1px solid #e2e8f0; }
.conv-msg.user .conv-bubble { background: #0ea5e9; color: #fff; }

.admin-actions { display: flex; gap: 12px; margin-top: 8px; padding-top: 20px; border-top: 1px solid #f1f5f9; }
</style>

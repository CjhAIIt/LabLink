<template>
  <div class="page-shell lab-detail-page">
    <section v-if="lab" class="page-hero detail-hero">
      <div class="hero-content">
        <div class="hero-text">
          <p class="eyebrow">{{ lab.labCode || 'LAB-CODE' }}</p>
          <h1>{{ lab.labName }}</h1>
          <p class="hero-subtitle">{{ lab.labDesc }}</p>
          <div class="hero-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>指导教师：{{ lab.teacherName || '待维护' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>地点：{{ lab.location || '待维护' }}</span>
            </div>
          </div>
        </div>
        <div class="hero-actions">
          <el-button type="primary" size="large" :disabled="!!userStore.userInfo?.labId" @click="handleApply">
            {{ userStore.userInfo?.labId ? '已加入实验室' : '立即申请加入' }}
          </el-button>
          <el-button size="large" @click="$router.back()">返回列表</el-button>
        </div>
      </div>
    </section>

    <div v-if="lab" class="detail-container">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="24" :md="16">
          <TablePageCard class="panel-card info-section" title="实验室介绍" subtitle="基础说明">
            <div class="rich-content">
              <p>{{ lab.basicInfo || '暂无详细介绍' }}</p>
            </div>
          </TablePageCard>

          <TablePageCard class="panel-card honor-section" title="荣誉展示" subtitle="成果沉淀">
            <div class="honor-list">
              <div v-if="lab.awards" class="honor-item">
                <p>{{ lab.awards }}</p>
              </div>
              <el-empty v-else description="暂无荣誉展示" :image-size="60" />
            </div>
          </TablePageCard>
        </el-col>

        <el-col :xs="24" :sm="24" :md="8">
          <TablePageCard
            class="panel-card senior-section"
            title="优秀学长"
            subtitle="成长样本"
            :count-label="`${parsedSeniors.length} 位`"
            count-tag-type="warning"
          >
            <div class="senior-list">
              <template v-if="parsedSeniors.length">
                <div v-for="(senior, index) in parsedSeniors" :key="index" class="senior-card">
                  <div class="senior-info">
                    <strong>{{ senior.name }}</strong>
                    <span class="senior-major">{{ senior.major }}</span>
                    <p class="senior-achievement">{{ senior.achievement }}</p>
                  </div>
                </div>
              </template>
              <el-empty v-else description="暂无优秀学长信息" :image-size="60" />
            </div>
          </TablePageCard>

          <TablePageCard class="panel-card skill-section" title="所需技能" subtitle="能力要求">
            <div class="skill-tags">
              <el-tag v-for="skill in skillList" :key="skill" effect="plain" class="skill-tag">
                {{ skill }}
              </el-tag>
              <span v-if="!skillList.length" class="empty-text">暂未明确技能要求</span>
            </div>
          </TablePageCard>
        </el-col>
      </el-row>
    </div>

    <div v-else-if="!loading" class="empty-panel">
      <el-empty description="未找到实验室详情" />
    </div>
  </div>
</template>

<script setup>
import { Location, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TablePageCard from '@/components/common/TablePageCard.vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const lab = ref(null)
const loading = ref(true)

const labId = computed(() => route.params.id)

const skillList = computed(() => {
  if (!lab.value?.requireSkill) return []
  return lab.value.requireSkill.split(/[，,、\s]+/).filter(Boolean)
})

const parsedSeniors = computed(() => {
  if (!lab.value?.outstandingSeniors) return []
  try {
    return JSON.parse(lab.value.outstandingSeniors)
  } catch (e) {
    console.error('Failed to parse seniors:', e)
    return []
  }
})

const fetchLabDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/labs/detail/${labId.value}`)
    lab.value = res.data
  } catch (e) {
    ElMessage.error('获取实验室详情失败')
  } finally {
    loading.value = false
  }
}

const handleApply = () => {
  if (userStore.userInfo?.labId) {
    ElMessage.warning('你已加入实验室，不能重复申请')
    return
  }
  if (!userStore.userInfo?.resume) {
    ElMessage.warning('请先到个人资料页提交简历后再申请实验室')
    router.push('/student/profile')
    return
  }
  router.push(`/student/labs?applyLabId=${labId.value}`)
}

onMounted(() => {
  fetchLabDetail()
})
</script>

<style scoped>
.lab-detail-page {
  background-color: #f8fafc;
}

.detail-hero {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: white;
  padding: 48px 60px;
  margin: -24px -24px 24px -24px;
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-text h1 {
  font-size: 32px;
  margin: 12px 0;
  color: white;
}

.hero-subtitle {
  font-size: 16px;
  color: #94a3b8;
  max-width: 600px;
  margin-bottom: 20px;
}

.hero-meta {
  display: flex;
  gap: 24px;
  color: #cbd5e1;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.panel-card {
  margin-bottom: 24px;
}

.rich-content {
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

.honor-item {
  padding: 12px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 4px;
  color: #92400e;
}

.senior-card {
  padding: 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.senior-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.senior-info strong {
  display: block;
  font-size: 16px;
  color: #1e293b;
}

.senior-major {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 8px 0;
  display: block;
}

.senior-achievement {
  font-size: 14px;
  color: #334155;
  margin: 0;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  border-radius: 6px;
}

.empty-text {
  color: #94a3b8;
  font-size: 14px;
}

@media (max-width: 768px) {
  .detail-hero {
    padding: 26px 20px;
    margin: -16px -12px 20px -12px;
    border-radius: 0 0 24px 24px;
  }

  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .hero-text h1 {
    font-size: 24px;
    line-height: 1.3;
  }

  .hero-subtitle {
    font-size: 14px;
    margin-bottom: 16px;
  }

  .hero-meta {
    flex-direction: column;
    gap: 10px;
  }

  .hero-actions {
    width: 100%;
    flex-direction: column;
  }

  .hero-actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .detail-container :deep(.el-row) {
    row-gap: 20px;
  }
}
</style>

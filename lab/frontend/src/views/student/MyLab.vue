<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">我的实验室</p>
          <h2>实验室成员与基础信息</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadLabData">刷新</el-button>
        </div>
      </div>
    </section>

    <div v-if="!userStore.userInfo?.labId" class="empty-panel">
      <el-empty description="你尚未加入实验室，可先前往“实验室查询”提交申请。" />
    </div>

    <template v-else>
      <section v-if="lab" class="content-grid two-column">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>实验室信息</span>
            </div>
          </template>

          <div class="detail-list">
            <div class="detail-item"><span>实验室名称</span><strong>{{ lab.labName }}</strong></div>
            <div class="detail-item"><span>实验室编码</span><strong>{{ lab.labCode || '未维护' }}</strong></div>
            <div class="detail-item"><span>指导教师</span><strong>{{ lab.teacherName || '待维护' }}</strong></div>
            <div class="detail-item"><span>地点</span><strong>{{ lab.location || '待维护' }}</strong></div>
            <div class="detail-item"><span>计划容量</span><strong>{{ lab.recruitNum || 0 }}</strong></div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>实验室简介</span>
            </div>
          </template>

          <p class="description-block">{{ lab.labDesc || '暂无简介' }}</p>
          <el-divider />
          <p class="description-block">{{ lab.basicInfo || '暂无基础信息' }}</p>
        </el-card>
      </section>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-header">
            <span>成员名单</span>
            <el-tag type="primary" effect="plain">{{ members.length }} 人</el-tag>
          </div>
        </template>

        <el-table v-loading="loading" :data="members" stripe>
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="major" label="专业" min-width="150" />
          <el-table-column prop="memberRole" label="角色" min-width="120">
            <template #default="{ row }">
              <el-tag :type="row.memberRole === 'lab_leader' ? 'success' : 'info'">
                {{ row.memberRole === 'lab_leader' ? '负责人' : '成员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="joinDate" label="加入日期" min-width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getLabById } from '@/api/lab'
import { getActiveLabMembers } from '@/api/labMembers'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const lab = ref(null)
const members = ref([])

const loadLabData = async () => {
  if (!userStore.userInfo?.labId) {
    return
  }
  loading.value = true
  try {
    const [labRes, memberRes] = await Promise.all([
      getLabById(userStore.userInfo.labId),
      getActiveLabMembers({ labId: userStore.userInfo.labId })
    ])
    lab.value = labRes.data || null
    members.value = memberRes.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLabData()
})
</script>

<style scoped>
.detail-list {
  display: grid;
  gap: 14px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.88);
}

.detail-item span {
  color: #64748b;
}

.description-block {
  line-height: 1.8;
  color: #475569;
}
</style>

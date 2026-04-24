<template>
  <div class="m-page">
    <section v-if="!managedLabId" class="panel-card">
      <el-empty description="当前账号没有绑定实验室管理范围。" :image-size="88" />
    </section>

    <template v-else>
      <section class="toolbar-card">
        <el-input v-model="keyword" clearable placeholder="搜索姓名、学号或专业" @keyup.enter="resetAndFetch" />
        <el-button plain :loading="loading" @click="resetAndFetch">刷新</el-button>
      </section>

      <section class="card-list">
        <article v-for="row in rows" :key="row.id || row.userId" class="record-card">
          <div class="card-head">
            <div>
              <strong>{{ row.realName || '未命名成员' }}</strong>
              <p>{{ row.studentId || row.username || '-' }}</p>
            </div>
            <span class="status-pill" :class="roleClass(row.memberRole)">{{ roleLabel(row.memberRole) }}</span>
          </div>

          <div class="meta-grid">
            <div><span>专业</span><strong>{{ row.major || '-' }}</strong></div>
            <div><span>年级</span><strong>{{ row.grade || '-' }}</strong></div>
            <div><span>加入日期</span><strong>{{ row.joinDate || '-' }}</strong></div>
            <div><span>联系方式</span><strong>{{ row.email || row.phone || '-' }}</strong></div>
          </div>

          <div class="action-row" v-if="canManageRow(row)">
            <el-button v-if="canPromote(row)" text type="primary" @click="appoint(row)">设为负责人</el-button>
            <el-button text type="danger" @click="remove(row)">移出实验室</el-button>
          </div>
        </article>

        <el-empty v-if="!loading && rows.length === 0" description="暂无成员记录" :image-size="84" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { appointLeader, getActiveLabMembers, removeLabMember } from '@/api/labMembers'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const rows = ref([])
const keyword = ref('')

const managedLabId = computed(() => userStore.userInfo?.managedLabId || userStore.userInfo?.labId || null)

const resetAndFetch = async () => {
  if (!managedLabId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const response = await getActiveLabMembers({ labId: managedLabId.value, keyword: keyword.value || undefined })
    rows.value = response.data || []
  } finally {
    loading.value = false
  }
}

const appoint = async (row) => {
  await appointLeader(row.id)
  ElMessage.success('已设置为负责人')
  await resetAndFetch()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认将 ${row.realName || row.studentId} 移出实验室吗？`, '移出确认', { type: 'warning' })
  await removeLabMember(row.id, { labId: managedLabId.value })
  ElMessage.success('成员已移出实验室')
  await resetAndFetch()
}

const canManageRow = (row) => {
  return row.memberRole !== 'lab_admin' && row.userId !== userStore.userInfo?.id
}

const canPromote = (row) => {
  return canManageRow(row) && row.memberRole !== 'leader' && row.memberRole !== 'lab_leader'
}

const roleLabel = (role) => ({
  lab_admin: '管理员',
  lab_leader: '负责人',
  leader: '负责人',
  member: '成员'
}[role] || '成员')

const roleClass = (role) => ({
  lab_admin: 'danger',
  lab_leader: 'success',
  leader: 'success',
  member: 'default'
}[role] || 'default')

onMounted(() => {
  resetAndFetch()
})
</script>

<style scoped>
.m-page {
  display: grid;
  gap: 14px;
}

.toolbar-card,
.panel-card,
.record-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.toolbar-card {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.card-list {
  display: grid;
  gap: 10px;
}

.record-card {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-head strong,
.meta-grid strong {
  color: #0f172a;
}

.card-head p,
.meta-grid span {
  color: #64748b;
}

.card-head p {
  margin: 6px 0 0;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.meta-grid span {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
}

.action-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-pill {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.success {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
}

.status-pill.danger {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.92);
}

.status-pill.default {
  color: #475569;
  background: rgba(241, 245, 249, 0.92);
}

@media (max-width: 560px) {
  .toolbar-card,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>

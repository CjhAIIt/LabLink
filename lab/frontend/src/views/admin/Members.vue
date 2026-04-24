<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">成员管理</p>
          <h2>成员名单、负责人任命与退出</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadMembers">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="成员姓名 / 学号" />
        </el-form-item>
        <el-form-item label="成员角色">
          <el-select v-model="filters.memberRole" clearable placeholder="全部角色" style="width: 140px">
            <el-option label="管理员" value="lab_admin" />
            <el-option label="负责人" value="lab_leader" />
            <el-option label="普通成员" value="member" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="在组" value="active" />
            <el-option label="已退出" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadMembers">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <TablePageCard title="成员名单" subtitle="实验室成员目录" :count-label="`${pagination.total} 条`">
      <el-table v-loading="loading" :data="members" stripe>
        <el-table-column prop="realName" label="成员姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="120" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column prop="memberRole" label="角色" min-width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.memberRole)">
              {{ roleLabel(row.memberRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'primary' : 'info'">
              {{ row.status === 'active' ? '在组' : '已退出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinDate" label="加入日期" min-width="120" />
        <el-table-column prop="quitDate" label="退出日期" min-width="120" />
        <el-table-column label="操作" fixed="right" min-width="220">
          <template #default="{ row }">
            <el-button
              v-if="canAppointLeader(row)"
              link
              type="primary"
              @click="handleAppointLeader(row)"
            >
              任命负责人
            </el-button>
            <el-button
              v-if="canRemoveMember(row)"
              link
              type="danger"
              @click="handleRemove(row)"
            >
              移出实验室
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </template>
    </TablePageCard>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { appointLeader, getLabMemberPage, removeLabMember } from '@/api/labMembers'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const members = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  keyword: '',
  memberRole: '',
  status: 'active'
})

const roleLabel = (memberRole) => ({
  lab_admin: '管理员',
  lab_leader: '负责人',
  leader: '负责人',
  member: '成员'
}[memberRole] || '成员')

const roleTagType = (memberRole) => ({
  lab_admin: 'danger',
  lab_leader: 'success',
  leader: 'success',
  member: 'info'
}[memberRole] || 'info')

const canAppointLeader = (row) => {
  return row.status === 'active'
    && row.memberRole !== 'lab_admin'
    && row.memberRole !== 'lab_leader'
    && row.memberRole !== 'leader'
}

const canRemoveMember = (row) => {
  return row.status === 'active'
    && row.memberRole !== 'lab_admin'
    && row.userId !== userStore.userInfo?.id
}

const loadMembers = async () => {
  loading.value = true
  try {
    const response = await getLabMemberPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      memberRole: filters.memberRole || undefined,
      status: filters.status || undefined
    })
    members.value = response.data.records || []
    pagination.total = response.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleAppointLeader = async (row) => {
  await ElMessageBox.confirm(`确认任命 ${row.realName} 为实验室负责人吗？`, '任命确认', { type: 'warning' })
  await appointLeader(row.id)
  ElMessage.success('负责人已任命')
  await loadMembers()
}

const handleRemove = async (row) => {
  const result = await ElMessageBox.prompt(`请输入移出 ${row.realName} 的说明`, '移出成员', {
    inputPlaceholder: '例如：毕业离组、项目结束等',
    confirmButtonText: '确认移出',
    cancelButtonText: '取消'
  }).catch(() => null)

  if (!result) {
    return
  }

  await removeLabMember(row.id, { remark: result.value })
  ElMessage.success('成员已移出实验室')
  await loadMembers()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadMembers()
}

onMounted(() => {
  loadMembers()
})
</script>

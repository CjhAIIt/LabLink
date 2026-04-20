<template>
  <div class="teacher-profile-page">
    <TablePageCard title="实验室成员资料" subtitle="教师查看">
      <template #header-extra>
        <el-tag type="info" effect="plain">{{ pagination.total }} items</el-tag>
        <el-button type="primary" @click="refresh">刷新</el-button>
      </template>
      <template #filters>
        <SearchToolbar
          v-model="filters.keyword"
          class="filter-form"
          placeholder="按姓名、学号、专业搜索"
          @search="handleSearch"
          @reset="resetFilters"
        >
          <el-form-item label="状态">
            <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
          </el-form-item>
        </SearchToolbar>
      </template>
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="140" />
        <el-table-column prop="major" label="专业" min-width="160" />
        <el-table-column prop="className" label="班级" min-width="140" />
        <el-table-column prop="direction" label="方向" min-width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.status" preset="profile" />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #pagination>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </template>
    </TablePageCard>

    <el-drawer v-model="detailVisible" title="资料详情" size="760px">
      <template v-if="detailLoading">
        <div class="drawer-loading">正在加载资料详情...</div>
      </template>

      <template v-else-if="detailProfile">
        <el-descriptions :column="2" border class="detail-card">
          <el-descriptions-item label="姓名">{{ detailProfile.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detailProfile.studentNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ detailProfile.collegeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实验室">{{ detailProfile.labName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ detailProfile.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ detailProfile.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detailProfile.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailProfile.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="方向">{{ detailProfile.direction || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :value="detailProfile.status" preset="profile" />
          </el-descriptions-item>
        </el-descriptions>

        <TablePageCard class="detail-section" title="个人介绍" subtitle="资料详情">
          <div class="multi-line">{{ detailProfile.introduction || '-' }}</div>
        </TablePageCard>

        <el-row :gutter="20">
          <el-col :xs="24" :lg="12">
            <TablePageCard class="detail-section" title="审核历史" subtitle="教师查看">
              <el-timeline v-if="reviewHistory.length">
                <el-timeline-item
                  v-for="item in reviewHistory"
                  :key="item.id"
                  :timestamp="formatDateTime(item.reviewTime || item.createTime)"
                  :type="timelineType(item.reviewStatus)"
                >
                  <div class="timeline-title">版本 {{ item.versionNo }} · {{ item.reviewStatus }}</div>
                  <div class="timeline-text">{{ item.reviewComment || '无审核意见' }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无审核历史" />
            </TablePageCard>
          </el-col>
          <el-col :xs="24" :lg="12">
            <TablePageCard class="detail-section" title="归档历史" subtitle="教师查看">
              <el-timeline v-if="archiveHistory.length">
                <el-timeline-item
                  v-for="item in archiveHistory"
                  :key="item.id"
                  :timestamp="formatDateTime(item.archivedAt || item.createTime)"
                  type="success"
                >
                  <div class="timeline-title">版本 {{ item.versionNo }} 已归档</div>
                  <div class="timeline-text">
                    {{ item.snapshot?.direction || item.snapshot?.major || '已生成归档快照' }}
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无归档历史" />
            </TablePageCard>
          </el-col>
        </el-row>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getProfileArchives, getProfileDetail, getProfilePage, getProfileReviews } from '@/api/profile'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const rows = ref([])
const detailProfile = ref(null)
const reviewHistory = ref([])
const archiveHistory = ref([])

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const fetchRows = async () => {
  loading.value = true
  try {
    const response = await getProfilePage({
      pageNum: pagination.current,
      pageSize: pagination.size,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    rows.value = response.data.records || []
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载资料列表失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  detailProfile.value = null
  reviewHistory.value = []
  archiveHistory.value = []

  try {
    const [detailRes, reviewRes, archiveRes] = await Promise.all([
      getProfileDetail(row.id),
      getProfileReviews(row.id),
      getProfileArchives(row.id)
    ])
    detailProfile.value = detailRes.data || null
    reviewHistory.value = reviewRes.data || []
    archiveHistory.value = archiveRes.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载资料详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchRows()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  pagination.current = 1
  fetchRows()
}

const refresh = () => {
  fetchRows()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchRows()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  fetchRows()
}

const timelineType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

const formatDateTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(
    2,
    '0'
  )}`
}

onMounted(() => {
  fetchRows()
})
</script>

<style scoped>
.teacher-profile-page {
  padding: 0;
}

.filter-form {
  margin-bottom: 12px;
}

.detail-card,
.detail-section {
  margin-bottom: 16px;
}

.drawer-loading {
  color: #64748b;
}

.multi-line {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.timeline-title {
  font-weight: 600;
  color: #0f172a;
}

.timeline-text {
  margin-top: 6px;
  color: #475569;
}
</style>

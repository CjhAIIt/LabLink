<template>
  <div class="profile-review-page">
    <TablePageCard title="资料审核中心" subtitle="成员资料">
      <template #header-extra>
        <el-tag type="info" effect="plain">共 {{ pagination.total }} 条</el-tag>
        <el-button type="primary" @click="refreshCurrentTab">刷新</el-button>
      </template>
      <template #filters>
        <SearchToolbar
          v-model="filters.keyword"
          class="filter-form"
          placeholder="按姓名、学号、专业搜索"
          @search="handleSearch"
          @reset="resetFilters"
        >
          <el-form-item v-if="activeTab === 'all'" label="状态">
            <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
          </el-form-item>
        </SearchToolbar>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="全部资料" name="all" />
      </el-tabs>

      <el-table v-loading="loading" :data="tableRows" stripe>
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="140" />
        <el-table-column prop="collegeName" label="学院" min-width="160" />
        <el-table-column prop="labName" label="实验室" min-width="160" />
        <el-table-column prop="major" label="专业" min-width="160" />
        <el-table-column prop="className" label="班级" min-width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.status" preset="profile" />
          </template>
        </el-table-column>
        <el-table-column :prop="activeTab === 'pending' ? 'submitTime' : 'updateTime'" :label="activeTab === 'pending' ? '提交时间' : '更新时间'" width="180">
          <template #default="{ row }">
            {{ formatDateTime(activeTab === 'pending' ? row.submitTime : row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="activeTab === 'pending'"
              size="small"
              type="success"
              @click="handleReviewAction('approve', row)"
            >
              通过
            </el-button>
            <el-button
              v-if="activeTab === 'pending'"
              size="small"
              type="danger"
              @click="handleReviewAction('reject', row)"
            >
              驳回
            </el-button>
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
          <el-descriptions-item label="资料附件" :span="2">
            <el-link
              v-if="detailProfile.attachmentUrl"
              :href="resolveFileUrl(detailProfile.attachmentUrl)"
              target="_blank"
              type="primary"
            >
              {{ getFileNameFromUrl(detailProfile.attachmentUrl, '成员资料附件') }}
            </el-link>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="简历材料" :span="2">
            <el-link
              v-if="detailProfile.resumeUrl && detailProfile.resumeUrl !== detailProfile.attachmentUrl"
              :href="resolveFileUrl(detailProfile.resumeUrl)"
              target="_blank"
              type="primary"
            >
              {{ getFileNameFromUrl(detailProfile.resumeUrl, '已上传简历') }}
            </el-link>
            <span v-else>{{ detailProfile.resumeUrl ? '同资料附件' : '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detailProfile.submittedAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近审核">{{ formatDateTime(detailProfile.lastReviewTime) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <TablePageCard class="detail-section" title="个人介绍" subtitle="资料详情">
          <div class="multi-line">{{ detailProfile.introduction || '-' }}</div>
        </TablePageCard>

        <TablePageCard class="detail-section" title="审核历史" subtitle="资料详情">
          <el-timeline v-if="reviewHistory.length">
            <el-timeline-item
              v-for="item in reviewHistory"
              :key="item.id"
              :timestamp="formatDateTime(item.reviewTime || item.createTime)"
              :type="timelineType(item.reviewStatus)"
            >
              <div class="timeline-title">版本 {{ item.versionNo }} · {{ item.reviewStatus }}</div>
              <div class="timeline-text">{{ item.reviewComment || '无审核意见' }}</div>
              <el-link
                v-if="item.snapshot?.attachmentUrl"
                :href="resolveFileUrl(item.snapshot.attachmentUrl)"
                target="_blank"
                type="primary"
              >
                查看该版本附件
              </el-link>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核历史" />
        </TablePageCard>

        <TablePageCard class="detail-section" title="归档历史" subtitle="资料详情">
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
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import { getFileNameFromUrl, resolveFileUrl } from '@/utils/file'
import {
  approveProfileReview,
  getPendingProfileReviewPage,
  getProfileArchives,
  getProfileDetail,
  getProfilePage,
  getProfileReviews,
  rejectProfileReview
} from '@/api/profile'

const activeTab = ref('pending')
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailProfile = ref(null)
const reviewHistory = ref([])
const archiveHistory = ref([])
const pendingRows = ref([])
const allRows = ref([])

const filters = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const tableRows = ref([])

const fetchTableData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.size,
      keyword: filters.keyword || undefined
    }

    if (activeTab.value === 'pending') {
      const response = await getPendingProfileReviewPage(params)
      pendingRows.value = response.data.records || []
      tableRows.value = pendingRows.value
      pagination.total = response.data.total || 0
      return
    }

    const response = await getProfilePage({
      ...params,
      status: filters.status || undefined
    })
    allRows.value = response.data.records || []
    tableRows.value = allRows.value
    pagination.total = response.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载资料列表失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  const profileId = row.profileId || row.id
  if (!profileId) {
    return
  }

  detailVisible.value = true
  detailLoading.value = true
  detailProfile.value = null
  reviewHistory.value = []
  archiveHistory.value = []

  try {
    const [detailRes, reviewRes, archiveRes] = await Promise.all([
      getProfileDetail(profileId),
      getProfileReviews(profileId),
      getProfileArchives(profileId)
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

const handleReviewAction = async (action, row) => {
  const actionLabel = action === 'approve' ? '通过' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入${actionLabel}该资料的审核意见。`,
      `${actionLabel}资料`,
      {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPattern: /[\s\S]{2,}/,
        inputErrorMessage: '审核意见至少输入 2 个字符'
      }
    )

    const payload = { reviewComment: value }
    if (action === 'approve') {
      await approveProfileReview(row.reviewId, payload)
      ElMessage.success('资料已审核通过')
    } else {
      await rejectProfileReview(row.reviewId, payload)
      ElMessage.success('资料已驳回')
    }

    if (detailVisible.value && detailProfile.value?.id === (row.profileId || row.id)) {
      await openDetail(row)
    }
    await fetchTableData()
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchTableData()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  pagination.current = 1
  fetchTableData()
}

const handleTabChange = () => {
  filters.status = ''
  pagination.current = 1
  fetchTableData()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchTableData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  fetchTableData()
}

const refreshCurrentTab = () => {
  fetchTableData()
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
  fetchTableData()
})
</script>

<style scoped>
.profile-review-page {
  padding: 0;
}

.filter-form {
  margin-bottom: 8px;
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

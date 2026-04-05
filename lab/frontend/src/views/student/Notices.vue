<template>
  <div class="page-shell">
    <section class="toolbar-card">
      <div class="toolbar-main">
        <div>
          <p class="eyebrow">公告中心</p>
          <h2>查看学校、学院和实验室范围内的公告信息</h2>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadNotices">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" :model="filters" class="toolbar-form">
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="公告标题 / 内容" />
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="filters.publishScope" clearable placeholder="全部范围" style="width: 140px">
            <el-option label="学校" value="school" />
            <el-option label="学院" value="college" />
            <el-option label="实验室" value="lab" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadNotices">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-card shadow="never" class="panel-card">
      <div v-if="!notices.length && !loading" class="empty-panel">
        <el-empty description="暂无公告" />
      </div>

      <div v-else v-loading="loading" class="notice-feed">
        <article v-for="notice in notices" :key="notice.id" class="notice-card">
          <div class="notice-card-head">
            <div>
              <p class="scope-pill">{{ scopeLabel(notice.publishScope) }}</p>
              <h3>{{ notice.title }}</h3>
            </div>
            <span>{{ formatDateTime(notice.publishTime) }}</span>
          </div>
          <p class="notice-card-content">{{ notice.content }}</p>
          <div class="notice-card-meta">
            <span>{{ notice.collegeName || '全校' }}</span>
            <span>{{ notice.labName || '公共公告' }}</span>
            <span>{{ notice.publisherName || '系统' }}</span>
          </div>
        </article>
      </div>

      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'
import { getNoticePage } from '@/api/notices'

const loading = ref(false)
const notices = ref([])

const filters = reactive({
  keyword: '',
  publishScope: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadNotices = async () => {
  loading.value = true
  try {
    const response = await getNoticePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      publishScope: filters.publishScope || undefined
    })
    notices.value = response.data?.records || []
    pagination.total = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  loadNotices()
}

const scopeLabel = (scope) => ({ school: '学校', college: '学院', lab: '实验室' })[scope] || scope
const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.notice-feed {
  display: grid;
  gap: 16px;
}

.notice-card {
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.96), rgba(236, 254, 255, 0.88));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.notice-card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.scope-pill {
  color: #0f766e;
  font-weight: 700;
}

.notice-card-content {
  color: #475569;
  line-height: 1.8;
}

.notice-card-meta {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

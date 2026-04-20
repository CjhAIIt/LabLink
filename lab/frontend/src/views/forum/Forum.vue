<template>
  <div class="forum-page">
    <TablePageCard title="交流论坛" subtitle="全部帖子与精选讨论" :count-label="`${total} 篇帖子`">
      <template #header-extra>
        <div class="forum-header-actions">
          <el-button @click="fetchPosts">
            <el-icon><RefreshRight /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            发布帖子
          </el-button>
        </div>
      </template>

      <template #filters>
        <div class="forum-toolbar">
          <el-tabs v-model="activeTab" class="forum-tabs" @tab-change="handleTabChange">
            <el-tab-pane label="全部帖子" name="all" />
            <el-tab-pane label="精选内容" name="essence" />
          </el-tabs>

          <SearchToolbar
            v-model="searchKeyword"
            keyword-label="帖子"
            placeholder="搜索标题或内容"
            keyword-width="240px"
            search-text="搜索"
            reset-text="重置"
            @search="handleSearch"
            @reset="resetFilters"
          >
            <template #actions>
              <span class="forum-toolbar__hint">
                <el-icon><Search /></el-icon>
                可按标题关键词或正文内容搜索
              </span>
            </template>
          </SearchToolbar>
        </div>
      </template>

      <div v-loading="loading" class="post-list">
        <el-card v-for="post in posts" :key="post.id" class="post-card" shadow="hover" @click="goToDetail(post.id)">
          <div class="post-main">
            <div class="post-title-row">
              <el-tag v-if="post.isPinned" type="danger" size="small" effect="dark">置顶</el-tag>
              <el-tag v-if="post.isEssence" type="warning" size="small" effect="dark">精选</el-tag>
              <h3 class="post-title">{{ post.title }}</h3>
            </div>
            <p class="post-preview">{{ getPreview(post.content) }}</p>
            <div class="post-meta">
              <span class="author">
                <el-avatar :size="20" :src="post.author?.avatar">
                  {{ (post.author?.realName || '匿').charAt(0) }}
                </el-avatar>
                {{ post.author?.realName || '匿名用户' }}
              </span>
              <span class="time">{{ formatTime(post.createTime) }}</span>
              <span class="stats">
                <el-icon><View /></el-icon> {{ post.viewCount }}
                <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
                <el-icon :class="{ liked: post.isLiked }"><Star /></el-icon> {{ post.likeCount }}
              </span>
            </div>
          </div>
        </el-card>
        <el-empty v-if="posts.length === 0" description="暂无帖子" />
      </div>

      <template #pagination>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchPosts"
        />
      </template>
    </TablePageCard>

    <el-dialog v-model="createDialogVisible" title="发布帖子" width="600px">
      <el-form :model="postForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="postForm.title" placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入帖子内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreatePost">发布</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, View, ChatDotRound, Star, RefreshRight } from '@element-plus/icons-vue'
import SearchToolbar from '@/components/common/SearchToolbar.vue'
import TablePageCard from '@/components/common/TablePageCard.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')
const searchKeyword = ref('')

const createDialogVisible = ref(false)
const submitting = ref(false)
const postForm = reactive({
  title: '',
  content: ''
})

const detailBasePath = computed(() => (route.path.startsWith('/admin') ? '/admin/forum/post' : '/student/forum/post'))

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/forum/post/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        isEssence: activeTab.value === 'essence'
      }
    })
    if (res.code === 200) {
      posts.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  fetchPosts()
}

const handleSearch = () => {
  pageNum.value = 1
  fetchPosts()
}

const resetFilters = () => {
  searchKeyword.value = ''
  activeTab.value = 'all'
  pageNum.value = 1
  fetchPosts()
}

const showCreateDialog = () => {
  postForm.title = ''
  postForm.content = ''
  createDialogVisible.value = true
}

const handleCreatePost = async () => {
  if (!postForm.title.trim() || !postForm.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  submitting.value = true
  try {
    const res = await request.post('/api/forum/post/add', postForm)
    if (res.code === 200) {
      ElMessage.success('帖子已发布')
      createDialogVisible.value = false
      fetchPosts()
    } else {
      ElMessage.error(res.message || '发布帖子失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '发布帖子失败')
  } finally {
    submitting.value = false
  }
}

const goToDetail = (id) => {
  router.push(`${detailBasePath.value}/${id}`)
}

const getPreview = (content) => {
  if (!content) {
    return ''
  }
  return content.length > 100 ? `${content.slice(0, 100)}...` : content
}

const formatTime = (timeStr) => {
  if (!timeStr) {
    return ''
  }
  return timeStr.replace('T', ' ').substring(0, 16)
}

onMounted(fetchPosts)
</script>

<style scoped>
.forum-page {
  max-width: 1000px;
  margin: 0 auto;
}

.forum-header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.forum-toolbar {
  display: grid;
  gap: 8px;
}

.forum-tabs {
  margin-bottom: 0;
}

.forum-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.forum-toolbar__hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.post-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.post-card:hover {
  transform: translateY(-2px);
}

.post-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.post-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.post-preview {
  color: #606266;
  font-size: 14px;
  margin: 0 0 12px 0;
  line-height: 1.5;
  white-space: pre-wrap;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 12px;
  color: #909399;
}

.author {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stats {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-left: auto;
}

.stats .el-icon {
  margin-right: 4px;
  font-size: 14px;
}

.liked {
  color: #e6a23c;
}
</style>

<template>
  <div class="post-detail-page">
    <el-button class="back-btn" link @click="$router.back()">
      <el-icon><Back /></el-icon>
      返回列表
    </el-button>

    <div v-loading="loading" class="content-wrapper">
      <el-card v-if="post" class="main-card">
        <div class="post-header">
          <h1 class="title">
            <el-tag v-if="post.isPinned" type="danger" effect="dark">置顶</el-tag>
            <el-tag v-if="post.isEssence" type="warning" effect="dark">精华</el-tag>
            {{ post.title }}
          </h1>
          <div class="meta-row">
            <div class="author-info">
              <el-avatar :size="40" :src="post.author?.avatar">
                {{ (post.author?.realName || 'U').charAt(0) }}
              </el-avatar>
              <div class="info-text">
                <span class="name">{{ post.author?.realName || '匿名用户' }}</span>
                <span class="time">{{ formatTime(post.createTime) }}</span>
              </div>
            </div>
            <div v-if="isAdmin" class="actions">
              <el-button size="small" :type="post.isPinned ? 'warning' : 'default'" @click="handlePin">
                {{ post.isPinned ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button size="small" :type="post.isEssence ? 'warning' : 'default'" @click="handleEssence">
                {{ post.isEssence ? '取消精华' : '设为精华' }}
              </el-button>
              <el-button size="small" type="danger" @click="handleDeletePost">删除帖子</el-button>
            </div>
          </div>
        </div>

        <div class="post-content">{{ post.content }}</div>

        <div class="post-footer">
          <div class="like-section">
            <el-button :type="post.isLiked ? 'warning' : 'default'" circle size="large" @click="handleLike">
              <el-icon><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
            </el-button>
            <span class="like-count">{{ post.likeCount }} 人觉得很赞</span>
          </div>
        </div>
      </el-card>

      <div class="comments-section">
        <h3>全部评论（{{ post?.commentCount || 0 }}）</h3>

        <div class="comment-input">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论"
          />
          <div class="input-actions">
            <el-button type="primary" :loading="commenting" @click="submitComment">发表评论</el-button>
          </div>
        </div>

        <div class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <el-avatar :size="32" :src="comment.author?.avatar">
              {{ (comment.author?.realName || 'U').charAt(0) }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-header">
                <span class="name">{{ comment.author?.realName || '匿名用户' }}</span>
                <span class="time">{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div v-if="isAdmin || isAuthor(comment)" class="comment-actions">
                <el-button type="danger" link size="small" @click="handleDeleteComment(comment.id)">
                  删除
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-if="comments.length === 0" description="暂无评论，快来抢沙发" />
        </div>

        <div v-if="total > 0" class="pagination">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="fetchComments"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Star, StarFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const post = ref(null)
const comments = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const commentContent = ref('')
const commenting = ref(false)

const isAdmin = computed(() => ['admin', 'super_admin'].includes(userStore.userInfo.role))

const isAuthor = (comment) => userStore.userInfo.id === comment.userId

const fetchPostDetail = async () => {
  loading.value = true
  try {
    const res = await request.get(`/api/forum/post/${route.params.id}`)
    if (res.code === 200) {
      post.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await request.get('/api/forum/comment/list', {
      params: {
        postId: route.params.id,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (res.code === 200) {
      comments.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
  }
}

const handleLike = async () => {
  try {
    const res = await request.post(`/api/forum/post/${post.value.id}/like`)
    if (res.code === 200) {
      post.value.isLiked = !post.value.isLiked
      post.value.likeCount += post.value.isLiked ? 1 : -1
    }
  } catch (error) {
    console.error(error)
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  commenting.value = true
  try {
    const res = await request.post('/api/forum/comment/add', {
      postId: post.value.id,
      content: commentContent.value
    })
    if (res.code === 200) {
      ElMessage.success('评论成功')
      commentContent.value = ''
      await fetchComments()
      post.value.commentCount += 1
    } else {
      ElMessage.error(res.message || '评论失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '评论失败')
  } finally {
    commenting.value = false
  }
}

const handlePin = async () => {
  try {
    await request.put(`/api/forum/post/${post.value.id}/pin`, null, {
      params: { isPinned: !post.value.isPinned }
    })
    post.value.isPinned = !post.value.isPinned
    ElMessage.success('操作成功')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleEssence = async () => {
  try {
    await request.put(`/api/forum/post/${post.value.id}/essence`, null, {
      params: { isEssence: !post.value.isEssence }
    })
    post.value.isEssence = !post.value.isEssence
    ElMessage.success('操作成功')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeletePost = () => {
  ElMessageBox.confirm('确定删除这篇帖子吗？删除后无法恢复。', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/api/forum/post/${post.value.id}`)
      ElMessage.success('删除成功')
      router.back()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

const handleDeleteComment = (id) => {
  ElMessageBox.confirm('确定删除这条评论吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/api/forum/comment/${id}`)
      ElMessage.success('删除成功')
      await fetchComments()
      post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - 1)
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

const formatTime = (timeStr) => {
  if (!timeStr) {
    return ''
  }
  return timeStr.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  fetchPostDetail()
  fetchComments()
})
</script>

<style scoped>
.post-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.back-btn {
  margin-bottom: 15px;
}

.main-card {
  margin-bottom: 20px;
}

.title {
  margin: 0 0 15px 0;
  font-size: 24px;
  color: #303133;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
  gap: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-text {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 600;
  color: #303133;
}

.time {
  font-size: 12px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  min-height: 100px;
  white-space: pre-wrap;
}

.post-footer {
  margin-top: 40px;
  text-align: center;
}

.like-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.like-count {
  font-size: 14px;
  color: #606266;
}

.comments-section {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.comments-section h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
}

.comment-input {
  margin-bottom: 30px;
}

.input-actions {
  margin-top: 10px;
  text-align: right;
}

.comment-item {
  display: flex;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f2f5;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-header .name {
  font-size: 14px;
  font-weight: 600;
}

.comment-header .time {
  font-size: 12px;
  color: #909399;
}

.comment-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-actions {
  margin-top: 5px;
  text-align: right;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>

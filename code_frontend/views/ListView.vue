<!-- src/views/ListView.vue -->
<template>
  <div id="app" class="container">
    <div class="header">
      <h1>🚀 GradPath AI 算法训练营</h1>
      <p>基于 DeepSeek 大模型，一键生成大厂面试算法题</p>

      <div class="search-box">
        <el-input v-model="keyword" placeholder="输入知识点（如：二叉树、动态规划、回溯算法）" class="custom-input" size="large" @keyup.enter="generate"></el-input>
        <el-button type="primary" size="large" @click="generate" :loading="loading" icon="Cpu">AI 出题</el-button>
      </div>
    </div>

    <div class="question-grid" v-loading="listLoading">
      <el-empty v-if="list.length === 0" description="暂无题目，快去生成一个吧！"></el-empty>

      <el-card class="box-card" v-for="item in list" :key="item.id" shadow="hover">
        <template #header>
          <div class="card-header">
            <span style="font-weight: bold; font-size: 16px;">{{ item.title }}</span>
            <el-tag :type="getDiffColor(item.difficulty)" effect="dark" size="small">{{ item.difficulty }}</el-tag>
          </div>
        </template>

        <div class="desc">{{ item.description }}</div>

        <div class="tags">
          <el-tag size="small" type="info" v-for="tag in parseTags(item.tags)" :key="tag" effect="plain">{{ tag }}</el-tag>
        </div>

        <div class="footer">
          <span style="font-size: 12px; color: #999;">ID: {{ item.id }}</span>
          <el-button type="primary" link @click="goToDetail(item.id)">
            开始挑战 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'  // 1. 导入 useRouter
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowRight, Cpu } from '@element-plus/icons-vue'

const router = useRouter()  // 2. 获取路由实例

const keyword = ref('')
const loading = ref(false)
const listLoading = ref(false)
const list = ref([])

// 查询列表
const fetchList = async () => {
  listLoading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/ai/list')
    if (res.data.code === 1) {
      list.value = res.data.data
    } else {
      list.value = res.data
    }
  } catch (err) {
    ElMessage.error('连接后端失败，请检查服务是否启动')
  } finally {
    listLoading.value = false
  }
}

// AI 生成
const generate = async () => {
  if (!keyword.value) return ElMessage.warning('请输入关键词')
  loading.value = true
  try {
    const res = await axios.post(`http://localhost:8080/api/ai/generate?keyword=${keyword.value}`)
    ElMessage.success('题目生成成功！')
    keyword.value = ''
    fetchList()
  } catch (err) {
    ElMessage.error('生成失败，AI 可能正在忙碌')
  } finally {
    loading.value = false
  }
}

const parseTags = (str) => {
  try { return JSON.parse(str) } catch (e) { return str ? str.split(/[,，]/) : [] }
}

const getDiffColor = (diff) => {
  if (diff === '简单') return 'success'
  if (diff === '困难') return 'danger'
  return 'warning'
}

// 3. 添加跳转函数
const goToDetail = (id) => {
  console.log('跳转到详情页，ID:', id)
  router.push(`/detail/${id}`)  // 使用 Vue Router 跳转
}

onMounted(() => fetchList())
</script>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; }
.header { text-align: center; margin-bottom: 40px; padding: 40px 0; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05); }
.header h1 { margin: 0 0 10px 0; color: #303133; }
.header p { color: #909399; margin: 0; }

.search-box { display: flex; justify-content: center; gap: 10px; margin-top: 20px; }
.custom-input { width: 500px; }

.question-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 20px; margin-top: 20px; }
.box-card { transition: all 0.3s; border: none; border-radius: 8px; }
.box-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }

.card-header { display: flex; justify-content: space-between; align-items: center; }
.desc { color: #606266; font-size: 14px; height: 40px; overflow: hidden; margin: 15px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.tags .el-tag { margin-right: 5px; }
.footer { border-top: 1px solid #ebeef5; padding-top: 15px; margin-top: 15px; display: flex; justify-content: space-between; align-items: center; }
</style>
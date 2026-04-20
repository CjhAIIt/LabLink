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

      <!-- 排序选择器 -->
      <div class="filter-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目标题、描述或标签..."
          class="search-input"
          size="default"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="default" @click="handleSearch">搜索</el-button>
        <el-select v-model="orderBy" placeholder="排序方式" size="default" @change="fetchList" style="margin-left: 10px;">
          <el-option label="按题号排序" value="id"></el-option>
          <el-option label="按难度排序" value="difficulty"></el-option>
          <el-option label="按创建时间排序" value="create_time"></el-option>
        </el-select>
        <el-select v-model="orderDirection" placeholder="排序方向" size="default" @change="fetchList" style="margin-left: 10px;">
          <el-option label="升序" value="ASC"></el-option>
          <el-option label="降序" value="DESC"></el-option>
        </el-select>
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

    <!-- 分页组件 -->
    <div class="pagination-box" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[9, 18, 27, 36]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowRight, Cpu, Search } from '@element-plus/icons-vue'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const listLoading = ref(false)
const list = ref([])

// 分页参数
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)

// 排序参数
const orderBy = ref('create_time')
const orderDirection = ref('DESC')

// 搜索参数
const searchKeyword = ref('')

// 查询列表
const fetchList = async () => {
  listLoading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/ai/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        orderBy: orderBy.value,
        orderDirection: orderDirection.value
      }
    })
    if (res.data.code === 1) {
      const data = res.data.data
      list.value = data.list
      total.value = data.total
      pageNum.value = data.pageNum
    } else {
      ElMessage.error('获取列表失败')
    }
  } catch (err) {
    ElMessage.error('连接后端失败，请检查服务是否启动')
  } finally {
    listLoading.value = false
  }
}

// 搜索事件
const handleSearch = () => {
  pageNum.value = 1
  fetchList()
}

// 分页事件
const handleSizeChange = (val) => {
  pageSize.value = val
  pageNum.value = 1
  fetchList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchList()
}

// AI 生成
const generate = async () => {
  if (!keyword.value) return ElMessage.warning('请输入关键词')
  loading.value = true
  try {
    const res = await axios.post(`http://localhost:8080/api/ai/generate?keyword=${keyword.value}`)
    ElMessage.success('题目生成成功！')
    keyword.value = ''
    pageNum.value = 1
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

const goToDetail = (id) => {
  console.log('跳转到详情页，ID:', id)
  router.push(`/detail/${id}`)
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

.filter-box { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 15px; }
.search-input { width: 400px; }

.question-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 20px; margin-top: 20px; }
.box-card { transition: all 0.3s; border: none; border-radius: 8px; }
.box-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }

.card-header { display: flex; justify-content: space-between; align-items: center; }
.desc { color: #606266; font-size: 14px; height: 40px; overflow: hidden; margin: 15px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.tags .el-tag { margin-right: 5px; }
.footer { border-top: 1px solid #ebeef5; padding-top: 15px; margin-top: 15px; display: flex; justify-content: space-between; align-items: center; }

.pagination-box { display: flex; justify-content: center; margin-top: 40px; margin-bottom: 40px; }
</style>
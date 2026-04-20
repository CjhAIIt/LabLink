<template>
  <div class="jobs-container">
    <div class="filter-box">
      <el-input v-model="search" placeholder="输入岗位，如：Java开发" class="search-input" size="large">
        <template #append><el-button icon="Search" @click="handleSearch">搜索</el-button></template>
      </el-input>
      <div class="tags">
        <span class="label">热门方向：</span>
        <el-tag v-for="tag in hotTags" :key="tag" class="tag-item" effect="plain" @click="search=tag">{{ tag }}</el-tag>
      </div>
    </div>

    <el-row :gutter="20" class="job-list">
      <el-col :span="6" v-for="job in jobs" :key="job.id">
        <el-card shadow="hover" class="job-card">
          <div class="job-header">
            <h3 class="job-title">{{ job.title }}</h3>
            <span class="salary">{{ job.salary }}</span>
          </div>
          <div class="company-info">
            <el-icon><OfficeBuilding /></el-icon> {{ job.company }}
            <el-tag size="small" type="info">{{ job.city }}</el-tag>
          </div>
          <div class="match-score">
            <span>AI 匹配度</span>
            <el-progress :percentage="job.matchScore" :color="getScoreColor(job.matchScore)" />
          </div>
          <el-divider />
          <div class="card-footer">
            <el-button text bg size="small" @click="generatePath(job.id)">生成学习路径</el-button>
            <el-button type="primary" size="small">投递简历</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const search = ref('')
const hotTags = ['Java后端', '前端Vue', 'Python爬虫', '大数据']
// 模拟数据 (后端对接时替换)
const jobs = ref([
  { id: 1, title: 'Java实习生', salary: '4k-6k', company: '字节跳动', city: '北京', matchScore: 85 },
  { id: 2, title: 'Web前端助理', salary: '5k-8k', company: '美团', city: '上海', matchScore: 60 },
  { id: 3, title: 'AI算法实习', salary: '8k-12k', company: 'DeepSeek', city: '杭州', matchScore: 92 },
])

const getScoreColor = (score) => score > 80 ? '#67c23a' : '#e6a23c'
const generatePath = (id) => alert(`正在调用AI分析岗位 ${id} 的JD，生成路径...`)
</script>

<style scoped>
.filter-box { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; text-align: center; }
.search-input { width: 50%; margin-bottom: 15px; }
.tag-item { margin-right: 10px; cursor: pointer; }
.job-card { margin-bottom: 20px; transition: transform 0.2s; }
.job-card:hover { transform: translateY(-5px); }
.job-header { display: flex; justify-content: space-between; font-weight: bold; }
.salary { color: #f56c6c; }
.company-info { margin: 10px 0; color: #666; font-size: 13px; display: flex; align-items: center; gap: 5px; }
.match-score { margin: 15px 0; font-size: 12px; color: #909399; }
.card-footer { display: flex; justify-content: space-between; }
</style>
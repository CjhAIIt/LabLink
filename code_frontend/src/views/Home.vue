<template>
  <div class="home">
    <el-card class="welcome-card">
      <h1>🎓 欢迎使用 GradPath 智途学习平台</h1>
      <p class="subtitle">一站式大学生职业规划与技能提升平台</p>

      <el-row :gutter="20" class="feature-cards">
        <el-col :span="6" v-for="feature in features" :key="feature.name">
          <el-card shadow="hover" class="feature-card" @click="$router.push(feature.path)">
            <div class="icon-wrapper">
              <component :is="feature.icon" :size="40" :color="feature.color" />
            </div>
            <h3>{{ feature.name }}</h3>
            <p>{{ feature.description }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { Cpu, Search, TrendCharts, Reading } from '@element-plus/icons-vue'

const features = [
  {
    name: '岗位雷达',
    description: '探索热门岗位信息',
    path: '/jobs',
    icon: Search,
    color: '#409EFF'
  },
  {
    name: '智能匹配',
    description: '分析能力差距',
    path: '/match',
    icon: TrendCharts,
    color: '#67C23A'
  },
  {
    name: '学习路径',
    description: '制定学习计划',
    path: '/learn',
    icon: Reading,
    color: '#E6A23C'
  },
  {
    name: '算法训练',
    description: 'AI智能出题',
    path: '/algo',
    icon: Cpu,
    color: '#F56C6C'
  }
]
</script>

<style scoped>
.home {
  padding: 20px;
}

.welcome-card {
  text-align: center;
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.welcome-card h1 {
  margin-bottom: 10px;
  font-size: 2.5rem;
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  margin-bottom: 40px;
}

.feature-cards {
  margin-top: 30px;
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.icon-wrapper {
  margin-bottom: 15px;
}

.feature-card h3 {
  margin: 10px 0;
  color: #303133;
}

.feature-card p {
  color: #909399;
  font-size: 14px;
}
</style>
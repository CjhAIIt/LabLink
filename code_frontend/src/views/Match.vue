<template>
  <div class="match-container">
    <div class="top-select">
      <span>当前分析目标：</span>
      <el-select v-model="targetJob" placeholder="选择你的意向岗位">
        <el-option label="Java高级开发 (字节跳动)" value="1" />
        <el-option label="全栈工程师 (独立开发)" value="2" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px">重新分析</el-button>
    </div>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="10">
        <el-card class="chart-card">
          <h3>能力匹配度分析</h3>
          <div class="radar-placeholder">
            <div class="score-circle">
              <span class="score">72</span>
              <span class="label">综合得分</span>
            </div>
            <p class="chart-tip">基于你的简历与做题记录生成</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <h3>差距分析与建议</h3>
          <el-table :data="gapData" style="width: 100%">
            <el-table-column prop="skill" label="核心技能" width="120" />
            <el-table-column label="现状 vs 要求">
              <template #default="scope">
                <el-progress
                    :percentage="scope.row.percentage"
                    :status="scope.row.percentage < 60 ? 'exception' : 'success'"
                />
                <small class="gap-text">{{ scope.row.desc }}</small>
              </template>
            </el-table-column>
            <el-table-column label="提升建议" width="120">
              <template #default="scope">
                <el-button link type="primary" @click="$router.push('/learn')">去学习</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const targetJob = ref('1')
const gapData = ref([
  { skill: 'Spring Cloud', percentage: 40, desc: '简历未提及，岗位要求熟练' },
  { skill: 'Redis', percentage: 80, desc: '掌握基本操作，缺乏实战经验' },
  { skill: '算法', percentage: 20, desc: 'LeetCode 刷题量不足 50 道' },
])
</script>

<style scoped>
.top-select { background: #fff; padding: 20px; border-radius: 8px; display: flex; align-items: center; }
.radar-placeholder { height: 300px; background: #f0f9eb; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 50%; width: 300px; margin: 0 auto; }
.score-circle { text-align: center; }
.score { font-size: 48px; font-weight: bold; color: #67c23a; display: block; }
.gap-text { color: #909399; font-size: 12px; }
</style>
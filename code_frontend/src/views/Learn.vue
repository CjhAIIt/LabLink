<template>
  <div class="learn-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card header="📚 我的学习计划">
          <el-menu default-active="1" class="path-menu">
            <el-menu-item index="1"><el-icon><Promotion /></el-icon>Java后端工程师（进行中）</el-menu-item>
            <el-menu-item index="2"><el-icon><Lock /></el-icon>分布式架构（待解锁）</el-menu-item>
          </el-menu>
          <el-button class="add-btn" type="primary" plain style="width: 100%; margin-top: 10px;">+ AI 生成新计划</el-button>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card>
          <template #header>
            <div class="path-header">
              <h2>Java 后端工程师成长路径</h2>
              <el-progress type="circle" :percentage="35" width="60" />
            </div>
          </template>

          <el-timeline>
            <el-timeline-item
                v-for="(step, index) in learningPath"
                :key="index"
                :timestamp="step.time"
                :type="step.status === 'done' ? 'success' : 'primary'"
                placement="top"
            >
              <el-card class="step-card">
                <h4>{{ step.title }}</h4>
                <p class="desc">{{ step.desc }}</p>

                <div class="resource-box">
                  <el-tag type="danger" effect="dark">Bilibili</el-tag>
                  <a :href="step.videoUrl" target="_blank" class="link-text">
                    {{ step.videoName }} <el-icon><TopRight /></el-icon>
                  </a>
                </div>

                <div class="action-box">
                  <el-button type="primary" size="small" @click="goToTest(step.keyword)">去刷题巩固</el-button>
                  <el-checkbox v-model="step.isFinished" label="标记为已学完" border size="small" />
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()

const learningPath = ref([
  {
    title: '阶段一：Java 基础语法',
    time: '预计 7 天',
    status: 'done',
    desc: '掌握变量、循环、面向对象编程思想。',
    videoUrl: 'https://www.bilibili.com/video/BV12J41137hu', // 尚硅谷/黑马链接
    videoName: '尚硅谷Java零基础入门教程',
    keyword: 'Java基础'
  },
  {
    title: '阶段二：MySQL 数据库',
    time: '预计 5 天',
    status: 'process',
    desc: '掌握 SQL 语句书写、索引原理与事务。',
    videoUrl: 'https://www.bilibili.com/video/BV1Kr4y1i7ru',
    videoName: 'MySQL数据库入门到精通',
    keyword: 'MySQL'
  }
])

const goToTest = (keyword) => {
  router.push(`/algo?keyword=${keyword}`)
}
</script>

<style scoped>
.path-header { display: flex; justify-content: space-between; align-items: center; }
.step-card { background: #f8fcfb; }
.resource-box { margin: 10px 0; background: #fff; padding: 10px; border-radius: 4px; border: 1px dashed #dcdfe6; }
.link-text { margin-left: 10px; color: #409EFF; text-decoration: none; font-weight: bold; }
.link-text:hover { text-decoration: underline; }
.action-box { display: flex; justify-content: space-between; margin-top: 10px; }
</style>
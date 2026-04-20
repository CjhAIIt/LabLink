<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="user-card">
          <div class="avatar-area">
            <el-avatar :size="100" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <h2>张三</h2>
            <p>吉林大学 | 软件工程 | 2026届</p>
          </div>
          <el-divider />
          <div class="resume-section">
            <h4>我的简历</h4>
            <el-upload
                class="upload-demo"
                drag
                action="http://localhost:8080/api/user/uploadResume"
                multiple
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">拖拽简历到此处 或 <em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">支持 PDF/Word，AI 自动解析技能点</div>
              </template>
            </el-upload>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card header="学习大数据" class="stats-card">
          <el-row>
            <el-col :span="8" class="stat-item">
              <div class="num">128</div>
              <div class="label">已刷题目</div>
            </el-col>
            <el-col :span="8" class="stat-item">
              <div class="num">32</div>
              <div class="label">学习小时</div>
            </el-col>
            <el-col :span="8" class="stat-item">
              <div class="num">85%</div>
              <div class="label">代码通过率</div>
            </el-col>
          </el-row>
        </el-card>

        <el-card header="最近提交" style="margin-top: 20px;">
          <el-timeline>
            <el-timeline-item timestamp="2026/01/25" placement="top">
              <el-card>
                <h4>反转二叉树</h4>
                <p>提交状态：<el-tag type="success">AC</el-tag> 耗时：2ms</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.user-card { text-align: center; }
.avatar-area { padding: 20px 0; }
.stat-item { text-align: center; border-right: 1px solid #eee; }
.stat-item:last-child { border-right: none; }
.num { font-size: 24px; font-weight: bold; color: #409EFF; }
.label { color: #909399; margin-top: 5px; }
</style>
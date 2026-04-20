<template>
  <div class="detail-container">

    <div class="left-panel">
      <div class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回题库
      </div>

      <div v-if="loading" class="loading-box">加载中...</div>
      <div v-else>
        <h2 class="q-title">{{ question.title }}</h2>
        <div class="q-meta">
          <el-tag :type="getDiffColor(question.difficulty)">{{ question.difficulty }}</el-tag>
          <el-tag v-for="tag in parseTags(question.tags)" :key="tag" type="info" effect="plain">
            {{ tag }}
          </el-tag>
        </div>

        <div class="q-desc">
          <h4>题目描述</h4>
          <p>{{ question.description }}</p>
        </div>

        <div class="sample-block">
          <div class="sample-label">输入格式：</div>
          <div class="sample-text">{{ question.inputFormat }}</div>
          <div class="sample-label" style="margin-top:10px">输出格式：</div>
          <div class="sample-text">{{ question.outputFormat }}</div>
        </div>

        <div class="sample-block">
          <div class="sample-label">样例输入：</div>
          <div class="sample-code">{{ sampleData.input }}</div>
          <div class="sample-label" style="margin-top:10px">样例输出：</div>
          <div class="sample-code">{{ sampleData.output }}</div>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <div class="toolbar">
        <el-select v-model="language" size="small" style="width: 120px" @change="changeLang">
          <el-option label="Java" value="java"></el-option>
          <el-option label="Python" value="python"></el-option>
          <el-option label="C++" value="cpp"></el-option>
        </el-select>

        <div class="btn-group">
          <el-button type="info" plain icon="VideoPlay" @click="handleRun('debug')" :loading="running">
            自测运行
          </el-button>
          <el-button type="success" icon="CircleCheckFilled" @click="handleRun('submit')" :loading="running">
            提交代码
          </el-button>
        </div>
      </div>

      <div id="monaco-editor" class="editor-container"></div>

      <el-drawer
          v-model="showConsole"
          :title="runMode === 'debug' ? '自测控制台' : '判题结果'"
          direction="btt"
          :size="drawerSize"
          :modal="false"
      >
        <div class="console-body">

          <div class="status-bar" :class="statusClass">
            <div class="status-left">
              <el-icon size="18"><component :is="statusIcon" /></el-icon>
              <span class="status-text">{{ statusText }}</span>
            </div>

            <el-button
                v-if="statusClass === 'error'"
                type="primary"
                class="ai-btn"
                size="small"
                icon="MagicStick"
                @click="askAI"
                :loading="aiLoading"
            >
              ✨ AI 帮我分析错误
            </el-button>
          </div>

          <div class="output-box">
            <div v-if="stdout">
              <div class="label">标准输出 (Stdout):</div>
              <pre class="code-block">{{ stdout }}</pre>
            </div>

            <div v-if="errorMsg">
              <div class="label error-label">错误信息 / 编译报错:</div>
              <pre class="code-block error-block">{{ errorMsg }}</pre>
            </div>

            <div v-if="!stdout && !errorMsg" class="label">无输出</div>
          </div>

          <div v-if="aiAnalysis" class="ai-box">
            <div class="ai-header">
              <span class="ai-title">🤖 AI 诊断报告</span>
              <el-button link size="small" @click="aiAnalysis=''">清空</el-button>
            </div>
            <div class="ai-content" v-html="aiAnalysis"></div>
          </div>

        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowLeft, VideoPlay, CircleCheckFilled, CircleCloseFilled, InfoFilled, MagicStick } from '@element-plus/icons-vue'
// 改成直接引用，不要用 CDN 链接
import * as monaco from 'monaco-editor'
const router = useRouter()
const route = useRoute()

// === 状态变量 ===
const question = ref({})
const loading = ref(true)
const running = ref(false)
const language = ref('java')
let editor = null

// 运行结果相关
const showConsole = ref(false)
const runMode = ref('debug') // 'debug' | 'submit'
const statusText = ref('')
const statusClass = ref('info') // 'info', 'success', 'error'
const statusIcon = ref('InfoFilled')
const stdout = ref('')
const errorMsg = ref('')

// AI 相关
const aiLoading = ref(false)
const aiAnalysis = ref('')
const drawerSize = computed(() => aiAnalysis.value ? '70%' : '400px') // 有AI回复时拉高抽屉

// 模板代码
const templates = {
  java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 请在此处编写代码\n        // 示例：int a = sc.nextInt();\n    }\n}`,
  python: `# 请在此处编写代码\nimport sys\n# for line in sys.stdin:\n#     print(line)`,
  cpp: `#include <iostream>\nusing namespace std;\n\nint main() {\n    // 请在此处编写代码\n    return 0;\n}`,
}

// 解析样例数据 (防止 JSON 解析报错)
const sampleData = computed(() => {
  try {
    return JSON.parse(question.value.sampleCase || '{}')
  } catch (e) {
    return { input: '', output: '' }
  }
})

// 获取题目详情
const loadDetail = async () => {
  const id = route.params.id
  if (!id) return
  try {
    const res = await axios.get(`http://localhost:8080/api/ai/detail/${id}`)
    if (res.data.code === 1) {
      question.value = res.data.data
      if (editor && !editor.getValue()) {
        editor.setValue(templates[language.value])
      }
    }
  } catch (e) {
    ElMessage.error('题目加载失败')
  } finally {
    loading.value = false
  }
}

// ---------------------- 核心运行逻辑 ----------------------

/**
 * 统一处理运行和提交
 * @param mode 'debug'(自测) 或 'submit'(提交)
 */
const handleRun = async (mode) => {
  if (!editor) return

  // 1. 重置状态
  running.value = true
  runMode.value = mode
  showConsole.value = true
  stdout.value = ''
  errorMsg.value = ''
  aiAnalysis.value = ''

  statusText.value = mode === 'debug' ? '正在编译运行...' : '正在判题...'
  statusClass.value = 'info'
  statusIcon.value = 'InfoFilled'

  try {
    // 2. 构造请求参数
    const url = mode === 'debug' ? '/api/judge/debug' : '/api/judge/submit'
    const payload = {
      language: language.value,
      code: editor.getValue(),
      input: sampleData.value.input,
      output: mode === 'submit' ? sampleData.value.output : null,
      questionTitle: question.value.title
    }

    console.log("发送请求:", payload) // 添加日志

    // 3. 发送请求
    const res = await axios.post(`http://localhost:8080${url}`, payload)
    console.log("收到响应:", res.data) // 添加日志

    // 检查响应格式
    if (res.data.code === 1) {
      // 成功响应，处理数据
      processResult(res.data.data, mode)
    } else {
      throw new Error(res.data.msg || '判题服务响应异常')
    }

  } catch (e) {
    console.error('运行出错:', e)
    statusText.value = '系统错误: ' + (e.message || '连接失败')
    statusClass.value = 'error'
    statusIcon.value = 'CircleCloseFilled'

    // 显示详细错误信息
    if (e.response) {
      errorMsg.value = JSON.stringify(e.response.data, null, 2)
    } else {
      errorMsg.value = e.message
    }
  } finally {
    running.value = false
  }
}
// 处理判题机返回的数据
const processResult = (data, mode) => {
  // 1. 直接从 data 中提取，不要找 .run
  stdout.value = data.stdout || ''

  // 2. 后端如果判题失败，错误信息在 error 字段里
  errorMsg.value = data.error || ''

  // 3. 匹配后端定义的下划线命名 exit_code
  const exitCode = data.exit_code

  if (mode === 'debug') {
    // === 自测模式 ===
    // 只要状态是 success 或者 exitCode 为 0
    if (data.status === 'success' || exitCode === 0) {
      statusText.value = '✅ 自测运行完成'
      statusClass.value = 'success'
      statusIcon.value = 'VideoPlay'
      errorMsg.value = '' // 清空之前的错误
    } else {
      statusText.value = '❌ 运行出错'
      statusClass.value = 'error'
      statusIcon.value = 'CircleCloseFilled'
      // 如果后端没给具体 error 字符串，再手动拼接退出码
      if (!data.error) {
        errorMsg.value = `程序退出代码: ${exitCode}`
      }
    }
  } else {
    // === 提交模式 ===
    if (data.status === 'success' || exitCode === 0) {
      // 优先使用后端传回的 correct 判题结果
      if (data.correct === true) {
        statusText.value = '✅ 恭喜！通过测试用例 (Accepted)'
        statusClass.value = 'success'
        statusIcon.value = 'CircleCheckFilled'
      } else if (data.status === 'wrong_answer') {
        statusText.value = '❌ 答案错误 (Wrong Answer)'
        statusClass.value = 'error'
        statusIcon.value = 'CircleCloseFilled'
        errorMsg.value = data.error || '输出与预期不符'
      }
    } else {
      statusText.value = '❌ 运行出错'
      statusClass.value = 'error'
      errorMsg.value = data.error || '执行过程中出现异常'
    }
  }
}
// ---------------------- AI 辅助逻辑 ----------------------

const askAI = async () => {
  aiLoading.value = true
  try {
    const res = await axios.post('http://localhost:8080/api/judge/analyze', {
      questionTitle: question.value.title,
      code: editor.getValue(),
      errorMsg: errorMsg.value || '结果不正确 (Wrong Answer)',
      output: sampleData.value.output // 期望输出
    })

    if (res.data.code === 1) {
      // 简单处理换行，使其在 HTML 中显示
      aiAnalysis.value = res.data.data.replace(/\n/g, '<br>')
    } else {
      ElMessage.error(res.data.msg || 'AI 分析失败')
    }
  } catch (e) {
    ElMessage.error('无法连接到 AI 服务')
  } finally {
    aiLoading.value = false
  }
}

// ---------------------- Monaco Editor 配置 ----------------------
const initEditor = () => {
  if (typeof window.monaco !== 'undefined') {
    require.config({ paths: { 'vs': 'https://cdn.staticfile.org/monaco-editor/0.30.1/min/vs' } })
    createMonaco()
  } else {
    const script = document.createElement('script')
    script.src = 'https://cdn.staticfile.org/monaco-editor/0.30.1/min/vs/loader.min.js'
    script.onload = () => {
      require.config({ paths: { 'vs': 'https://cdn.staticfile.org/monaco-editor/0.30.1/min/vs' } })
      createMonaco()
    }
    document.head.appendChild(script)
  }
}

const createMonaco = () => {
  if (editor) return
  const container = document.getElementById('monaco-editor')
  if (!container) return

  editor = monaco.editor.create(container, {
    value: templates[language.value],
    language: language.value,
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 16,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    tabSize: 4
  })
}

const changeLang = (val) => {
  if (!editor) return
  monaco.editor.setModelLanguage(editor.getModel(), val)
  editor.setValue(templates[val])
}

// ---------------------- 工具函数 ----------------------
const parseTags = (str) => {
  try { return JSON.parse(str) } catch (e) { return [] }
}

const getDiffColor = (d) => {
  const map = { '简单': 'success', '困难': 'danger', '中等': 'warning' }
  return map[d] || 'info'
}

const goBack = () => router.push('/algo')

onMounted(() => {
  initEditor()
  loadDetail()
})
</script>

<style scoped>
.detail-container {
  display: flex;
  height: calc(100vh - 60px); /* 减去顶部导航栏高度 */
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  width: 400px;
  padding: 20px;
  background: #fff;
  border-right: 1px solid #e6e6e6;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.back-btn {
  cursor: pointer;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 14px;
}
.back-btn:hover { color: #409eff; }

.q-title { margin-top: 0; margin-bottom: 10px; color: #303133; }
.q-meta { margin-bottom: 20px; display: flex; gap: 8px; }
.q-desc { line-height: 1.6; color: #303133; font-size: 15px; }

.sample-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  margin-top: 20px;
  border: 1px solid #ebeef5;
}
.sample-label { font-weight: bold; color: #909399; font-size: 12px; margin-bottom: 5px; }
.sample-code {
  font-family: 'Consolas', monospace;
  background: #fff;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  color: #333;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  overflow: hidden;
}

.toolbar {
  height: 50px;
  background: #252526;
  border-bottom: 1px solid #3e3e42;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.btn-group { display: flex; gap: 10px; }

.editor-container {
  flex: 1; /* 占满剩余高度 */
  overflow: hidden;
}

/* 抽屉内容 */
.console-body {
  padding: 0 20px 20px;
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 15px;
  border-radius: 6px;
  margin-bottom: 15px;
  font-weight: 600;
}
.status-bar.info { background: #f4f4f5; color: #909399; }
.status-bar.success { background: #f0f9eb; color: #67c23a; }
.status-bar.error { background: #fef0f0; color: #f56c6c; }

.status-left { display: flex; align-items: center; gap: 8px; }

/* AI 按钮特效 */
.ai-btn {
  background: linear-gradient(135deg, #8b5cf6, #ec4899);
  border: none;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(139, 92, 246, 0.4);
  transition: transform 0.2s;
}
.ai-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 15px rgba(139, 92, 246, 0.6);
}

.output-box {
  background: #2b2b2b;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 15px;
  border: 1px solid #444;
}

.label { color: #858585; font-size: 12px; margin-bottom: 5px; }
.error-label { color: #f56c6c; }

.code-block {
  font-family: 'Consolas', monospace;
  color: #d4d4d4;
  white-space: pre-wrap;
  font-size: 14px;
  margin: 0;
}
.error-block { color: #fca5a5; }

/* AI 结果框 */
.ai-box {
  background: #fdfbf7;
  border: 1px solid #faecd8;
  padding: 15px;
  border-radius: 8px;
  margin-top: 10px;
}
.ai-header { display: flex; justify-content: space-between; margin-bottom: 10px; }
.ai-title { font-weight: bold; color: #e6a23c; }
.ai-content { color: #333; line-height: 1.6; font-size: 15px; }
</style>
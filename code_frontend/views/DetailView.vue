<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>编程挑战 - IDE</title>
  <link rel="stylesheet" href="https://unpkg.com/element-plus/dist/index.css" />
  <script src="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.30.1/min/vs/loader.min.js"></script>
  <style>
    body, html { margin: 0; padding: 0; height: 100%; overflow: hidden; font-family: 'Helvetica Neue', sans-serif; }
    #app { display: flex; height: 100vh; }

    /* 左侧：题目区 */
    .left-panel { width: 400px; min-width: 350px; padding: 20px; overflow-y: auto; background: #fff; border-right: 1px solid #ddd; box-sizing: border-box; }
    .back-btn { margin-bottom: 20px; cursor: pointer; color: #409eff; font-size: 14px; display: flex; align-items: center; }
    .q-title { margin-top: 0; font-size: 24px; color: #303133; }
    .q-meta { margin-bottom: 20px; display: flex; gap: 10px; }
    .q-desc { line-height: 1.8; color: #606266; font-size: 15px; }
    .sample-block { background: #f5f7fa; padding: 15px; border-radius: 4px; margin: 15px 0; border: 1px solid #e4e7ed; }
    .sample-label { font-size: 12px; color: #909399; margin-bottom: 5px; font-weight: bold; }
    .sample-code { font-family: 'Consolas', monospace; color: #333; white-space: pre-wrap; }

    /* 右侧：编辑区 */
    .right-panel { flex: 1; display: flex; flex-direction: column; background: #1e1e1e; }
    .toolbar { height: 50px; background: #252526; display: flex; align-items: center; justify-content: space-between; padding: 0 15px; border-bottom: 1px solid #333; }
    .editor-container { flex: 1; position: relative; }
    .console { height: 150px; background: #1e1e1e; border-top: 1px solid #333; color: #fff; padding: 10px 15px; overflow-y: auto; font-family: 'Consolas', monospace; display: none; }
    .console-title { color: #858585; font-size: 12px; margin-bottom: 5px; text-transform: uppercase; }

    /* 调整 Select 样式适应暗色主题 */
    .lang-select { width: 120px; }
  </style>
</head>
<body>
<div id="app">
  <div class="left-panel" v-loading="loading">
    <div class="back-btn" @click="goBack"><el-icon><Arrow-Left /></el-icon> 返回列表</div>

    <h1 class="q-title">{{ question.title }}</h1>
    <div class="q-meta">
      <el-tag :type="getDiffColor(question.difficulty)">{{ question.difficulty }}</el-tag>
      <el-tag type="info" v-for="tag in parseTags(question.tags)" :key="tag">{{ tag }}</el-tag>
    </div>

    <div class="q-desc">
      <p><strong>题目描述：</strong>{{ question.description }}</p>
      <p><strong>输入格式：</strong>{{ question.inputFormat }}</p>
      <p><strong>输出格式：</strong>{{ question.outputFormat }}</p>
    </div>

    <div class="sample-block" v-if="sampleData.input">
      <div class="sample-label">Sample Input:</div>
      <div class="sample-code">{{ sampleData.input }}</div>
      <div class="sample-label" style="margin-top: 10px;">Sample Output:</div>
      <div class="sample-code">{{ sampleData.output }}</div>
    </div>
  </div>

  <div class="right-panel">
    <div class="toolbar">
      <el-select v-model="language" class="lang-select" @change="changeLang" size="small">
        <el-option label="Java" value="java"></el-option>
        <el-option label="Python" value="python"></el-option>
      </el-select>
      <div>
        <el-button type="success" size="small" icon="Video-Play" @click="runCode" :loading="running">运行代码</el-button>
      </div>
    </div>

    <div id="monaco-editor" class="editor-container"></div>

    <div class="console" id="console-box">
      <div class="console-title">Execution Result</div>
      <pre style="margin: 0;" :style="{ color: isError ? '#f56c6c' : '#67c23a' }">{{ resultMsg }}</pre>
    </div>
  </div>
</div>

<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
<script src="https://unpkg.com/element-plus"></script>
<script src="https://unpkg.com/@element-plus/icons-vue"></script>
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

<script>
  const { createApp, ref, onMounted, computed } = Vue;

  const app = createApp({
    setup() {
      const question = ref({});
      const loading = ref(true);
      const running = ref(false);
      const language = ref('java');
      const resultMsg = ref('');
      const isError = ref(false);
      let editor = null;

      // 模板库
      const templates = {
        java: `import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 在此处编写你的代码\n        // 示例：int a = sc.nextInt();\n        \n    }\n}`,
        python: `# 在此处编写你的代码\n# 示例：\n# import sys\n# for line in sys.stdin:\n#     print(line)`
      };

      const sampleData = computed(() => {
        try { return JSON.parse(question.value.sampleCase || '{}'); } catch(e) { return {}; }
      });

      // 获取 ID
      const id = new URLSearchParams(window.location.search).get('id');

      // 加载详情
      const loadDetail = async () => {
        if (!id) return;
        try {
          const res = await axios.get(`http://localhost:8080/api/ai/detail/${id}`);
          if (res.data.code === 1) {
            question.value = res.data.data;
            // 第一次加载时，如果编辑器好了，就设置值
            if(editor && !editor.getValue()) {
              editor.setValue(templates.java);
            }
          }
        } catch (e) {
          ElementPlus.ElMessage.error('获取题目失败');
        } finally {
          loading.value = false;
        }
      };

      // 初始化 Monaco Editor
      const initEditor = () => {
        require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.30.1/min/vs' }});
        require(['vs/editor/editor.main'], function() {
          editor = monaco.editor.create(document.getElementById('monaco-editor'), {
            value: templates.java,
            language: 'java',
            theme: 'vs-dark',
            automaticLayout: true,
            fontSize: 14,
            minimap: { enabled: false }
          });
        });
      };

      // 切换语言
      const changeLang = (val) => {
        if(!editor) return;
        monaco.editor.setModelLanguage(editor.getModel(), val);
        editor.setValue(templates[val]);
      };

      // 运行代码 (调用后端)
      const runCode = async () => {
        running.value = true;
        document.getElementById('console-box').style.display = 'block';
        resultMsg.value = '正在连接判题机...';
        isError.value = false;

        try {
          const res = await axios.post('http://localhost:8080/api/ai/run', {
            language: language.value,
            code: editor.getValue(),
            input: sampleData.value.input || "",
            output: sampleData.value.output || ""
          });

          // 后端返回 Result 对象
          if (res.data.code === 1) {
            // 后端 JudgeUtils 如果返回 null 表示成功，这里 success msg 是我们自己写的
            // 我们可以约定：如果内容包含 "❌" 或 "Error" 则标红
            resultMsg.value = res.data.data || res.data.msg;
            if(resultMsg.value.includes('❌') || resultMsg.value.includes('Error')) {
              isError.value = true;
            }
          } else {
            resultMsg.value = res.data.msg;
            isError.value = true;
          }
        } catch (err) {
          resultMsg.value = "系统错误：无法连接到后端";
          isError.value = true;
        } finally {
          running.value = false;
        }
      };

      const parseTags = (str) => {
        try { return JSON.parse(str); } catch(e) { return str ? str.split(/[,，]/) : []; }
      };
      const getDiffColor = (d) => ({'简单':'success','困难':'danger','中等':'warning'}[d] || 'info');
      const goBack = () => window.location.href = 'index.html';

      onMounted(() => {
        initEditor();
        loadDetail();
      });

      return { question, loading, running, language, resultMsg, isError, sampleData, runCode, changeLang, parseTags, getDiffColor, goBack };
    }
  });

  for (const [key, component] of Object.entries(ElementPlusIconsVue)) { app.component(key, component) }
  app.use(ElementPlus);
  app.mount('#app');
</script>
</body>
</html>
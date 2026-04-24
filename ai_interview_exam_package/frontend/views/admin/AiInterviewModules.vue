<template>
  <div class="module-manage">
    <div class="page-top">
      <div>
        <h1>面试模块管理</h1>
        <p class="subtitle">配置 AI 面试可选的技术模块，学生将从中选择面试方向。</p>
      </div>
      <div class="top-actions">
        <el-switch v-model="formalOpen" active-text="正式面试已开放" inactive-text="正式面试已关闭" @change="toggleFormal" />
        <el-button type="primary" @click="openDialog()">新增模块</el-button>
      </div>
    </div>

    <el-table :data="modules" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="moduleName" label="模块名称" min-width="140" />
      <el-table-column prop="moduleCode" label="编码" width="120" />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模块' : '新增模块'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="模块名称"><el-input v-model="form.moduleName" /></el-form-item>
        <el-form-item label="模块编码"><el-input v-model="form.moduleCode" /></el-form-item>
        <el-form-item label="模块说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="提示词模板"><el-input v-model="form.promptTemplate" type="textarea" :rows="4" placeholder="该模块的 AI 面试提示词" /></el-form-item>
        <el-form-item label="评分规则"><el-input v-model="form.scoreRule" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminModules, createModule, updateModule, deleteModule, toggleFormalInterview, getFormalInterviewStatus } from '@/api/aiInterview'

const loading = ref(false)
const modules = ref([])
const formalOpen = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editId = ref(null)
const form = ref({ moduleName: '', moduleCode: '', description: '', promptTemplate: '', scoreRule: '', status: true })

onMounted(async () => {
  await loadModules()
  try { const r = await getFormalInterviewStatus(); formalOpen.value = r?.data?.open ?? false } catch {}
})

async function loadModules() {
  loading.value = true
  try { const r = await getAdminModules(); modules.value = r?.data || [] } catch {}
  loading.value = false
}

function openDialog(row) {
  if (row) {
    isEdit.value = true; editId.value = row.id
    form.value = { ...row }
  } else {
    isEdit.value = false; editId.value = null
    form.value = { moduleName: '', moduleCode: '', description: '', promptTemplate: '', scoreRule: '', status: true }
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) { await updateModule(editId.value, form.value) }
    else { await createModule(form.value) }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    await loadModules()
  } catch { ElMessage.error('操作失败') }
  saving.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模块「${row.moduleName}」？`, '提示', { type: 'warning' })
    await deleteModule(row.id)
    ElMessage.success('删除成功')
    await loadModules()
  } catch {}
}

async function toggleFormal(val) {
  try { await toggleFormalInterview({ open: val }) } catch { formalOpen.value = !val }
}
</script>

<style scoped>
.module-manage { padding: 0 4px; }
.page-top { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; }
.page-top h1 { font-size: 20px; font-weight: 700; color: #0f172a; margin: 0 0 4px; }
.subtitle { font-size: 13px; color: #64748b; margin: 0; }
.top-actions { display: flex; align-items: center; gap: 16px; }
</style>

<template>
  <div class="m-page">
    <section class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索实验室名称" @clear="reload">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button plain :loading="loading" @click="reload">刷新</el-button>
    </section>

    <section v-loading="loading" class="list">
      <button v-for="item in labs" :key="item.id" class="lab-card" type="button" @click="openDetail(item)">
        <div class="lab-head">
          <strong class="lab-name">{{ item.labName || '未命名实验室' }}</strong>
          <span class="lab-code">{{ item.labCode || `#${item.id}` }}</span>
        </div>
        <p v-if="item.labDesc" class="lab-desc">{{ item.labDesc }}</p>
        <div class="lab-meta">
          <span v-if="item.collegeName" class="meta-chip">{{ item.collegeName }}</span>
          <span v-if="item.location" class="meta-chip">{{ item.location }}</span>
          <span class="meta-chip">成员 {{ item.currentNum ?? 0 }} / {{ item.recruitNum ?? '-' }}</span>
        </div>
      </button>

      <el-empty v-if="!loading && labs.length === 0" description="暂无实验室" :image-size="80" />
    </section>

    <el-drawer v-model="detailVisible" :with-header="false" size="92%">
      <div class="drawer">
        <div class="drawer-head">
          <strong class="drawer-title">{{ active?.labName || '实验室详情' }}</strong>
          <button class="drawer-close" type="button" @click="detailVisible = false">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <p class="drawer-meta">{{ active?.labCode || `#${active?.id ?? ''}` }}</p>
        <div class="drawer-body">
          <div v-if="active?.labDesc" class="section">
            <label>实验室简介</label>
            <p>{{ active.labDesc }}</p>
          </div>
          <div class="section">
            <label>所属学院</label>
            <p>{{ active?.collegeName || '-' }}</p>
          </div>
          <div class="section">
            <label>所在地点</label>
            <p>{{ active?.location || '-' }}</p>
          </div>
          <div class="section">
            <label>指导教师</label>
            <p>{{ active?.teacherName || '待维护' }}</p>
          </div>
          <div class="section">
            <label>成员规模</label>
            <p>{{ active?.currentNum ?? 0 }} / {{ active?.recruitNum ?? '-' }}</p>
          </div>
          <div v-if="active?.requireSkill" class="section">
            <label>要求技能</label>
            <p>{{ active.requireSkill }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { Close, Search } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'
import { getLabPage } from '@/api/lab'

const keyword = ref('')
const loading = ref(false)
const labs = ref([])

const detailVisible = ref(false)
const active = ref(null)

const openDetail = (lab) => {
  active.value = lab
  detailVisible.value = true
}

const reload = async () => {
  loading.value = true
  try {
    const res = await getLabPage({
      pageNum: 1,
      pageSize: 50,
      labName: keyword.value?.trim() || undefined
    })
    labs.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <el-card class="growth-nav" shadow="never">
    <div class="nav-copy">
      <div>
        <p class="eyebrow">GROWTH CENTER</p>
        <h1>{{ title }}</h1>
        <p class="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="nav-links">
      <button
        v-for="item in items"
        :key="item.path"
        class="nav-link"
        :class="{ active: isActive(item.path), locked: isLocked(item) }"
        :disabled="isLocked(item)"
        @click="handleNavigate(item)"
      >
        <strong>{{ item.label }}</strong>
        <span>{{ item.caption }}</span>
      </button>
    </div>
  </el-card>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  assessmentReady: {
    type: Boolean,
    default: true
  }
})

const route = useRoute()
const router = useRouter()

const items = [
  {
    path: '/student/guide',
    label: '成长测评',
    caption: '先完成 20 题测评，再看路径推荐'
  },
  {
    path: '/student/guide/jobs',
    label: '路径广场',
    caption: '浏览全部成长方向和推荐路径'
  },
  {
    path: '/student/guide/match',
    label: '能力匹配',
    caption: '查看你的动态推荐度和短板'
  },
  {
    path: '/student/guide/learn',
    label: '学习路线',
    caption: '按阶段推进课程、项目和表达'
  },
  {
    path: '/student/guide/practice',
    label: '练习中心',
    caption: '进入题库、判题和 AI 出题'
  },
  {
    path: '/student/guide/interview',
    label: 'AI 面试',
    caption: '语音模拟真实追问与复盘'
  }
]

const isActive = (path) => route.path === path
const isLocked = (item) => item.path !== '/student/guide' && !props.assessmentReady
const handleNavigate = (item) => {
  if (isLocked(item)) {
    return
  }
  router.push(item.path)
}
</script>

<style scoped>
.growth-nav {
  border: 1px solid rgba(14, 165, 233, 0.14);
  background:
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.2), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(240, 249, 255, 0.96));
}

.nav-copy {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: #0284c7;
}

.nav-copy h1 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.description {
  margin: 12px 0 0;
  max-width: 760px;
  line-height: 1.8;
  color: #475569;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.nav-links {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.nav-link {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  padding: 16px 14px;
  text-align: left;
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.nav-link strong,
.nav-link span {
  display: block;
}

.nav-link strong {
  font-size: 15px;
}

.nav-link span {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.nav-link:hover,
.nav-link.active {
  transform: translateY(-2px);
  border-color: rgba(14, 165, 233, 0.34);
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.1);
}

.nav-link.active {
  background: linear-gradient(180deg, #eff6ff, #ecfeff);
}

.nav-link:disabled,
.nav-link.locked {
  cursor: not-allowed;
  opacity: 0.58;
  transform: none;
  box-shadow: none;
}

@media (max-width: 1280px) {
  .nav-links {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .nav-copy {
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-copy h1 {
    font-size: 24px;
  }

  .nav-links {
    grid-template-columns: 1fr;
  }
}
</style>

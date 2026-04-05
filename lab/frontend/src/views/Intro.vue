<template>
  <div class="intro-page">
    <section class="hero">
      <div class="hero-copy">
        <BrandLogo
          size="lg"
          title="安徽信息工程学院"
          subtitle="实验室管理平台 · 全校版"
        />
        <h1>统一服务学校、学院、实验室、老师与学生的协同平台</h1>
        <p>
          平台按“学校 - 学院 - 实验室 - 成员”组织业务，面向全校实验室治理场景，
          支持学院主数据、实验室创建、成员申请、公告发布与统计分析。
        </p>
        <div class="hero-actions">
          <router-link class="primary-link" to="/login">进入平台</router-link>
          <router-link class="ghost-link" to="/register">学生注册</router-link>
        </div>
      </div>

      <div class="hero-card">
        <h2>平台定位</h2>
        <ul>
          <li>学校管理员统一维护学院主数据与关键审批</li>
          <li>学院管理员负责本学院实验室与申请事项协同</li>
          <li>学生可浏览实验室、提交入组申请、查看公告与记录</li>
        </ul>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <p class="eyebrow">学院范围</p>
        <h2>默认学院主数据按安徽信息工程学院全校版初始化</h2>
      </div>
      <div class="college-grid">
        <article v-for="college in colleges" :key="college.code" class="college-card">
          <span class="code">{{ college.code }}</span>
          <h3>{{ college.name }}</h3>
          <p>{{ college.description }}</p>
        </article>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <p class="eyebrow">角色体系</p>
        <h2>按照需求文档收敛为校院两级治理 + 实验室业务协同</h2>
      </div>
      <div class="role-grid">
        <article v-for="role in roles" :key="role.title" class="role-card">
          <h3>{{ role.title }}</h3>
          <p class="scope">{{ role.scope }}</p>
          <p>{{ role.description }}</p>
        </article>
      </div>
    </section>

    <section class="section flow-section">
      <div class="section-header">
        <p class="eyebrow">业务流程</p>
        <h2>当前版本已接入的主流程与后续迭代方向</h2>
      </div>
      <div class="flow-grid">
        <article class="flow-card">
          <h3>已具备基础能力</h3>
          <ul>
            <li>学院主数据管理</li>
            <li>实验室创建申请的学院 / 学校双层审核字段</li>
            <li>实验室、成员、公告、统计分析等基础模块</li>
          </ul>
        </article>
        <article class="flow-card">
          <h3>按需求文档继续扩展</h3>
          <ul>
            <li>老师注册审批链</li>
            <li>指导老师申请与变更流程</li>
            <li>审批日志、状态机和历史追踪表</li>
          </ul>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import BrandLogo from '@/components/BrandLogo.vue'

const colleges = [
  {
    code: 'CS',
    name: '计算机与软件工程学院',
    description: '面向软件工程、计算机科学与技术、网络工程等专业方向。'
  },
  {
    code: 'AI',
    name: '大数据与人工智能学院',
    description: '面向人工智能、数据科学与大数据技术等方向的实验室建设。'
  },
  {
    code: 'EEE',
    name: '电气与电子工程学院',
    description: '覆盖电子信息工程、通信工程、电气工程及其自动化等领域。'
  },
  {
    code: 'ME',
    name: '机械工程学院',
    description: '支持智能制造、机械设计制造及其自动化等实验室场景。'
  },
  {
    code: 'MGT',
    name: '管理工程学院',
    description: '面向市场营销、财务管理、供应链管理等应用方向。'
  },
  {
    code: 'ART',
    name: '艺术设计学院',
    description: '支持视觉传达、环境设计、产品设计等实验室与工作室。'
  }
]

const roles = [
  {
    title: '学校管理员',
    scope: '全校范围',
    description: '负责学院主数据、关键审批、跨学院统筹和平台级配置。'
  },
  {
    title: '学院管理员',
    scope: '单个学院',
    description: '负责本学院实验室创建审批、成员事项协同和日常管理。'
  },
  {
    title: '实验室负责人',
    scope: '单个实验室',
    description: '负责实验室成员管理、空间资料维护、公告发布和招新协同。'
  },
  {
    title: '学生',
    scope: '所属学院 / 实验室',
    description: '浏览实验室信息、提交入组申请、查看公告和个人记录。'
  }
]
</script>

<style scoped>
.intro-page {
  min-height: 100vh;
  padding: 40px 20px 60px;
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.12), transparent 32%),
    radial-gradient(circle at bottom right, rgba(14, 165, 233, 0.12), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.hero,
.section {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 24px;
  align-items: stretch;
  margin-bottom: 36px;
}

.hero-copy,
.hero-card,
.college-card,
.role-card,
.flow-card {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-copy {
  padding: 32px;
}

.hero-copy h1 {
  margin: 24px 0 14px;
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.08;
  color: #0f172a;
}

.hero-copy p {
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
  font-size: 16px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 26px;
  flex-wrap: wrap;
}

.primary-link,
.ghost-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132px;
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
}

.primary-link {
  background: linear-gradient(135deg, #1d4ed8, #0ea5e9);
  color: #fff;
}

.ghost-link {
  border: 1px solid #cbd5e1;
  color: #0f172a;
  background: #fff;
}

.hero-card {
  padding: 28px;
}

.hero-card h2,
.section-header h2 {
  margin: 0;
  color: #0f172a;
}

.hero-card ul,
.flow-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.section {
  margin-bottom: 28px;
}

.section-header {
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.college-grid,
.role-grid,
.flow-grid {
  display: grid;
  gap: 18px;
}

.college-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.role-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.flow-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.college-card,
.role-card,
.flow-card {
  padding: 22px;
}

.college-card .code {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #075985;
  font-size: 12px;
  font-weight: 800;
}

.college-card h3,
.role-card h3,
.flow-card h3 {
  margin: 14px 0 10px;
  color: #0f172a;
}

.college-card p,
.role-card p,
.flow-card p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.scope {
  margin-bottom: 8px !important;
  color: #0f766e !important;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-copy,
  .hero-card {
    padding: 24px;
  }
}
</style>

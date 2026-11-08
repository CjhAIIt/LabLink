# 多端适配实现说明

本文档说明 LabLink 前端的多端适配实现：同一套代码在桌面浏览器与手机浏览器上自动切换两套界面体系，并预留了原生打包（Capacitor / Electron）的接入点。

界面规范部分见 [FRONTEND-UI-GUIDE.md](./FRONTEND-UI-GUIDE.md)。

## 1. 设计目标

- 一份代码同时服务桌面端与移动端，不需要维护两套仓库
- 桌面端保留侧边栏 + 顶栏 + 宽表格的既有形态，移动端切换为顶栏 + 底部标签栏 + 卡片列表
- 两种形态复用同一层 API 封装与状态管理，不重复实现业务逻辑
- 运行时按视口宽度自动分流，用户不需要手动选择版本

## 2. 运行时分流

分流入口是 `src/utils/portal.js` 中的 `isMobilePortal()`：

```js
export function isMobilePortal() {
  if (typeof window === 'undefined') {
    return false
  }

  try {
    const isNative = Boolean(window?.Capacitor?.isNativePlatform?.())
    if (isNative) {
      return true
    }
  } catch (error) {
    // ignore
  }

  return window.matchMedia?.('(max-width: 768px)')?.matches ?? window.innerWidth <= 768
}
```

判定顺序：

1. 服务端渲染或无 `window` 环境返回 `false`，走桌面端
2. 检测到 Capacitor 原生运行时，直接返回 `true`（原生 App 一律按移动端渲染，不看视口宽度）
3. 其余情况按视口宽度判断，`768px` 及以下视为移动端

Capacitor 的检测通过可选链访问 `window.Capacitor`，因此**未安装 Capacitor 时该分支安全跳过**，Web 端构建不依赖原生依赖。

`resolvePortalHome()` 也复用了该函数，登录后的落地页会直接指向移动端路由：

```js
if (portalRole === 'teacher') {
  return isMobilePortal() ? '/m/teacher/dashboard' : '/teacher/dashboard'
}
if (portalRole === 'admin') {
  return isMobilePortal() ? '/m/admin/dashboard' : '/admin/dashboard'
}
return isMobilePortal() ? '/m/student/dashboard' : '/student/dashboard'
```

管理员端已有移动端专属页面 `/m/admin/*`，窄屏下不再退回桌面端布局。

## 3. 路由分流

`src/router/index.js` 中新增三棵移动端路由树：

| 路径 | 布局 | 子路由 |
| --- | --- | --- |
| `/m/student` | `MobileStudentLayout` | `dashboard`、`labs`、`labs/:id`、`applications`、`notices`、`profile` |
| `/m/teacher` | `MobileTeacherLayout` | `dashboard`、`create-applies`、`notices`、`profile` |
| `/m/admin` | `MobileAdminLayout` | `dashboard`、`approvals`、`labs`、`notices`、`profile` |

全局前置守卫在鉴权检查之前做路径互斥重定向，保证同一页面在两种形态间始终只有一个可达地址：

```js
const CATCH_ALL_ROUTE_PATH = '/:pathMatch(.*)*'

const hasRoute = (path) => {
  const resolved = router.resolve({ path })
  return resolved.matched.length > 0 && !resolved.matched.some((record) => record.path === CATCH_ALL_ROUTE_PATH)
}

const resolveCounterpartPath = (path, fromPrefix, toPrefix) => {
  if (!path.startsWith(fromPrefix)) {
    return null
  }
  const candidate = path.replace(fromPrefix, toPrefix)
  return hasRoute(candidate) ? candidate : null
}

// 守卫内部：移动端优先使用移动端页面，桌面端反之
const studentTarget = resolveCounterpartPath(to.path, '/student', '/m/student')
if (studentTarget) {
  next({ path: studentTarget, query: to.query, hash: to.hash })
  return
}
const adminTarget = resolveCounterpartPath(to.path, '/admin', '/m/admin')
if (adminTarget) {
  next({ path: adminTarget, query: to.query, hash: to.hash })
  return
}
```

要点：

- 重定向保留 `query` 与 `hash`，带参数的深链接不会丢参
- 移动端路径前缀是 `/m/...`，不会与 `/student`、`/teacher`、`/admin` 前缀互相误伤
- **只有存在对应路由时才改写路径**。没有移动端实现的页面（例如 `/student/attendance`、`/student/space`、`/teacher/workspace`、`/admin/lab-admin`）不做改写，直接用桌面端页面渲染，避免命中兜底路由后被踢回登录页、丢失深链接
- 守卫在每次导航时重新计算，浏览器窗口缩放跨越 768px 后，下一次导航即切换形态
- 重定向发生在鉴权之前，未登录用户访问 `/student/dashboard` 时先切到 `/m/student/dashboard`，再由鉴权逻辑送回登录页

## 4. 移动端布局外壳

`MobileStudentLayout.vue`、`MobileTeacherLayout.vue` 与 `MobileAdminLayout.vue` 结构一致：

```
m-shell
├─ m-topbar        顶栏：返回按钮（子页显示）、页面标题、用户头像下拉
├─ m-content       内容区，底部留出标签栏高度 + 安全区
└─ m-tabbar        固定底部标签栏
```

### 4.1 标签栏配置

学生端 5 个标签：

```js
const tabItems = [
  { path: '/m/student/dashboard', label: '首页', icon: HomeFilled },
  { path: '/m/student/labs', label: '实验室', icon: OfficeBuilding },
  { path: '/m/student/applications', label: '申请', icon: Document },
  { path: '/m/student/notices', label: '公告', icon: Bell },
  { path: '/m/student/profile', label: '我的', icon: User }
]
```

教师端 3 个标签：首页、公告、我的。

管理端 5 个标签：首页、审批、实验室、公告、我的。

标签栏使用 `grid-template-columns: repeat(v-bind('tabItems.length'), minmax(0, 1fr))`，
列数跟随标签数量自动变化，新增标签无需改样式。

### 4.2 安全区适配

顶栏与标签栏分别预留刘海屏和底部手势区：

```css
.m-topbar {
  padding: calc(12px + env(safe-area-inset-top)) 14px 12px 14px;
}

.m-content {
  padding: 14px 14px calc(84px + env(safe-area-inset-bottom)) 14px;
}

.m-tabbar {
  padding: 10px 10px calc(10px + env(safe-area-inset-bottom)) 10px;
}
```

### 4.3 桌面端布局的窄屏处理

`AdminLayout` / `StudentLayout` / `TeacherLayout` 在 `960px` 以下把侧边栏改为抽屉：

- 侧边栏 `position: fixed`，`height: 100dvh`，带阴影浮于内容之上
- 折叠时 `transform: translateX(-100%)` 并设置 `pointer-events: none`，避免隐藏后仍拦截点击
- 展开时渲染 `.sidebar-mask` 遮罩层，点击遮罩或切换路由后自动收起
- 顶栏标题超长时截断显示，避免挤压用户信息区

```js
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 960 : false)
const isCollapse = ref(isMobile.value)
const sidebarVisible = computed(() => isMobile.value && !isCollapse.value)
```

## 5. 移动端页面

移动端页面统一放在 `src/views/mobile/`：

| 文件 | 路径 | 说明 |
| --- | --- | --- |
| `mobile/student/Dashboard.vue` | `/m/student/dashboard` | 学生首页：问候横幅、实验室/学院/申请/我的实验室四项指标、功能入口、最新公告 |
| `mobile/student/Labs.vue` | `/m/student/labs` | 实验室列表，卡片式展示，支持关键字搜索与分页加载 |
| `mobile/student/LabDetail.vue` | `/m/student/labs/:id` | 实验室详情与加入申请，已加入时按钮置灰 |
| `mobile/student/Applications.vue` | `/m/student/applications` | 我的申请记录与审核进度 |
| `mobile/teacher/Dashboard.vue` | `/m/teacher/dashboard` | 教师首页：创建申请数量、所属学院、申请入口 |
| `mobile/teacher/CreateApplies.vue` | `/m/teacher/create-applies` | 发起实验室创建申请、查看提交记录 |
| `mobile/admin/Dashboard.vue` | `/m/admin/dashboard` | 管理首页：问候横幅、实验室/成员/待办/招新四项指标、待办审批清单 |
| `mobile/admin/Approvals.vue` | `/m/admin/approvals` | 审批中心：三类审批合并展示，支持按类型筛选与审批动作 |
| `mobile/admin/Labs.vue` | `/m/admin/labs` | 实验室列表：名称、编号、地点、成员数，支持关键字搜索 |
| `mobile/admin/Notices.vue` | `/m/admin/notices` | 公告管理：公告列表与发布公告抽屉 |
| `mobile/Notices.vue` | `/m/student/notices`、`/m/teacher/notices` | 公告中心，学生端与教师端共用 |
| `mobile/Profile.vue` | `/m/student/profile`、`/m/teacher/profile`、`/m/admin/profile` | 个人信息与退出登录，三端共用 |

### 5.1 移动端页面写法约定

- 页面根元素使用 `<div class="m-page">`，内部使用 `hero`（横幅）、`stat-card`（指标卡）、`quick-card`（功能入口）、`card` / `list`（列表卡片）、`panel`（分组面板）等移动端语义类
- **语义类不能叠用**：`mobile.css` 里 `.hero` 是首页的深色横幅，`.card` 是普通白卡，
  写在同一个元素上会让白卡覆盖横幅的底色、横幅的浅色文字压白底而看不见。
  需要变体就另起类名（如个人信息页的 `.profile-hero`）。
- 不使用 `el-table`，列表一律用卡片
- 表单使用 `el-form` 的纵向布局，`label-width="0"`
- 移动端页面只依赖 `src/api/` 下已有的接口封装，不直接书写 axios 调用

### 5.2 与桌面端页面的对应关系

移动端页面是桌面端页面的移动形态子集，不是一一映射：

| 桌面端 | 移动端 | 差异 |
| --- | --- | --- |
| `student/Dashboard.vue` | `mobile/student/Dashboard.vue` | 指标由 4 列变 2 列，公告改为纵向卡片 |
| `student/Labs.vue` | `mobile/student/Labs.vue` | 表格改为卡片，筛选项折叠 |
| `student/LabDetail.vue` | `mobile/student/LabDetail.vue` | 信息分组改为折叠区块 |
| `student/Applications.vue` | `mobile/student/Applications.vue` | 表格改为申请卡片 |
| `teacher/Dashboard.vue` | `mobile/teacher/Dashboard.vue` | 仅保留核心指标与申请入口 |
| `teacher/LabCreateApplies.vue` | `mobile/teacher/CreateApplies.vue` | 表单精简，字段分步填写 |
| `student/Notices.vue` | `mobile/Notices.vue` | 公告由表格改为列表 |
| `student/Profile.vue` | `mobile/Profile.vue` | 只读信息展示，编辑能力下沉到桌面端 |

未提供移动端实现的页面（考勤、资料空间、我的实验室、实验室工作台等）不做路径改写，在移动端直接使用桌面端页面渲染。这些页面自身已通过全局样式与抽屉式侧边栏适配窄屏显示。

## 6. 新增移动端页面的步骤

1. 在 `src/views/mobile/` 下按端归类新建组件，参照同类页面写法
2. 在 `src/router/index.js` 中导入组件，并在 `/m/student`、`/m/teacher` 或 `/m/admin` 的 `children` 中注册路由，务必填写 `meta.title`
3. 如需新增底部标签，在对应布局的 `tabItems` 中增加条目，标签栏列数会自动跟随
4. 若桌面端也有该功能，在 `src/utils/portal.js` 的 `resolvePortalHome()` 中无需改动（落地页只按角色区分）
5. 执行 `npm run build` 与 `npx eslint . --ext .vue,.js` 验证

## 7. 原生打包的接入点

前端已经为原生打包预留了检测能力，尚未包含打包工具链。

### 7.1 已完成

- `isMobilePortal()` 检测 `window.Capacitor.isNativePlatform()`，原生运行时直接走移动端界面
- `src/utils/request.js` 与 `src/utils/file.js` 已支持通过 `VITE_API_BASE_URL` 指定后端绝对地址，原生环境无法使用相对路径的问题由此解决
- 移动端界面已适配安全区、触摸点击、点击高亮

### 7.2 待接入

引入 Capacitor 时需要补齐：

```bash
cd frontend
npm install @capacitor/core @capacitor/android
npm install -D @capacitor/cli
npx cap init
```

`capacitor.config.json` 建议配置：

```json
{
  "appId": "com.aiit.labmanagement",
  "appName": "LabLink",
  "webDir": "dist",
  "server": {
    "cleartext": true
  }
}
```

注意：`npx cap init` 会写入 `capacitor.config.json`，同时需要在 `src/utils/request.js` 中把原生环境的兜底 baseURL 指向实际后端地址。当前仓库刻意没有内置具体 IP，避免把某台服务器的地址写死在源码里，请通过环境变量配置：

```env
# .env.production
VITE_API_BASE_URL=https://api.example.edu.cn
```

桌面端打包（Electron / Tauri）的方案对比见 [Desktop_Android_Development_Guide.md](./Desktop_Android_Development_Guide.md)。

## 8. 已知边界

- `isMobilePortal()` 依据视口宽度判定，桌面浏览器窗口收窄到 768px 以下时，下次导航会切换到移动端界面，属于预期行为
- 移动端与桌面端是两套路由树，功能覆盖以移动端页面清单为准，其余页面复用桌面端实现
- 管理端已覆盖首页、审批、实验室、公告、我的五项；其余管理功能（学院管理、综合检索、考勤、统计分析等）在窄屏下仍使用桌面布局的抽屉形态

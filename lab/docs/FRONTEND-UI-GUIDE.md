# 前端界面规范

本文档说明 `lab/frontend` 的界面基线、响应式断点、页面骨架与常用样式类，供新增页面和重构既有页面时对照执行。

## 1. 视觉基线

### 1.1 品牌

平台统一品牌标识为 **LabLink**，对应中文副标题“高校实验室管理平台”。

- 品牌标识组件：`src/components/BrandLogo.vue`
- 标识色块文字固定为 `Link`，主色为品牌蓝 `#2563eb`（深色态 `#1d4ed8`）
- 品牌在侧边栏、登录页、注册页、平台介绍页中的标题必须来自 `BrandLogo` 的 `title` / `subtitle` 属性，不要在页面里重复硬编码全称

### 1.2 颜色

| 用途 | 色值 |
| --- | --- |
| 主色 | `#2563eb` / `#1d4ed8` |
| 学生端主色 | `#2563eb` |
| 教师端主色 | `#7c3aed` |
| 页面底色 | `#f8fafc`，渐变至 `#f1f5f9` |
| 卡片底色 | `#ffffff` |
| 主文字 | `#0f172a` |
| 次文字 | `#64748b` |
| 分隔线 | `#e2e8f0` |

页面级渐变统一使用 `linear-gradient`，圆角统一 20px 以上（移动端 20px，桌面端 16px 居多），阴影使用低透明度蓝灰色如 `rgba(148, 163, 184, 0.18)`。

### 1.3 字体与滚动条

- 基础字号 14px，`body` 已开启 `-webkit-font-smoothing: antialiased`
- 滚动条统一 8px，轨道 `#f1f1f1`，滑块 `#c1c1c1`
- 移动端已加 `-webkit-tap-highlight-color: transparent`，避免点击出现系统高亮方块

## 2. 响应式断点

平台使用三个断点，含义不同，不要混用：

| 断点 | 含义 | 触发行为 |
| --- | --- | --- |
| `960px` | 侧边栏抽屉化 | `AdminLayout` / `StudentLayout` / `TeacherLayout` 的侧边栏变为遮罩式抽屉，默认折叠 |
| `900px` | 内容区收敛 | `.content-grid.two-column` 变为单列，`.page-hero` 改为纵向排布 |
| `768px` | 移动端形态 | `isMobilePortal()` 返回 `true`，路由分流到 `/m/student`、`/m/teacher`；同时全局样式收紧字号、间距与表格 |

`960px` 只影响桌面布局的侧边栏表现，`768px` 才决定是否切换到移动端页面体系。768px 到 960px 之间仍然使用桌面页面，只是侧边栏收起为抽屉。

## 3. 全局样式类

全局样式集中在 `src/assets/css/global.css`，页面不要重复定义同名的通用类。

### 3.1 工具类

`text-center`、`text-right`、`text-left`、`flex`、`flex-center`、`flex-between`、`flex-column`、`full-height`、`full-width`、`mt-10`、`mt-20`、`mb-10`、`mb-20`、`ml-10`、`mr-10`、`p-10`、`p-20`。

### 3.2 页面骨架类

| 类名 | 用途 |
| --- | --- |
| `.page-shell` | 页面根容器，纵向 flex + gap |
| `.page-hero` | 顶部横幅区，深色渐变背景，大圆角 |
| `.page-hero h1` / `.hero-subtitle` / `.eyebrow` | 横幅内的标题、副标题、眉标 |
| `.metric-grid` / `.metric-card` | 指标卡网格与单卡。桌面按容器宽度自适应（`auto-fit` + `minmax(180px, 1fr)`），768px 以下固定为 1 列 |
| `.metric-label` / `.metric-value` / `.metric-tip` | 指标卡的标签、数值、补充说明 |
| `.content-grid.two-column` | 双栏内容区 |
| `.panel-card` / `.toolbar-card` | 内容卡片、工具条卡片 |
| `.toolbar-main` / `.toolbar-actions` / `.toolbar-form` | 工具条内部的标题区、操作区、筛选表单区 |
| `.panel-header` | 卡片头部，标题 + 操作按钮 |
| `.empty-panel` / `.rank-list` / `.rank-item` | 空状态与排行榜 |

### 3.3 表格与表单容器

`.card-container`、`.el-table`（统一表格头底色与字号）、`.pagination-container`、`.form-container`、`.search-container`、`.button-group`、`.status-tag` 及 `.status-pending` / `.status-approved` / `.status-rejected`。

## 4. 窄屏下的表格处理

Element Plus 的 `el-table` 在窄屏下横向溢出，可读性差。平台采用两种处理方式，按场景选择：

1. **卡片列表替代**：在页面内维护 `isMobile` 布尔值（由 `window.innerWidth <= 768` 驱动，监听 `resize` 更新），在 `max-width: 768px` 时用 `v-if="isMobile"` 渲染卡片列表、`v-else` 渲染 `el-table`。
   参考实现：`src/views/student/Applications.vue`。
2. **横向滚动兜底**：表格保留，但用 `min-width` 固定列宽，让容器横向滚动。
   全局兜底规则在 `global.css` 中：

   ```css
   .panel-card :where(.el-table__inner-wrapper) {
     min-width: 680px;
   }
   ```

对弹窗统一使用相对宽度，避免固定像素宽度在窄屏溢出：

```html
<el-dialog width="min(720px, 92vw)" />
```

## 5. 布局与页面组织

```
src/
├─ layouts/          布局外壳
│  ├─ AdminLayout.vue          管理端（侧边栏 + 顶栏）
│  ├─ StudentLayout.vue        学生端（侧边栏 + 顶栏）
│  ├─ TeacherLayout.vue        教师端（侧边栏 + 顶栏）
│  ├─ MobileStudentLayout.vue  移动端学生端（顶栏 + 底部标签栏）
│  └─ MobileTeacherLayout.vue  移动端教师端（顶栏 + 底部标签栏）
├─ views/            页面
│  ├─ admin/  student/  teacher/   桌面端页面
│  └─ mobile/                        移动端页面
│     ├─ Notices.vue  Profile.vue
│     ├─ student/  Applications.vue Dashboard.vue LabDetail.vue Labs.vue
│     └─ teacher/  CreateApplies.vue Dashboard.vue
├─ components/       跨页面复用组件
├─ api/              按业务域拆分的接口封装
└─ utils/            请求封装、鉴权、门户角色、文件地址处理
```

桌面端三套布局共用同一套结构：左侧 `aside.sidebar` + 右侧 `div.main-shell`（内含 `header.topbar` 与 `div.content`）。新增角色端时复制 `StudentLayout.vue` 并替换菜单项，不要另起结构。

## 6. 交互约定

- 所有 `button` 统一 `cursor: pointer`，禁用态 `cursor: not-allowed`
- 可点击元素必须提供 hover 反馈与 `:focus-visible` 轮廓，不要移除浏览器默认焦点环
- 移动端按钮最小点击区域不小于 44px，主要操作按钮在窄屏下占满整行
- 顶部标题在窄屏下必须截断而非换行，使用 `white-space: nowrap; overflow: hidden; text-overflow: ellipsis`
- 移动端底部标签栏必须预留 `env(safe-area-inset-bottom)`，内容区预留等高的底部内边距，避免被标签栏遮挡

## 7. 数据刷新

公开页面（`/intro`、`/lab-info/:id`）在窗口重新获得焦点或页面重新可见时刷新数据，保证管理员在后台改动后前台立即可见：

```js
const handleWindowFocus = () => refresh()
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') refresh()
}

onMounted(() => {
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
```

事件监听必须在 `onBeforeUnmount` 中解绑，避免重复挂载导致请求风暴。

## 8. 构建约定

### 8.1 路由按需加载

`src/router/index.js` 中所有页面组件都用动态 `import()` 声明：

```js
const StudentLabs = () => import('@/views/student/Labs.vue')
```

新增路由时同样使用动态导入，不要改成静态 `import`。这让首屏只下载实际访问的模块，主入口包从 280 kB 降到 19 kB 左右。

### 8.2 Element Plus 按需引入

`vite.config.mjs` 通过 `unplugin-vue-components` + `ElementPlusResolver` 按需引入，`main.js` 中不再有 `app.use(ElementPlus)` 和整包 CSS 引入。

注意两点：

- **以编程方式调用的组件**（`ElMessage` / `ElMessageBox` / `ElNotification`）不出现在模板里，插件不会为它们注入样式，需要在 `src/assets/css/element-plus-programmatic.js` 中统一引入。新增同类组件时记得补上。
- **`vite.config.mjs` 必须是 ESM**。`unplugin-vue-components` 是 ESM-only 包，配置文件写成 `.js`（CommonJS）会导致构建失败。

`build.rollupOptions.output.manualChunks` 只固定 Vue 核心到 `vendor` 分块，不要把 `element-plus` 整包也固定进去，否则未使用的组件仍会进入首屏。

### 8.3 中文语言包

`src/App.vue` 用 `el-config-provider` 包裹路由并注入 `zhCn` 语言包。没有这一步，分页、日期选择器、消息框的默认文案会是英文（`OK` / `Cancel` / `Go to`）。

## 9. 未接入路由的预留页面

以下页面存在于 `src/views/` 下但未在路由中注册，属于按功能开关预留的实现，不会进入构建产物的首屏：

| 文件 | 说明 |
| --- | --- |
| `views/admin/AdminManagement.vue`、`LabAdminManagement.vue`、`LabAdminTransfer.vue`、`LabInfoManagement.vue` | 管理员功能页，其中 `LabAdminTransfer.vue` 待接入路由并补齐后端接口 |
| `views/admin/Delivery.vue`、`Students.vue` | 交付与学生管理页 |
| `views/admin/GrowthQuestionBank.vue` | 成长中心题库，模块开关 `app.modules.growth-center.enabled` |
| `views/student/Delivery.vue`、`EquipmentApply.vue`、`FreshmanGuide.vue`、`LabPortal.vue` | 学生端交付、设备申请、新生指南、实验室门户 |
| `views/student/GradPath*.vue` | 成长路径系列（学习、岗位、匹配、面试） |
| `views/student/StudentWrittenExam.vue` | 笔试模块，模块开关 `app.modules.written-exam.enabled` |
| `views/forum/Forum.vue`、`ForumPostDetail.vue` | 论坛模块，模块开关 `app.modules.forum.enabled` |
| `views/LabInfoIndex.vue`、`LabStudents.vue`、`OutstandingStudents.vue` | 实验室公开信息页 |

这些页面不会进入打包产物：Vite 只打包被引用的模块，未在路由中注册即不会生成对应 chunk。但它们会占用代码评审与维护精力，启用模块开关时应同步补齐路由。

## 10. 新增页面检查清单

- [ ] 根元素使用 `.page-shell`，横幅使用 `.page-hero`，内容区使用 `.panel-card`
- [ ] 表格或筛选表单在 768px 以下可用（卡片列表或横向滚动）
- [ ] 弹窗宽度使用 `min(420px, 92vw)` 一类的相对值
- [ ] 监听 `resize` 的组件在 `onBeforeUnmount` 中移除监听
- [ ] 移动端需要该页面时，同步在 `src/views/mobile/` 下提供对应实现，并在 `src/router/index.js` 注册 `/m/*` 路由
- [ ] 路由中该页面使用动态 `import()` 声明，未改成静态导入
- [ ] 新引用的编程式 Element Plus 组件已补充到 `src/assets/css/element-plus-programmatic.js`
- [ ] `npm run build` 与 `npx eslint . --ext .vue,.js` 均无报错

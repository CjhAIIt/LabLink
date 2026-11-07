# 前端界面规范

本文档说明 `lab/frontend` 的界面基线、响应式断点、页面骨架与常用样式类，供新增页面和重构既有页面时对照执行。

## 1. 视觉基线

### 1.1 设计令牌

所有视觉取值集中在 `src/assets/css/tokens.css`，由 `global.css` 的 `@import` 引入。
**页面里不写裸十六进制色值**，一律用 `var(--ll-*)`；确需新色先加令牌再用。

| 令牌组 | 说明 |
| --- | --- |
| `--ll-brand-50` … `--ll-brand-700` | 主色阶，主色为 `--ll-brand-600`（#4f46e5） |
| `--ll-logo-from` / `--ll-logo-mid` / `--ll-logo-to` | 品牌标识色块的三段蓝青渐变 |
| `--ll-code-bg` / `--ll-code-fg` | 代码编辑区底色与前景色 |
| `--ll-gray-0` … `--ll-gray-900` | 中性色阶 |
| `--ll-bg-body` / `--ll-bg-surface` / `--ll-bg-subtle` / `--ll-bg-hover` | 语义背景 |
| `--ll-text-primary` / `secondary` / `tertiary` / `disabled` / `inverse` | 语义文字 |
| `--ll-border-subtle` / `default` / `strong` / `brand` | 语义描边 |
| `--ll-radius-xs` … `--ll-radius-full` | 圆角，小 4 / 中 8 / 卡片 12 / 大 16 |
| `--ll-shadow-xs` … `--ll-shadow-lg` | 阴影，很浅；仅浮层可用 `md` 以上 |
| `--ll-space-1` … `--ll-space-12` | 4px 基准间距 |
| `--ll-text-xs` … `--ll-text-3xl` | 字号 |
| `--ll-sidebar-width` / `--ll-topbar-height` / `--ll-control-height` | 布局尺寸 |
| `--ll-transition-fast` / `--ll-transition-base` | 动效时长 |

分层原则：**用 1px 描边而不是阴影做分隔**，阴影只留给浮层（下拉、弹窗、抽屉）。
圆角上限 16px。数字（指标值、表格数值）统一 `font-variant-numeric: tabular-nums`。

当前全库 `.vue` 文件的裸色值与超大圆角均已清零，新增代码请沿用令牌，不要再写十六进制色值。

### 1.2 暗色主题

主题通过覆盖语义令牌实现，页面代码不需要感知主题。

- 开关：`composables/useTheme.js` 的 `useTheme()`，状态持久化在 `localStorage.lab_theme`
- 切换按钮：`components/ThemeToggle.vue`，已接入桌面顶栏、移动端顶栏、公开页与平台介绍页
- 应用方式：`<html data-theme="dark">`，`index.html` 里的内联脚本在首帧渲染前套用，避免暗色用户看到白闪
- 首次访问未设置过主题时跟随系统 `prefers-color-scheme`

暗色下有两类颜色不能跟随反转，单独用令牌固定：

| 令牌 | 用途 |
| --- | --- |
| `--ll-on-brand` | 品牌色实心背景上的文字，两套主题下都是浅色 |
| `--ll-bg-chrome` | 吸顶导航等半透明 chrome 层的底色 |

### 1.3 品牌

平台统一品牌标识为 **LabLink**，对应中文副标题“高校实验室管理平台”。

- 品牌标识组件：`src/components/BrandLogo.vue`
- 标识色块文字固定为 `Link`，主色为品牌蓝 `#2563eb`（深色态 `#1d4ed8`）
- 品牌在侧边栏、登录页、注册页、平台介绍页中的标题必须来自 `BrandLogo` 的 `title` / `subtitle` 属性，不要在页面里重复硬编码全称

### 1.4 颜色

| 用途 | 色值 |
| --- | --- |
| 主色 | `--ll-brand-600` / `#4f46e5` |
| 页面底色 | `--ll-bg-body`，`#f7f8fa` |
| 卡片底色 | `--ll-bg-surface`，`#ffffff` |
| 主文字 | `--ll-text-primary`，`#171a23` |
| 次文字 | `--ll-text-secondary`，`#646b7d` |
| 分隔线 | `--ll-border-subtle`，`#e8eaee` |

页面级渐变只允许出现在 `.page-hero--brand`（工作台首页的强调头部），
其余位置一律使用扁平卡片。

### 1.5 字体与滚动条

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
| `.page-shell` | 页面根容器，纵向 grid + gap |
| `.page-hero` | 页面头部，**扁平**：白底 + 1px 描边，标题 / 说明 / 右侧操作区 |
| `.page-hero--brand` | 强调型头部，仅工作台首页使用（品牌深色渐变） |
| `.page-hero h1` / `.hero-subtitle` / `.eyebrow` | 头部内的标题、副标题、眉标 |
| `.metric-grid` / `.metric-card` | 指标卡网格与单卡。桌面按容器宽度自适应（`auto-fit` + `minmax(200px, 1fr)`），768px 以下固定为 1 列 |
| `.metric-label` / `.metric-value` / `.metric-tip` | 指标卡的标签、数值、补充说明 |
| `.content-grid.two-column` | 双栏内容区 |
| `.panel-card` / `.toolbar-card` | 内容卡片、筛选工具条卡片 |
| `.toolbar-main` / `.toolbar-actions` / `.toolbar-form` | 工具条内部的标题区、操作区、筛选表单区 |
| `.panel-header` | 卡片头部，标题 + 操作按钮 |
| `.section-title` | 区块标题，左侧带品牌色竖条 |
| `.icon-chip` | 图标底片，36px 方块 |
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
├─ layouts/
│  ├─ AppShell.vue            桌面端共用外壳（侧边栏 + 顶栏 + 内容区）
│  ├─ AuthLayout.vue          公开页共用外壳（品牌 + 卡片 + 能力说明）
│  ├─ MobileShell.vue         移动端共用外壳（顶栏 + 底部标签栏）
│  ├─ AdminLayout.vue         管理端：只提供菜单数据与业务逻辑
│  ├─ StudentLayout.vue       学生端：同上
│  ├─ TeacherLayout.vue       教师端：同上
│  ├─ MobileStudentLayout.vue 移动端学生端：只提供标签栏数据
│  └─ MobileTeacherLayout.vue 移动端教师端：同上
├─ views/
│  ├─ admin/  student/  teacher/   桌面端页面
│  └─ mobile/                        移动端页面
│     ├─ Notices.vue  Profile.vue
│     ├─ student/  Applications.vue Dashboard.vue LabDetail.vue Labs.vue
│     └─ teacher/  CreateApplies.vue Dashboard.vue
├─ assets/css/
│  ├─ tokens.css              设计令牌层
│  ├─ global.css              桌面端页面基元与 Element Plus 覆盖
│  └─ mobile.css              移动端页面基元
├─ components/       跨页面复用组件
├─ api/              按业务域拆分的接口封装
└─ utils/            请求封装、鉴权、门户角色、文件地址处理
```

### 5.1 三个共用外壳

**`AppShell.vue`（桌面端）** —— 管理端 / 学生端 / 教师端共用，通过 props 注入：

| prop | 说明 |
| --- | --- |
| `roleLabel` | 顶栏左侧角色标签 |
| `menuGroups` | 导航分组 `[{ label, items: [{ path, label, icon, children? }] }]` |
| `userName` / `userMeta` / `userInitial` | 用户信息 |

新增角色端时只需复制一份 Layout 并提供 `menuGroups`，不要再复制外壳样式。

两种折叠状态是分开的，不要混用：

- **桌面端收成图标栏**：`isRail`，宽度 64px，`el-menu` 的 `collapse` 生效
- **移动端抽屉**：`isDrawerOpen`，侧边栏浮出并配遮罩层

**`AuthLayout.vue`（公开页）** —— 登录 / 注册 / 教师注册共用。props：`title`、`subtitle`、`features`、`width`；
表单放默认插槽，底部链接放 `footer` 插槽。

**`MobileShell.vue`（移动端）** —— 学生端 / 教师端共用。props：`tabItems`、`userInitial`、`rootPath`；
标签栏列数由 `tabItems.length` 决定，新增标签无需改样式。

### 5.2 页面骨架约定

桌面端页面根元素用 `.page-shell`，内容区外层由 `AppShell` 提供最大宽度与内边距；
移动端页面根元素用 `.m-page`，类名集中在 `mobile.css`。
**移动端页面不要写 `<style scoped>`**，统一在 `mobile.css` 中维护，避免各处样式漂移。

## 6. 交互约定

- 所有 `button` 统一 `cursor: pointer`，禁用态 `cursor: not-allowed`
- 可点击元素必须提供 hover 反馈；`global.css` 已提供全局 `:focus-visible` 轮廓，不要移除浏览器默认焦点环
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

- [ ] 根元素使用 `.page-shell`，头部使用 `.page-hero`（工作台首页用 `.page-hero--brand`），内容区使用 `.panel-card`
- [ ] 表格或筛选表单在 768px 以下可用（卡片列表或横向滚动）
- [ ] 弹窗宽度使用 `min(420px, 92vw)` 一类的相对值
- [ ] 监听 `resize` 的组件在 `onBeforeUnmount` 中移除监听
- [ ] 移动端需要该页面时，同步在 `src/views/mobile/` 下提供对应实现，并在 `src/router/index.js` 注册 `/m/*` 路由
- [ ] 移动端页面**没有** `<style scoped>`，样式统一写在 `assets/css/mobile.css`
- [ ] 路由中该页面使用动态 `import()` 声明，未改成静态导入
- [ ] 新引用的编程式 Element Plus 组件已补充到 `src/assets/css/element-plus-programmatic.js`
- [ ] 页面样式里没有裸十六进制色值与超过 16px 的圆角，一律用 `var(--ll-*)`
- [ ] 新增导航项时同步维护 `menuGroups` 的分组标题；新增移动端标签时标签栏列数自动跟随
- [ ] `npm run build` 与 `npx eslint . --ext .vue,.js` 均无报错

# 前端界面规范

本文档说明 `lab/frontend` 的界面基线、响应式断点、页面骨架与常用样式类，供新增页面和重构既有页面时对照执行。

## 1. 视觉基线

视觉基线参考 Linear、shadcn/ui 与 Vercel Geist：中性色用 Zinc 阶，主色为克制的品牌蓝，
只用淡彩语义色标注状态，避免霓虹感与厚重拟物。

### 1.1 设计令牌

所有视觉取值集中在 `src/assets/css/tokens.css`，由 `global.css` 的 `@import` 引入。
**页面里不写裸十六进制色值**，一律用 `var(--ll-*)`；确需新色先加令牌再用。

| 令牌组 | 说明 |
| --- | --- |
| `--ll-zinc-0` … `--ll-zinc-900` | 中性色阶。浅色 `#ffffff / #f4f4f5 / #e4e4e7`，暗色整体翻转为 `#09090b / #18181b / #27272a` |
| `--ll-gray-0` … `--ll-gray-900` | 历史灰阶别名，逐项指向 Zinc，**新增代码不要再用** |
| `--ll-brand-50` … `--ll-brand-700` | 主色阶（品牌蓝）。浅色主题下 `600 = #2563eb`，暗色主题下整阶反相成浅蓝 |
| `--ll-accent-50` … `--ll-accent-700` | 强调色阶，默认与品牌蓝同族，用于选中态、链接与图表；`--ll-accent-600` 会被界面设置里的主色覆盖 |
| `--ll-hero-from` / `--ll-hero-to` / `--ll-on-hero` | 深色区块（英雄区、标识块）底色。**不随主题反相**，浅色暗色下都是深蓝，保证底上的文字始终可读 |
| `--ll-logo-from` / `--ll-logo-mid` / `--ll-logo-to` / `--ll-on-logo` | 品牌标识色块的三段渐变与块上文字 |
| `--ll-success` / `--ll-running` / `--ll-warning` / `--ll-danger` / `--ll-info` | 语义色，浅色下为深字浅底，暗色下为亮字深底，各自带 `-bg` `-border` |
| `--ll-chart-1` … `--ll-chart-6` | 图表辅助色 |
| `--ll-bg-body` / `--ll-bg-surface` / `--ll-bg-subtle` / `--ll-bg-hover` / `--ll-bg-selected` | 语义背景，`--ll-bg-selected` 由 `color-mix` 跟随强调色 |
| `--ll-text-primary` / `secondary` / `tertiary` / `disabled` / `inverse` / `brand` / `link` | 语义文字 |
| `--ll-on-brand` | **实心品牌底上的文字**。品牌阶在暗色下反相为浅蓝，所以此令牌在暗色下是深色 `#09090b` |
| `--ll-border-subtle` / `default` / `strong` / `brand` | 语义描边 |
| `--ll-radius-xs` … `--ll-radius-full` | 圆角，小 3 / 中 5 / 卡片 7 / 大 10 / 超大 14 |
| `--ll-shadow-xs` … `--ll-shadow-lg` | 阴影，很浅；仅浮层可用 `md` 以上 |
| `--ll-space-1` … `--ll-space-12` | 4px 基准间距 |
| `--ll-text-xs` … `--ll-text-3xl` | 字号，基础 13px |
| `--ll-font-sans` / `--ll-font-mono` | 无衬线 UI 字体与等宽字体。**设备编号、CAS 号、条码、批次号、数值指标一律用 `--ll-font-mono`** |
| `--ll-sidebar-width` / `--ll-topbar-height` / `--ll-control-height` / `--ll-row-height` | 布局尺寸，表格行高 34px |
| `--ll-transition-fast` / `--ll-transition-base` | 动效时长 |

分层原则：**用 1px 描边而不是阴影做分隔**，阴影只留给浮层（下拉、弹窗、抽屉）。
圆角上限 14px。数字（指标值、表格数值）统一 `font-variant-numeric: tabular-nums`。

当前全库 `.vue` 文件的裸色值与超大圆角均已清零，新增代码请沿用令牌，不要再写十六进制色值。

### 1.2 暗色主题

主题通过覆盖语义令牌实现，页面代码不需要感知主题。

- 开关：`stores/settings.js` 的 `useSettingsStore()`，状态持久化在 `localStorage.lab_settings`
- 切换入口：`components/ThemeToggle.vue`（顶栏一键切换）与 `layouts/components/ThemeSettings.vue`（设置抽屉，三选一）
- 应用方式：`<html data-theme="dark">`，`index.html` 里的内联脚本在首帧渲染前套用，避免暗色用户看到白闪
- 未设置过主题时跟随系统 `prefers-color-scheme`

暗色下有三类颜色不能简单跟随反转，各用独立令牌收口：

| 令牌 | 用途 |
| --- | --- |
| `--ll-on-brand` | 实心品牌底上的文字。品牌阶在暗色下反相成浅蓝，所以此令牌在暗色下是深色 `#09090b` |
| `--ll-hero-from` / `--ll-hero-to` / `--ll-on-hero` | 深色区块底色与底上的文字，**两套主题下都保持深色**，避免英雄区在暗色下变成白底白字 |
| `--ll-bg-chrome` | 吸顶导航等半透明 chrome 层的底色 |

**新增页面时不要写死浅色背景**（例如 `rgba(248, 250, 252, 0.92)`），
一律用 `--ll-bg-surface` / `--ll-bg-subtle` / `--ll-bg-muted`，否则暗色主题下会出现白底浅字。

Element Plus 在暗色下的主色是浅蓝，其「主色实心底」默认配白字，对比度不足，
`global.css` 中用 `[data-theme='dark'] .el-button--primary:not(.is-plain):not(.is-link):not(.is-text)`
把这类实心按钮的文字改为深色。新增自定义主色时同理。

### 1.3 品牌

平台统一品牌标识为 **LabLink**，对应中文副标题“高校实验室管理平台”。

- 品牌标识组件：`src/components/BrandLogo.vue`；标识图形组件：`src/components/BrandMark.vue`
- 标识图形为「蓝色圆角方块 + 白色菱形与三条平行斜线」，矢量组件为 `src/components/BrandMark.vue`，
  与 `public/favicon.svg` 是同一套图形；侧边栏、登录页、平台介绍页共用，不允许各写一份
- 品牌在侧边栏、登录页、注册页、平台介绍页中的标题必须来自 `BrandLogo` 的 `title` / `subtitle` 属性，不要在页面里重复硬编码全称

### 1.4 颜色

| 用途 | 色值 |
| --- | --- |
| 主色 | `--ll-brand-600`，浅色 `#2563eb`，暗色 `#93c5fd` |
| 强调色 | `--ll-accent-600`，浅色 `#2563eb`，暗色 `#93c5fd` |
| 页面底色 | `--ll-bg-body`，浅色 `#f4f4f5`，暗色 `#09090b` |
| 卡片底色 | `--ll-bg-surface`，浅色 `#ffffff`，暗色 `#18181b` |
| 主文字 | `--ll-text-primary`，浅色 `#09090b`，暗色 `#fafafa` |
| 次文字 | `--ll-text-secondary`，浅色 `#3f3f46`，暗色 `#a1a1aa` |
| 分隔线 | `--ll-border-subtle`，浅色 `#e4e4e7`，暗色 `#27272a` |

页面级渐变只允许出现在 `.page-hero--brand`（工作台首页的强调头部）、
移动端 `.hero` 横幅与深色区块，其余位置一律使用扁平卡片。

英雄区的底色来自 `--ll-hero-from` / `--ll-hero-to`，两组 ±60° 重复渐变叠出六边形网格纹饰，
不要在页面里另写渐变。

### 1.5 字体与滚动条

- 基础字号 13px，`body` 已开启 `-webkit-font-smoothing: antialiased`
- 标识与数值（设备编号、学号、批次号、指标值）使用 `--ll-font-mono`
- 滚动条统一 8px，轨道 `#f1f1f1`，滑块 `#c1c1c1`
- 移动端已加 `-webkit-tap-highlight-color: transparent`，避免点击出现系统高亮方块

### 1.6 界面设置

顶栏的齿轮按钮打开「界面设置」抽屉（`layouts/components/ThemeSettings.vue`），
统一收口主题模式、界面密度、主色调与导航形态，状态全部持久化在 `localStorage.lab_settings`。

| 设置项 | 取值 | 落地方式 |
| --- | --- | --- |
| 主题模式 | 浅色 / 深色 / 跟随系统 | `<html data-theme="dark">`，`auto` 时读 `prefers-color-scheme` |
| 界面密度 | 默认 / 紧凑 | `<html data-density="compact">` + `global.css` 中的 `[data-density='compact']` 块，收紧表格行高、控件高度与留白 |
| 主色调 | 品牌蓝 / 靛蓝 / 青蓝 / 紫罗兰 / 自定义取色 | 覆盖 `--ll-accent-600`、`--ll-accent-700` 与 `--el-color-primary` |
| 收起侧边栏 | 开 / 关 | 桌面端侧边栏收成 64px 图标栏 |
| 显示页签栏 | 开 / 关 | 顶栏下方显示多页签导航 |

主色调预设**明暗成对**：`THEME_PRESETS` 里每项都有 `colors.primary`（浅色）与
`darkPrimary` / `darkPrimaryStrong`（暗色）。暗色下必须换成亮色版本，
否则强调色会压暗到几乎看不见。自定义取色只有一个色值，
暗色下由 `mixWithWhite()` 自动混白提亮，无需用户手填两次。

## 2. 响应式断点

平台使用三个断点，含义不同，不要混用：

| 断点 | 含义 | 触发行为 |
| --- | --- | --- |
| `960px` | 侧边栏抽屉化 | `AdminLayout` / `StudentLayout` / `TeacherLayout` 的侧边栏变为遮罩式抽屉，默认折叠 |
| `900px` | 内容区收敛 | `.content-grid.two-column` 变为单列，`.page-hero` 改为纵向排布 |
| `768px` | 移动端形态 | `isMobilePortal()` 返回 `true`，路由分流到 `/m/student`、`/m/teacher`、`/m/admin`；同时全局样式收紧字号、间距与表格 |

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
| `.page-hero--brand` | 强调型头部，仅工作台首页使用（品牌深蓝渐变 + 六边形网格纹饰） |
| `.page-hero h1` / `.hero-subtitle` / `.eyebrow` | 头部内的标题、副标题、眉标 |
| `.metric-grid` / `.metric-card` | 指标卡网格与单卡。桌面按容器宽度自适应（`auto-fit` + `minmax(200px, 1fr)`），768px 以下固定为 1 列 |
| `.metric-head` / `.metric-icon` | 指标卡图标块：彩色底 + 同色图形。色调用 `.metric-icon--success` / `--warning` / `--danger` / `--info` 区分 |
| `.metric-card--success` 等 | 指标卡按语义色描边，与图标块呼应 |
| `.metric-delta--up` / `.metric-delta--down` | 增减量文字，上涨用绿、下跌用红 |
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

**表格里的长文本列必须加 `show-overflow-tooltip`**（实验室名、邮箱、学院名、专业、标题等）。
否则列宽被固定操作列挤占时内容会被静默截断，用户既看不到完整值也不知道被截断了。
纯数字、短枚举、状态列不需要加。

对弹窗统一使用相对宽度，避免固定像素宽度在窄屏溢出：

```html
<el-dialog width="min(720px, 92vw)" />
```

## 5. 布局与页面组织

```
src/
├─ layouts/
│  ├─ AppShell.vue            桌面端共用外壳（侧边栏 + 顶栏 + 面包屑 + 页签栏 + 内容区）
│  ├─ AuthLayout.vue          公开页共用外壳（品牌 + 卡片 + 能力说明）
│  ├─ MobileShell.vue         移动端共用外壳（顶栏 + 底部标签栏）
│  ├─ AdminLayout.vue         管理端：只提供菜单数据与业务逻辑
│  ├─ StudentLayout.vue       学生端：同上
│  ├─ TeacherLayout.vue       教师端：同上
│  ├─ MobileStudentLayout.vue 移动端学生端：只提供标签栏数据
│  ├─ MobileTeacherLayout.vue 移动端教师端：同上
│  ├─ MobileAdminLayout.vue   移动端管理端：只提供标签栏数据
│  └─ components/
│     ├─ TagsView.vue         多页签导航，由 composables/useTagsView.js 驱动
│     └─ ThemeSettings.vue    界面设置抽屉（主题 / 密度 / 主色 / 侧边栏 / 页签）
├─ views/
│  ├─ admin/  student/  teacher/   桌面端页面
│  └─ mobile/                        移动端页面
│     ├─ Notices.vue  Profile.vue
│     ├─ admin/      Dashboard.vue Approvals.vue Labs.vue Notices.vue
│     ├─ student/  Applications.vue Dashboard.vue LabDetail.vue Labs.vue
│     └─ teacher/  CreateApplies.vue Dashboard.vue
├─ assets/css/
│  ├─ tokens.css              设计令牌层
│  ├─ global.css              桌面端页面基元与 Element Plus 覆盖
│  └─ mobile.css              移动端页面基元
├─ components/       跨页面复用组件
├─ stores/           Pinia 状态：settings.js（界面设置）、user.js 等
├─ composables/      useTheme.js、useTagsView.js 等组合式逻辑
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

外壳自带四块内容，顶栏与侧边栏的形态由「界面设置」统一驱动：

- **面包屑**：顶栏左侧「角色 › 页面标题」，让用户始终知道自己在哪一层
- **页签栏**：`TagsView`，记录访问过的页面，最多保留 12 个，可在设置里关闭
- **侧边栏折叠**：`isRail`（桌面端收成 64px 图标栏）与 `isDrawerOpen`（移动端抽屉）是两种状态，不要混用
- **界面设置入口**：顶栏齿轮按钮，打开 `ThemeSettings` 抽屉

**`AuthLayout.vue`（公开页）** —— 登录 / 注册 / 教师注册共用。props：`title`、`subtitle`、`features`、`width`；
表单放默认插槽，底部链接放 `footer` 插槽。

**`MobileShell.vue`（移动端）** —— 学生端 / 教师端 / 管理端共用。props：`tabItems`、`userInitial`、`rootPath`；
标签栏列数由 `tabItems.length` 决定，新增标签无需改样式。

### 5.2 页面骨架约定

桌面端页面根元素用 `.page-shell`，内容区外层由 `AppShell` 提供最大宽度与内边距；
移动端页面根元素用 `.m-page`，类名集中在 `mobile.css`。
**移动端页面不要写 `<style scoped>`**，统一在 `mobile.css` 中维护，避免各处样式漂移。

`mobile.css` 的规则都挂在 `.m-shell` 下，靠类名语义区分用途。
`.hero` 是首页的**深色横幅**，`.card` 是**普通白卡**，两者不能叠在同一个元素上——
`mobile.css` 里 `.card` 写在 `.hero` 之后会覆盖它的背景与布局，导致横幅变成白卡、
标题文字沿用横幅的浅色而看不见。个人信息页头部就踩过这个坑，
现在单独使用 `.profile-hero`。

新增移动端页面时，同名的语义类只复用一次：
横幅用 `.hero`、普通卡片用 `.card`、属性行用 `.row`，需要变体就另起一个类名。

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

## 10. 前端测试

前端使用 Vitest（jsdom 环境），测试文件放在被测模块同级，命名为 `*.test.js`。

```bash
npm run test          # 跑一次
npm run test:watch    # 监听模式
```

配置文件是 `vitest.config.mjs`（独立于 `vite.config.mjs`，避免测试环境加载按需引入插件）。

当前覆盖：

| 文件 | 覆盖内容 |
| --- | --- |
| `src/router/routeResolution.test.js` | 桌面端与移动端路由互斥解析，含「没有对端实现时不得改写路径」这条规则 |
| `src/utils/portal.test.js` | 门户角色判定与落地页选择 |
| `src/utils/request.test.js` | 响应拦截器，重点是会话失效时清空会话并跳登录页 |
| `src/composables/useTheme.test.js` | 主题持久化、切换与系统偏好兜底 |
| `src/stores/settings.test.js` | 界面设置：主题模式、密度、主色预设、自定义主色、侧边栏与页签开关的持久化与落地，含「暗色下强调色换成亮色版本」 |

新增业务逻辑时优先把纯逻辑抽成独立模块再写单测（`routeResolution.js` 就是这么做的），
这样不必为了测试而加载整个应用。

## 11. 新增页面检查清单

- [ ] 根元素使用 `.page-shell`，头部使用 `.page-hero`（工作台首页用 `.page-hero--brand`），内容区使用 `.panel-card`
- [ ] 指标卡使用 `.metric-head` + `.metric-icon`，图标色与卡片语义色一致，不要自绘色块
- [ ] 深色区块用 `--ll-hero-from` / `--ll-hero-to` / `--ll-on-hero`，实心品牌底上的文字用 `--ll-on-brand`
- [ ] 页面样式里没有裸十六进制色值与超过 14px 的圆角，一律用 `var(--ll-*)`
- [ ] 没有硬编码的浅色背景（如 `rgba(248,250,252,.92)`），否则暗色主题下会变成白底浅字
- [ ] 标识与数值（设备编号、学号、批次号、指标值）使用 `--ll-font-mono`
- [ ] 表格或筛选表单在 768px 以下可用（卡片列表或横向滚动）
- [ ] 弹窗宽度使用 `min(420px, 92vw)` 一类的相对值
- [ ] 监听 `resize` 的组件在 `onBeforeUnmount` 中移除监听
- [ ] 移动端需要该页面时，同步在 `src/views/mobile/` 下提供对应实现，并在 `src/router/index.js` 注册 `/m/*` 路由
- [ ] 移动端页面**没有** `<style scoped>`，样式统一写在 `assets/css/mobile.css`
- [ ] 移动端页面的语义类名没有与 `mobile.css` 里的通用类冲突（`.hero` 是深色横幅、`.card` 是白卡）
- [ ] 路由中该页面使用动态 `import()` 声明，未改成静态导入
- [ ] 新引用的编程式 Element Plus 组件已补充到 `src/assets/css/element-plus-programmatic.js`
- [ ] 新增导航项时同步维护 `menuGroups` 的分组标题；新增移动端标签时标签栏列数自动跟随
- [ ] `npm run build`、`npx eslint . --ext .vue,.js` 与 `npm run test` 均无报错

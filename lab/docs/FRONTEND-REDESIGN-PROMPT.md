# 前端界面重做提示词

> 本文件是交给 AI 编码工具（或前端同事）执行的提示词。使用时把「提示词正文」整段发出即可。
> 仓库中 `lab/frontend/src/assets/css/tokens.css` 与 `global.css` 已按本提示词的设计语言落地，
> 可作为参照基线，不必推翻重写。

---

## 提示词正文

### 角色

你是一位资深前端架构师，擅长企业级中后台界面的设计实现，熟悉 Vue 3 与 Element Plus，
有 Ant Design Pro、Arco Design、Linear、GitHub 等主流后台产品的落地经验。
你的审美偏克制、密度高、重视信息层级，**不使用大面积渐变、超大圆角和厚重投影**。

### 任务

把 LabLink 实验室管理平台的前端做一次**整体视觉重做**。当前界面观感陈旧，
需要建立一套统一的设计语言，并让三端（管理端 / 学生端 / 教师端）与移动端整体焕新。

### 仓库与现状

- 仓库根目录：`C:\Users\cjh\Documents\LabLink`
- 前端目录：`lab/frontend`
- 技术栈：Vue 3.3 + Vite 4 + Element Plus 2.4 + Pinia + vue-router 4，JavaScript（非 TS）
- 关键路径：
  - `src/assets/css/tokens.css` —— **设计令牌层，已存在，直接使用**
  - `src/assets/css/global.css` —— **全局基元样式，已按新语言重写，直接使用**
  - `src/assets/css/element-plus-programmatic.js` —— 编程式调用组件的样式补齐入口
  - `src/layouts/` —— `AdminLayout` / `StudentLayout` / `TeacherLayout` / `MobileStudentLayout` / `MobileTeacherLayout`
  - `src/views/admin|student|teacher|mobile/` —— 页面
  - `src/components/BrandLogo.vue` —— 品牌标识
  - `src/router/index.js` —— 路由，含 `/m/student`、`/m/teacher` 两棵移动端路由树
  - `src/App.vue` —— 已用 `el-config-provider` 注入 zh-cn 语言包
- 路由分流：`src/utils/portal.js` 的 `isMobilePortal()` 按视口宽度决定走桌面端还是 `/m/*`

### 设计参考（取其形，不照抄色）

| 参考对象 | 借鉴点 |
| --- | --- |
| **Ant Design Pro / Ant Design 5** | 侧边栏分组、页面头部与内容区分层、表格密度、标签页与筛选区组织 |
| **Linear / Vercel Dashboard** | 克制的中性色、极细分隔线代替阴影、数字用等宽字形、紧凑行高 |
| **GitHub** | 表格优先、1px 描边、6–8px 圆角、悬停态用浅底色而非投影 |
| **飞书 / 阿里云控制台** | 分组导航、工具条与内容区分离、空状态与加载态的统一表达 |
| **Arco Design** | 表单栅格、筛选区折叠、状态色克制 |

### 设计语言（已落地为令牌，务必复用而不是另起一套）

```css
--ll-brand-600: #18181b;   /* 主色，石墨；暗色下反相为 #fafafa */
--ll-accent-600: #4f46e5;   /* 强调色，靛蓝；暗色下为 #a5b4fc */
--ll-zinc-50 … --ll-zinc-900;   /* 中性色阶，Zinc */
--ll-bg-surface: #ffffff;   /* 卡片底色 */
--ll-bg-body: #f4f4f5;      /* 页面底色 */
--ll-text-primary / secondary / tertiary;
--ll-border-subtle: #e4e4e7;   /* 1px 描边，优先于阴影做分隔 */
--ll-hero-from / --ll-hero-to / --ll-on-hero;  /* 深色区块底色，不随主题反相 */
--ll-radius-sm 5px / md 7px / lg 10px / xl 14px;
--ll-shadow-xs / sm / md / lg;  /* 极浅，多层 */
--ll-space-1 … --ll-space-12;  /* 4px 基准间距 */
--ll-font-mono;   /* 标识与数值用等宽字体 */
--ll-sidebar-width: 236px;  --ll-topbar-height: 56px;
```

必须遵守：

1. **页面里不出现裸十六进制色值**，一律用 `var(--ll-*)`。确需新色先加令牌再用。
2. **圆角不超过 14px**，卡片 10px、控件 7px、小元素 5px。
3. **分层靠 1px 描边**，阴影只在浮层（下拉、弹窗、抽屉）使用。
4. **信息密度优先**：正文 13px、表格行高 34px、控件高 32px，另提供一档「紧凑」密度。
5. 数字（指标卡数值、表格数值）使用 `font-variant-numeric: tabular-nums`；
   设备编号、学号、批次号等标识使用 `--ll-font-mono`。
6. 动效只用 `--ll-transition-fast`（120ms）与 `--ll-transition-base`（200ms）。
7. 深色区块（英雄区、标识块）用 `--ll-hero-*` 令牌，实心品牌底上的文字用 `--ll-on-brand`，
   **不要写死浅色背景**，否则暗色主题下会出现白底浅字。

### 改造范围（按优先级执行）

#### 1. 应用外壳（最高优先级，影响全部页面）

三套桌面布局 `AdminLayout` / `StudentLayout` / `TeacherLayout` 结构重复，
**先抽出一个共用组件 `src/layouts/AppShell.vue`**，通过 props 注入菜单分组、角色名、用户信息，
三个布局各自只保留菜单数据与业务逻辑，消除三份重复样式。

外壳要求：

- **侧边栏** 236px，纯白底，右侧 1px 描边，不要投影。
  菜单项高 38px、圆角 8px、图标 16px、选中态为品牌色浅底 + 品牌色文字（不要纯黑加粗）。
  按业务分组，分组标题为 11px 大写灰色文字。分组示例（学生端）：
  `概览 / 实验室 / 事务 / 账号`。
- **桌面端折叠为 64px 图标栏**（`el-menu` 的 `collapse` 能力 + tooltip），
  与移动端的抽屉折叠区分开：桌面是"收成图标条"，移动端是"抽屉滑出"。
  现有代码把两者混用一个 `isCollapse`，需要拆成「桌面图标栏状态」和「移动端抽屉状态」两个变量。
- **顶栏** 56px，白底 + 1px 下边框，吸顶。
  左侧折叠按钮 + 面包屑式标题（角色小标签 + 当前页标题），右侧用户信息。
  用户头像块去掉外投影，改为 1px 描边 + hover 浅底。
- **内容区** 页面底色 `--ll-bg-body`，内边距 24px，内容最大宽度 1560px 居中。

#### 2. 页面基元（全局已重写，检查各页面是否正确使用）

已重写的全局类：`page-shell`、`page-hero`、`metric-grid/card`、`panel-card`、
`toolbar-card`、`panel-header`、`content-grid.two-column`、`rank-*`、`empty-panel`，
并新增 `page-hero--brand`（强调型头部）、`section-title`（区块标题）、`icon-chip`（图标底片）。

- `.page-hero` 默认是**扁平**页面头部（白底 + 1px 描边 + 22px 标题 + 次级说明）。
  只有工作台首页使用 `.page-hero--brand` 的品牌深色渐变版本，
  **其余页面一律不再出现彩色渐变横幅**。
- `.metric-card` 为白底描边卡片，hover 时描边加深并浮起 1px，不要整块换色。
- `.toolbar-card` 是"筛选区"，`.panel-card` 是"内容区"，两者不要混用。

#### 3. 三端工作台首页

`views/student/Dashboard.vue`、`views/teacher/Dashboard.vue`、`views/admin/Dashboard.vue`：

- 顶部改用 `.page-hero--brand`，删掉原先各自为政的 `linear-gradient` +
  `radial-gradient` 叠色（现在这三种深色渐变是最"土"的部分），
  统一为 `--ll-hero-from → --ll-hero-to` 的单一方向渐变。
- 指标卡区用 `.metric-grid` + `.metric-card`；
  数值统一 28px / 600 字重 / 等宽数字。
- 内容区用 `.content-grid.two-column` + `.panel-card`，
  面板头部用 `.panel-header`，空态用 `.empty-panel`。
- 列表项（招新计划、公告、排行）改成**扁平列表行**：
  1px 分隔线或浅底色，不要每个条目一个圆角卡片。

#### 4. 列表 / 表单 / 详情页（覆盖 `views/admin|student|teacher/` 其余页面）

- **筛选区**：`.toolbar-card` 内用 `.toolbar-form`，一行放得下就一行，
  放不下按 3 列 / 2 列 / 1 列自适应。
- **表格**：Element Plus 表格统一覆盖（已在 `global.css` 中做了基础覆盖），
  表头浅灰底、字号 13px、单元格内边距 10px 0、悬停行用 `--ll-brand-50`。
- **弹窗**：宽度用 `min(720px, 92vw)` 这类相对值；
  `el-dialog` 圆角 16px、标题 16px/600。
- **表单**：`el-form` 标签 90–110px 固定宽，控件高 32px、圆角 8px；
  危险操作按钮用 `type="danger"` 且放在表单底部右侧。
- **空态**：统一用 `el-empty`，描述文案用 `--ll-text-tertiary`，
  有引导动作时在下方放一个主按钮。

#### 5. 移动端

`src/layouts/MobileStudentLayout.vue`、`MobileTeacherLayout.vue` 及 `src/views/mobile/`：

- 底部标签栏：`position: fixed`，白底 + 顶部 1px 描边，选中态品牌色，
  保留 `env(safe-area-inset-bottom)` 内边距；内容区预留等高底部空间。
- 顶栏 52px，白底 + 底部描边；标题 16px/600；返回按钮 32px 触控区。
- 移动端卡片全部 12px 圆角、16px 内边距，指标网格 2 列（不是 1 列，
  单列太松散，主流 App 都是 2 列）。
- 移动端不要出现横向滚动条，表格改卡片列表。

#### 6. 公开页（第一印象，最影响观感）

`views/Intro.vue`（平台介绍）、`views/Login.vue`、`views/Register.vue`、`views/TeacherRegister.vue`：

- 当前登录页是"左右两栏 + 径向渐变底 + 24px 圆角 + 60px 投影"，是最需要重做的部分。
- 改为**居中单栏卡片**（宽度 400–440px）：品牌标识 → 标题 → 表单 → 底部辅助链接，
  背景用极浅的中性色，不再用紫蓝粉三色径向渐变。
- 平台介绍页改为**左文右图 / 特性网格**：标题区 + 4 个特性卡片（图标 + 标题 + 说明）
  + 实验室概览列表，去掉整屏的渐变背景与多层装饰圆形。
- 这三个页面是访客第一眼，必须做到干净、可信、信息清楚。

### 约束

- **不要引入新依赖**。需要图标就用已在用的 `@element-plus/icons-vue`。
- **不要改动后端**。本次只动 `lab/frontend`。
- **不要改动路由结构与业务逻辑**，只调整表现层与结构组织。
- Element Plus 是**按需引入**的（`vite.config.mjs` + `unplugin-vue-components`）：
  新用到的组件模板标签会自动按需引入；
  但**以编程方式调用**的组件（`ElMessage` / `ElMessageBox` / `ElNotification`）
  必须同步补进 `src/assets/css/element-plus-programmatic.js`，否则样式丢失。
- `vite.config.mjs` 是 ESM（`unplugin-vue-components` 是 ESM-only 包），不要改回 `.js`。
- 不要把 `manualChunks` 里的 `element-plus` 整包固定进去，会破坏按需引入。
- 保持 `960px`（侧边栏抽屉）与 `768px`（移动端形态）两个断点语义不变。
- 保留移动端与桌面端两套路由树的分流逻辑不变；
  **新增移动端页面树时**，同样在 `src/router/index.js` 注册 `/m/*` 路由，
  并在 `src/router/routeResolution.js` 里配好与桌面端的互斥关系。

### 验收标准

1. `cd lab/frontend && npm run build` 通过，无报错。
2. `npx eslint . --ext .vue,.js,.jsx,.cjs,.mjs --ignore-path .gitignore` 无输出。
3. 页面中不再出现裸十六进制颜色（`grep -rn "#[0-9a-fA-F]\{6\}" src/` 的结果需逐条确认是令牌定义或确有必要的图片/图表色）。
4. 圆角最大值不超过 14px；除浮层外无 `box-shadow` 超过 `--ll-shadow-sm`。
5. 1440px 与 1024px 桌面视口、390px 手机视口下，
   管理端 / 学生端 / 教师端 / 登录页 / 平台介绍页均无横向滚动条、无元素重叠、无文字截断异常；
   **浅色与暗色两套主题都要过一遍**。
6. `el-table`、`el-dialog`、`el-drawer`、`el-select`、`el-date-picker`、`el-pagination`
   在按需引入后样式完整（尤其下拉面板与日期面板）。
7. 键盘可达：所有可点击元素有 `:focus-visible` 轮廓。

### 执行顺序

1. 抽 `AppShell.vue` 并重做外壳 → 三套桌面布局接入。
2. 按页面类型逐个改造：工作台首页 → 列表页 → 表单/详情页 → 公开页。
3. 移动端外壳与移动端页面。
4. 每个阶段跑一次 build + lint，最后做一轮多视口浏览器回归。

---

## 执行结果（2026-11）

本提示词已在 `feat/frontend-redesign` 分支落地，对应 PR #2。

| 范围 | 状态 | 说明 |
| --- | --- | --- |
| 应用外壳 | 已完成 | 新增 `AppShell.vue` / `AuthLayout.vue` / `MobileShell.vue` 三个共用外壳，三端桌面布局与公开页不再各写一份 |
| 页面基元 | 已完成 | `global.css` 重写并新增 `mobile.css`；`.page-hero` 扁平化 + `.page-hero--brand` 强调变体 |
| 工作台首页 | 已完成 | 三端首页统一品牌渐变，列表项改为扁平分隔线列表行 |
| 公开页 | 已完成 | 登录 / 注册 / 教师注册改居中单栏，平台介绍页改导航 + 首屏 + 特性网格 |
| 列表 / 表单 / 详情页 | 已完成 | 全库 `.vue` 的 241 处裸色值与 58 处超大圆角令牌化，含尚未接入路由的预留页面 |
| 移动端 | 已完成 | 8 个页面移除 scoped 样式统一走 `mobile.css`，指标网格两列，规则挂在 `.m-shell` 下避免与桌面端通用类名冲突 |
| 令牌层 | 已完成 | `tokens.css` 覆盖色阶、圆角、阴影、间距、字号、布局尺寸、动效、品牌标识与代码编辑区 |
| 第三轮：管理端模板参考 | 已完成 | 参考 `vue3-element-admin` / `vue-pure-admin` 落地界面设置抽屉、页签栏、面包屑、主色预设与密度档位；新增移动端管理端页面树 `/m/admin/*` |

验收标准核对：

| 标准 | 结果 |
| --- | --- |
| `npm run build` 通过 | 通过 |
| `npx eslint` 无输出 | 通过 |
| 无裸十六进制色值 | 全库 `.vue` 0 处 |
| 圆角不超过 16px | 全库 `.vue` 0 处超限 |
| 浮层外无过重投影 | 19 处自定义投影已归一到 `--ll-shadow-xs` / `--ll-shadow-sm`，仅浮层保留 `lg` |
| 多视口无横向滚动 | 1440 / 1024 / 390 实测通过 |
| Element Plus 按需引入后样式完整 | 已补 `element-plus-programmatic.js` |
| 键盘可达 | `global.css` 已提供全局 `:focus-visible` 轮廓 |

### 暗色主题（追加）

在上述改造之外补齐了暗色主题：

- `tokens.css` 增加 `[data-theme='dark']` 令牌块，覆盖中性色、语义色、阴影与品牌标识渐变
- `global.css` 增加 Element Plus 暗色变量覆盖与组件级修正，表格、弹窗、下拉、分页、标签页等无需改动即可跟随
- 新增 `composables/useTheme.js` 与 `components/ThemeToggle.vue`，切换入口接入桌面顶栏、移动端顶栏、公开页与平台介绍页
- `index.html` 内联脚本在首帧渲染前套用主题，避免暗色用户看到白闪；首次访问跟随系统 `prefers-color-scheme`

暗色令牌有三类不能简单反转，2026-11 第三轮统一收口（详见 `FRONTEND-UI-GUIDE.md` §1.2）：

| 令牌 | 语义 |
| --- | --- |
| `--ll-on-brand` | **实心品牌底上的文字**。品牌阶在暗色下反相成浅色，所以此令牌在暗色下是深色 `#09090b` |
| `--ll-hero-from` / `--ll-hero-to` / `--ll-on-hero` | 深色区块底色与底上的文字，两套主题下都保持深色石墨 |
| `--ll-bg-chrome` | 吸顶栏半透明底色 |

### 第三轮：参考管理端模板的重构

参考 `youlaitech/vue3-element-admin` 与 `pure-admin/vue-pure-admin` 两个开源模板，
在现有 Vue 3 + Element Plus 栈上落地它们的招牌能力，**不引入第二套前端框架**：

| 参考模板的能力 | LabLink 的落地 |
| --- | --- |
| 设置抽屉（主题模式 / 布局 / 尺寸 / 页签开关） | `stores/settings.js` + `layouts/components/ThemeSettings.vue`，统一收口主题模式、界面密度、主色调、侧边栏折叠与页签栏 |
| 页签视图（tags view） | `layouts/components/TagsView.vue` + `composables/useTagsView.js`，最多保留 12 个页签 |
| 面包屑导航 | `AppShell.vue` 顶栏左侧「角色 › 页面标题」 |
| 主题色预设 | `THEME_PRESETS` 四套色板（石墨 / 靛蓝 / 青碧 / 石板）+ 自定义取色，明暗成对取值 |
| 紧凑 / 默认密度档位 | `[data-density='compact']` 收紧表格行高、控件高度与页面留白 |

这一轮同时修掉了上一轮遗留的暗色缺陷：

- `--ll-logo-*` 只在暗色块里定义，浅色主题下标识块渐变整体失效 → 补齐 `:root` 定义
- `--ll-gray-*` 灰阶在令牌重写时整体丢失，27 处引用悬空 → 补回指向 Zinc 的兼容别名
- 品牌阶在暗色下反相后，深色区块（英雄区、标识块）变成白底白字 → 拆出 `--ll-hero-*` 令牌
- 主色预设把单色值写死成内联样式，暗色下强调色压暗 → 预设改为明暗成对，自定义色自动混白提亮
- Element Plus 暗色下主色实心底配白字对比度不足 → 定向覆盖实心按钮、单选、复选的文字色
- 移动端个人信息页把 `.hero`（深色横幅）与 `.card`（白卡）叠在同一元素上，姓名浅色压白底 → 拆出 `.profile-hero`

新增移动端管理端页面树 `/m/admin/*`（首页 / 审批 / 实验室 / 公告 / 我的），
与桌面端 `/admin/*` 互斥，由 `utils/routeResolution.js` 统一解析。

---

## 附：给执行者的补充说明

- 设计基线已经写好了，**不要重写 `tokens.css` 与 `global.css` 的结构**，
  它们已经把色板、圆角、阴影、间距、字号、Element Plus 覆盖收敛到一处。
  你的工作是让页面**用上**这套语言，而不是再造一套。
- 改页面时优先**复用全局类**（`page-shell` / `page-hero` / `panel-card` / `toolbar-card` /
  `metric-card` / `panel-header` / `section-title` / `icon-chip`），
  页面内 scoped 样式只写页面特有的部分。
- 如果发现某个页面的 `scoped` 样式里写了大量裸色值和圆角，优先删掉这些局部样式，
  改用全局类 —— 这是本次重做最主要的收益来源。

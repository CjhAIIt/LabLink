# 安徽信息工程学院实验室管理平台 - 多端开发指南 (桌面端与安卓端)

本文档旨在为当前基于 Vue 3 + Vite 构建的 Web 版实验室管理平台提供向 **桌面端 (Windows/macOS/Linux)** 和 **安卓移动端 (Android)** 扩展的开发指南与技术方案。

## 1. 桌面端开发方案 (Desktop)

对于现有的 Vue 3 + Vite 前端项目，将其打包为桌面端应用的最佳实践是使用 **Tauri** 或 **Electron**。考虑到性能和包体积，推荐首选 **Tauri**。

### 1.1 推荐方案：Tauri (基于 Rust)
Tauri 是一个用于为所有主流桌面操作系统构建微小、极速二进制文件的框架。它复用了操作系统的原生 Webview，打包体积通常只有几 MB。

#### 初始化与集成步骤：
1. **环境准备**：
   - 安装 Node.js
   - 安装 Rust 编译环境 (Rustc, Cargo)
   - 安装 Windows 构建工具 (C++ build tools)

2. **在现有前端项目中集成 Tauri**：
   ```bash
   cd frontend
   npm install -D @tauri-apps/cli
   npx tauri init
   ```
   在初始化过程中，按如下配置：
   - App name: `LabManagement`
   - Window title: `安徽信息工程学院实验室管理平台`
   - Web frontend path: `../dist` (指向 vite build 的产物)
   - Dev server url: `http://localhost:5173` (指向 vite dev 的地址)
   - Frontend dev cmd: `npm run dev`
   - Frontend build cmd: `npm run build`

3. **开发与调试**：
   ```bash
   npx tauri dev
   ```
   这会同时启动 Vite 的开发服务器和 Tauri 的桌面端窗口，支持热更新。

4. **构建与发布**：
   ```bash
   npx tauri build
   ```
   构建完成后，会在 `src-tauri/target/release/bundle` 目录下生成对应的 `.msi` 或 `.exe` 安装包。

### 1.2 备选方案：Electron
如果项目中未来需要大量调用 Node.js 的底层系统 API（如极深度的文件系统操作或原生硬件交互），可以使用 Electron。
- 安装：`npm install -D electron electron-builder vite-plugin-electron`
- 配置：修改 `vite.config.js` 引入 electron 插件，编写 `main.js` 作为主进程入口。

---

## 2. 安卓端开发方案 (Android)

将现有的 Web 前端转换为 Android 应用，最平滑、改动最小的方案是使用 **Capacitor**。如果需要极致的原生性能并打算重写 UI，则可以选择 React Native 或 Flutter。考虑到快速复用现有 Element Plus + Vue 3 代码，**强烈推荐 Capacitor**。

### 2.1 推荐方案：Capacitor (由 Ionic 团队维护)
Capacitor 允许你将现有的 Web 应用（如本项目的 Vue 产物）直接嵌入到一个原生的 Android WebView 中，并提供调用手机原生硬件（摄像头、通知、地理位置等）的 JS API。

#### 初始化与集成步骤：
1. **环境准备**：
   - 安装 Node.js
   - 安装 Android Studio 并配置好 Android SDK
   - 配置环境变量 `ANDROID_HOME`

2. **在现有前端项目中集成 Capacitor**：
   ```bash
   cd frontend
   npm install @capacitor/core
   npm install -D @capacitor/cli
   npx cap init
   ```
   在初始化过程中：
   - App name: `实验室管理平台`
   - App Package ID: `com.aiit.labmanagement`
   - Web asset directory: `dist`

3. **添加 Android 平台支持**：
   ```bash
   npm install @capacitor/android
   npx cap add android
   ```

4. **同步代码与调试**：
   - 先执行 Web 端的打包：`npm run build`
   - 将打包好的代码同步到 Android 项目中：`npx cap sync`
   - 打开 Android Studio 进行调试或打包：`npx cap open android`

### 2.2 安卓端适配注意事项：
现有的前端 UI 主要针对桌面端大屏设计（特别是 Sidebar 侧边栏和 Element Plus 的大表格）。如果要打包成 Android App，需要在 Vue 项目中进行如下适配：
1. **响应式布局 (Responsive Design)**：
   - 使用 CSS Media Queries (`@media (max-width: 768px)`) 将左右侧边栏布局改为底部导航栏 (Bottom Tab Bar) 或抽屉式侧边栏 (Drawer)。
   - 表格组件在手机上体验较差，建议在移动端尺寸下，将 `el-table` 切换为卡片列表 (`el-card` list) 展示。
2. **安全区域 (Safe Area)**：
   - 适配刘海屏和状态栏，在 CSS 中使用 `env(safe-area-inset-top)` 等属性。
3. **网络请求**：
   - Android App 中无法使用相对路径进行 API 请求。需确保 Axios 的 `baseURL` 配置为后端的绝对路径（如 `https://api.lab.aiit.edu.cn`），并处理好跨域或 SSL 证书问题。

---

## 3. 多端开发 API 与环境配置管理

为了在同一套 Vue 3 代码中兼容 Web、Desktop、Android 三端，建议使用环境变量进行区分：

在 `frontend/.env` 系列文件中：
```env
# .env.web
VITE_APP_PLATFORM=web
VITE_API_BASE_URL=/api

# .env.desktop
VITE_APP_PLATFORM=desktop
VITE_API_BASE_URL=https://api.yoursite.com

# .env.android
VITE_APP_PLATFORM=android
VITE_API_BASE_URL=https://api.yoursite.com
```

在代码中通过 `import.meta.env.VITE_APP_PLATFORM` 来判断当前运行环境，从而渲染不同的 UI 组件或调用不同的原生 API（例如在 Desktop 环境下调用 Tauri API，在 Android 环境下调用 Capacitor API）。

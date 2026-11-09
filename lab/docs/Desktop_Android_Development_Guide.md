# 桌面端与安卓端打包指南

本文档说明如何把 `lab/frontend` 打包成 **桌面端（Windows/macOS/Linux）** 与 **安卓端 APK**。

两套原生壳已在本仓库落地并验证通过：

| 端 | 方案 | 产物 |
| --- | --- | --- |
| 桌面端 | [Tauri 2](https://tauri.app)（Rust + 系统 WebView） | `src-tauri/target/release/bundle/` 下的 NSIS 安装包与 MSI |
| 安卓端 | [Capacitor 8](https://capacitorjs.com)（原生 WebView） | `android/app/build/outputs/apk/` 下的 APK |

选型理由：Tauri 复用系统 WebView，安装包只有几 MB；Capacitor 能直接把现有 Vue 产物装进原生 WebView，
Element Plus + Vue 3 的代码一行不用改。

---

## 1. 前置环境

| 工具 | 版本要求 | 用途 |
| --- | --- | --- |
| Node.js | 20+（本机 24） | 前端构建与 CLI |
| Rust | 1.82+（本机 1.94） | 编译 Tauri |
| Visual Studio Build Tools | 含「使用 C++ 的桌面开发」 | Tauri 在 Windows 上的链接器 |
| JDK | 21+（本机 21） | Capacitor 的 Gradle 插件 |
| Android SDK | Platform 36 + Build-Tools 35+ | 编译 APK |

环境变量：

```bash
# Windows
setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"
setx PATH "%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin"
```

Gradle 由工程自带的 `gradlew` 自动下载，无需单独安装。

---

## 2. 目录结构

```
lab/frontend/
├─ capacitor.config.json     Capacitor 配置（appId、webDir、androidScheme）
├─ scripts/
│  └─ build-native.mjs       原生端打包脚本：注入 API 地址并串联构建
├─ src-tauri/                Tauri 工程
│  ├─ Cargo.toml
│  ├─ tauri.conf.json        窗口、图标、打包目标
│  ├─ capabilities/          权限声明
│  └─ icons/                 由 `npx tauri icon` 从品牌 SVG 生成
└─ android/                  Capacitor 生成的安卓工程（需入库）
   ├─ app/src/main/assets/    cap sync 生成的 Web 产物（已忽略）
   └─ gradlew                 Gradle 包装器
```

---

## 3. 打包脚本

`scripts/build-native.mjs` 负责把「前端构建 → 原生打包」串起来，核心是**注入后端绝对地址**：

```bash
# 桌面端
npm run build:desktop
npm run build:desktop -- --api=http://192.168.1.10:8081

# 安卓端
npm run build:android
npm run build:android -- --api=http://192.168.1.10:8081
npm run build:android:release          # 出签名包
```

后端地址的优先级：

1. `--api=` 参数
2. 环境变量 `VITE_API_BASE_URL`
3. 自动探测本机局域网 IPv4 + `8081` 端口

> **为什么必须用绝对地址**：Web 端走 Vite 开发代理，接口地址是相对路径 `/api`；
> 原生壳的 WebView 里没有代理，相对路径会打到 `tauri://localhost/api` 或 `https://localhost/api`，
> 必然 404。脚本会把地址写进 `VITE_API_BASE_URL`，前端 `utils/request.js` 与 `utils/file.js` 直接读取它。

---

## 4. 桌面端（Tauri）

### 4.1 开发调试

```bash
npm run tauri dev
```

会先起 Vite（`http://localhost:3000`），再拉起桌面窗口并热更新。

### 4.2 打包

```bash
npm run build:desktop
```

产物：

```
src-tauri/target/release/bundle/
├─ nsis/LabLink_1.0.0_x64-setup.exe     # Windows 安装包（当前用户模式，含简体中文）
└─ msi/LabLink_1.0.0_x64_en-US.msi       # MSI 安装包
```

### 4.3 换图标

图标源是 `public/favicon.svg`（品牌蓝圆角方块 + 白色菱形斜线）：

```bash
npx tauri icon public/favicon.svg
```

会同时刷新 `src-tauri/icons/` 下的全尺寸图标与 Windows `.ico`。

### 4.4 改了 Rust 依赖后

```bash
cd src-tauri && cargo build --release
```

首次编译约 4–5 分钟（拉取并编译 crates），之后是增量。

---

## 5. 安卓端（Capacitor）

### 5.1 同步 Web 产物

```bash
npm run cap:sync
```

等价于 `npx cap sync android`：把 `dist/` 拷到 `android/app/src/main/assets/public/`，
并刷新原生插件配置。

### 5.2 打 APK

```bash
npm run build:android            # debug 包，可用 debug 签名直接安装
npm run build:android:release    # release 包，需要正式签名
```

产物：

```
android/app/build/outputs/apk/debug/app-debug.apk
android/app/build/outputs/apk/release/app-release.apk
```

安装到设备：

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### 5.3 正式签名

签名密钥**不入库**（已在 `.gitignore` 里排除 `*.keystore` / `*.jks`）。首次生成：

```bash
keytool -genkeypair -v \
  -keystore lablink-release.keystore \
  -alias lablink \
  -keyalg RSA -keysize 2048 -validity 10000
```

然后在 `android/gradle.properties` 里配置（该文件不入库）：

```properties
LABLINK_STORE_FILE=lablink-release.keystore
LABLINK_STORE_PASSWORD=***
LABLINK_KEY_ALIAS=lablink
LABLINK_KEY_PASSWORD=***
```

并在 `android/app/build.gradle` 的 `signingConfigs` 里引用这些变量，
`release` 构建类型使用 `signingConfigs.release`。

> 密钥一旦丢失就无法为同一个应用 ID 发布更新，务必离线备份。

### 5.4 模拟器访问本机后端

安卓模拟器访问宿主机用 `10.0.2.2`：

```bash
npm run build:android -- --api=http://10.0.2.2:8081
```

真机与模拟器不同网段时，用电脑的局域网 IP，并在手机上手动配置代理或保证同网段直连。

---

## 6. 跨域：原生端的 Origin 是什么

原生 WebView 发出的请求带自己的 `Origin`，与 Web 端不同：

| 端 | Origin |
| --- | --- |
| Web（开发） | `http://127.0.0.1:5193` 之类的开发端口 |
| Tauri 桌面端 | `tauri://localhost` |
| Capacitor 安卓端 | `https://localhost`（`capacitor.config.json` 里 `androidScheme: https`） |

后端在**非生产环境**下已放行这些来源（见 `AppSecurityProperties.resolveAllowedOriginPatterns()`）：
`http(s)://localhost[:任意端口]`、`http(s)://127.0.0.1[:任意端口]`、`tauri://localhost`、`capacitor://localhost`。

**生产环境不放宽任何一条**，必须通过 `APP_SECURITY_ALLOWED_ORIGINS_CSV` 显式声明实际来源，
详见 [DEPLOYMENT-SECURITY.md](./DEPLOYMENT-SECURITY.md)。

---

## 7. 路由模式

原生壳里没有服务端路由回退，因此原生端使用 **hash 路由**：

```js
// router/index.js
history: isNativeApp() ? createWebHashHistory() : createWebHistory()
```

平台判定见 `utils/portal.js` 的 `isNativeApp()`：识别 Capacitor 桥接对象，或 `tauri:` / `capacitor:` 协议。

Web 端保持 history 模式，URL 不受影响。

---

## 8. 常见问题

**Q：打包后接口全部 401 / 请求失败**

A：多半是后端地址没注入或连不上。先看产物里的地址是否正确：
在原生端打开控制台（安卓端需 `webContentsDebuggingEnabled`，已默认开启），
或在 `scripts/build-native.mjs` 里显式传 `--api=`。另外确认手机能访问到那台后端。

**Q：安卓端提示 `Invalid CORS request`**

A：后端的白名单没放行原生端来源。非生产环境会自动放行；
生产环境要把 `https://localhost` 等实际来源写进 `APP_SECURITY_ALLOWED_ORIGINS_CSV`。

**Q：Gradle 报「无效的目标发行版: 21」**

A：Gradle 用了 JDK 17。`build-native.mjs` 会自动找 JDK 21 并覆盖 `JAVA_HOME`；
若本机没装 JDK 21，请手动设置 `JAVA_HOME` 后重试。

**Q：桌面端窗口是白屏**

A：先确认 `npm run build` 产出了 `dist/`；Tauri 的 `frontendDist` 指向 `../dist`，
目录不存在时构建会失败。另外检查 `vite.config.mjs` 的 base 是否为 `/`。

**Q：安卓端应用图标没变**

A：图标在 `android/app/src/main/res/mipmap-*/` 下，重新复制一遍：

```bash
for d in mipmap-hdpi mipmap-mdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi mipmap-anydpi-v26; do
  cp -f src-tauri/icons/android/$d/* android/app/src/main/res/$d/
done
```

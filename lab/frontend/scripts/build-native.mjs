/**
 * 原生端打包脚本
 *
 * Web 端走 Vite 代理，接口地址是相对路径 `/api`；
 * 桌面端（Tauri）与安卓端（Capacitor）的 WebView 没有代理，
 * 必须拿到后端的绝对地址，所以这里统一在打包时注入 VITE_API_BASE_URL。
 *
 * 用法：
 *   node scripts/build-native.mjs desktop
 *   node scripts/build-native.mjs android
 *   node scripts/build-native.mjs android --api=http://192.168.1.10:8081
 *   node scripts/build-native.mjs android --release
 *
 * 地址优先级：--api 参数 > 环境变量 VITE_API_BASE_URL > 本机局域网 IP + 后端端口。
 */

import { spawn, spawnSync } from 'node:child_process'
import { networkInterfaces } from 'node:os'
import { existsSync, readdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const DEFAULT_BACKEND_PORT = 8081

/**
 * 列出可用的局域网 IPv4 地址，附带排序分值。
 *
 * 只取 RFC1918 私有地址（10/8、172.16/12、192.168/16），
 * 并排除保留网段（198.18/15 是基准测试用、169.254/16 是链路本地）；
 * 虚拟网卡（WSL、VPN 隧道、虚拟交换机的网卡）排在物理网卡之后。
 */
function listLanAddresses() {
  const interfaces = networkInterfaces()
  const reserved = [/^198\.18\./, /^169\.254\./, /^127\./]
  const virtualName = /virtual|vmware|vbox|hyper-?v|vethernet|tun|tap|wsl|loopback|meta/i
  const results = []

  for (const name of Object.keys(interfaces)) {
    for (const item of interfaces[name] || []) {
      const family = typeof item.family === 'string' ? item.family : `IPv${item.family}`
      if (family !== 'IPv4' || item.internal) {
        continue
      }
      const address = item.address
      if (reserved.some((pattern) => pattern.test(address))) {
        continue
      }
      const isPrivate = /^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/.test(address)
      let score = isPrivate ? 100 : 50
      if (virtualName.test(name)) {
        score -= 60
      }
      results.push({ name, address, score })
    }
  }

  return results.sort((a, b) => b.score - a.score)
}

/** 取最适合真机访问的局域网地址。 */
function detectLanAddress() {
  const candidates = listLanAddresses()
  return candidates.length ? candidates[0].address : null
}

function parseArgs(argv) {
  const [platform, ...rest] = argv
  const options = { platform, api: '', release: false, skipWebBuild: false }
  for (const arg of rest) {
    if (arg.startsWith('--api=')) {
      options.api = arg.slice('--api='.length).trim()
    } else if (arg === '--release') {
      options.release = true
    } else if (arg === '--skip-web-build') {
      options.skipWebBuild = true
    }
  }
  return options
}

function resolveApiBaseUrl(explicit) {
  if (explicit) {
    return explicit.replace(/\/+$/, '')
  }
  if (process.env.VITE_API_BASE_URL) {
    return process.env.VITE_API_BASE_URL.replace(/\/+$/, '')
  }
  const lan = detectLanAddress()
  if (lan) {
    console.log('[native] 候选地址：')
    for (const item of listLanAddresses()) {
      console.log(`[native]   ${item.address}  (${item.name})`)
    }
    return `http://${lan}:${DEFAULT_BACKEND_PORT}`
  }
  // 安卓模拟器访问宿主机用 10.0.2.2
  return `http://10.0.2.2:${DEFAULT_BACKEND_PORT}`
}

/** 依次执行命令，失败即中断。 */
function run(command, args, env) {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(command, args, {
      cwd: ROOT,
      env: { ...process.env, ...env },
      stdio: 'inherit',
      shell: process.platform === 'win32'
    })
    child.on('error', rejectPromise)
    child.on('close', (code) => {
      if (code === 0) {
        resolvePromise()
      } else {
        rejectPromise(new Error(`${command} ${args.join(' ')} 退出码 ${code}`))
      }
    })
  })
}

async function main() {
  const options = parseArgs(process.argv.slice(2))
  const platform = options.platform

  if (!['desktop', 'android'].includes(platform)) {
    console.error('用法：node scripts/build-native.mjs <desktop|android> [--api=地址] [--release]')
    process.exit(1)
  }

  const apiBaseUrl = resolveApiBaseUrl(options.api)
  const env = { VITE_API_BASE_URL: apiBaseUrl }

  console.log(`[native] 目标平台：${platform}`)
  console.log(`[native] 后端地址：${apiBaseUrl}`)

  // 1. 打包 Web 产物（桌面端由 tauri 的 beforeBuildCommand 再打一次，这里先打给安卓端用）
  if (!options.skipWebBuild) {
    await run('npx', ['vite', 'build'], env)
  }

  if (platform === 'desktop') {
    // 2. 桌面端：Tauri 会复用上面的环境变量，beforeBuildCommand 重新打产物时也会带上
    await run('npx', ['tauri', 'build'], env)
    console.log('[native] 桌面端产物：src-tauri/target/release/bundle/')
    return
  }

  // 3. 安卓端：同步 Web 产物后交给 Gradle 打包
  await run('npx', ['cap', 'sync', 'android'], env)

  const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
  const task = options.release ? 'assembleRelease' : 'assembleDebug'
  const gradleDir = resolve(ROOT, 'android')
  const gradleEnv = {}

  // Capacitor 8 的 Gradle 插件要求 JDK 21，JAVA_HOME 指向旧版本时自动找一个可用的
  const jdkHome = resolveJdk21()
  if (jdkHome) {
    gradleEnv.JAVA_HOME = jdkHome
    console.log(`[native] JAVA_HOME=${jdkHome}`)
  } else {
    console.warn('[native] 未找到 JDK 21，若 Gradle 报错请设置 JAVA_HOME 后重试')
  }

  await new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(gradlew, [`${task}`, '--no-daemon'], {
      cwd: gradleDir,
      env: { ...process.env, ...gradleEnv },
      stdio: 'inherit',
      shell: process.platform === 'win32'
    })
    child.on('error', rejectPromise)
    child.on('close', (code) => {
      if (code === 0) {
        resolvePromise()
      } else {
        rejectPromise(new Error(`gradle ${task} 退出码 ${code}`))
      }
    })
  })

  const apkDir = resolve(
    gradleDir,
    'app/build/outputs/apk',
    options.release ? 'release' : 'debug'
  )
  console.log(`[native] 安卓端产物：${apkDir}`)
}

/** 读取某个 JDK 目录的主版本号。 */
function readJavaMajor(javaHome) {
  try {
    // java -version 把版本信息打到 stderr，两路都要收
    const result = spawnSync(resolve(javaHome, 'bin', 'java'), ['-version'], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    })
    const output = `${result.stdout || ''}\n${result.stderr || ''}`
    const match = output.match(/version "(\d+)/)
    return match ? Number(match[1]) : null
  } catch (error) {
    return null
  }
}

/**
 * 找一个 JDK 21 及以上的安装目录。
 *
 * 依次尝试 JAVA_HOME、PATH 上的 java、常见的安装位置与 ~/.jdks。
 */
function resolveJdk21() {
  const candidates = []

  if (process.env.JAVA_HOME) {
    candidates.push(process.env.JAVA_HOME)
  }

  if (process.platform === 'win32') {
    const roots = [
      'C:/Program Files/Java',
      'C:/Program Files/Eclipse Adoptium',
      'C:/Program Files/Microsoft',
      'C:/Program Files/Zulu',
      'C:/Program Files/BellSoft'
    ]
    for (const root of roots) {
      if (!existsSync(root)) {
        continue
      }
      for (const name of readdirSync(root)) {
        if (/jdk|java|temurin|zulu|corretto/i.test(name)) {
          candidates.push(resolve(root, name))
        }
      }
    }
  }

  const userJdks = resolve(process.env.USERPROFILE || process.env.HOME || '', '.jdks')
  if (existsSync(userJdks)) {
    for (const name of readdirSync(userJdks)) {
      candidates.push(resolve(userJdks, name))
    }
  }

  for (const home of candidates) {
    if (!home || !existsSync(resolve(home, 'bin', 'java.exe'))) {
      continue
    }
    const major = readJavaMajor(home)
    if (major && major >= 21) {
      return home
    }
  }

  return null
}

main().catch((error) => {
  console.error('[native] 打包失败：', error.message)
  process.exit(1)
})

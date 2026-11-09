import { defineStore } from 'pinia'

const STORAGE_KEY = 'lab_settings'

/**
 * 主题模式：浅色 / 深色 / 跟随系统。
 */
export const THEME_MODES = {
  LIGHT: 'light',
  DARK: 'dark',
  AUTO: 'auto'
}

/**
 * 界面密度：默认 / 紧凑。紧凑模式收紧表格行高、控件高度与间距。
 */
export const DENSITIES = {
  DEFAULT: 'default',
  COMPACT: 'compact'
}

/**
 * 主色预设，参考 Arco Design / Ant Design 的色板思路，
 * 统一为「主色 + 成功 + 警告 + 危险 + 信息」五色一组。
 *
 * 主色与平台的品牌蓝同族，切换预设只改强调色（选中态、链接、图标底），
 * 不会动品牌本身。
 *
 * darkPrimary 是同一主色在暗色主题下的取值：暗色底上必须用亮色，
 * 否则强调色会压暗到几乎看不见，因此每个预设都成对给出明暗两套。
 */
export const THEME_PRESETS = [
  {
    id: 'blue',
    name: '品牌蓝',
    description: '与平台主色一致，最稳妥',
    colors: {
      primary: '#2563eb',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#8b5cf6'
    },
    darkPrimary: '#60a5fa',
    darkPrimaryStrong: '#93c5fd'
  },
  {
    id: 'indigo',
    name: '靛蓝',
    description: '冷静专业，突出层级',
    colors: {
      primary: '#4f46e5',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#0ea5e9'
    },
    darkPrimary: '#818cf8',
    darkPrimaryStrong: '#a5b4fc'
  },
  {
    id: 'cyan',
    name: '青蓝',
    description: '清新沉稳，适合教学场景',
    colors: {
      primary: '#0891b2',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#2563eb'
    },
    darkPrimary: '#22d3ee',
    darkPrimaryStrong: '#67e8f9'
  },
  {
    id: 'violet',
    name: '紫罗兰',
    description: '偏科研质感，区分度更高',
    colors: {
      primary: '#7c3aed',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#d946ef'
    },
    darkPrimary: '#a78bfa',
    darkPrimaryStrong: '#c4b5fd'
  }
]

const DEFAULTS = {
  themeMode: THEME_MODES.AUTO,
  density: DENSITIES.DEFAULT,
  accent: THEME_PRESETS[0].id,
  customAccent: '',
  sidebarCollapsed: false,
  showTagsView: true
}

function readStored() {
  if (typeof window === 'undefined') {
    return { ...DEFAULTS }
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return { ...DEFAULTS }
    }
    const stored = { ...DEFAULTS, ...JSON.parse(raw) }
    // 预设改版后旧的 id 会失效，这里回落到默认预设，避免界面里没有选中项
    if (!THEME_PRESETS.some((item) => item.id === stored.accent)) {
      stored.accent = DEFAULTS.accent
    }
    return stored
  } catch (error) {
    return { ...DEFAULTS }
  }
}

function persist(state) {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        themeMode: state.themeMode,
        density: state.density,
        accent: state.accent,
        customAccent: state.customAccent,
        sidebarCollapsed: state.sidebarCollapsed,
        showTagsView: state.showTagsView
      })
    )
  } catch (error) {
    // 存储不可用时只在当前会话生效
  }
}

/**
 * 把颜色按比例向白色混合。
 *
 * 自定义主色只有一个色值，暗色主题需要一版更亮的等价色，
 * 这里用混白的方式近似，避免再让用户手填一次颜色。
 */
function mixWithWhite(hex, ratio) {
  const value = String(hex).replace('#', '').trim()
  const full = value.length === 3 ? value.split('').map((c) => c + c).join('') : value
  if (!/^[0-9a-fA-F]{6}$/.test(full)) {
    return hex
  }
  const num = parseInt(full, 16)
  const mix = (channel) => Math.round(channel + (255 - channel) * ratio)
  const parts = [mix((num >> 16) & 255), mix((num >> 8) & 255), mix(num & 255)]
  return `#${parts.map((c) => c.toString(16).padStart(2, '0')).join('')}`
}

/** 把主色写成 CSS 变量，页面只认令牌不认具体色值。 */
function applyAccent(state, dark) {
  if (typeof document === 'undefined') {
    return
  }
  const preset = THEME_PRESETS.find((item) => item.id === state.accent) || THEME_PRESETS[0]
  const root = document.documentElement

  let accent = state.customAccent || preset.colors.primary
  let accentStrong = accent

  if (dark) {
    if (state.customAccent) {
      accent = mixWithWhite(state.customAccent, 0.45)
      accentStrong = mixWithWhite(state.customAccent, 0.62)
    } else {
      accent = preset.darkPrimary
      accentStrong = preset.darkPrimaryStrong
    }
  }

  root.style.setProperty('--ll-accent-600', accent)
  root.style.setProperty('--ll-accent-700', accentStrong)
  root.style.setProperty('--el-color-primary', accent)
  root.style.setProperty('--el-color-primary-dark-2', accentStrong)
}

export const useSettingsStore = defineStore('settings', {
  state: () => readStored(),

  getters: {
    /**
     * 紧凑模式判定。
     *
     * 注意：getter 不能与 state 同名，Pinia 中同名的 getter 会遮蔽状态项，
     * 这里只保留派生属性，原始设置项直接从 state 读取。
     */
    isCompact: (state) => state.density === DENSITIES.COMPACT,
    accentPreset: (state) =>
      THEME_PRESETS.find((item) => item.id === state.accent) || THEME_PRESETS[0],
    resolvedAccent: (state) =>
      state.customAccent ||
      (THEME_PRESETS.find((item) => item.id === state.accent) || THEME_PRESETS[0]).colors.primary
  },

  actions: {
    /** 应用到 DOM：主题模式、密度、主色。 */
    applyToDocument() {
      if (typeof document === 'undefined') {
        return
      }
      const root = document.documentElement

      root.setAttribute('data-density', this.density)
      root.classList.toggle('is-compact', this.isCompact)

      const prefersDark =
        typeof window !== 'undefined' &&
        window.matchMedia?.('(prefers-color-scheme: dark)')?.matches

      const dark = this.themeMode === THEME_MODES.DARK ||
        (this.themeMode === THEME_MODES.AUTO && prefersDark)

      if (dark) {
        root.setAttribute('data-theme', 'dark')
      } else {
        root.removeAttribute('data-theme')
      }

      applyAccent(this, dark)
    },

    setThemeMode(mode) {
      this.themeMode = mode
      persist(this.$state)
      this.applyToDocument()
    },

    setDensity(density) {
      this.density = density
      persist(this.$state)
      this.applyToDocument()
    },

    setAccent(accent) {
      this.accent = accent
      this.customAccent = ''
      persist(this.$state)
      this.applyToDocument()
    },

    setCustomAccent(color) {
      this.customAccent = color || ''
      persist(this.$state)
      this.applyToDocument()
    },

    setSidebarCollapsed(collapsed) {
      this.sidebarCollapsed = collapsed
      persist(this.$state)
    },

    setShowTagsView(show) {
      this.showTagsView = show
      persist(this.$state)
    }
  }
})

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
 * darkPrimary 是同一主色在暗色主题下的取值：暗色底上必须用亮色，
 * 否则强调色会压暗到几乎看不见，因此每个预设都成对给出明暗两套。
 */
export const THEME_PRESETS = [
  {
    id: 'graphite',
    name: '石墨',
    description: '中性克制，适合长时间使用',
    colors: {
      primary: '#18181b',
      success: '#059669',
      warning: '#d97706',
      danger: '#e11d48',
      info: '#52525b'
    },
    darkPrimary: '#d4d4d8',
    darkPrimaryStrong: '#fafafa'
  },
  {
    id: 'indigo',
    name: '靛蓝',
    description: '冷静专业，突出层级',
    colors: {
      primary: '#4f46e5',
      success: '#059669',
      warning: '#d97706',
      danger: '#e11d48',
      info: '#0284c7'
    },
    darkPrimary: '#a5b4fc',
    darkPrimaryStrong: '#c7d2fe'
  },
  {
    id: 'teal',
    name: '青碧',
    description: '清新沉稳，适合教学场景',
    colors: {
      primary: '#0f766e',
      success: '#16a34a',
      warning: '#d97706',
      danger: '#e11d48',
      info: '#0891b2'
    },
    darkPrimary: '#5eead4',
    darkPrimaryStrong: '#99f6e4'
  },
  {
    id: 'slate',
    name: '石板',
    description: '低饱和，久看不累',
    colors: {
      primary: '#334155',
      success: '#0f766e',
      warning: '#b45309',
      danger: '#be123c',
      info: '#0369a1'
    },
    darkPrimary: '#cbd5e1',
    darkPrimaryStrong: '#e2e8f0'
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
    return { ...DEFAULTS, ...JSON.parse(raw) }
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

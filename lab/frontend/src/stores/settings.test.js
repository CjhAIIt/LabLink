import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { DENSITIES, THEME_MODES, THEME_PRESETS, useSettingsStore } from './settings'

function createStore() {
  setActivePinia(createPinia())
  return useSettingsStore()
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('data-theme')
  document.documentElement.removeAttribute('data-density')
})

describe('settings store', () => {
  it('默认使用跟随系统主题与默认密度', () => {
    const store = createStore()

    expect(store.themeMode).toBe(THEME_MODES.AUTO)
    expect(store.density).toBe(DENSITIES.DEFAULT)
    expect(store.isCompact).toBe(false)
  })

  it('切换主题模式会写入本地存储并落到根元素', () => {
    const store = createStore()

    store.setThemeMode(THEME_MODES.DARK)

    expect(store.themeMode).toBe(THEME_MODES.DARK)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(JSON.parse(localStorage.getItem('lab_settings')).themeMode).toBe(THEME_MODES.DARK)
  })

  it('切换密度会写入本地存储并落到根元素', () => {
    const store = createStore()

    store.setDensity(DENSITIES.COMPACT)

    expect(store.isCompact).toBe(true)
    expect(document.documentElement.getAttribute('data-density')).toBe(DENSITIES.COMPACT)
    expect(document.documentElement.classList.contains('is-compact')).toBe(true)
  })

  it('选择主色预设会覆盖强调色变量', () => {
    const store = createStore()
    const preset = THEME_PRESETS.find((item) => item.id === 'teal')

    store.setAccent('teal')

    expect(store.accent).toBe('teal')
    expect(store.resolvedAccent).toBe(preset.colors.primary)
    expect(document.documentElement.style.getPropertyValue('--ll-accent-600')).toBe(preset.colors.primary)
  })

  it('自定义主色优先于预设', () => {
    const store = createStore()

    store.setAccent('teal')
    store.setCustomAccent('#123456')

    expect(store.resolvedAccent).toBe('#123456')
    expect(document.documentElement.style.getPropertyValue('--ll-accent-600')).toBe('#123456')
  })

  it('重新选择预设会清空自定义主色', () => {
    const store = createStore()

    store.setCustomAccent('#123456')
    store.setAccent('indigo')

    expect(store.customAccent).toBe('')
    expect(store.resolvedAccent).toBe(
      THEME_PRESETS.find((item) => item.id === 'indigo').colors.primary
    )
  })

  it('暗色主题下强调色换成预设的亮色版本', () => {
    const store = createStore()
    const preset = THEME_PRESETS.find((item) => item.id === 'teal')

    store.setAccent('teal')
    store.setThemeMode(THEME_MODES.DARK)

    expect(document.documentElement.style.getPropertyValue('--ll-accent-600')).toBe(
      preset.darkPrimary
    )
  })

  it('暗色主题下自定义主色会被提亮，保证在深色底上可读', () => {
    const store = createStore()

    store.setThemeMode(THEME_MODES.DARK)
    store.setCustomAccent('#123456')

    const applied = document.documentElement.style.getPropertyValue('--ll-accent-600')
    expect(applied).not.toBe('#123456')
    expect(applied).toMatch(/^#[0-9a-f]{6}$/)
  })

  it('侧边栏折叠与页签开关都会被持久化', () => {
    const store = createStore()

    store.setSidebarCollapsed(true)
    store.setShowTagsView(false)

    const stored = JSON.parse(localStorage.getItem('lab_settings'))
    expect(stored.sidebarCollapsed).toBe(true)
    expect(stored.showTagsView).toBe(false)
  })

  it('读取已保存的设置', () => {
    localStorage.setItem(
      'lab_settings',
      JSON.stringify({ themeMode: 'dark', density: 'compact', accent: 'slate', customAccent: '' })
    )

    const store = createStore()

    expect(store.themeMode).toBe('dark')
    expect(store.density).toBe('compact')
    expect(store.accent).toBe('slate')
  })
})

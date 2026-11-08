import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { THEME_MODES } from '@/stores/settings'

/**
 * 主题状态统一放在 settings store，useTheme 只做转发，
 * 因此用例需要真实的 Pinia 实例。
 */
function loadUseTheme() {
  setActivePinia(createPinia())
  return import('./useTheme')
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('data-theme')
  document.documentElement.removeAttribute('data-density')
  vi.unstubAllGlobals()
})

describe('useTheme', () => {
  it('auto 模式下跟随系统偏好', async () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query.includes('dark'),
      media: query
    }))

    const { useTheme } = await loadUseTheme()
    const { isDark } = useTheme()

    expect(isDark.value).toBe(true)
  })

  it('auto 模式下系统为亮色时判定为亮色', async () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: false,
      media: query
    }))

    const { useTheme } = await loadUseTheme()
    const { isDark } = useTheme()

    expect(isDark.value).toBe(false)
  })

  it('显式选择深色后不受系统偏好影响', async () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: false,
      media: query
    }))

    const { useTheme } = await loadUseTheme()
    const { isDark, setTheme } = useTheme()

    setTheme(THEME_MODES.DARK)

    expect(isDark.value).toBe(true)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('显式选择亮色会移除根元素上的暗色标记', async () => {
    const { useTheme } = await loadUseTheme()
    const { isDark, setTheme } = useTheme()

    setTheme(THEME_MODES.DARK)
    expect(document.documentElement.hasAttribute('data-theme')).toBe(true)

    setTheme(THEME_MODES.LIGHT)

    expect(isDark.value).toBe(false)
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false)
  })

  it('切换主题会写入本地存储', async () => {
    const { useTheme } = await loadUseTheme()
    const { toggleTheme } = useTheme()

    toggleTheme()

    const stored = JSON.parse(localStorage.getItem('lab_settings') || '{}')
    expect(stored.themeMode).toBe(THEME_MODES.DARK)
  })
})

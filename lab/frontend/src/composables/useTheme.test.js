import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

const STORAGE_KEY = 'lab_theme'

/**
 * useTheme 内部用模块级 ref 保存主题状态（全局主题需要共享），
 * 因此每个用例都要重新加载模块，避免用例之间互相污染。
 */
async function loadUseTheme() {
  vi.resetModules()
  const module = await import('./useTheme')
  return module.useTheme()
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.removeAttribute('data-theme')
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('useTheme', () => {
  it('默认跟随系统偏好', async () => {
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query.includes('dark'),
      media: query
    }))

    const { currentTheme } = await loadUseTheme()
    expect(currentTheme.value).toBe('dark')
  })

  it('本地已保存的主题优先于系统偏好', async () => {
    localStorage.setItem(STORAGE_KEY, 'light')
    vi.stubGlobal('matchMedia', (query) => ({
      matches: query.includes('dark'),
      media: query
    }))

    const { currentTheme } = await loadUseTheme()
    expect(currentTheme.value).toBe('light')
  })

  it('切换主题会写入本地存储并同步到根元素', async () => {
    const { isDark, toggleTheme } = await loadUseTheme()
    expect(isDark.value).toBe(false)

    toggleTheme()
    await nextTick()

    expect(isDark.value).toBe(true)
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('切回亮色时移除根元素属性', async () => {
    const { isDark, toggleTheme } = await loadUseTheme()
    toggleTheme()
    await nextTick()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')

    toggleTheme()
    await nextTick()

    expect(isDark.value).toBe(false)
    expect(localStorage.getItem(STORAGE_KEY)).toBe('light')
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false)
  })

  it('setTheme 忽略非法取值', async () => {
    const { currentTheme, setTheme } = await loadUseTheme()

    setTheme('rainbow')

    expect(currentTheme.value).not.toBe('rainbow')
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})

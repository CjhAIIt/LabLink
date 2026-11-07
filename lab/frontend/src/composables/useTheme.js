import { computed, ref, watch } from 'vue'

const STORAGE_KEY = 'lab_theme'
const THEMES = ['light', 'dark']

const currentTheme = ref(readStoredTheme())

function readStoredTheme() {
  if (typeof window === 'undefined') {
    return 'light'
  }
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored && THEMES.includes(stored)) {
      return stored
    }
  } catch (error) {
    // localStorage 不可用时退回系统偏好
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)')?.matches ? 'dark' : 'light'
}

function applyTheme(theme) {
  if (typeof document === 'undefined') {
    return
  }
  if (theme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

export function useTheme() {
  const isDark = computed(() => currentTheme.value === 'dark')

  const setTheme = (theme) => {
    if (!THEMES.includes(theme)) {
      return
    }
    currentTheme.value = theme
    try {
      localStorage.setItem(STORAGE_KEY, theme)
    } catch (error) {
      // 忽略存储失败，主题仍然在当前会话生效
    }
  }

  const toggleTheme = () => {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  watch(currentTheme, applyTheme, { immediate: true })

  return { currentTheme, isDark, setTheme, toggleTheme }
}

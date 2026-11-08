import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSettingsStore, THEME_MODES } from '@/stores/settings'

/**
 * 主题相关的对外接口。
 *
 * 实际的设置状态统一放在 settings store，这里只做转发，
 * 避免主题与设置面板出现两套状态。
 */
export function useTheme() {
  const settingsStore = useSettingsStore()
  const { themeMode } = storeToRefs(settingsStore)

  const isDark = computed(() => {
    if (themeMode.value === THEME_MODES.DARK) {
      return true
    }
    if (themeMode.value === THEME_MODES.LIGHT) {
      return false
    }
    if (typeof window === 'undefined') {
      return false
    }
    return Boolean(window.matchMedia?.('(prefers-color-scheme: dark)')?.matches)
  })

  const setTheme = (mode) => {
    settingsStore.setThemeMode(mode)
  }

  const toggleTheme = () => {
    setTheme(isDark.value ? THEME_MODES.LIGHT : THEME_MODES.DARK)
  }

  return { themeMode, isDark, setTheme, toggleTheme }
}

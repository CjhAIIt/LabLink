import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

export const LAB_BREAKPOINTS = Object.freeze({
  phone: 576,
  phoneWide: 768,
  tablet: 992
})

export function useViewport() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : LAB_BREAKPOINTS.tablet + 1)

  const syncWidth = () => {
    if (typeof window === 'undefined') {
      return
    }
    width.value = window.innerWidth
  }

  onMounted(() => {
    syncWidth()
    window.addEventListener('resize', syncWidth)
    window.addEventListener('orientationchange', syncWidth)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', syncWidth)
    window.removeEventListener('orientationchange', syncWidth)
  })

  const isPhone = computed(() => width.value <= LAB_BREAKPOINTS.phone)
  const isPhoneWide = computed(() => width.value <= LAB_BREAKPOINTS.phoneWide)
  const isTablet = computed(() => width.value > LAB_BREAKPOINTS.phoneWide && width.value <= LAB_BREAKPOINTS.tablet)
  const isMobile = computed(() => width.value <= LAB_BREAKPOINTS.phoneWide)
  const isCompact = computed(() => width.value <= LAB_BREAKPOINTS.tablet)
  const isDesktop = computed(() => width.value > LAB_BREAKPOINTS.tablet)

  return {
    width,
    isPhone,
    isPhoneWide,
    isTablet,
    isMobile,
    isCompact,
    isDesktop
  }
}

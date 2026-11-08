import { ref } from 'vue'

/**
 * 已访问路由的页签集合。
 *
 * 用模块级状态而不是 store：页签属于纯前端的临时导航状态，
 * 刷新后从当前路由重新播种即可，不需要持久化。
 */
const visitedRoutes = ref([])

/** 页签最多保留的数量，超出后淘汰最早的一个。 */
const MAX_TAGS = 12

function seed(route) {
  if (!route?.path) {
    return
  }
  if (visitedRoutes.value.some((item) => item.path === route.path)) {
    return
  }
  visitedRoutes.value = [...visitedRoutes.value, buildTag(route)].slice(-MAX_TAGS)
}

function buildTag(route) {
  return {
    path: route.path,
    fullPath: route.fullPath,
    title: route.meta?.title || route.name || route.path,
    affix: route.meta?.affix === true
  }
}

/**
 * 页签视图状态。
 */
export function useTagsView() {
  const addRoute = (route) => seed(route)

  const removeRoute = (path) => {
    visitedRoutes.value = visitedRoutes.value.filter((item) => item.path !== path)
  }

  const removeOthers = (path) => {
    visitedRoutes.value = visitedRoutes.value.filter(
      (item) => item.path === path || item.affix
    )
  }

  const removeAll = () => {
    visitedRoutes.value = visitedRoutes.value.filter((item) => item.affix)
  }

  const removeRight = (path) => {
    const index = visitedRoutes.value.findIndex((item) => item.path === path)
    if (index === -1) {
      return
    }
    visitedRoutes.value = visitedRoutes.value.filter(
      (item, itemIndex) => itemIndex <= index || item.affix
    )
  }

  const removeLeft = (path) => {
    const index = visitedRoutes.value.findIndex((item) => item.path === path)
    if (index === -1) {
      return
    }
    visitedRoutes.value = visitedRoutes.value.filter(
      (item, itemIndex) => itemIndex >= index || item.affix
    )
  }

  return {
    visitedRoutes,
    addRoute,
    removeRoute,
    removeOthers,
    removeAll,
    removeRight,
    removeLeft
  }
}

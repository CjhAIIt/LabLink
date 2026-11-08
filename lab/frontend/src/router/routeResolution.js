/**
 * 桌面端与移动端路由的互斥解析。
 *
 * 独立成模块是为了能脱离 Vue Router 单测这部分逻辑：
 * 「没有移动端对应页面时不能改写路径」这条规则一旦写错，
 * 用户访问无对应实现的页面就会被踢回登录页并丢失深链接。
 */

export const CATCH_ALL_ROUTE_PATH = '/:pathMatch(.*)*'

/**
 * 判断某个路径是否有真实路由（命中兜底路由不算）。
 *
 * @param {(path: string) => Array<{ path: string }>} resolve 路由解析函数
 */
export function createHasRoute(resolve) {
  return (path) => {
    const matched = resolve(path)
    return matched.length > 0 && !matched.some((record) => record.path === CATCH_ALL_ROUTE_PATH)
  }
}

/**
 * 把路径前缀换成对端前缀，只有对端确实存在该路由时才返回新路径。
 *
 * @returns {string | null} 新路径，没有对应路由时返回 null
 */
export function resolveCounterpartPath(path, fromPrefix, toPrefix, hasRoute) {
  if (!path.startsWith(fromPrefix)) {
    return null
  }
  const candidate = path.replace(fromPrefix, toPrefix)
  return hasRoute(candidate) ? candidate : null
}

/**
 * 按当前形态解析应当跳转到的对端路径。
 *
 * 移动端把 /student、/teacher 换成 /m/student、/m/teacher；
 * 桌面端反向。返回 null 表示该页面没有对端实现，保持原路径渲染。
 *
 * @param {string} path 当前路径
 * @param {{ isMobile: boolean, hasRoute: (path: string) => boolean }} options
 * @returns {string | null}
 */
export function resolvePortalTarget(path, { isMobile, hasRoute }) {
  const pairs = isMobile
    ? [
        ['/student', '/m/student'],
        ['/teacher', '/m/teacher'],
        ['/admin', '/m/admin']
      ]
    : [
        ['/m/student', '/student'],
        ['/m/teacher', '/teacher'],
        ['/m/admin', '/admin']
      ]

  for (const [fromPrefix, toPrefix] of pairs) {
    const target = resolveCounterpartPath(path, fromPrefix, toPrefix, hasRoute)
    if (target) {
      return target
    }
  }

  return null
}

import { describe, expect, it } from 'vitest'
import {
  CATCH_ALL_ROUTE_PATH,
  createHasRoute,
  resolveCounterpartPath,
  resolvePortalTarget
} from './routeResolution'

/**
 * 构造一个假的路由表：只有列出的路径有真实路由，
 * 其余路径命中兜底路由（相当于 vue-router 的 404 分支）。
 */
function createFakeRouter(existingPaths) {
  return (path) => {
    if (existingPaths.includes(path)) {
      return [{ path }]
    }
    return [{ path: CATCH_ALL_ROUTE_PATH }]
  }
}

const MOBILE_STUDENT_PATHS = ['/m/student/dashboard', '/m/student/labs', '/m/student/applications']
const DESKTOP_STUDENT_PATHS = ['/student/dashboard', '/student/labs', '/student/attendance']

describe('createHasRoute', () => {
  it('真实路由返回 true', () => {
    const hasRoute = createHasRoute(createFakeRouter(['/student/labs']))
    expect(hasRoute('/student/labs')).toBe(true)
  })

  it('命中兜底路由返回 false', () => {
    const hasRoute = createHasRoute(createFakeRouter(['/student/labs']))
    expect(hasRoute('/student/attendance')).toBe(false)
  })

  it('空匹配返回 false', () => {
    const hasRoute = createHasRoute(() => [])
    expect(hasRoute('/anything')).toBe(false)
  })
})

describe('resolveCounterpartPath', () => {
  const hasRoute = createHasRoute(createFakeRouter(['/m/student/labs']))

  it('前缀匹配时换成对端路径', () => {
    expect(resolveCounterpartPath('/student/labs', '/student', '/m/student', hasRoute)).toBe('/m/student/labs')
  })

  it('前缀不匹配时返回 null', () => {
    expect(resolveCounterpartPath('/teacher/dashboard', '/student', '/m/student', hasRoute)).toBeNull()
  })

  it('对端没有对应路由时返回 null', () => {
    // /student/attendance 在移动端没有实现，不能改写路径
    expect(resolveCounterpartPath('/student/attendance', '/student', '/m/student', hasRoute)).toBeNull()
  })
})

describe('resolvePortalTarget', () => {
  it('移动端把学生端路径改写到移动端实现', () => {
    const hasRoute = createHasRoute(createFakeRouter(MOBILE_STUDENT_PATHS))
    const target = resolvePortalTarget('/student/labs', { isMobile: true, hasRoute })
    expect(target).toBe('/m/student/labs')
  })

  it('移动端没有对应实现时保持原路径', () => {
    const hasRoute = createHasRoute(createFakeRouter(MOBILE_STUDENT_PATHS))
    // /student/attendance 在移动端没有实现
    const target = resolvePortalTarget('/student/attendance', { isMobile: true, hasRoute })
    expect(target).toBeNull()
  })

  it('桌面端把移动端路径改写回桌面端实现', () => {
    const hasRoute = createHasRoute(createFakeRouter(DESKTOP_STUDENT_PATHS))
    const target = resolvePortalTarget('/m/student/labs', { isMobile: false, hasRoute })
    expect(target).toBe('/student/labs')
  })

  it('桌面端没有对应实现时保持原路径', () => {
    const hasRoute = createHasRoute(createFakeRouter(DESKTOP_STUDENT_PATHS))
    // /m/student/applications 在桌面端没有实现
    const target = resolvePortalTarget('/m/student/applications', { isMobile: false, hasRoute })
    expect(target).toBeNull()
  })

  it('管理端路径不受形态影响', () => {
    const hasRoute = createHasRoute(createFakeRouter(['/admin/dashboard']))
    expect(resolvePortalTarget('/admin/dashboard', { isMobile: true, hasRoute })).toBeNull()
    expect(resolvePortalTarget('/admin/dashboard', { isMobile: false, hasRoute })).toBeNull()
  })
})

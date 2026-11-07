import { beforeEach, describe, expect, it, vi } from 'vitest'

const push = vi.fn()
const clearUserInfo = vi.fn()
const currentRoute = { value: { path: '/student/dashboard' } }

vi.mock('@/router', () => ({
  default: {
    push,
    currentRoute
  }
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ clearUserInfo })
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) }
}))

const axiosMock = {
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() }
  }
}

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => axiosMock)
  }
}))

describe('request 响应拦截器', () => {
  let onFulfilled
  let onRejected

  beforeEach(async () => {
    vi.resetModules()
    push.mockClear()
    clearUserInfo.mockClear()
    currentRoute.value = { path: '/student/dashboard' }
    localStorage.clear()

    await import('./request')

    // 第二个 use 注册的是响应拦截器
    ;[onFulfilled, onRejected] = axiosMock.interceptors.response.use.mock.calls[0]
  })

  it('业务码 401 时清空会话并跳登录页', async () => {
    await expect(onFulfilled({ data: { code: 401, message: '令牌已过期' } })).rejects.toThrow()

    expect(clearUserInfo).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('HTTP 401 时清空会话并跳登录页', async () => {
    const error = { response: { status: 401 }, request: {} }

    await expect(onRejected(error)).rejects.toBe(error)

    expect(clearUserInfo).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('已经在登录页时不重复跳转', async () => {
    currentRoute.value = { path: '/login' }

    await expect(onFulfilled({ data: { code: 401, message: '令牌已过期' } })).rejects.toThrow()

    expect(clearUserInfo).toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('非 401 的业务错误不跳转', async () => {
    await expect(onFulfilled({ data: { code: 500, message: '服务异常' } })).rejects.toThrow()

    expect(clearUserInfo).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('请求成功时不跳转', () => {
    const result = onFulfilled({ data: { code: 200, data: [] } })

    expect(result).toEqual({ code: 200, data: [] })
    expect(clearUserInfo).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('HTTP 500 不清空会话', async () => {
    const error = { response: { status: 500 }, request: {} }

    await expect(onRejected(error)).rejects.toBe(error)

    expect(clearUserInfo).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })
})

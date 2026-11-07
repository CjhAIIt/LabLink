import { afterEach, describe, expect, it } from 'vitest'
import { resolvePortalHome, resolvePortalRole } from './portal'

afterEach(() => {
  window.innerWidth = 1440
})

describe('resolvePortalRole', () => {
  it('管理员角色归为 admin', () => {
    expect(resolvePortalRole({ role: 'admin' })).toBe('admin')
    expect(resolvePortalRole({ role: 'super_admin' })).toBe('admin')
  })

  it('教师角色归为 teacher', () => {
    expect(resolvePortalRole({ role: 'teacher' })).toBe('teacher')
  })

  it('主要身份为教师时归为 teacher', () => {
    expect(resolvePortalRole({ role: 'student', primaryIdentity: 'teacher' })).toBe('teacher')
  })

  it('其余情况归为 student', () => {
    expect(resolvePortalRole({ role: 'student' })).toBe('student')
  })

  it('没有用户信息时返回空字符串', () => {
    expect(resolvePortalRole(null)).toBe('')
    expect(resolvePortalRole(undefined)).toBe('')
  })
})

describe('resolvePortalHome', () => {
  it('管理员进入管理工作台', () => {
    expect(resolvePortalHome('admin')).toBe('/admin/dashboard')
  })

  it('桌面视口下学生进入学生工作台', () => {
    window.innerWidth = 1440
    expect(resolvePortalHome('student')).toBe('/student/dashboard')
  })

  it('移动视口下学生进入移动端工作台', () => {
    window.innerWidth = 390
    expect(resolvePortalHome('student')).toBe('/m/student/dashboard')
  })

  it('移动视口下教师进入移动端工作台', () => {
    window.innerWidth = 390
    expect(resolvePortalHome('teacher')).toBe('/m/teacher/dashboard')
  })

  it('管理员不受视口宽度影响', () => {
    window.innerWidth = 390
    expect(resolvePortalHome('admin')).toBe('/admin/dashboard')
  })

  it('可以直接传入用户信息对象', () => {
    window.innerWidth = 1440
    expect(resolvePortalHome({ role: 'teacher' })).toBe('/teacher/dashboard')
  })
})

import { vi } from 'vitest'

// hoisted：在 vi.mock 工厂外共享被捕获的拦截器 handler
const mock = vi.hoisted(() => {
  const handlers = {
    request: { fulfilled: null, rejected: null },
    response: { fulfilled: null, rejected: null }
  }
  const instance = {
    interceptors: {
      request: {
        use: (fulfilled, rejected) => { handlers.request.fulfilled = fulfilled; handlers.request.rejected = rejected }
      },
      response: {
        use: (fulfilled, rejected) => { handlers.response.fulfilled = fulfilled; handlers.response.rejected = rejected }
      }
    },
    get: vi.fn(),
    post: vi.fn()
  }
  return { handlers, instance }
})

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mock.instance)
  }
}))

import request from '../../src/utils/request'

describe('request HTTP 封装', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('应通过 axios.create 创建实例', async () => {
    const axios = (await import('axios')).default
    expect(axios.create).toHaveBeenCalled()
    expect(request).toBe(mock.instance)
  })

  describe('请求拦截器：token 注入与 Content-Type', () => {
    it('有 token 时应注入 Authorization 头', () => {
      localStorage.setItem('token', 'abc123')
      const config = { headers: {} }
      const result = mock.handlers.request.fulfilled(config)
      expect(result.headers.Authorization).toBe('Bearer abc123')
    })

    it('无 token 时不应设置 Authorization', () => {
      const config = { headers: {} }
      const result = mock.handlers.request.fulfilled(config)
      expect(result.headers.Authorization).toBeUndefined()
    })

    it('普通请求应默认设置 JSON Content-Type', () => {
      const config = { headers: {}, data: {} }
      const result = mock.handlers.request.fulfilled(config)
      expect(result.headers['Content-Type']).toContain('application/json')
    })

    it('FormData 请求不应覆盖 Content-Type', () => {
      const form = new FormData()
      const config = { headers: {}, data: form }
      const result = mock.handlers.request.fulfilled(config)
      expect(result.headers['Content-Type']).toBeUndefined()
    })

    it('已有 Content-Type 时不应覆盖', () => {
      const config = { headers: { 'Content-Type': 'text/plain' }, data: {} }
      const result = mock.handlers.request.fulfilled(config)
      expect(result.headers['Content-Type']).toBe('text/plain')
    })

    it('请求拦截器 rejected 应透传错误', async () => {
      const err = new Error('req fail')
      await expect(mock.handlers.request.rejected(err)).rejects.toBe(err)
    })
  })

  describe('响应拦截器：业务返回与错误处理', () => {
    it('正常响应应返回 response.data', () => {
      const data = { code: 200, data: 'payload' }
      expect(mock.handlers.response.fulfilled({ data })).toBe(data)
    })

    it('业务 code=401 应清除登录态并重定向', async () => {
      localStorage.setItem('token', 't')
      localStorage.setItem('user', 'u')
      await expect(
        mock.handlers.response.fulfilled({ data: { code: 401 } })
      ).rejects.toThrow('未授权')
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('user')).toBeNull()
    })

    it('HTTP 401 应清除登录态', async () => {
      localStorage.setItem('token', 't')
      const err = { response: { status: 401, data: {} } }
      await expect(mock.handlers.response.rejected(err)).rejects.toBe(err)
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('HTTP 500 不应清除登录态', async () => {
      localStorage.setItem('token', 't')
      const err = { response: { status: 500, data: {}, statusText: 'Server Error' } }
      await expect(mock.handlers.response.rejected(err)).rejects.toBe(err)
      expect(localStorage.getItem('token')).toBe('t')
    })

    it('无 response 时应回退到 error.message', async () => {
      const err = new Error('Network Down')
      await expect(mock.handlers.response.rejected(err)).rejects.toThrow('Network Down')
    })
  })
})

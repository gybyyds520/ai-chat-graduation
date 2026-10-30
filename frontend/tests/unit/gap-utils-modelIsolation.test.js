import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ModelSandbox, ModelIsolationManager } from '../../src/utils/modelIsolation'

describe('gap: utils/modelIsolation 模型沙箱隔离', () => {
  it('ModelSandbox 初始化状态为 initialized 且 isIsolated=true', () => {
    const sb = new ModelSandbox('m1', {})
    const s = sb.getState()
    expect(s.status).toBe('initialized')
    expect(s.isIsolated).toBe(true)
    sb.destroy()
  })

  it('setMemory/getMemory 命名空间隔离', () => {
    const sb = new ModelSandbox('ns-a', {})
    sb.setMemory('k', 'v')
    expect(sb.getMemory('k')).toBe('v')
    sb.destroy()
  })

  it('不同沙箱内存互不可见', () => {
    const sb1 = new ModelSandbox('iso-a', {})
    const sb2 = new ModelSandbox('iso-b', {})
    sb1.setMemory('shared', '1')
    expect(sb2.getMemory('shared')).toBeUndefined()
    sb1.destroy(); sb2.destroy()
  })

  it('clearMemory 清空当前沙箱内存', () => {
    const sb = new ModelSandbox('clr', {})
    sb.setMemory('a', 1)
    sb.setMemory('b', 2)
    sb.clearMemory()
    expect(sb.getMemory('a')).toBeUndefined()
    expect(sb.getMemory('b')).toBeUndefined()
    sb.destroy()
  })

  it('hasAccess 默认放行未列入黑名单的资源', () => {
    const sb = new ModelSandbox('acc', {})
    expect(sb.hasAccess('tokens')).toBe(true)
    expect(sb.hasAccess('/any/path')).toBe(true)
    sb.destroy()
  })

  it('acquireResource 累加 resourceUsage 并触发事件', () => {
    const sb = new ModelSandbox('res', {})
    let emitted = null
    sb.subscribe('resource-acquired', (e) => { emitted = e.detail })
    sb.acquireResource('memory', 128)
    expect(sb.getState().resourceUsage.memory).toBe(128)
    expect(emitted).toEqual({ resourcePath: 'memory', amount: 128 })
    sb.destroy()
  })

  it('releaseResource 在用量足够时扣减', () => {
    const sb = new ModelSandbox('rel', {})
    sb.acquireResource('cpu', 80)
    sb.releaseResource('cpu', 30)
    expect(sb.getState().resourceUsage.cpu).toBe(50)
    sb.destroy()
  })

  it('releaseResource 用量不足时不扣成负数', () => {
    const sb = new ModelSandbox('rel2', {})
    sb.acquireResource('cpu', 10)
    sb.releaseResource('cpu', 999)
    expect(sb.getState().resourceUsage.cpu).toBe(10)
    sb.destroy()
  })

  it('ModelIsolationManager createSandbox 同 modelId 返回同一实例', () => {
    const mgr = new ModelIsolationManager()
    const s1 = mgr.createSandbox('same-id', {})
    const s2 = mgr.createSandbox('same-id', {})
    expect(s1).toBe(s2)
    mgr.destroy()
  })

  it('getSandbox 未创建返回 undefined', () => {
    const mgr = new ModelIsolationManager()
    expect(mgr.getSandbox('never-exist')).toBeUndefined()
    mgr.destroy()
  })

  it('destroySandbox 后 getSandbox 返回 undefined', () => {
    const mgr = new ModelIsolationManager()
    mgr.createSandbox('todestroy', {})
    mgr.destroySandbox('todestroy')
    expect(mgr.getSandbox('todestroy')).toBeUndefined()
    mgr.destroy()
  })

  it('getIsolationStatus 不存在的模型返回 {exists:false}', () => {
    const mgr = new ModelIsolationManager()
    expect(mgr.getIsolationStatus('ghost')).toEqual({ exists: false })
    mgr.destroy()
  })

  it('enforceIsolation 设置 isIsolated=true 并清空内存', () => {
    const mgr = new ModelIsolationManager()
    mgr.createSandbox('enf', {})
    const sb = mgr.getSandbox('enf')
    sb.setMemory('k', 'v')
    mgr.enforceIsolation('enf')
    expect(sb.getState().isIsolated).toBe(true)
    expect(sb.getMemory('k')).toBeUndefined()
    mgr.destroy()
  })

  it('relaxIsolation 设置 isIsolated=false', () => {
    const mgr = new ModelIsolationManager()
    mgr.createSandbox('rlx', {})
    mgr.relaxIsolation('rlx')
    expect(mgr.getSandbox('rlx').getState().isIsolated).toBe(false)
    mgr.destroy()
  })
})

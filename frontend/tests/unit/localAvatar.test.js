import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LocalAvatar from '../../src/components/LocalAvatar.vue'

describe('LocalAvatar 头像组件', () => {
  it('无 src 但有 seed 时渲染本地生成的 SVG 头像', () => {
    const wrapper = mount(LocalAvatar, { props: { seed: 'Alice', size: 44 } })
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toMatch(/^data:image\/svg\+xml;base64,/)
  })

  it('传入 src 时优先渲染真实图片', () => {
    const wrapper = mount(LocalAvatar, {
      props: { seed: 'Alice', size: 44, src: '/uploads/character/abc.png' }
    })
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('/uploads/character/abc.png')
  })

  it('src 加载失败时回退到本地生成的 SVG 头像', async () => {
    const wrapper = mount(LocalAvatar, {
      props: { seed: 'Bob', size: 40, src: '/broken/not-exist.png' }
    })
    expect(wrapper.find('img').attributes('src')).toBe('/broken/not-exist.png')
    await wrapper.find('img').trigger('error')
    expect(wrapper.find('img').attributes('src')).toMatch(/^data:image\/svg\+xml;base64,/)
  })

  it('src 为空且 seed 为空时渲染字母色块兜底', () => {
    const wrapper = mount(LocalAvatar, { props: { seed: '', size: 40, src: '' } })
    expect(wrapper.find('.local-avatar').exists()).toBe(true)
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('不同 variant 生成不同头像，同一 variant 同 seed 稳定', () => {
    const a1 = mount(LocalAvatar, { props: { seed: '小明', variant: 'bottts' } })
    const a2 = mount(LocalAvatar, { props: { seed: '小明', variant: 'bottts' } })
    const b = mount(LocalAvatar, { props: { seed: '小明', variant: 'jdenticon' } })
    expect(a1.find('img').attributes('src')).toBe(a2.find('img').attributes('src'))
    expect(a1.find('img').attributes('src')).not.toBe(b.find('img').attributes('src'))
  })

  it('size 同时作用于本地头像与图片', () => {
    const img = mount(LocalAvatar, { props: { seed: 'X', size: 64, src: '/a.png' } })
    expect(img.find('img').attributes('style')).toContain('width: 64px')
    expect(img.find('img').attributes('style')).toContain('height: 64px')
  })
})

import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CitationList from '../../src/components/CitationList.vue'

describe('gap: components/CitationList 引用列表', () => {
  it('citations 为空时不渲染任何内容', () => {
    const wrapper = mount(CitationList, { props: { citations: [] } })
    expect(wrapper.find('.citation-list').exists()).toBe(false)
  })

  it('渲染标题与引用数量', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: 'A', url: '' }] }
    })
    expect(wrapper.find('.citation-list-title').text()).toContain('1')
  })

  it('safeUrl 时渲染为 <a> 标签', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: '百度', url: 'https://example.com' }] }
    })
    const a = wrapper.find('a.citation-title')
    expect(a.exists()).toBe(true)
    expect(a.attributes('href')).toBe('https://example.com')
    expect(a.attributes('target')).toBe('_blank')
  })

  it('不安全 url 时渲染为 span', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: 'Bad', url: 'javascript:alert(1)' }] }
    })
    expect(wrapper.find('a.citation-title').exists()).toBe(false)
    expect(wrapper.find('span.citation-title').exists()).toBe(true)
  })

  it('title 缺失时显示"无标题"', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, url: 'https://example.com' }] }
    })
    expect(wrapper.find('.citation-title').text()).toBe('无标题')
  })

  it('source 存在时追加显示', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: 'T', url: 'https://e.com', source: '维基' }] }
    })
    expect(wrapper.find('.citation-source').text()).toContain('维基')
  })

  it('短 snippet（<=60）不显示展开/收起按钮', () => {
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: 'T', url: 'https://e.com', snippet: '短摘要' }] }
    })
    expect(wrapper.find('.citation-toggle').exists()).toBe(false)
  })

  it('长 snippet 默认截断，点击后展开', async () => {
    const long = '这是一段非常长的摘要内容用于测试截断逻辑是否正常工作应该超过六十字这是一段非常长的摘要内容用于测试截断逻辑是否正常工作应该超过六十字'
    expect(long.length).toBeGreaterThan(60)
    const wrapper = mount(CitationList, {
      props: { citations: [{ index: 1, title: 'T', url: 'https://e.com', snippet: long }] }
    })
    expect(wrapper.find('.citation-toggle').exists()).toBe(true)
    expect(wrapper.find('.citation-toggle').text()).toContain('展开')
    // 初始显示截断
    expect(wrapper.find('.citation-snippet').text()).toContain('…')
    await wrapper.find('.citation-snippet').trigger('click')
    expect(wrapper.find('.citation-toggle').text()).toContain('收起')
  })
})

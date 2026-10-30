import {
  elfOutfitSets,
  DEFAULT_OUTFIT_SET,
  ELF_OUTFIT_TOP_TYPES,
  ELF_OUTFIT_BOTTOM_TYPES,
  ELF_OUTFIT_SHOE_TYPES,
  ELF_OUTFIT_SOCK_TYPES,
  ELF_OUTFIT_ACCESSORY_TYPES,
  ELF_OUTFIT_PART_KEYS,
  ELF_OUTFIT_COLOR_PALETTES,
  buildMixOutfit,
  applyColorOverride,
  removeColorOverride
} from '../../data/elfOutfitSets'

describe('elfOutfitSets 换装套装数据（换装小游戏级）', () => {
  it('1. 提供 8 套完整套装', () => {
    expect(elfOutfitSets.length).toBe(8)
    expect(DEFAULT_OUTFIT_SET.id).toBe('school')
  })

  it('2. 套装 id 全局唯一', () => {
    const ids = elfOutfitSets.map(s => s.id)
    expect(new Set(ids).size).toBe(ids.length)
  })

  it('3. 每套字段齐全：名称/emoji/描述/款式/发饰/配色', () => {
    for (const s of elfOutfitSets) {
      expect(typeof s.name).toBe('string')
      expect(s.name.length).toBeGreaterThan(0)
      expect(typeof s.emoji).toBe('string')
      expect(s.emoji.length).toBeGreaterThan(0)
      expect(typeof s.description).toBe('string')
      expect(s.description.length).toBeGreaterThan(0)
      expect(ELF_OUTFIT_TOP_TYPES).toContain(s.top)
      expect(ELF_OUTFIT_BOTTOM_TYPES).toContain(s.bottom)
      expect(ELF_OUTFIT_SHOE_TYPES).toContain(s.shoes)
      expect(ELF_OUTFIT_SOCK_TYPES).toContain(s.socks)
      expect(ELF_OUTFIT_ACCESSORY_TYPES).toContain(s.accessory)
      expect(s.colors).toBeTruthy()
      expect(typeof s.colors.top).toBe('string')
    }
  })

  it('4. 所有颜色为合法 6 位十六进制', () => {
    const HEX_RE = /^#[0-9a-fA-F]{6}$/
    for (const s of elfOutfitSets) {
      for (const key of Object.keys(s.colors)) {
        expect(s.colors[key]).toMatch(HEX_RE)
      }
    }
  })

  it('5. 每套都有上衣色/强调色/鞋色/发饰色（渲染必需）', () => {
    for (const s of elfOutfitSets) {
      expect(s.colors.top).toBeTruthy()
      expect(s.colors.accent).toBeTruthy()
      expect(s.colors.shoes).toBeTruthy()
      expect(s.colors.accessory).toBeTruthy()
    }
  })

  it('6. 无袜套装（none）也声明 socks 颜色（渲染防呆）', () => {
    for (const s of elfOutfitSets) {
      expect(s.colors.socks).toBeTruthy()
    }
  })

  it('7. 部件键枚举完整且互不重复', () => {
    expect(ELF_OUTFIT_PART_KEYS).toEqual(['top', 'bottom', 'shoes', 'socks', 'accessory'])
    expect(new Set(ELF_OUTFIT_PART_KEYS).size).toBe(ELF_OUTFIT_PART_KEYS.length)
  })

  it('8. 混搭部件类型枚举均非空', () => {
    expect(ELF_OUTFIT_TOP_TYPES.length).toBeGreaterThanOrEqual(6)
    expect(ELF_OUTFIT_BOTTOM_TYPES.length).toBeGreaterThanOrEqual(6)
    expect(ELF_OUTFIT_SHOE_TYPES.length).toBeGreaterThanOrEqual(5)
    expect(ELF_OUTFIT_ACCESSORY_TYPES).toContain('none')
    expect(ELF_OUTFIT_ACCESSORY_TYPES.length).toBeGreaterThanOrEqual(5)
  })

  it('9. 换色色板覆盖全部部件键且颜色合法', () => {
    const HEX_RE = /^#[0-9a-fA-F]{6}$/
    for (const part of ELF_OUTFIT_PART_KEYS) {
      const palette = ELF_OUTFIT_COLOR_PALETTES[part]
      expect(Array.isArray(palette)).toBe(true)
      expect(palette.length).toBeGreaterThanOrEqual(5)
      for (const c of palette) expect(c).toMatch(HEX_RE)
    }
    // trim 也必须有（下摆/滚边换色）
    expect(Array.isArray(ELF_OUTFIT_COLOR_PALETTES.trim)).toBe(true)
  })

  it('10. 新增套装（公主裙/哥特/古风华服）款式落在新枚举内', () => {
    const princess = elfOutfitSets.find(s => s.id === 'princess')
    const gothic = elfOutfitSets.find(s => s.id === 'gothic')
    expect(princess.top).toBe('dress')
    expect(princess.accessory).toBe('crown')
    expect(princess.shoes).toBe('pumps')
    expect(gothic.top).toBe('sailor')
    expect(gothic.bottom).toBe('pencil')
    expect(gothic.accessory).toBe('star')
    // 古风华服（参考图风格：青绿汉服 + 曳地长裙 + 发簪）
    const gufeng = elfOutfitSets.find(s => s.id === 'gufeng')
    expect(gufeng).toBeTruthy()
    expect(gufeng.top).toBe('hanfu')
    expect(ELF_OUTFIT_TOP_TYPES).toContain('hanfu')
    expect(gufeng.bottom).toBe('long-skirt')
    expect(ELF_OUTFIT_BOTTOM_TYPES).toContain('long-skirt')
    expect(gufeng.accessory).toBe('hairpin')
    expect(ELF_OUTFIT_ACCESSORY_TYPES).toContain('hairpin')
  })

  it('11. buildMixOutfit 单件混搭：替换部件且其余继承基底', () => {
    const school = elfOutfitSets.find(s => s.id === 'school')
    const mixed = buildMixOutfit(school, 'top', 'sailor')
    expect(mixed.top).toBe('sailor')
    expect(mixed.bottom).toBe(school.bottom)
    expect(mixed.shoes).toBe(school.shoes)
    expect(mixed.socks).toBe(school.socks)
    expect(mixed.accessory).toBe(school.accessory)
    expect(mixed.colors).toEqual(school.colors)
    // 可被渲染器消费：带 id 兜底与 top+colors 判定
    expect(mixed.top && mixed.colors).toBeTruthy()
  })

  it('12. buildMixOutfit 支持连续混搭（在自定义结果上继续改）', () => {
    const mixed1 = buildMixOutfit(DEFAULT_OUTFIT_SET, 'bottom', 'flared')
    const mixed2 = buildMixOutfit(mixed1, 'accessory', 'crown')
    expect(mixed2.bottom).toBe('flared')
    expect(mixed2.accessory).toBe('crown')
    expect(mixed2.top).toBe(DEFAULT_OUTFIT_SET.top)
  })

  it('13. applyColorOverride 覆盖颜色且不改原对象', () => {
    const school = elfOutfitSets.find(s => s.id === 'school')
    const colored = applyColorOverride(school, 'top', '#123456')
    expect(colored.colors.top).toBe('#123456')
    expect(school.colors.top).toBe('#ffffff') // 原对象不变
    // 其余颜色保留
    expect(colored.colors.bottom).toBe(school.colors.bottom)
    const HEX_RE = /^#[0-9a-fA-F]{6}$/
    for (const key of Object.keys(colored.colors)) expect(colored.colors[key]).toMatch(HEX_RE)
  })

  it('14. removeColorOverride 还原默认色且保留其他覆盖', () => {
    const custom = applyColorOverride(applyColorOverride(DEFAULT_OUTFIT_SET, 'top', '#111111'), 'shoes', '#222222')
    const restored = removeColorOverride(custom, 'top')
    // 删除后该键移除（由 UI 层回落套装默认色）
    expect('top' in restored.colors).toBe(false)
    expect(restored.colors.shoes).toBe('#222222')
    // 其余字段不受影响
    expect(restored.top).toBe(custom.top)
  })
})

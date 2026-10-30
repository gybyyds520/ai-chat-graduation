import {
  animeElfCharacters,
  elfAnimeCharacters,
  ANIME_HAIRSTYLES,
  ANIME_ACCESSORIES,
  getAnimeCharacterById,
  isAnimeCharacter,
  validateAnimeCharacter
} from '../../data/animeElfData'

describe('animeElfData 二次元桌宠角色数据', () => {
  it('1. 提供 7 位二次元女角色，且 elfAnimeCharacters 为同一集合', () => {
    expect(animeElfCharacters.length).toBe(7)
    expect(elfAnimeCharacters).toBe(animeElfCharacters)
  })

  it('2. 每个角色 id 唯一且以 anime- 开头', () => {
    const ids = animeElfCharacters.map((c) => c.id)
    expect(new Set(ids).size).toBe(ids.length)
    ids.forEach((id) => expect(id.startsWith('anime-')).toBe(true))
  })

  it('3. 每个角色字段完整：名称/描述/性格/表情符号/发型/配件/主色', () => {
    animeElfCharacters.forEach((c) => {
      expect(typeof c.name).toBe('string')
      expect(c.name.length).toBeGreaterThan(0)
      expect(c.category).toBe('anime')
      expect(typeof c.description).toBe('string')
      expect(typeof c.personality).toBe('string')
      expect(c.emoji).toMatch(/^[\u{1F300}-\u{1FAFF}\u2600-\u27BF]/u)
      expect(ANIME_HAIRSTYLES).toContain(c.hairstyle)
      expect(ANIME_ACCESSORIES).toContain(c.accessory)
      expect(c.baseColor).toMatch(/^#[0-9a-fA-F]{6}$/)
    })
  })

  it('4. palette 包含全部 8 个合法 6 位十六进制颜色字段', () => {
    const hexRe = /^#[0-9a-fA-F]{6}$/
    const required = ['hair', 'hairHighlight', 'eyes', 'eyesHighlight', 'skin', 'skinShadow', 'outfit', 'outfitAccent', 'ribbon', 'blush']
    animeElfCharacters.forEach((c) => {
      required.forEach((key) => {
        expect(hexRe.test(c.palette[key]), `${c.id}.palette.${key}=${c.palette[key]}`).toBe(true)
      })
    })
  })

  it('5. 发型/配件覆盖多样：7 角色发型互不相同，配件不重复', () => {
    const styles = animeElfCharacters.map((c) => c.hairstyle)
    expect(new Set(styles).size).toBe(7)
    const accs = animeElfCharacters.map((c) => c.accessory)
    expect(new Set(accs).size).toBe(accs.length)
  })

  it('5b. 默认形象为古风美人「青黛」（盘发 + 玉簪，参考图形象）', () => {
    const qingdai = animeElfCharacters[0]
    expect(qingdai.id).toBe('anime-qingdai')
    expect(qingdai.name).toBe('青黛')
    expect(qingdai.hairstyle).toBe('gufeng-bun')
    expect(qingdai.accessory).toBe('jade-hairpin')
    expect(ANIME_HAIRSTYLES).toContain('gufeng-bun')
    expect(ANIME_ACCESSORIES).toContain('jade-hairpin')
  })

  it('6. getAnimeCharacterById 精确查找，未知 id 返回 undefined', () => {
    expect(getAnimeCharacterById('anime-yingnai').name).toBe('樱奈')
    expect(getAnimeCharacterById('nonexistent')).toBeUndefined()
  })

  it('7. isAnimeCharacter 判断角色类别', () => {
    expect(isAnimeCharacter(animeElfCharacters[0])).toBe(true)
    expect(isAnimeCharacter({ category: 'basic' })).toBe(false)
    expect(isAnimeCharacter(null)).toBe(false)
    expect(isAnimeCharacter(undefined)).toBe(false)
  })

  it('8. validateAnimeCharacter：合法角色通过，缺字段/非法值报错', () => {
    expect(validateAnimeCharacter(animeElfCharacters[0]).valid).toBe(true)
    expect(validateAnimeCharacter(animeElfCharacters[0]).errors).toEqual([])

    const bad1 = validateAnimeCharacter({ id: 'ball', category: 'basic' })
    expect(bad1.valid).toBe(false)
    expect(bad1.errors.length).toBeGreaterThan(0)

    const bad2 = validateAnimeCharacter({ ...animeElfCharacters[0], hairstyle: 'unknown-style' })
    expect(bad2.valid).toBe(false)
    expect(bad2.errors.join()).toContain('hairstyle')

    const bad3 = validateAnimeCharacter({ ...animeElfCharacters[0], palette: { ...animeElfCharacters[0].palette, eyes: 'red' } })
    expect(bad3.valid).toBe(false)
    expect(bad3.errors.join()).toContain('eyes')

    expect(validateAnimeCharacter(null).valid).toBe(false)
  })

  it('9. 每个角色均有可被 AnimeElfSprite 使用的完整参数（scale 与 specialEffect）', () => {
    animeElfCharacters.forEach((c) => {
      expect(c.scale.body).toBeGreaterThan(0)
      expect(c.scale.head).toBeGreaterThan(0)
      expect(typeof c.specialEffect).toBe('string')
    })
  })

  it('10. 每个角色均有角色化台词池：lines 非空、文案有效且角色间不重复（体现角色人格）', () => {
    animeElfCharacters.forEach((c) => {
      expect(Array.isArray(c.lines), `${c.id} 缺少 lines 台词池`).toBe(true)
      expect(c.lines.length, `${c.id} 台词池为空`).toBeGreaterThanOrEqual(3)
      c.lines.forEach((l) => {
        expect(typeof l).toBe('string')
        expect(l.length).toBeGreaterThan(0)
      })
    })
    // 角色化：任意两个角色的台词池不得有交集，避免"通用台词复制"
    for (let i = 0; i < animeElfCharacters.length; i++) {
      for (let j = i + 1; j < animeElfCharacters.length; j++) {
        const dup = animeElfCharacters[i].lines.filter((l) => animeElfCharacters[j].lines.includes(l))
        expect(dup, `${animeElfCharacters[i].name} 与 ${animeElfCharacters[j].name} 台词重复`).toEqual([])
      }
    }
  })
})

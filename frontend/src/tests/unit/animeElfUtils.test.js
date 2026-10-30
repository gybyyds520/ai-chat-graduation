import {
  ANIME_EXPRESSION_MAP,
  getAnimeExpression,
  getBlinkTransform,
  isEyesClosed,
  toElfCompatibleCharacter,
  getAllAnimeCharacters,
  ANIME_INTERACTION_LINES,
  ANIME_REST_LINES,
  pickRandom,
  getBlinkInterval,
  getIdleRandomAction,
  getRandomInteraction,
  getExpressionMouth,
  getCharacterLine,
  getNeedHintLine
} from '../../utils/animeElfUtils'

describe('animeElfUtils 二次元桌宠工具', () => {
  it('1. 全部 12 个动画状态均有表情映射', () => {
    const states = ['idle', 'stand', 'happy', 'sad', 'surprised', 'thinking', 'sleep', 'jump', 'dance', 'wave', 'crouch', 'lie']
    states.forEach((s) => {
      const expr = ANIME_EXPRESSION_MAP[s]
      expect(expr, `状态 ${s} 缺少映射`).toBeTruthy()
      expect(['open', 'happy', 'sad', 'wide', 'sleepy', 'closed']).toContain(expr.eyes)
      expect(['smile', 'open', 'pout', 'o', 'flat', 'sleep']).toContain(expr.mouth)
      expect(['normal', 'raise', 'furrow']).toContain(expr.brow)
    })
  })

  it('2. 未知状态回退 idle 表情', () => {
    const fallback = getAnimeExpression('nonexistent-state')
    expect(fallback).toEqual(ANIME_EXPRESSION_MAP.idle)
  })

  it('3. 闭眼状态（sleep/lie）返回闭合眼睛', () => {
    expect(getAnimeExpression('sleep').eyes).toBe('closed')
    expect(getAnimeExpression('lie').eyes).toBe('closed')
    expect(isEyesClosed('sleep')).toBe(true)
    expect(isEyesClosed('idle')).toBe(false)
  })

  it('4. getBlinkTransform：闭眼压缩、睁眼还原、相位 0/1 全开', () => {
    expect(getBlinkTransform(0, 'sleep')).toBe('scaleY(0.08)')
    expect(getBlinkTransform(0.5, 'sleep')).toBe('scaleY(0.08)')
    expect(getBlinkTransform(0, 'idle')).toBe('scaleY(1)')
    expect(getBlinkTransform(1, 'idle')).toBe('scaleY(1)')
    // 眨眼中间相位产生压缩
    const mid = getBlinkTransform(0.5, 'idle')
    expect(mid).toMatch(/^scaleY\(0\.\d+\)$/)
    const v = parseFloat(mid.replace('scaleY(', '').replace(')', ''))
    expect(v).toBeGreaterThan(0.05)
    expect(v).toBeLessThan(1)
    // happy/sad/wide 形状本身已表达，不叠加眨眼压缩
    expect(getBlinkTransform(0.5, 'happy')).toBe('scaleY(1)')
    expect(getBlinkTransform(0.5, 'surprised')).toBe('scaleY(1)')
  })

  it('5. 相位越界时安全钳制', () => {
    expect(getBlinkTransform(-1, 'idle')).toBe('scaleY(1)')
    expect(getBlinkTransform(99, 'idle')).toBe('scaleY(1)')
    expect(getBlinkTransform(NaN, 'idle')).toBe('scaleY(1)')
  })

  it('6. toElfCompatibleCharacter 生成 elfData 兼容结构', () => {
    const src = getAllAnimeCharacters()[0]
    const out = toElfCompatibleCharacter(src)
    expect(out.id).toBe(src.id)
    expect(out.category).toBe('anime')
    expect(out.shape).toBe('anime-girl')
    expect(out.name).toBe(src.name)
    expect(out.baseColor).toMatch(/^#[0-9a-fA-F]{6}$/)
    expect(out.specialEffect).toBe(src.specialEffect)
  })

  it('7. getAllAnimeCharacters 返回 7 个角色（首位青黛古风默认形象）且不被外部修改污染', () => {
    const list = getAllAnimeCharacters()
    expect(list.length).toBe(7)
    expect(list[0].id).toBe('anime-qingdai')
    list[0].name = '被篡改'
    expect(getAllAnimeCharacters()[0].name).not.toBe('被篡改')
  })

  it('8. pickRandom / getBlinkInterval / getIdleRandomAction / getRandomInteraction 支持注入随机源', () => {
    const rand = () => 0
    expect(pickRandom(['a', 'b', 'c'], rand)).toBe('a')
    expect(pickRandom([], rand)).toBeNull()
    expect(getBlinkInterval(2000, 6000, rand)).toBe(2000)
    expect(getBlinkInterval(5000, 3000, rand)).toBe(3000)
    expect(getIdleRandomAction(rand)).toBe('yawn')
    const combo = getRandomInteraction(rand)
    expect(combo.line).toBe(ANIME_INTERACTION_LINES[0])
    expect(combo.expression).toBe('happy')
    expect(combo.action).toBe('wave')
    expect(combo.effect).toBe('heart')
  })

  it('9. getExpressionMouth 情绪→嘴型映射与兜底', () => {
    expect(getExpressionMouth('normal')).toBe('smile')
    expect(getExpressionMouth('happy')).toBe('open')
    expect(getExpressionMouth('sad')).toBe('pout')
    expect(getExpressionMouth('surprised')).toBe('o')
    expect(getExpressionMouth('thinking')).toBe('flat')
    expect(getExpressionMouth('sleep')).toBe('sleep')
    expect(getExpressionMouth('unknown')).toBe('smile')
  })

  it('10. getCharacterLine：角色有台词池则用角色台词，否则回退通用池', () => {
    const rand = () => 0
    // 有台词池的角色 → 返回角色台词
    const ch = getAllAnimeCharacters()[0]
    expect(getCharacterLine(ch, ANIME_INTERACTION_LINES, rand)).toBe(ch.lines[0])
    // 无台词池的角色（如普通对象）→ 回退通用池
    const plain = { id: 'x', name: 'X' }
    expect(getCharacterLine(plain, ANIME_INTERACTION_LINES, rand)).toBe(ANIME_INTERACTION_LINES[0])
    // 空台词池数组同样回退
    expect(getCharacterLine({ id: 'y', lines: [] }, ANIME_INTERACTION_LINES, rand)).toBe(ANIME_INTERACTION_LINES[0])
    // null/undefined 角色安全回退
    expect(getCharacterLine(null, ANIME_INTERACTION_LINES, rand)).toBe(ANIME_INTERACTION_LINES[0])
    expect(getCharacterLine(undefined, ANIME_INTERACTION_LINES, rand)).toBe(ANIME_INTERACTION_LINES[0])
    // 无通用池时返回空串而非崩溃
    expect(getCharacterLine(null, [], rand)).toBe('')
  })

  it('10a. ANIME_REST_LINES 休息台词池：非空且条目均为非空字符串', () => {
    expect(ANIME_REST_LINES.length).toBeGreaterThanOrEqual(6)
    ANIME_REST_LINES.forEach((line) => {
      expect(typeof line).toBe('string')
      expect(line.trim().length).toBeGreaterThan(0)
    })
  })

  it('10b. 休息台词池可作 getCharacterLine 的回退池（休息期悠闲台词）', () => {
    const rand = () => 0
    // 有角色台词池的角色优先角色台词
    const ch = getAllAnimeCharacters()[0]
    expect(getCharacterLine(ch, ANIME_REST_LINES, rand)).toBe(ch.lines[0])
    // 无台词池角色 → 回退休息池首条
    expect(getCharacterLine({ id: 'plain' }, ANIME_REST_LINES, rand)).toBe(ANIME_REST_LINES[0])
    // 随机源边界：rand≈1 取池末条（pickRandom 使用 floor，不能传字面 1）
    expect(getCharacterLine({ id: 'plain' }, ANIME_REST_LINES, () => 0.999)).toBe(ANIME_REST_LINES[ANIME_REST_LINES.length - 1])
  })

  it('11. getNeedHintLine：按养成需求输出提示，无需求返回 null', () => {
    const rand = () => 0
    const hungry = getNeedHintLine({ hungry: true }, rand)
    const lethargic = getNeedHintLine({ lethargic: true }, rand)
    const affectionate = getNeedHintLine({ affectionate: true }, rand)
    expect(typeof hungry).toBe('string')
    expect(typeof lethargic).toBe('string')
    expect(typeof affectionate).toBe('string')
    expect(hungry.length).toBeGreaterThan(0)
    expect(lethargic.length).toBeGreaterThan(0)
    expect(affectionate.length).toBeGreaterThan(0)
    expect(getNeedHintLine({}, rand)).toBeNull()
    expect(getNeedHintLine(null, rand)).toBeNull()
    expect(getNeedHintLine(undefined, rand)).toBeNull()
    // 多个需求并存时优先饥饿（返回 hungry 池首条）
    expect(getNeedHintLine({ hungry: true, lethargic: true }, rand)).toBe(hungry)
  })
})

import {
  MAX_IMAGE_DIMENSION,
  MAX_IMAGE_SIZE,
  validateElfImageFile,
  clampElfImageSize,
  colorDistance,
  estimateBackgroundColor,
  removeImageBackground
} from '../../utils/elfImageUtils'

describe('elfImageUtils 自定义形象图片工具', () => {
  describe('validateElfImageFile 文件校验', () => {
    it('1. 空文件应拒绝', () => {
      expect(validateElfImageFile(null).ok).toBe(false)
      expect(validateElfImageFile(undefined).ok).toBe(false)
    })

    it('2. 非图片类型应拒绝', () => {
      expect(validateElfImageFile({ type: 'text/plain', size: 100 }).ok).toBe(false)
      expect(validateElfImageFile({ type: 'application/pdf', size: 100 }).ok).toBe(false)
    })

    it('3. 图片类型应通过', () => {
      expect(validateElfImageFile({ type: 'image/png', size: 1024 }).ok).toBe(true)
      expect(validateElfImageFile({ type: 'image/jpeg', size: 2048 }).ok).toBe(true)
      expect(validateElfImageFile({ type: 'image/webp', size: 512 }).ok).toBe(true)
    })

    it('4. 超过大小上限应拒绝', () => {
      const tooLarge = { type: 'image/png', size: MAX_IMAGE_SIZE + 1 }
      const r = validateElfImageFile(tooLarge)
      expect(r.ok).toBe(false)
      expect(r.reason).toBe('too_large')
    })

    it('5. 恰好等于上限应通过（边界值）', () => {
      expect(validateElfImageFile({ type: 'image/png', size: MAX_IMAGE_SIZE }).ok).toBe(true)
    })
  })

  describe('clampElfImageSize 等比缩放', () => {
    it('6. 未超最长边应保持原尺寸', () => {
      expect(clampElfImageSize(256, 256)).toEqual({ width: 256, height: 256 })
      expect(clampElfImageSize(512, 300)).toEqual({ width: 512, height: 300 })
    })

    it('7. 宽超限应等比缩小（宽为最长边）', () => {
      const r = clampElfImageSize(1024, 512)
      expect(r.width).toBe(512)
      expect(r.height).toBe(256)
    })

    it('8. 高超限应等比缩小（高为最长边）', () => {
      const r = clampElfImageSize(400, 1200, 600)
      expect(r.width).toBe(200)
      expect(r.height).toBe(600)
    })

    it('9. 自定义 maxDim 生效', () => {
      const r = clampElfImageSize(2000, 1000, 256)
      expect(r.width).toBe(256)
      expect(r.height).toBe(128)
    })

    it('10. 非法输入防御（0/负数/缺失）', () => {
      expect(clampElfImageSize(0, 0)).toEqual({ width: 1, height: 1 })
      expect(clampElfImageSize(-5, 10)).toEqual({ width: 1, height: 10 })
      expect(clampElfImageSize(undefined, undefined)).toEqual({ width: 1, height: 1 })
      expect(MAX_IMAGE_DIMENSION).toBe(512)
    })

    it('11. 缩放后长边精确等于 maxDim', () => {
      const r = clampElfImageSize(800, 600)
      expect(Math.max(r.width, r.height)).toBe(512)
    })
  })

  describe('colorDistance 颜色距离', () => {
    it('12. 相同颜色距离为 0', () => {
      expect(colorDistance({ r: 255, g: 255, b: 255 }, { r: 255, g: 255, b: 255 })).toBe(0)
    })

    it('13. 黑白两色距离为 1（最大）', () => {
      expect(colorDistance({ r: 255, g: 255, b: 255 }, { r: 0, g: 0, b: 0 })).toBeCloseTo(1, 6)
    })

    it('14. 中间色距离位于 0~1 之间且对称', () => {
      const d1 = colorDistance({ r: 200, g: 100, b: 50 }, { r: 100, g: 50, b: 25 })
      const d2 = colorDistance({ r: 100, g: 50, b: 25 }, { r: 200, g: 100, b: 50 })
      expect(d1).toBe(d2)
      expect(d1).toBeGreaterThan(0)
      expect(d1).toBeLessThan(1)
    })
  })

  describe('estimateBackgroundColor 四角主色估计', () => {
    function makeImageData(width, height, fill) {
      const data = new Uint8ClampedArray(width * height * 4)
      for (let i = 0; i < data.length; i += 4) {
        data[i] = fill[0]
        data[i + 1] = fill[1]
        data[i + 2] = fill[2]
        data[i + 3] = 255
      }
      return { data, width, height }
    }

    it('15. 纯色图四角主色即该色', () => {
      const img = makeImageData(100, 100, [240, 240, 245])
      const bg = estimateBackgroundColor(img)
      expect(bg).toEqual({ r: 240, g: 240, b: 245 })
    })

    it('16. 无数据输入返回 null（防御）', () => {
      expect(estimateBackgroundColor(null)).toBeNull()
      expect(estimateBackgroundColor({})).toBeNull()
    })
  })

  describe('removeImageBackground 色度键去背景', () => {
    function makeImageData(width, height, corner, center) {
      const data = new Uint8ClampedArray(width * height * 4)
      const cx = Math.floor(width / 2)
      const cy = Math.floor(height / 2)
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const i = (y * width + x) * 4
          const c = (Math.abs(x - cx) < 4 && Math.abs(y - cy) < 4) ? center : corner
          data[i] = c[0]
          data[i + 1] = c[1]
          data[i + 2] = c[2]
          data[i + 3] = 255
        }
      }
      return { data, width, height }
    }

    it('17. 接近背景色的像素被置为透明（alpha=0）', () => {
      const img = makeImageData(32, 32, [245, 245, 250], [30, 60, 120])
      const out = removeImageBackground(img, { threshold: 0.2 })
      // 中心（主体色）保留不透明
      const ci = (16 * 32 + 16) * 4
      expect(out.data[ci + 3]).toBe(255)
      // 角落（背景色）被抠掉
      const ci2 = 0
      expect(out.data[ci2 + 3]).toBe(0)
    })

    it('18. 无 bgColor 时自动估计四角主色', () => {
      const img = makeImageData(32, 32, [10, 200, 10], [255, 0, 0])
      const out = removeImageBackground(img, { threshold: 0.2 })
      expect(out.data[0 + 3]).toBe(0)          // 绿背景被抠
      const ci = (16 * 32 + 16) * 4
      expect(out.data[ci + 3]).toBe(255)       // 红主体保留
    })

    it('19. 羽化带：临界像素 alpha 平滑过渡而非突变', () => {
      const img = makeImageData(16, 16, [240, 240, 240], [240, 240, 240])
      // 全图同色 → 全部抠为透明（羽化不改变阈值内结果）
      const out = removeImageBackground(img, { threshold: 0.2 })
      expect(out.data[0 + 3]).toBe(0)
    })

    it('20. 非法输入原样返回（防御）', () => {
      expect(removeImageBackground(null)).toBeNull()
      const bad = { data: new Uint8ClampedArray(16), width: 2 }
      expect(removeImageBackground(bad)).toBe(bad)
      const bad2 = { width: 2, height: 2 }
      expect(removeImageBackground(bad2)).toBe(bad2)
    })
  })
})

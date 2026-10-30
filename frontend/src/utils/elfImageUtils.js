/**
 * elfImageUtils.js — 桌宠自定义形象图片工具（纯函数，可独立测试）
 *
 * 职责：
 *  1. 校验用户选择的图片文件（类型 / 大小）
 *  2. 计算图片压缩后的目标尺寸（最长边限制，保证 localStorage 体积可控）
 *  3. 将 File 读取并压缩为 dataURL（供 elfConfig.customImage 使用）
 *  4. 智能去背景：四角主色检测 → 色度键抠图 + 边缘羽化（供 3D 立牌渲染）
 *
 * 纯函数/无 DOM 强依赖（FileReader/canvas 仅在浏览器可用，
 * 测试时通过注入实现或仅测纯计算部分）。
 */

/** 自定义形象默认最长边限制（px） */
export const MAX_IMAGE_DIMENSION = 512
/** 自定义形象默认大小上限（字节） */
export const MAX_IMAGE_SIZE = 5 * 1024 * 1024
/** 压缩 JPEG 质量 */
export const IMAGE_QUALITY = 0.85

/**
 * 校验图片文件：必须是 image/* 类型且不超过大小上限。
 * @param {File|{type:string,size:number}} file
 * @returns {{ ok: boolean, reason?: string }}
 */
export function validateElfImageFile(file) {
  if (!file) return { ok: false, reason: 'no_file' }
  if (typeof file.type !== 'string' || !file.type.startsWith('image/')) {
    return { ok: false, reason: 'not_image' }
  }
  if (typeof file.size === 'number' && file.size > MAX_IMAGE_SIZE) {
    return { ok: false, reason: 'too_large' }
  }
  return { ok: true }
}

/**
 * 计算等比缩放后的目标尺寸（最长边不超过 maxDim）。
 * @param {number} width - 原始宽（>0）
 * @param {number} height - 原始高（>0）
 * @param {number} [maxDim=512]
 * @returns {{ width:number, height:number }} 缩放后的整数尺寸
 */
export function clampElfImageSize(width, height, maxDim = MAX_IMAGE_DIMENSION) {
  const w = Math.max(1, Math.floor(width || 1))
  const h = Math.max(1, Math.floor(height || 1))
  const m = Math.max(1, Math.floor(maxDim || MAX_IMAGE_DIMENSION))
  const longest = Math.max(w, h)
  if (longest <= m) return { width: w, height: h }
  const ratio = m / longest
  return {
    width: Math.max(1, Math.round(w * ratio)),
    height: Math.max(1, Math.round(h * ratio))
  }
}

/**
 * 读取并压缩图片文件为 dataURL（浏览器环境）。
 * 先按目标尺寸绘制到 canvas，再导出 JPEG dataURL。
 * @param {File} file
 * @param {object} [opts]
 * @param {number} [opts.maxDim=512]
 * @param {number} [opts.quality=0.85]
 * @returns {Promise<string>} JPEG dataURL
 */
export function readElfImageDataURL(file, opts = {}) {
  const maxDim = opts.maxDim || MAX_IMAGE_DIMENSION
  const quality = opts.quality || IMAGE_QUALITY
  return new Promise((resolve, reject) => {
    if (typeof FileReader === 'undefined' || typeof document === 'undefined' || typeof Image === 'undefined') {
      reject(new Error('browser_required'))
      return
    }
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('read_failed'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('decode_failed'))
      img.onload = () => {
        try {
          const size = clampElfImageSize(img.naturalWidth, img.naturalHeight, maxDim)
          const canvas = document.createElement('canvas')
          canvas.width = size.width
          canvas.height = size.height
          const ctx = canvas.getContext('2d')
          if (!ctx) {
            // 极老浏览器无 2d 上下文：退回原 dataURL（仅当它足够小）
            resolve(String(reader.result))
            return
          }
          ctx.drawImage(img, 0, 0, size.width, size.height)
          resolve(canvas.toDataURL('image/jpeg', quality))
        } catch (e) {
          reject(e)
        }
      }
      img.src = String(reader.result)
    }
    reader.readAsDataURL(file)
  })
}

/* ════════════════════════════════════════════════════════════
 * 智能去背景（色度键）：供"图片自动渲染为 3D 立体人物"使用
 * 全部为纯函数，输入输出为 ImageData-like 结构
 * （{ data: Uint8ClampedArray, width, height }），可独立测试。
 * ════════════════════════════════════════════════════════════ */

/** 去背景默认配置 */
export const REMOVE_BG_DEFAULTS = {
  /** 四角采样半径（占短边比例） */
  cornerRatio: 0.08,
  /** 判定为背景的色距阈值（0~1，越大越激进） */
  threshold: 0.18,
  /** 羽化宽度（0~1，相对 threshold，越大过渡越柔和） */
  feather: 0.35
}

/**
 * 计算像素颜色距离（归一化 0~1，欧氏距离 / √3）
 * @param {{r:number,g:number,b:number}} a
 * @param {{r:number,g:number,b:number}} b
 * @returns {number}
 */
export function colorDistance(a, b) {
  const dr = (a.r - b.r) / 255
  const dg = (a.g - b.g) / 255
  const db = (a.b - b.b) / 255
  return Math.min(1, Math.sqrt(dr * dr + dg * dg + db * db) / Math.sqrt(3))
}

/**
 * 从图像四角区域采样，估计背景主色。
 * 纯函数：遍历四角像素（半径 = max(1, cornerRatio * 短边)），
 * 计算平均色；完全空图返回 null。
 * @param {{data:Uint8ClampedArray,width:number,height:number}} imageData
 * @param {object} [opts]
 * @returns {{r:number,g:number,b:number}|null}
 */
export function estimateBackgroundColor(imageData, opts = {}) {
  if (!imageData || !imageData.data || !imageData.width || !imageData.height) return null
  const { width, height, data } = imageData
  const cornerRatio = opts.cornerRatio || REMOVE_BG_DEFAULTS.cornerRatio
  const radius = Math.max(1, Math.floor(Math.min(width, height) * cornerRatio))

  let sumR = 0
  let sumG = 0
  let sumB = 0
  let count = 0

  const regions = [
    [0, 0],
    [width - radius, 0],
    [0, height - radius],
    [width - radius, height - radius]
  ]

  for (const [ox, oy] of regions) {
    for (let y = oy; y < Math.min(height, oy + radius); y++) {
      for (let x = ox; x < Math.min(width, ox + radius); x++) {
        const i = (y * width + x) * 4
        sumR += data[i]
        sumG += data[i + 1]
        sumB += data[i + 2]
        count++
      }
    }
  }
  if (count === 0) return null
  return {
    r: Math.round(sumR / count),
    g: Math.round(sumG / count),
    b: Math.round(sumB / count)
  }
}

/**
 * 色度键去背景：把接近背景色的像素设为透明，边缘做柔和羽化过渡。
 * 纯函数，返回新的 data 数组（不修改输入）。
 * @param {{data:Uint8ClampedArray,width:number,height:number}} imageData
 * @param {object} [opts]
 * @param {{r:number,g:number,b:number}|null} [opts.bgColor] 背景主色（缺省自动估计）
 * @returns {{data:Uint8ClampedArray,width:number,height:number}}
 */
export function removeImageBackground(imageData, opts = {}) {
  if (!imageData || !imageData.data || !imageData.width || !imageData.height) {
    return imageData
  }
  const { width, height, data } = imageData
  const bg = opts.bgColor || estimateBackgroundColor(imageData, opts)
  if (!bg) return imageData

  const threshold = opts.threshold ?? REMOVE_BG_DEFAULTS.threshold
  const feather = opts.feather ?? REMOVE_BG_DEFAULTS.feather

  const out = new Uint8ClampedArray(data.length)
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    const alpha = data[i + 3]
    const d = colorDistance({ r, g, b }, bg)

    let newAlpha = alpha
    if (d < threshold) {
      newAlpha = 0
    } else if (d < threshold * (1 + feather)) {
      // 羽化带：从 0 平滑过渡到原 alpha
      const t = (d - threshold) / (threshold * feather)
      newAlpha = Math.round(alpha * Math.min(1, t))
    }

    out[i] = r
    out[i + 1] = g
    out[i + 2] = b
    out[i + 3] = newAlpha
  }
  return { data: out, width, height }
}

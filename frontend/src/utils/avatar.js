export const generateAvatarUrl = (seed, size = 40) => {
  return {
    component: 'LocalAvatar',
    props: { seed, size }
  }
}

export const getAvatarLetter = (seed) => {
  if (!seed) return '?'
  const cleaned = seed.replace(/[^a-zA-Z\u4e00-\u9fa5]/g, '')
  if (cleaned.length === 0) return '?'
  const lastChar = cleaned[cleaned.length - 1]
  if (/[\u4e00-\u9fa5]/.test(lastChar)) {
    return lastChar.substring(0, 1)
  }
  return lastChar.toUpperCase()
}

export const hashCode = (str) => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i)
    hash |= 0
  }
  return hash
}

export const colors = [
  ['#FF6B6B', '#FF8E8E'],
  ['#4ECDC4', '#7EDDD6'],
  ['#45B7D1', '#7FC8E3'],
  ['#96CEB4', '#B5E0C4'],
  ['#FFEAA7', '#FFF3C4'],
  ['#DDA0DD', '#E8B8E8'],
  ['#98D8C8', '#B8E8DC'],
  ['#F7DC6F', '#FAEDB0'],
  ['#BB8FCE', '#D2B4DE'],
  ['#85C1E9', '#AED6F1']
]

export const getAvatarColors = (seed) => {
  const hash = hashCode(seed || Math.random().toString())
  return colors[Math.abs(hash) % colors.length]
}

/** 头像/配图允许的最大体积（MB） */
export const AVATAR_MAX_SIZE_MB = 5

/**
 * 校验待上传图片：必须是 image/* 类型且不超过 maxSizeMB。
 * 纯函数，便于单测；返回 { valid, message }。
 * @param {{type?:string,size?:number}} file
 * @param {{maxSizeMB?:number, label?:string}} [opts]
 */
export function validateImageFile(file, opts = {}) {
  const maxSizeMB = opts.maxSizeMB || AVATAR_MAX_SIZE_MB
  const label = opts.label || '图片'
  if (!file) return { valid: false, message: '请选择文件' }
  if (!file.type || !file.type.startsWith('image/')) {
    return { valid: false, message: '请上传图片文件' }
  }
  const sizeMB = file.size / 1024 / 1024
  if (sizeMB >= maxSizeMB) {
    return { valid: false, message: `${label}不能超过${maxSizeMB}MB` }
  }
  return { valid: true, message: '' }
}

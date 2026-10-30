<template>
  <img
    v-if="displaySrc"
    :src="displaySrc"
    :style="imgStyle"
    class="local-avatar-img"
    alt=""
    @error="onImgError"
  />
  <div v-else class="local-avatar" :style="avatarStyle">
    <span class="avatar-letter">{{ letter }}</span>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { generateLocalAvatarSvg } from '@/utils/localAvatar'

const props = defineProps({
  seed: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 40
  },
  // 可选：真实的头像图片 URL。传入后优先渲染该图片；加载失败自动回退到本地生成头像。
  src: {
    type: String,
    default: ''
  },
  // 本地生成头像的风格（avataaars/bottts/bottts-neutral/jdenticon/gridy/micah）。
  variant: {
    type: String,
    default: 'avataaars'
  }
})

const imgFailed = ref(false)

// 展示地址优先级：传入 src（且未加载失败）> 本地按 seed+风格生成 > 空（走字母色块兜底）
const displaySrc = computed(() => {
  if (props.src && !imgFailed.value) return props.src
  if (props.seed) return generateLocalAvatarSvg(props.seed, props.variant)
  return ''
})

const onImgError = () => {
  imgFailed.value = true
}

const hashCode = (str) => {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i)
    hash |= 0
  }
  return hash
}

const colors = [
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

const colorPair = computed(() => {
  const hash = hashCode(props.seed || Math.random().toString())
  return colors[Math.abs(hash) % colors.length]
})

const letter = computed(() => {
  if (!props.seed) return '?'
  const cleaned = props.seed.replace(/[^a-zA-Z\u4e00-\u9fa5]/g, '')
  if (cleaned.length === 0) return '?'
  const lastChar = cleaned[cleaned.length - 1]
  if (/[\u4e00-\u9fa5]/.test(lastChar)) {
    return lastChar.substring(0, 1)
  }
  return lastChar.toUpperCase()
})

const avatarStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  background: `linear-gradient(135deg, ${colorPair.value[0]}, ${colorPair.value[1]})`,
  fontSize: `${props.size * 0.4}px`
}))

const imgStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  borderRadius: '50%',
  objectFit: 'cover'
}))
</script>

<style scoped>
.local-avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-family: Arial, sans-serif;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.local-avatar-img {
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.avatar-letter {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
</style>

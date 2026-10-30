<template>
  <svg
    :viewBox="'0 0 240 380'"
    class="anime-elf"
    :class="[`hair-${character.hairstyle}`, `state-${state}`, talking ? 'is-talking' : '', idleAction ? `idle-action-${idleAction}` : '']"
    role="img"
    :aria-label="character.name"
  >
    <defs>
      <!-- 头发：发根深 → 发梢浅 -->
      <linearGradient :id="`${uid}-hair`" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" :stop-color="palette.hair" />
        <stop offset="55%" :stop-color="palette.hairHighlight" />
        <stop offset="100%" :stop-color="palette.hair" />
      </linearGradient>
      <!-- 虹膜：上深下浅 -->
      <radialGradient :id="`${uid}-eye`" cx="50%" cy="30%" r="80%">
        <stop offset="0%" :stop-color="palette.eyesHighlight" />
        <stop offset="55%" :stop-color="palette.eyes" />
        <stop offset="100%" stop-color="#2b2440" />
      </radialGradient>
      <!-- 服装渐变 -->
      <linearGradient :id="`${uid}-outfit`" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" :stop-color="palette.outfit" />
        <stop offset="100%" :stop-color="palette.outfitAccent" />
      </linearGradient>
    </defs>

    <!-- ══════════ 后发（最底层，分层：外层） ══════════ -->
    <g class="anime-back-hair">
      <!-- 后发主体：从头后垂落至腰下 -->
      <path
        d="M 62 104 C 58 52, 88 28, 120 28 C 152 28, 182 52, 178 104 C 184 160, 180 220, 172 268 C 150 278, 90 278, 68 268 C 60 220, 56 160, 62 104 Z"
        :fill="`url(#${uid}-hair)`"
      />
      <!-- 发梢暗部阴影线 -->
      <path d="M 72 250 C 74 220, 76 190, 74 160" stroke="rgba(0,0,0,0.12)" stroke-width="3" fill="none" stroke-linecap="round" />
      <path d="M 168 250 C 166 220, 164 190, 166 160" stroke="rgba(0,0,0,0.12)" stroke-width="3" fill="none" stroke-linecap="round" />

      <!-- 发型变体：长发（双侧超长鬓发） -->
      <template v-if="character.hairstyle === 'long'">
        <path d="M 60 110 C 54 150, 56 210, 62 286 C 66 296, 76 296, 78 284 C 72 210, 72 150, 74 112 Z" :fill="`url(#${uid}-hair)`" />
        <path d="M 180 110 C 186 150, 184 210, 178 286 C 174 296, 164 296, 162 284 C 168 210, 168 150, 166 112 Z" :fill="`url(#${uid}-hair)`" />
      </template>
      <!-- 双马尾 -->
      <template v-else-if="character.hairstyle === 'twin'">
        <path d="M 64 66 C 40 84, 28 124, 34 176 C 36 190, 50 192, 52 176 C 48 138, 56 104, 72 84 Z" :fill="`url(#${uid}-hair)`" />
        <path d="M 176 66 C 200 84, 212 124, 206 176 C 204 190, 190 192, 188 176 C 192 138, 184 104, 168 84 Z" :fill="`url(#${uid}-hair)`" />
      </template>
      <!-- 高马尾 -->
      <template v-else-if="character.hairstyle === 'ponytail'">
        <path d="M 118 30 C 112 6, 136 2, 146 12 C 166 26, 180 48, 184 74 C 186 86, 178 92, 168 88 C 156 78, 140 56, 128 38 Z" :fill="`url(#${uid}-hair)`" />
      </template>
      <!-- 卷发：发卷团 -->
      <template v-else-if="character.hairstyle === 'curly'">
        <circle cx="56" cy="120" r="20" :fill="`url(#${uid}-hair)`" />
        <circle cx="184" cy="120" r="20" :fill="`url(#${uid}-hair)`" />
        <circle cx="62" cy="150" r="15" :fill="`url(#${uid}-hair)`" />
        <circle cx="178" cy="150" r="15" :fill="`url(#${uid}-hair)`" />
      </template>
      <!-- 古风盘发：头顶发髻（发髻 + 高光，参考图古典盘发） -->
      <template v-else-if="character.hairstyle === 'gufeng-bun'">
        <circle cx="120" cy="38" r="21" :fill="`url(#${uid}-hair)`" />
        <path d="M 100 32 C 106 22, 134 22, 140 32 C 132 26, 108 26, 100 32 Z" :fill="palette.hairHighlight" opacity="0.45" />
        <!-- 发髻底部收发包 -->
        <path d="M 96 52 C 100 44, 140 44, 144 52 C 130 58, 110 58, 96 52 Z" :fill="`url(#${uid}-hair)`" opacity="0.9" />
      </template>
    </g>

    <!-- ══════════ 身体（3-4 头身） ══════════ -->
    <g class="anime-body">
      <!-- 腿 -->
      <path d="M 96 292 L 96 340 C 96 346, 102 348, 106 348 L 106 292 Z" :fill="palette.skin" />
      <path d="M 134 292 L 134 348 C 138 348, 144 346, 144 340 L 144 292 Z" :fill="palette.skin" />
      <!-- 袜口 -->
      <rect x="95" y="330" width="12" height="7" :fill="palette.outfitAccent" opacity="0.9" />
      <rect x="133" y="330" width="12" height="7" :fill="palette.outfitAccent" opacity="0.9" />
      <!-- 圆头鞋 -->
      <ellipse cx="101" cy="350" rx="14" ry="7.5" :fill="palette.outfitAccent" />
      <ellipse cx="139" cy="350" rx="14" ry="7.5" :fill="palette.outfitAccent" />

      <!-- 百褶裙（带褶皱阴影线） -->
      <path
        d="M 82 226 L 158 226 C 166 252, 176 274, 180 292 C 140 302, 100 302, 60 292 C 64 274, 74 252, 82 226 Z"
        :fill="`url(#${uid}-outfit)`"
      />
      <!-- 裙摆褶皱垂直线 -->
      <path d="M 92 232 L 88 290" stroke="rgba(0,0,0,0.12)" stroke-width="2.4" fill="none" />
      <path d="M 120 230 L 120 294" stroke="rgba(0,0,0,0.12)" stroke-width="2.4" fill="none" />
      <path d="M 148 232 L 152 290" stroke="rgba(0,0,0,0.12)" stroke-width="2.4" fill="none" />
      <!-- 裙摆花边 -->
      <path d="M 62 284 Q 120 298 178 284" :fill="palette.outfitAccent" opacity="0.5" />

      <!-- 上衣（衬衫） -->
      <path
        d="M 82 172 C 76 190, 76 210, 82 226 L 158 226 C 164 210, 164 190, 158 172 Z"
        :fill="`url(#${uid}-outfit)`"
      />
      <!-- 水手领 -->
      <path d="M 86 172 L 120 198 L 154 172 Z" :fill="palette.outfitAccent" />
      <path d="M 90 174 L 120 194 L 150 174 Z" :fill="palette.outfit" opacity="0.85" />

      <!-- 袖子（带翻边袖口） -->
      <path d="M 82 174 C 66 184, 60 200, 66 214 C 74 212, 82 196, 88 182 Z" :fill="`url(#${uid}-outfit)`" />
      <path d="M 158 174 C 174 184, 180 200, 174 214 C 166 212, 158 196, 152 182 Z" :fill="`url(#${uid}-outfit)`" />
      <!-- 袖口翻边 -->
      <ellipse cx="67" cy="214" rx="8" ry="5" :fill="palette.outfitAccent" />
      <ellipse cx="173" cy="214" rx="8" ry="5" :fill="palette.outfitAccent" />
      <!-- 小手 -->
      <circle cx="67" cy="222" r="6" :fill="palette.skin" />
      <circle cx="173" cy="222" r="6" :fill="palette.skin" />

      <!-- 胸前领结 / 蝴蝶结 -->
      <g :fill="palette.ribbon">
        <path d="M 120 200 L 108 209 L 120 216 Z" />
        <path d="M 120 200 L 132 209 L 120 216 Z" />
        <circle cx="120" cy="208" r="4.5" :fill="palette.ribbon" stroke="#fff" stroke-width="1" />
      </g>
    </g>

    <!-- ══════════ 头（3-4 头身：头约占 35%） ══════════ -->
    <g class="anime-head">
      <!-- 脖子 -->
      <rect x="110" y="150" width="20" height="24" rx="8" :fill="palette.skinShadow" />
      <!-- 脸 -->
      <path
        d="M 70 110 C 70 68, 92 48, 120 48 C 148 48, 170 68, 170 110 C 170 138, 146 156, 120 156 C 94 156, 70 138, 70 110 Z"
        :fill="palette.skin"
      />
      <!-- 耳朵 -->
      <ellipse cx="70" cy="114" rx="7" ry="11" :fill="palette.skin" />
      <ellipse cx="170" cy="114" rx="7" ry="11" :fill="palette.skin" />

      <!-- 中层侧发（鬓发） -->
      <g class="anime-side-hair">
        <path d="M 68 96 C 64 116, 68 138, 76 150 L 84 142 C 76 128, 74 110, 76 94 Z" :fill="`url(#${uid}-hair)`" />
        <path d="M 172 96 C 176 116, 172 138, 164 150 L 156 142 C 164 128, 166 110, 164 94 Z" :fill="`url(#${uid}-hair)`" />
      </g>

      <!-- 前发（刘海，最上层） -->
      <g class="anime-front-hair">
        <path
          d="M 64 96 C 60 46, 92 26, 120 26 C 148 26, 180 46, 176 96 C 170 72, 154 60, 134 56 C 124 54, 116 54, 106 56 C 86 60, 70 72, 64 96 Z"
          :fill="`url(#${uid}-hair)`"
        />
        <!-- 刘海高光 -->
        <path d="M 84 50 C 100 42, 140 42, 156 50 C 142 44, 98 44, 84 50 Z" :fill="palette.hairHighlight" opacity="0.7" />
      </g>

      <!-- 五官 -->
      <g class="anime-features">
        <!-- 眉毛 -->
        <g class="anime-brow" :fill="browColor" stroke="none">
          <path v-if="expr.brow === 'normal'" d="M 80 88 Q 92 83 104 87 Q 92 78 80 88 Z" />
          <path v-else-if="expr.brow === 'raise'" d="M 78 84 Q 92 74 106 80 Q 92 70 78 84 Z" />
          <path v-else d="M 78 90 Q 92 84 106 90 Q 92 80 78 90 Z" />
          <path v-if="expr.brow === 'normal'" d="M 136 87 Q 148 83 160 88 Q 148 78 136 87 Z" />
          <path v-else-if="expr.brow === 'raise'" d="M 134 80 Q 148 74 162 84 Q 148 70 134 80 Z" />
          <path v-else d="M 134 90 Q 148 84 162 90 Q 148 80 134 90 Z" />
        </g>

        <!-- 眼睛（精致大眼：上下眼睑 + 虹膜渐变 + 双高光 + 睫毛） -->
        <g class="anime-eyes" :class="{ 'no-blink': noBlink }">
          <!-- 左眼 -->
          <g :transform="`translate(0 0) ${leftEyeShift}`">
            <g v-if="expr.eyes === 'happy'">
              <path d="M 78 110 Q 92 95 106 110" :stroke="eyeLine" stroke-width="4.5" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'sad'">
              <path d="M 78 106 Q 92 119 106 106" :stroke="eyeLine" stroke-width="4.5" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'wide'">
              <ellipse cx="92" cy="108" rx="17" ry="22" fill="#fff" />
              <ellipse cx="92" cy="109" rx="12" ry="16" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="92" cy="109" rx="5.2" ry="7.2" fill="#1f2430" />
              <circle cx="96" cy="102" r="4.2" fill="#fff" opacity="0.95" />
              <circle cx="86" cy="114" r="2.4" fill="#fff" opacity="0.7" />
              <path d="M 76 94 Q 92 84 108 94" :stroke="eyeLine" stroke-width="2.6" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'sleepy'">
              <ellipse cx="92" cy="110" rx="15" ry="17" fill="#fff" />
              <ellipse cx="92" cy="112" rx="9" ry="11" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="92" cy="112" rx="4" ry="5.4" fill="#1f2430" />
              <circle cx="95" cy="108" r="2.8" fill="#fff" opacity="0.9" />
              <path d="M 76 100 Q 92 92 108 100 L 108 104 L 76 104 Z" :fill="palette.skin" />
            </g>
            <g v-else-if="expr.eyes === 'closed'">
              <path d="M 77 108 Q 92 114 107 108" :stroke="eyeLine" stroke-width="3.6" fill="none" stroke-linecap="round" />
            </g>
            <g v-else>
              <ellipse cx="92" cy="108" rx="15" ry="19" fill="#fff" />
              <ellipse cx="92" cy="109" rx="10.5" ry="14" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="92" cy="109" rx="4.6" ry="6.4" fill="#1f2430" />
              <circle cx="96" cy="103" r="3.8" fill="#fff" opacity="0.95" />
              <circle cx="87" cy="113" r="2.1" fill="#fff" opacity="0.7" />
              <path d="M 78 95 Q 92 86 106 95" :stroke="eyeLine" stroke-width="2.6" fill="none" stroke-linecap="round" />
              <path d="M 80 122 Q 92 127 104 122" :stroke="eyeLine" stroke-width="1.4" fill="none" stroke-linecap="round" opacity="0.5" />
            </g>
          </g>
          <!-- 右眼 -->
          <g :transform="`translate(0 0) ${rightEyeShift}`">
            <g v-if="expr.eyes === 'happy'">
              <path d="M 134 110 Q 148 95 162 110" :stroke="eyeLine" stroke-width="4.5" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'sad'">
              <path d="M 134 106 Q 148 119 162 106" :stroke="eyeLine" stroke-width="4.5" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'wide'">
              <ellipse cx="148" cy="108" rx="17" ry="22" fill="#fff" />
              <ellipse cx="148" cy="109" rx="12" ry="16" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="148" cy="109" rx="5.2" ry="7.2" fill="#1f2430" />
              <circle cx="152" cy="102" r="4.2" fill="#fff" opacity="0.95" />
              <circle cx="142" cy="114" r="2.4" fill="#fff" opacity="0.7" />
              <path d="M 132 94 Q 148 84 164 94" :stroke="eyeLine" stroke-width="2.6" fill="none" stroke-linecap="round" />
            </g>
            <g v-else-if="expr.eyes === 'sleepy'">
              <ellipse cx="148" cy="110" rx="15" ry="17" fill="#fff" />
              <ellipse cx="148" cy="112" rx="9" ry="11" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="148" cy="112" rx="4" ry="5.4" fill="#1f2430" />
              <circle cx="151" cy="108" r="2.8" fill="#fff" opacity="0.9" />
              <path d="M 132 100 Q 148 92 164 100 L 164 104 L 132 104 Z" :fill="palette.skin" />
            </g>
            <g v-else-if="expr.eyes === 'closed'">
              <path d="M 133 108 Q 148 114 163 108" :stroke="eyeLine" stroke-width="3.6" fill="none" stroke-linecap="round" />
            </g>
            <g v-else>
              <ellipse cx="148" cy="108" rx="15" ry="19" fill="#fff" />
              <ellipse cx="148" cy="109" rx="10.5" ry="14" :fill="`url(#${uid}-eye)`" />
              <ellipse cx="148" cy="109" rx="4.6" ry="6.4" fill="#1f2430" />
              <circle cx="152" cy="103" r="3.8" fill="#fff" opacity="0.95" />
              <circle cx="143" cy="113" r="2.1" fill="#fff" opacity="0.7" />
              <path d="M 134 95 Q 148 86 162 95" :stroke="eyeLine" stroke-width="2.6" fill="none" stroke-linecap="round" />
              <path d="M 136 122 Q 148 127 160 122" :stroke="eyeLine" stroke-width="1.4" fill="none" stroke-linecap="round" opacity="0.5" />
            </g>
          </g>
        </g>

        <!-- 鼻子（简化小点） -->
        <circle cx="120" cy="124" r="1.8" :fill="palette.skinShadow" opacity="0.8" />

        <!-- 腮红（淡粉椭圆） -->
        <ellipse cx="84" cy="126" rx="10" ry="5" :fill="palette.blush" :opacity="blushOpacity" />
        <ellipse cx="156" cy="126" rx="10" ry="5" :fill="palette.blush" :opacity="blushOpacity" />

        <!-- 嘴（随表情切换，说话时开合） -->
        <g class="anime-mouth">
          <path v-if="expr.mouth === 'smile'" d="M 110 132 Q 120 139 130 132" :stroke="mouthColor" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <g v-else-if="expr.mouth === 'open'">
            <path d="M 108 129 Q 120 146 132 129 Z" :fill="mouthColor" />
            <ellipse cx="120" cy="136" rx="6.5" ry="4.5" fill="#ff9c8a" />
          </g>
          <path v-else-if="expr.mouth === 'pout'" d="M 110 138 Q 120 131 130 138" :stroke="mouthColor" stroke-width="3.5" fill="none" stroke-linecap="round" />
          <circle v-else-if="expr.mouth === 'o'" cx="120" cy="133" r="6.5" :fill="mouthColor" />
          <path v-else-if="expr.mouth === 'flat'" d="M 110 134 L 130 134" :stroke="mouthColor" stroke-width="3.4" fill="none" stroke-linecap="round" />
          <path v-else-if="expr.mouth === 'sleep'" d="M 114 134 Q 120 137 126 134" :stroke="mouthColor" stroke-width="3" fill="none" stroke-linecap="round" />
          <path v-else d="M 114 134 Q 120 138 126 134" :stroke="mouthColor" stroke-width="3" fill="none" stroke-linecap="round" />
        </g>
      </g>

      <!-- 配件 -->
      <g v-if="character.accessory && character.accessory !== 'none'" class="anime-accessory">
        <g v-if="character.accessory === 'bow'">
          <path d="M 150 44 L 138 34 L 140 50 Z" :fill="palette.ribbon" />
          <path d="M 150 44 L 162 34 L 160 50 Z" :fill="palette.ribbon" />
          <circle cx="150" cy="44" r="4.5" :fill="palette.ribbon" stroke="#fff" stroke-width="1" />
        </g>
        <g v-else-if="character.accessory === 'cat-ears'">
          <path d="M 64 56 L 54 18 L 86 46 Z" :fill="palette.hair" />
          <path d="M 62 46 L 57 26 L 72 43 Z" :fill="palette.ribbon" opacity="0.8" />
          <path d="M 176 56 L 186 18 L 154 46 Z" :fill="palette.hair" />
          <path d="M 178 46 L 183 26 L 168 43 Z" :fill="palette.ribbon" opacity="0.8" />
        </g>
        <g v-else-if="character.accessory === 'ribbon'">
          <path d="M 88 34 Q 120 26 152 34 L 150 27 Q 120 19 90 27 Z" :fill="palette.ribbon" />
          <path d="M 152 32 L 166 20 L 161 35 Z" :fill="palette.ribbon" />
        </g>
        <g v-else-if="character.accessory === 'headphones'">
          <path d="M 70 104 A 56 56 0 0 1 170 104 L 164 104 A 50 50 0 0 0 76 104 Z" :fill="palette.outfitAccent" />
          <ellipse cx="70" cy="112" rx="9" ry="15" :fill="palette.outfitAccent" />
          <ellipse cx="170" cy="112" rx="9" ry="15" :fill="palette.outfitAccent" />
        </g>
        <g v-else-if="character.accessory === 'crown'">
          <path d="M 86 38 L 94 22 L 105 35 L 120 18 L 135 35 L 146 22 L 154 38 Z" fill="#ffd700" stroke="#e6b800" stroke-width="1.5" />
          <circle cx="120" cy="15" r="3" fill="#ff6f9c" />
        </g>
        <g v-else-if="character.accessory === 'flower'">
          <g :fill="palette.ribbon">
            <circle cx="150" cy="38" r="6" />
            <circle cx="144" cy="44" r="6" />
            <circle cx="156" cy="44" r="6" />
            <circle cx="144" cy="32" r="6" />
            <circle cx="156" cy="32" r="6" />
          </g>
          <circle cx="150" cy="38" r="4.5" fill="#ffe066" stroke="#fff" stroke-width="1" />
        </g>
        <g v-else-if="character.accessory === 'star-hairpin'">
          <path d="M 146 28 L 149 36 L 158 37 L 151 43 L 153 52 L 146 47 L 139 52 L 141 43 L 134 37 L 143 36 Z" fill="#ffe066" stroke="#f5c518" stroke-width="1" />
        </g>
        <g v-else-if="character.accessory === 'jade-hairpin'">
          <!-- 玉簪：斜插发髻 + 簪头玉珠 + 珠坠流苏（参考图玉质配饰） -->
          <path d="M 106 28 L 140 54" stroke="#a8d8c8" stroke-width="4.5" stroke-linecap="round" />
          <circle cx="105" cy="27" r="5.5" fill="#a8d8c8" stroke="#7fc8b8" stroke-width="1.5" />
          <circle cx="146" cy="58" r="3.2" fill="#a8d8c8" />
          <circle cx="152" cy="66" r="2.6" fill="#a8d8c8" />
          <circle cx="157" cy="74" r="2.2" fill="#a8d8c8" />
        </g>
      </g>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { getAnimeExpression, isEyesClosed } from '@/utils/animeElfUtils'

const props = defineProps({
  character: { type: Object, required: true },
  state: { type: String, default: 'idle' },
  uid: { type: String, default: 'ae' },
  // 说话中：嘴部开合动画
  talking: { type: Boolean, default: false },
  // 随机待机动作提示（yawn/stretch/look-around/blink/sway）
  idleAction: { type: String, default: '' }
})

const palette = computed(() => props.character.palette || {})
const expr = computed(() => getAnimeExpression(props.state))
const eyeLine = computed(() => palette.value.eyes || '#3a3a5c')
const mouthColor = computed(() => palette.value.outfitAccent || '#c96a4a')
const browColor = computed(() => '#4a3a2f')

// happy/sad/wide/closed 表情不叠加 CSS 眨眼动画（形状本身已表达）
const noBlink = computed(() => {
  const e = expr.value.eyes
  return e === 'happy' || e === 'sad' || e === 'wide' || e === 'closed'
})

const blushOpacity = computed(() => (props.state === 'happy' ? 0.65 : 0.4))

// 思考/惊讶时微幅错位，增强生动感
const leftEyeShift = computed(() => {
  if (props.state === 'thinking') return 'rotate(-2 92 108)'
  if (props.state === 'surprised') return 'translate(-1 0)'
  return ''
})
const rightEyeShift = computed(() => {
  if (props.state === 'thinking') return 'rotate(2 148 108)'
  if (props.state === 'surprised') return 'translate(1 0)'
  return ''
})

// 供外部判断是否闭眼（调试/测试辅助）
defineExpose({ isEyesClosed: () => isEyesClosed(props.state) })
</script>

<style scoped>
.anime-elf {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

/* ── 呼吸起伏：idle 身体 scaleY 0.985~1.015，周期 ~3s ── */
.anime-body {
  transform-origin: 50% 100%;
  animation: animeBreath 3s ease-in-out infinite;
}
@keyframes animeBreath {
  0%, 100% { transform: scaleY(0.985); }
  50% { transform: scaleY(1.015); }
}
/* 睡觉时呼吸更慢更沉 */
.state-sleep .anime-body { animation-duration: 5s; }
/* 跳舞时身体上下弹跳 */
.state-dance .anime-body { animation: animeDanceBob 0.5s ease-in-out infinite; }
@keyframes animeDanceBob {
  0%, 100% { transform: translateY(0) scaleY(1); }
  50% { transform: translateY(-6px) scaleY(1.03); }
}
/* 跳跃：抛物线 + 落地压缩 */
.state-jump .anime-body { animation: animeJump 0.6s ease-in-out infinite; }
@keyframes animeJump {
  0%, 100% { transform: translateY(0) scaleY(0.96); }
  40% { transform: translateY(-18px) scaleY(1.04); }
  70% { transform: translateY(0) scaleY(0.98); }
}
/* 走路：左右摇摆 + 小幅弹跳 */
.state-walk .anime-body,
.state-run .anime-body { animation: animeWalk 0.4s ease-in-out infinite; }
@keyframes animeWalk {
  0%, 100% { transform: translateX(0) rotate(-2deg) scaleY(0.99); }
  25% { transform: translateX(-2px) rotate(0deg) scaleY(1.02); }
  50% { transform: translateX(0) rotate(2deg) scaleY(0.99); }
  75% { transform: translateX(2px) rotate(0deg) scaleY(1.02); }
}
.state-run .anime-body { animation-duration: 0.22s; }

/* 眨眼：约 3.8s 一次，闭合瞬间 scaleY 压扁 */
.anime-eyes {
  transform-origin: 50% 88%;
  animation: animeBlink 3.8s ease-in-out infinite;
}
.anime-eyes.no-blink { animation: none; }
@keyframes animeBlink {
  0%, 91%, 100% { transform: scaleY(1); }
  94%, 97% { transform: scaleY(0.1); }
}

/* 后发缓慢左右摆动（发摆动画，原点在头顶） */
.anime-back-hair {
  transform-origin: 50% 10%;
  animation: hairSway 4.6s ease-in-out infinite;
}
@keyframes hairSway {
  0%, 100% { transform: rotateZ(0deg); }
  30% { transform: rotateZ(1.6deg); }
  70% { transform: rotateZ(-1.6deg); }
}
/* 双马尾/卷发团额外摆动 */
.hair-twin .anime-back-hair,
.hair-curly .anime-back-hair { animation-duration: 3.4s; }

/* 头部缓慢微摆 */
.anime-head {
  transform-origin: 50% 98%;
  animation: headSway 6.5s ease-in-out infinite;
}
@keyframes headSway {
  0%, 100% { transform: rotateZ(0deg); }
  35% { transform: rotateZ(1deg); }
  75% { transform: rotateZ(-1deg); }
}

/* 说话口型：嘴部快速开合 */
.anime-mouth {
  transform-origin: 50% 50%;
}
.is-talking .anime-mouth {
  animation: mouthTalk 0.28s ease-in-out infinite;
}
@keyframes mouthTalk {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(1.6); }
}

/* 随机待机动作：打哈欠（张嘴眯眼） */
.idle-action-yawn .anime-mouth { animation: yawnMouth 1.8s ease-in-out; }
@keyframes yawnMouth {
  0% { transform: scaleY(1); }
  40% { transform: scaleY(2.2); }
  100% { transform: scaleY(1); }
}
/* 伸懒腰：身体上提 */
.idle-action-stretch .anime-body { animation: stretchBody 1.6s ease-in-out; }
@keyframes stretchBody {
  0% { transform: scaleY(1); }
  50% { transform: translateY(-8px) scaleY(1.05); }
  100% { transform: scaleY(1); }
}
/* 转头：头左右看 */
.idle-action-look-around .anime-head { animation: lookAround 1.6s ease-in-out; }
@keyframes lookAround {
  0%, 100% { transform: rotateZ(0deg); }
  30% { transform: rotateZ(6deg); }
  70% { transform: rotateZ(-6deg); }
}
/* 点头：头部上下连点 */
.idle-action-nod .anime-head { animation: nodHead 1.2s ease-in-out; }
@keyframes nodHead {
  0%, 100% { transform: rotateZ(0deg); }
  25% { transform: rotateZ(-12deg); }
  50% { transform: rotateZ(0deg); }
  75% { transform: rotateZ(-12deg); }
}
/* 摇头：头部左右摆 */
.idle-action-shake-head .anime-head { animation: shakeHead 1.4s ease-in-out; }
@keyframes shakeHead {
  0%, 100% { transform: rotateZ(0deg); }
  25% { transform: rotateZ(14deg); }
  50% { transform: rotateZ(-14deg); }
  75% { transform: rotateZ(10deg); }
}
/* 挥手：单臂摆动 */
.idle-action-wave .anime-arm { animation: waveArm 1.8s ease-in-out; }
@keyframes waveArm {
  0%, 100% { transform: rotateZ(0deg); }
  50% { transform: rotateZ(-18deg); }
}
/* 睡觉时关闭所有摆动 */
.state-sleep .anime-back-hair,
.state-sleep .anime-head,
.state-sleep .anime-body {
  animation-play-state: paused;
}
</style>

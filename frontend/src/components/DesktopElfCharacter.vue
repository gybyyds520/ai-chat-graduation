<template>
  <div 
    class="desktop-elf" 
    ref="elfRootRef"
    :class="{ dragging: isDragging, 'elf-night': elfIsDark, 'elf-light': !elfIsDark }"
    :style="elfPositionStyle"
    @click="handleClick"
    @dblclick="handleDoubleClick"
    @contextmenu="handleContextMenu"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @mousedown="handleMouseDown"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
    @touchcancel="handleTouchCancel"
    @dragover.prevent="handleElfDragOver"
    @drop.prevent="handleElfDrop"
  >
    <div class="elf-container">
      <div class="elf-character" :class="{ walking: isWalking && !isJumping, jumping: isJumping, dragging: isDragging, 'drop-bounce': isDropping, 'press-bounce': isPressBounce, 'walking-left': isWalking && moveDir < 0 }" :style="[characterStyle, elfSizedStyle]">
        <!-- 3D 渲染容器：mode=3d 且渲染器就绪时显示（二次元少女走程序化 3D 建模） -->
        <div v-show="is3DActive" ref="elf3dContainer" class="elf-3d-container"></div>
        <!-- 精灵形象：优先显示用户上传的自定义图片（customImage，任意角色均生效） -->
        <!-- 无自定义图时：二次元角色显示 AI 精绘 PNG / AnimeElfSprite，其他角色走程序化 SVG -->
        <img
          v-if="!is3DActive && spriteImageUrl"
          :src="spriteImageUrl"
          class="anime-sprite anime-sprite-img"
          :style="animeSpriteScaleStyle"
          draggable="false"
          alt=""
        />
        <AnimeElfSprite
          v-else-if="isAnimeCharacter && !is3DActive"
          :character="currentCharacter"
          :state="currentState"
          :uid="currentCharacter.id"
          :talking="!!(interaction.activeDialog && interaction.activeDialog.value) || chatTalking"
          :idle-action="(animate.idleAction && animate.idleAction.value) || ''"
          class="anime-sprite"
          :style="animeSpriteScaleStyle"
        />
        <svg
          v-if="!is3DActive && !isAnimeCharacter && !spriteImageUrl"
          viewBox="0 0 200 200"
          class="elf-svg"
          :style="{ filter: glowFilter }"
        >
          <defs>
            <radialGradient id="bodyGradient" cx="40%" cy="30%" r="60%">
              <stop offset="0%" :stop-color="highlightColor" />
              <stop offset="60%" :stop-color="currentColor" />
              <stop offset="100%" :stop-color="shadowColor" />
            </radialGradient>
            <radialGradient id="faceGradient" cx="50%" cy="40%" r="55%">
              <stop offset="0%" stop-color="#fff5e6" />
              <stop offset="50%" stop-color="#ffe4c4" />
              <stop offset="100%" stop-color="#ffdab9" />
            </radialGradient>
            <linearGradient id="hairGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#4a4a4a" />
              <stop offset="100%" stop-color="#2d2d2d" />
            </linearGradient>
            <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
            <filter id="strongGlow" x="-100%" y="-100%" width="300%" height="300%">
              <feGaussianBlur stdDeviation="8" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
            <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="4" stdDeviation="3" flood-color="rgba(0,0,0,0.3)"/>
            </filter>
          </defs>
          
          <g :transform="bodyTransform">
            <ellipse 
              cx="100" cy="150" rx="35" ry="8" 
              :fill="shadowColor" opacity="0.3"
            />
            
            <g v-if="currentCharacter.shape === 'humanoid'" class="humanoid-body">
              <template v-if="currentCharacter.id.startsWith('zodiac-') || currentCharacter.id.startsWith('myth-')">
                <text x="100" y="100" text-anchor="middle" dominant-baseline="middle" font-size="80">{{ currentCharacter.emoji }}</text>
              </template>
              <template v-else>
                <ellipse cx="100" cy="125" rx="35" ry="40" :fill="bodyGradient" :stroke="borderColor" stroke-width="2" filter="url(#shadow)"/>
                <ellipse cx="100" cy="75" rx="32" ry="30" fill="url(#faceGradient)" :stroke="borderColor" stroke-width="1.5" filter="url(#shadow)"/>
                <ellipse cx="100" cy="60" rx="28" ry="18" fill="url(#hairGradient)" stroke="#1a1a1a" stroke-width="1"/>
                <ellipse cx="70" cy="110" rx="12" ry="20" :fill="bodyGradient" :stroke="borderColor" stroke-width="1.5" transform="rotate(-15 70 110)"/>
                <ellipse cx="130" cy="110" rx="12" ry="20" :fill="bodyGradient" :stroke="borderColor" stroke-width="1.5" transform="rotate(15 130 110)"/>
                <ellipse cx="75" cy="150" rx="10" ry="15" :fill="bodyGradient" :stroke="borderColor" stroke-width="1.5"/>
                <ellipse cx="125" cy="150" rx="10" ry="15" :fill="bodyGradient" :stroke="borderColor" stroke-width="1.5"/>
              </template>
            </g>
            
            <g v-else class="basic-shape">
              <circle v-if="currentCharacter.shape === 'sphere'" cx="100" cy="100" r="50" :fill="bodyGradient"/>
              <rect v-else-if="currentCharacter.shape === 'cube'" x="50" y="50" width="100" height="100" rx="10" :fill="bodyGradient"/>
              <polygon v-else-if="currentCharacter.shape === 'star'" points="100,10 120,80 190,80 130,120 150,190 100,150 50,190 70,120 10,80 80,80" :fill="bodyGradient"/>
              <path v-else-if="currentCharacter.shape === 'heart'" d="M100,30 Q100,10 80,10 Q60,10 60,30 Q60,55 100,90 Q140,55 140,30 Q140,10 120,10 Q100,10 100,30" :fill="bodyGradient"/>
              <polygon v-else-if="currentCharacter.shape === 'diamond'" points="100,10 190,100 100,190 10,100" :fill="bodyGradient"/>
            </g>
            
            <g class="face" :style="faceStyle" v-if="!isEmojiCharacter">
              <g class="eyes" :style="animate.eyesStyle">
                <ellipse cx="82" cy="72" rx="7" ry="9" :fill="skinColors.eyes"/>
                <ellipse cx="118" cy="72" rx="7" ry="9" :fill="skinColors.eyes"/>
                <circle cx="84" cy="70" r="2.5" fill="#ffffff"/>
                <circle cx="120" cy="70" r="2.5" fill="#ffffff"/>
                <ellipse cx="85" cy="71" rx="1.5" ry="2" fill="#000000"/>
                <ellipse cx="121" cy="71" rx="1.5" ry="2" fill="#000000"/>
              </g>
              
              <path 
                class="mouth" 
                :d="mouthPath" 
                :fill="skinColors.mouth"
                :style="animate.mouthStyle"
              />
              
              <g v-if="currentState === 'happy'" class="blush">
                <ellipse cx="68" cy="82" rx="7" ry="4" fill="#ffb6c1" opacity="0.5"/>
                <ellipse cx="132" cy="82" rx="7" ry="4" fill="#ffb6c1" opacity="0.5"/>
              </g>
              
              <ellipse cx="72" cy="88" rx="10" ry="5" fill="#ffdab9" opacity="0.6"/>
              <ellipse cx="128" cy="88" rx="10" ry="5" fill="#ffdab9" opacity="0.6"/>
            </g>
            
            <g v-if="currentOutfit.id !== 'none'" class="outfit" :transform="outfitTransform">
              <text v-if="currentOutfit.emoji" x="100" y="100" text-anchor="middle" font-size="40">{{ currentOutfit.emoji }}</text>
            </g>
          </g>
        </svg>
        
        <div v-if="interaction.activeEmotion && interaction.activeEmotion.value" class="emotion-emoji">
          {{ interaction.activeEmotion.value.emoji }}
        </div>
        
        <div v-if="currentCharacter.specialEffect" class="special-effect" :class="currentCharacter.specialEffect">
          <span v-if="currentCharacter.specialEffect === 'sparkle'">✨</span>
          <span v-else-if="currentCharacter.specialEffect === 'fire'">🔥</span>
          <span v-else-if="currentCharacter.specialEffect === 'wind'">💨</span>
          <span v-else-if="currentCharacter.specialEffect === 'rainbow'">🌈</span>
          <span v-else-if="currentCharacter.specialEffect === 'water'">💧</span>
          <span v-else-if="currentCharacter.specialEffect === 'lightning'">⚡</span>
          <span v-else-if="currentCharacter.specialEffect === 'cloud'">☁️</span>
          <span v-else-if="currentCharacter.specialEffect === 'sun'">☀️</span>
        </div>

        <!-- 桌宠特效层：爱心/星星/音符/睡眠 zZ 飘升 -->
        <div v-if="isAnimeCharacter" class="elf-fx-layer">
          <span
            v-for="fx in spriteEffects"
            :key="fx.id"
            class="elf-fx"
            :class="`fx-${fx.kind}`"
            :style="{ left: fx.x + '%' }"
          >{{ fx.emoji }}</span>
        </div>
      </div>

        <div class="elf-shadow"></div>

      <div v-if="interaction.activeDialog && interaction.activeDialog.value && elfConfig.bubbleEnabled" class="dialog-bubble" :style="{ '--bubble-accent': bubbleAccent }">
        <div class="dialog-content">{{ interaction.activeDialog.value.content }}</div>
        <div class="dialog-arrow"></div>
      </div>

      <!-- 双击快捷菜单 -->
      <div v-if="quickMenuOpen && isAnimeCharacter" class="elf-quick-menu" @click.stop>
        <button class="quick-menu-item" @click="cycleCharacter(-1)">⟵ 上一角色</button>
        <button class="quick-menu-item" @click="cycleCharacter(1)">下一角色 ⟶</button>
        <button class="quick-menu-item" @click="quickFeed">🍚 喂食</button>
        <button class="quick-menu-item" @click="quickPat">🫳 摸摸头</button>
        <button class="quick-menu-item" @click="openActMenu">🎬 动作表演</button>
        <button class="quick-menu-item" @click="openSizeMenu">📏 大小</button>
        <button class="quick-menu-item" @click="randomAction">🎲 随机动作</button>
        <button class="quick-menu-item" @click="openImagePicker">🖼️ 更换形象</button>
        <button class="quick-menu-item" @click="cycleOutfit">👗 换装</button>
        <button class="quick-menu-item" @click="openChatFromMenu">💬 对话</button>
        <button class="quick-menu-item" @click="startVoiceMode">🎤 语音对话</button>
        <button class="quick-menu-item" @click="openReminderPanel">⏰ 快捷提醒</button>
        <button class="quick-menu-item" @click="openConfigFromMenu">⚙️ 打开配置</button>
      </div>

      <!-- 动作表演面板：游戏角色经典动作一键演出 -->
      <div v-if="actMenuOpen && isAnimeCharacter" class="elf-act-menu" @click.stop>
        <div class="act-menu-title">🎬 动作表演</div>
        <div class="act-menu-grid">
          <button
            v-for="act in actMenuItems"
            :key="act.id"
            class="act-menu-item"
            :class="{ active: act.id === lastPlayedAction }"
            @click="playActAction(act)"
          >
            <span class="act-emoji">{{ act.emoji }}</span>
            <span class="act-label">{{ act.label }}</span>
          </button>
        </div>
        <button class="act-menu-close" @click="actMenuOpen = false">✕ 收起</button>
      </div>

      <!-- 大小调节面板：滑块 + 快捷档位，与设计器配置联动 -->
      <div v-if="sizePanelOpen && isAnimeCharacter" class="elf-size-menu" @click.stop>
        <div class="act-menu-title">📏 精灵大小</div>
        <div class="size-slider-row">
          <input
            class="size-range"
            type="range"
            min="0.4"
            max="2.5"
            step="0.1"
            :value="elfSizeRatio"
            @input="applyElfSize($event.target.value)"
          />
          <span class="size-value">{{ elfSizeRatio.toFixed(1) }}x</span>
        </div>
        <div class="size-presets">
          <button
            v-for="p in ELF_SIZE_PRESETS"
            :key="p.label"
            class="size-preset-btn"
            :class="{ active: Math.abs(elfSizeRatio - p.value) < 0.001 }"
            @click="applyElfSize(p.value)"
          >{{ p.label }}</button>
        </div>
        <button class="act-menu-close" @click="sizePanelOpen = false">✕ 收起</button>
      </div>

      <!-- 换装衣柜面板：换装小游戏级（预设套装 / 单件混搭 / 服装换色） -->
      <div v-if="outfitMenuOpen && isAnimeCharacter" class="elf-outfit-menu" @click.stop>
        <div class="act-menu-title">👗 换装衣柜</div>
        <div v-if="elfConfig.customImage" class="outfit-tip">换装将切换为程序化 3D 形象（清除自定义图片）</div>
        <div class="mix-tabs">
          <button class="mix-tab" :class="{ active: mixPanelTab === 'set' }" @click="mixPanelTab = 'set'">套装</button>
          <button class="mix-tab" :class="{ active: mixPanelTab === 'mix' }" @click="mixPanelTab = 'mix'">混搭</button>
          <button class="mix-tab" :class="{ active: mixPanelTab === 'color' }" @click="mixPanelTab = 'color'">换色</button>
        </div>

        <!-- 页签1：预设套装 -->
        <div v-if="mixPanelTab === 'set'" class="outfit-set-grid">
          <button
            v-for="s in outfitSets"
            :key="s.id"
            class="outfit-set-btn"
            :class="{ active: currentOutfitSet.id === s.id }"
            @click="selectOutfitSet(s)"
          >
            <span class="outfit-set-emoji">{{ s.emoji }}</span>
            <span class="outfit-set-name">{{ s.name }}</span>
          </button>
        </div>

        <!-- 页签2：单件混搭 -->
        <div v-if="mixPanelTab === 'mix'" class="mix-panel">
          <div v-for="part in ['top', 'bottom', 'shoes', 'socks', 'accessory']" :key="part" class="mix-row">
            <div class="mix-row-label">{{ MIX_PART_LABELS[part] }}</div>
            <div class="mix-type-list">
              <button
                v-for="t in part === 'top' ? ['shirt', 'puff', 'tshirt', 'yukata', 'pajama', 'sailor', 'dress']
                  : part === 'bottom' ? ['pleated-skirt', 'puff-skirt', 'shorts', 'straight', 'pants', 'pencil', 'flared']
                  : part === 'shoes' ? ['leather', 'maryjane', 'sneakers', 'clogs', 'slippers', 'pumps']
                  : part === 'socks' ? ['long', 'short', 'none']
                  : ['bow', 'ribbon', 'star', 'crown', 'flower', 'none']"
                :key="t"
                class="mix-type-btn"
                :class="{ active: currentOutfitSet[part] === t }"
                @click="applyMixPart(part, t)"
              >{{ part === 'accessory' && t === 'none' ? '无' : (MIX_TYPE_LABELS[t] || t) }}</button>
            </div>
          </div>
        </div>

        <!-- 页签3：服装换色 -->
        <div v-if="mixPanelTab === 'color'" class="mix-panel">
          <div v-for="part in ['top', 'bottom', 'shoes', 'socks', 'accessory']" :key="part" class="mix-row">
            <div class="mix-row-label">{{ MIX_PART_LABELS[part] }}</div>
            <div class="color-palette">
              <button
                v-for="c in ELF_OUTFIT_COLOR_PALETTES[part]"
                :key="c"
                class="color-dot"
                :style="{ background: c }"
                :class="{ active: partColorFor(part) === c }"
                :title="c"
                @click="applyPartColor(part, c)"
              />
              <button
                v-if="customOutfit && customOutfit.colors && customOutfit.colors[part]"
                class="color-reset"
                @click="resetPartColor(part)"
              >还原</button>
            </div>
          </div>
        </div>

        <button class="act-menu-close" @click="outfitMenuOpen = false">✕ 收起</button>
      </div>

      <!-- 隐藏文件选择：右键"更换形象"触发；拖拽图片到精灵上也会直接更换 -->
      <input ref="elfImageInputRef" type="file" accept="image/*" class="elf-hidden-file-input" @change="onCustomImageChange" />

      <!-- 快捷提醒面板：1/5/10/30/60 分钟 + 自定义，到点气泡提醒 -->
      <div v-if="reminderPanelOpen" class="elf-reminder-panel" :style="{ '--panel-accent': panelAccentColor }" @click.stop>
        <div class="reminder-title">⏰ 快捷提醒</div>
        <div class="reminder-quick">
          <button
            v-for="m in REMINDER_QUICK_MINUTES"
            :key="m"
            class="reminder-quick-btn"
            @click="startReminder(m)"
          >{{ m }} 分钟</button>
        </div>
        <div class="reminder-custom">
          <input
            v-model.number="reminderCustomMinutes"
            type="number"
            min="1"
            max="720"
            placeholder="自定义分钟"
            @keyup.enter="startCustomReminder"
          />
          <button class="reminder-custom-btn" @click="startCustomReminder">开始</button>
        </div>
        <div v-if="reminderActive" class="reminder-active">
          <span>⏳ {{ reminderCountdownText }}</span>
          <button class="reminder-cancel-btn" @click="cancelReminder">取消</button>
        </div>
        <button class="reminder-close-btn" @click="reminderPanelOpen = false">✕</button>
      </div>

      <!-- 语音对话模式浮层：麦克风指示 + 波形动画 + 实时识别文本 -->
      <div v-if="voiceMode" class="elf-voice-overlay" @click.stop>
        <div class="voice-wave" :class="{ listening: voiceLoop.isListening.value, speaking: voiceLoop.isSpeaking.value }">
          <span v-for="n in 5" :key="n" class="wave-bar" :style="{ animationDelay: (n * 0.12) + 's' }"></span>
        </div>
        <div class="voice-status">{{ voiceStatusText }}</div>
        <div class="voice-transcript" v-if="voiceLoop.interimTranscript.value">{{ voiceLoop.interimTranscript.value }}</div>
        <button class="voice-close-btn" @click.stop="stopVoiceMode">✕ 结束对话</button>
      </div>
      
      <div class="elf-status-bar" @click.stop="toggleTimeDisplay">
        <div class="status-dot" :class="currentMood"></div>
        <span class="status-text">{{ currentCharacter.name }}</span>
        <span class="status-sub">{{ showTime ? currentTime : currentCharacter.personality }}</span>
      </div>
      <div class="elf-growth-bar" @click.stop>
        <span class="growth-cell" title="心情">😊<i><b :style="{ width: growth.mood + '%' }"></b></i></span>
        <span class="growth-cell" title="饱腹">🍱<i><b :style="{ width: growth.fullness + '%' }"></b></i></span>
        <span class="growth-cell" title="亲密度">💗<i><b :style="{ width: Math.min(100, growth.intimacy / 10) + '%' }"></b></i></span>
        <button class="growth-feed-btn" title="喂食" @click.stop="feedNow()">🍚</button>
      </div>
      <div class="elf-soul-bar" v-if="elfSoul.hasSoul" @click.stop title="精灵灵魂：互动会塑造她的内心">
        <span class="soul-emotion" :class="'soul-' + elfSoul.emotion">✦ {{ soulEmotionLabel }}</span>
        <span class="soul-version">灵魂 Lv.{{ elfSoul.version }}</span>
      </div>
    </div>
    
    <div class="elf-panel" :class="{ open: panelOpen }" :style="[panelPosition, { '--panel-accent': panelAccentColor }]">
      <div class="panel-accent-bar"></div>
      <div class="panel-header">
        <h3>{{ currentCharacter.name }}</h3>
        <div class="panel-header-actions">
          <button class="config-center-btn" @click.stop="openDesigner" title="打开配置中心">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
            </svg>
            <span>配置中心</span>
          </button>
          <button class="close-btn" @click.stop="panelOpen = false">×</button>
        </div>
      </div>
      
      <div class="panel-tabs">
        <button 
          v-for="tab in visibleTabs" 
          :key="tab.id"
          @click="currentTab = tab.id"
          :class="{ active: currentTab === tab.id }"
        >
          {{ tab.name }}
        </button>
      </div>
      
      <div class="panel-content">
        <div v-if="currentTab === 'character'" class="tab-content">
          <!-- 自定义形象：上传图片自动渲染为 3D 立体人物（去背景立牌，优先于内置角色渲染） -->
          <div class="custom-image-section" :class="{ 'has-image': !!elfConfig.customImage }">
            <div class="custom-image-header">
              <span class="custom-image-title">🖼️ 自定义形象</span>
              <span class="custom-image-hint">上传图片自动渲染为 3D 立体人物</span>
            </div>
            <div class="custom-image-body">
              <div v-if="elfConfig.customImage" class="custom-image-preview">
                <img :src="elfConfig.customImage" alt="自定义形象预览" draggable="false" />
                <button class="custom-image-remove" @click.stop="clearCustomImage" title="恢复默认形象">✕</button>
              </div>
              <div v-else class="custom-image-empty">
                <span class="custom-image-upload-icon">＋</span>
                <span>尚未设置自定义形象</span>
              </div>
              <label class="custom-image-upload-btn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                {{ elfConfig.customImage ? '更换图片' : '上传图片' }}
                <input type="file" accept="image/*" class="custom-image-file-input" @change="onCustomImageChange" />
              </label>
              <button v-if="elfConfig.customImage" class="custom-image-reset-btn" @click.stop="clearCustomImage">
                恢复默认
              </button>
              <label class="custom-image-bg-toggle" title="开启后自动去除图片背景，立体感更好">
                <input type="checkbox" :checked="elfConfig.customImageRemoveBg !== false"
                  @change="toggleCustomImageRemoveBg" />
                <span>自动去背景</span>
              </label>
            </div>
          </div>

          <div class="category-tabs">
            <button 
              v-for="cat in categories" 
              :key="cat.id"
              @click="selectedCategory = cat.id"
              :class="{ active: selectedCategory === cat.id }"
            >
              {{ cat.name }}
            </button>
          </div>
          <div class="char-grid">
            <div 
              v-for="char in filteredCharacters" 
              :key="char.id"
              @click="selectCharacter(char)"
              :class="{ selected: currentCharacter.id === char.id }"
              class="char-card"
            >
              <div class="char-emoji">
                <AnimeElfSprite
                  v-if="char.category === 'anime'"
                  :character="char"
                  :state="'idle'"
                  :uid="char.id"
                  class="char-anime-preview"
                />
                <template v-else>{{ char.emoji }}</template>
              </div>
              <div class="char-info">
                <span class="char-name">{{ char.name }}</span>
                <span class="char-desc">{{ char.description }}</span>
              </div>
              <div v-if="char.category === 'anime'" class="char-tag anime">二次元</div>
              <div v-else-if="char.category === 'zodiac'" class="char-tag zodiac">生肖</div>
              <div v-else-if="char.category === 'myth'" class="char-tag myth">山海经</div>
            </div>
          </div>
        </div>
        
        <div v-if="currentTab === 'action'" class="tab-content">
          <div class="action-grid">
            <button
              v-for="action in actions"
              :key="action.id"
              @click="triggerAction3D(action.id)"
              :class="{ active: animate.currentState === action.id }"
              class="action-btn"
            >
              <span class="action-icon">{{ getActionIcon(action.id) }}</span>
              <span class="action-name">{{ action.name }}</span>
            </button>
          </div>
        </div>

        <div v-if="currentTab === 'chat'" class="tab-content chat-tab">
          <div v-if="isOfflineMode" class="chat-offline-hint">离线模式：未连接后端，使用预置台词回复</div>
          <div class="chat-messages" ref="chatScrollRef">
            <div v-if="chatMessages.length === 0" class="chat-empty">
              和{{ currentCharacter.name }}说点什么吧~
            </div>
            <div
              v-for="(m, i) in chatMessages"
              :key="i"
              class="chat-bubble"
              :class="m.role"
            >
              <div class="chat-bubble-content">
                {{ m.content
                }}<span v-if="m.streaming" class="chat-cursor">▍</span>
              </div>
            </div>
          </div>
          <div class="chat-input-row">
            <input
              v-model="chatInput"
              @keyup.enter="sendChatMessage"
              placeholder="说点什么..."
              :disabled="isChatStreaming"
            />
            <button class="chat-send-btn" @click="sendChatMessage" :disabled="isChatStreaming">
              {{ isChatStreaming ? '...' : '发送' }}
            </button>
          </div>
          <div class="movement-toggles">
            <label class="movement-toggle">
              <input type="checkbox" :checked="elfConfig.movementEnabled !== false" @change="toggleMovementEnabled" />
              自主巡逻
            </label>
            <label class="movement-toggle">
              <input type="checkbox" :checked="elfConfig.followMouse === true" @change="toggleFollowMouse" />
              跟随鼠标
            </label>
          </div>
        </div>
        
        <div v-if="currentTab === 'skin'" class="tab-content">
          <div class="skin-grid">
            <div 
              v-for="skin in skins" 
              :key="skin.id"
              @click="selectSkin(skin)"
              :class="{ selected: currentSkin.id === skin.id }"
              class="skin-card"
            >
              <div class="skin-preview" :style="getSkinPreviewStyle(skin)"></div>
              <span class="skin-name">{{ skin.name }}</span>
            </div>
          </div>
        </div>
        
        <div v-if="currentTab === 'outfit'" class="tab-content">
          <div class="outfit-grid">
            <div
              v-for="outfit in outfits"
              :key="outfit.id"
              @click="selectOutfit(outfit)"
              :class="{ selected: currentOutfit.id === outfit.id }"
              class="outfit-card"
            >
              <span class="outfit-emoji">{{ outfit.emoji }}</span>
              <span class="outfit-name">{{ outfit.name }}</span>
            </div>
          </div>
        </div>

        <div v-if="currentTab === 'widgets'" class="tab-content">
          <div class="widgets-header">
            <p class="widgets-desc">设计器中配置的组件列表，点击可前往配置中心编辑布局。</p>
            <button class="open-designer-btn" @click="openDesigner">
              打开配置中心
            </button>
          </div>
          <div v-if="configuredWidgets.length === 0" class="widgets-empty">
            <span>暂无组件，前往配置中心添加吧~</span>
          </div>
          <div v-else class="widgets-list">
            <div
              v-for="widget in configuredWidgets"
              :key="widget.id"
              class="widget-row"
              @click="openDesigner"
            >
              <span class="widget-row-name">
                {{ widgetTypeNames[widget.type] || widget.type }}
              </span>
              <span class="widget-row-arrow">›</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { elfCharacters, elfActions, elfSkins, elfOutfits } from '@/data/elfData'
import { elfOutfitSets, DEFAULT_OUTFIT_SET, ELF_OUTFIT_ACCESSORY_TYPES, ELF_OUTFIT_COLOR_PALETTES } from '@/data/elfOutfitSets'
import { animeElfCharacters, isAnimeCharacter as checkAnime, ANIME_ACCESSORIES } from '@/data/animeElfData'
import AnimeElfSprite from '@/components/elf/AnimeElfSprite.vue'
import { useElfAnimation } from '@/composables/useElfAnimation'
import { useElfInteraction } from '@/composables/useElfInteraction'
import { useElf3DRenderer } from '@/composables/useElf3DRenderer'
import { useElfMovement } from '@/composables/useElfMovement'
import { normalizeGaze, getIdleStage, isHeadTap, isComboClick, getTimePeriod, getInactivityLevel, ELF_ACCESSORY_SLOTS } from '@/utils/elf3dConfig'
import { getCharacterLine, getNeedHintLine, getIdleRandomAction, ANIME_REST_LINES, ANIME_ACT_MENU, ANIME_RANDOM_ACTIONS } from '@/utils/animeElfUtils'
import { useElfVoiceLoop } from '@/composables/useElfVoiceLoop'
import { buildChatStreamUrl, parseSSEBuffer, getOfflineReply } from '@/utils/elfChatSse'
import { useElfConfig } from '@/utils/elfBridge'
import { validateElfImageFile, readElfImageDataURL } from '@/utils/elfImageUtils'
import { createLogger } from '@/utils/logger'
import request from '@/utils/request'

const logger = createLogger('DesktopElf')
const router = useRouter()

// ─── 昼夜主题联动：一键白天黑夜按钮切换后，精灵场景光照/色调同步变化 ───
const { currentTheme } = useTheme()
const elfIsDark = computed(() => currentTheme.value === 'dark')

// ─── 全局配置桥接（与设计器/桌面同步） ───
const { config: elfConfig, updateConfig: updateElfConfig } = useElfConfig()
const characters = ref(elfCharacters)
const actions = ref(elfActions)
const skins = ref(elfSkins)
const outfits = ref(elfOutfits)

const currentCharacter = ref(animeElfCharacters[0])
const currentSkin = ref(elfSkins[0])
const currentOutfit = ref(elfOutfits[0])
/** 换装套装（anime 3D 角色专用，V4 换装小游戏级） */
const outfitSets = ref(elfOutfitSets)
/** 默认形象套装：古风华服（参考图青绿汉服美人，无配置时兜底） */
const DEFAULT_ELF_OUTFIT_ID = 'gufeng'
/** 当前生效套装（computed：自由混搭优先于预设套装） */
const customOutfit = ref(null)
const currentOutfitSet = computed(() => {
  if (customOutfit.value && customOutfit.value.top && customOutfit.value.colors) {
    return {
      id: 'custom',
      name: '自由搭配',
      emoji: '🎨',
      description: '单件混搭 + 自定义配色',
      ...customOutfit.value
    }
  }
  return (
    outfitSets.value.find(s => s.id === elfConfig.value.outfitSetId)
    || outfitSets.value.find(s => s.id === DEFAULT_ELF_OUTFIT_ID)
    || DEFAULT_OUTFIT_SET
  )
})
/** 换装面板页签：set=预设套装 / mix=单件混搭 / color=服装换色 */
const mixPanelTab = ref('set')
/** 换装面板开关（快捷菜单"👗 换装"打开） */
const outfitMenuOpen = ref(false)
/** 混搭部件中文名映射 */
const MIX_PART_LABELS = {
  top: '上衣',
  bottom: '下装',
  shoes: '鞋',
  socks: '袜子',
  accessory: '发饰'
}
/** 混搭部件类型中文名映射 */
const MIX_TYPE_LABELS = {
  shirt: '衬衫', puff: '泡泡袖', tshirt: 'T恤', yukata: '浴衣', pajama: '睡衣', sailor: '水手服', dress: '连衣裙',
  'pleated-skirt': '百褶裙', 'puff-skirt': '蓬蓬裙', shorts: '短裤', straight: '长下摆', pants: '睡裤', pencil: '铅笔裙', flared: 'A字裙',
  leather: '小皮鞋', maryjane: '玛丽珍', sneakers: '运动鞋', clogs: '木屐', slippers: '拖鞋', pumps: '高跟鞋',
  long: '长袜', short: '短袜', none: '无袜',
  bow: '蝴蝶结', ribbon: '发带', star: '星星', crown: '皇冠', flower: '花环'
}
const panelOpen = ref(false)
const currentTab = ref('character')
const selectedCategory = ref('anime')

// ─── 桌宠互动增强：特效层 + 双击快捷菜单 ───
const spriteEffects = ref([])
const quickMenuOpen = ref(false)
/** 动作表演面板开关 + 动作菜单数据 + 最近播放动作（高亮） */
const actMenuOpen = ref(false)
const actMenuItems = ANIME_ACT_MENU
const lastPlayedAction = ref('')
/** 大小调节面板开关 */
const sizePanelOpen = ref(false)
let fxSeq = 0
let clickTimer = null

// 快捷菜单与动作面板互斥：任一打开时关闭另一个
watch([quickMenuOpen, actMenuOpen, sizePanelOpen, outfitMenuOpen], ([qm, am, sp, om]) => {
  if (qm && am) actMenuOpen.value = false
  if (qm && sp) sizePanelOpen.value = false
  if (qm && om) outfitMenuOpen.value = false
})

/** 隐藏文件选择器（右键"更换形象"触发；拖拽图片到精灵上也会直接更换） */
const elfImageInputRef = ref(null)

/**
 * 快捷菜单：更换形象（触发隐藏文件选择，复用上传/压缩/持久化逻辑）
 */
function openImagePicker() {
  quickMenuOpen.value = false
  const input = elfImageInputRef.value
  if (input) { try { input.click() } catch (e) {} }
}

/**
 * 拖拽图片到精灵上：只拦截文件拖入（不拦截文本/链接拖拽）
 */
function handleElfDragOver(e) {
  if (e.dataTransfer && e.dataTransfer.types && e.dataTransfer.types.includes('Files')) {
    e.preventDefault()
  }
}

/**
 * 拖拽放下：把第一张图片作为自定义形象（与上传同一校验/压缩/持久化链路）
 */
async function handleElfDrop(e) {
  const dt = e.dataTransfer
  if (!dt || !dt.files || dt.files.length === 0) return
  const file = dt.files[0]
  const check = validateElfImageFile(file)
  if (!check.ok) {
    const msg = check.reason === 'not_image'
      ? '请拖入图片文件哦～'
      : check.reason === 'too_large'
        ? '图片太大了，请选择 5MB 以内的图片～'
        : '请先准备一张图片～'
    try { interaction.showDialog(msg, 2000) } catch (err) {}
    return
  }
  try {
    const dataUrl = await readElfImageDataURL(file)
    updateElfConfig({ customImage: dataUrl })
    try { interaction.showDialog('形象更新成功～', 1800) } catch (err) {}
  } catch (err) {
    logger.warn('拖拽更换形象读取失败', err)
    try { interaction.showDialog('图片读取失败，换一张试试吧～', 2000) } catch (err2) {}
  }
}

// ─── 点击按压回弹（Squash & Stretch 手感） ───
const isPressBounce = ref(false)
let pressBounceTimer = null

// ─── 快捷提醒面板（1/5/10/30/60 分钟 + 自定义，到点气泡提醒） ───
const REMINDER_QUICK_MINUTES = [1, 5, 10, 30, 60]
const reminderPanelOpen = ref(false)
const reminderCustomMinutes = ref(30)
const reminderActive = ref(false)
const reminderCountdownText = ref('')
let reminderTimer = null
let reminderTickTimer = null
let reminderEndAt = 0

/** 触发一次按压回弹动画（240ms 压缩-超调-复位） */
function triggerPressBounce() {
  if (isPressBounce.value) return
  isPressBounce.value = true
  if (pressBounceTimer) clearTimeout(pressBounceTimer)
  pressBounceTimer = setTimeout(() => {
    isPressBounce.value = false
    pressBounceTimer = null
  }, 240)
}

/** 右键（桌面端）：弹出快捷菜单，替代浏览器默认菜单 */
function handleContextMenu(event) {
  if (event.target.closest('.elf-panel') || event.target.closest('.elf-quick-menu') || event.target.closest('.elf-reminder-panel')) return
  event.preventDefault()
  if (isDragging.value) return
  if (isAnimeCharacter.value) {
    quickMenuOpen.value = !quickMenuOpen.value
  } else {
    togglePanel()
  }
}

/** 快捷菜单：喂食（动作 + 数据 + 台词反馈） */
function quickFeed() {
  quickMenuOpen.value = false
  triggerAction3D('feed')
  spawnEffect('star', '🍱')
  try { interaction.showDialog('好耶，谢谢你喂我～', 1600) } catch (e) {}
  feedNow()
}

/** 快捷菜单：摸摸头（动作 + 数据 + 台词反馈） */
function quickPat() {
  quickMenuOpen.value = false
  triggerAction3D('pat')
  spawnEffect('heart', '❤️')
  try { interaction.showDialog('好舒服～再摸摸嘛', 1600) } catch (e) {}
  registerPatGrowth()
}

/** 打开快捷提醒面板 */
function openReminderPanel() {
  quickMenuOpen.value = false
  reminderPanelOpen.value = true
  updatePanelPosition()
}

/** 启动提醒（分钟数） */
function startReminder(minutes) {
  const m = Math.max(1, Math.min(720, Math.floor(Number(minutes) || 1)))
  reminderEndAt = Date.now() + m * 60000
  reminderActive.value = true
  reminderPanelOpen.value = false
  updateReminderCountdown()
  if (reminderTickTimer) clearInterval(reminderTickTimer)
  reminderTickTimer = setInterval(updateReminderCountdown, 1000)
  if (reminderTimer) clearTimeout(reminderTimer)
  reminderTimer = setTimeout(() => {
    fireReminder(m)
  }, m * 60000)
  try { interaction.showDialog(`好的，${m} 分钟后提醒你～`, 1800) } catch (e) {}
}

/** 自定义分钟提醒 */
function startCustomReminder() {
  const v = Number(reminderCustomMinutes.value)
  if (!v || v < 1) {
    try { interaction.showDialog('请输入 1~720 之间的分钟数哦～', 1800) } catch (e) {}
    return
  }
  startReminder(Math.min(720, Math.floor(v)))
}

/** 倒计时文案（供面板显示，纯函数式更新） */
function updateReminderCountdown() {
  const remain = Math.max(0, reminderEndAt - Date.now())
  if (remain <= 0) { reminderCountdownText.value = '马上到啦！'; return }
  const totalSec = Math.ceil(remain / 1000)
  const mm = Math.floor(totalSec / 60)
  const ss = totalSec % 60
  reminderCountdownText.value = `${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

/** 到点触发：气泡 + 特效（声音已由系统提醒承担） */
function fireReminder(minutes) {
  reminderActive.value = false
  if (reminderTickTimer) { clearInterval(reminderTickTimer); reminderTickTimer = null }
  if (reminderTimer) { clearTimeout(reminderTimer); reminderTimer = null }
  triggerAction3D('happy')
  spawnEffect('heart', '⏰')
  try { interaction.showDialog(`主人，${minutes} 分钟到啦！该休息一下咯～`, 4000) } catch (e) {}
  // 桌面端（Electron）可触发系统通知；Web 端以气泡为准
  if (window.desktopApp && typeof window.desktopApp.notify === 'function') {
    try { window.desktopApp.notify('桌宠提醒', `${minutes} 分钟到啦，休息一下吧～`) } catch (e) {}
  }
}

/** 取消提醒 */
function cancelReminder() {
  reminderActive.value = false
  reminderPanelOpen.value = false
  if (reminderTickTimer) { clearInterval(reminderTickTimer); reminderTickTimer = null }
  if (reminderTimer) { clearTimeout(reminderTimer); reminderTimer = null }
  try { interaction.showDialog('好的，取消提醒～', 1200) } catch (e) {}
}

// ─── 3D 渲染器集成（渐进式 + 优雅降级） ───
const elf3dContainer = ref(null)
// 传给 3D 渲染器的配置（渲染器每帧读取 .value）
const threeConfig = ref({
  autoRotate: elfConfig.value.autoRotate !== false,
  rotateSpeed: 0.005,
  shadowEnabled: true,
  backgroundColor: 0x1a1a2e,
  size: elfConfig.value.size || 1.0
})
// useElf3DRenderer 必须在 setup 期间同步调用，以便其内部 onMounted/onUnmounted 绑定本组件
const threeRenderer = useElf3DRenderer(elf3dContainer, threeConfig)

// 是否真正显示 3D：模式为 3d 且渲染器就绪。
// 自定义图片存在时也进入 3D（自动去背景后渲染为 3D 立体立牌，不再退回 2D 图片直显）；
// 3D 不可用（WebGL 失败/模式 2d）时自动降级 2D 立绘。
const is3DActive = computed(() =>
  elfConfig.value.mode === '3d' &&
  threeRenderer.isReady.value
)

const isAnimeCharacter = computed(() => checkAnime(currentCharacter.value))

/**
 * AI 精绘立绘 PNG 地址（2D 降级模式 / 3D 贴图共用）。
 * 优先使用用户上传的自定义形象图片（customImage）；
 * 否则内置 6 位二次元角色有 public/assets/elf-sprites/elf_<key>.png，
 * 其他角色（自定义/几何/生肖等）返回空串走程序化渲染。
 */
const spriteImageUrl = computed(() => {
  // 用户上传的自定义形象优先（dataURL 或 URL 均可）
  if (elfConfig.value.customImage) return elfConfig.value.customImage
  const c = currentCharacter.value
  // 注意：isAnimeCharacter 是 computed ref，须用 checkAnime(c) 判断（误用 computed 会导致 render 崩溃）
  if (!c || !checkAnime(c)) return ''
  const key = typeof c.id === 'string' ? c.id.replace('anime-', '') : ''
  if (!/^[a-z0-9_-]+$/i.test(key)) return ''
  return `/assets/elf-sprites/elf_${key}.png`
})

// 气泡主色跟随角色 palette（ribbon → outfitAccent 兜底）
const bubbleAccent = computed(() => {
  const p = currentCharacter.value && currentCharacter.value.palette
  return (p && (p.ribbon || p.outfitAccent)) || '#ff6f9c'
})

// 面板强调色跟随角色 palette（outfitAccent → ribbon 兜底，用于 header 渐变/选中态）
const panelAccentColor = computed(() => {
  const p = currentCharacter.value && currentCharacter.value.palette
  return (p && (p.outfitAccent || p.ribbon)) || '#4f46e5'
})

// 组件库 widget type → 中文名（用于"组件"tab 列表展示）
const widgetTypeNames = {
  clock: '时钟',
  weather: '天气',
  calendar: '日历',
  notes: '笔记',
  systemMonitor: '系统监控',
  alarm: '闹钟',
  music: '音乐'
}

// 设计器保存的 widget 列表（只读展示）
const configuredWidgets = computed(() => {
  const list = elfConfig.value.widgets
  return Array.isArray(list) ? list : []
})

const elfPosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const isDropping = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const showTime = ref(false)
const currentTime = ref('')
const panelPosition = ref({ top: 'auto', bottom: 'auto', left: 'auto', right: 'auto' })
const elfRootRef = ref(null)

// 面板/对话打开或拖拽时收起全部浮层（isDragging 已在上方定义，避免 TDZ）
watch([panelOpen, () => isDragging.value], ([po, id]) => {
  if (po || id) {
    quickMenuOpen.value = false
    actMenuOpen.value = false
    sizePanelOpen.value = false
    outfitMenuOpen.value = false
  }
})

// ─── 高级互动（市面桌宠体验）：眼神跟随 / 拖拽挣扎 / 松手摔落 / 待机瞌睡 / 随机卖萌 ───
// 待机随机卖萌台词池
const ELF_IDLE_LINES = [
  '主人，陪我玩一会儿嘛～',
  '今天也要元气满满哦！',
  '嘿嘿，你在偷看我吗？',
  '有点无聊呀…想聊天～',
  '发现主人！蹭蹭～',
  '走累了，歇一下下~',
  '要不要戳戳我呀？'
]
let gazeHandler = null
let gazeLastTs = 0
let idleCheckTimer = null
let randomTalkTimer = null
let lastActiveAt = Date.now()

let timeInterval = null

const tabs = [
  { id: 'character', name: '角色' },
  { id: 'action', name: '动作' },
  { id: 'chat', name: '对话' },
  { id: 'skin', name: '皮肤' },
  { id: 'outfit', name: '换装' },
  { id: 'widgets', name: '组件' }
]

const categories = [
  { id: 'all', name: '全部' },
  { id: 'anime', name: '二次元' },
  { id: 'basic', name: '基础' },
  { id: 'zodiac', name: '十二生肖' },
  { id: 'myth', name: '山海经' }
]

// 二次元角色自带配件体系，隐藏皮肤/换装 tab（保留角色/动作/对话/组件）
const visibleTabs = computed(() => {
  if (isAnimeCharacter.value) {
    return tabs.filter((t) => t.id === 'character' || t.id === 'action' || t.id === 'chat' || t.id === 'widgets')
  }
  return tabs
})

const animate = useElfAnimation()
const interaction = useElfInteraction()
// ===== 桌面养成四维状态（心情/饱腹/亲密度/经验） =====
const growth = ref({ mood: 80, fullness: 70, intimacy: 10, experience: 0, lethargic: false, hungry: false, affectionate: false, suggestion: '' })
// ===== 精灵灵魂状态（灵魂引擎 elf 维度：情绪/心情/亲近/版本） =====
const elfSoul = ref({ hasSoul: false, emotion: 'neutral', moodScore: 0, attitudeScore: 0, version: 0, coreBeliefs: '', recentExperiences: [] })
const voiceLoop = useElfVoiceLoop({ onReply: (t) => { try { interaction.showDialog(t, 4000) } catch (e) {} } })

// ===== 语音对话模式（M2 ⑥）：双击/长按精灵进入语音闭环，结束后回到正常状态 =====
const voiceMode = ref(false)
let longPressTimer = null

const voiceStatusText = computed(() => {
  if (voiceLoop.isSpeaking.value) return '正在说话…'
  if (voiceLoop.isListening.value) return '正在聆听，请说话…'
  return '准备中…'
})

function startVoiceMode() {
  if (voiceMode.value) return
  if (!voiceLoop.supported.value) {
    try { interaction.showDialog('当前浏览器不支持语音识别，已为你打开文字对话～', 3500) } catch (e) {}
    openChatFromMenu()
    return
  }
  quickMenuOpen.value = false
  voiceMode.value = true
  try { interaction.showDialog('你好呀，说话就能和我聊天～', 2500) } catch (e) {}
  voiceLoop.start()
}

function stopVoiceMode() {
  if (!voiceMode.value) return
  voiceMode.value = false
  voiceLoop.stop()
  try { interaction.showDialog('好的，下次再聊～', 2000) } catch (e) {}
}
let comboClicks = []

async function loadGrowthState() {
  try {
    const elfId = elfConfig.value?.elfId
    if (!elfId) return
    const { data } = await request.get('/api/elf/' + elfId + '/growth')
    if (data && data.data) growth.value = { ...growth.value, ...data.data }
  } catch (e) { /* 未登录/无精灵时静默 */ }
}

// ===== 加载精灵灵魂状态（灵魂引擎 elf 维度） =====
async function loadElfSoul() {
  try {
    const elfId = elfConfig.value?.elfId
    if (!elfId) return
    const { data } = await request.get('/api/elf/' + elfId + '/soul')
    if (data && data.data) elfSoul.value = { ...elfSoul.value, ...data.data }
  } catch (e) { /* 未登录/无精灵时静默 */ }
}

// 情绪 → 中文标签 + emoji（与后端 currentEmotion 对齐）
const soulEmotionLabel = computed(() => {
  const map = { happy: '开心', sad: '低落', angry: '生气', anxious: '不安', calm: '平静', neutral: '平静' }
  return map[elfSoul.value.emotion] || '平静'
})

async function feedNow() {
  try {
    const elfId = elfConfig.value?.elfId
    if (!elfId) return
    await request.post('/api/elf/' + elfId + '/feed', {})
    // 不在这里重复触发动作（动作由 triggerAction3D('feed') 统一触发），只做数据更新
    spawnEffect('star', '🍱')
    await loadGrowthState()
    await loadElfSoul()
  } catch (e) {}
}

// 摸头 → 亲密度增长（前端乐观更新 + 后端接口）
async function registerPatGrowth() {
  try {
    const elfId = elfConfig.value?.elfId
    if (!elfId) return
    await request.post('/api/elf/' + elfId + '/pat', {})
    spawnEffect('heart', '❤️')
    await loadGrowthState()
    await loadElfSoul()
  } catch (e) { /* 未登录时静默 */ }
}

// 连续点击连招：3 秒内 >=3 次 → dance/happy 特殊动作
function registerComboClick() {
  const now = Date.now()
  comboClicks = comboClicks.filter((t) => now - t < 3000)
  comboClicks.push(now)
  if (isComboClick(comboClicks.length, 3)) {
    comboClicks = []
    triggerAction3D(Math.random() > 0.5 ? 'dance' : 'happy')
    spawnEffect('star', '✨')
  }
}
let growthTimer = null
let inactivityTimer = null

// 久坐提醒：30 分钟无鼠标活动 → 提醒活动一下
function checkInactivityReminder() {
  const idleMin = (Date.now() - lastActiveAt) / 60000
  const level = getInactivityLevel(idleMin)
  if (level >= 2) {
    try { interaction.showDialog('已经坐了好久啦，起来活动一下吧～', 3000) } catch (e) {}
    try { triggerAction3D('wave') } catch (e) {}
  }
}

// 时段问候：夜晚打哈欠/建议休息
function greetByTimePeriod() {
  const period = getTimePeriod(new Date().getHours())
  if (period === 'night') {
    try { interaction.showDialog('夜深啦，早点休息哦～', 3500) } catch (e) {}
    try { triggerAction3D('sleep') } catch (e) {}
  } else if (period === 'morning') {
    try { interaction.showDialog('早安！新的一天开始啦', 3000) } catch (e) {}
  }
}

// ─── 自由移动系统（二次升级） ───
// elfPosition 是相对右下角锚点的偏移；锚点按精灵当前尺寸动态计算，保证大精灵不越界
function anchorTopLeft() {
  const w = typeof window !== 'undefined' ? window.innerWidth : 800
  const h = typeof window !== 'undefined' ? window.innerHeight : 600
  const sz = elfSizePx.value || 120
  return {
    x: w - sz - 24,
    y: h - sz - 24
  }
}
function offsetToAbs(ox, oy) {
  const a = anchorTopLeft()
  return { x: a.x + ox, y: a.y + oy }
}
function absToOffset(ax, ay) {
  const a = anchorTopLeft()
  return { x: ax - a.x, y: ay - a.y }
}

const isWalking = ref(false)
const isJumping = ref(false)
const moveDir = ref(0) // 水平移动方向：-1 左 / 1 右 / 0 停（驱动 3D 朝向与 2D 镜像）
const movement = useElfMovement(elfConfig, {
  getBounds: () => {
    // 按精灵当前尺寸留边：保证大精灵也完整留在可视区内（margin=半宽/半高+12 安全距）
    const w = typeof window !== 'undefined' ? window.innerWidth : 800
    const h = typeof window !== 'undefined' ? window.innerHeight : 600
    const half = elfSizePx.value / 2 + 12
    return {
      minX: half,
      minY: half,
      maxX: Math.max(half + 40, w - half),
      maxY: Math.max(half + 40, h - half)
    }
  },
  onUpdate: (ax, ay, dx, dy) => {
    const off = absToOffset(ax, ay)
    elfPosition.value = off
    // 记录水平朝向：位移向量 dx>0 朝右、dx<0 朝左
    const absDx = Math.abs(dx || 0)
    moveDir.value = absDx > 0.5 ? (dx > 0 ? 1 : -1) : 0
    if (threeRenderer && typeof threeRenderer.setDirection === 'function') {
      try { threeRenderer.setDirection(moveDir.value) } catch (e) {}
    }
  },
  onMovingChange: (moving) => {
    isWalking.value = moving
    // 一旦开始行走，停止休息期微动作调度（行走自带动态）
    if (moving) clearRestAction()
    // 驱动 3D 渲染器进入/退出走路步态（摆臂摆腿 + 前倾 + 朝移动方向转身）
    if (threeRenderer && typeof threeRenderer.setWalking === 'function') {
      try { threeRenderer.setWalking(moving, moveDir.value) } catch (e) {}
    }
  },
  onEdgeHit: () => {
    // 碰边折返：跳跃一下再继续走
    triggerElfJump()
  },
  onArrive: () => {
    // 到达目标：随机播一个待机小动作 + 偶尔冒一句台词（灵魂感，不再机械折返）
    try {
      const action = getIdleRandomAction()
      if (action !== 'blink') triggerAction3D(action)
      if (Math.random() < 0.45) {
        interaction.showDialog(getCharacterLine(currentCharacter.value, ['走累了，歇一下下~', '这里风景不错嘛', '呼——休息一下'], Math.random), 2600)
      }
      // 长休息期间持续"活气"：随机待机动作 + 悠闲台词（休息时不是雕像）
      scheduleRestAction()
    } catch (e) {}
  },
  onStrollPause: () => {
    // 中途驻足：播"张望/思考"瞬时动作（2D 立绘 idleAction 由组件层映射）
    try {
      if (animate && animate.idleAction) {
        animate.idleAction.value = 'look-around'
        setTimeout(() => { if (animate.idleAction) animate.idleAction.value = '' }, 1400)
      }
      if (threeRenderer && typeof threeRenderer.playAction === 'function') {
        try { threeRenderer.playAction('sway') } catch (e) {}
      }
    } catch (e) {}
  },
  onDeliberate: () => {
    // 休息结束"回神"：先张望/思考一下再迈步（消除"起来就走"的机械感）
    try {
      if (animate && animate.idleAction) {
        animate.idleAction.value = 'look-around'
        setTimeout(() => { if (animate.idleAction) animate.idleAction.value = '' }, 1200)
      }
      if (threeRenderer && typeof threeRenderer.playAction === 'function') {
        try { threeRenderer.playAction('sway') } catch (e) {}
      }
    } catch (e) {}
  }
})

/** 触发跳跃：3D 叠加抛物线 + 2D 状态机 + 特效 */
function triggerElfJump() {
  if (threeRenderer && typeof threeRenderer.triggerJump === 'function') {
    try { threeRenderer.triggerJump() } catch (e) {}
  }
  try { animate.triggerAction('jump') } catch (e) {}
  isJumping.value = true
  setTimeout(() => { isJumping.value = false }, 560)
  spawnEffect('star', '⭐')
}

// 随机跳跃：行走中每 6~13 秒随机跳一次（活泼步态）
let randomJumpTimer = null
let lastDragEndAt = 0
// 本次按下是否真正拖动过（位移 >10px，与触摸阈值一致）；仅真拖拽松手后才忽略后续 click
let dragMoved = false
const DRAG_THRESHOLD = 10
function scheduleRandomJump() {
  if (randomJumpTimer) clearTimeout(randomJumpTimer)
  // 拉长随机跳跃间隔：12~24 秒（与"走动不频繁"的从容节奏一致）
  const wait = 12000 + Math.random() * 12000
  randomJumpTimer = setTimeout(() => {
    if (isWalking.value) triggerElfJump()
    scheduleRandomJump()
  }, wait)
}

/**
 * 休息期"活气"调度：长休息期间每 4~10 秒随机待机动作 + 小概率悠闲台词。
 * 让精灵在不动时也有呼吸般的生命感，而不是静止雕像。
 * 仅休息（未走动/未拖拽/未开面板/未交互）时执行；开始行走即停止。
 */
let restActionTimer = null
function scheduleRestAction() {
  if (restActionTimer) clearTimeout(restActionTimer)
  const wait = 4000 + Math.random() * 6000
  restActionTimer = setTimeout(() => {
    restActionTimer = null
    try {
      if (!isWalking.value && !isDragging.value && !panelOpen.value && !interaction.isInteracting.value) {
        const action = getIdleRandomAction()
        if (animate && animate.idleAction) {
          animate.idleAction.value = action
          setTimeout(() => { if (animate.idleAction) animate.idleAction.value = '' }, 1500)
        }
        if (Math.random() < 0.25) {
          interaction.showDialog(getCharacterLine(currentCharacter.value, ANIME_REST_LINES, Math.random), 2400)
        }
      }
    } catch (e) {}
    // 只要仍处于休息状态就继续排下一次（走动/拖拽会通过 clearRestAction 停止）
    if (!isWalking.value && !isDragging.value) scheduleRestAction()
  }, wait)
}

/** 停止休息期微动作调度（开始行走 / 卸载时调用） */
function clearRestAction() {
  if (restActionTimer) {
    clearTimeout(restActionTimer)
    restActionTimer = null
  }
}

/** 记录用户活动时间（重置待机瞌睡计时） */
function resetIdleTimer() {
  lastActiveAt = Date.now()
}

/** 眼神跟随鼠标：把鼠标相对桌宠的偏移归一化后交给 3D 渲染器（40ms 节流） */
function handleGazeMove(e) {
  resetIdleTimer()
  const el = elfRootRef.value
  if (!el) return
  const n = Date.now()
  if (n - gazeLastTs < 40) return
  gazeLastTs = n
  if (!threeRenderer || typeof threeRenderer.setGaze !== 'function') return
  try {
    const r = el.getBoundingClientRect()
    const g = normalizeGaze(e.clientX, e.clientY, r)
    threeRenderer.setGaze(g.x, g.y)
    // 立体真实感：同一鼠标偏移驱动相机视差（立绘相对背景透视位移）
    if (typeof threeRenderer.setCameraParallax === 'function') {
      threeRenderer.setCameraParallax(g.x, g.y)
    }
  } catch (err) {}
}

/** 待机检查：无操作 60s 打瞌睡、120s 睡着；有活动即清醒 */
function idleCheck() {
  if (isDragging.value || panelOpen.value) {
    resetIdleTimer()
    return
  }
  const stage = getIdleStage(Date.now() - lastActiveAt)
  const st = animate.currentState.value
  if (stage === 2 && st !== 'sleep' && !isWalking.value && !interaction.isInteracting.value) {
    triggerAction3D('sleep')
    spawnEffect('zzz', '💤')
    try { interaction.showDialog('zzZ… 好困哦～', 3000) } catch (e) {}
  } else if (stage === 1 && st !== 'sleep' && st !== 'thinking' && !isWalking.value && !interaction.isInteracting.value) {
    triggerAction3D('thinking')
    try { interaction.showDialog('（打个哈欠）好困呀…', 3000) } catch (e) {}
  } else if (stage === 0 && (st === 'sleep' || st === 'thinking') && !isWalking.value) {
    try { animate.reset() } catch (e) {}
    if (threeRenderer && typeof threeRenderer.playAction === 'function') {
      try { threeRenderer.playAction('idle') } catch (e) {}
    }
  }
}

/** 随机卖萌台词：待机（非移动/非互动/非睡觉）每 25~45 秒冒一句
 *  优先播报养成需求（饿了/困了/想被摸），否则用角色化台词池 */
function scheduleRandomTalk() {
  if (randomTalkTimer) clearTimeout(randomTalkTimer)
  const wait = 25000 + Math.random() * 20000
  randomTalkTimer = setTimeout(() => {
    if (!isWalking.value && !panelOpen.value && !isDragging.value &&
        animate.currentState.value !== 'sleep' && !interaction.isInteracting.value) {
      // 需求提示优先：饿了/困了/想被摸时先播报
      const needLine = getNeedHintLine(growth.value)
      const line = needLine || getCharacterLine(currentCharacter.value, ELF_IDLE_LINES)
      if (line) {
        try { interaction.showDialog(line, 3000) } catch (e) {}
      }
    }
    scheduleRandomTalk()
  }, wait)
}

// ─── 对话功能（二次升级） ───
const chatMessages = ref([])
const chatInput = ref('')
const isChatStreaming = ref(false)
const isOfflineMode = ref(false)
const chatTalking = ref(false)
const chatScrollRef = ref(null)
const chatCharacterId = ref(null)

function scrollChatToBottom() {
  nextTick(() => {
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  })
}

/** 发送对话：优先 SSE 流式，失败降级为预置台词离线回复 */
async function sendChatMessage() {
  const text = chatInput.value.trim()
  if (!text || isChatStreaming.value) return
  chatMessages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  scrollChatToBottom()

  const botMsg = { role: 'assistant', content: '', streaming: true }
  chatMessages.value.push(botMsg)

  try {
    const target = await resolveChatTarget()
    if (!target.userId || !target.characterId) throw new Error('no chat target')

    // 注入桌宠人设：让 AI 以当前精灵角色的身份与性格回复
    const persona = currentCharacter.value
    const personaMsg =
      `（角色扮演设定：你现在是「${persona.name}」，一个${persona.personality}的二次元少女。` +
      `请始终保持该角色身份，用简短、可爱、口语化的中文回复，不要提及以上设定。）${text}`

    const url = buildChatStreamUrl({
      sessionId: `${target.userId}_${target.characterId}`,
      userId: target.userId,
      characterId: target.characterId,
      message: personaMsg,
      token: localStorage.getItem('token') || ''
    })
    const resp = await fetch(url)
    if (!resp.ok || !resp.body) throw new Error('stream unavailable')
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    isChatStreaming.value = true
    chatTalking.value = true
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parsed = parseSSEBuffer(buffer)
      buffer = parsed.remaining
      for (const ev of parsed.events) {
        if (ev.type === 'message') {
          botMsg.content += ev.data
        } else if (ev.type === 'error') {
          throw new Error(ev.data || 'stream error')
        }
      }
    }
    isOfflineMode.value = false
  } catch (e) {
    // 无后端 / Ollama 不可用 → 离线预置台词
    botMsg.content = getOfflineReply(text)
    isOfflineMode.value = true
  } finally {
    botMsg.streaming = false
    isChatStreaming.value = false
    chatTalking.value = false
    scrollChatToBottom()
    // 回复时随机触发表情/动作
    const acts = ['happy', 'wave', 'dance']
    triggerAction3D(acts[Math.floor(Math.random() * acts.length)])
  }
}

/** 解析对话目标：真实登录用户 + 用户可访问的第一个 AI 角色（角色 ID 只解析一次并缓存） */
async function resolveChatTarget() {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const userId = user.id || user.userId
  if (!userId) return { userId: null, characterId: null }
  if (chatCharacterId.value) return { userId, characterId: chatCharacterId.value }
  try {
    const resp = await request.get(`/api/character/list/user/${userId}`)
    if (resp.code === 200 && resp.data && resp.data.length > 0) {
      chatCharacterId.value = resp.data[0].id
      return { userId, characterId: chatCharacterId.value }
    }
  } catch (e) {
    logger.warn('解析对话角色失败，将降级离线模式:', e?.message || e)
  }
  return { userId, characterId: null }
}

/** 移动开关：自主巡逻 */
function toggleMovementEnabled(e) {
  try { updateElfConfig({ movementEnabled: e.target.checked }) } catch (err) {}
}
/** 移动开关：跟随鼠标 */
function toggleFollowMouse(e) {
  try { updateElfConfig({ followMouse: e.target.checked }) } catch (err) {}
}

/** 上传自定义形象图片：校验 → 压缩为 dataURL → 保存到 elfConfig.customImage */
async function onCustomImageChange(e) {
  const file = e.target && e.target.files && e.target.files[0]
  try { e.target.value = '' } catch (err) {}
  const check = validateElfImageFile(file)
  if (!check.ok) {
    const msg = check.reason === 'not_image'
      ? '请选择图片文件哦～'
      : check.reason === 'too_large'
        ? '图片太大了，请选择 5MB 以内的图片～'
        : '请先选择一张图片～'
    try { interaction.showDialog(msg, 2000) } catch (err) {}
    return
  }
  try {
    const dataUrl = await readElfImageDataURL(file)
    updateElfConfig({ customImage: dataUrl })
    try { interaction.showDialog('形象更新成功～', 1800) } catch (err) {}
  } catch (err) {
    logger.warn('自定义形象读取失败', err)
    try { interaction.showDialog('图片读取失败，换一张试试吧～', 2000) } catch (err2) {}
  }
}

/** 清除自定义形象：恢复内置角色渲染 */
function clearCustomImage() {
  updateElfConfig({ customImage: null })
  try { interaction.showDialog('已恢复默认形象～', 1600) } catch (err) {}
}

/** 切换自动去背景：变化后重新同步 3D 立牌 */
function toggleCustomImageRemoveBg(e) {
  updateElfConfig({ customImageRemoveBg: !!e.target.checked })
  nextTick(() => sync3DMesh())
}

const currentState = computed(() => animate.currentState.value)

const isEmojiCharacter = computed(() => {
  const id = currentCharacter.value.id
  return id.startsWith('zodiac-') || id.startsWith('myth-')
})

const currentColor = computed(() => {
  return currentSkin.value.colors.body || currentCharacter.value.baseColor
})

const highlightColor = computed(() => {
  return currentSkin.value.colors.highlight || '#ffffff'
})

const borderColor = computed(() => {
  const base = currentCharacter.value.baseColor
  return base
})

const shadowColor = computed(() => {
  return currentCharacter.value.baseColor
})

const skinColors = computed(() => {
  return {
    eyes: currentSkin.value.colors.eyes || '#1f2937',
    mouth: currentSkin.value.colors.mouth || '#1f2937'
  }
})

const glowFilter = computed(() => {
  if (currentSkin.value.colors.glow) {
    return `drop-shadow(0 0 10px ${currentSkin.value.colors.glow})`
  }
  return 'none'
})

const bodyGradient = computed(() => 'url(#bodyGradient)')

const characterStyle = computed(() => ({
  ...animate.transformStyle.value
}))

// ─── 桌宠大小控制（快捷菜单"大小"面板，与设计器共享 elfConfig.size） ───
/** 大小比例（0.4x ~ 2.5x，默认 1.0） */
const elfSizeRatio = computed(() => {
  const s = Number(elfConfig.value.size) || 1.0
  return Math.min(2.5, Math.max(0.4, s))
})
/** 精灵本体像素尺寸 = 120px 基准 × 比例 */
const elfSizePx = computed(() => Math.round(120 * elfSizeRatio.value))
/** 精灵容器尺寸注入（3D 与几何 SVG 共用） */
const elfSizedStyle = computed(() => ({
  width: elfSizePx.value + 'px',
  height: elfSizePx.value + 'px'
}))
/** 2D 二次元立绘按比例缩放（立绘基准 128×168，底边锚定） */
const animeSpriteScaleStyle = computed(() => ({
  transform: `translateY(-14px) scale(${elfSizeRatio.value})`,
  transformOrigin: 'bottom center'
}))

/** 大小面板：四档快捷设置 */
const ELF_SIZE_PRESETS = [
  { label: '迷你', value: 0.6 },
  { label: '小', value: 0.8 },
  { label: '标准', value: 1.0 },
  { label: '大', value: 1.4 }
]

/** 应用大小：持久化配置 + 同步 3D 渲染器（尺寸变化后 resize canvas） */
function applyElfSize(size) {
  const v = Math.min(2.5, Math.max(0.4, Number(size) || 1.0))
  try { updateElfConfig({ size: v }) } catch (e) {}
  threeConfig.value = { ...threeConfig.value, size: v }
  requestAnimationFrame(() => {
    try {
      if (threeRenderer && typeof threeRenderer.resize === 'function') {
        threeRenderer.resize(elfSizePx.value, elfSizePx.value)
      }
    } catch (e) {}
  })
}

/** 打开大小面板 */
function openSizeMenu() {
  quickMenuOpen.value = false
  sizePanelOpen.value = true
}

/** 监听外部配置（设计器/桌面端）size 变化，同步渲染器 */
watch(() => elfConfig.value.size, (v) => {
  if (v == null) return
  threeConfig.value = { ...threeConfig.value, size: Number(v) || 1.0 }
  requestAnimationFrame(() => {
    try {
      if (threeRenderer && typeof threeRenderer.resize === 'function') {
        threeRenderer.resize(elfSizePx.value, elfSizePx.value)
      }
    } catch (e) {}
  })
})

const elfPositionStyle = computed(() => {
  if (elfPosition.value.x === 0 && elfPosition.value.y === 0) {
    return {}
  }
  return {
    transform: `translate(${elfPosition.value.x}px, ${elfPosition.value.y}px)`
  }
})

const bodyTransform = computed(() => {
  return ''
})

const faceStyle = computed(() => ({
  transform: currentState.value === 'thinking' 
    ? `rotate(${Math.sin(Date.now() * 0.002) * 5}deg)` 
    : 'none'
}))

const mouthPath = computed(() => {
  switch (currentState.value) {
    case 'happy': return 'M85,85 Q100,100 115,85'
    case 'sad': return 'M85,95 Q100,85 115,95'
    case 'surprised': return 'M85,90 A10,10 0 1,0 115,90'
    case 'thinking': return 'M90,90 Q100,95 110,90'
    case 'sleep': return 'M85,75 L115,75 M88,80 L112,80'
    default: return 'M85,90 Q100,92 115,90'
  }
})

const outfitTransform = computed(() => {
  const outfit = currentOutfit.value
  const offsets = {
    hat: 'translate(0, -60)',
    glasses: 'translate(0, -30)',
    scarf: 'translate(0, 20)',
    bow: 'translate(0, -40)',
    crown: 'translate(0, -65)',
    wings: 'translate(0, 0)',
    coat: 'translate(0, 10)',
    robe: 'translate(0, 15)',
    armor: 'translate(0, 10)'
  }
  return offsets[outfit.id] || 'translate(0, 0)'
})

function updatePanelPosition() {
  const elfWidth = 120
  // 面板实际宽度：CSS 已限制 max-width: calc(100vw - 24px)，移动端按真实宽度参与定位计算
  const panelWidth = Math.min(420, Math.max(260, window.innerWidth - 24))
  const panelHeight = Math.min(550, window.innerHeight - 40)
  const margin = 15
  
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  
  const elfRight = viewportWidth - 30 + elfPosition.value.x
  const elfBottom = viewportHeight - 30 + elfPosition.value.y
  const elfLeft = elfRight - elfWidth
  const elfTop = elfBottom - elfWidth
  
  let panelTop = 'auto'
  let panelBottom = 'auto'
  let panelLeft = 'auto'
  let panelRight = 'auto'
  
  const spaceAbove = elfTop
  const spaceBelow = viewportHeight - elfBottom
  const spaceToLeft = elfLeft
  const spaceToRight = viewportWidth - elfRight
  
  logger.debug('PANEL_POSITION_CALC_START', {
    elfPosition: elfPosition.value,
    elfDimensions: { width: elfWidth, height: elfWidth },
    panelDimensions: { width: panelWidth, height: panelHeight },
    viewport: { width: viewportWidth, height: viewportHeight },
    elfBounds: { left: elfLeft, right: elfRight, top: elfTop, bottom: elfBottom },
    availableSpace: { above: spaceAbove, below: spaceBelow, left: spaceToLeft, right: spaceToRight }
  })
  
  let positionChoice = 'default'
  
  if (spaceAbove >= panelHeight + margin) {
    panelBottom = `${elfWidth + margin}px`
    positionChoice = 'above'
    logger.debug('PANEL_POSITION_CHOICE', `选择上方位置，上方空间: ${spaceAbove}px >= 所需空间: ${panelHeight + margin}px`)
  } else if (spaceBelow >= panelHeight + margin) {
    panelTop = `${elfWidth + margin}px`
    positionChoice = 'below'
    logger.debug('PANEL_POSITION_CHOICE', `选择下方位置，下方空间: ${spaceBelow}px >= 所需空间: ${panelHeight + margin}px`)
  } else {
    const availableHeight = Math.max(spaceAbove, spaceBelow) - margin
    panelBottom = `${elfWidth + margin}px`
    positionChoice = 'default_above'
    logger.warn('PANEL_POSITION_WARNING', `空间不足，上方空间: ${spaceAbove}px, 下方空间: ${spaceBelow}px, 所需空间: ${panelHeight + margin}px, 使用默认上方位置`)
  }
  
  let horizontalChoice = 'default'
  
  if (spaceToRight >= panelWidth + margin) {
    panelLeft = `${elfWidth + margin}px`
    horizontalChoice = 'right'
    logger.debug('PANEL_HORIZONTAL_CHOICE', `选择右侧位置，右侧空间: ${spaceToRight}px >= 所需空间: ${panelWidth + margin}px`)
  } else if (spaceToLeft >= panelWidth + margin) {
    panelRight = `${elfWidth + margin}px`
    horizontalChoice = 'left'
    logger.debug('PANEL_HORIZONTAL_CHOICE', `选择左侧位置，左侧空间: ${spaceToLeft}px >= 所需空间: ${panelWidth + margin}px`)
  } else {
    const availableWidth = Math.max(spaceToLeft, spaceToRight) - margin
    if (spaceToRight > spaceToLeft) {
      panelLeft = `${elfWidth + margin}px`
      horizontalChoice = 'default_right'
    } else {
      panelRight = `${elfWidth + margin}px`
      horizontalChoice = 'default_left'
    }
    logger.warn('PANEL_HORIZONTAL_WARNING', `水平空间不足，左侧空间: ${spaceToLeft}px, 右侧空间: ${spaceToRight}px, 所需空间: ${panelWidth + margin}px, 使用${horizontalChoice === 'default_right' ? '右侧' : '左侧'}位置`)
  }
  
  panelPosition.value = { top: panelTop, bottom: panelBottom, left: panelLeft, right: panelRight }
  
  logger.info('PANEL_POSITION_CALC_COMPLETE', {
    finalPosition: panelPosition.value,
    positionChoice: { vertical: positionChoice, horizontal: horizontalChoice },
    calculatedAt: new Date().toLocaleTimeString('zh-CN')
  })
}

const currentMood = computed(() => {
  const moods = {
    sleep: 'sleepy',
    sad: 'sad',
    happy: 'happy',
    surprised: 'excited',
    thinking: 'thinking'
  }
  return moods[currentState.value] || 'happy'
})

const filteredCharacters = computed(() => {
  if (selectedCategory.value === 'all') return characters.value
  return characters.value.filter(c => c.category === selectedCategory.value)
})

function getActionIcon(actionId) {
  const icons = {
    stand: '🧍', crouch: '🦶', lie: '😴',
    wave: '👋', dance: '💃', jump: '🦘',
    idle: '✨', sleep: '😪', happy: '😊',
    sad: '😢', surprised: '😲', thinking: '🤔',
    sit: '🪑', feed: '🍰', pat: '🫳', run: '🏃‍♀️'
  }
  return icons[actionId] || '🧍'
}

function getSkinPreviewStyle(skin) {
  return {
    background: skin.colors.glow 
      ? `linear-gradient(135deg, #4ade80, ${skin.colors.glow})`
      : '#4ade80',
    boxShadow: skin.colors.glow ? `0 0 15px ${skin.colors.glow}` : 'none'
  }
}

function selectCharacter(char) {
  currentCharacter.value = char
  animate.reset()
  sync3DMesh()
  try { updateElfConfig({ characterId: char.id }) } catch (e) {}
}

function selectSkin(skin) {
  currentSkin.value = skin
  sync3DMesh()
  try { updateElfConfig({ skinId: skin.id }) } catch (e) {}
}

function selectOutfit(outfit) {
  currentOutfit.value = outfit
  sync3DMesh()
  try { updateElfConfig({ outfitId: outfit.id }) } catch (e) {}
}

/** 选择换装套装（anime 3D 角色，V4 换装小游戏级） */
function selectOutfitSet(set) {
  // 选择预设套装时清除自由混搭/换色覆盖
  customOutfit.value = null
  // 换装仅对程序化 3D 形象生效：若当前使用自定义图片立绘，自动切回程序化 3D
  if (elfConfig.value.customImage) {
    try { updateElfConfig({ customImage: null }) } catch (e) {}
    if (typeof threeRenderer.setCustomSprite === 'function') {
      try { threeRenderer.setCustomSprite(null) } catch (e) {}
    }
  }
  sync3DMesh()
  try { updateElfConfig({ outfitSetId: set.id, outfitCustom: null }) } catch (e) {}
  spawnEffect('sparkle', '✨')
}

/** 单件混搭：切换某部件的款式（进入自由搭配模式） */
function applyMixPart(part, type) {
  const cur = { ...(customOutfit.value || {}), [part]: type }
  cur.colors = { ...(cur.colors || {}) }
  customOutfit.value = cur
  sync3DMesh()
  try { updateElfConfig({ outfitSetId: '', outfitCustom: cur }) } catch (e) {}
  spawnEffect('sparkle', '✨')
}

/** 服装换色：覆盖某部件颜色（保持当前套装其余配置） */
function applyPartColor(part, color) {
  const base = currentOutfitSet.value || DEFAULT_OUTFIT_SET
  const cur = customOutfit.value
    ? { ...customOutfit.value }
    : { top: base.top, bottom: base.bottom, shoes: base.shoes, socks: base.socks, accessory: base.accessory }
  cur.colors = { ...(cur.colors || {}), [part]: color }
  customOutfit.value = cur
  sync3DMesh()
  try { updateElfConfig({ outfitSetId: '', outfitCustom: cur }) } catch (e) {}
}

/** 服装换色：还原某部件为套装默认色 */
function resetPartColor(part) {
  if (!customOutfit.value || !customOutfit.value.colors) return
  const next = { ...customOutfit.value, colors: { ...customOutfit.value.colors } }
  delete next.colors[part]
  customOutfit.value = next
  sync3DMesh()
  try { updateElfConfig({ outfitSetId: '', outfitCustom: next }) } catch (e) {}
}

/** 当前生效的部件颜色（自定义覆盖色 → 套装默认色） */
function partColorFor(part) {
  const base = currentOutfitSet.value || DEFAULT_OUTFIT_SET
  return (customOutfit.value && customOutfit.value.colors && customOutfit.value.colors[part]) || base.colors[part]
}

/**
 * 同步当前角色/皮肤/换装到 3D 渲染器（若可用）。
 * 存在自定义形象图片时：进入"3D 立体立牌"模式（图片自动去背景后作为
 * 3D 场景中的立体人物，支持呼吸摆动/转身/跳跃/阴影/视差）；
 * 否则走内置程序化 3D 建模（二次元少女）/内置立绘贴图。
 */
function sync3DMesh() {
  if (!threeRenderer || typeof threeRenderer.updateElfMesh !== 'function') return
  try {
    const custom = elfConfig.value.customImage
    if (custom) {
      if (typeof threeRenderer.setCustomSprite === 'function') {
        threeRenderer.setCustomSprite(custom, {
          removeBg: elfConfig.value.customImageRemoveBg !== false
        })
        return
      }
    }
    // anime 角色 → 换装套装（V4）；非 anime → 原饰品
    const outfitFor3D = checkAnime(currentCharacter.value) ? currentOutfitSet.value : currentOutfit.value
    threeRenderer.updateElfMesh(currentCharacter.value, currentSkin.value, outfitFor3D)
  } catch (e) {
    // 3D 不可用则静默降级 2D（保留诊断日志便于排查）
    if (typeof console !== 'undefined') {
      console.error('[ElfSync] 3D 同步异常:', e && e.message ? e.message : e)
    }
  }
}

/**
 * 触发动作：同时驱动 2D 动画状态机与 3D 渲染器
 */
function triggerAction3D(actionId) {
  animate.triggerAction(actionId)
  if (threeRenderer && typeof threeRenderer.playAction === 'function') {
    try { threeRenderer.playAction(actionId) } catch (e) {}
  }
  // 互动与养成数据联动：喂食/摸头 产生真实数据变化（技能门禁：互动=动作+反馈+数据变化）
  if (actionId === 'feed') {
    feedNow()
  } else if (actionId === 'pat') {
    registerPatGrowth()
  }
}

/**
 * 跳转到设计器（配置中心）
 */
function openDesigner() {
  try {
    router.push('/desktop-elf')
  } catch (e) {}
}

/**
 * 从共享配置恢复角色/皮肤/换装
 */
function applySavedAppearance() {
  const cfg = elfConfig.value
  if (cfg.characterId) {
    // 默认形象全面更新：旧 anime 角色（含旧默认樱奈）迁移至新默认古风美人「青黛」，
    // 用户仍可在精灵快捷菜单中手动切换回其他角色。
    const savedAnime = cfg.characterId.startsWith('anime-')
    const isQingdai = cfg.characterId === 'anime-qingdai'
    if (savedAnime && !isQingdai) {
      const qingdai = characters.value.find(ch => ch.id === 'anime-qingdai')
      if (qingdai) {
        currentCharacter.value = qingdai
        updateElfConfig({ characterId: 'anime-qingdai' })
      }
    } else if (!savedAnime) {
      const c = characters.value.find(ch => ch.id === cfg.characterId)
      if (c) currentCharacter.value = c
    }
  }
  if (cfg.skinId) {
    const s = skins.value.find(sk => sk.id === cfg.skinId)
    if (s) currentSkin.value = s
  }
  if (cfg.outfitId) {
    const o = outfits.value.find(ot => ot.id === cfg.outfitId)
    if (o) currentOutfit.value = o
  }
  if (cfg.outfitSetId) {
    const os = outfitSets.value.find(s => s.id === cfg.outfitSetId)
    if (os) customOutfit.value = null
  }
  // 自由混搭/换色覆盖优先于预设套装
  if (cfg.outfitCustom && cfg.outfitCustom.top && cfg.outfitCustom.colors) {
    customOutfit.value = cfg.outfitCustom
  }
}

function handleClick(event) {
  // 移动端 tap 已手动触发：抑制浏览器合成的重复 click
  if (suppressElfClick) { suppressElfClick = false; return }
  if (event.target.closest('.elf-panel') || event.target.closest('.elf-quick-menu') || event.target.closest('.elf-reminder-panel')) {
    return
  }
  if (isDragging.value) return
  // 拖拽松手后的 click 忽略（350ms 内），避免摔落反应被随机互动覆盖
  if (Date.now() - lastDragEndAt < 350) return

  // 每次有效点击叠加按压回弹（Squash & Stretch 手感，不打断后续动作）
  triggerPressBounce()

  if (isAnimeCharacter.value) {
    // 头部区域（上半 45%）连续点击 → 摸摸头：pat 动作 + 爱心特效 + 亲密度+1
    const rect = elfRootRef.value ? elfRootRef.value.getBoundingClientRect() : null
    const oy = rect ? event.clientY - rect.top : 0
    if (isHeadTap(oy, rect ? rect.height : 0)) {
      registerComboClick()
      triggerAction3D('pat')
      spawnEffect('heart', '❤️')
      try { interaction.showDialog('好舒服～再摸摸嘛', 1600) } catch (e) {}
      loadGrowthState()
      return
    }
    // 二次元：单击 = 随机互动（延迟 220ms 以区分双击）
    registerComboClick()
    if (clickTimer) clearTimeout(clickTimer)
    clickTimer = setTimeout(() => {
      const combo = interaction.triggerRandomInteraction((actionId) => triggerAction3D(actionId))
      spawnEffect(combo.effect, combo.effectEmoji)
      clickTimer = null
    }, 220)
  } else {
    interaction.handleMouseEvent({ type: 'click' }, (actionId) => {
      triggerAction3D(actionId)
    })
    togglePanel()
  }
}

/** 双击 = 弹出/收起快捷菜单 */
function handleDoubleClick(event) {
  // 移动端 tap 已手动触发：抑制浏览器合成的重复 dblclick
  if (suppressElfClick) { suppressElfClick = false; return }
  if (event.target.closest('.elf-panel') || event.target.closest('.elf-quick-menu')) return
  if (isDragging.value) return
  if (!isAnimeCharacter.value) {
    togglePanel()
    return
  }
  if (clickTimer) { clearTimeout(clickTimer); clickTimer = null }
  quickMenuOpen.value = !quickMenuOpen.value
}

/** 生成一个飘升特效，2s 后自动移除 */
function spawnEffect(kind, emoji) {
  const id = ++fxSeq
  const x = 30 + Math.random() * 40
  spriteEffects.value.push({ id, kind, emoji, x })
  setTimeout(() => {
    spriteEffects.value = spriteEffects.value.filter((e) => e.id !== id)
  }, 2000)
}

/** 上一/下一个二次元角色 */
function cycleCharacter(dir) {
  const list = animeElfCharacters
  const idx = list.findIndex((c) => c.id === currentCharacter.value.id)
  const next = list[(idx + dir + list.length) % list.length]
  selectCharacter(next)
  quickMenuOpen.value = false
}

/** 随机动作 */
function randomAction() {
  const acts = ANIME_RANDOM_ACTIONS
  triggerAction3D(acts[Math.floor(Math.random() * acts.length)])
  quickMenuOpen.value = false
}

/** 打开动作表演面板（快捷菜单 → 动作列表） */
function openActMenu() {
  quickMenuOpen.value = false
  actMenuOpen.value = true
}

/** 播放指定游戏角色动作：3D 渲染器按动作时长推进，2D 降级层同步触发 */
function playActAction(act) {
  lastPlayedAction.value = act.id
  actMenuOpen.value = false
  if (threeRenderer && typeof threeRenderer.playAction === 'function') {
    try { threeRenderer.playAction(act.id, act.duration || 1800) } catch (e) {}
  }
  if (animate && typeof animate.triggerAction === 'function') {
    try { animate.triggerAction(act.id) } catch (e) {}
  }
  // 互动与养成数据联动
  if (act.id === 'feed') {
    feedNow()
  } else if (act.id === 'pat') {
    registerPatGrowth()
  }
  // 配合动作的情绪表情 + 小特效（不是所有动作都冒特效，保持克制）
  const fxByAct = { cheer: 'star', heart: 'heart', kiss: 'heart', victory: 'star', clap: 'star', spin: 'note' }
  if (fxByAct[act.id]) {
    spawnEffect(fxByAct[act.id], fxByAct[act.id] === 'heart' ? '💗' : fxByAct[act.id] === 'note' ? '🎵' : '⭐')
  }
}

/** 换装：二次元打开换装套装面板（V4）；非二次元循环配件 */
function cycleOutfit() {
  if (checkAnime(currentCharacter.value)) {
    quickMenuOpen.value = false
    outfitMenuOpen.value = true
    return
  }
  const cur = currentCharacter.value
  const idx = ANIME_ACCESSORIES.indexOf(cur.accessory)
  const next = ANIME_ACCESSORIES[(idx + 1) % ANIME_ACCESSORIES.length]
  currentCharacter.value = { ...cur, accessory: next }
  spawnEffect('star', '⭐')
}

/** 收起换装面板 */
function closeOutfitMenu() {
  outfitMenuOpen.value = false
}

/** 从快捷菜单打开配置 */
function openConfigFromMenu() {
  quickMenuOpen.value = false
  openDesigner()
}

/** 从快捷菜单打开对话面板 */
function openChatFromMenu() {
  quickMenuOpen.value = false
  currentTab.value = 'chat'
  panelOpen.value = true
  updatePanelPosition()
}

// 进入睡眠 → 飘出 zZ 特效
watch(() => animate.currentState.value, (s) => {
  if (s === 'sleep' && isAnimeCharacter.value) {
    spawnEffect('zzz', '💤')
  }
})

function handleMouseEnter(event) {
  interaction.handleMouseEvent(event, (actionId) => {
    triggerAction3D(actionId)
  })
}

function handleMouseLeave() {
}

function togglePanel() {
  const wasOpen = panelOpen.value
  panelOpen.value = !panelOpen.value
  
  if (panelOpen.value) {
    logger.info('PANEL_OPEN_START', {
      elfPosition: elfPosition.value,
      timestamp: new Date().toLocaleTimeString('zh-CN')
    })
    updatePanelPosition()
    logger.info('PANEL_OPEN_COMPLETE', {
      panelPosition: panelPosition.value,
      timestamp: new Date().toLocaleTimeString('zh-CN')
    })
  } else {
    logger.info('PANEL_CLOSE', {
      prevPosition: panelPosition.value,
      timestamp: new Date().toLocaleTimeString('zh-CN')
    })
  }
}

function handleMouseDown(event) {
  const target = event.target
  if (target.closest('.elf-panel')) {
    return
  }
  isDragging.value = true
  dragMoved = false
  dragStart.value = {
    x: event.clientX - elfPosition.value.x,
    y: event.clientY - elfPosition.value.y
  }
  panelOpen.value = false
  // 拖拽时暂停自主移动
  try { movement.pause() } catch (e) {}
  // 拖拽拎起：3D 角色悬空挣扎
  if (threeRenderer && typeof threeRenderer.setDragging === 'function') {
    try { threeRenderer.setDragging(true) } catch (e) {}
  }
  resetIdleTimer()

  // M2 ⑥：长按（700ms 未拖动）进入语音对话模式
  longPressTimer = setTimeout(() => {
    if (!dragMoved) startVoiceMode()
  }, 700)

  document.addEventListener('mousemove', documentMouseMove)
  document.addEventListener('mouseup', documentMouseUp)
}

function documentMouseMove(event) {
  if (isDragging.value) {
    const nx = event.clientX - dragStart.value.x
    const ny = event.clientY - dragStart.value.y
    // 位移未超阈值 → 保持原位不移动元素（避免 mouseup 时元素移位导致 click 不合成）
    if (!dragMoved) {
      if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null }
      const dx = Math.abs(nx - elfPosition.value.x)
      const dy = Math.abs(ny - elfPosition.value.y)
      if (dx < DRAG_THRESHOLD && dy < DRAG_THRESHOLD) return
      dragMoved = true
    }
    elfPosition.value = { x: nx, y: ny }
  }
}

function documentMouseUp() {
  isDragging.value = false
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null }
  const wasDragged = dragMoved
  // 仅真拖拽（有位移）才进入 click 忽略窗口；纯单击放行 click（互动/双击菜单正常）
  if (wasDragged) {
    lastDragEndAt = Date.now()
  }
  document.removeEventListener('mousemove', documentMouseMove)
  document.removeEventListener('mouseup', documentMouseUp)
  // 仅真拖拽松手才有“摔落”反应；纯单击不触发摔落台词（避免覆盖点击互动）
  if (wasDragged && threeRenderer && typeof threeRenderer.setDragging === 'function') {
    try { threeRenderer.setDragging(false) } catch (e) {}
    try { threeRenderer.drop() } catch (e) {}
  }
  if (wasDragged) {
    isDropping.value = true
    setTimeout(() => { isDropping.value = false }, 460)
    // 摔落台词延迟到鼠标互动窗口（~250ms）之后显示，避免被拖拽瞬间的随机互动覆盖
    setTimeout(() => {
      try { interaction.showDialog('哎哟！摔疼我啦～', 2400) } catch (e) {}
    }, 300)
    try { animate.triggerAction('surprised') } catch (e) {}
    spawnEffect('star', '⭐')
    resetIdleTimer()
    // 摔落动画播完再恢复自主移动（避免走路姿态盖掉摔落）
    setTimeout(() => {
      try {
        const abs = offsetToAbs(elfPosition.value.x, elfPosition.value.y)
        movement.setPosition(abs.x, abs.y)
        movement.resume()
      } catch (e) {}
    }, 500)
  } else {
    // 纯单击：恢复自主移动（若拖拽时暂停了）
    try { movement.resume() } catch (e) {}
  }
}

function handleMouseMove(event) {
}

function handleMouseUp() {
}

// ===== 移动端触摸支持：区分 tap 与 drag，修复移动端点击失效 =====
// 原实现只有 mousedown/mousemove/mouseup：移动端触摸时每次 tap 都会走"按下→拖拽→松手"流程，
// mouseup 后 350ms 内 click 被忽略，导致精灵互动/双击菜单/状态栏在手机上点不动。
// 触摸流程：touchstart 只记录起点（不进入拖拽）→ 位移超过阈值才视为拖拽 → 纯 tap 手动触发互动。
// 注意：模板不再无条件 touchstart.prevent，改为精灵本体按需 preventDefault（面板/菜单保留原生滚动与点击）；
// 精灵本体 preventDefault 后 Android Chrome 不会合成 click，因此纯 tap 必须手动触发。
let touchStartPoint = null
const TOUCH_DRAG_THRESHOLD = 10
let touchTarget = null
let suppressElfClick = false
let lastTapTime = 0
let tapClickTimer = null

function handleTouchStart(event) {
  const target = event.target
  // 面板/快捷菜单内部：不阻止默认行为，保证原生滚动、输入、点击可用
  if (target.closest('.elf-panel') || target.closest('.elf-quick-menu')) return
  // 精灵本体：阻止默认手势（页面滚动/缩放/文本选择），保证 tap/拖拽可控
  if (event.cancelable) event.preventDefault()
  touchTarget = target
  const t = event.touches && event.touches[0]
  if (!t) return
  touchStartPoint = { x: t.clientX, y: t.clientY }
  dragStart.value = {
    x: t.clientX - elfPosition.value.x,
    y: t.clientY - elfPosition.value.y
  }
}

function handleTouchMove(event) {
  const t = event.touches && event.touches[0]
  if (!t || !touchStartPoint) return
  if (!isDragging.value) {
    const dx = Math.abs(t.clientX - touchStartPoint.x)
    const dy = Math.abs(t.clientY - touchStartPoint.y)
    // 位移未超阈值 → 视为点击（tap），不进入拖拽
    if (dx < TOUCH_DRAG_THRESHOLD && dy < TOUCH_DRAG_THRESHOLD) return
    // 超过阈值才真正进入拖拽
    isDragging.value = true
    panelOpen.value = false
    try { movement.pause() } catch (e) {}
    if (threeRenderer && typeof threeRenderer.setDragging === 'function') {
      try { threeRenderer.setDragging(true) } catch (e) {}
    }
    resetIdleTimer()
  }
  if (isDragging.value) {
    if (event.cancelable) event.preventDefault()
    elfPosition.value = {
      x: t.clientX - dragStart.value.x,
      y: t.clientY - dragStart.value.y
    }
  }
}

function handleTouchCancel() {
  touchStartPoint = null
  if (isDragging.value) {
    isDragging.value = false
    lastDragEndAt = Date.now()
    if (threeRenderer && typeof threeRenderer.setDragging === 'function') {
      try { threeRenderer.setDragging(false) } catch (e) {}
      try { threeRenderer.drop() } catch (e) {}
    }
    try { animate.triggerAction('idle') } catch (e) {}
    try {
      const abs = offsetToAbs(elfPosition.value.x, elfPosition.value.y)
      movement.setPosition(abs.x, abs.y)
      movement.resume()
    } catch (e) {}
  }
  touchTarget = null
}

function handleTouchEnd(event) {
  touchStartPoint = null
  if (isDragging.value) {
    // 真拖拽松手：摔落动画 + click 忽略窗口（与鼠标拖拽一致）
    isDragging.value = false
    lastDragEndAt = Date.now()
    if (threeRenderer && typeof threeRenderer.setDragging === 'function') {
      try { threeRenderer.setDragging(false) } catch (e) {}
      try { threeRenderer.drop() } catch (e) {}
    }
    isDropping.value = true
    setTimeout(() => { isDropping.value = false }, 460)
    setTimeout(() => {
      try { interaction.showDialog('哎哟！摔疼我啦～', 2400) } catch (e) {}
    }, 300)
    try { animate.triggerAction('surprised') } catch (e) {}
    spawnEffect('star', '⭐')
    resetIdleTimer()
    setTimeout(() => {
      try {
        const abs = offsetToAbs(elfPosition.value.x, elfPosition.value.y)
        movement.setPosition(abs.x, abs.y)
        movement.resume()
      } catch (e) {}
    }, 500)
    touchTarget = null
    return
  }
  // 纯 tap：精灵本体手动触发互动/双击菜单
  // （touchstart 已 preventDefault → 浏览器不会合成原生 click，必须手动触发）
  const target = touchTarget || event.target
  touchTarget = null
  if (!target || !target.closest) return
  // 状态栏：在面板外，touchstart 已 preventDefault，手动触发其点击（toggleTimeDisplay）
  if (target.closest('.elf-status-bar')) {
    toggleTimeDisplay()
    suppressElfClick = true
    setTimeout(() => { suppressElfClick = false }, 450)
    return
  }
  // 面板/快捷菜单/输入框/按钮：touchstart 未 prevent（允许原生滚动与点击），
  // 由浏览器原生 click 处理即可，不再手动触发，避免双重触发
  if (
    target.closest('.elf-panel') ||
    target.closest('.elf-quick-menu') ||
    target.closest('input, textarea') ||
    target.closest('button, a, [role="button"]')
  ) {
    return
  }
  const now = Date.now()
  if (now - lastTapTime < 300) {
    // 双击 → 快捷菜单
    lastTapTime = 0
    if (tapClickTimer) { clearTimeout(tapClickTimer); tapClickTimer = null }
    suppressElfClick = false  // 手动触发前清除抑制，避免被自己吞掉
    handleDoubleClick({ target })
  } else {
    lastTapTime = now
    if (tapClickTimer) clearTimeout(tapClickTimer)
    tapClickTimer = setTimeout(() => {
      tapClickTimer = null
      lastTapTime = 0
      suppressElfClick = false  // 手动触发前清除抑制（浏览器 click 已在前 100ms 被 suppress 吞掉）
      handleClick({ target })
    }, 260)
  }
  // 抑制浏览器后续 click/dblclick 重复触发
  suppressElfClick = true
  setTimeout(() => { suppressElfClick = false }, 450)
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit',
    second: '2-digit'
  })
}

function toggleTimeDisplay() {
  showTime.value = !showTime.value
  if (showTime.value) {
    updateTime()
    if (timeInterval) {
      clearInterval(timeInterval)
    }
    timeInterval = setInterval(updateTime, 1000)
  } else {
    if (timeInterval) {
      clearInterval(timeInterval)
      timeInterval = null
    }
  }
}

function cleanup() {
  try { voiceLoop.stop() } catch (e) {}
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null }
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
  if (clickTimer) { clearTimeout(clickTimer); clickTimer = null }
  if (pressBounceTimer) { clearTimeout(pressBounceTimer); pressBounceTimer = null }
  if (reminderTimer) { clearTimeout(reminderTimer); reminderTimer = null }
  if (reminderTickTimer) { clearInterval(reminderTickTimer); reminderTickTimer = null }
  document.removeEventListener('mousemove', documentMouseMove)
  document.removeEventListener('mouseup', documentMouseUp)
}

// 3D 就绪后：按容器尺寸调整并把当前角色 mesh 送进去
watch(
  () => threeRenderer.isReady.value,
  (ready) => {
    if (ready) {
      nextTick(() => {
        try { threeRenderer.resize() } catch (e) {}
        sync3DMesh()
      })
    }
  }
)

// 模式切换：通知 3D 渲染器显隐 canvas，并在切到 3d 时重建 mesh
watch(
  () => elfConfig.value.mode,
  (mode) => {
    if (threeRenderer && typeof threeRenderer.setMode === 'function') {
      try { threeRenderer.setMode(mode) } catch (e) {}
    }
    if (mode === '3d') {
      nextTick(() => sync3DMesh())
    }
  }
)

// 自定义形象图片变化：重新同步 3D（存在 → 3D 立体立牌；清除 → 恢复程序化建模）
watch(
  () => elfConfig.value.customImage,
  () => {
    nextTick(() => sync3DMesh())
  }
)

// autoRotate 变化实时同步给 3D 渲染器配置
watch(
  () => elfConfig.value.autoRotate,
  (v) => {
    threeConfig.value = { ...threeConfig.value, autoRotate: v !== false }
  }
)

// 面板打开时暂停自主移动，关闭后恢复（避免边配置边乱跑）
watch(panelOpen, (open) => {
  try {
    if (open) movement.pause()
    else movement.resume()
  } catch (e) {}
})

onMounted(() => {
  // 从共享配置恢复角色/皮肤/换装
  applySavedAppearance()
  // 二次元桌宠迁移：配置版本 < 2 且当前角色非二次元时，
  // 自动切换到默认二次元女角色（樱奈），并写入 elfVersion=2 标记。
  // 之后用户在面板/快捷菜单中主动选择的任意角色（含生肖/基础）都会被尊重，不再强制切换。
  if (elfConfig.value.elfVersion !== 2 && !isAnimeCharacter.value) {
    currentCharacter.value = animeElfCharacters[0]
    updateElfConfig({ characterId: animeElfCharacters[0].id, elfVersion: 2 })
  }
  interaction.handleSystemEvent({ type: 'startup' }, (actionId) => {
    triggerAction3D(actionId)
  })
  // 自由移动系统：把移动起点同步到精灵当前（右下角锚点）绝对位置
  try {
    const abs = offsetToAbs(elfPosition.value.x, elfPosition.value.y)
    movement.setPosition(abs.x, abs.y)
  } catch (e) {}
  // 随机跳跃调度（行走中每 6~13 秒跳一次）
  scheduleRandomJump()
  // 高级互动：眼神跟随 + 待机瞌睡 + 随机卖萌
  gazeHandler = (e) => handleGazeMove(e)
  window.addEventListener('mousemove', gazeHandler)
  idleCheckTimer = setInterval(idleCheck, 8000)
  scheduleRandomTalk()
  // 养成状态拉取 + 久坐提醒 + 时段问候
  loadGrowthState()
  loadElfSoul()
  growthTimer = setInterval(loadGrowthState, 60000)
  inactivityTimer = setInterval(checkInactivityReminder, 60000)
  greetByTimePeriod()
})

onUnmounted(() => {
  if (randomJumpTimer) clearTimeout(randomJumpTimer)
  if (restActionTimer) clearTimeout(restActionTimer)
  if (gazeHandler) window.removeEventListener('mousemove', gazeHandler)
  if (idleCheckTimer) clearInterval(idleCheckTimer)
  if (randomTalkTimer) clearTimeout(randomTalkTimer)
  if (tapClickTimer) clearTimeout(tapClickTimer)
  if (pressBounceTimer) clearTimeout(pressBounceTimer)
  if (reminderTimer) clearTimeout(reminderTimer)
  if (reminderTickTimer) clearInterval(reminderTickTimer)
  cleanup()
})
</script>

<style scoped>
.desktop-elf {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  cursor: pointer;
  perspective: 800px;
  /* 注意：touch-action 不能放在根容器上，否则会连同面板一起禁用原生滚动。
     只对精灵本体（.elf-character）限制手势，面板/快捷菜单保留原生滚动与点击。 */
}

.elf-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  transform-style: preserve-3d;
}

.elf-character {
  width: 120px;
  height: 120px;
  position: relative;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1), filter 0.6s ease;
  transform-style: preserve-3d;
  filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.3));
  /* 移动端：阻止浏览器手势（滚动/缩放）抢占精灵本体的触摸事件，保证 tap/拖拽可控 */
  touch-action: none;
}

/* 昼夜联动：黑夜模式给精灵加月色冷光晕、压暗环境，白天模式明亮自然 */
.desktop-elf.elf-night .elf-character {
  filter: drop-shadow(0 8px 26px rgba(120, 170, 255, 0.38)) brightness(1.03);
}

.desktop-elf.elf-night .elf-3d-container {
  filter: hue-rotate(6deg) saturate(0.94) brightness(1.04);
}

.desktop-elf.elf-light .elf-character {
  filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.28)) brightness(1.05);
}

.desktop-elf.elf-night {
  /* 暗夜下精灵整体投月色投影 */
  text-shadow: 0 0 18px rgba(140, 190, 255, 0.25);
}

.elf-3d-container {
  position: absolute;
  inset: 0;
  width: 120px;
  height: 120px;
  z-index: 1;
  pointer-events: none;
}

.elf-3d-container canvas {
  width: 100% !important;
  height: 100% !important;
  display: block;
  pointer-events: auto;
}

.elf-character:hover {
  transform: rotateY(-15deg) rotateX(5deg) scale(1.05);
}

.elf-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
  transform: translateZ(20px);
}

/* 二次元立绘容器：略大于几何精灵，展示全身立绘 */
.anime-sprite {
  position: absolute;
  inset: 0;
  width: 128px;
  height: 168px;
  transform: translateY(-14px);
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, 0.28));
}

/* AI 精绘 PNG 立绘（2D 降级模式）：等比缩放不拉伸 */
.anime-sprite-img {
  width: 128px;
  height: 168px;
  object-fit: contain;
  transform: translateY(-14px);
  pointer-events: none;
}

/* 角色卡内的二次元小立绘 */
.char-anime-preview {
  width: 46px;
  height: 60px;
  display: block;
}

.elf-shadow {
  position: absolute;
  bottom: -15px;
  width: 80px;
  height: 12px;
  background: radial-gradient(ellipse, rgba(0, 0, 0, 0.4) 0%, transparent 70%);
  transform: rotateX(60deg) scale(0.8);
  transform-origin: center bottom;
  transition: all 0.3s ease;
}

.elf-character:hover + .elf-shadow {
  transform: rotateX(60deg) scale(1);
  opacity: 0.6;
}

.emotion-emoji {
  position: absolute;
  top: -35px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32px;
  animation: emotionFloat 1.5s ease-out forwards;
  pointer-events: none;
}

@keyframes emotionFloat {
  0% { transform: translateX(-50%) translateY(0) scale(0.5); opacity: 0; }
  20% { transform: translateX(-50%) translateY(-10px) scale(1.2); opacity: 1; }
  100% { transform: translateX(-50%) translateY(-40px) scale(1); opacity: 0; }
}

.special-effect {
  position: absolute;
  font-size: 24px;
  animation: effectPulse 2s ease-in-out infinite;
  pointer-events: none;
}

.special-effect.sparkle { top: -10px; left: 20px; animation-delay: 0s; }
.special-effect.fire { bottom: 0; right: -5px; animation-delay: 0.5s; }
.special-effect.wind { right: -15px; top: 50%; animation-delay: 0.3s; }
.special-effect.rainbow { top: -20px; animation-delay: 0s; }
.special-effect.water { bottom: 10px; left: -10px; animation-delay: 0.7s; }
.special-effect.lightning { top: 0; right: 0; animation-duration: 0.5s; }

@keyframes effectPulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

.dialog-bubble {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 15px;
  max-width: 220px;
  z-index: 30;
  animation: bubbleIn 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes bubbleIn {
  0% { opacity: 0; transform: translateX(-50%) scale(0.7); }
  100% { opacity: 1; transform: translateX(-50%) scale(1); }
}

.dialog-content {
  background: var(--bubble-accent, rgba(255, 255, 255, 0.98));
  backdrop-filter: blur(10px);
  padding: 12px 18px;
  border-radius: 18px;
  font-size: 13px;
  color: #ffffff;
  font-weight: 500;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.18);
  white-space: nowrap;
}

.dialog-arrow {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 9px solid transparent;
  border-right: 9px solid transparent;
  border-top: 10px solid var(--bubble-accent, rgba(255, 255, 255, 0.98));
}

/* ── 桌宠特效层 ── */
.elf-fx-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: visible;
  z-index: 20;
}
.elf-fx {
  position: absolute;
  top: 10%;
  font-size: 22px;
  animation: fxFloat 2s ease-out forwards;
}
/* 爱心：向上飘 + 渐隐 */
.fx-heart { animation-name: fxHeart; }
@keyframes fxHeart {
  0% { opacity: 0; transform: translateY(0) scale(0.6); }
  20% { opacity: 1; }
  100% { opacity: 0; transform: translateY(-70px) scale(1.1) rotate(-8deg); }
}
/* 星星：旋转 + 闪烁 */
.fx-star { animation-name: fxStar; }
@keyframes fxStar {
  0% { opacity: 0; transform: translateY(0) rotate(0deg) scale(0.5); }
  25% { opacity: 1; }
  100% { opacity: 0; transform: translateY(-60px) rotate(220deg) scale(1.2); }
}
/* 音符：左右摇摆上升 */
.fx-note { animation-name: fxNote; }
@keyframes fxNote {
  0% { opacity: 0; transform: translate(0, 0) scale(0.6); }
  25% { opacity: 1; }
  50% { transform: translate(6px, -30px) scale(1); }
  100% { opacity: 0; transform: translate(-6px, -70px) scale(1.1); }
}
/* 睡眠 zZ：渐大渐隐 */
.fx-zzz { animation-name: fxZzz; }
@keyframes fxZzz {
  0% { opacity: 0; transform: translate(0, 0) scale(0.5); }
  30% { opacity: 0.9; }
  100% { opacity: 0; transform: translate(10px, -50px) scale(1.5); }
}

/* ── 双击快捷菜单 ── */
/* 隐藏文件选择器：不占布局、不可见，仅由右键"更换形象"/拖拽逻辑触发 */
.elf-hidden-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
  overflow: hidden;
}
.elf-quick-menu {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  background: rgba(30, 41, 59, 0.96);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  z-index: 40;
  white-space: nowrap;
  animation: bubbleIn 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  touch-action: pan-y;
}
.quick-menu-item {
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  font-size: 12px;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}
.quick-menu-item:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ===== 动作表演面板（游戏角色经典动作） ===== */
.elf-act-menu {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 12px;
  width: 236px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.97);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 41;
  animation: bubbleIn 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  touch-action: pan-y;
}
.act-menu-title {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-align: center;
  margin-bottom: 10px;
  letter-spacing: 1px;
}
.act-menu-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
}
.act-menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 8px 4px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, transform 0.12s;
}
.act-menu-item:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}
.act-menu-item.active {
  background: rgba(244, 114, 182, 0.35);
  box-shadow: 0 0 0 1px rgba(244, 114, 182, 0.6);
}
.act-emoji {
  font-size: 20px;
  line-height: 1;
}
.act-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.92);
}
.act-menu-close {
  display: block;
  width: 100%;
  margin-top: 10px;
  padding: 8px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.act-menu-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ===== 精灵大小调节面板 ===== */
.elf-size-menu {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 12px;
  width: 200px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.97);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 41;
  animation: bubbleIn 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  touch-action: pan-y;
}
.size-slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.size-range {
  flex: 1;
  accent-color: #f472b6;
  cursor: pointer;
  height: 4px;
}
.size-value {
  font-size: 12px;
  color: #fff;
  min-width: 34px;
  text-align: right;
}
.size-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.size-preset-btn {
  padding: 7px 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, transform 0.12s;
}
.size-preset-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-1px);
}
.size-preset-btn.active {
  background: rgba(244, 114, 182, 0.35);
  box-shadow: 0 0 0 1px rgba(244, 114, 182, 0.6);
}

/* ===== 换装套装面板（换装小游戏级） ===== */
.elf-outfit-menu {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 12px;
  width: 230px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.97);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 41;
  animation: bubbleIn 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  touch-action: pan-y;
}
.outfit-set-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}
.outfit-tip {
  font-size: 11px;
  color: rgba(255, 214, 130, 0.9);
  background: rgba(255, 180, 60, 0.12);
  border: 1px solid rgba(255, 180, 60, 0.25);
  border-radius: 8px;
  padding: 5px 8px;
  margin-bottom: 8px;
  line-height: 1.4;
}
.outfit-set-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  transition: background 0.15s, transform 0.12s;
}
.outfit-set-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  transform: translateY(-1px);
}
.outfit-set-btn.active {
  background: rgba(167, 139, 250, 0.32);
  box-shadow: 0 0 0 1px rgba(167, 139, 250, 0.65);
}
.outfit-set-emoji {
  font-size: 20px;
  line-height: 1;
}
.outfit-set-name {
  font-size: 12px;
}

/* ===== 换装衣柜：页签 / 混搭 / 换色 ===== */
.mix-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.mix-tab {
  flex: 1;
  padding: 6px 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.mix-tab:hover {
  background: rgba(255, 255, 255, 0.16);
}
.mix-tab.active {
  background: rgba(167, 139, 250, 0.32);
  box-shadow: 0 0 0 1px rgba(167, 139, 250, 0.65);
}
.mix-panel {
  max-height: 220px;
  overflow-y: auto;
  margin-bottom: 10px;
  padding-right: 2px;
}
.mix-row {
  margin-bottom: 8px;
}
.mix-row-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 5px;
}
.mix-type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}
.mix-type-btn {
  padding: 5px 8px;
  border: none;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  cursor: pointer;
  transition: background 0.15s, transform 0.12s;
}
.mix-type-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  transform: translateY(-1px);
}
.mix-type-btn.active {
  background: rgba(244, 114, 182, 0.32);
  box-shadow: 0 0 0 1px rgba(244, 114, 182, 0.65);
}
.color-palette {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}
.color-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.25);
  cursor: pointer;
  transition: transform 0.12s, box-shadow 0.12s;
  padding: 0;
}
.color-dot:hover {
  transform: scale(1.15);
}
.color-dot.active {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.95);
  transform: scale(1.1);
}
.color-reset {
  padding: 4px 8px;
  border: none;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  font-size: 11px;
  cursor: pointer;
}
.color-reset:hover {
  background: rgba(255, 255, 255, 0.2);
}

.elf-status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 16px;
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  /* 动态壁纸化：平时隐藏，鼠标悬停桌宠时淡入（点状态栏可切换时间显示） */
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.25s ease, transform 0.25s ease;
  pointer-events: none;
}

.elf-growth-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding: 4px 12px;
  background: rgba(30, 41, 59, 0.9);
  border-radius: 14px;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.25s ease;
  pointer-events: none;
}
.desktop-elf:hover .elf-growth-bar { opacity: 1; pointer-events: auto; }
.growth-cell { display: inline-flex; align-items: center; gap: 3px; font-size: 12px; }

/* ===== 精灵灵魂状态条（灵魂引擎 elf 维度） ===== */
.elf-soul-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
  padding: 3px 12px;
  background: rgba(88, 28, 135, 0.75);
  border: 1px solid rgba(196, 181, 253, 0.35);
  border-radius: 12px;
  font-size: 11px;
  color: #e9d5ff;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.25s ease;
  pointer-events: none;
}
.desktop-elf:hover .elf-soul-bar { opacity: 1; pointer-events: auto; }
.soul-emotion { font-weight: 600; letter-spacing: 0.3px; }
.soul-emotion.soul-happy { color: #fcd34d; }
.soul-emotion.soul-sad { color: #93c5fd; }
.soul-emotion.soul-angry { color: #fca5a5; }
.soul-emotion.soul-anxious { color: #c4b5fd; }
.soul-version { opacity: 0.85; margin-left: auto; }
.growth-cell i {
  display: inline-block; width: 34px; height: 5px; border-radius: 3px;
  background: rgba(255,255,255,0.18); overflow: hidden;
}
.growth-cell i b { display: block; height: 100%; background: #ff6f9c; border-radius: 3px; }
.growth-feed-btn {
  border: none; background: rgba(255,255,255,0.15); border-radius: 10px;
  cursor: pointer; font-size: 13px; padding: 2px 6px;
}

.desktop-elf:hover .elf-status-bar {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

/* 移动端（无 hover 能力）：状态栏常显，避免"点不到" */
@media (hover: none), (pointer: coarse) {
  .desktop-elf .elf-status-bar {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }
  /* 移动端面板/快捷菜单宽度与位置适配：避免溢出屏幕边缘 */
  .elf-panel {
    right: 0;
    left: auto;
  }
  .elf-quick-menu {
    max-width: calc(100vw - 24px);
    white-space: normal;
  }
  .elf-quick-menu .quick-menu-item {
    font-size: 13px;
    padding: 9px 16px;
  }
  .panel-tabs button {
    font-size: 13px;
    padding: 10px 4px;
  }
  .panel-content {
    padding: 12px;
    max-height: min(450px, 55vh);
  }
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.status-dot.happy { background: #4ade80; }
.status-dot.sad { background: #60a5fa; }
.status-dot.excited { background: #fbbf24; }
.status-dot.sleepy { background: #a78bfa; }
.status-dot.thinking { background: #f472b6; }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.9); }
}

.status-text {
  color: white;
  font-size: 13px;
  font-weight: 600;
}

.status-sub {
  color: rgba(255, 255, 255, 0.6);
  font-size: 11px;
}

.elf-panel {
  position: absolute;
  bottom: 100%;
  right: 0;
  width: 420px;
  max-width: calc(100vw - 24px);
  background: rgba(30, 41, 59, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 
    0 -8px 32px rgba(0, 0, 0, 0.5),
    0 0 40px rgba(79, 70, 229, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  opacity: 0;
  transform: translateY(20px) rotateX(5deg);
  visibility: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 15px;
  overflow: hidden;
  transform-style: preserve-3d;
  /* 面板内部允许垂直滚动（聊天记录/角色网格/动作网格），不继承精灵本体的手势限制 */
  touch-action: pan-y;
}

.elf-panel.open {
  opacity: 1;
  transform: translateY(0) rotateX(0deg);
  visibility: visible;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

/* 面板顶部角色色渐变条：让角色色板真正进入面板 */
.panel-accent-bar {
  height: 4px;
  background: linear-gradient(90deg, var(--panel-accent, #4f46e5), rgba(255, 255, 255, 0.25), var(--panel-accent, #4f46e5));
  border-radius: 0 0 6px 6px;
}

.panel-header h3 {
  color: white;
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-header h3::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--panel-accent, #4f46e5);
  box-shadow: 0 0 8px var(--panel-accent, #4f46e5);
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-center-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(79, 70, 229, 0.2);
  color: #c7d2fe;
  border-radius: 16px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.config-center-btn:hover {
  background: rgba(79, 70, 229, 0.4);
  color: white;
}

.panel-tabs {
  display: flex;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-tabs button {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.panel-tabs button.active {
  color: white;
}

.panel-tabs button.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 2px;
  background: var(--panel-accent, #4f46e5);
  border-radius: 1px;
}

.panel-content {
  padding: 16px;
  max-height: 450px;
  overflow-y: auto;
}

.tab-content {
  animation: fadeIn 0.2s ease;
}

/* ── 走路摇摆（移动系统驱动）：前后倾 + 上下起伏，呈现真实步态而非平移 ── */
.elf-character.walking {
  animation: elfWalkBob 0.55s ease-in-out infinite;
}
@keyframes elfWalkBob {
  0%, 100% { transform: translateY(0) rotate(-2.5deg) scaleY(1); }
  50% { transform: translateY(-6px) rotate(2.5deg) scaleY(0.97); }
}

/* ── 跳跃（碰边折返 / 随机跳跃）：起跳拉伸 + 落地压缩 ── */
.elf-character.jumping {
  animation: elfJump 0.55s ease-out;
}
@keyframes elfJump {
  0% { transform: translateY(0); }
  35% { transform: translateY(-22px) scaleY(1.08); }
  70% { transform: translateY(0) scaleY(0.94); }
  100% { transform: translateY(0); }
}

/* 朝左走：2D 立绘镜像翻转（3D 由渲染器转身） */
.elf-character.walking-left .anime-sprite {
  transform: translateY(-14px) scaleX(-1);
}

/* ── 拖拽拎起挣扎（2D 降级）：快速左右摇晃 ── */
.elf-character.dragging {
  animation: elfWiggle 0.28s ease-in-out infinite;
}
@keyframes elfWiggle {
  0%, 100% { transform: rotate(-6deg) translateY(-6px); }
  50% { transform: rotate(6deg) translateY(-10px); }
}

/* ── 松手摔落弹跳（2D 降级）：悬空下落 + 落地压缩回弹 ── */
.elf-character.drop-bounce {
  animation: elfDrop 0.46s ease-out;
}
@keyframes elfDrop {
  0% { transform: translateY(-26px); }
  55% { transform: translateY(0) scaleY(0.88); }
  75% { transform: translateY(0) scaleY(1.06); }
  100% { transform: translateY(0) scaleY(1); }
}

/* ── 点击按压回弹（Squash & Stretch）：压缩 → 超调 → 复位 ── */
.elf-character.press-bounce {
  animation: elfPress 0.24s ease-out;
}
@keyframes elfPress {
  0% { transform: scaleY(1) scaleX(1); }
  40% { transform: scaleY(0.88) scaleX(1.1); }
  70% { transform: scaleY(1.08) scaleX(0.96); }
  100% { transform: scaleY(1) scaleX(1); }
}

/* ── 快捷提醒面板 ── */
.elf-reminder-panel {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 12px;
  min-width: 210px;
  padding: 14px;
  background: rgba(30, 41, 59, 0.97);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  z-index: 45;
  animation: bubbleIn 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
  touch-action: pan-y;
}
.reminder-title {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.reminder-quick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}
.reminder-quick-btn {
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 12px;
  padding: 7px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.reminder-quick-btn:hover {
  background: rgba(255, 255, 255, 0.22);
}
.reminder-custom {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.reminder-custom input {
  flex: 1;
  min-width: 0;
  padding: 7px 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
}
.reminder-custom input:focus {
  border-color: var(--panel-accent, #4f46e5);
}
.reminder-custom-btn {
  border: none;
  background: var(--panel-accent, #4f46e5);
  color: #fff;
  font-size: 12px;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
}
.reminder-custom-btn:hover {
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 78%, #000);
}
.reminder-active {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 18%, transparent);
  border: 1px solid color-mix(in srgb, var(--panel-accent, #4f46e5) 40%, transparent);
  border-radius: 8px;
  color: color-mix(in srgb, var(--panel-accent, #4f46e5) 55%, #fff);
  font-size: 12px;
  margin-bottom: 10px;
}
.reminder-cancel-btn {
  border: none;
  background: rgba(255, 255, 255, 0.12);
  color: #fca5a5;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
}
.reminder-close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.reminder-close-btn:hover {
  background: rgba(255, 255, 255, 0.22);
}

/* ── 对话 tab ── */
.chat-tab {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-offline-hint {
  font-size: 11px;
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.12);
  border: 1px solid rgba(251, 191, 36, 0.3);
  padding: 6px 10px;
  border-radius: 8px;
}
.chat-messages {
  height: 260px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px;
}
.chat-empty {
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
  margin-top: 80px;
  font-size: 13px;
}
.chat-bubble {
  display: flex;
  max-width: 85%;
}
.chat-bubble.user {
  align-self: flex-end;
}
.chat-bubble.assistant {
  align-self: flex-start;
}
.chat-bubble-content {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.chat-bubble.user .chat-bubble-content {
  background: var(--panel-accent, #4f46e5);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-bubble.assistant .chat-bubble-content {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.92);
  border-bottom-left-radius: 4px;
}
.chat-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: chatBlink 0.8s steps(1) infinite;
}
@keyframes chatBlink {
  50% { opacity: 0; }
}
.chat-input-row {
  display: flex;
  gap: 8px;
}
.chat-input-row input {
  flex: 1;
  padding: 9px 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  border-radius: 10px;
  font-size: 13px;
  outline: none;
}
.chat-input-row input:focus {
  border-color: var(--panel-accent, #4f46e5);
}
.chat-send-btn {
  padding: 9px 16px;
  border: none;
  background: var(--panel-accent, #4f46e5);
  color: #fff;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
}
.chat-send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.movement-toggles {
  display: flex;
  gap: 16px;
  padding-top: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 10px;
}
.movement-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
  cursor: pointer;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

/* ── 自定义形象（用户上传图片） ── */
.custom-image-section {
  margin-bottom: 14px;
  padding: 12px;
  border: 1px dashed rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
}
.custom-image-section.has-image {
  border-style: solid;
  border-color: var(--panel-accent, #4f46e5);
}
.custom-image-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  gap: 8px;
}
.custom-image-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
}
.custom-image-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.custom-image-body {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.custom-image-preview {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}
.custom-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.custom-image-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.custom-image-empty {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  border: 1px dashed rgba(255, 255, 255, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 9px;
  flex-shrink: 0;
  text-align: center;
}
.custom-image-upload-icon {
  font-size: 16px;
  line-height: 1;
}
.custom-image-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid var(--panel-accent, #4f46e5);
  background: var(--panel-accent, #4f46e5);
  color: #fff;
  border-radius: 18px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.custom-image-upload-btn:hover {
  opacity: 0.85;
}
.custom-image-file-input {
  display: none;
}
.custom-image-reset-btn {
  padding: 7px 12px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  border-radius: 18px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.custom-image-reset-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}
.custom-image-bg-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  user-select: none;
}
.custom-image-bg-toggle input {
  width: 14px;
  height: 14px;
  accent-color: var(--panel-accent, #4f46e5);
  cursor: pointer;
}

.category-tabs button {
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tabs button.active {
  background: var(--panel-accent, #4f46e5);
  border-color: var(--panel-accent, #4f46e5);
  color: white;
}

.char-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.char-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
  position: relative;
}

.char-card:hover {
  background: rgba(255, 255, 255, 0.1);
}

.char-card.selected {
  border-color: var(--panel-accent, #4f46e5);
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 20%, transparent);
}

.char-emoji {
  font-size: 32px;
}

.char-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.char-info .char-name {
  color: white;
  font-size: 13px;
  font-weight: 600;
}

.char-info .char-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}

.char-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
}

.char-tag.zodiac {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.char-tag.anime {
  background: rgba(236, 72, 153, 0.2);
  color: #f472b6;
}

.char-tag.myth {
  background: rgba(167, 139, 250, 0.2);
  color: #a78bfa;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.action-btn.active {
  border-color: var(--panel-accent, #4f46e5);
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 20%, transparent);
}

.action-icon {
  font-size: 28px;
  margin-bottom: 6px;
}

.action-name {
  color: white;
  font-size: 11px;
}

.skin-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.skin-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.skin-card:hover {
  background: rgba(255, 255, 255, 0.1);
}

.skin-card.selected {
  border-color: var(--panel-accent, #4f46e5);
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 20%, transparent);
}

.skin-preview {
  width: 45px;
  height: 45px;
  border-radius: 50%;
}

.skin-name {
  color: white;
  font-size: 11px;
}

.outfit-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.outfit-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.outfit-card:hover {
  background: rgba(255, 255, 255, 0.1);
}

.outfit-card.selected {
  border-color: var(--panel-accent, #4f46e5);
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 20%, transparent);
}

.outfit-emoji {
  font-size: 28px;
}

.outfit-name {
  color: white;
  font-size: 11px;
}

.widgets-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}

.widgets-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  line-height: 1.5;
}

.open-designer-btn {
  padding: 10px 14px;
  border: none;
  border-radius: 10px;
  background: var(--panel-accent, #4f46e5);
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.open-designer-btn:hover {
  background: color-mix(in srgb, var(--panel-accent, #4f46e5) 78%, #000);
  transform: translateY(-1px);
}

.widgets-empty {
  padding: 24px 12px;
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.widgets-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.widget-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.widget-row:hover {
  background: rgba(255, 255, 255, 0.1);
}

.widget-row-name {
  color: white;
  font-size: 13px;
}

.widget-row-arrow {
  color: rgba(255, 255, 255, 0.4);
  font-size: 16px;
}

::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ===== 语音对话模式浮层（M2 ⑥） ===== */
.elf-voice-overlay {
  position: absolute;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 60;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 22px;
  background: rgba(20, 22, 30, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.35);
  min-width: 200px;
}
.voice-wave {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 34px;
}
.voice-wave .wave-bar {
  width: 5px;
  height: 8px;
  border-radius: 3px;
  background: #6ee7b7;
  opacity: 0.4;
}
.voice-wave.listening .wave-bar,
.voice-wave.speaking .wave-bar {
  animation: voiceWave 1s ease-in-out infinite;
}
@keyframes voiceWave {
  0%, 100% { height: 8px; }
  50% { height: 30px; }
}
.voice-status {
  font-size: 13px;
  color: #e2e8f0;
}
.voice-transcript {
  font-size: 12px;
  color: #94a3b8;
  max-width: 220px;
  text-align: center;
}
.voice-close-btn {
  margin-top: 4px;
  padding: 6px 14px;
  font-size: 12px;
  color: #fca5a5;
  background: transparent;
  border: 1px solid rgba(252, 165, 165, 0.4);
  border-radius: 999px;
  cursor: pointer;
}
.voice-close-btn:hover { background: rgba(252, 165, 165, 0.12); }
</style>
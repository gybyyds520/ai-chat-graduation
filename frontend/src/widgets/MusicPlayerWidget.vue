<template>
  <div class="music-widget">
    <div class="music-header">
      <h3 class="music-title">🎵 音乐</h3>
      <div class="header-actions">
        <button class="import-btn" @click="triggerFileImport">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>导入</span>
        </button>
        <input 
          type="file" 
          ref="fileInput" 
          class="hidden-file-input"
          accept="audio/*"
          multiple
          @change="handleFileImport"
        />
      </div>
    </div>

    <div v-if="currentTrack" class="player-section">
      <div class="album-art">
        <div class="art-placeholder">
          <span class="art-icon">🎵</span>
        </div>
      </div>
      
      <div class="track-info">
        <h4 class="track-name">{{ currentTrack.name }}</h4>
        <p class="track-meta">{{ currentTrack.artist }} - {{ currentTrack.album }}</p>
      </div>

      <div class="progress-section">
        <span class="time-label">{{ formatDuration(progress.currentTime) }}</span>
        <div class="progress-bar" @click="handleProgressClick">
          <div class="progress-fill" :style="{ width: progress.progress + '%' }"></div>
          <div class="progress-thumb" :style="{ left: progress.progress + '%' }"></div>
        </div>
        <span class="time-label">{{ formatDuration(progress.duration) }}</span>
      </div>

      <div class="controls-section">
        <button class="control-btn" @click="musicPlayer.previous()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="19 20 9 12 19 4 19 20"/>
            <polygon points="5 20 15 12 5 4 5 20"/>
          </svg>
        </button>
        
        <button class="play-btn" @click="musicPlayer.togglePlay()">
          <svg v-if="!isPlaying" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="6" y="4" width="4" height="16"/>
            <rect x="14" y="4" width="4" height="16"/>
          </svg>
        </button>
        
        <button class="control-btn" @click="musicPlayer.next()">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 4 15 12 5 20 5 4"/>
            <polygon points="19 4 9 12 19 20 19 4"/>
          </svg>
        </button>

        <button class="mode-btn" @click="cycleMode" :title="playModeLabel">
          {{ playModeIcon }}
        </button>
      </div>

      <div class="volume-section">
        <button class="volume-btn" @click="musicPlayer.toggleMute()">
          <svg v-if="volume > 0" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <path d="M15.54 8.46a5 5 0 010 7.07"/>
            <path d="M19.07 4.93a10 10 0 010 14.14"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <line x1="23" y1="9" x2="17" y2="15"/>
            <line x1="17" y1="9" x2="23" y2="15"/>
          </svg>
        </button>
        <input 
          type="range" 
          min="0" 
          max="100" 
          :value="volume * 100"
          @input="handleVolumeChange"
          class="volume-slider"
        />
      </div>
    </div>

    <div v-else class="empty-player">
      <div class="empty-icon">🎵</div>
      <p>暂无音乐</p>
      <button class="empty-btn" @click="triggerFileImport">导入音乐文件</button>
    </div>

    <div class="library-section">
      <div class="library-tabs">
        <button 
          v-for="tab in libraryTabs" 
          :key="tab.id"
          @click="currentTab = tab.id"
          :class="{ active: currentTab === tab.id }"
          class="tab-btn"
        >
          {{ tab.name }}
        </button>
      </div>

      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索音乐..."
          class="search-input"
        />
      </div>

      <div class="track-list">
        <div 
          v-for="(track, index) in filteredTracks" 
          :key="track.id"
          class="track-item"
          :class="{ active: currentTrack?.id === track.id }"
          @click="playTrack(index)"
        >
          <span class="track-number">{{ index + 1 }}</span>
          <div class="track-details">
            <span class="track-title">{{ track.name }}</span>
            <span class="track-artist">{{ track.artist }}</span>
          </div>
          <span class="track-duration">{{ formatDuration(track.duration) }}</span>
        </div>

        <div v-if="filteredTracks.length === 0" class="empty-track-list">
          <p>{{ searchQuery ? '未找到匹配的音乐' : '暂无音乐，请导入' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import musicPlayer, { PLAY_MODES } from '../utils/musicPlayer'

const fileInput = ref(null)
const currentTrack = ref(null)
const isPlaying = ref(false)
const progress = ref({ currentTime: 0, duration: 0, progress: 0 })
const volume = ref(0.7)
const currentTab = ref('all')
const searchQuery = ref('')

const libraryTabs = [
  { id: 'all', name: '全部' },
  { id: 'artist', name: '艺术家' },
  { id: 'album', name: '专辑' }
]

const playModeLabel = computed(() => {
  return musicPlayer.getPlayModeLabel(musicPlayer.getPlayMode())
})

const playModeIcon = computed(() => {
  return musicPlayer.getPlayModeIcon(musicPlayer.getPlayMode())
})

const allTracks = computed(() => {
  return musicPlayer.getPlaylist()
})

const filteredTracks = computed(() => {
  if (!searchQuery.value) return allTracks.value
  return musicPlayer.searchTracks(searchQuery.value)
})

function formatDuration(seconds) {
  return musicPlayer.formatDuration(seconds)
}

function triggerFileImport() {
  fileInput.value?.click()
}

function handleFileImport(event) {
  const files = Array.from(event.target.files)
  if (files.length > 0) {
    musicPlayer.addTracks(files)
    event.target.value = ''
  }
}

function playTrack(index) {
  musicPlayer.play(index)
}

function handleProgressClick(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - rect.left
  const percentage = (x / rect.width) * 100
  musicPlayer.seek(percentage)
}

function handleVolumeChange(event) {
  const value = parseInt(event.target.value) / 100
  volume.value = value
  musicPlayer.setVolume(value)
}

function cycleMode() {
  musicPlayer.cyclePlayMode()
}

function updatePlayerState() {
  currentTrack.value = musicPlayer.getCurrentTrack()
  isPlaying.value = musicPlayer.isPlaying()
  progress.value = musicPlayer.getProgressInfo()
  volume.value = musicPlayer.getVolume()
}

let unsubscribe = null

onMounted(() => {
  updatePlayerState()
  
  unsubscribe = musicPlayer.subscribe((event, data) => {
    switch (event) {
      case 'play':
      case 'pause':
      case 'stop':
      case 'tracksAdded':
      case 'trackRemoved':
      case 'playlistChanged':
        updatePlayerState()
        break
      case 'timeupdate':
        progress.value = data
        break
      case 'volumeChange':
        volume.value = data
        break
    }
  })
})

onUnmounted(() => {
  if (unsubscribe) {
    unsubscribe()
  }
})
</script>

<style scoped>
.music-widget {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  color: #ffffff;
  overflow: hidden;
}

.music-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.music-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.import-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.import-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.hidden-file-input {
  display: none;
}

.player-section {
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.album-art {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.art-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.3), rgba(139, 92, 246, 0.3));
  display: flex;
  align-items: center;
  justify-content: center;
}

.art-icon {
  font-size: 32px;
}

.track-info {
  text-align: center;
  margin-bottom: 12px;
}

.track-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
}

.track-meta {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.time-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  min-width: 40px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  cursor: pointer;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4f46e5, #8b5cf6);
  border-radius: 3px;
  transition: width 0.1s linear;
}

.progress-thumb {
  width: 14px;
  height: 14px;
  background: #ffffff;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  transition: left 0.1s linear;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.controls-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.control-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.play-btn {
  width: 52px;
  height: 52px;
  border: none;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  border-radius: 50%;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.play-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.5);
}

.mode-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.volume-section {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.volume-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.volume-btn:hover {
  color: #ffffff;
}

.volume-slider {
  width: 100px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  outline: none;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  background: #ffffff;
  border-radius: 50%;
  cursor: pointer;
}

.volume-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  background: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.empty-player {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-player p {
  margin: 0 0 16px 0;
  font-size: 14px;
}

.empty-btn {
  padding: 10px 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  border-radius: 8px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.library-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.library-tabs {
  display: flex;
  padding: 8px 12px;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tab-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: rgba(255, 255, 255, 0.8);
}

.tab-btn.active {
  background: rgba(79, 70, 229, 0.3);
  color: #ffffff;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
}

.search-box svg {
  color: rgba(255, 255, 255, 0.5);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 13px;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.track-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.track-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.track-item.active {
  background: rgba(79, 70, 229, 0.2);
}

.track-number {
  width: 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  text-align: center;
}

.track-item.active .track-number {
  color: #4f46e5;
}

.track-details {
  flex: 1;
  overflow: hidden;
}

.track-title {
  display: block;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-artist {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

.track-duration {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  min-width: 45px;
  text-align: right;
}

.empty-track-list {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-track-list p {
  margin: 0;
  font-size: 13px;
}
</style>
import { createLogger } from './logger'

const logger = createLogger('MusicPlayer')

const STORAGE_KEY = 'desktop-elf-music-library'
const PLAYBACK_STATE_KEY = 'desktop-elf-playback-state'

const PLAY_MODES = {
  SEQUENCE: 'sequence',
  SHUFFLE: 'shuffle',
  LOOP_ONE: 'loop_one',
  LOOP_ALL: 'loop_all'
}

class MusicPlayer {
  constructor() {
    this.audio = new Audio()
    this.library = []
    this.playlist = []
    this.currentIndex = -1
    this.playMode = PLAY_MODES.SEQUENCE
    this.volume = 0.7
    this.isPlaying = false
    this.listeners = new Set()
    this.progressInterval = null
    
    this.init()
  }

  init() {
    this.loadLibrary()
    this.loadPlaybackState()
    this.setupAudioEvents()
    this.audio.volume = this.volume
  }

  loadLibrary() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        this.library = JSON.parse(saved)
        this.playlist = [...this.library]
        logger.info('MUSIC_LIBRARY_LOADED', `Loaded ${this.library.length} songs`)
      }
    } catch (e) {
      logger.error('MUSIC_LIBRARY_LOAD_FAILED', 'Failed to load music library', { error: e.message })
      this.library = []
      this.playlist = []
    }
  }

  saveLibrary() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.library))
      logger.info('MUSIC_LIBRARY_SAVED', `Saved ${this.library.length} songs`)
    } catch (e) {
      logger.error('MUSIC_LIBRARY_SAVE_FAILED', 'Failed to save music library', { error: e.message })
    }
  }

  loadPlaybackState() {
    try {
      const saved = localStorage.getItem(PLAYBACK_STATE_KEY)
      if (saved) {
        const state = JSON.parse(saved)
        this.playMode = state.playMode || PLAY_MODES.SEQUENCE
        this.volume = state.volume || 0.7
      }
    } catch (e) {
      logger.warn('PLAYBACK_STATE_LOAD_FAILED', 'Failed to load playback state', { error: e.message })
    }
  }

  savePlaybackState() {
    try {
      const state = {
        playMode: this.playMode,
        volume: this.volume
      }
      localStorage.setItem(PLAYBACK_STATE_KEY, JSON.stringify(state))
    } catch (e) {
      logger.warn('PLAYBACK_STATE_SAVE_FAILED', 'Failed to save playback state', { error: e.message })
    }
  }

  setupAudioEvents() {
    this.audio.addEventListener('ended', () => {
      this.onTrackEnded()
    })

    this.audio.addEventListener('timeupdate', () => {
      this.notifyListeners('timeupdate', this.getProgressInfo())
    })

    this.audio.addEventListener('loadedmetadata', () => {
      this.notifyListeners('metadata', this.getCurrentTrack())
    })

    this.audio.addEventListener('error', (e) => {
      logger.error('AUDIO_ERROR', 'Audio playback error', { error: e.message })
      this.notifyListeners('error', e)
    })

    this.audio.addEventListener('play', () => {
      this.isPlaying = true
      this.notifyListeners('play', this.getCurrentTrack())
    })

    this.audio.addEventListener('pause', () => {
      this.isPlaying = false
      this.notifyListeners('pause', this.getCurrentTrack())
    })
  }

  getLibrary() {
    return [...this.library]
  }

  getPlaylist() {
    return [...this.playlist]
  }

  getCurrentTrack() {
    if (this.currentIndex >= 0 && this.currentIndex < this.playlist.length) {
      return {
        ...this.playlist[this.currentIndex],
        index: this.currentIndex,
        total: this.playlist.length
      }
    }
    return null
  }

  getProgressInfo() {
    return {
      currentTime: this.audio.currentTime,
      duration: this.audio.duration || 0,
      progress: this.audio.duration ? (this.audio.currentTime / this.audio.duration) * 100 : 0
    }
  }

  getPlayMode() {
    return this.playMode
  }

  getVolume() {
    return this.volume
  }

  isPlaying() {
    return this.isPlaying
  }

  addTracks(files) {
    const newTracks = []
    
    files.forEach(file => {
      const url = URL.createObjectURL(file)
      const track = {
        id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
        name: file.name.replace(/\.[^/.]+$/, ''),
        artist: '未知艺术家',
        album: '未知专辑',
        url: url,
        fileType: file.type,
        duration: 0,
        size: file.size,
        addedAt: Date.now()
      }
      
      this.extractMetadata(track, file)
      newTracks.push(track)
    })

    this.library.push(...newTracks)
    this.playlist = [...this.library]
    this.saveLibrary()
    this.notifyListeners('tracksAdded', newTracks)
    logger.info('TRACKS_ADDED', `Added ${newTracks.length} tracks to library`)

    return newTracks
  }

  extractMetadata(track, file) {
    const audio = new Audio()
    audio.src = track.url
    audio.preload = 'metadata'
    
    audio.addEventListener('loadedmetadata', () => {
      track.duration = audio.duration
      this.saveLibrary()
      this.notifyListeners('trackUpdated', track)
      logger.debug('METADATA_EXTRACTED', `Track duration: ${track.name} - ${track.duration}s`)
    })
    
    audio.addEventListener('error', () => {
      logger.warn('METADATA_EXTRACT_FAILED', `Failed to extract metadata for: ${track.name}`)
    })
  }

  removeTrack(id) {
    const index = this.library.findIndex(t => t.id === id)
    if (index === -1) {
      logger.warn('TRACK_REMOVE_FAILED', `Track not found: ${id}`)
      return false
    }

    const track = this.library[index]
    if (track.url) {
      URL.revokeObjectURL(track.url)
    }

    this.library.splice(index, 1)
    this.playlist = this.playlist.filter(t => t.id !== id)
    
    if (this.currentIndex >= this.playlist.length) {
      this.currentIndex = Math.max(0, this.playlist.length - 1)
    }

    this.saveLibrary()
    this.notifyListeners('trackRemoved', track)
    logger.info('TRACK_REMOVED', `Removed track: ${id}`)

    return true
  }

  clearLibrary() {
    this.library.forEach(track => {
      if (track.url) {
        URL.revokeObjectURL(track.url)
      }
    })
    
    this.library = []
    this.playlist = []
    this.currentIndex = -1
    this.stop()
    this.saveLibrary()
    this.notifyListeners('libraryCleared')
    logger.info('LIBRARY_CLEARED', 'Music library cleared')
  }

  setPlaylist(tracks) {
    this.playlist = tracks
    this.currentIndex = -1
    this.notifyListeners('playlistChanged', tracks)
    logger.info('PLAYLIST_CHANGED', `Playlist set with ${tracks.length} tracks`)
  }

  play(index = 0) {
    if (this.playlist.length === 0) {
      logger.warn('PLAY_FAILED', 'Playlist is empty')
      return false
    }

    if (index < 0 || index >= this.playlist.length) {
      logger.warn('PLAY_FAILED', `Invalid index: ${index}`)
      return false
    }

    this.currentIndex = index
    const track = this.playlist[index]
    
    this.audio.src = track.url
    this.audio.load()
    this.audio.play().then(() => {
      this.isPlaying = true
      this.notifyListeners('play', { ...track, index, total: this.playlist.length })
      logger.info('PLAY_TRACK', `Playing track: ${track.name}`)
    }).catch(e => {
      logger.error('PLAY_FAILED', 'Failed to play track', { error: e.message })
      this.notifyListeners('error', e)
    })

    return true
  }

  pause() {
    if (this.audio.paused) return
    
    this.audio.pause()
    this.isPlaying = false
    this.notifyListeners('pause', this.getCurrentTrack())
    logger.debug('PAUSE_TRACK', 'Track paused')
  }

  togglePlay() {
    if (this.isPlaying) {
      this.pause()
    } else if (this.currentIndex >= 0) {
      this.audio.play().then(() => {
        this.isPlaying = true
        this.notifyListeners('play', this.getCurrentTrack())
      }).catch(e => {
        logger.error('TOGGLE_PLAY_FAILED', 'Failed to toggle play', { error: e.message })
      })
    } else if (this.playlist.length > 0) {
      this.play(0)
    }
  }

  stop() {
    this.audio.pause()
    this.audio.currentTime = 0
    this.isPlaying = false
    this.notifyListeners('stop')
    logger.debug('STOP_TRACK', 'Track stopped')
  }

  next() {
    if (this.playlist.length === 0) return

    let nextIndex
    
    switch (this.playMode) {
      case PLAY_MODES.SHUFFLE:
        nextIndex = Math.floor(Math.random() * this.playlist.length)
        break
      case PLAY_MODES.LOOP_ONE:
        nextIndex = this.currentIndex
        break
      case PLAY_MODES.LOOP_ALL:
        nextIndex = (this.currentIndex + 1) % this.playlist.length
        break
      case PLAY_MODES.SEQUENCE:
      default:
        nextIndex = this.currentIndex + 1
        if (nextIndex >= this.playlist.length) {
          this.stop()
          return
        }
        break
    }

    this.play(nextIndex)
  }

  previous() {
    if (this.playlist.length === 0) return

    let prevIndex
    
    if (this.audio.currentTime > 3) {
      this.audio.currentTime = 0
      return
    }

    switch (this.playMode) {
      case PLAY_MODES.SHUFFLE:
        prevIndex = Math.floor(Math.random() * this.playlist.length)
        break
      case PLAY_MODES.LOOP_ONE:
        prevIndex = this.currentIndex
        break
      case PLAY_MODES.LOOP_ALL:
        prevIndex = (this.currentIndex - 1 + this.playlist.length) % this.playlist.length
        break
      case PLAY_MODES.SEQUENCE:
      default:
        prevIndex = Math.max(0, this.currentIndex - 1)
        break
    }

    this.play(prevIndex)
  }

  onTrackEnded() {
    logger.debug('TRACK_ENDED', `Track ended: ${this.currentIndex}`)
    this.next()
  }

  seek(progress) {
    if (!this.audio.duration) return
    
    const time = (progress / 100) * this.audio.duration
    this.audio.currentTime = time
    this.notifyListeners('seek', { progress, time })
    logger.debug('SEEK', `Seeked to ${progress}%`)
  }

  setVolume(volume) {
    this.volume = Math.max(0, Math.min(1, volume))
    this.audio.volume = this.volume
    this.savePlaybackState()
    this.notifyListeners('volumeChange', this.volume)
    logger.debug('VOLUME_CHANGE', `Volume set to ${this.volume}`)
  }

  toggleMute() {
    if (this.audio.muted) {
      this.audio.muted = false
      this.audio.volume = this.volume
    } else {
      this.audio.muted = true
    }
    this.notifyListeners('muteChange', this.audio.muted)
    logger.debug('MUTE_TOGGLE', `Mute: ${this.audio.muted}`)
  }

  setPlayMode(mode) {
    if (!Object.values(PLAY_MODES).includes(mode)) {
      logger.warn('PLAY_MODE_INVALID', `Invalid play mode: ${mode}`)
      return
    }

    this.playMode = mode
    this.savePlaybackState()
    this.notifyListeners('playModeChange', mode)
    logger.info('PLAY_MODE_CHANGE', `Play mode changed to: ${mode}`)
  }

  cyclePlayMode() {
    const modes = Object.values(PLAY_MODES)
    const currentIndex = modes.indexOf(this.playMode)
    const nextMode = modes[(currentIndex + 1) % modes.length]
    this.setPlayMode(nextMode)
    return nextMode
  }

  getPlayModeLabel(mode) {
    const labels = {
      [PLAY_MODES.SEQUENCE]: '顺序播放',
      [PLAY_MODES.SHUFFLE]: '随机播放',
      [PLAY_MODES.LOOP_ONE]: '单曲循环',
      [PLAY_MODES.LOOP_ALL]: '列表循环'
    }
    return labels[mode] || mode
  }

  getPlayModeIcon(mode) {
    const icons = {
      [PLAY_MODES.SEQUENCE]: '🔄',
      [PLAY_MODES.SHUFFLE]: '🔀',
      [PLAY_MODES.LOOP_ONE]: '🔂',
      [PLAY_MODES.LOOP_ALL]: '🔁'
    }
    return icons[mode] || '🔄'
  }

  getTracksByArtist(artist) {
    return this.library.filter(t => t.artist === artist)
  }

  getTracksByAlbum(album) {
    return this.library.filter(t => t.album === album)
  }

  getArtists() {
    const artists = new Set(this.library.map(t => t.artist))
    return Array.from(artists)
  }

  getAlbums() {
    const albums = new Set(this.library.map(t => t.album))
    return Array.from(albums)
  }

  searchTracks(query) {
    const lowerQuery = query.toLowerCase()
    return this.library.filter(t => 
      t.name.toLowerCase().includes(lowerQuery) ||
      t.artist.toLowerCase().includes(lowerQuery) ||
      t.album.toLowerCase().includes(lowerQuery)
    )
  }

  formatDuration(seconds) {
    if (!seconds || isNaN(seconds)) return '00:00'
    
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  subscribe(listener) {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  notifyListeners(event, data) {
    this.listeners.forEach(listener => {
      try {
        listener(event, data)
      } catch (e) {
        logger.error('LISTENER_ERROR', 'Error in listener', { error: e.message })
      }
    })
  }

  destroy() {
    this.stop()
    this.audio.removeEventListener('ended', this.onTrackEnded)
    this.clearLibrary()
    this.listeners.clear()
    logger.info('MUSIC_PLAYER_DESTROYED', 'Music player destroyed')
  }
}

const musicPlayer = new MusicPlayer()

export { musicPlayer, PLAY_MODES }
export default musicPlayer
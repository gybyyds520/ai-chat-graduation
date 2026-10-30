import { musicPlayer, PLAY_MODES } from '../src/utils/musicPlayer'

describe('MusicPlayer', () => {
  beforeEach(() => {
    musicPlayer.library = []
    musicPlayer.playlist = []
    musicPlayer.currentIndex = -1
    musicPlayer.isPlaying = false
    localStorage.removeItem('desktop-elf-music-library')
  })

  describe('基本功能', () => {
    it('应该能够添加音乐文件', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      const tracks = musicPlayer.addTracks([mockFile])
      
      expect(tracks.length).toBe(1)
      expect(tracks[0].id).toBeDefined()
      expect(tracks[0].name).toBe('test')
      expect(tracks[0].url).toBeDefined()
    })

    it('应该能够获取音乐库', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      const library = musicPlayer.getLibrary()
      expect(library.length).toBe(1)
    })

    it('应该能够获取播放列表', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      const playlist = musicPlayer.getPlaylist()
      expect(playlist.length).toBe(1)
    })

    it('应该能够删除音乐文件', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      const tracks = musicPlayer.addTracks([mockFile])
      
      const result = musicPlayer.removeTrack(tracks[0].id)
      expect(result).toBe(true)
      expect(musicPlayer.getLibrary().length).toBe(0)
    })

    it('删除不存在的音乐应该返回false', () => {
      const result = musicPlayer.removeTrack('nonexistent-id')
      expect(result).toBe(false)
    })

    it('应该能够清空音乐库', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      musicPlayer.clearLibrary()
      expect(musicPlayer.getLibrary().length).toBe(0)
      expect(musicPlayer.getPlaylist().length).toBe(0)
    })
  })

  describe('播放控制', () => {
    it('应该能够播放音乐', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      const result = musicPlayer.play(0)
      expect(result).toBe(true)
      expect(musicPlayer.currentIndex).toBe(0)
    })

    it('播放空列表应该返回false', () => {
      const result = musicPlayer.play(0)
      expect(result).toBe(false)
    })

    it('应该能够暂停音乐', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      musicPlayer.play(0)
      
      musicPlayer.pause()
      expect(musicPlayer.isPlaying).toBe(false)
    })

    it('应该能够停止音乐', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      musicPlayer.play(0)
      
      musicPlayer.stop()
      expect(musicPlayer.isPlaying).toBe(false)
      expect(musicPlayer.audio.currentTime).toBe(0)
    })

    it('应该能够切换播放/暂停', async () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])

      musicPlayer.togglePlay()
      await vi.waitFor(() => expect(musicPlayer.isPlaying).toBe(true))

      musicPlayer.togglePlay()
      expect(musicPlayer.isPlaying).toBe(false)
    })
  })

  describe('播放模式', () => {
    it('应该支持顺序播放模式', () => {
      musicPlayer.setPlayMode(PLAY_MODES.SEQUENCE)
      expect(musicPlayer.playMode).toBe(PLAY_MODES.SEQUENCE)
    })

    it('应该支持随机播放模式', () => {
      musicPlayer.setPlayMode(PLAY_MODES.SHUFFLE)
      expect(musicPlayer.playMode).toBe(PLAY_MODES.SHUFFLE)
    })

    it('应该支持单曲循环模式', () => {
      musicPlayer.setPlayMode(PLAY_MODES.LOOP_ONE)
      expect(musicPlayer.playMode).toBe(PLAY_MODES.LOOP_ONE)
    })

    it('应该支持列表循环模式', () => {
      musicPlayer.setPlayMode(PLAY_MODES.LOOP_ALL)
      expect(musicPlayer.playMode).toBe(PLAY_MODES.LOOP_ALL)
    })

    it('应该能够循环切换播放模式', () => {
      const initialMode = musicPlayer.playMode
      const newMode = musicPlayer.cyclePlayMode()
      
      expect(newMode).not.toBe(initialMode)
    })

    it('应该能够获取播放模式标签', () => {
      const label = musicPlayer.getPlayModeLabel(PLAY_MODES.SEQUENCE)
      expect(label).toBe('顺序播放')
    })

    it('应该能够获取播放模式图标', () => {
      const icon = musicPlayer.getPlayModeIcon(PLAY_MODES.SHUFFLE)
      expect(icon).toBe('🔀')
    })
  })

  describe('上一曲/下一曲', () => {
    it('应该能够播放下一曲', () => {
      const mockFile1 = new File(['mock audio data'], 'test1.mp3', { type: 'audio/mp3' })
      const mockFile2 = new File(['mock audio data'], 'test2.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile1, mockFile2])
      
      musicPlayer.play(0)
      musicPlayer.next()
      
      expect(musicPlayer.currentIndex).toBe(1)
    })

    it('应该能够播放上一曲', () => {
      const mockFile1 = new File(['mock audio data'], 'test1.mp3', { type: 'audio/mp3' })
      const mockFile2 = new File(['mock audio data'], 'test2.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile1, mockFile2])
      
      musicPlayer.play(1)
      musicPlayer.previous()
      
      expect(musicPlayer.currentIndex).toBe(0)
    })
  })

  describe('进度和音量', () => {
    it('应该能够获取进度信息', () => {
      const progress = musicPlayer.getProgressInfo()
      expect(progress.currentTime).toBeDefined()
      expect(progress.duration).toBeDefined()
      expect(progress.progress).toBeDefined()
    })

    it('应该能够设置进度', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      musicPlayer.seek(50)
    })

    it('应该能够设置音量', () => {
      musicPlayer.setVolume(0.5)
      expect(musicPlayer.volume).toBe(0.5)
      expect(musicPlayer.audio.volume).toBe(0.5)
    })

    it('音量应该被限制在0-1范围内', () => {
      musicPlayer.setVolume(-1)
      expect(musicPlayer.volume).toBe(0)
      
      musicPlayer.setVolume(2)
      expect(musicPlayer.volume).toBe(1)
    })

    it('应该能够切换静音', () => {
      musicPlayer.toggleMute()
      expect(musicPlayer.audio.muted).toBe(true)
      
      musicPlayer.toggleMute()
      expect(musicPlayer.audio.muted).toBe(false)
    })
  })

  describe('搜索和分类', () => {
    it('应该能够搜索音乐', () => {
      const mockFile1 = new File(['mock audio data'], 'hello.mp3', { type: 'audio/mp3' })
      const mockFile2 = new File(['mock audio data'], 'world.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile1, mockFile2])
      
      const results = musicPlayer.searchTracks('hello')
      expect(results.length).toBe(1)
      expect(results[0].name).toBe('hello')
    })

    it('应该能够按艺术家获取音乐', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      const tracks = musicPlayer.addTracks([mockFile])
      tracks[0].artist = 'Test Artist'
      
      const results = musicPlayer.getTracksByArtist('Test Artist')
      expect(results.length).toBe(1)
    })

    it('应该能够获取所有艺术家', () => {
      const mockFile1 = new File(['mock audio data'], 'test1.mp3', { type: 'audio/mp3' })
      const mockFile2 = new File(['mock audio data'], 'test2.mp3', { type: 'audio/mp3' })
      const tracks = musicPlayer.addTracks([mockFile1, mockFile2])
      tracks[0].artist = 'Artist A'
      tracks[1].artist = 'Artist B'
      
      const artists = musicPlayer.getArtists()
      expect(artists.length).toBe(2)
    })

    it('应该能够获取所有专辑', () => {
      const mockFile1 = new File(['mock audio data'], 'test1.mp3', { type: 'audio/mp3' })
      const mockFile2 = new File(['mock audio data'], 'test2.mp3', { type: 'audio/mp3' })
      const tracks = musicPlayer.addTracks([mockFile1, mockFile2])
      tracks[0].album = 'Album A'
      tracks[1].album = 'Album B'
      
      const albums = musicPlayer.getAlbums()
      expect(albums.length).toBe(2)
    })
  })

  describe('时间格式化', () => {
    it('应该能够格式化时长', () => {
      const formatted = musicPlayer.formatDuration(120)
      expect(formatted).toBe('02:00')
    })

    it('应该能够格式化0秒', () => {
      const formatted = musicPlayer.formatDuration(0)
      expect(formatted).toBe('00:00')
    })

    it('应该能够格式化分钟数', () => {
      const formatted = musicPlayer.formatDuration(3661)
      expect(formatted).toBe('61:01')
    })
  })

  describe('本地存储', () => {
    it('应该能够保存音乐库到本地存储', () => {
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      const saved = localStorage.getItem('desktop-elf-music-library')
      expect(saved).toBeDefined()
    })

    it('应该能够从本地存储加载音乐库', () => {
      const library = [{
        id: 'test-id',
        name: 'test',
        url: 'mock-url',
        artist: 'unknown',
        album: 'unknown'
      }]
      localStorage.setItem('desktop-elf-music-library', JSON.stringify(library))
      
      musicPlayer.loadLibrary()
      const loaded = musicPlayer.getLibrary()
      
      expect(loaded.length).toBe(1)
      expect(loaded[0].id).toBe('test-id')
    })
  })

  describe('事件订阅', () => {
    it('应该支持事件订阅', () => {
      const listener = vi.fn()
      const unsubscribe = musicPlayer.subscribe(listener)
      
      const mockFile = new File(['mock audio data'], 'test.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile])
      
      expect(listener).toHaveBeenCalled()
      unsubscribe()
    })

    it('取消订阅后不应该再收到事件', () => {
      const listener = vi.fn()
      const unsubscribe = musicPlayer.subscribe(listener)
      
      const mockFile1 = new File(['mock audio data'], 'test1.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile1])
      unsubscribe()
      
      const mockFile2 = new File(['mock audio data'], 'test2.mp3', { type: 'audio/mp3' })
      musicPlayer.addTracks([mockFile2])
      
      expect(listener).toHaveBeenCalledTimes(1)
    })
  })
})
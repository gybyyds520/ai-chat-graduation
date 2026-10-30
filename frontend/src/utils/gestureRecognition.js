// 手势识别服务
class GestureRecognitionService {
  constructor(options = {}) {
    this.enabled = false
    this.videoElement = null
    this.canvasElement = null
    this.ctx = null
    this.hands = null
    this.isDetecting = false
    this.gestureCallbacks = {}
    this.frameRate = options.frameRate || 30
    this.sensitivity = options.sensitivity || 0.8
    
    // 支持的手势
    this.supportedGestures = ['wave', 'point', 'fist', 'open', 'swipe_left', 'swipe_right', 'pinch']
    
    // 上一帧手势状态
    this.lastGesture = null
    this.gestureStartTime = 0
  }

  // 初始化手势识别
  async init(videoElement, canvasElement) {
    try {
      this.videoElement = videoElement
      this.canvasElement = canvasElement
      this.ctx = canvasElement.getContext('2d')

      // 加载MediaPipe手势识别模型
      const { Hands, HAND_CONNECTIONS } = await import('@mediapipe/hands')
      
      this.hands = new Hands({
        locateFile: (file) => {
          return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`
        }
      })

      this.hands.setOptions({
        maxNumHands: 2,
        modelComplexity: 1,
        minDetectionConfidence: 0.7,
        minTrackingConfidence: 0.5
      })

      this.hands.onResults((results) => {
        this.onResults(results)
      })

      this.enabled = true
      console.log('[Gesture] 手势识别引擎已初始化')
      return true
    } catch (error) {
      console.error('[Gesture] 初始化失败:', error)
      this.enabled = false
      return false
    }
  }

  // 处理识别结果
  onResults(results) {
    // 清空画布
    this.ctx.clearRect(0, 0, this.canvasElement.width, this.canvasElement.height)

    if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
      for (const landmarks of results.multiHandLandmarks) {
        // 绘制手势点
        this.drawHandLandmarks(landmarks)
        
        // 识别手势
        const gesture = this.recognizeGesture(landmarks)
        if (gesture) {
          this.handleGesture(gesture)
        }
      }
    }
  }

  // 绘制手势点
  drawHandLandmarks(landmarks) {
    // 绘制连接线
    const connections = [
      [0, 1], [1, 2], [2, 3], [3, 4], // 拇指
      [0, 5], [5, 6], [6, 7], [7, 8], // 食指
      [0, 9], [9, 10], [10, 11], [11, 12], // 中指
      [0, 13], [13, 14], [14, 15], [15, 16], // 无名指
      [0, 17], [17, 18], [18, 19], [19, 20], // 小指
      [5, 9], [9, 13], [13, 17] // 手掌
    ]

    // 绘制连接线
    this.ctx.strokeStyle = '#4f46e5'
    this.ctx.lineWidth = 2
    for (const [i, j] of connections) {
      const start = landmarks[i]
      const end = landmarks[j]
      this.ctx.beginPath()
      this.ctx.moveTo(start.x * this.canvasElement.width, start.y * this.canvasElement.height)
      this.ctx.lineTo(end.x * this.canvasElement.width, end.y * this.canvasElement.height)
      this.ctx.stroke()
    }

    // 绘制关节点
    this.ctx.fillStyle = '#7c3aed'
    for (const landmark of landmarks) {
      this.ctx.beginPath()
      this.ctx.arc(
        landmark.x * this.canvasElement.width,
        landmark.y * this.canvasElement.height,
        5, 0, 2 * Math.PI
      )
      this.ctx.fill()
    }
  }

  // 识别手势
  recognizeGesture(landmarks) {
    // 获取指尖和根部位置
    const thumbTip = landmarks[4]
    const thumbBase = landmarks[2]
    const indexTip = landmarks[8]
    const middleTip = landmarks[12]
    const ringTip = landmarks[16]
    const pinkyTip = landmarks[20]
    const wrist = landmarks[0]
    const palm = landmarks[9]

    // 计算手指伸展状态
    const fingersExtended = this.areFingersExtended(landmarks)

    // 检测不同手势
    if (this.isFist(fingersExtended)) {
      return 'fist'
    } else if (this.isOpenHand(fingersExtended)) {
      return 'open'
    } else if (this.isPointing(indexTip, middleTip, ringTip, pinkyTip)) {
      return 'point'
    } else if (this.isWaving(landmarks)) {
      return 'wave'
    } else if (this.isSwipe(landmarks)) {
      return this.getSwipeDirection(landmarks)
    } else if (this.isPinch(thumbTip, indexTip)) {
      return 'pinch'
    }

    return null
  }

  // 判断手指是否伸展
  areFingersExtended(landmarks) {
    const fingerTips = [8, 12, 16, 20]
    const fingerBases = [6, 10, 14, 18]
    const fingersExtended = []

    for (let i = 0; i < fingerTips.length; i++) {
      const tip = landmarks[fingerTips[i]]
      const base = landmarks[fingerBases[i]]
      const extended = tip.y < base.y // 简单判断：指尖在根部上方
      fingersExtended.push(extended)
    }

    return fingersExtended
  }

  // 检测拳头
  isFist(fingersExtended) {
    return fingersExtended.every(extended => !extended)
  }

  // 检测张开手
  isOpenHand(fingersExtended) {
    return fingersExtended.every(extended => extended)
  }

  // 检测指向
  isPointing(indexTip, middleTip, ringTip, pinkyTip) {
    return indexTip.y < middleTip.y && 
           indexTip.y < ringTip.y && 
           indexTip.y < pinkyTip.y
  }

  // 检测挥手
  isWaving(landmarks) {
    // 简化的挥手检测：食指在一定范围内移动
    const currentX = landmarks[8].x
    const delta = Math.abs(currentX - (this.lastGesture === 'wave' ? this.lastX : currentX))
    this.lastX = currentX
    return delta > 0.1 && delta < 0.3
  }

  // 检测滑动
  isSwipe(landmarks) {
    const thumb = landmarks[4]
    const index = landmarks[8]
    const dx = index.x - thumb.x
    return Math.abs(dx) > 0.3
  }

  // 获取滑动方向
  getSwipeDirection(landmarks) {
    const thumb = landmarks[4]
    const index = landmarks[8]
    const dx = index.x - thumb.x
    return dx > 0 ? 'swipe_right' : 'swipe_left'
  }

  // 检测捏合
  isPinch(thumbTip, indexTip) {
    const distance = Math.sqrt(
      Math.pow(thumbTip.x - indexTip.x, 2) +
      Math.pow(thumbTip.y - indexTip.y, 2)
    )
    return distance < 0.1
  }

  // 处理识别到的手势
  handleGesture(gesture) {
    const now = Date.now()

    // 防抖：相同手势需要间隔一定时间才触发
    if (gesture === this.lastGesture && now - this.gestureStartTime < 500) {
      return
    }

    if (gesture !== this.lastGesture) {
      this.lastGesture = gesture
      this.gestureStartTime = now
    }

    // 触发回调
    if (this.gestureCallbacks[gesture]) {
      this.gestureCallbacks[gesture].forEach(callback => callback())
    }
  }

  // 注册手势回调
  onGesture(gesture, callback) {
    if (this.supportedGestures.includes(gesture)) {
      if (!this.gestureCallbacks[gesture]) {
        this.gestureCallbacks[gesture] = []
      }
      this.gestureCallbacks[gesture].push(callback)
    }
  }

  // 开始检测
  async start() {
    if (!this.enabled || this.isDetecting) return

    this.isDetecting = true
    console.log('[Gesture] 开始手势检测')

    const detect = async () => {
      if (!this.isDetecting) return

      if (this.videoElement && this.videoElement.readyState >= 2) {
        await this.hands.send({ image: this.videoElement })
      }

      setTimeout(detect, 1000 / this.frameRate)
    }

    detect()
  }

  // 停止检测
  stop() {
    this.isDetecting = false
    console.log('[Gesture] 停止手势检测')
  }

  // 销毁
  destroy() {
    this.stop()
    if (this.hands) {
      this.hands.close()
    }
    this.enabled = false
  }

  // 获取支持的手势列表
  getSupportedGestures() {
    return [...this.supportedGestures]
  }

  // 检查是否支持手势识别
  isSupported() {
    return 'mediaDevices' in navigator && 'getUserMedia' in navigator.mediaDevices
  }
}

// 创建全局单例
const gestureRecognitionService = new GestureRecognitionService()

export default gestureRecognitionService

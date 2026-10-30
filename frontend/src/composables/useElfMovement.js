/**
 * useElfMovement.js — 桌面精灵自由移动系统
 *
 * 职责：
 *  1. 提供移动相关纯函数（随机目标 / 朝目标移动 / 碰边检测 / 走路相位），
 *     全部支持注入随机源，便于确定性单元测试。
 *  2. 提供 Vue composable useElfMovement()：自主巡逻 / 跟随鼠标 / 拖拽暂停恢复。
 *
 * 设计原则：
 *  - 纯函数部分不依赖 DOM/three.js，可在 jsdom/node 下直接测试
 *  - composable 部分只通过 onUpdate 回调把新位置交还给组件，不直接操作 DOM
 *  - RAF 循环不可见时暂停（由组件层 visibilitychange 或 RAF 天然节流）
 */

import { ref, onMounted, onUnmounted } from 'vue'

// ─── 纯函数部分（可独立测试） ───────────────────────────────────

/**
 * 在可视范围内计算一个随机目标点（留 margin 边距）。
 * @param {number} currentX 当前 x（保留参数语义，实际不参与计算，便于扩展为避位）
 * @param {number} currentY 当前 y
 * @param {number} maxX 可视区最大 x
 * @param {number} maxY 可视区最大 y
 * @param {() => number} [rand=Math.random] 随机源
 * @param {number} [margin=40] 边距（px）
 * @returns {{ x:number, y:number }}
 */
export function calculateNextTarget(currentX, currentY, maxX, maxY, rand = Math.random, margin = 40) {
  const loX = margin
  const hiX = Math.max(margin, maxX - margin)
  const loY = margin
  const hiY = Math.max(margin, maxY - margin)
  const x = loX + rand() * (hiX - loX)
  const y = loY + rand() * (hiY - loY)
  return { x, y }
}

/**
 * 在自定义边界矩形内随机选目标点（支持 minX/minY，供大尺寸精灵完整留在可视区）。
 * @param {{minX?:number, minY?:number, maxX:number, maxY:number}} bounds
 * @param {() => number} [rand=Math.random]
 * @param {number} [margin=40] 距边界最小边距（px）
 * @returns {{ x:number, y:number }}
 */
export function calculateNextTargetIn(bounds, rand = Math.random, margin = 40) {
  const loX = (bounds.minX ?? 0) + margin
  const hiX = Math.max(loX, (bounds.maxX ?? 0) - margin)
  const loY = (bounds.minY ?? 0) + margin
  const hiY = Math.max(loY, (bounds.maxY ?? 0) - margin)
  const x = loX + rand() * (hiX - loX)
  const y = loY + rand() * (hiY - loY)
  return { x, y }
}

/**
 * 朝目标点移动一步。speed 单位像素/秒，dt 单位秒。
 * 到达目标（步长覆盖剩余距离或距离 < 阈值）时 arrived=true 并精确停在目标点。
 * @returns {{ x:number, y:number, arrived:boolean, dx:number, dy:number, moved:number }}
 */
export function calculateMovement(currentX, currentY, targetX, targetY, speed, dt) {
  const dx = targetX - currentX
  const dy = targetY - currentY
  const dist = Math.hypot(dx, dy)
  const step = Math.max(0, speed) * Math.max(0, dt || 0)

  // 已在目标点（或一步之内到达）→ 精确停止
  if (dist <= step || dist < 1) {
    return { x: targetX, y: targetY, arrived: true, dx: 0, dy: 0, moved: dist }
  }

  const ratio = step / dist
  return {
    x: currentX + dx * ratio,
    y: currentY + dy * ratio,
    arrived: false,
    dx: dx * ratio,
    dy: dy * ratio,
    moved: step
  }
}

/**
 * 碰边检测：精灵中心是否进入可视区边缘 margin 内。
 * @param {number} x
 * @param {number} y
 * @param {number} maxX
 * @param {number} maxY
 * @param {number} [margin=0]
 * @returns {boolean}
 */
export function checkEdgeHit(x, y, maxX, maxY, margin = 0) {
  return (
    x <= margin ||
    y <= margin ||
    x >= maxX - margin ||
    y >= maxY - margin
  )
}

/**
 * 走路动画相位：根据累计行走距离返回摆臂/摆腿/身体起伏相位。
 * 一个完整步幅 strideLength 对应 sin 一个周期。
 * @param {number} accumulatedDistance 累计行走距离（px）
 * @param {number} [dt=0] 保留参数（步长由距离累积驱动，dt 仅语义占位）
 * @param {number} [strideLength=40] 一个步幅长度（px）
 * @returns {{ phase:number, swing:number, bob:number }}
 *   phase: 连续相位角（弧度，0~2π 周期，供 3D 渲染器驱动完整步态）
 *   swing: -1~1 摆臂/摆腿幅度；bob: 0~1 身体上下起伏
 */
export function getWalkAnimationPhase(accumulatedDistance, dt = 0, strideLength = 40) {
  const d = Math.max(0, accumulatedDistance || 0)
  const phase = (d / strideLength) * Math.PI
  return {
    phase,
    swing: Math.sin(phase),
    bob: Math.abs(Math.sin(phase))
  }
}

/**
 * 到达目标后的随机等待时间（毫秒），默认 15000~45000ms。
 * 长休息 → 走动不再频繁，精灵大部分时间在"休息+待机小动作"，节奏像生命体。
 * @param {number} [minMs=15000]
 * @param {number} [maxMs=45000]
 * @param {() => number} [rand=Math.random]
 * @returns {number}
 */
export function getRandomWait(minMs = 15000, maxMs = 45000, rand = Math.random) {
  const lo = Math.min(minMs, maxMs)
  const hi = Math.max(minMs, maxMs)
  if (hi <= lo) return lo
  return Math.floor(lo + rand() * (hi - lo))
}

/**
 * 生成一次"中途驻足"计划：行走一定时长后停步张望。
 * 默认行走 3000~6000ms 后驻足 1200~2600ms（走短歇长，从容节奏）。
 * @param {number} [minWalkMs=3000]
 * @param {number} [maxWalkMs=6000]
 * @param {number} [minPauseMs=1200]
 * @param {number} [maxPauseMs=2600]
 * @param {() => number} [rand=Math.random]
 * @returns {{ walkMs:number, pauseMs:number }}
 */
export function getStrollPause(minWalkMs = 3000, maxWalkMs = 6000, minPauseMs = 1200, maxPauseMs = 2600, rand = Math.random) {
  const loW = Math.min(minWalkMs, maxWalkMs)
  const hiW = Math.max(minWalkMs, maxWalkMs)
  const loP = Math.min(minPauseMs, maxPauseMs)
  const hiP = Math.max(minPauseMs, maxPauseMs)
  return {
    walkMs: Math.floor(loW + rand() * (hiW - loW)),
    pauseMs: Math.floor(loP + rand() * (hiP - loP))
  }
}

/**
 * 休息结束到迈步前的"回神"时长（毫秒），默认 800~2200ms。
 * 长休息结束后先发愣/张望再起身，消除"起来就走"的机械感。
 * @param {number} [minMs=800]
 * @param {number} [maxMs=2200]
 * @param {() => number} [rand=Math.random]
 * @returns {number}
 */
export function getDeliberateMs(minMs = 800, maxMs = 2200, rand = Math.random) {
  const lo = Math.min(minMs, maxMs)
  const hi = Math.max(minMs, maxMs)
  if (hi <= lo) return lo
  return Math.floor(lo + rand() * (hi - lo))
}

/**
 * 走路速度波动系数（0.85~1.15，sin 平滑）：避免匀速移动的机械感。
 * 同一 phaseOffset 下曲线平滑连续，不逐帧跳变。
 * @param {number} ts 时间戳（ms）
 * @param {number} [phaseOffset=0] 相位偏移，避免所有精灵同步波动
 * @returns {number} 速度系数 0.85~1.15
 */
export function getSpeedWave(ts = 0, phaseOffset = 0) {
  const t = Math.max(0, ts || 0) / 2200 + (phaseOffset || 0)
  return 0.85 + 0.3 * (0.5 + 0.5 * Math.sin(t))
}

// ─── Composable 部分 ─────────────────────────────────────────────

/**
 * 自由移动系统 composable。
 *
 * @param {import('vue').Ref<Object>} configRef - elfBridge 配置 ref（movementEnabled/followMouse/moveSpeed）
 * @param {object} [options]
 * @param {(x:number, y:number, dx?:number, dy?:number) => void} options.onUpdate - 新位置回调（组件内写 elfPosition；dx/dy 为单步位移向量，用于朝向）
 * @param {() => {maxX:number, maxY:number}} [options.getBounds] - 返回可视区边界
 * @param {(moving:boolean) => void} [options.onMovingChange] - 移动状态变化回调（驱动走路动画）
 * @param {(followX:number, followY:number) => void} [options.onFollow] - 跟随鼠标目标设置
 * @param {(x:number, y:number) => void} [options.onEdgeHit] - 碰边回调（到达可视区边缘时触发，带防抖；用于折返跳跃等表现）
 * @param {() => void} [options.onArrive] - 自主巡逻到达目标后的回调（组件层播待机小动作/台词，赋予"灵魂感"）
 * @param {() => void} [options.onStrollPause] - 中途驻足开始时的回调（组件层播"张望/思考"等瞬时动作）
 * @param {() => void} [options.onDeliberate] - 休息结束、迈步前的"回神"回调（组件层播"发愣/张望"表情）
 */
export function useElfMovement(configRef, options = {}) {
  const {
    onUpdate = () => {},
    getBounds = () => ({ maxX: window.innerWidth || 800, maxY: window.innerHeight || 600 }),
    onMovingChange = () => {},
    onFollow = () => {},
    onEdgeHit = () => {},
    onArrive = () => {},
    onStrollPause = () => {},
    onDeliberate = () => {}
  } = options

  const isMoving = ref(false)      // 是否正在行走
  const isPaused = ref(false)      // 拖拽 / 面板打开时暂停
  const walkPhase = ref(0)         // 当前走路连续相位（弧度，0~2π 周期；供 3D 渲染器驱动完整步态）

  // 内部状态
  let rafId = null
  let lastTs = 0
  let current = { x: 0, y: 0 }
  let target = { x: 0, y: 0 }
  let accumulatedDistance = 0
  let waitUntil = 0
  let destroyed = false
  let followTarget = null
  let mouseMoveHandler = null
  let edgeHitAt = 0               // 碰边防抖时间戳
  // 中途驻足（去机械化：走走停停）
  let stroll = { walkMs: 0, pauseMs: 0 }
  let strollWalkedAt = 0          // 本轮行走开始时间
  let strollPauseUntil = 0        // 驻足结束时间（0=行走中）
  // 意向停顿（去机械化：休息结束后先"回神"再迈步）
  let deliberateUntil = 0         // 回神结束时间戳（0=不在回神期）
  let deliberateNotified = false  // 回神开始是否已通知组件（一次性）
  let speedPhaseOffset = Math.random() * Math.PI * 2  // 速度波动相位偏移（各精灵不同步）

  function cfg() {
    return (configRef && configRef.value) || {}
  }

  /** 进入跟随鼠标模式：记录鼠标位置为目标 */
  function setFollowTarget(x, y) {
    followTarget = { x, y }
    onFollow(x, y)
  }

  /** 拖拽暂停：暂停自主巡逻与跟随 */
  function pause() {
    isPaused.value = true
    if (isMoving.value) {
      isMoving.value = false
      onMovingChange(false)
    }
  }

  /** 拖拽结束 / 条件满足时恢复 */
  function resume() {
    isPaused.value = false
    // 立即选新目标，避免停在原地发呆（拖拽恢复属于用户预期动作，跳过回神期）
    deliberateUntil = 0
    deliberateNotified = false
    pickNewTarget()
  }

  function pickNewTarget() {
    const b = safeBounds()
    const t = calculateNextTargetIn(b)
    target = t
  }

  function safeBounds() {
    try {
      const b = getBounds()
      return {
        minX: b.minX ?? 0,
        minY: b.minY ?? 0,
        maxX: b.maxX || 800,
        maxY: b.maxY || 600
      }
    } catch (e) {
      return { minX: 0, minY: 0, maxX: 800, maxY: 600 }
    }
  }

  /** 设置精灵初始位置（组件挂载后调用） */
  function setPosition(x, y) {
    current = { x, y }
    pickNewTarget()
  }

  function loop(ts) {
    if (destroyed) return
    rafId = requestAnimationFrame(loop)

    const conf = cfg()
    const enabled = conf.movementEnabled !== false
    const follow = conf.followMouse === true
    const baseSpeed = typeof conf.moveSpeed === 'number' ? conf.moveSpeed : 50
    // 速度微波动：sin 平滑 0.85~1.15，避免匀速机械感
    const speed = baseSpeed * getSpeedWave(ts, speedPhaseOffset)

    if (!enabled || isPaused.value) {
      lastTs = ts
      return
    }

    const dt = lastTs ? Math.min((ts - lastTs) / 1000, 0.1) : 0
    lastTs = ts
    // 当前时间戳：循环主体（含碰边判断）共用，避免块级作用域导致 ReferenceError
    const now = ts

    // 目标：跟随鼠标优先，否则自主巡逻
    let goal = null
    if (follow && followTarget) {
      goal = followTarget
    } else {
      // 到达后的长休息：等待中不移动（拉长间隔 → 走动不频繁）
      if (waitUntil && now < waitUntil) {
        if (isMoving.value) {
          isMoving.value = false
          onMovingChange(false)
        }
        return
      }
      // 休息结束后的"回神"：发愣一会儿再迈步（消除"起来就走"的机械感）
      if (deliberateUntil && now < deliberateUntil) {
        if (isMoving.value) {
          isMoving.value = false
          onMovingChange(false)
        }
        if (!deliberateNotified) {
          deliberateNotified = true
          onDeliberate()
        }
        return
      }
      // 中途驻足张望：行走中到点停步（去机械感，走走停停）
      if (strollPauseUntil && now < strollPauseUntil) {
        if (isMoving.value) {
          isMoving.value = false
          onMovingChange(false)
        }
        return
      }
      goal = target
    }

    const result = calculateMovement(current.x, current.y, goal.x, goal.y, speed, dt)
    current = { x: result.x, y: result.y }
    accumulatedDistance += result.moved || 0

    // 走路连续相位（供 3D 渲染器驱动完整步态：摆臂/摆腿/起伏）
    const phase = getWalkAnimationPhase(accumulatedDistance)
    walkPhase.value = phase.phase

    // 状态翻转
    const nowMoving = !result.arrived
    if (nowMoving !== isMoving.value) {
      isMoving.value = nowMoving
      onMovingChange(nowMoving)
    }

    // 自主巡逻：行走中累计到本轮驻足计划时长 → 触发一次中途驻足（去机械化，走走停停）
    if (nowMoving && !follow) {
      if (!stroll.walkMs || stroll.walkMs <= 0) {
        stroll = getStrollPause()
        strollWalkedAt = ts
      } else if (ts - strollWalkedAt >= stroll.walkMs) {
        const pauseMs = stroll.pauseMs
        stroll = { walkMs: 0, pauseMs: 0 }
        strollPauseUntil = ts + pauseMs
        onStrollPause()
      }
    }

    // 碰边折返触发（自主巡逻移动中、距边缘 12px 内、防抖 900ms）
    if (nowMoving && !follow) {
      const b = safeBounds()
      const hit = checkEdgeHit(current.x, current.y, b.maxX, b.maxY, 12) ||
        current.x <= b.minX + 12 || current.y <= b.minY + 12
      if (hit && now - edgeHitAt > 900) {
        edgeHitAt = now
        onEdgeHit(current.x, current.y)
      }
    }

    // 通知组件更新位置（带位移向量，用于移动朝向）
    onUpdate(current.x, current.y, result.dx, result.dy)

    // 自主巡逻到达 → 触发灵魂动作回调 + 长休息后选新目标
    if (!follow && result.arrived) {
      onArrive()
      waitUntil = ts + getRandomWait()
      stroll = { walkMs: 0, pauseMs: 0 }
      pickNewTarget()
      // 休息结束后的"回神"：发愣片刻再迈步；回神开始即通知组件播"张望/思考"表情
      deliberateUntil = waitUntil + getDeliberateMs()
      deliberateNotified = false
    }
  }

  function start() {
    if (destroyed) return
    if (rafId != null) return
    lastTs = 0
    waitUntil = 0
    deliberateUntil = 0
    deliberateNotified = false
    pickNewTarget()
    rafId = requestAnimationFrame(loop)

    // 跟随鼠标：始终挂监听（事件回调内按当前配置开关决定是否跟随），
    // 这样用户在面板里开启"跟随鼠标"后无需重启即可生效。
    if (typeof window !== 'undefined') {
      mouseMoveHandler = (e) => {
        if (cfg().followMouse === true) {
          setFollowTarget(e.clientX, e.clientY)
        }
      }
      window.addEventListener('mousemove', mouseMoveHandler)
    }
  }

  function stop() {
    destroyed = true
    if (rafId != null) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    if (mouseMoveHandler && typeof window !== 'undefined') {
      window.removeEventListener('mousemove', mouseMoveHandler)
      mouseMoveHandler = null
    }
  }

  onMounted(start)
  onUnmounted(stop)

  return {
    isMoving,
    isPaused,
    walkPhase,
    start,
    stop,
    pause,
    resume,
    setPosition,
    setFollowTarget
  }
}

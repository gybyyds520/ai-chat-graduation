/**
 * useElf3DRenderer.js — Three.js 精灵 3D 渲染器 composable
 *
 * 负责创建/管理 three.js 场景（Scene/Camera/Renderer/Lights/Ground/动画循环）。
 * 所有 three import 均为动态 import，确保 jsdom 测试环境下加载本模块不崩溃。
 */

import { ref, onMounted, onUnmounted } from 'vue'
import {
  getElf3DGeometry,
  getElfMaterial,
  getOutfitAccessories,
  getAction3DTransform,
  getDefault3DConfig,
  getAnimeElf3DPalette,
  getAnimeElfHairPartDescriptors,
  getAnimeElfLimbAction
} from '@/utils/elf3dConfig'
import { removeImageBackground } from '@/utils/elfImageUtils'
import { elfOutfitSets, DEFAULT_OUTFIT_SET } from '@/data/elfOutfitSets'

export function useElf3DRenderer(containerRef, configRef) {
  // ─── 响应式状态 ───
  const isReady = ref(false)
  const isMode2D = ref(false)

  // ─── 内部变量（非响应式） ───
  let three = null
  let scene = null
  let camera = null
  let renderer = null
  let elfGroup = null
  let groundMesh = null
  let animationFrameId = null
  let currentAction = 'idle'
  let actionProgress = 0
  let actionStartTime = 0
  let actionDuration = 1000
  let isActionPlaying = false
  // 姿态平滑层（动画流畅度核心）：跨帧指数逼近目标姿态，消除动作切换/结束时的瞬间跳变，
  // 让所有动作像动漫角色一样流畅过渡
  const poseSmooth = {}
  function smoothPose(name, target, dt, speed = 14) {
    if (poseSmooth[name] === undefined) poseSmooth[name] = target
    const k = 1 - Math.exp(-dt * speed)
    const cur = poseSmooth[name]
    const next = cur + (target - cur) * k
    poseSmooth[name] = next
    return next
  }
  let elapsedTime = 0
  let visibilityHandler = null
  let disposed = false
  // 二次元少女四肢骨骼引用（用于 wave/dance/walk 等局部动画）
  let limbRefs = null
  // 走路状态：{ active:boolean, dirX:number(-1|0|1), phase:number(连续弧度) }
  let walkState = null
  // 跳跃叠加状态：{ start:number, duration:number }（跳完自动清除）
  let jumpState = null
  let lastAnimTs = 0
  // 高级互动状态（市面桌宠体验）：眼神跟随 / 拖拽拎起 / 松手摔落 / 眨眼 / 呼吸
  let gazeTarget = null        // { x:-1~1, y:-1~1 }，null=正对屏幕
  let draggingState = false    // 拖拽拎起：悬空挣扎
  let dropState = null         // { start:number, duration:number }：松手摔落弹跳
  let blinkState = null        // { next:number, until:number }：下一次眨眼/本次眨眼结束
  let breathPhase = 0
  // 立绘贴图模式（逼真人物）：用 AI 生成精绘立绘 PNG 作为 3D 场景中的透明贴图纸片，
  // 保留 walk/jump/drag/drop/gaze/breath 全部动画状态机；无立绘时回退程序化几何建模
  let spriteMode = false
  let mirrorX = 1              // 立绘左右走镜像（纸片不侧转，用 scaleX 翻转表达转身）
  let spriteReqToken = 0       // 异步贴图加载的过期令牌（角色切换后丢弃旧加载结果）
  // 相机视差（立体真实感）：鼠标位置驱动相机微移 → 立绘纸片相对页面产生透视位移
  let camParallax = { x: 0, y: 0 }
  let camSmooth = { x: 0, y: 0 }
  let camLookAtY = 0.15
  // V3 游戏级渲染：三色调卡通渐变贴图（init 时创建一次，材质共用）
  let toonGradientMap = null

  // ─── 内部工具 ───

  function getConfig() {
    return configRef?.value || getDefault3DConfig()
  }

  /**
   * 根据配置参数创建几何体
   */
  function createGeometry(geomDesc) {
    if (!three) return null

    switch (geomDesc.type) {
      case 'sphere':
        return new three.SphereGeometry(geomDesc.params.radius, 32, 32)
      case 'box':
        return new three.BoxGeometry(
          geomDesc.params.width,
          geomDesc.params.height,
          geomDesc.params.depth
        )
      case 'octahedron':
        return new three.OctahedronGeometry(geomDesc.params.radius, 0)
      case 'cone':
        return new three.ConeGeometry(geomDesc.params.radius, geomDesc.params.height, 16)
      case 'cylinder':
        return new three.CylinderGeometry(
          geomDesc.params.radiusTop || geomDesc.params.radius,
          geomDesc.params.radiusBottom || geomDesc.params.radius,
          geomDesc.params.height,
          16
        )
      case 'torus':
        return new three.TorusGeometry(
          geomDesc.params.radius,
          geomDesc.params.tube,
          12,
          24
        )
      case 'extrude': {
        // 用 Shape + ExtrudeGeometry 实现 star/heart
        const shapeName = geomDesc.params.shape
        const shapePts = []
        if (shapeName === 'star') {
          // 五角星路径点
          const outerR = 1
          const innerR = 0.4
          for (let i = 0; i < 10; i++) {
            const r = i % 2 === 0 ? outerR : innerR
            const angle = (i * Math.PI) / 5 - Math.PI / 2
            shapePts.push(new three.Vector2(Math.cos(angle) * r, Math.sin(angle) * r))
          }
        } else if (shapeName === 'heart') {
          // 简化心形路径
          shapePts.push(new three.Vector2(0, 0.5))
          shapePts.push(new three.Vector2(-0.5, 1))
          shapePts.push(new three.Vector2(-1, 0.3))
          shapePts.push(new three.Vector2(0, -0.8))
          shapePts.push(new three.Vector2(1, 0.3))
          shapePts.push(new three.Vector2(0.5, 1))
        }
        const shape = new three.Shape(shapePts)
        return new three.ExtrudeGeometry(shape, {
          depth: geomDesc.params.depth || 0.3,
          bevelEnabled: false
        })
      }
      case 'humanoid': {
        // 人形：用两个 sphere 组合（身体 + 头），这里返回一个 group 由调用者处理
        return null // 由 buildElfMesh 特殊处理
      }
      default:
        return new three.SphereGeometry(1, 32, 32)
    }
  }

  /**
   * 根据材质描述创建 MeshStandardMaterial
   */
  function createMaterial(matDesc) {
    if (!three) return null
    return new three.MeshStandardMaterial({
      color: new three.Color(matDesc.color),
      emissive: new three.Color(matDesc.emissive),
      emissiveIntensity: matDesc.emissiveIntensity,
      roughness: matDesc.roughness,
      metalness: matDesc.metalness
    })
  }

  // ─── 关键方法 ───

  /**
   * 判断是否为二次元少女角色（category=anime 或 shape=anime-girl）
   */
  function isAnimeGirl(character) {
    return Boolean(
      character &&
      (character.category === 'anime' || character.shape === 'anime-girl')
    )
  }

  /**
   * 用 MeshToonMaterial 构建卡通材质（卡通风格）
   */
  function toonMaterial(color, opts = {}) {
    return new three.MeshToonMaterial({
      color: new three.Color(color),
      transparent: opts.transparent || false,
      opacity: opts.opacity !== undefined ? opts.opacity : 1
    })
  }

  /**
   * 程序化生成二次元脸部贴图（CanvasTexture）：
   * 眉毛/嘴/腮红/鼻点绘制在透明底上，贴到头部前方；
   * 眼睛用独立凸起球体（可眨眼缩放），不画进贴图。
   * @param {object} palette
   * @returns {THREE.CanvasTexture|null}
   */
  function createAnimeFaceTexture(palette) {
    if (!three || typeof document === 'undefined') return null
    const pal = getAnimeElf3DPalette(palette)
    try {
      const canvas = document.createElement('canvas')
      canvas.width = 256
      canvas.height = 256
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      ctx.clearRect(0, 0, 256, 256)
      // 眉毛（柔和上扬）
      ctx.strokeStyle = pal.brow || '#8a6a5a'
      ctx.lineWidth = 7
      ctx.lineCap = 'round'
      ctx.beginPath()
      ctx.moveTo(52, 76)
      ctx.quadraticCurveTo(86, 60, 120, 74)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(136, 74)
      ctx.quadraticCurveTo(170, 60, 204, 76)
      ctx.stroke()
      // 微笑嘴
      ctx.strokeStyle = '#c9706a'
      ctx.lineWidth = 6
      ctx.beginPath()
      ctx.moveTo(104, 170)
      ctx.quadraticCurveTo(128, 186, 152, 170)
      ctx.stroke()
      // 腮红
      ctx.fillStyle = pal.blush || '#ff9dbb'
      ctx.globalAlpha = 0.5
      ctx.beginPath()
      ctx.ellipse(56, 142, 28, 15, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.beginPath()
      ctx.ellipse(200, 142, 28, 15, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.globalAlpha = 1
      // 鼻点
      ctx.fillStyle = '#e8b49a'
      ctx.beginPath()
      ctx.arc(128, 138, 5, 0, Math.PI * 2)
      ctx.fill()
      const tex = new three.CanvasTexture(canvas)
      tex.colorSpace = three.SRGBColorSpace
      return tex
    } catch (e) {
      return null
    }
  }

  /**
   * 程序化构建 3D 卡通少女角色（Q 版 3-4 头身）。
   * 全部由 three.js 基础几何体拼装，颜色按 palette 参数化。
   *
   * @param {object} character - 二次元角色对象（含 hairstyle）
   * @param {object} palette - 调色板
   * @returns {THREE.Group|null}
   */
  function buildAnimeElf3D(character, palette) {
    if (!three || !scene) return null

    const pal = getAnimeElf3DPalette(palette || (character && character.palette))
    const hairParts = getAnimeElfHairPartDescriptors(character && character.hairstyle)

    const group = new three.Group()
    const refs = {}

    // 材质
    const skinMat = toonMaterial(pal.skin)
    const hairMat = toonMaterial(pal.hair)
    const eyesMat = toonMaterial(pal.eyes)
    const eyeHighlightMat = toonMaterial(pal.eyeHighlight)
    const whiteMat = toonMaterial('#ffffff')
    const outfitMat = toonMaterial(pal.outfit)
    const outfitAccentMat = toonMaterial(pal.outfitAccent)
    const ribbonMat = toonMaterial(pal.ribbon)
    const blushMat = toonMaterial(pal.blush, { transparent: true, opacity: 0.6 })
    const browMat = toonMaterial(pal.brow)

    // 尺度（Q 版：头大身小）
    const headR = 0.42
    const headY = 0.95

    // ─── 头部 ───
    const headGroup = new three.Group()
    headGroup.position.y = headY
    group.add(headGroup)

    // 脸
    const face = new three.Mesh(new three.SphereGeometry(headR, 24, 24), skinMat)
    face.scale.set(1, 1.05, 0.92)
    face.castShadow = true
    headGroup.add(face)

    // 眼睛：两个球（深色虹膜 + 白色高光点）
    const eyeGeo = new three.SphereGeometry(0.07, 12, 12)
    const leftEye = new three.Mesh(eyeGeo, eyesMat)
    leftEye.position.set(-0.14, 0.02, headR * 0.85)
    headGroup.add(leftEye)
    const rightEye = new three.Mesh(eyeGeo, eyesMat)
    rightEye.position.set(0.14, 0.02, headR * 0.85)
    headGroup.add(rightEye)
    refs.eyes = [leftEye, rightEye]
    // 高光点
    const hlGeo = new three.SphereGeometry(0.022, 8, 8)
    const hlL = new three.Mesh(hlGeo, eyeHighlightMat)
    hlL.position.set(-0.12, 0.05, headR * 0.95)
    headGroup.add(hlL)
    const hlR = new three.Mesh(hlGeo, eyeHighlightMat)
    hlR.position.set(0.16, 0.05, headR * 0.95)
    headGroup.add(hlR)

    // 眉毛：薄 Box
    const browGeo = new three.BoxGeometry(0.11, 0.02, 0.02)
    const browL = new three.Mesh(browGeo, browMat)
    browL.position.set(-0.14, 0.13, headR * 0.88)
    headGroup.add(browL)
    const browR = new three.Mesh(browGeo, browMat)
    browR.position.set(0.14, 0.13, headR * 0.88)
    headGroup.add(browR)

    // 腮红：半透明粉色球（贴脸两侧）
    const blushGeo = new three.SphereGeometry(0.05, 10, 10)
    const blushL = new three.Mesh(blushGeo, blushMat)
    blushL.position.set(-0.22, -0.06, headR * 0.82)
    blushL.scale.set(1, 0.6, 0.4)
    headGroup.add(blushL)
    const blushR = new three.Mesh(blushGeo, blushMat)
    blushR.position.set(0.22, -0.06, headR * 0.82)
    blushR.scale.set(1, 0.6, 0.4)
    headGroup.add(blushR)

    // 嘴巴：小扁盒
    const mouth = new three.Mesh(new three.BoxGeometry(0.08, 0.018, 0.02), ribbonMat)
    mouth.position.set(0, -0.16, headR * 0.9)
    headGroup.add(mouth)

    // ─── 头发（分层） ───
    const hairGroup = new three.Group()
    headGroup.add(hairGroup)

    // 后发：略大于头的球，包住头后部
    if (hairParts.includes('back')) {
      const backHair = new three.Mesh(
        new three.SphereGeometry(headR * 1.08, 24, 24),
        hairMat
      )
      backHair.position.set(0, 0.04, -0.06)
      backHair.scale.set(1.02, 1.06, 1.0)
      backHair.castShadow = true
      hairGroup.add(backHair)
    }
    // 刘海：前额半球
    if (hairParts.includes('bangs')) {
      const bangs = new three.Mesh(
        new three.SphereGeometry(headR * 0.92, 20, 20, 0, Math.PI * 2, 0, Math.PI * 0.42),
        hairMat
      )
      bangs.position.set(0, 0.16, 0.04)
      hairGroup.add(bangs)
    }
    // 侧发（鬓发）
    if (hairParts.includes('side-l')) {
      const sl = new three.Mesh(new three.CapsuleGeometry(0.05, 0.32, 6, 12), hairMat)
      sl.position.set(-headR * 0.95, -0.1, 0.05)
      hairGroup.add(sl)
    }
    if (hairParts.includes('side-r')) {
      const sr = new three.Mesh(new three.CapsuleGeometry(0.05, 0.32, 6, 12), hairMat)
      sr.position.set(headR * 0.95, -0.1, 0.05)
      hairGroup.add(sr)
    }
    // 双马尾
    if (hairParts.includes('twin-l')) {
      const tl = new three.Mesh(new three.CapsuleGeometry(0.07, 0.4, 6, 12), hairMat)
      tl.position.set(-headR * 1.25, -0.15, -0.05)
      tl.rotation.z = 0.25
      hairGroup.add(tl)
    }
    if (hairParts.includes('twin-r')) {
      const tr = new three.Mesh(new three.CapsuleGeometry(0.07, 0.4, 6, 12), hairMat)
      tr.position.set(headR * 1.25, -0.15, -0.05)
      tr.rotation.z = -0.25
      hairGroup.add(tr)
    }
    // 高马尾
    if (hairParts.includes('ponytail')) {
      const pt = new three.Mesh(new three.CapsuleGeometry(0.07, 0.45, 6, 12), hairMat)
      pt.position.set(0, 0.55, -headR * 0.8)
      pt.rotation.x = 0.5
      hairGroup.add(pt)
    }
    // 卷发团
    if (hairParts.includes('curly-l')) {
      const cl = new three.Mesh(new three.SphereGeometry(0.13, 12, 12), hairMat)
      cl.position.set(-headR * 1.0, -0.05, 0)
      hairGroup.add(cl)
    }
    if (hairParts.includes('curly-r')) {
      const cr = new three.Mesh(new three.SphereGeometry(0.13, 12, 12), hairMat)
      cr.position.set(headR * 1.0, -0.05, 0)
      hairGroup.add(cr)
    }

    // ─── 身体 ───
    const bodyGroup = new three.Group()
    group.add(bodyGroup)

    // 躯干（上窄下宽圆柱）
    const torso = new three.Mesh(
      new three.CylinderGeometry(0.22, 0.28, 0.5, 16),
      outfitMat
    )
    torso.position.y = 0.45
    torso.castShadow = true
    bodyGroup.add(torso)

    // 水手领：颈部 Torus
    const sailorCollar = new three.Mesh(
      new three.TorusGeometry(0.19, 0.045, 8, 20),
      whiteMat
    )
    sailorCollar.position.y = 0.72
    sailorCollar.rotation.x = Math.PI / 2
    bodyGroup.add(sailorCollar)

    // 蝴蝶结：胸前 Box
    const bow = new three.Mesh(new three.BoxGeometry(0.16, 0.1, 0.04), ribbonMat)
    bow.position.set(0, 0.66, 0.24)
    bodyGroup.add(bow)

    // 百褶裙：下半身圆柱（上窄下宽）
    const skirt = new three.Mesh(
      new three.CylinderGeometry(0.24, 0.38, 0.28, 16),
      outfitAccentMat
    )
    skirt.position.y = 0.16
    skirt.castShadow = true
    bodyGroup.add(skirt)

    // ─── 四肢（带骨骼 pivot，便于摆臂/摆腿） ───
    // 手臂 Group：pivot 在肩部
    const leftArm = new three.Group()
    leftArm.position.set(-0.28, 0.66, 0)
    const leftArmMesh = new three.Mesh(
      new three.CapsuleGeometry(0.05, 0.32, 6, 12),
      skinMat
    )
    leftArmMesh.position.y = -0.18
    leftArmMesh.castShadow = true
    leftArm.add(leftArmMesh)
    bodyGroup.add(leftArm)

    const rightArm = new three.Group()
    rightArm.position.set(0.28, 0.66, 0)
    const rightArmMesh = new three.Mesh(
      new three.CapsuleGeometry(0.05, 0.32, 6, 12),
      skinMat
    )
    rightArmMesh.position.y = -0.18
    rightArmMesh.castShadow = true
    rightArm.add(rightArmMesh)
    bodyGroup.add(rightArm)

    // 腿 Group：pivot 在髋部
    const leftLeg = new three.Group()
    leftLeg.position.set(-0.12, 0.02, 0)
    const leftLegMesh = new three.Mesh(
      new three.CapsuleGeometry(0.06, 0.34, 6, 12),
      skinMat
    )
    leftLegMesh.position.y = -0.2
    leftLegMesh.castShadow = true
    leftLeg.add(leftLegMesh)
    bodyGroup.add(leftLeg)

    const rightLeg = new three.Group()
    rightLeg.position.set(0.12, 0.02, 0)
    const rightLegMesh = new three.Mesh(
      new three.CapsuleGeometry(0.06, 0.34, 6, 12),
      skinMat
    )
    rightLegMesh.position.y = -0.2
    rightLegMesh.castShadow = true
    rightLeg.add(rightLegMesh)
    bodyGroup.add(rightLeg)

    refs.leftArm = leftArm
    refs.rightArm = rightArm
    refs.leftLeg = leftLeg
    refs.rightLeg = rightLeg
    refs.body = bodyGroup
    refs.head = headGroup

    // 整体：略微缩小并居中（相机 z=5，视野 50°）
    group.scale.set(0.9, 0.9, 0.9)
    group.position.y = -0.9

    limbRefs = refs
    return group
  }

  /**
   * 立体虚拟形象 V2：精细程序化 3D 建模（替代纸片立绘/基础 Q 版）。
   * 特点：
   *  - 二次元脸部 CanvasTexture 贴图（眉/嘴/腮红/鼻）+ 独立凸起大眼睛（可眨眼）
   *  - 两段式骨骼四肢（上臂+前臂、大腿+小腿+鞋），走路自然弯曲
   *  - 分层头发（刘海/后发/侧发/长发/双马尾/高马尾/卷发按 hairstyle）
   *  - 卡通渲染（MeshToonMaterial）+ 白袜/鞋/领结细节
   *  - 保持 refs/limbRefs 接口，全部动画状态机（walk/jump/gaze/blink/breath）复用
   * @param {object} character
   * @param {object} palette
   * @returns {THREE.Group|null}
   */
  function buildAnimeElf3DV2(character, palette) {
    if (!three || !scene) return null
    const pal = getAnimeElf3DPalette(palette || (character && character.palette))
    const hairParts = getAnimeElfHairPartDescriptors(character && character.hairstyle)

    const group = new three.Group()
    const refs = {}

    // 材质（卡通渲染）
    const skinMat = toonMaterial(pal.skin)
    const hairMat = toonMaterial(pal.hair)
    const hairHiMat = toonMaterial(pal.hairHighlight || '#ffffff', { transparent: true, opacity: 0.3 })
    const eyesMat = toonMaterial(pal.eyes)
    const eyeHiMat = toonMaterial(pal.eyeHighlight)
    const whiteMat = toonMaterial('#ffffff')
    const outfitMat = toonMaterial(pal.outfit)
    const accentMat = toonMaterial(pal.outfitAccent)
    const ribbonMat = toonMaterial(pal.ribbon)
    const blushMat = toonMaterial(pal.blush, { transparent: true, opacity: 0.55 })
    const darkMat = toonMaterial('#3a3345')
    const sockMat = toonMaterial('#f4f7fb')

    // 比例：接近立绘全身（头略小、身体修长），脚落地
    const headR = 0.36
    const headY = 1.14
    const shoulderY = 0.84

    // ─── 头部 ───
    const headGroup = new three.Group()
    headGroup.position.y = headY
    group.add(headGroup)

    // 头球（皮肤）
    const face = new three.Mesh(new three.SphereGeometry(headR, 32, 32), skinMat)
    face.scale.set(1, 1.06, 0.94)
    face.castShadow = true
    headGroup.add(face)

    // 二次元脸贴图（眉/嘴/腮红/鼻，眼睛留空由球体表达）
    const faceTex = createAnimeFaceTexture(pal)
    if (faceTex) {
      const facePlane = new three.Mesh(
        new three.PlaneGeometry(headR * 1.6, headR * 1.6),
        new three.MeshBasicMaterial({
          map: faceTex,
          transparent: true,
          alphaTest: 0.02,
          depthWrite: false
        })
      )
      facePlane.position.set(0, -0.02, headR * 0.9)
      headGroup.add(facePlane)
    }

    // 独立大眼睛（凸起球体，blink 时 y 压扁）
    const eyeGeo = new three.SphereGeometry(0.08, 16, 16)
    const leftEye = new three.Mesh(eyeGeo, eyesMat)
    leftEye.position.set(-0.13, 0.04, headR * 0.88)
    leftEye.scale.set(1, 1.25, 0.32)
    headGroup.add(leftEye)
    const rightEye = new three.Mesh(eyeGeo, eyesMat)
    rightEye.position.set(0.13, 0.04, headR * 0.88)
    rightEye.scale.set(1, 1.25, 0.32)
    headGroup.add(rightEye)
    refs.eyes = [leftEye, rightEye]
    // 高光点（眼上方偏外）
    const hlGeo = new three.SphereGeometry(0.026, 8, 8)
    const hlL = new three.Mesh(hlGeo, eyeHiMat)
    hlL.position.set(-0.11, 0.09, headR * 1.02)
    headGroup.add(hlL)
    const hlR = new three.Mesh(hlGeo, eyeHiMat)
    hlR.position.set(0.15, 0.09, headR * 1.02)
    headGroup.add(hlR)

    // ─── 头发（分层发片） ───
    const hairGroup = new three.Group()
    headGroup.add(hairGroup)

    // 后发：包住头后部
    if (hairParts.includes('back')) {
      const backHair = new three.Mesh(
        new three.SphereGeometry(headR * 1.06, 24, 24, 0, Math.PI * 2, 0, Math.PI * 0.55),
        hairMat
      )
      backHair.position.set(0, 0.03, -0.05)
      backHair.scale.set(1.04, 1.05, 1.02)
      backHair.castShadow = true
      hairGroup.add(backHair)
    }
    // 刘海：前额弧面（两片叠加出层次）
    if (hairParts.includes('bangs')) {
      const bangs1 = new three.Mesh(
        new three.SphereGeometry(headR * 0.94, 20, 20, 0, Math.PI * 2, 0, Math.PI * 0.4),
        hairMat
      )
      bangs1.position.set(0, 0.15, 0.05)
      hairGroup.add(bangs1)
      const bangs2 = new three.Mesh(
        new three.SphereGeometry(headR * 0.92, 20, 20, 0, Math.PI * 2, Math.PI * 0.18, Math.PI * 0.16),
        hairHiMat
      )
      bangs2.position.set(0, 0.14, 0.08)
      hairGroup.add(bangs2)
    }
    // 侧发（鬓发）
    if (hairParts.includes('side-l')) {
      const sl = new three.Mesh(new three.CapsuleGeometry(0.045, 0.3, 6, 12), hairMat)
      sl.position.set(-headR * 0.98, -0.12, 0.04)
      hairGroup.add(sl)
    }
    if (hairParts.includes('side-r')) {
      const sr = new three.Mesh(new three.CapsuleGeometry(0.045, 0.3, 6, 12), hairMat)
      sr.position.set(headR * 0.98, -0.12, 0.04)
      hairGroup.add(sr)
    }
    // 长发（后背垂发）
    if (hairParts.includes('long')) {
      const longHair = new three.Mesh(
        new three.CapsuleGeometry(0.13, 0.62, 8, 14),
        hairMat
      )
      longHair.position.set(0, -0.28, -0.18)
      longHair.scale.set(1.1, 1, 0.55)
      hairGroup.add(longHair)
      const longHair2 = new three.Mesh(
        new three.CapsuleGeometry(0.06, 0.5, 6, 12),
        hairMat
      )
      longHair2.position.set(-0.12, -0.12, -0.16)
      longHair2.rotation.z = 0.08
      hairGroup.add(longHair2)
      const longHair3 = new three.Mesh(
        new three.CapsuleGeometry(0.06, 0.5, 6, 12),
        hairMat
      )
      longHair3.position.set(0.12, -0.12, -0.16)
      longHair3.rotation.z = -0.08
      hairGroup.add(longHair3)
    }
    // 双马尾
    if (hairParts.includes('twin-l')) {
      const tl = new three.Mesh(new three.CapsuleGeometry(0.06, 0.42, 6, 12), hairMat)
      tl.position.set(-headR * 1.35, -0.18, -0.05)
      tl.rotation.z = 0.28
      hairGroup.add(tl)
      const tl2 = new three.Mesh(new three.CapsuleGeometry(0.04, 0.3, 6, 12), hairHiMat)
      tl2.position.set(-headR * 1.5, -0.45, -0.06)
      tl2.rotation.z = 0.5
      hairGroup.add(tl2)
    }
    if (hairParts.includes('twin-r')) {
      const tr = new three.Mesh(new three.CapsuleGeometry(0.06, 0.42, 6, 12), hairMat)
      tr.position.set(headR * 1.35, -0.18, -0.05)
      tr.rotation.z = -0.28
      hairGroup.add(tr)
      const tr2 = new three.Mesh(new three.CapsuleGeometry(0.04, 0.3, 6, 12), hairHiMat)
      tr2.position.set(headR * 1.5, -0.45, -0.06)
      tr2.rotation.z = -0.5
      hairGroup.add(tr2)
    }
    // 高马尾
    if (hairParts.includes('ponytail')) {
      const pt = new three.Mesh(new three.CapsuleGeometry(0.075, 0.5, 6, 12), hairMat)
      pt.position.set(0, 0.5, -headR * 0.82)
      pt.rotation.x = 0.55
      hairGroup.add(pt)
      const pt2 = new three.Mesh(new three.CapsuleGeometry(0.045, 0.3, 6, 12), hairHiMat)
      pt2.position.set(0, 0.78, -headR * 1.0)
      pt2.rotation.x = 0.8
      hairGroup.add(pt2)
    }
    // 卷发团
    if (hairParts.includes('curly-l')) {
      const cl = new three.Mesh(new three.SphereGeometry(0.13, 12, 12), hairMat)
      cl.position.set(-headR * 1.05, -0.06, 0.02)
      hairGroup.add(cl)
      const cl2 = new three.Mesh(new three.SphereGeometry(0.08, 10, 10), hairHiMat)
      cl2.position.set(-headR * 1.2, -0.3, 0.05)
      hairGroup.add(cl2)
    }
    if (hairParts.includes('curly-r')) {
      const cr = new three.Mesh(new three.SphereGeometry(0.13, 12, 12), hairMat)
      cr.position.set(headR * 1.05, -0.06, 0.02)
      hairGroup.add(cr)
      const cr2 = new three.Mesh(new three.SphereGeometry(0.08, 10, 10), hairHiMat)
      cr2.position.set(headR * 1.2, -0.3, 0.05)
      hairGroup.add(cr2)
    }

    // ─── 身体 ───
    const bodyGroup = new three.Group()
    group.add(bodyGroup)

    // 躯干（上窄下宽）
    const torso = new three.Mesh(
      new three.CapsuleGeometry(0.2, 0.38, 8, 16),
      outfitMat
    )
    torso.position.y = shoulderY - 0.24
    torso.castShadow = true
    bodyGroup.add(torso)

    // 领口/水手领
    const collar = new three.Mesh(
      new three.TorusGeometry(0.17, 0.04, 8, 20),
      whiteMat
    )
    collar.position.y = shoulderY + 0.03
    collar.rotation.x = Math.PI / 2
    bodyGroup.add(collar)

    // 领结（胸前）
    const bow = new three.Mesh(new three.BoxGeometry(0.14, 0.09, 0.04), ribbonMat)
    bow.position.set(0, shoulderY - 0.1, 0.19)
    bodyGroup.add(bow)

    // 裙子（上窄下宽锥形，体现裙摆）
    const skirt = new three.Mesh(
      new three.CylinderGeometry(0.21, 0.42, 0.3, 20),
      accentMat
    )
    skirt.position.y = 0.24
    skirt.castShadow = true
    bodyGroup.add(skirt)
    // 裙摆白边
    const skirtHem = new three.Mesh(
      new three.TorusGeometry(0.42, 0.035, 6, 20),
      whiteMat
    )
    skirtHem.position.y = 0.1
    skirtHem.rotation.x = Math.PI / 2
    bodyGroup.add(skirtHem)
    // 腰带 accent
    const belt = new three.Mesh(
      new three.CylinderGeometry(0.205, 0.205, 0.05, 16),
      ribbonMat
    )
    belt.position.y = 0.4
    bodyGroup.add(belt)

    // ─── 四肢（两段式骨骼，走路自然弯曲） ───
    // 左臂：肩 pivot + 上臂（袖）+ 前臂（皮肤）
    const leftArm = new three.Group()
    leftArm.position.set(-0.26, shoulderY + 0.02, 0)
    const leftUpper = new three.Mesh(
      new three.CapsuleGeometry(0.05, 0.2, 6, 12),
      outfitMat
    )
    leftUpper.position.y = -0.12
    leftArm.add(leftUpper)
    const leftForearm = new three.Group()
    leftForearm.position.y = -0.26
    const leftLower = new three.Mesh(
      new three.CapsuleGeometry(0.042, 0.17, 6, 12),
      skinMat
    )
    leftLower.position.y = -0.09
    leftForearm.add(leftLower)
    leftArm.add(leftForearm)
    bodyGroup.add(leftArm)
    refs.leftArm = leftArm
    refs.leftForearm = leftForearm

    // 右臂
    const rightArm = new three.Group()
    rightArm.position.set(0.26, shoulderY + 0.02, 0)
    const rightUpper = new three.Mesh(
      new three.CapsuleGeometry(0.05, 0.2, 6, 12),
      outfitMat
    )
    rightUpper.position.y = -0.12
    rightArm.add(rightUpper)
    const rightForearm = new three.Group()
    rightForearm.position.y = -0.26
    const rightLower = new three.Mesh(
      new three.CapsuleGeometry(0.042, 0.17, 6, 12),
      skinMat
    )
    rightLower.position.y = -0.09
    rightForearm.add(rightLower)
    rightArm.add(rightForearm)
    bodyGroup.add(rightArm)
    refs.rightArm = rightArm
    refs.rightForearm = rightForearm

    // 左腿：髋 pivot + 大腿 + 小腿（白袜）+ 鞋
    const leftLeg = new three.Group()
    leftLeg.position.set(-0.11, 0.04, 0)
    const leftThigh = new three.Mesh(
      new three.CapsuleGeometry(0.06, 0.22, 6, 12),
      skinMat
    )
    leftThigh.position.y = -0.13
    leftLeg.add(leftThigh)
    const leftShin = new three.Group()
    leftShin.position.y = -0.29
    const leftShinMesh = new three.Mesh(
      new three.CapsuleGeometry(0.045, 0.18, 6, 12),
      sockMat
    )
    leftShinMesh.position.y = -0.1
    leftShin.add(leftShinMesh)
    const leftShoe = new three.Mesh(
      new three.BoxGeometry(0.1, 0.055, 0.16),
      darkMat
    )
    leftShoe.position.set(0, -0.23, 0.03)
    leftShoe.castShadow = true
    leftShin.add(leftShoe)
    leftLeg.add(leftShin)
    bodyGroup.add(leftLeg)
    refs.leftLeg = leftLeg

    // 右腿
    const rightLeg = new three.Group()
    rightLeg.position.set(0.11, 0.04, 0)
    const rightThigh = new three.Mesh(
      new three.CapsuleGeometry(0.06, 0.22, 6, 12),
      skinMat
    )
    rightThigh.position.y = -0.13
    rightLeg.add(rightThigh)
    const rightShin = new three.Group()
    rightShin.position.y = -0.29
    const rightShinMesh = new three.Mesh(
      new three.CapsuleGeometry(0.045, 0.18, 6, 12),
      sockMat
    )
    rightShinMesh.position.y = -0.1
    rightShin.add(rightShinMesh)
    const rightShoe = new three.Mesh(
      new three.BoxGeometry(0.1, 0.055, 0.16),
      darkMat
    )
    rightShoe.position.set(0, -0.23, 0.03)
    rightShoe.castShadow = true
    rightShin.add(rightShoe)
    rightLeg.add(rightShin)
    bodyGroup.add(rightLeg)
    refs.rightLeg = rightLeg

    refs.body = bodyGroup
    refs.head = headGroup

    // 整体：脚落地（腿底局部约 -0.56 → 世界地面 -1.2）
    group.scale.set(0.95, 0.95, 0.95)
    group.position.y = -0.64
    group.traverse((node) => {
      if (node.isMesh) {
        node.castShadow = true
        if (node !== face && node !== leftEye && node !== rightEye) {
          node.receiveShadow = false
        }
      }
    })

    limbRefs = refs
    return group
  }

  // ═══════════════════════════════════════════════════════════════
  // 游戏级 3D 人物建模 V3（Game-Art 立体建模 + 卡通渲染管线）
  //  - 5 头身修长比例 + 耳朵/脖子/锁骨/手/脚等精细部件（非 Q 版积木）
  //  - MeshToonMaterial 三色调渐变贴图（亮/中/暗）+ 头发高光层
  //  - 背面放大描边（inverted hull）→ 经典游戏卡通轮廓
  //  - 精致虹膜大眼（CanvasTexture：瞳孔/双层高光/上睫毛）+ 可眨眼
  //  - 半球光 + 轮廓光 + 主光三光源（在 init 中配置），立体感接近游戏渲染
  //  - limbRefs 接口与 V2 完全一致：全部动画状态机（walk/jump/drag/drop/
  //    gaze/blink/breath/idle/wave/dance/sleep/feed/pat）零改动复用
  // ═══════════════════════════════════════════════════════════════

  /** 三色调卡通渐变贴图（MeshToonMaterial.gradientMap） */
  function createToonGradientMap() {
    if (!three || typeof document === 'undefined') return null
    try {
      const canvas = document.createElement('canvas')
      canvas.width = 4
      canvas.height = 64
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      const grad = ctx.createLinearGradient(0, 0, 0, 64)
      grad.addColorStop(0, '#ffffff')   // 亮部
      grad.addColorStop(0.42, '#d9d9d9') // 中间调
      grad.addColorStop(1, '#8a8a8a')   // 暗部（柔和过渡，非硬切）
      ctx.fillStyle = grad
      ctx.fillRect(0, 0, 4, 64)
      const tex = new three.CanvasTexture(canvas)
      tex.minFilter = three.NearestFilter
      tex.magFilter = three.NearestFilter
      tex.needsUpdate = true
      return tex
    } catch (e) {
      return null
    }
  }

  /** V3 卡通材质（三色调） */
  function toonMatV3(color, opts = {}) {
    const mat = new three.MeshToonMaterial({
      color: new three.Color(color),
      transparent: opts.transparent || false,
      opacity: opts.opacity !== undefined ? opts.opacity : 1,
      gradientMap: toonGradientMap || null
    })
    return mat
  }

  /** V5 真人感皮肤材质（柔和清漆 + 微光泽，游戏角色质感） */
  function realSkinMat(color, opts = {}) {
    return new three.MeshPhysicalMaterial({
      color: new three.Color(color),
      roughness: opts.roughness !== undefined ? opts.roughness : 0.5,
      metalness: 0,
      clearcoat: opts.clearcoat !== undefined ? opts.clearcoat : 0.3,
      clearcoatRoughness: 0.4,
      transparent: opts.transparent || false,
      opacity: opts.opacity !== undefined ? opts.opacity : 1,
      side: three.FrontSide
    })
  }

  /** V5 真人感头发材质（发丝光泽 + 高光清漆） */
  function realHairMat(color, opts = {}) {
    return new three.MeshPhysicalMaterial({
      color: new three.Color(color),
      roughness: opts.roughness !== undefined ? opts.roughness : 0.36,
      metalness: 0.05,
      clearcoat: opts.clearcoat !== undefined ? opts.clearcoat : 0.75,
      clearcoatRoughness: 0.22,
      transparent: opts.transparent || false,
      opacity: opts.opacity !== undefined ? opts.opacity : 1,
      side: three.FrontSide
    })
  }

  /** V5 真人感服装材质（棉/丝绸面料差异） */
  function realClothMat(color, opts = {}) {
    return new three.MeshPhysicalMaterial({
      color: new three.Color(color),
      roughness: opts.roughness !== undefined ? opts.roughness : 0.82,
      metalness: 0,
      clearcoat: opts.clearcoat !== undefined ? opts.clearcoat : 0.06,
      transparent: opts.transparent || false,
      opacity: opts.opacity !== undefined ? opts.opacity : 1,
      side: three.FrontSide
    })
  }

  /** 虹膜贴图不可用时的兜底眼睛材质（三色调卡通） */
  function eyesMatV3(palette) {
    const pal = getAnimeElf3DPalette(palette)
    return toonMatV3(pal.eyes || '#ff6f9c')
  }

  /**
   * 为 mesh 生成背面放大描边（inverted hull）。
   * 轮廓线作为父组的子对象，放大比例 scaleMul 只作用于该 mesh 的自身缩放。
   */
  function addOutline(mesh, scaleMul = 1.045, color = '#332b3d', opacity = 0.95) {
    if (!three || !mesh) return null
    try {
      const outline = new three.Mesh(
        mesh.geometry,
        new three.MeshBasicMaterial({
          color: new three.Color(color),
          side: three.BackSide,
          transparent: true,
          opacity,
          depthWrite: false,
          polygonOffset: true,
          polygonOffsetFactor: -2
        })
      )
      outline.position.copy(mesh.position)
      outline.rotation.copy(mesh.rotation)
      outline.scale.copy(mesh.scale).multiplyScalar(scaleMul)
      outline.renderOrder = 0
      mesh.parent?.add(outline)
      return outline
    } catch (e) {
      return null
    }
  }

  /**
   * 精致二次元虹膜贴图（大眼：虹膜渐变 + 瞳孔 + 双层高光 + 上眼睑影 + 睫毛）。
   * 贴到凸起眼球前侧，配合 V2 的眨眼压扁动画。
   */
  function createAnimeEyeTexture(palette) {
    if (!three || typeof document === 'undefined') return null
    const pal = getAnimeElf3DPalette(palette)
    try {
      const canvas = document.createElement('canvas')
      canvas.width = 256
      canvas.height = 256
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      ctx.clearRect(0, 0, 256, 256)
      // 眼白
      ctx.fillStyle = '#ffffff'
      ctx.beginPath()
      ctx.ellipse(128, 128, 108, 118, 0, 0, Math.PI * 2)
      ctx.fill()
      // 上眼睑阴影（立体感）
      const lidGrad = ctx.createLinearGradient(0, 0, 0, 120)
      lidGrad.addColorStop(0, 'rgba(120,90,130,0.0)')
      lidGrad.addColorStop(1, 'rgba(120,90,130,0.28)')
      ctx.fillStyle = lidGrad
      ctx.beginPath()
      ctx.ellipse(128, 40, 112, 90, 0, 0, Math.PI * 2)
      ctx.fill()
      // 虹膜（渐变圆）
      const irisGrad = ctx.createRadialGradient(128, 138, 6, 128, 138, 72)
      irisGrad.addColorStop(0, pal.eyeHighlight || '#ffffff')
      irisGrad.addColorStop(0.22, pal.eyes || '#ff6f9c')
      irisGrad.addColorStop(1, '#2a1a3a')
      ctx.fillStyle = irisGrad
      ctx.beginPath()
      ctx.ellipse(128, 142, 62, 68, 0, 0, Math.PI * 2)
      ctx.fill()
      // 瞳孔
      ctx.fillStyle = '#241a2e'
      ctx.beginPath()
      ctx.arc(128, 144, 24, 0, Math.PI * 2)
      ctx.fill()
      // 主高光（大）
      ctx.fillStyle = 'rgba(255,255,255,0.95)'
      ctx.beginPath()
      ctx.arc(104, 116, 20, 0, Math.PI * 2)
      ctx.fill()
      // 副高光（小）
      ctx.fillStyle = 'rgba(255,255,255,0.7)'
      ctx.beginPath()
      ctx.arc(152, 166, 10, 0, Math.PI * 2)
      ctx.fill()
      // 下睫毛线
      ctx.strokeStyle = 'rgba(60,40,70,0.5)'
      ctx.lineWidth = 4
      ctx.beginPath()
      ctx.moveTo(78, 210)
      ctx.quadraticCurveTo(128, 232, 178, 210)
      ctx.stroke()
      const tex = new three.CanvasTexture(canvas)
      tex.colorSpace = three.SRGBColorSpace
      return tex
    } catch (e) {
      return null
    }
  }

  /**
   * 游戏级 3D 人物建模 V3。
   * @returns {THREE.Group|null}
   */
  /**
   * 解析换装套装：传入对象含 id 且在 elfOutfitSets 中 → 返回该套装；
   * 否则回退默认校服套装（二次元 3D 角色始终有完整套装）。
   */
  function resolveOutfitSet(outfit) {
    if (outfit && outfit.id) {
      const found = elfOutfitSets.find((s) => s.id === outfit.id)
      if (found) return found
    }
    if (outfit && outfit.top && outfit.colors) return outfit
    return DEFAULT_OUTFIT_SET
  }

  /**
   * 构建换装套装 3D 服装部件（换装小游戏级：上衣/下装/鞋/袜 全套程序化建模）。
   * 挂在 bodyGroup 下，与骨骼（手臂/腿）位置协调；脚底 y=0 基准。
   * @param {Object} set 套装数据（top/bottom/shoes/socks/colors）
   * @param {Object} mats 材质集 { topMat, topTrimMat, bottomMat, accentMat, trimMat, sockMat, shoeMat, darkMat }
   * @param {THREE.Group} bodyGroup
   * @param {Object} body 身体参数 { shoulderY, hipY, headR }
   * @returns {{ chest:? , skirt:? }} 关键 mesh（供 receiveShadow）
   */
  function buildOutfit3D(set, mats, bodyGroup, body) {
    if (!three) return {}
    const { shoulderY, hipY } = body
    // 美型少女比例参数（V4 传入更细的胸围/裙摆系数；V3 保持默认）
    const chestR = body.chestR || 0.21
    const skirtScale = body.skirtScale || 1
    const out = {}
    const topMat = mats.topMat
    const topTrimMat = mats.topTrimMat || mats.topMat
    const bottomMat = mats.bottomMat
    const accentMat = mats.accentMat
    const trimMat = mats.trimMat
    const sockMat = mats.sockMat
    const shoeMat = mats.shoeMat

    // ─── 上衣（胸/腰一体 + 款式差异） ───
    const topType = set.top || 'shirt'
    if (topType === 'shirt') {
      // 学院衬衫：圆润上身 + 立领 + 胸口暗线
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR, chestR * 0.95, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.15, 1, 0.88)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 领口白立领
      const collar = new three.Mesh(new three.TorusGeometry(chestR * 0.88, 0.042, 10, 24), trimMat)
      collar.position.y = shoulderY + 0.02
      collar.rotation.x = Math.PI / 2
      bodyGroup.add(collar)
      // 领结
      const bow = new three.Mesh(new three.BoxGeometry(0.16, 0.1, 0.045), accentMat)
      bow.position.set(0, shoulderY - 0.12, chestR * 1.02)
      bodyGroup.add(bow)
      // 锁骨阴影线
      const collarLineMat = new three.Mesh(
        new three.BoxGeometry(0.2, 0.012, 0.012),
        topTrimMat
      )
      collarLineMat.position.set(-0.1, shoulderY - 0.18, chestR * 0.93)
      collarLineMat.rotation.z = 0.18
      bodyGroup.add(collarLineMat)
      const collarLineR = new three.Mesh(
        new three.BoxGeometry(0.2, 0.012, 0.012),
        topTrimMat
      )
      collarLineR.position.set(0.1, shoulderY - 0.18, chestR * 0.93)
      collarLineR.rotation.z = -0.18
      bodyGroup.add(collarLineR)
    } else if (topType === 'puff') {
      // 甜心泡泡袖上衣：上宽下收 + 胸前装饰
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR * 1.1, chestR * 0.76, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.22, 1, 0.9)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 胸前花边（两层半透明圆环）
      const lace1 = new three.Mesh(new three.TorusGeometry(chestR * 0.76, 0.03, 8, 24), trimMat)
      lace1.position.y = shoulderY - 0.28
      lace1.rotation.x = Math.PI / 2
      bodyGroup.add(lace1)
      const lace2 = new three.Mesh(new three.TorusGeometry(chestR * 0.62, 0.025, 8, 20), trimMat)
      lace2.position.y = shoulderY - 0.4
      lace2.rotation.x = Math.PI / 2
      bodyGroup.add(lace2)
    } else if (topType === 'tshirt') {
      // 运动 T 恤：宽松上身 + 短袖口 + 领口色带
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR * 1.05, chestR * 0.86, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.18, 1, 0.86)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      const neckBand = new three.Mesh(new three.TorusGeometry(chestR * 0.83, 0.03, 8, 22), accentMat)
      neckBand.position.y = shoulderY + 0.03
      neckBand.rotation.x = Math.PI / 2
      bodyGroup.add(neckBand)
      // 胸前号码块（色带）
      const num = new three.Mesh(new three.BoxGeometry(0.16, 0.14, 0.02), accentMat)
      num.position.set(0, shoulderY - 0.22, chestR * 0.9)
      bodyGroup.add(num)
    } else if (topType === 'yukata') {
      // 和风浴衣：V 领上衣 + 宽腰带 + 细绳结
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR, chestR * 1.14, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.18, 1.06, 0.86)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // V 领（深色三角色块）
      const vNeck = new three.Mesh(new three.ConeGeometry(0.1, 0.16, 4), topTrimMat)
      vNeck.position.set(0, shoulderY + 0.02, chestR * 0.9)
      vNeck.rotation.x = Math.PI
      vNeck.scale.set(1, 0.7, 0.2)
      bodyGroup.add(vNeck)
      // 宽腰带
      const obi = new three.Mesh(new three.CylinderGeometry(chestR * 0.95, chestR * 1.0, 0.1, 20), accentMat)
      obi.position.y = hipY + 0.34
      bodyGroup.add(obi)
      // 腰后蝴蝶结
      const knot = new three.Mesh(new three.BoxGeometry(0.14, 0.1, 0.04), accentMat)
      knot.position.set(0, hipY + 0.36, -0.2)
      bodyGroup.add(knot)
    } else if (topType === 'sailor') {
      // 水手服：合体上衣 + 大翻领 + 条纹领巾
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR, chestR * 1.0, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.12, 1, 0.85)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 大翻领（左右两片斜三角）
      const collarL = new three.Mesh(new three.ConeGeometry(0.11, 0.18, 4), trimMat)
      collarL.position.set(-0.09, shoulderY - 0.03, chestR * 0.82)
      collarL.rotation.z = 0.4
      collarL.rotation.x = 0.25
      collarL.scale.set(1, 1, 0.35)
      bodyGroup.add(collarL)
      const collarR = new three.Mesh(new three.ConeGeometry(0.11, 0.18, 4), trimMat)
      collarR.position.set(0.09, shoulderY - 0.03, chestR * 0.82)
      collarR.rotation.z = -0.4
      collarR.rotation.x = 0.25
      collarR.scale.set(1, 1, 0.35)
      bodyGroup.add(collarR)
      // 领巾（垂下长条 + 结）
      const scarf = new three.Mesh(new three.BoxGeometry(0.05, 0.34, 0.015), accentMat)
      scarf.position.set(0, shoulderY - 0.3, chestR * 0.9)
      bodyGroup.add(scarf)
      const scarfKnot = new three.Mesh(new three.BoxGeometry(0.07, 0.06, 0.02), accentMat)
      scarfKnot.position.set(0, shoulderY - 0.16, chestR * 0.92)
      bodyGroup.add(scarfKnot)
    } else if (topType === 'dress') {
      // 连衣裙：收腰合体上身 + 心形领口 + 泡泡袖口
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR * 0.9, chestR * 0.8, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.18, 1.1, 0.88)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 心形领口（两个小圆 + 尖）
      const neckLeft = new three.Mesh(new three.SphereGeometry(0.045, 10, 10), topTrimMat)
      neckLeft.position.set(-0.055, shoulderY + 0.02, chestR * 0.9)
      neckLeft.scale.set(1, 1.4, 0.4)
      bodyGroup.add(neckLeft)
      const neckRight = new three.Mesh(new three.SphereGeometry(0.045, 10, 10), topTrimMat)
      neckRight.position.set(0.055, shoulderY + 0.02, chestR * 0.9)
      neckRight.scale.set(1, 1.4, 0.4)
      bodyGroup.add(neckRight)
      // 蝴蝶结腰饰
      const waistBow = new three.Mesh(new three.BoxGeometry(0.14, 0.08, 0.04), accentMat)
      waistBow.position.set(0, hipY + 0.18, chestR * 0.9)
      bodyGroup.add(waistBow)
    } else if (topType === 'hanfu') {
      // 汉服：交领右衽上衣 + 宽袖 + 束腰带（古风）
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR * 1.02, chestR * 1.0, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.2, 1.1, 0.9)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 交领（左襟压右襟的斜领片）
      const collarL = new three.Mesh(new three.BoxGeometry(0.1, 0.16, 0.02), topTrimMat)
      collarL.position.set(-0.06, shoulderY - 0.02, chestR * 0.96)
      collarL.rotation.z = 0.35
      bodyGroup.add(collarL)
      const collarR = new three.Mesh(new three.BoxGeometry(0.1, 0.16, 0.02), topTrimMat)
      collarR.position.set(0.06, shoulderY - 0.02, chestR * 0.96)
      collarR.rotation.z = -0.35
      bodyGroup.add(collarR)
      // 束腰带（宽腰带 + 玉扣）
      const belt = new three.Mesh(new three.CylinderGeometry(0.175, 0.19, 0.09, 20), accentMat)
      belt.position.y = hipY + 0.22
      bodyGroup.add(belt)
      const jade = new three.Mesh(new three.TorusGeometry(0.035, 0.012, 8, 16), trimMat)
      jade.position.set(0, hipY + 0.22, 0.19)
      jade.rotation.x = Math.PI / 2
      bodyGroup.add(jade)
      // 宽袖（两侧垂袖，加长遮手）
      const sleeveL = new three.Mesh(new three.CapsuleGeometry(0.06, 0.3, 8, 14), topMat)
      sleeveL.position.set(-0.32, shoulderY - 0.16, 0.02)
      sleeveL.rotation.z = 0.1
      bodyGroup.add(sleeveL)
      const sleeveR = new three.Mesh(new three.CapsuleGeometry(0.06, 0.3, 8, 14), topMat)
      sleeveR.position.set(0.32, shoulderY - 0.16, 0.02)
      sleeveR.rotation.z = -0.1
      bodyGroup.add(sleeveR)
      // 袖口滚边
      const cuffL = new three.Mesh(new three.TorusGeometry(0.045, 0.014, 8, 16), trimMat)
      cuffL.position.set(-0.32, shoulderY - 0.32, 0.02)
      cuffL.rotation.x = Math.PI / 2
      bodyGroup.add(cuffL)
      const cuffR = new three.Mesh(new three.TorusGeometry(0.045, 0.014, 8, 16), trimMat)
      cuffR.position.set(0.32, shoulderY - 0.32, 0.02)
      cuffR.rotation.x = Math.PI / 2
      bodyGroup.add(cuffR)
    } else {
      const chest = new three.Mesh(new three.CapsuleGeometry(chestR * 1.1, chestR * 1.05, 10, 20), topMat)
      chest.position.y = shoulderY - 0.12
      chest.scale.set(1.24, 1.04, 0.92)
      chest.castShadow = true
      bodyGroup.add(chest)
      out.chest = chest
      addOutline(chest, 1.035, '#2e2433', 0.9)
      // 门襟条
      const placket = new three.Mesh(new three.BoxGeometry(0.05, 0.3, 0.02), trimMat)
      placket.position.set(-0.05, shoulderY - 0.24, chestR * 0.95)
      bodyGroup.add(placket)
      // 纽扣
      for (let i = 0; i < 3; i++) {
        const btn = new three.Mesh(new three.SphereGeometry(0.018, 8, 8), accentMat)
        btn.position.set(-0.05, shoulderY - 0.16 - i * 0.09, chestR * 1.03)
        bodyGroup.add(btn)
      }
    }
    // 腰（所有款式统一的收腰段，用上衣色）
    const waist = new three.Mesh(new three.CylinderGeometry(0.16, 0.19, 0.22, 20), topMat)
    waist.position.y = hipY + 0.18
    waist.castShadow = true
    bodyGroup.add(waist)

    // ─── 下装 ───
    const bottomType = set.bottom || 'pleated-skirt'
    if (bottomType === 'pleated-skirt') {
      const skirt = new three.Mesh(new three.CylinderGeometry(0.2 * skirtScale, 0.46 * skirtScale, 0.34, 24), bottomMat)
      skirt.position.y = hipY - 0.06
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      const skirtMid = new three.Mesh(
        new three.CylinderGeometry(0.3 * skirtScale, 0.42 * skirtScale, 0.18, 24),
        toonMatV3(set.colors.topTrim, { transparent: true, opacity: 0.55 })
      )
      skirtMid.position.y = hipY - 0.24
      skirtMid.scale.set(1, 0.9, 1)
      bodyGroup.add(skirtMid)
      const skirtHem = new three.Mesh(new three.TorusGeometry(0.46 * skirtScale, 0.04, 8, 28), trimMat)
      skirtHem.position.y = hipY - 0.25
      skirtHem.rotation.x = Math.PI / 2
      bodyGroup.add(skirtHem)
      // 百褶线（8 条细柱）
      for (let i = 0; i < 8; i++) {
        const a = (i / 8) * Math.PI * 2
        const pleat = new three.Mesh(
          new three.BoxGeometry(0.015, 0.3, 0.02),
          toonMatV3(set.colors.topTrim, { transparent: true, opacity: 0.5 })
        )
        pleat.position.set(Math.cos(a) * 0.33 * skirtScale, hipY - 0.08, Math.sin(a) * 0.33 * skirtScale)
        pleat.rotation.y = -a
        bodyGroup.add(pleat)
      }
      // 腰带
      const belt = new three.Mesh(new three.CylinderGeometry(0.195 * skirtScale, 0.2 * skirtScale, 0.06, 20), accentMat)
      belt.position.y = hipY + 0.06
      bodyGroup.add(belt)
    } else if (bottomType === 'puff-skirt') {
      // 蓬蓬裙：双层外扩 + 下摆花边
      const skirt = new three.Mesh(new three.CylinderGeometry(0.24 * skirtScale, 0.5 * skirtScale, 0.3, 24), bottomMat)
      skirt.position.y = hipY - 0.05
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      const skirt2 = new three.Mesh(
        new three.CylinderGeometry(0.34 * skirtScale, 0.54 * skirtScale, 0.18, 24),
        toonMatV3(set.colors.trim, { transparent: true, opacity: 0.5 })
      )
      skirt2.position.y = hipY - 0.22
      skirt2.scale.set(1, 0.92, 1)
      bodyGroup.add(skirt2)
      const hem = new three.Mesh(new three.TorusGeometry(0.52 * skirtScale, 0.05, 8, 30), trimMat)
      hem.position.y = hipY - 0.24
      hem.rotation.x = Math.PI / 2
      bodyGroup.add(hem)
    } else if (bottomType === 'shorts') {
      // 运动短裤：宽口短筒
      const shorts = new three.Mesh(new three.CylinderGeometry(0.2 * skirtScale, 0.26 * skirtScale, 0.22, 20), bottomMat)
      shorts.position.y = hipY - 0.02
      shorts.castShadow = true
      bodyGroup.add(shorts)
      out.skirt = shorts
      addOutline(shorts, 1.03, '#2e2433', 0.9)
      const cuffL = new three.Mesh(new three.TorusGeometry(0.2 * skirtScale, 0.03, 8, 18), accentMat)
      cuffL.position.set(-0.14, hipY - 0.13, 0)
      cuffL.rotation.x = Math.PI / 2
      bodyGroup.add(cuffL)
      const cuffR = new three.Mesh(new three.TorusGeometry(0.2 * skirtScale, 0.03, 8, 18), accentMat)
      cuffR.position.set(0.14, hipY - 0.13, 0)
      cuffR.rotation.x = Math.PI / 2
      bodyGroup.add(cuffR)
    } else if (bottomType === 'straight') {
      // 和风长下摆（浴衣直筒至脚踝）
      const skirt = new three.Mesh(new three.CylinderGeometry(0.24 * skirtScale, 0.3 * skirtScale, 0.62, 20), bottomMat)
      skirt.position.y = hipY - 0.28
      skirt.scale.set(1, 1, 0.8)
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      const hem = new three.Mesh(new three.TorusGeometry(0.3 * skirtScale, 0.035, 8, 24), trimMat)
      hem.position.y = hipY - 0.6
      hem.rotation.x = Math.PI / 2
      bodyGroup.add(hem)
    } else if (bottomType === 'pencil') {
      // 铅笔裙（包臀修身）：上宽下收窄筒 + 开衩线
      const skirt = new three.Mesh(new three.CylinderGeometry(0.22 * skirtScale, 0.2 * skirtScale, 0.34, 20), bottomMat)
      skirt.position.y = hipY - 0.1
      skirt.scale.set(1, 1, 0.78)
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      const belt = new three.Mesh(new three.CylinderGeometry(0.22 * skirtScale, 0.225 * skirtScale, 0.05, 20), accentMat)
      belt.position.y = hipY + 0.05
      bodyGroup.add(belt)
      // 前开衩线
      const slit = new three.Mesh(new three.BoxGeometry(0.012, 0.2, 0.012), trimMat)
      slit.position.set(0, hipY - 0.2, 0.2 * skirtScale)
      bodyGroup.add(slit)
    } else if (bottomType === 'flared') {
      // A 字裙：上窄下宽自然外扩 + 荷叶边下摆
      const skirt = new three.Mesh(new three.CylinderGeometry(0.2 * skirtScale, 0.52 * skirtScale, 0.32, 24), bottomMat)
      skirt.position.y = hipY - 0.07
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      const flare = new three.Mesh(new three.CylinderGeometry(0.36 * skirtScale, 0.52 * skirtScale, 0.12, 24), trimMat)
      flare.position.y = hipY - 0.22
      flare.scale.set(1, 0.9, 1)
      bodyGroup.add(flare)
      const hem = new three.Mesh(new three.TorusGeometry(0.52 * skirtScale, 0.04, 8, 28), trimMat)
      hem.position.y = hipY - 0.24
      hem.rotation.x = Math.PI / 2
      bodyGroup.add(hem)
    } else if (bottomType === 'long-skirt') {
      // 古风长裙：曳地长裙摆 + 裙摆缘边 + 玉坠
      const skirt = new three.Mesh(new three.CylinderGeometry(0.2 * skirtScale, 0.58 * skirtScale, 0.5, 28), bottomMat)
      skirt.position.y = hipY - 0.16
      skirt.castShadow = true
      bodyGroup.add(skirt)
      out.skirt = skirt
      addOutline(skirt, 1.03, '#2e2433', 0.9)
      // 裙摆缘边（滚边）
      const hem = new three.Mesh(new three.TorusGeometry(0.58 * skirtScale, 0.035, 8, 32), trimMat)
      hem.position.y = hipY - 0.42
      hem.rotation.x = Math.PI / 2
      bodyGroup.add(hem)
      // 腰侧玉坠（丝绦）
      const tassel = new three.Mesh(new three.CapsuleGeometry(0.012, 0.16, 6, 10), accentMat)
      tassel.position.set(-0.16, hipY - 0.12, 0.17)
      tassel.rotation.x = 0.2
      bodyGroup.add(tassel)
      const jadeDrop = new three.Mesh(new three.SphereGeometry(0.02, 10, 10), trimMat)
      jadeDrop.position.set(-0.16, hipY - 0.25, 0.17)
      bodyGroup.add(jadeDrop)
    } else {
      // pants 睡裤：两腿筒（宽松）
      const leftLegCloth = new three.Mesh(new three.CapsuleGeometry(0.09, 0.3, 8, 14), bottomMat)
      leftLegCloth.position.set(-0.13, hipY - 0.26, 0)
      leftLegCloth.scale.set(1, 1, 0.8)
      bodyGroup.add(leftLegCloth)
      const rightLegCloth = new three.Mesh(new three.CapsuleGeometry(0.09, 0.3, 8, 14), bottomMat)
      rightLegCloth.position.set(0.13, hipY - 0.26, 0)
      rightLegCloth.scale.set(1, 1, 0.8)
      bodyGroup.add(rightLegCloth)
      const waist = new three.Mesh(new three.CylinderGeometry(0.2 * skirtScale, 0.2 * skirtScale, 0.08, 20), accentMat)
      waist.position.y = hipY + 0.01
      bodyGroup.add(waist)
    }

    // ─── 鞋（在腿部构建，见 buildShoe3D） ───
    return out
  }

  /**
   * 构建发饰 3D（蝴蝶结/发带/星星发夹/皇冠/花环/无）。
   * 挂在 headGroup 上，随头部动作一起动。
   * @param {string} accessoryType
   * @param {Object} mats { accessoryMat, trimMat, accentMat }
   * @param {THREE.Group} headGroup
   * @param {number} headR
   */
  function buildAccessory3D(accessoryType, mats, headGroup, headR) {
    if (!three || !headGroup) return null
    const acc = accessoryType || 'none'
    if (acc === 'none') return null
    const accMat = mats.accessoryMat || mats.accentMat
    const trimMat = mats.trimMat
    const accentMat = mats.accentMat
    const topY = headR * 1.05
    const group = new three.Group()

    if (acc === 'bow') {
      // 蝴蝶结发饰（两翼 + 中结）
      const wingL = new three.Mesh(new three.ConeGeometry(0.09, 0.05, 4), accMat)
      wingL.position.set(-0.09, topY, 0.02)
      wingL.rotation.z = 0.5
      wingL.scale.set(1, 1, 0.5)
      group.add(wingL)
      const wingR = new three.Mesh(new three.ConeGeometry(0.09, 0.05, 4), accMat)
      wingR.position.set(0.09, topY, 0.02)
      wingR.rotation.z = -0.5
      wingR.scale.set(1, 1, 0.5)
      group.add(wingR)
      const knot = new three.Mesh(new three.SphereGeometry(0.03, 8, 8), accentMat)
      knot.position.set(0, topY, 0.035)
      group.add(knot)
    } else if (acc === 'ribbon') {
      // 发带：环绕头部的细带 + 尾结
      const band = new three.Mesh(new three.TorusGeometry(headR * 1.02, 0.022, 8, 24), accMat)
      band.position.y = topY
      band.rotation.x = Math.PI / 2
      group.add(band)
      const tailL = new three.Mesh(new three.BoxGeometry(0.02, 0.16, 0.01), accMat)
      tailL.position.set(-0.18, topY - 0.06, 0)
      tailL.rotation.z = 0.3
      group.add(tailL)
      const tailR = new three.Mesh(new three.BoxGeometry(0.02, 0.16, 0.01), accMat)
      tailR.position.set(0.18, topY - 0.06, 0)
      tailR.rotation.z = -0.3
      group.add(tailR)
    } else if (acc === 'star') {
      // 星星发夹：小锥星 + 圆环底座
      const star = new three.Mesh(new three.OctahedronGeometry(0.06, 0), accMat)
      star.position.set(0, topY + 0.03, 0.03)
      star.scale.set(1, 1.3, 1)
      group.add(star)
      const base = new three.Mesh(new three.TorusGeometry(0.05, 0.012, 6, 16), trimMat)
      base.position.set(0, topY, 0.02)
      base.rotation.x = Math.PI / 2
      group.add(base)
    } else if (acc === 'crown') {
      // 皇冠：底座环 + 三个齿 + 宝石
      const base = new three.Mesh(new three.CylinderGeometry(0.06, 0.065, 0.025, 10), accMat)
      base.position.set(0, topY + 0.015, 0)
      group.add(base)
      for (let i = 0; i < 3; i++) {
        const a = (i / 3) * Math.PI * 2 - Math.PI / 2
        const spike = new three.Mesh(new three.ConeGeometry(0.018, 0.06, 6), accMat)
        spike.position.set(Math.cos(a) * 0.05, topY + 0.055, Math.sin(a) * 0.05)
        group.add(spike)
      }
      const gem = new three.Mesh(new three.SphereGeometry(0.014, 8, 8), accentMat)
      gem.position.set(0, topY + 0.015, 0.055)
      group.add(gem)
    } else if (acc === 'flower') {
      // 花环：5 瓣小花（半球瓣）
      for (let i = 0; i < 5; i++) {
        const a = (i / 5) * Math.PI * 2
        const petal = new three.Mesh(new three.SphereGeometry(0.028, 8, 8), accMat)
        petal.position.set(Math.cos(a) * 0.055, topY + 0.02, Math.sin(a) * 0.055)
        petal.scale.set(1, 0.6, 1)
        group.add(petal)
      }
      const center = new three.Mesh(new three.SphereGeometry(0.022, 8, 8), accentMat)
      center.position.set(0, topY + 0.025, 0)
      group.add(center)
    } else if (acc === 'hairpin') {
      // 古风发簪：斜插发簪 + 珠坠流苏（盘发点睛）
      const pin = new three.Mesh(new three.CylinderGeometry(0.008, 0.012, 0.34, 8), accMat)
      pin.position.set(-0.1, topY - 0.02, -0.04)
      pin.rotation.z = 0.42
      group.add(pin)
      const head = new three.Mesh(new three.SphereGeometry(0.026, 10, 10), accMat)
      head.position.set(-0.21, topY + 0.02, -0.04)
      group.add(head)
      // 珠坠流苏（两条垂珠链）
      const beadGeo = new three.SphereGeometry(0.011, 8, 8)
      for (let i = 0; i < 3; i++) {
        const bead = new three.Mesh(beadGeo, trimMat)
        bead.position.set(-0.27, topY - 0.02 - i * 0.05, -0.05)
        group.add(bead)
      }
      const bead2 = new three.Mesh(beadGeo, accentMat)
      bead2.position.set(-0.31, topY - 0.14, -0.06)
      group.add(bead2)
    }

    headGroup.add(group)
    return group
  }

  function buildAnimeElf3DV3(character, palette, outfitSet) {
    if (!three || !scene) return null
    const pal = getAnimeElf3DPalette(palette || (character && character.palette))
    const hairParts = getAnimeElfHairPartDescriptors(character && character.hairstyle)
    // 换装套装（V4 换装小游戏级）：二次元 3D 角色始终穿完整套装
    const set = resolveOutfitSet(outfitSet)

    const group = new three.Group()
    const refs = {}

    // ─── 材质（三色调卡通） ───
    const skinMat = toonMatV3(pal.skin)
    const hairMat = toonMatV3(pal.hair)
    const hairHiMat = toonMatV3(pal.hairHighlight || '#ffffff', { transparent: true, opacity: 0.35 })
    const whiteMat = toonMatV3('#fdfdfd')
    // 套装材质（服装颜色来自套装配色，不随角色调色板）
    const topMat = toonMatV3(set.colors.top)
    const topTrimMat = toonMatV3(set.colors.topTrim || set.colors.top)
    const bottomMat = toonMatV3(set.colors.bottom)
    const accentMat = toonMatV3(set.colors.accent)
    const trimMat = toonMatV3(set.colors.trim || '#ffffff')
    const sockMat = toonMatV3(set.colors.socks || '#f6f8fc')
    const shoeMat = toonMatV3(set.colors.shoes)
    const ribbonMat = toonMatV3(pal.ribbon)
    const blushMat = toonMatV3(pal.blush, { transparent: true, opacity: 0.6 })
    const darkMat = toonMatV3('#33334a')

    // ─── 比例（5 头身游戏角色；脚底 y=0） ───
    const headR = 0.34
    const headY = 1.78
    const shoulderY = 1.34
    const hipY = 0.62

    // ─── 头部 ───
    const headGroup = new three.Group()
    headGroup.position.y = headY
    group.add(headGroup)

    // 脸（细分球，下颌微收）
    const face = new three.Mesh(new three.SphereGeometry(headR, 48, 48), skinMat)
    face.scale.set(1, 1.08, 0.95)
    face.castShadow = true
    headGroup.add(face)
    addOutline(face, 1.035, '#2e2433', 0.9)

    // 耳朵（左右小椭球，游戏角色常见）
    const earMat = toonMatV3(pal.skin)
    const earGeo = new three.SphereGeometry(0.07, 14, 14)
    const earL = new three.Mesh(earGeo, earMat)
    earL.position.set(-headR * 0.98, -0.06, 0)
    earL.scale.set(0.55, 0.85, 0.55)
    headGroup.add(earL)
    const earR = new three.Mesh(earGeo, earMat)
    earR.position.set(headR * 0.98, -0.06, 0)
    earR.scale.set(0.55, 0.85, 0.55)
    headGroup.add(earR)

    // 眼睛：凸起眼球（虹膜贴图）+ 独立高光
    const eyeTex = createAnimeEyeTexture(pal)
    const eyeGeo = new three.SphereGeometry(0.085, 20, 20)
    const eyeMatBase = eyeTex
      ? new three.MeshBasicMaterial({ map: eyeTex, transparent: true, alphaTest: 0.02 })
      : eyesMatV3(pal)
    const leftEye = new three.Mesh(eyeGeo, eyeMatBase)
    leftEye.position.set(-0.135, 0.05, headR * 0.86)
    leftEye.scale.set(1, 1.32, 0.36)
    headGroup.add(leftEye)
    const rightEye = new three.Mesh(eyeGeo, eyeMatBase)
    rightEye.position.set(0.135, 0.05, headR * 0.86)
    rightEye.scale.set(1, 1.32, 0.36)
    headGroup.add(rightEye)
    refs.eyes = [leftEye, rightEye]
    // 玻璃高光（眼球外层半透明白点）
    const hlGeo = new three.SphereGeometry(0.024, 10, 10)
    const hlL = new three.Mesh(hlGeo, toonMatV3('#ffffff', { transparent: true, opacity: 0.95 }))
    hlL.position.set(-0.112, 0.105, headR * 1.0)
    headGroup.add(hlL)
    const hlR = new three.Mesh(hlGeo, toonMatV3('#ffffff', { transparent: true, opacity: 0.95 }))
    hlR.position.set(0.158, 0.105, headR * 1.0)
    headGroup.add(hlR)

    // 眉毛
    const browGeo = new three.BoxGeometry(0.12, 0.022, 0.03)
    const browMat = toonMatV3(pal.brow)
    const browL = new three.Mesh(browGeo, browMat)
    browL.position.set(-0.135, 0.16, headR * 0.86)
    headGroup.add(browL)
    const browR = new three.Mesh(browGeo, browMat)
    browR.position.set(0.135, 0.16, headR * 0.86)
    headGroup.add(browR)

    // 腮红
    const blushGeo = new three.SphereGeometry(0.055, 12, 12)
    const blushL = new three.Mesh(blushGeo, blushMat)
    blushL.position.set(-0.23, -0.05, headR * 0.82)
    blushL.scale.set(1, 0.55, 0.35)
    headGroup.add(blushL)
    const blushR = new three.Mesh(blushGeo, blushMat)
    blushR.position.set(0.23, -0.05, headR * 0.82)
    blushR.scale.set(1, 0.55, 0.35)
    headGroup.add(blushR)

    // 嘴巴（浅色微笑扁盒）
    const mouth = new three.Mesh(new three.BoxGeometry(0.09, 0.02, 0.022), toonMatV3('#d9706e'))
    mouth.position.set(0, -0.16, headR * 0.88)
    headGroup.add(mouth)

    // ─── 头发（分层 + 描边 + 高光层） ───
    const hairGroup = new three.Group()
    headGroup.add(hairGroup)

    if (hairParts.includes('back')) {
      const backHair = new three.Mesh(
        new three.SphereGeometry(headR * 1.12, 32, 32, 0, Math.PI * 2, 0, Math.PI * 0.6),
        hairMat
      )
      backHair.position.set(0, 0.05, -0.07)
      backHair.scale.set(1.04, 1.08, 1.02)
      backHair.castShadow = true
      hairGroup.add(backHair)
      addOutline(backHair, 1.03, '#2e2433', 0.85)
    }
    if (hairParts.includes('bangs')) {
      const bangs = new three.Mesh(
        new three.SphereGeometry(headR * 0.96, 32, 32, 0, Math.PI * 2, 0, Math.PI * 0.42),
        hairMat
      )
      bangs.position.set(0, 0.17, 0.05)
      hairGroup.add(bangs)
      // 刘海高光层
      const bangsHi = new three.Mesh(
        new three.SphereGeometry(headR * 0.94, 24, 24, 0, Math.PI * 2, 0, Math.PI * 0.24),
        hairHiMat
      )
      bangsHi.position.set(0, 0.15, 0.08)
      hairGroup.add(bangsHi)
      addOutline(bangs, 1.035, '#2e2433', 0.8)
    }
    if (hairParts.includes('side-l')) {
      const sl = new three.Mesh(new three.CapsuleGeometry(0.05, 0.34, 8, 14), hairMat)
      sl.position.set(-headR * 1.0, -0.12, 0.05)
      hairGroup.add(sl)
    }
    if (hairParts.includes('side-r')) {
      const sr = new three.Mesh(new three.CapsuleGeometry(0.05, 0.34, 8, 14), hairMat)
      sr.position.set(headR * 1.0, -0.12, 0.05)
      hairGroup.add(sr)
    }
    if (hairParts.includes('long')) {
      const longHair = new three.Mesh(
        new three.CapsuleGeometry(0.14, 0.68, 10, 16),
        hairMat
      )
      longHair.position.set(0, -0.3, -0.2)
      longHair.scale.set(1.12, 1, 0.55)
      longHair.castShadow = true
      hairGroup.add(longHair)
      addOutline(longHair, 1.03, '#2e2433', 0.8)
      const longHair2 = new three.Mesh(new three.CapsuleGeometry(0.065, 0.52, 8, 12), hairMat)
      longHair2.position.set(-0.13, -0.14, -0.18)
      longHair2.rotation.z = 0.08
      hairGroup.add(longHair2)
      const longHair3 = new three.Mesh(new three.CapsuleGeometry(0.065, 0.52, 8, 12), hairMat)
      longHair3.position.set(0.13, -0.14, -0.18)
      longHair3.rotation.z = -0.08
      hairGroup.add(longHair3)
    }
    if (hairParts.includes('twin-l')) {
      const tl = new three.Mesh(new three.CapsuleGeometry(0.07, 0.46, 8, 14), hairMat)
      tl.position.set(-headR * 1.35, -0.2, -0.06)
      tl.rotation.z = 0.3
      tl.castShadow = true
      hairGroup.add(tl)
      const tl2 = new three.Mesh(new three.CapsuleGeometry(0.045, 0.32, 8, 12), hairHiMat)
      tl2.position.set(-headR * 1.5, -0.5, -0.07)
      tl2.rotation.z = 0.52
      hairGroup.add(tl2)
    }
    if (hairParts.includes('twin-r')) {
      const tr = new three.Mesh(new three.CapsuleGeometry(0.07, 0.46, 8, 14), hairMat)
      tr.position.set(headR * 1.35, -0.2, -0.06)
      tr.rotation.z = -0.3
      tr.castShadow = true
      hairGroup.add(tr)
      const tr2 = new three.Mesh(new three.CapsuleGeometry(0.045, 0.32, 8, 12), hairHiMat)
      tr2.position.set(headR * 1.5, -0.5, -0.07)
      tr2.rotation.z = -0.52
      hairGroup.add(tr2)
    }
    if (hairParts.includes('ponytail')) {
      const pt = new three.Mesh(new three.CapsuleGeometry(0.08, 0.54, 8, 14), hairMat)
      pt.position.set(0, 0.55, -headR * 0.85)
      pt.rotation.x = 0.55
      pt.castShadow = true
      hairGroup.add(pt)
      const pt2 = new three.Mesh(new three.CapsuleGeometry(0.05, 0.34, 8, 12), hairHiMat)
      pt2.position.set(0, 0.85, -headR * 1.05)
      pt2.rotation.x = 0.82
      hairGroup.add(pt2)
    }
    if (hairParts.includes('curly-l')) {
      const cl = new three.Mesh(new three.SphereGeometry(0.14, 14, 14), hairMat)
      cl.position.set(-headR * 1.08, -0.06, 0.02)
      cl.castShadow = true
      hairGroup.add(cl)
      const cl2 = new three.Mesh(new three.SphereGeometry(0.085, 12, 12), hairHiMat)
      cl2.position.set(-headR * 1.24, -0.32, 0.06)
      hairGroup.add(cl2)
    }
    if (hairParts.includes('curly-r')) {
      const cr = new three.Mesh(new three.SphereGeometry(0.14, 14, 14), hairMat)
      cr.position.set(headR * 1.08, -0.06, 0.02)
      cr.castShadow = true
      hairGroup.add(cr)
      const cr2 = new three.Mesh(new three.SphereGeometry(0.085, 12, 12), hairHiMat)
      cr2.position.set(headR * 1.24, -0.32, 0.06)
      hairGroup.add(cr2)
    }

    // ─── 脖子（连接头与躯干，游戏建模必须有） ───
    const neck = new three.Mesh(new three.CylinderGeometry(0.075, 0.09, 0.16, 16), skinMat)
    neck.position.y = headY - headR * 1.05 - 0.08
    neck.castShadow = true
    group.add(neck)

    // ─── 身体（胸/腰/髋分段，S 曲线；服装由换装套装构建） ───
    const bodyGroup = new three.Group()
    group.add(bodyGroup)

    // 套装服装部件（上衣/下装/腰带/领结等，V4 换装小游戏级）
    const outfitParts = buildOutfit3D(set, {
      topMat,
      topTrimMat,
      bottomMat,
      accentMat,
      trimMat,
      sockMat,
      shoeMat
    }, bodyGroup, { shoulderY, hipY, headR })

    // ─── 四肢（两段式骨骼 + 手/脚细节，动画接口与 V2 一致） ───
    // 左臂：肩 pivot + 上臂（袖，取套装上衣色）+ 前臂（皮肤）+ 手
    const leftArm = new three.Group()
    leftArm.position.set(-0.28, shoulderY + 0.02, 0)
    const leftUpper = new three.Mesh(new three.CapsuleGeometry(0.055, 0.22, 8, 14), topMat)
    leftUpper.position.y = -0.13
    leftUpper.castShadow = true
    leftArm.add(leftUpper)
    const leftForearm = new three.Group()
    leftForearm.position.y = -0.29
    const leftLower = new three.Mesh(new three.CapsuleGeometry(0.045, 0.19, 8, 14), skinMat)
    leftLower.position.y = -0.1
    leftForearm.add(leftLower)
    const leftHand = new three.Mesh(new three.SphereGeometry(0.045, 12, 12), skinMat)
    leftHand.position.y = -0.2
    leftForearm.add(leftHand)
    leftArm.add(leftForearm)
    bodyGroup.add(leftArm)
    refs.leftArm = leftArm
    refs.leftForearm = leftForearm

    // 右臂
    const rightArm = new three.Group()
    rightArm.position.set(0.28, shoulderY + 0.02, 0)
    const rightUpper = new three.Mesh(new three.CapsuleGeometry(0.055, 0.22, 8, 14), topMat)
    rightUpper.position.y = -0.13
    rightUpper.castShadow = true
    rightArm.add(rightUpper)
    const rightForearm = new three.Group()
    rightForearm.position.y = -0.29
    const rightLower = new three.Mesh(new three.CapsuleGeometry(0.045, 0.19, 8, 14), skinMat)
    rightLower.position.y = -0.1
    rightForearm.add(rightLower)
    const rightHand = new three.Mesh(new three.SphereGeometry(0.045, 12, 12), skinMat)
    rightHand.position.y = -0.2
    rightForearm.add(rightHand)
    rightArm.add(rightForearm)
    bodyGroup.add(rightArm)
    refs.rightArm = rightArm
    refs.rightForearm = rightForearm

    // 袜子与鞋（按套装款式构建）
    const sockType = set.socks || 'long'
    const shoeType = set.shoes || 'leather'
    const sockH = sockType === 'long' ? 0.26 : sockType === 'short' ? 0.1 : 0

    // 左腿：髋 pivot + 大腿 + 小腿（袜）+ 鞋
    const leftLeg = new three.Group()
    leftLeg.position.set(-0.13, hipY, 0)
    const leftThigh = new three.Mesh(new three.CapsuleGeometry(0.07, 0.24, 8, 14), skinMat)
    leftThigh.position.y = -0.14
    leftThigh.castShadow = true
    leftLeg.add(leftThigh)
    const leftShin = new three.Group()
    leftShin.position.y = -0.31
    const leftShinMesh = new three.Mesh(new three.CapsuleGeometry(0.05, sockH, 8, 14), sockMat)
    leftShinMesh.position.y = -0.11
    leftShin.add(leftShinMesh)
    const leftShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    leftShoe.position.set(0, -0.25, 0.035)
    leftShoe.castShadow = true
    leftShin.add(leftShoe)
    leftLeg.add(leftShin)
    bodyGroup.add(leftLeg)
    refs.leftLeg = leftLeg

    // 右腿
    const rightLeg = new three.Group()
    rightLeg.position.set(0.13, hipY, 0)
    const rightThigh = new three.Mesh(new three.CapsuleGeometry(0.07, 0.24, 8, 14), skinMat)
    rightThigh.position.y = -0.14
    rightThigh.castShadow = true
    rightLeg.add(rightThigh)
    const rightShin = new three.Group()
    rightShin.position.y = -0.31
    const rightShinMesh = new three.Mesh(new three.CapsuleGeometry(0.05, sockH, 8, 14), sockMat)
    rightShinMesh.position.y = -0.11
    rightShin.add(rightShinMesh)
    const rightShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    rightShoe.position.set(0, -0.25, 0.035)
    rightShoe.castShadow = true
    rightShin.add(rightShoe)
    rightLeg.add(rightShin)
    bodyGroup.add(rightLeg)
    refs.rightLeg = rightLeg

    refs.body = bodyGroup
    refs.head = headGroup

    // 整体：脚底踩 y=0（世界地面 -1.2 由 group.position.y 对齐）
    group.scale.set(0.92, 0.92, 0.92)
    group.position.y = -1.2
    const shadowChecks = [face, outfitParts.chest, outfitParts.skirt]
    group.traverse((node) => {
      if (node.isMesh) {
        node.castShadow = true
        node.receiveShadow = shadowChecks.includes(node)
      }
    })

    limbRefs = refs
    return group
  }

  /**
   * 游戏级 3D 人物建模 V4：美型少女（1:7 头身，闪耀暖暖式）。
   * 精致五官 + 多层长发 + S 曲线身体 + 细长四肢 + 换装部件（上衣/裙/袜/鞋/发饰）。
   * limbRefs 结构与 V3 一致（head/leftArm/rightArm/leftForearm/rightForearm/leftLeg/rightLeg/body/eyes），
   * 动作系统（getAction3DTransform/getAnimeElfLimbAction）完全兼容。
   */
  function buildAnimeElf3DV4(character, palette, outfitSet) {
    if (!three || !scene) return null
    const pal = getAnimeElf3DPalette(palette || (character && character.palette))
    const hairParts = getAnimeElfHairPartDescriptors(character && character.hairstyle)
    const set = resolveOutfitSet(outfitSet)

    const group = new three.Group()
    const refs = {}

    // ─── 材质（三色调卡通 + 美型） ───
    const skinMat = toonMatV3(pal.skin)
    const hairMat = toonMatV3(pal.hair)
    const hairHiMat = toonMatV3(pal.hairHighlight || '#ffffff', { transparent: true, opacity: 0.35 })
    const topMat = toonMatV3(set.colors.top)
    const topTrimMat = toonMatV3(set.colors.topTrim || set.colors.top)
    const bottomMat = toonMatV3(set.colors.bottom)
    const accentMat = toonMatV3(set.colors.accent)
    const trimMat = toonMatV3(set.colors.trim || '#ffffff')
    const sockMat = toonMatV3(set.colors.socks || '#f6f8fc')
    const shoeMat = toonMatV3(set.colors.shoes)
    const accessoryMat = toonMatV3(set.colors.accessory || set.colors.accent)
    const blushMat = toonMatV3(pal.blush, { transparent: true, opacity: 0.6 })
    const darkMat = toonMatV3('#33334a')

    // ─── 美型比例（1:7 头身；脚底 y=0） ───
    const headR = 0.30
    const headY = 1.80
    const shoulderY = 1.42
    const chestY = 1.20
    const waistY = 0.90
    const hipY = 0.64
    // 传给服装构建的美型参数（胸围更细、裙摆收窄）
    const body = { shoulderY, hipY, headR, chestR: 0.15, skirtScale: 0.72 }

    // ─── 头部 ───
    const headGroup = new three.Group()
    headGroup.position.y = headY
    group.add(headGroup)

    // 脸（瓜子脸：纵长微收）
    const face = new three.Mesh(new three.SphereGeometry(headR, 48, 48), skinMat)
    face.scale.set(1, 1.1, 0.94)
    face.castShadow = true
    headGroup.add(face)
    addOutline(face, 1.032, '#2e2433', 0.9)

    // 耳朵
    const earGeo = new three.SphereGeometry(0.055, 14, 14)
    const earL = new three.Mesh(earGeo, skinMat)
    earL.position.set(-headR * 1.0, -0.05, 0)
    earL.scale.set(0.55, 0.85, 0.55)
    headGroup.add(earL)
    const earR = new three.Mesh(earGeo, skinMat)
    earR.position.set(headR * 1.0, -0.05, 0)
    earR.scale.set(0.55, 0.85, 0.55)
    headGroup.add(earR)

    // 眼睛：凸起眼球（精致虹膜贴图）+ 独立高光
    const eyeTex = createAnimeEyeTexture(pal)
    const eyeGeo = new three.SphereGeometry(0.078, 20, 20)
    const eyeMatBase = eyeTex
      ? new three.MeshBasicMaterial({ map: eyeTex, transparent: true, alphaTest: 0.02 })
      : eyesMatV3(pal)
    const leftEye = new three.Mesh(eyeGeo, eyeMatBase)
    leftEye.position.set(-0.13, 0.05, headR * 0.88)
    leftEye.scale.set(1, 1.34, 0.38)
    headGroup.add(leftEye)
    const rightEye = new three.Mesh(eyeGeo, eyeMatBase)
    rightEye.position.set(0.13, 0.05, headR * 0.88)
    rightEye.scale.set(1, 1.34, 0.38)
    headGroup.add(rightEye)
    refs.eyes = [leftEye, rightEye]
    // 玻璃高光
    const hlGeo = new three.SphereGeometry(0.022, 10, 10)
    const hlL = new three.Mesh(hlGeo, toonMatV3('#ffffff', { transparent: true, opacity: 0.95 }))
    hlL.position.set(-0.105, 0.095, headR * 1.02)
    headGroup.add(hlL)
    const hlR = new three.Mesh(hlGeo, toonMatV3('#ffffff', { transparent: true, opacity: 0.95 }))
    hlR.position.set(0.152, 0.095, headR * 1.02)
    headGroup.add(hlR)

    // 眉毛（细长微挑）
    const browGeo = new three.BoxGeometry(0.1, 0.018, 0.025)
    const browMat = toonMatV3(pal.brow)
    const browL = new three.Mesh(browGeo, browMat)
    browL.position.set(-0.13, 0.155, headR * 0.88)
    browL.rotation.z = 0.08
    headGroup.add(browL)
    const browR = new three.Mesh(browGeo, browMat)
    browR.position.set(0.13, 0.155, headR * 0.88)
    browR.rotation.z = -0.08
    headGroup.add(browR)

    // 鼻子（小巧立体）
    const nose = new three.Mesh(new three.ConeGeometry(0.018, 0.03, 6), skinMat)
    nose.position.set(0, -0.02, headR * 0.95)
    nose.rotation.x = -0.5
    headGroup.add(nose)

    // 嘴（微笑弧）
    const mouth = new three.Mesh(new three.TorusGeometry(0.045, 0.008, 6, 16, Math.PI), toonMatV3('#d9706e'))
    mouth.position.set(0, -0.13, headR * 0.9)
    mouth.rotation.z = Math.PI
    mouth.scale.set(1, 1, 0.4)
    headGroup.add(mouth)

    // 腮红
    const blushGeo = new three.SphereGeometry(0.045, 12, 12)
    const blushL = new three.Mesh(blushGeo, blushMat)
    blushL.position.set(-0.2, -0.05, headR * 0.84)
    blushL.scale.set(1, 0.55, 0.35)
    headGroup.add(blushL)
    const blushR = new three.Mesh(blushGeo, blushMat)
    blushR.position.set(0.2, -0.05, headR * 0.84)
    blushR.scale.set(1, 0.55, 0.35)
    headGroup.add(blushR)

    // ─── 头发（美型多层长发：后发 + 长发及腰 + 刘海 + 侧发 + 发型特色） ───
    const hairGroup = new three.Group()
    headGroup.add(hairGroup)

    // 后发（包裹后脑）
    const backHair = new three.Mesh(
      new three.SphereGeometry(headR * 1.08, 32, 32, 0, Math.PI * 2, 0, Math.PI * 0.62),
      hairMat
    )
    backHair.position.set(0, 0.06, -0.05)
    backHair.scale.set(1.03, 1.08, 1.0)
    backHair.castShadow = true
    hairGroup.add(backHair)
    addOutline(backHair, 1.03, '#2e2433', 0.85)

    // 长发及腰（美型核心）
    const longBack = new three.Mesh(new three.CapsuleGeometry(0.12, 0.8, 10, 16), hairMat)
    longBack.position.set(0, -0.32, -0.15)
    longBack.scale.set(1.25, 1, 0.55)
    longBack.castShadow = true
    hairGroup.add(longBack)
    addOutline(longBack, 1.03, '#2e2433', 0.8)
    // 长发分层（左右发束）
    const longL = new three.Mesh(new three.CapsuleGeometry(0.05, 0.5, 8, 12), hairMat)
    longL.position.set(-0.12, -0.16, -0.14)
    longL.rotation.z = 0.12
    hairGroup.add(longL)
    const longR = new three.Mesh(new three.CapsuleGeometry(0.05, 0.5, 8, 12), hairMat)
    longR.position.set(0.12, -0.16, -0.14)
    longR.rotation.z = -0.12
    hairGroup.add(longR)

    // 刘海（前额片 + 高光层）
    const bangs = new three.Mesh(
      new three.SphereGeometry(headR * 0.94, 32, 32, 0, Math.PI * 2, 0, Math.PI * 0.4),
      hairMat
    )
    bangs.position.set(0, 0.17, 0.06)
    hairGroup.add(bangs)
    const bangsHi = new three.Mesh(
      new three.SphereGeometry(headR * 0.92, 24, 24, 0, Math.PI * 2, 0, Math.PI * 0.22),
      hairHiMat
    )
    bangsHi.position.set(0, 0.15, 0.09)
    hairGroup.add(bangsHi)
    addOutline(bangs, 1.03, '#2e2433', 0.8)

    // 侧发（垂到胸前）
    const sideL = new three.Mesh(new three.CapsuleGeometry(0.045, 0.42, 8, 12), hairMat)
    sideL.position.set(-headR * 1.02, -0.12, 0.04)
    sideL.rotation.z = 0.1
    hairGroup.add(sideL)
    const sideR = new three.Mesh(new three.CapsuleGeometry(0.045, 0.42, 8, 12), hairMat)
    sideR.position.set(headR * 1.02, -0.12, 0.04)
    sideR.rotation.z = -0.1
    hairGroup.add(sideR)

    // 发型特色（双马尾/马尾/卷发）
    if (hairParts.includes('twin-l')) {
      const tl = new three.Mesh(new three.CapsuleGeometry(0.06, 0.5, 8, 14), hairMat)
      tl.position.set(-headR * 1.3, -0.18, -0.04)
      tl.rotation.z = 0.32
      tl.castShadow = true
      hairGroup.add(tl)
      const tl2 = new three.Mesh(new three.CapsuleGeometry(0.038, 0.34, 8, 12), hairHiMat)
      tl2.position.set(-headR * 1.5, -0.5, -0.05)
      tl2.rotation.z = 0.55
      hairGroup.add(tl2)
    }
    if (hairParts.includes('twin-r')) {
      const tr = new three.Mesh(new three.CapsuleGeometry(0.06, 0.5, 8, 14), hairMat)
      tr.position.set(headR * 1.3, -0.18, -0.04)
      tr.rotation.z = -0.32
      tr.castShadow = true
      hairGroup.add(tr)
      const tr2 = new three.Mesh(new three.CapsuleGeometry(0.038, 0.34, 8, 12), hairHiMat)
      tr2.position.set(headR * 1.5, -0.5, -0.05)
      tr2.rotation.z = -0.55
      hairGroup.add(tr2)
    }
    if (hairParts.includes('ponytail')) {
      const pt = new three.Mesh(new three.CapsuleGeometry(0.07, 0.6, 8, 14), hairMat)
      pt.position.set(0, 0.6, -headR * 0.8)
      pt.rotation.x = 0.5
      pt.castShadow = true
      hairGroup.add(pt)
      const pt2 = new three.Mesh(new three.CapsuleGeometry(0.042, 0.36, 8, 12), hairHiMat)
      pt2.position.set(0, 0.9, -headR * 1.0)
      pt2.rotation.x = 0.78
      hairGroup.add(pt2)
    }
    if (hairParts.includes('curly-l')) {
      const cl = new three.Mesh(new three.SphereGeometry(0.12, 14, 14), hairMat)
      cl.position.set(-headR * 1.05, -0.04, 0.02)
      cl.castShadow = true
      hairGroup.add(cl)
      const cl2 = new three.Mesh(new three.SphereGeometry(0.07, 12, 12), hairHiMat)
      cl2.position.set(-headR * 1.2, -0.3, 0.06)
      hairGroup.add(cl2)
    }
    if (hairParts.includes('curly-r')) {
      const cr = new three.Mesh(new three.SphereGeometry(0.12, 14, 14), hairMat)
      cr.position.set(headR * 1.05, -0.04, 0.02)
      cr.castShadow = true
      hairGroup.add(cr)
      const cr2 = new three.Mesh(new three.SphereGeometry(0.07, 12, 12), hairHiMat)
      cr2.position.set(headR * 1.2, -0.3, 0.06)
      hairGroup.add(cr2)
    }

    // 发饰（换装部件，随头动）
    buildAccessory3D(set.accessory, { accessoryMat, trimMat, accentMat }, headGroup, headR)

    // ─── 脖子 ───
    const neck = new three.Mesh(new three.CylinderGeometry(0.06, 0.075, 0.12, 16), skinMat)
    neck.position.y = headY - headR * 1.05 - 0.06
    neck.castShadow = true
    group.add(neck)

    // ─── 身体（S 曲线：胸/腰/髋分段，美型细腰长腿） ───
    const bodyGroup = new three.Group()
    group.add(bodyGroup)

    // 胸（合体微翘）
    const chestMesh = new three.Mesh(new three.CapsuleGeometry(0.15, 0.1, 10, 20), skinMat)
    chestMesh.position.y = chestY
    chestMesh.scale.set(1.35, 1, 0.8)
    chestMesh.castShadow = true
    bodyGroup.add(chestMesh)
    // 腰（细腰）
    const waistMesh = new three.Mesh(new three.CylinderGeometry(0.09, 0.12, 0.18, 18), skinMat)
    waistMesh.position.y = waistY
    bodyGroup.add(waistMesh)
    // 髋（圆润）
    const hipMesh = new three.Mesh(new three.SphereGeometry(0.15, 20, 20), skinMat)
    hipMesh.position.set(0, hipY + 0.02, 0)
    hipMesh.scale.set(1.15, 0.85, 0.9)
    bodyGroup.add(hipMesh)

    // 服装部件（美型参数：细胸围、收裙摆）
    const outfitParts = buildOutfit3D(set, {
      topMat,
      topTrimMat,
      bottomMat,
      accentMat,
      trimMat,
      sockMat,
      shoeMat
    }, bodyGroup, body)

    // ─── 四肢（美型细长，两段式骨骼） ───
    // 左臂
    const leftArm = new three.Group()
    leftArm.position.set(-0.27, shoulderY + 0.02, 0)
    const leftUpper = new three.Mesh(new three.CapsuleGeometry(0.042, 0.22, 8, 14), topMat)
    leftUpper.position.y = -0.13
    leftUpper.castShadow = true
    leftArm.add(leftUpper)
    const leftForearm = new three.Group()
    leftForearm.position.y = -0.28
    const leftLower = new three.Mesh(new three.CapsuleGeometry(0.034, 0.2, 8, 14), skinMat)
    leftLower.position.y = -0.1
    leftForearm.add(leftLower)
    const leftHand = new three.Mesh(new three.SphereGeometry(0.038, 12, 12), skinMat)
    leftHand.position.y = -0.21
    leftForearm.add(leftHand)
    leftArm.add(leftForearm)
    bodyGroup.add(leftArm)
    refs.leftArm = leftArm
    refs.leftForearm = leftForearm

    // 右臂
    const rightArm = new three.Group()
    rightArm.position.set(0.27, shoulderY + 0.02, 0)
    const rightUpper = new three.Mesh(new three.CapsuleGeometry(0.042, 0.22, 8, 14), topMat)
    rightUpper.position.y = -0.13
    rightUpper.castShadow = true
    rightArm.add(rightUpper)
    const rightForearm = new three.Group()
    rightForearm.position.y = -0.28
    const rightLower = new three.Mesh(new three.CapsuleGeometry(0.034, 0.2, 8, 14), skinMat)
    rightLower.position.y = -0.1
    rightForearm.add(rightLower)
    const rightHand = new three.Mesh(new three.SphereGeometry(0.038, 12, 12), skinMat)
    rightHand.position.y = -0.21
    rightForearm.add(rightHand)
    rightArm.add(rightForearm)
    bodyGroup.add(rightArm)
    refs.rightArm = rightArm
    refs.rightForearm = rightForearm

    // 袜与鞋
    const sockType = set.socks || 'long'
    const shoeType = set.shoes || 'leather'
    const sockH = sockType === 'long' ? 0.34 : sockType === 'short' ? 0.12 : 0

    // 左腿（长腿：髋→膝→踝→脚底）
    const leftLeg = new three.Group()
    leftLeg.position.set(-0.12, hipY, 0)
    const leftThigh = new three.Mesh(new three.CapsuleGeometry(0.05, 0.3, 8, 14), skinMat)
    leftThigh.position.y = -0.17
    leftThigh.castShadow = true
    leftLeg.add(leftThigh)
    const leftShin = new three.Group()
    leftShin.position.y = -0.36
    const leftShinMesh = new three.Mesh(new three.CapsuleGeometry(0.038, sockH, 8, 14), sockMat)
    leftShinMesh.position.y = -0.12
    leftShin.add(leftShinMesh)
    const leftShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    leftShoe.position.set(0, -0.27, 0.035)
    leftShoe.castShadow = true
    leftShin.add(leftShoe)
    leftLeg.add(leftShin)
    bodyGroup.add(leftLeg)
    refs.leftLeg = leftLeg

    // 右腿
    const rightLeg = new three.Group()
    rightLeg.position.set(0.12, hipY, 0)
    const rightThigh = new three.Mesh(new three.CapsuleGeometry(0.05, 0.3, 8, 14), skinMat)
    rightThigh.position.y = -0.17
    rightThigh.castShadow = true
    rightLeg.add(rightThigh)
    const rightShin = new three.Group()
    rightShin.position.y = -0.36
    const rightShinMesh = new three.Mesh(new three.CapsuleGeometry(0.038, sockH, 8, 14), sockMat)
    rightShinMesh.position.y = -0.12
    rightShin.add(rightShinMesh)
    const rightShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    rightShoe.position.set(0, -0.27, 0.035)
    rightShoe.castShadow = true
    rightShin.add(rightShoe)
    rightLeg.add(rightShin)
    bodyGroup.add(rightLeg)
    refs.rightLeg = rightLeg

    refs.body = bodyGroup
    refs.head = headGroup

    // 整体：脚底踩 y=0（世界地面 -1.2 由 group.position.y 对齐）
    group.scale.set(0.92, 0.92, 0.92)
    group.position.y = -1.2
    const shadowChecks = [face, outfitParts.chest, outfitParts.skirt]
    group.traverse((node) => {
      if (node.isMesh) {
        node.castShadow = true
        node.receiveShadow = shadowChecks.includes(node)
      }
    })

    limbRefs = refs
    return group
  }

  /**
   * 真人感游戏角色建模 V5（stylized-realistic：真实皮肤/发丝材质 + 精细五官 + 自然身体比例）。
   * 在 V4 美型少女基础上全面升级材质与细节，保持 1:7 头身与换装兼容；
   * 去掉卡通描边，用物理材质明暗体现立体与真实感。
   * @returns {THREE.Group|null}
   */
  function buildRealElf3DV5(character, palette, outfitSet) {
    if (!three || !scene) return null
    const pal = getAnimeElf3DPalette(palette || (character && character.palette))
    const hairParts = getAnimeElfHairPartDescriptors(character && character.hairstyle)
    const set = resolveOutfitSet(outfitSet)

    const group = new three.Group()
    const refs = {}

    // ─── 真人感材质（物理材质：皮肤清漆 / 发丝高光 / 面料差异） ───
    const skinMat = realSkinMat(pal.skin)
    const hairMat = realHairMat(pal.hair)
    const hairHiMat = realHairMat(pal.hairHighlight || '#ffffff', { transparent: true, opacity: 0.3 })
    const topMat = realClothMat(set.colors.top)
    const topTrimMat = realClothMat(set.colors.topTrim || set.colors.top)
    const bottomMat = realClothMat(set.colors.bottom)
    const accentMat = realClothMat(set.colors.accent, { clearcoat: 0.35, roughness: 0.6 })
    const trimMat = realClothMat(set.colors.trim || '#ffffff')
    const sockMat = realClothMat(set.colors.socks || '#f6f8fc', { roughness: 0.9 })
    const shoeMat = realClothMat(set.colors.shoes, { clearcoat: 0.5, roughness: 0.35 })
    const accessoryMat = realClothMat(set.colors.accessory || set.colors.accent, { clearcoat: 0.4, roughness: 0.45 })
    // 玉质材质（古风玉簪/玉坠：高光清漆 + 低粗糙，温润玉泽）
    const jadeMat = realClothMat(set.colors.trim || '#dff0e8', { clearcoat: 0.95, roughness: 0.12 })
    const blushMat = realSkinMat(pal.blush, { transparent: true, opacity: 0.28 })
    const darkMat = realClothMat('#33334a', { roughness: 0.5 })
    const eyeWhiteMat = realSkinMat('#ffffff', { roughness: 0.12, clearcoat: 0.5 })
    const lipMat = realSkinMat('#e48a8d', { roughness: 0.42, clearcoat: 0.5 })
    const lipLineMat = realSkinMat('#b95a5e', { roughness: 0.5 })
    const lashMat = realClothMat('#2a2330', { roughness: 0.4 })

    // ─── 美型真人比例（1:7 头身；脚底 y=0） ───
    const headR = 0.30
    const headY = 1.80
    const shoulderY = 1.42
    const chestY = 1.20
    const waistY = 0.90
    const hipY = 0.64
    const body = { shoulderY, hipY, headR, chestR: 0.15, skirtScale: 0.72 }

    // ─── 头部 ───
    const headGroup = new three.Group()
    headGroup.position.y = headY
    group.add(headGroup)

    // 脸（瓜子脸：纵长微收，高细分清晰，无卡通描边，靠光照立体）
    const face = new three.Mesh(new three.SphereGeometry(headR, 72, 72), skinMat)
    face.scale.set(1, 1.08, 0.92)
    face.castShadow = true
    headGroup.add(face)

    // 耳朵（真实耳廓：外耳 + 内凹）
    const earL = buildRealEar(skinMat, three)
    earL.position.set(-headR * 1.02, -0.06, 0)
    headGroup.add(earL)
    const earR = buildRealEar(skinMat, three)
    earR.position.set(headR * 1.02, -0.06, 0)
    headGroup.add(earR)

    // ─── 眼睛（真人感：眼白 + 精致虹膜 + 睫毛 + 上眼睑线） ───
    const eyeTex = createAnimeEyeTexture(pal)
    const eyeWhiteGeo = new three.SphereGeometry(0.077, 28, 28)
    const buildEye = (side) => {
      const eyeGroup = new three.Group()
      // 眼白（微凸）
      const white = new three.Mesh(eyeWhiteGeo, eyeWhiteMat)
      white.scale.set(1.3, 1.06, 0.46)
      eyeGroup.add(white)
      // 虹膜盘（CanvasTexture：虹膜+瞳孔+双层高光+上下眼睑影）
      if (eyeTex) {
        const iris = new three.Mesh(
          new three.CircleGeometry(0.052, 28),
          new three.MeshBasicMaterial({ map: eyeTex, transparent: true, alphaTest: 0.02, depthWrite: false })
        )
        iris.position.set(0, 0.004, 0.024)
        iris.rotation.y = Math.PI
        eyeGroup.add(iris)
      } else {
        const iris = new three.Mesh(new three.SphereGeometry(0.05, 20, 20), eyesMatV3(pal))
        iris.position.set(0, 0, 0.022)
        iris.scale.set(1, 1.25, 0.3)
        eyeGroup.add(iris)
      }
      // 玻璃高光（双层）
      const hl1 = new three.Mesh(new three.SphereGeometry(0.02, 10, 10), realClothMat('#ffffff', { transparent: true, opacity: 0.95, clearcoat: 1, roughness: 0.1 }))
      hl1.position.set(-0.024, 0.032, 0.05)
      eyeGroup.add(hl1)
      const hl2 = new three.Mesh(new three.SphereGeometry(0.009, 8, 8), realClothMat('#ffffff', { transparent: true, opacity: 0.75, clearcoat: 1, roughness: 0.1 }))
      hl2.position.set(0.026, 0.012, 0.05)
      eyeGroup.add(hl2)
      // 上眼睑线（眼线，盖住眼球上缘）
      const lid = new three.Mesh(new three.SphereGeometry(0.078, 24, 24, 0, Math.PI * 2, 0, Math.PI * 0.42), lashMat)
      lid.position.set(0, 0.028, 0.02)
      lid.scale.set(1.32, 0.9, 0.44)
      eyeGroup.add(lid)
      // 睫毛（上睫毛 3 根，向外展开）
      const lashAngles = [-0.62, -0.32, 0, 0.32, 0.62]
      lashAngles.forEach((a, i) => {
        const lash = new three.Mesh(new three.ConeGeometry(0.007, 0.05, 6), lashMat)
        lash.position.set(Math.sin(a) * 0.062, 0.055, 0.022)
        lash.rotation.z = a * 0.85
        lash.rotation.x = -0.9
        eyeGroup.add(lash)
      })
      eyeGroup.position.set(side * 0.13, 0.05, headR * 0.86)
      headGroup.add(eyeGroup)
      return eyeGroup
    }
    const leftEye = buildEye(-1)
    const rightEye = buildEye(1)
    refs.eyes = [leftEye, rightEye]

    // 眉毛（细长微弯，真实眉形）
    const buildBrow = (side) => {
      const brow = new three.Mesh(
        new three.TorusGeometry(0.088, 0.009, 8, 20, Math.PI * 0.62),
        realHairMat(pal.brow, { roughness: 0.6, clearcoat: 0.2 })
      )
      brow.position.set(side * 0.135, 0.168, headR * 0.85)
      brow.rotation.y = side * 0.18
      brow.rotation.x = Math.PI * 0.5
      headGroup.add(brow)
      return brow
    }
    buildBrow(-1)
    buildBrow(1)

    // 鼻子（鼻梁 + 鼻尖 + 鼻翼，立体真实）
    const noseBridge = new three.Mesh(new three.CylinderGeometry(0.012, 0.017, 0.045, 10), skinMat)
    noseBridge.position.set(0, 0.005, headR * 0.93)
    noseBridge.rotation.x = 0.12
    headGroup.add(noseBridge)
    const noseTip = new three.Mesh(new three.SphereGeometry(0.016, 12, 12), skinMat)
    noseTip.position.set(0, -0.02, headR * 0.98)
    headGroup.add(noseTip)
    const noseWingL = new three.Mesh(new three.SphereGeometry(0.011, 10, 10), skinMat)
    noseWingL.position.set(-0.024, -0.026, headR * 0.94)
    headGroup.add(noseWingL)
    const noseWingR = new three.Mesh(new three.SphereGeometry(0.011, 10, 10), skinMat)
    noseWingR.position.set(0.024, -0.026, headR * 0.94)
    headGroup.add(noseWingR)

    // 嘴（真实唇形：上唇薄 + 下唇饱满 + 唇线，微启）
    const upperLip = new three.Mesh(new three.SphereGeometry(0.038, 16, 12, 0, Math.PI * 2, 0, Math.PI * 0.5), lipMat)
    upperLip.position.set(0, -0.1, headR * 0.9)
    upperLip.scale.set(1, 0.5, 0.7)
    headGroup.add(upperLip)
    const lowerLip = new three.Mesh(new three.SphereGeometry(0.042, 16, 12, 0, Math.PI * 2, Math.PI * 0.5, Math.PI * 0.5), lipMat)
    lowerLip.position.set(0, -0.142, headR * 0.88)
    lowerLip.scale.set(1, 0.72, 0.78)
    headGroup.add(lowerLip)
    // 唇线（上下唇交界深色线）
    const lipLine = new three.Mesh(new three.TorusGeometry(0.034, 0.0045, 6, 16, Math.PI), lipLineMat)
    lipLine.position.set(0, -0.112, headR * 0.9)
    lipLine.rotation.z = Math.PI
    lipLine.scale.set(1, 1, 0.5)
    headGroup.add(lipLine)

    // 腮红（淡雅自然）
    const blushGeo = new three.SphereGeometry(0.05, 14, 14)
    const blushL = new three.Mesh(blushGeo, blushMat)
    blushL.position.set(-0.21, -0.06, headR * 0.8)
    blushL.scale.set(1, 0.5, 0.3)
    headGroup.add(blushL)
    const blushR = new three.Mesh(blushGeo, blushMat)
    blushR.position.set(0.21, -0.06, headR * 0.8)
    blushR.scale.set(1, 0.5, 0.3)
    headGroup.add(blushR)

    // ─── 头发（真人发丝质感：多层 + 发尾尖 + 高光层） ───
    const hairGroup = new three.Group()
    headGroup.add(hairGroup)

    // 后发（包裹后脑，发丝分层）
    const backHair = new three.Mesh(
      new three.SphereGeometry(headR * 1.09, 40, 40, 0, Math.PI * 2, 0, Math.PI * 0.66),
      hairMat
    )
    backHair.position.set(0, 0.07, -0.04)
    backHair.scale.set(1.04, 1.1, 1.0)
    backHair.castShadow = true
    hairGroup.add(backHair)

    // 长发主体（三束：中心 + 左右，发尾收尖）
    const mainHair = new three.Mesh(new three.CapsuleGeometry(0.115, 0.82, 12, 20), hairMat)
    mainHair.position.set(0, -0.3, -0.16)
    mainHair.scale.set(1.28, 1, 0.55)
    mainHair.castShadow = true
    hairGroup.add(mainHair)
    // 发尾尖
    const tailTip = new three.Mesh(new three.ConeGeometry(0.09, 0.16, 14), hairMat)
    tailTip.position.set(0, -0.82, -0.17)
    hairGroup.add(tailTip)
    // 左右发束（垂至胸前）
    const strandGeo = new three.CapsuleGeometry(0.042, 0.55, 10, 16)
    const strandL = new three.Mesh(strandGeo, hairMat)
    strandL.position.set(-0.14, -0.2, -0.13)
    strandL.rotation.z = 0.14
    hairGroup.add(strandL)
    const strandR = new three.Mesh(strandGeo, hairMat)
    strandR.position.set(0.14, -0.2, -0.13)
    strandR.rotation.z = -0.14
    hairGroup.add(strandR)

    // 前侧发（垂在脸侧，真实层次）
    const frontL = new three.Mesh(new three.CapsuleGeometry(0.035, 0.4, 10, 16), hairMat)
    frontL.position.set(-headR * 1.0, -0.14, 0.05)
    frontL.rotation.z = 0.12
    hairGroup.add(frontL)
    const frontR = new three.Mesh(new three.CapsuleGeometry(0.035, 0.4, 10, 16), hairMat)
    frontR.position.set(headR * 1.0, -0.14, 0.05)
    frontR.rotation.z = -0.12
    hairGroup.add(frontR)

    // 刘海（多层错落，真人自然感）
    const bangs1 = new three.Mesh(
      new three.SphereGeometry(headR * 0.95, 32, 32, 0, Math.PI * 2, 0, Math.PI * 0.42),
      hairMat
    )
    bangs1.position.set(0, 0.19, 0.05)
    hairGroup.add(bangs1)
    const bangs2 = new three.Mesh(
      new three.SphereGeometry(headR * 0.9, 28, 28, Math.PI * 0.55, Math.PI * 0.8, 0, Math.PI * 0.32),
      hairMat
    )
    bangs2.position.set(-0.03, 0.1, 0.14)
    bangs2.rotation.y = 0.35
    hairGroup.add(bangs2)
    // 刘海高光层
    const bangsHi = new three.Mesh(
      new three.SphereGeometry(headR * 0.92, 28, 28, 0, Math.PI * 2, 0, Math.PI * 0.2),
      hairHiMat
    )
    bangsHi.position.set(0, 0.17, 0.1)
    hairGroup.add(bangsHi)

    // 古风盘发（gufeng-bun）：发髻 + 收发包 + 玉簪 + 玉耳坠（参考图古典盘发美人）
    if (hairParts.includes('bun')) {
      // 后脑发髻（扁圆髻，头顶偏后）
      const bun = new three.Mesh(new three.SphereGeometry(headR * 0.42, 24, 24), hairMat)
      bun.position.set(0, 0.45, -0.18)
      bun.scale.set(1.18, 0.82, 1.05)
      bun.castShadow = true
      hairGroup.add(bun)
      // 发髻根部收发包（环形，衔接后发）
      const bunBase = new three.Mesh(new three.TorusGeometry(headR * 0.34, headR * 0.1, 10, 24), hairMat)
      bunBase.position.set(0, 0.34, -0.18)
      bunBase.rotation.x = Math.PI / 2
      hairGroup.add(bunBase)
      // 玉簪（斜插发髻，簪头玉珠）
      const pin = new three.Mesh(new three.CylinderGeometry(0.007, 0.011, 0.3, 8), jadeMat)
      pin.position.set(-0.07, 0.4, -0.16)
      pin.rotation.z = 0.55
      hairGroup.add(pin)
      const pinHead = new three.Mesh(new three.SphereGeometry(0.028, 12, 12), jadeMat)
      pinHead.position.set(-0.19, 0.51, -0.2)
      hairGroup.add(pinHead)
      // 玉簪流苏（珠链轻垂）
      const tasselGeo = new three.SphereGeometry(0.011, 8, 8)
      for (let i = 0; i < 3; i++) {
        const bead = new three.Mesh(tasselGeo, jadeMat)
        bead.position.set(-0.26, 0.42 - i * 0.045, -0.22)
        hairGroup.add(bead)
      }
      // 玉耳坠（双耳垂珠，古风玉质配饰）
      const earringGeo = new three.CylinderGeometry(0.006, 0.006, 0.05, 6)
      const dropGeo = new three.SphereGeometry(0.024, 12, 12)
      ;[-1, 1].forEach((side) => {
        const chain = new three.Mesh(earringGeo, jadeMat)
        chain.position.set(side * headR * 1.05, -0.15, 0.03)
        hairGroup.add(chain)
        const drop = new three.Mesh(dropGeo, jadeMat)
        drop.position.set(side * headR * 1.05, -0.21, 0.035)
        hairGroup.add(drop)
      })
    }

    // 发型特色（双马尾/马尾/卷发）
    if (hairParts.includes('twin-l')) {
      const tl = new three.Mesh(new three.CapsuleGeometry(0.055, 0.52, 10, 16), hairMat)
      tl.position.set(-headR * 1.32, -0.16, -0.02)
      tl.rotation.z = 0.34
      tl.castShadow = true
      hairGroup.add(tl)
      const tlTip = new three.Mesh(new three.ConeGeometry(0.045, 0.14, 10), hairMat)
      tlTip.position.set(-headR * 1.62, -0.56, -0.06)
      tlTip.rotation.z = 0.52
      hairGroup.add(tlTip)
    }
    if (hairParts.includes('twin-r')) {
      const tr = new three.Mesh(new three.CapsuleGeometry(0.055, 0.52, 10, 16), hairMat)
      tr.position.set(headR * 1.32, -0.16, -0.02)
      tr.rotation.z = -0.34
      tr.castShadow = true
      hairGroup.add(tr)
      const trTip = new three.Mesh(new three.ConeGeometry(0.045, 0.14, 10), hairMat)
      trTip.position.set(headR * 1.62, -0.56, -0.06)
      trTip.rotation.z = -0.52
      hairGroup.add(trTip)
    }
    if (hairParts.includes('ponytail')) {
      const pt = new three.Mesh(new three.CapsuleGeometry(0.065, 0.62, 10, 16), hairMat)
      pt.position.set(0, 0.62, -headR * 0.82)
      pt.rotation.x = 0.52
      pt.castShadow = true
      hairGroup.add(pt)
      const ptTip = new three.Mesh(new three.ConeGeometry(0.05, 0.16, 10), hairMat)
      ptTip.position.set(0, 0.98, -headR * 1.05)
      ptTip.rotation.x = 0.72
      hairGroup.add(ptTip)
    }
    if (hairParts.includes('curly-l')) {
      const cl = new three.Mesh(new three.SphereGeometry(0.115, 16, 16), hairMat)
      cl.position.set(-headR * 1.06, -0.06, 0.04)
      cl.castShadow = true
      hairGroup.add(cl)
    }
    if (hairParts.includes('curly-r')) {
      const cr = new three.Mesh(new three.SphereGeometry(0.115, 16, 16), hairMat)
      cr.position.set(headR * 1.06, -0.06, 0.04)
      cr.castShadow = true
      hairGroup.add(cr)
    }

    // 发饰（换装部件，随头动）
    buildAccessory3D(set.accessory, { accessoryMat, trimMat, accentMat }, headGroup, headR)

    // ─── 脖子（细长自然） ───
    const neck = new three.Mesh(new three.CylinderGeometry(0.055, 0.07, 0.15, 20), skinMat)
    neck.position.y = headY - headR * 1.08 - 0.07
    neck.castShadow = true
    group.add(neck)

    // ─── 身体（S 曲线 + 锁骨/肩头细节） ───
    const bodyGroup = new three.Group()
    group.add(bodyGroup)

    // 肩头（圆润）
    const shoulderGeo = new three.SphereGeometry(0.055, 14, 14)
    const shoulderL = new three.Mesh(shoulderGeo, skinMat)
    shoulderL.position.set(-0.24, shoulderY + 0.04, 0)
    bodyGroup.add(shoulderL)
    const shoulderR = new three.Mesh(shoulderGeo, skinMat)
    shoulderR.position.set(0.24, shoulderY + 0.04, 0)
    bodyGroup.add(shoulderR)
    // 锁骨（两条自然锁骨线）
    const clavL = new three.Mesh(new three.CapsuleGeometry(0.012, 0.14, 6, 10), skinMat)
    clavL.position.set(-0.09, shoulderY - 0.05, 0.07)
    clavL.rotation.z = 0.5
    bodyGroup.add(clavL)
    const clavR = new three.Mesh(new three.CapsuleGeometry(0.012, 0.14, 6, 10), skinMat)
    clavR.position.set(0.09, shoulderY - 0.05, 0.07)
    clavR.rotation.z = -0.5
    bodyGroup.add(clavR)

    // 胸（合体微翘）
    const chestMesh = new three.Mesh(new three.CapsuleGeometry(0.15, 0.1, 12, 24), skinMat)
    chestMesh.position.y = chestY
    chestMesh.scale.set(1.32, 1, 0.8)
    chestMesh.castShadow = true
    bodyGroup.add(chestMesh)
    // 腰（细腰）
    const waistMesh = new three.Mesh(new three.CylinderGeometry(0.088, 0.115, 0.2, 22), skinMat)
    waistMesh.position.y = waistY
    bodyGroup.add(waistMesh)
    // 髋（圆润）
    const hipMesh = new three.Mesh(new three.SphereGeometry(0.15, 24, 24), skinMat)
    hipMesh.position.set(0, hipY + 0.02, 0)
    hipMesh.scale.set(1.12, 0.85, 0.88)
    bodyGroup.add(hipMesh)

    // 服装部件（真人面料材质 + 美型参数）
    const outfitParts = buildOutfit3D(set, {
      topMat,
      topTrimMat,
      bottomMat,
      accentMat,
      trimMat,
      sockMat,
      shoeMat
    }, bodyGroup, body)

    // ─── 四肢（细长两段式 + 真实手掌/手指） ───
    // 左臂
    const leftArm = new three.Group()
    leftArm.position.set(-0.27, shoulderY + 0.02, 0)
    const leftUpper = new three.Mesh(new three.CapsuleGeometry(0.042, 0.22, 10, 16), topMat)
    leftUpper.position.y = -0.13
    leftUpper.castShadow = true
    leftArm.add(leftUpper)
    const leftForearm = new three.Group()
    leftForearm.position.y = -0.28
    const leftLower = new three.Mesh(new three.CapsuleGeometry(0.032, 0.2, 10, 16), skinMat)
    leftLower.position.y = -0.1
    leftForearm.add(leftLower)
    const leftHand = buildRealHand(skinMat, three)
    leftHand.position.y = -0.21
    leftForearm.add(leftHand)
    leftArm.add(leftForearm)
    bodyGroup.add(leftArm)
    refs.leftArm = leftArm
    refs.leftForearm = leftForearm

    // 右臂
    const rightArm = new three.Group()
    rightArm.position.set(0.27, shoulderY + 0.02, 0)
    const rightUpper = new three.Mesh(new three.CapsuleGeometry(0.042, 0.22, 10, 16), topMat)
    rightUpper.position.y = -0.13
    rightUpper.castShadow = true
    rightArm.add(rightUpper)
    const rightForearm = new three.Group()
    rightForearm.position.y = -0.28
    const rightLower = new three.Mesh(new three.CapsuleGeometry(0.032, 0.2, 10, 16), skinMat)
    rightLower.position.y = -0.1
    rightForearm.add(rightLower)
    const rightHand = buildRealHand(skinMat, three)
    rightHand.position.y = -0.21
    rightForearm.add(rightHand)
    rightArm.add(rightForearm)
    bodyGroup.add(rightArm)
    refs.rightArm = rightArm
    refs.rightForearm = rightForearm

    // 袜与鞋
    const sockType = set.socks || 'long'
    const shoeType = set.shoes || 'leather'
    const sockH = sockType === 'long' ? 0.34 : sockType === 'short' ? 0.12 : 0

    // 左腿（长腿：髋→膝→踝→脚底）
    const leftLeg = new three.Group()
    leftLeg.position.set(-0.12, hipY, 0)
    const leftThigh = new three.Mesh(new three.CapsuleGeometry(0.048, 0.3, 10, 16), skinMat)
    leftThigh.position.y = -0.17
    leftThigh.castShadow = true
    leftLeg.add(leftThigh)
    const leftShin = new three.Group()
    leftShin.position.y = -0.36
    const leftShinMesh = new three.Mesh(new three.CapsuleGeometry(0.036, sockH, 10, 16), sockMat)
    leftShinMesh.position.y = -0.12
    leftShin.add(leftShinMesh)
    const leftShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    leftShoe.position.set(0, -0.27, 0.035)
    leftShoe.castShadow = true
    leftShin.add(leftShoe)
    leftLeg.add(leftShin)
    bodyGroup.add(leftLeg)
    refs.leftLeg = leftLeg

    // 右腿
    const rightLeg = new three.Group()
    rightLeg.position.set(0.12, hipY, 0)
    const rightThigh = new three.Mesh(new three.CapsuleGeometry(0.048, 0.3, 10, 16), skinMat)
    rightThigh.position.y = -0.17
    rightThigh.castShadow = true
    rightLeg.add(rightThigh)
    const rightShin = new three.Group()
    rightShin.position.y = -0.36
    const rightShinMesh = new three.Mesh(new three.CapsuleGeometry(0.036, sockH, 10, 16), sockMat)
    rightShinMesh.position.y = -0.12
    rightShin.add(rightShinMesh)
    const rightShoe = buildShoe3D(shoeType, shoeMat, darkMat, three)
    rightShoe.position.set(0, -0.27, 0.035)
    rightShoe.castShadow = true
    rightShin.add(rightShoe)
    rightLeg.add(rightShin)
    bodyGroup.add(rightLeg)
    refs.rightLeg = rightLeg

    refs.body = bodyGroup
    refs.head = headGroup
    refs.hair = hairGroup

    // 整体：脚底踩 y=0（真人感版本稍大，细节更清晰可见）
    group.scale.set(0.98, 0.98, 0.98)
    group.position.y = -1.2
    const shadowChecks = [face, outfitParts.chest, outfitParts.skirt]
    group.traverse((node) => {
      if (node.isMesh) {
        node.castShadow = true
        node.receiveShadow = shadowChecks.includes(node)
      }
    })

    limbRefs = refs
    return group
  }

  function buildRealEar(skinMat, threeInst) {
    const ear = new threeInst.Group()
    const outer = new threeInst.Mesh(new threeInst.SphereGeometry(0.052, 16, 16), skinMat)
    outer.scale.set(0.5, 0.85, 0.62)
    ear.add(outer)
    const inner = new threeInst.Mesh(new threeInst.SphereGeometry(0.026, 12, 12), skinMat)
    inner.position.set(-0.014, -0.004, 0.02)
    inner.scale.set(0.5, 0.7, 0.4)
    ear.add(inner)
    return ear
  }

  /** V5 真人手掌（手掌 + 5 指） */
  function buildRealHand(skinMat, threeInst) {
    const hand = new threeInst.Group()
    const palm = new threeInst.Mesh(new threeInst.SphereGeometry(0.034, 12, 12), skinMat)
    palm.scale.set(1, 0.85, 0.72)
    hand.add(palm)
    const fingerGeo = new threeInst.CapsuleGeometry(0.0075, 0.04, 4, 8)
    const angles = [-0.5, -0.22, 0, 0.22, 0.5]
    angles.forEach((a, i) => {
      const finger = new threeInst.Mesh(fingerGeo, skinMat)
      const x = Math.sin(a) * 0.026
      finger.position.set(x, 0.015, 0.026)
      finger.rotation.z = a * 0.6
      finger.rotation.x = 1.25
      hand.add(finger)
    })
    return hand
  }

  /**
   * 构建鞋款 3D（学院皮鞋/玛丽珍/运动鞋/木屐/毛绒拖鞋/高跟鞋）
   */
  function buildShoe3D(shoeType, shoeMat, darkMat, threeInst) {
    if (shoeType === 'sneakers') {
      const shoe = new threeInst.Mesh(new threeInst.BoxGeometry(0.13, 0.07, 0.22), shoeMat)
      // 鞋底白条
      const sole = new threeInst.Mesh(new threeInst.BoxGeometry(0.13, 0.022, 0.22), darkMat)
      sole.position.y = -0.035
      shoe.add(sole)
      // 鞋带
      const lace = new threeInst.Mesh(new threeInst.BoxGeometry(0.03, 0.05, 0.01), darkMat)
      lace.position.set(0, 0.015, 0.06)
      shoe.add(lace)
      return shoe
    }
    if (shoeType === 'maryjane') {
      const shoe = new threeInst.Mesh(new threeInst.BoxGeometry(0.12, 0.055, 0.2), shoeMat)
      // 圆头
      const toe = new threeInst.Mesh(new threeInst.SphereGeometry(0.055, 10, 10), shoeMat)
      toe.position.set(0, 0, 0.09)
      toe.scale.set(1, 0.8, 1)
      shoe.add(toe)
      // 搭扣
      const strap = new threeInst.Mesh(new threeInst.BoxGeometry(0.02, 0.05, 0.01), darkMat)
      strap.position.set(0, 0.02, 0.02)
      shoe.add(strap)
      return shoe
    }
    if (shoeType === 'clogs') {
      // 木屐：方形台 + 横带
      const shoe = new threeInst.Mesh(new threeInst.BoxGeometry(0.13, 0.045, 0.22), shoeMat)
      const band = new threeInst.Mesh(new threeInst.BoxGeometry(0.06, 0.06, 0.015), shoeMat)
      band.position.set(0, 0.035, -0.03)
      shoe.add(band)
      return shoe
    }
    if (shoeType === 'slippers') {
      // 毛绒拖鞋：椭圆底 + 毛绒面
      const shoe = new threeInst.Mesh(new threeInst.CylinderGeometry(0.07, 0.06, 0.05, 12), shoeMat)
      shoe.rotation.x = Math.PI / 2
      const fur = new threeInst.Mesh(new threeInst.SphereGeometry(0.07, 10, 10), shoeMat)
      fur.position.y = 0.03
      fur.scale.set(1, 0.6, 1.4)
      shoe.add(fur)
      return shoe
    }
    if (shoeType === 'pumps') {
      // 高跟鞋：尖圆头鞋身 + 细跟（美型少女）
      const shoe = new threeInst.Mesh(new threeInst.BoxGeometry(0.1, 0.05, 0.2), shoeMat)
      const toe = new threeInst.Mesh(new threeInst.SphereGeometry(0.045, 10, 10), shoeMat)
      toe.position.set(0, 0, 0.08)
      toe.scale.set(1, 0.7, 1)
      shoe.add(toe)
      // 鞋跟（细锥）
      const heel = new threeInst.Mesh(new threeInst.CylinderGeometry(0.012, 0.018, 0.09, 8), darkMat)
      heel.position.set(0, -0.065, -0.06)
      shoe.add(heel)
      // 鞋口
      const rim = new threeInst.Mesh(new threeInst.BoxGeometry(0.11, 0.012, 0.06), darkMat)
      rim.position.set(0, 0.03, -0.02)
      shoe.add(rim)
      return shoe
    }
    // 默认学院小皮鞋
    const shoe = new threeInst.Mesh(new threeInst.BoxGeometry(0.12, 0.06, 0.2), shoeMat)
    const sole = new threeInst.Mesh(new threeInst.BoxGeometry(0.12, 0.02, 0.2), darkMat)
    sole.position.y = -0.035
    shoe.add(sole)
    return shoe
  }

  /**
   * 根据角色/皮肤/服装构建精灵 mesh 组
   */
  /**
   * 立绘贴图模式：为二次元角色加载 AI 精绘立绘 PNG 并替换为透明贴图纸片。
   * 加载期间先保留程序化 3D，加载成功后无缝替换；加载失败静默保持程序化。
   * @param {Object} character
   * @returns {boolean} 是否已发起立绘加载（角色是 anime 且存在贴图 URL 规则）
   */
  function tryBuildSprite(character) {
    if (!three || !scene) return false
    const id = character && character.id
    if (typeof id !== 'string' || !id.startsWith('anime-')) return false
    const key = id.slice('anime-'.length)
    if (!/^[a-z0-9_-]+$/i.test(key)) return false
    const url = '/assets/elf-sprites/elf_' + key + '.png'
    const token = ++spriteReqToken
    let loader
    try {
      loader = new three.TextureLoader()
    } catch (e) {
      return false
    }
    loader.load(
      url,
      (texture) => {
        if (disposed || token !== spriteReqToken) {
          texture?.dispose()
          return
        }
        try {
          texture.colorSpace = three.SRGBColorSpace
          texture.anisotropy = 4
          applySpriteTexture(texture)
        } catch (e) {
          // 纹理应用失败：保持当前程序化模型
        }
      },
      undefined,
      () => { /* 加载失败：保持程序化 3D 建模 */ }
    )
    return true
  }

  /**
   * 用户自定义形象 → 3D 立体立牌：
   * 加载图片（dataURL/URL）→ 智能去背景（色度键 + 羽化）→ 生成 CanvasTexture
   * → 走 applySpriteTexture 立牌管线（脚下阴影/镜像转身/走路转体/跳跃翻转/相机视差）。
   * 去背景失败或禁用时保留原图（仍为 3D 场景中的立牌，透明度遵循原图 alpha）。
   * @param {string} source - 图片 dataURL 或 URL
   * @param {object} [opts] - { removeBg?: boolean, threshold?: number, feather?: number }
   * @returns {Promise<boolean>} 是否成功进入立牌模式
   */
  async function setCustomSprite(source, opts = {}) {
    if (!three || !scene) return false
    if (typeof source !== 'string' || !source) return false
    const token = ++spriteReqToken
    try {
      const image = await loadImageElement(source)
      if (disposed || token !== spriteReqToken) return false
      const texture = buildCustomSpriteTexture(image, opts)
      if (!texture) return false
      if (disposed || token !== spriteReqToken) {
        texture.dispose()
        return false
      }
      applySpriteTexture(texture)
      return true
    } catch (e) {
      return false
    }
  }

  /**
   * 加载图片元素（Promise 化）
   */
  function loadImageElement(source) {
    return new Promise((resolve, reject) => {
      if (typeof Image === 'undefined') {
        reject(new Error('no_image'))
        return
      }
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error('decode_failed'))
      img.src = source
    })
  }

  /**
   * 把图片绘制为带透明通道的 CanvasTexture。
   * opts.removeBg !== false 时执行色度键去背景（四角主色 + 羽化）。
   * 纯 canvas 操作，不依赖 three 场景状态。
   * @param {HTMLImageElement} image
   * @param {object} [opts]
   * @returns {THREE.CanvasTexture|null}
   */
  function buildCustomSpriteTexture(image, opts = {}) {
    try {
      const maxDim = 512
      const ratio = Math.min(1, maxDim / Math.max(image.naturalWidth || 1, image.naturalHeight || 1))
      const w = Math.max(1, Math.round((image.naturalWidth || 1) * ratio))
      const h = Math.max(1, Math.round((image.naturalHeight || 1) * ratio))
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      ctx.clearRect(0, 0, w, h)
      ctx.drawImage(image, 0, 0, w, h)

      const removeBg = opts.removeBg !== false
      if (removeBg) {
        try {
          const imgData = ctx.getImageData(0, 0, w, h)
          const cleaned = removeImageBackground(imgData, {
            threshold: opts.threshold,
            feather: opts.feather
          })
          ctx.putImageData(cleaned, 0, 0)
        } catch (e) {
          // 去背景失败：保留原图（透明度遵循原 alpha）
        }
      }

      const tex = new three.CanvasTexture(canvas)
      tex.colorSpace = three.SRGBColorSpace
      tex.anisotropy = 4
      return tex
    } catch (e) {
      return null
    }
  }

  /**
   * 用立绘贴图替换当前 elfGroup（透明纸片，脚踩地面 y=0）
   * @param {*} texture three.Texture（PNG 透明）
   */
  function applySpriteTexture(texture) {
    if (disposed || !scene) return
    // 清理旧 mesh（含程序化几何 / 旧立绘）
    if (elfGroup) {
      scene.remove(elfGroup)
      elfGroup.traverse((child) => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) {
          const mats = Array.isArray(child.material) ? child.material : [child.material]
          mats.forEach((m) => {
            if (m.map) m.map.dispose()
            m.dispose()
          })
        }
      })
    }
    limbRefs = null
    spriteMode = true

    const imgW = texture.image ? texture.image.width : 1
    const imgH = texture.image ? texture.image.height : 1.6
    const h = 2.75 // 立绘高度（视口占比约 59%，容器内约 76px——清晰可辨）
    const w = h * (imgW / imgH)

    const geo = new three.PlaneGeometry(w, h)
    const mat = new three.MeshBasicMaterial({
      map: texture,
      transparent: true,
      alphaTest: 0.03,
      depthWrite: false,
      side: three.DoubleSide
    })
    const mesh = new three.Mesh(geo, mat)
    mesh.position.y = h / 2 // 底边踩在 y=0
    mesh.renderOrder = 5

    const group = new three.Group()
    group.add(mesh)
    group.position.y = -1.2 // 与程序化角色地面高度对齐（脚踩地）
    // 脚下椭圆阴影（立体落地感）：半透明黑圆贴片在脚底
    try {
      const shGeo = new three.CircleGeometry(0.55, 24)
      const shMat = new three.MeshBasicMaterial({
        color: 0x000000,
        transparent: true,
        opacity: 0.22,
        depthWrite: false
      })
      const shMesh = new three.Mesh(shGeo, shMat)
      shMesh.rotation.x = -Math.PI / 2
      shMesh.position.y = 0.02
      shMesh.renderOrder = 4
      group.add(shMesh)
    } catch (e) { /* 阴影贴片失败不影响立绘 */ }
    elfGroup = group
    mirrorX = 1
    scene.add(elfGroup)
  }

  function buildElfMesh(character, skin, outfit) {
    if (!three || !scene) return null

    // 清理旧 mesh
    if (elfGroup) {
      scene.remove(elfGroup)
      elfGroup.traverse(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) {
          if (Array.isArray(child.material)) {
            child.material.forEach(m => {
              if (m.map) m.map.dispose()
              m.dispose()
            })
          } else {
            if (child.material.map) child.material.map.dispose()
            child.material.dispose()
          }
        }
      })
    }
    limbRefs = null
    // 重置立绘模式状态（若为新角色触发贴图加载，加载成功后重新置 true）
    spriteMode = false
    mirrorX = 1

    const group = new three.Group()

    // 二次元少女 → 游戏级真人感 3D 人物建模（V5：stylized-realistic 物理材质 + 精细五官 + 换装），
    // 失败依次回退 V4（美型少女 1:7 + 卡通渲染 + 描边）→ V3（5头身精细建模）→ V2（精细程序化）→ V1（旧版程序化），全程立体 3D
    if (isAnimeGirl(character)) {
      // V5 真人感优先
      try {
        const v5Group = buildRealElf3DV5(character, character && character.palette, outfit)
        if (v5Group) {
          elfGroup = v5Group
          scene.add(elfGroup)
          if (typeof console !== 'undefined') {
            console.log('[Elf3D] V5 真人感 OK outfit=', outfit && outfit.id)
          }
          return elfGroup
        }
      } catch (e) {
        if (typeof console !== 'undefined') {
          console.error('[Elf3D] V5 构建异常:', e && e.message ? e.message : e)
        }
      }
      // V4 美型少女兜底
      try {
        const v4Group = buildAnimeElf3DV4(character, character && character.palette, outfit)
        if (v4Group) {
          elfGroup = v4Group
          scene.add(elfGroup)
          if (typeof console !== 'undefined') {
            console.log('[Elf3D] V4 美型 OK outfit=', outfit && outfit.id)
          }
          return elfGroup
        }
      } catch (e) {
        if (typeof console !== 'undefined') {
          console.error('[Elf3D] V4 构建异常:', e && e.message ? e.message : e)
        }
      }
      let v3Group = null
      try {
        v3Group = buildAnimeElf3DV3(character, character && character.palette, outfit)
      } catch (e) {
        // 诊断：V3 构建异常时打印原因（换装/建模排查用），随后回退 V2
        if (typeof console !== 'undefined') {
          console.error('[Elf3D] V3 构建异常:', e && e.message ? e.message : e)
        }
      }
      if (v3Group) {
        elfGroup = v3Group
        scene.add(elfGroup)
        // 诊断：确认 V3 服装材质颜色
        if (typeof console !== 'undefined') {
          let firstColor = ''
          v3Group.traverse((n) => {
            if (!firstColor && n.isMesh && n.material && n.material.color) {
              firstColor = '#' + n.material.color.getHexString()
            }
          })
          console.log('[Elf3D] V3 OK outfit=', outfit && outfit.id, 'firstColor=', firstColor)
        }
        return elfGroup
      }
      if (typeof console !== 'undefined') {
        console.warn('[Elf3D] V3 返回 null → 回退 V2')
      }
      const animeGroup = buildAnimeElf3DV2(character, character && character.palette)
      if (animeGroup) {
        elfGroup = animeGroup
        scene.add(elfGroup)
        return elfGroup
      }
      // V2 构建失败 → 回退旧版程序化建模（仍为立体几何，非纸片）
      const legacyGroup = buildAnimeElf3D(character, character && character.palette)
      if (legacyGroup) {
        elfGroup = legacyGroup
        scene.add(elfGroup)
        return elfGroup
      }
      // 构建失败 → 继续走通用几何体兜底
    }

    const geomDesc = getElf3DGeometry(character)
    const matDesc = getElfMaterial(character, skin)
    const material = createMaterial(matDesc)

    // 身体主体
    if (geomDesc.type === 'humanoid') {
      // 人形：身体（胶囊/圆柱近似）+ 头（球体）
      const bodyGeom = new three.CylinderGeometry(0.35, 0.4, geomDesc.params.bodyHeight, 16)
      const bodyMesh = new three.Mesh(bodyGeom, material)
      bodyMesh.position.y = geomDesc.params.bodyHeight / 2
      bodyMesh.castShadow = true
      group.add(bodyMesh)

      const headGeom = new three.SphereGeometry(geomDesc.params.headRadius, 24, 24)
      const headMesh = new three.Mesh(headGeom, material)
      headMesh.position.y = geomDesc.params.bodyHeight + geomDesc.params.headRadius * 0.8
      headMesh.castShadow = true
      group.add(headMesh)
    } else {
      const geom = createGeometry(geomDesc)
      if (geom) {
        const mesh = new three.Mesh(geom, material)
        mesh.castShadow = true
        group.add(mesh)
      }
    }

    // 应用整体 scale
    if (geomDesc.scale) {
      group.scale.set(geomDesc.scale.x, geomDesc.scale.y, geomDesc.scale.z)
    }

    // 服装饰品
    const accessories = getOutfitAccessories(outfit)
    accessories.forEach(acc => {
      const accGeom = createGeometry(acc.geometry)
      if (!accGeom) return
      const accMat = new three.MeshStandardMaterial({
        color: new three.Color(acc.color),
        roughness: 0.4,
        metalness: 0.2
      })
      const accMesh = new three.Mesh(accGeom, accMat)
      accMesh.position.set(acc.position.x, acc.position.y, acc.position.z)
      accMesh.castShadow = true
      group.add(accMesh)
    })

    elfGroup = group
    scene.add(elfGroup)
    return elfGroup
  }

  /**
   * 角色切换时重建 mesh
   */
  function updateElfMesh(character, skin, outfit) {
    buildElfMesh(character, skin, outfit)
  }

  /**
   * 触发动作动画
   */
  function playAction(action, duration = 1000) {
    currentAction = action
    actionProgress = 0
    actionDuration = duration
    actionStartTime = performance.now()
    isActionPlaying = true
  }

  /**
   * 切换走路状态（自由移动系统驱动）。
   * active=true 时渲染器进入步态循环：摆臂摆腿 + 身体前倾 + 朝移动方向转身，
   * 并暂停 autoRotate 与普通动作动画；active=false 时恢复 idle/动作。
   * @param {boolean} active - 是否正在行走
   * @param {number} [dirX=0] - 水平移动方向：-1 朝左、1 朝右、0 停
   */
  function setWalking(active, dirX = 0) {
    if (active) {
      if (!walkState) walkState = { active: true, dirX, phase: 0 }
      else {
        walkState.active = true
        walkState.dirX = dirX
      }
    } else if (walkState) {
      walkState.active = false
    }
  }

  /**
   * 更新行走方向（自由移动系统每帧回调；只在行走期间生效）
   * @param {number} dirX - -1 朝左、1 朝右、0 停
   */
  function setDirection(dirX) {
    if (walkState) walkState.dirX = dirX
  }

  /**
   * 触发跳跃（叠加在走路姿态上：抛物线起跳 + 双臂上举 + 身体拉伸，约 0.5s 后自动落回）
   */
  function triggerJump() {
    jumpState = { start: performance.now(), duration: 520 }
  }

  /**
   * 设置注视方向（眼神跟随鼠标）。
   * @param {number|null} x - 归一化水平注视 -1~1；传 null 恢复正对屏幕
   * @param {number|null} [y] - 归一化垂直注视 -1~1
   */
  function setGaze(x, y = 0) {
    if (x === null || x === undefined) {
      gazeTarget = null
      return
    }
    gazeTarget = { x: Math.max(-1, Math.min(1, x)), y: Math.max(-1, Math.min(1, y)) }
  }

  /**
   * 相机视差：鼠标相对精灵中心的归一化偏移驱动相机微移（-1~1），
   * 让立绘纸片相对背景产生透视位移 → 立体真实感。
   * @param {number|null} x
   * @param {number} [y]
   */
  function setCameraParallax(x, y = 0) {
    if (x === null || x === undefined) {
      camParallax = { x: 0, y: 0 }
      return
    }
    camParallax = {
      x: Math.max(-1, Math.min(1, x)),
      y: Math.max(-1, Math.min(1, y))
    }
  }

  /**
   * 拖拽拎起状态：角色悬空挣扎（手脚扑腾 + 摇摆），拖拽中暂停步态与注视。
   * @param {boolean} active
   */
  function setDragging(active) {
    draggingState = !!active
    if (draggingState) {
      dropState = null
      gazeTarget = null
      if (walkState) walkState.active = false
    }
  }

  /**
   * 松手摔落：从拎起高度加速下落 + 落地压缩回弹（约 0.42s）
   */
  function drop() {
    dropState = { start: performance.now(), duration: 420 }
  }

  /**
   * 调整 canvas 尺寸
   */
  function resize(width, height) {
    if (!renderer || !camera) return
    const w = width || containerRef?.value?.clientWidth || 300
    const h = height || containerRef?.value?.clientHeight || 300
    renderer.setSize(w, h)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }

  /**
   * 切换渲染模式：'3d' 或 '2d'（2d 时隐藏 canvas，由 SVG fallback 接管）
   */
  function setMode(mode) {
    isMode2D.value = mode === '2d'
    if (renderer?.domElement) {
      renderer.domElement.style.display = isMode2D.value ? 'none' : 'block'
    }
  }

  // ─── 动画循环 ───

  function animate() {
    if (disposed) return

    animationFrameId = requestAnimationFrame(animate)
    const config = getConfig()
    const now = performance.now()
    const dt = lastAnimTs ? Math.min((now - lastAnimTs) / 1000, 0.1) : 0
    lastAnimTs = now

    // 走路状态：时间推进连续步态相位（约 2.4 步/秒）；拖拽拎起时暂停步态
    const walkingNow = Boolean(walkState && walkState.active && elfGroup && !draggingState)
    const draggingNow = Boolean(draggingState && elfGroup)
    if (walkingNow) {
      walkState.phase = (walkState.phase || 0) + dt * Math.PI * 2 * 2.4
    }

    // 眨眼状态机（拖拽/走路时专注动作，不眨眼）
    if (elfGroup && !draggingNow && !walkingNow) {
      if (!blinkState) {
        blinkState = { next: now + 2600 + Math.random() * 3400, until: 0 }
      }
      if (blinkState.until > 0 && now > blinkState.until) {
        blinkState.until = 0
        blinkState.next = now + 2600 + Math.random() * 3400
      } else if (blinkState.until === 0 && now > blinkState.next) {
        blinkState.until = now + 140 // 眨眼持续 0.14s
      }
    }

    // 呼吸相位（1.3s 周期）
    breathPhase += dt * (Math.PI * 2 / 1.3)

    // 自动旋转（走路/拖拽时暂停；立绘纸片不旋转，避免"纸片转圈"）
    if (config.autoRotate && elfGroup && !walkingNow && !draggingNow && !spriteMode) {
      elfGroup.rotation.y += config.rotateSpeed
    }

    // 动作进度推进（走路/拖拽时由对应姿态接管）
    if (isActionPlaying && actionDuration > 0 && !walkingNow && !draggingNow) {
      const elapsed = now - actionStartTime
      actionProgress = Math.min(elapsed / actionDuration, 1)
      if (actionProgress >= 1) {
        isActionPlaying = false
      }
    }

    // 应用变换
    if (elfGroup) {
      const s = geomScaleCache || { x: 1, y: 1, z: 1 }
      const t = now

      if (draggingNow) {
        // ─── 拖拽拎起：悬空挣扎（手脚扑腾 + 三轴立体摇摆） ───
        elfGroup.position.set(0, smoothPose('dragPosY', 0.45 + Math.sin(t / 90) * 0.04, dt, 10), 0)
        // 立绘模式：X/Y/Z 三轴摆动 → 立体挣扎（纸片转体挣扎，不再只是左右晃）
        elfGroup.rotation.x = smoothPose('dragRotX', 0.18 + (spriteMode ? Math.sin(t / 90) * 0.1 : 0), dt, 10)
        elfGroup.rotation.y = smoothPose('dragRotY', Math.sin(t / 140) * (spriteMode ? 0.45 : 0.22) + Math.sin(t / 70) * 0.12, dt, 10)
        elfGroup.rotation.z = smoothPose('dragRotZ', Math.sin(t / 70) * 0.15, dt, 10)
        elfGroup.scale.set(s.x, s.y, s.z)
        if (limbRefs) {
          if (limbRefs.leftArm) limbRefs.leftArm.rotation.z = smoothPose('leftArmZ', Math.sin(t / 110) * 0.9 + 0.4, dt, 12)
          if (limbRefs.rightArm) limbRefs.rightArm.rotation.z = smoothPose('rightArmZ', -Math.sin(t / 110) * 0.9 - 0.4, dt, 12)
          // 前臂自然弯曲（跟随上臂 + 基础弧度）
          if (limbRefs.leftForearm) limbRefs.leftForearm.rotation.z = smoothPose('leftForearmZ', (Math.sin(t / 110) * 0.9 + 0.4) * 0.65 + 0.35, dt, 12)
          if (limbRefs.rightForearm) limbRefs.rightForearm.rotation.z = smoothPose('rightForearmZ', (-Math.sin(t / 110) * 0.9 - 0.4) * 0.65 + 0.35, dt, 12)
          if (limbRefs.leftLeg) limbRefs.leftLeg.rotation.z = smoothPose('leftLegZ', Math.sin(t / 130) * 0.55, dt, 12)
          if (limbRefs.rightLeg) limbRefs.rightLeg.rotation.z = smoothPose('rightLegZ', -Math.sin(t / 130) * 0.55, dt, 12)
          if (limbRefs.body) {
            limbRefs.body.rotation.z = smoothPose('bodyRotZ', Math.sin(t / 70) * 0.08, dt, 12)
            limbRefs.body.position.y = smoothPose('bodyY', Math.sin(t / 90) * 0.03, dt, 12)
          }
          if (limbRefs.head) {
            limbRefs.head.rotation.x = smoothPose('headRotX', -0.55, dt, 10)
            limbRefs.head.rotation.z = smoothPose('headRotZ', Math.sin(t / 80) * 0.1, dt, 10)
          }
        }
      } else if (walkingNow) {
        // ─── 走路步态：前倾 + 起伏 + 摇摆 + 朝移动方向转身 + 摆臂摆腿 ───
        const prog = (((walkState.phase % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2)) / (Math.PI * 2)
        const tf = getAction3DTransform('walk', prog)

        let rotY = 0
        if (walkState.dirX > 0.05) rotY = Math.PI / 2
        else if (walkState.dirX < -0.05) rotY = -Math.PI / 2

        elfGroup.position.set(
          smoothPose('walkPosX', tf.position.x, dt, 12),
          smoothPose('walkPosY', tf.position.y, dt, 12),
          smoothPose('walkPosZ', tf.position.z, dt, 12)
        )
        elfGroup.rotation.x = smoothPose('walkRotX', tf.rotation.x, dt, 12)
        elfGroup.rotation.y = smoothPose('walkRotY', rotY, dt, 12)
        elfGroup.rotation.z = smoothPose('walkRotZ', tf.rotation.z, dt, 12)

        // 二次元少女四肢摆臂摆腿 + 身体摇摆（立绘模式无四肢，跳过）
        if (limbRefs) {
          const limb = getAnimeElfLimbAction('walk', prog)
          if (limbRefs.leftArm) limbRefs.leftArm.rotation.z = smoothPose('leftArmZ', limb.leftArmZ, dt, 14)
          if (limbRefs.rightArm) limbRefs.rightArm.rotation.z = smoothPose('rightArmZ', limb.rightArmZ, dt, 14)
          // 前臂自然弯曲（走路摆臂带肘弯）
          if (limbRefs.leftForearm) limbRefs.leftForearm.rotation.z = smoothPose('leftForearmZ', limb.leftArmZ * 0.65 + 0.35, dt, 14)
          if (limbRefs.rightForearm) limbRefs.rightForearm.rotation.z = smoothPose('rightForearmZ', limb.rightArmZ * 0.65 + 0.35, dt, 14)
          if (limbRefs.leftLeg) limbRefs.leftLeg.rotation.z = smoothPose('leftLegZ', limb.leftLegZ, dt, 14)
          if (limbRefs.rightLeg) limbRefs.rightLeg.rotation.z = smoothPose('rightLegZ', limb.rightLegZ, dt, 14)
          if (limbRefs.body) {
            limbRefs.body.rotation.z = smoothPose('bodyRotZ', limb.bodyRotZ, dt, 14)
            limbRefs.body.position.y = smoothPose('bodyY', limb.bodyY, dt, 14)
          }
          if (limbRefs.head) {
            limbRefs.head.rotation.x = smoothPose('headRotX', 0.08, dt, 14) // 走路低头看路
            limbRefs.head.rotation.z = smoothPose('headRotZ', 0, dt, 14)
          }
        }

        // 立绘模式：纸片不侧转 90°（避免"翻面"），用镜像 + 绕 Y 轴转体扭动表达转身 → 立体真实
        if (spriteMode) {
          mirrorX = walkState.dirX < -0.05 ? -1 : 1
          const twist = Math.sin(walkState.phase) * 0.18 // 走路转体扭动（立体感）
          elfGroup.rotation.y = twist * (mirrorX < 0 ? -1 : 1)
          elfGroup.rotation.z = tf.rotation.z
          elfGroup.scale.x = s.x * mirrorX
        } else {
          elfGroup.scale.x = s.x
        }

        // 跳跃叠加：抛物线起跳 + 双臂上举 + 身体拉伸 + 立绘水平转体（立体翻转身）
        if (jumpState) {
          const jp = (now - jumpState.start) / jumpState.duration
          if (jp >= 1) {
            jumpState = null
          } else {
            elfGroup.position.y += 4 * jp * (1 - jp) * 0.8
            const stretch = 1 + Math.sin(jp * Math.PI) * 0.18
            elfGroup.scale.set(s.x * (spriteMode ? mirrorX : 1) * stretch, s.y / stretch, s.z)
            if (spriteMode) {
              // 水平转体 180°（跳起翻转，落地回到原朝向）→ 立体动感
              const spin = Math.sin(jp * Math.PI) * Math.PI * 0.7
              elfGroup.rotation.y += spin * (mirrorX < 0 ? -1 : 1)
            }
            if (limbRefs) {
              if (limbRefs.leftArm) limbRefs.leftArm.rotation.z = smoothPose('leftArmZ', 0.95, dt, 14)
              if (limbRefs.rightArm) limbRefs.rightArm.rotation.z = smoothPose('rightArmZ', 0.95, dt, 14)
              // 跳跃双臂上举时前臂随之举起
              if (limbRefs.leftForearm) limbRefs.leftForearm.rotation.z = smoothPose('leftForearmZ', 0.95, dt, 14)
              if (limbRefs.rightForearm) limbRefs.rightForearm.rotation.z = smoothPose('rightForearmZ', 0.95, dt, 14)
            }
          }
        } else {
          elfGroup.scale.set(s.x * (spriteMode ? mirrorX : 1), s.y, s.z)
        }
      } else {
        // ─── 普通动作变换（idle/呼吸/眨眼/眼神跟随/动作） ───
        const transform = getAction3DTransform(currentAction, actionProgress)
        elfGroup.position.set(
          smoothPose('posX', transform.position.x, dt, 14),
          smoothPose('posY', transform.position.y, dt, 14),
          smoothPose('posZ', transform.position.z, dt, 14)
        )
        elfGroup.rotation.x = smoothPose('rotX', transform.rotation.x, dt, 14)
        elfGroup.rotation.z = smoothPose('rotZ', transform.rotation.z, dt, 14)

        // 保留 autoRotate 的 rotation.y，只覆盖动作带来的 y 旋转
        if (config.autoRotate) {
          if (currentAction === 'spin') {
            // 转圈动作：整圈旋转覆盖 autoRotate（游戏角色原地转圈）
            elfGroup.rotation.y = transform.rotation.y
          }
          // 其他动作：autoRotate 已经在上面累加了，动作旋转叠加为 0，避免冲突
        } else {
          elfGroup.rotation.y = smoothPose('rotY', transform.rotation.y, dt, 14)
        }

        elfGroup.scale.set(
          s.x * smoothPose('scaleX', transform.scale.x, dt, 14),
          s.y * smoothPose('scaleY', transform.scale.y, dt, 14),
          s.z * transform.scale.z
        )

        // 二次元少女四肢局部动画（wave/dance/jump 等骨骼摆幅）
        if (limbRefs) {
          const limb = getAnimeElfLimbAction(currentAction, actionProgress)
          if (limbRefs.leftArm) limbRefs.leftArm.rotation.z = smoothPose('leftArmZ', limb.leftArmZ, dt, 14)
          if (limbRefs.rightArm) limbRefs.rightArm.rotation.z = smoothPose('rightArmZ', limb.rightArmZ, dt, 14)
          // 手臂前伸（飞吻抛手等）：rotation.x 向前
          if (limbRefs.leftArm && limb.leftArmX !== undefined) limbRefs.leftArm.rotation.x = smoothPose('leftArmX', limb.leftArmX, dt, 14)
          if (limbRefs.rightArm && limb.rightArmX !== undefined) limbRefs.rightArm.rotation.x = smoothPose('rightArmX', limb.rightArmX, dt, 14)
          // 前臂弯曲：动作显式定义时用动作值，否则按摆臂自然弯肘
          if (limbRefs.leftForearm) limbRefs.leftForearm.rotation.z = smoothPose('leftForearmZ', limb.leftForearmZ !== undefined ? limb.leftForearmZ : limb.leftArmZ * 0.65 + 0.35, dt, 14)
          if (limbRefs.rightForearm) limbRefs.rightForearm.rotation.z = smoothPose('rightForearmZ', limb.rightForearmZ !== undefined ? limb.rightForearmZ : limb.rightArmZ * 0.65 + 0.35, dt, 14)
          if (limbRefs.leftLeg) limbRefs.leftLeg.rotation.z = smoothPose('leftLegZ', limb.leftLegZ, dt, 14)
          if (limbRefs.rightLeg) limbRefs.rightLeg.rotation.z = smoothPose('rightLegZ', limb.rightLegZ, dt, 14)
          if (limbRefs.body) {
            limbRefs.body.rotation.z = smoothPose('bodyRotZ', limb.bodyRotZ, dt, 14)
            limbRefs.body.position.y = smoothPose('bodyY', limb.bodyY, dt, 14)
          }
          // 动作头部姿态（点头/摇头/歪头等）；未定义时由眼神跟随接管
          if (limbRefs.head) {
            const hx = limb.headRotX
            const hy = limb.headRotY
            const hz = limb.headRotZ
            if (hx !== undefined || hy !== undefined || hz !== undefined) {
              limbRefs.head.rotation.x = smoothPose('headRotX', hx !== undefined ? hx : 0, dt, 14)
              limbRefs.head.rotation.y = smoothPose('headRotY', hy !== undefined ? hy : 0, dt, 14)
              limbRefs.head.rotation.z = smoothPose('headRotZ', hz !== undefined ? hz : 0, dt, 14)
            }
          }
        }

        // 立绘模式动作降级：无独立四肢/表情帧 → 用整体摆动 + 倾斜模拟动作表达
        if (spriteMode) {
          mirrorX = 1
          if (currentAction === 'idle' || currentAction === 'stand') {
            // 立体呼吸：绕 Y 轴轻微左右摆动 + 微倾（立牌不再是死板平面，有立体生命感）
            elfGroup.rotation.y = Math.sin(now / 2600) * 0.05 + Math.sin(now / 1300) * 0.02
            elfGroup.rotation.z = Math.sin(now / 2000) * 0.015
          } else if (currentAction === 'jump') {
            elfGroup.rotation.z = 0
          } else if (currentAction === 'sleep') {
            elfGroup.rotation.z = 0.18 // 睡着歪头
            elfGroup.rotation.x = -0.08
          } else if (currentAction === 'thinking') {
            elfGroup.rotation.z = 0.06 // 歪头思考
            elfGroup.position.y += 0.02
          } else {
            // wave/dance/happy/surprised 等：轻微左右摆动 + 起伏
            const sw = Math.sin(actionProgress * Math.PI * 2)
            elfGroup.rotation.z = sw * 0.12
            elfGroup.position.y += Math.abs(sw) * 0.05
          }
          // 眼神跟随：整体微微转向鼠标方向（替代头部转向）
          if (gazeTarget) {
            elfGroup.rotation.y += gazeTarget.x * 0.12
            elfGroup.rotation.x += -gazeTarget.y * 0.06
          }
        }

        // 眼神跟随：头部/眼睛转向鼠标方向（idle 时最明显；动作自带头姿态时不覆盖）
        if (limbRefs && limbRefs.head && (currentAction === 'idle' || currentAction === 'stand' || currentAction === 'happy')) {
          const limbNow = getAnimeElfLimbAction(currentAction, actionProgress)
          const hasHeadPose = limbNow.headRotX !== undefined || limbNow.headRotY !== undefined || limbNow.headRotZ !== undefined
          if (!hasHeadPose) {
            if (gazeTarget) {
              limbRefs.head.rotation.y = smoothPose('gazeY', gazeTarget.x * 0.35, dt, 10)
              limbRefs.head.rotation.x = smoothPose('gazeX', -gazeTarget.y * 0.25, dt, 10)
            } else {
              limbRefs.head.rotation.y = smoothPose('gazeY', 0, dt, 10)
              limbRefs.head.rotation.x = smoothPose('gazeX', 0, dt, 10)
            }
          }
        }

        // 眨眼：眼睛 z 向压扁（闭合）
        if (limbRefs && limbRefs.eyes && blinkState && blinkState.until > 0 && now < blinkState.until) {
          for (const eye of limbRefs.eyes) {
            eye.scale.y = 0.12
            eye.scale.z = 0.6
          }
        } else if (limbRefs && limbRefs.eyes) {
          for (const eye of limbRefs.eyes) {
            eye.scale.y = 1
            eye.scale.z = 1
          }
        }

        // 呼吸：idle/stand 时身体轻微起伏 + 缩放（真人感：幅度小、频率缓）+ 长发随呼吸轻摆
        if (currentAction === 'idle' || currentAction === 'stand') {
          const br = Math.sin(breathPhase)
          elfGroup.position.y += br * 0.025
          elfGroup.scale.set(
            s.x * (1 + br * 0.012),
            s.y * (1 - br * 0.012),
            s.z
          )
          // 头发/裙摆轻微摆动（真人感动态）
          if (limbRefs && limbRefs.hair) {
            limbRefs.hair.rotation.z = br * 0.012
            limbRefs.hair.rotation.x = br * 0.006
          }
        }
      }

      // 松手摔落：从拎起高度加速下落 + 落地压缩回弹（拖拽中不触发）
      if (dropState && !draggingNow) {
        const dp = Math.min((now - dropState.start) / dropState.duration, 1)
        if (dp >= 1) {
          dropState = null
        } else {
          const fall = 0.45 * (1 - Math.pow(1 - dp, 2)) // 重力加速下落
          elfGroup.position.y += 0.45 - fall
          const squash = dp > 0.72 ? (1 - Math.sin(((dp - 0.72) / 0.28) * Math.PI) * 0.16) : 1
          elfGroup.scale.set(s.x * (spriteMode ? mirrorX : 1) * squash, s.y / squash, s.z)
          if (spriteMode) {
            // 摔落水平打转（立体感：落地时转半圈）
            const rot = Math.sin(dp * Math.PI) * 1.1
            elfGroup.rotation.y += rot * (mirrorX < 0 ? -1 : 1)
          }
        }
      }
    }

    // 相机视差平滑（立体真实感）：鼠标位置驱动相机微移，立绘相对背景产生透视位移；
    // 拖拽时相机回中（专注挣扎动画）
    if (camera) {
      const tx = draggingNow ? 0 : camParallax.x
      const ty = draggingNow ? 0 : camParallax.y
      camSmooth.x += (tx - camSmooth.x) * 0.08
      camSmooth.y += (ty - camSmooth.y) * 0.08
      camera.position.x = camSmooth.x * 0.22
      camera.position.y = 0 + camSmooth.y * 0.16
      camera.lookAt(0, camLookAtY, 0)
    }

    renderer?.render(scene, camera)
  }

  let geomScaleCache = null

  // ─── 初始化（onMounted） ───

  async function init() {
    try {
      // 动态 import three，避免 jsdom 下崩溃
      three = await import('three')
    } catch (e) {
      // jsdom 或环境不支持 three，静默 fallback
      return
    }

    if (disposed) return

    const container = containerRef?.value
    if (!container) return

    const config = getConfig()
    const width = container.clientWidth || 300
    const height = container.clientHeight || 300

    // Scene（背景透明：桌宠悬浮效果，像动态壁纸一样只显示精灵本体，无外框）
    scene = new three.Scene()
    scene.background = null

    // Camera
    camera = new three.PerspectiveCamera(50, width / height, 0.1, 100)
    camera.position.z = 5

    // Renderer
    renderer = new three.WebGLRenderer({ antialias: true, alpha: true })
    renderer.setSize(width, height)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
    // 电影级色调映射：色彩更真实柔和（真人感渲染核心）
    renderer.toneMapping = three.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.12
    renderer.outputColorSpace = three.SRGBColorSpace
    if (config.shadowEnabled) {
      renderer.shadowMap.enabled = true
      // three r184+ 已弃用 PCFSoftShadowMap，使用 PCFShadowMap（软阴影）
      renderer.shadowMap.type = three.PCFShadowMap
    }
    container.appendChild(renderer.domElement)

    // Lights（游戏级四光源：半球光定调 + 主光塑形 + 正面补光提亮面部 + 轮廓光勾边）
    const ambientLight = new three.AmbientLight(0xffffff, 0.45)
    scene.add(ambientLight)

    const hemiLight = new three.HemisphereLight(0xffffff, 0x3a3a5c, 0.65)
    scene.add(hemiLight)

    const dirLight = new three.DirectionalLight(0xffffff, 1.25)
    dirLight.position.set(3, 5, 2)
    if (config.shadowEnabled) {
      dirLight.castShadow = true
      dirLight.shadow.mapSize.width = 1024
      dirLight.shadow.mapSize.height = 1024
    }
    scene.add(dirLight)

    // 轮廓背光（rim light）：从角色后方打冷光，勾出头发/肩膀轮廓 → 游戏渲染氛围
    const rimLight = new three.DirectionalLight(0xa8c0ff, 0.6)
    rimLight.position.set(-2.5, 2, -3.2)
    scene.add(rimLight)

    // 正面补光（fill light）：提亮面部与服装细节，让五官清晰可见
    const fillLight = new three.DirectionalLight(0xfff2e8, 0.5)
    fillLight.position.set(-1.8, 0.8, 3.2)
    scene.add(fillLight)

    // 三色调卡通渐变贴图（V3 材质共用；创建失败时 V3 材质退化为单色卡通）
    toonGradientMap = createToonGradientMap()

    // Ground shadow plane
    if (config.shadowEnabled) {
      // 脚下柔和阴影：小面积 + 低透明，透明背景后仅见椭圆淡影，不构成外框
      const groundGeom = new three.PlaneGeometry(5.5, 5.5)
      const groundMat = new three.ShadowMaterial({ opacity: 0.16 })
      groundMesh = new three.Mesh(groundGeom, groundMat)
      groundMesh.rotation.x = -Math.PI / 2
      groundMesh.position.y = -1.2
      groundMesh.receiveShadow = true
      scene.add(groundMesh)
    }

    // 构建初始精灵 mesh（由外部调用 buildElfMesh 传入实际角色数据）
    isReady.value = true

    // 启动动画循环
    animate()

    // visibilitychange：页面隐藏暂停 RAF，可见恢复
    visibilityHandler = () => {
      if (document.hidden) {
        if (animationFrameId) {
          cancelAnimationFrame(animationFrameId)
          animationFrameId = null
        }
      } else {
        if (!animationFrameId && !disposed) {
          animate()
        }
      }
    }
    document.addEventListener('visibilitychange', visibilityHandler)
  }

  // ─── 挂载 / 卸载 ───

  onMounted(() => {
    init()
  })

  onUnmounted(() => {
    disposed = true

    // 取消 RAF
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }

    // 移除 visibilitychange 监听
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
    }

    // 释放 three.js 资源
    if (scene && elfGroup) {
      elfGroup.traverse(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) {
          if (Array.isArray(child.material)) {
            child.material.forEach(m => m.dispose())
          } else {
            child.material.dispose()
          }
        }
      })
      scene.remove(elfGroup)
    }

    if (groundMesh) {
      groundMesh.geometry?.dispose()
      groundMesh.material?.dispose()
      groundMesh = null
    }

    if (toonGradientMap) {
      toonGradientMap.dispose()
      toonGradientMap = null
    }

    renderer?.dispose()

    // 从 DOM 移除 canvas
    if (renderer?.domElement?.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }

    renderer = null
    scene = null
    camera = null
    elfGroup = null
    three = null
    isReady.value = false
  })

  return {
    isReady,
    isMode2D,
    buildElfMesh,
    updateElfMesh,
    playAction,
    resize,
    setMode,
    setWalking,
    setDirection,
    triggerJump,
    setGaze,
    setCameraParallax,
    setDragging,
    drop,
    setCustomSprite
  }
}

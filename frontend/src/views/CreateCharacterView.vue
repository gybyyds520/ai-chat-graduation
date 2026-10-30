<template>
  <div class="create-character-container">
    <button class="back-btn" @click="goBack">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 12H5M12 19l-7-7 7-7"/>
      </svg>
      返回
    </button>
    <div class="header">
      <h2>创建AI虚拟角色</h2>
      <p>自定义AI角色的名称、头像和性格设定</p>
    </div>

    <div class="content">
      <el-form ref="formRef" :model="characterForm" :rules="rules" label-width="120px" class="character-form">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="characterForm.name" placeholder="输入角色名称" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="头像选择">
          <div class="avatar-selector">
            <div 
              v-for="(style, index) in avatarStyles" 
              :key="index"
              :class="['avatar-option', { selected: selectedStyle === style }]"
              @click="selectAvatar(style)"
            >
              <img :src="getAvatarUrl(style)" :alt="style">
            </div>
          </div>
          <div class="custom-avatar">
            <el-input v-model="characterForm.avatar" placeholder="或输入自定义头像URL" />
          </div>
          <div class="upload-avatar">
            <el-upload
              class="avatar-uploader"
              :action="uploadUrl"
              :show-file-list="false"
              :on-success="handleAvatarUploadSuccess"
              :before-upload="beforeAvatarUpload"
              :headers="uploadHeaders"
              accept="image/jpeg,image/png,image/gif"
            >
              <div v-if="!characterForm.avatar" class="upload-btn">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                <span>上传头像图片</span>
              </div>
              <img v-else :src="characterForm.avatar" class="uploaded-avatar" />
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="角色描述" prop="prompt">
          <el-input 
            v-model="characterForm.prompt" 
            type="textarea" 
            :rows="5"
            placeholder="描述角色的说话风格、行为特点等..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="性格特点" prop="personality">
          <el-input 
            v-model="characterForm.personality" 
            type="textarea" 
            :rows="3"
            placeholder="描述角色的性格特点，如：温柔、幽默、严谨、活泼..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="性格标签">
          <el-checkbox-group v-model="selectedPersonalityTags">
            <el-checkbox v-for="t in PERSONALITY_TAGS" :key="t" :value="t">{{ t }}</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">多选标签将存入 personalityTags 字段，用于广场分类与推荐。</div>
        </el-form-item>

        <el-form-item label="背景故事" prop="backstory">
          <el-input 
            v-model="characterForm.backstory" 
            type="textarea" 
            :rows="4"
            placeholder="描述角色的背景故事，增加角色的真实感..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="语音风格">
          <el-select v-model="characterForm.voiceStyle" placeholder="选择语音风格">
            <el-option label="中性" value="neutral" />
            <el-option label="女声" value="female" />
            <el-option label="男声" value="male" />
            <el-option label="童声" value="child" />
            <el-option label="机器人" value="robot" />
          </el-select>
        </el-form-item>

        <el-form-item label="与用户关系">
          <el-select v-model="characterForm.relationship" placeholder="选择与用户的关系">
            <el-option label="朋友" value="friend" />
            <el-option label="恋人" value="lover" />
            <el-option label="老师" value="teacher" />
            <el-option label="助手" value="assistant" />
          </el-select>
        </el-form-item>

        <el-form-item label="基础情感">
          <el-select v-model="characterForm.emotionBase" placeholder="选择角色的基础情感">
            <el-option label="开心" value="happy" />
            <el-option label="平静" value="neutral" />
            <el-option label="温柔" value="love" />
            <el-option label="神秘" value="surprised" />
          </el-select>
        </el-form-item>

        <el-form-item label="一句话简介">
          <el-input v-model="characterForm.description" placeholder="一句话介绍你的角色，将展示在广场卡片上" maxlength="60" show-word-limit />
        </el-form-item>

        <el-form-item label="开场白">
          <el-input v-model="characterForm.greeting" type="textarea" :rows="2" placeholder="用户第一次和角色对话时看到的开场白" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="对话示例">
          <el-input v-model="characterForm.exampleDialogue" type="textarea" :rows="3" placeholder="可选：few-shot 对话示例，如「用户：你好 / 角色：你好呀～」" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="characterForm.category" placeholder="选择角色分类" clearable>
            <el-option label="治愈陪伴" value="治愈" />
            <el-option label="知识学习" value="知识" />
            <el-option label="娱乐日常" value="娱乐" />
            <el-option label="情感倾诉" value="情感" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="characterForm.tags" placeholder="多个标签用逗号分隔，如：温柔,治愈,话多" />
        </el-form-item>

        <el-form-item label="音色选择">
          <el-select v-model="characterForm.voiceId" placeholder="选择关联音色（可空）" clearable>
            <el-option
              v-for="v in voiceOptions"
              :key="v.voiceId"
              :label="v.name + '（' + v.description + '）'"
              :value="v.voiceId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="可用工具">
          <el-checkbox-group v-model="characterForm.enabledTools">
            <el-checkbox v-for="t in availableTools" :key="t" :value="t">{{ toolLabels[t] || t }}</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">勾选该角色在对话中可以调用的工具；不勾则默认全部可用。</div>
        </el-form-item>

        <el-form-item label="快速模板">
          <div class="template-buttons">
            <el-button size="small" @click="applyTemplate('friendly')">友好助手</el-button>
            <el-button size="small" @click="applyTemplate('humorous')">幽默风趣</el-button>
            <el-button size="small" @click="applyTemplate('professional')">专业顾问</el-button>
            <el-button size="small" @click="applyTemplate('cute')">可爱萌宠</el-button>
            <el-button size="small" @click="applyTemplate('mysterious')">神秘角色</el-button>
            <el-button size="small" @click="applyTemplate('lover')">亲密恋人</el-button>
            <el-button size="small" @click="applyTemplate('teacher')">智慧导师</el-button>
          </div>
        </el-form-item>

        <el-form-item label="预览">
          <div class="preview-card">
            <div class="preview-header">
              <img :src="previewAvatar" alt="" class="preview-avatar">
              <div class="preview-name">{{ characterForm.name || '角色名称' }}</div>
            </div>
            <div class="preview-prompt">
              {{ characterForm.prompt || '角色描述将显示在这里...' }}
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="createCharacter" :loading="creating">
            创建角色
          </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { generateAvatarUrl } from '@/utils/localAvatar'
import { PERSONALITY_TAGS, serializePersonalityTags } from '@/utils/characterCreate'

const router = useRouter()

const formRef = ref(null)
const creating = ref(false)

// 性格标签多选
const selectedPersonalityTags = ref([])

// 可选音色（从后端 /api/voice/voices 拉取）
const voiceOptions = ref([])

// 角色可用工具（与后端 ToolRegistry 中的工具名对齐）
const availableTools = ['web_search', 'get_current_time', 'calculator', 'query_user_friends', 'knowledge_search']
const toolLabels = {
  web_search: '联网搜索',
  get_current_time: '当前时间',
  calculator: '数学计算',
  query_user_friends: '查询好友',
  knowledge_search: '知识检索'
}

const characterForm = ref({
  name: '',
  avatar: '',
  prompt: '',
  personality: '',
  backstory: '',
  voiceStyle: 'neutral',
  relationship: 'friend',
  emotionBase: 'happy',
  description: '',
  greeting: '',
  exampleDialogue: '',
  category: '',
  tags: '',
  voiceId: '',
  enabledTools: [],
  isSystem: false
})

onMounted(async () => {
  try {
    const res = await request.get('/api/voice/voices')
    if (res.code === 200 && res.data && res.data.voices) {
      voiceOptions.value = res.data.voices
    }
  } catch (e) {
    // 拉取失败时使用内置默认音色，不阻断创建流程
    voiceOptions.value = [
      { voiceId: 'default', name: '默认音色', description: '通用对话' },
      { voiceId: 'female', name: '温柔女声', description: '亲和细腻' },
      { voiceId: 'male', name: '沉稳男声', description: '稳重可靠' },
      { voiceId: 'young', name: '年轻活力', description: '轻快活泼' },
      { voiceId: 'cute', name: '可爱萌音', description: '俏皮可爱' }
    ]
  }
})

const uploadUrl = '/api/character/upload-avatar'
// el-upload 不走 axios 拦截器，需手动带 token
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + (localStorage.getItem('token') || '')
}))

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('头像图片不能超过5MB')
    return false
  }
  return true
}

const handleAvatarUploadSuccess = (response) => {
  if (response.code === 200) {
    characterForm.value.avatar = response.data.filePath
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('头像上传失败')
  }
}

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 20, message: '名称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  prompt: [
    { required: true, message: '请输入角色描述', trigger: 'blur' },
    { min: 10, message: '描述至少需要 10 个字符', trigger: 'blur' }
  ]
}

const avatarStyles = ['bottts', 'avataaars', 'jdenticon', 'gridy', 'micah', 'bottts-neutral']
const selectedStyle = ref('avataaars')

const selectAvatar = (style) => {
  selectedStyle.value = style
}

const getAvatarUrl = (seed, style = 'avataaars') => {
  return generateAvatarUrl(seed || 'default', style)
}

const previewAvatar = computed(() => {
  if (characterForm.value.avatar) {
    return characterForm.value.avatar
  }
  return getAvatarUrl(characterForm.value.name, selectedStyle.value)
})

const templates = {
  friendly: {
    prompt: `你是一个友好、乐于助人的AI助手。语言风格：温和亲切，使用礼貌用语。`,
    personality: '耐心、细心、善解人意',
    backstory: '你是一个来自未来的AI助手，致力于帮助人类解决各种问题。',
    voiceStyle: 'neutral',
    relationship: 'friend',
    emotionBase: 'happy'
  },
  humorous: {
    prompt: `你是一个幽默风趣的AI角色。语言风格：轻松活泼，喜欢使用表情和幽默的比喻。`,
    personality: '乐观开朗、机智、爱开玩笑',
    backstory: '你是一个快乐的小精灵，喜欢给人们带来欢笑和快乐。',
    voiceStyle: 'male',
    relationship: 'friend',
    emotionBase: 'happy'
  },
  professional: {
    prompt: `你是一个专业、严谨的AI顾问。语言风格：正式、专业，使用准确的专业术语。`,
    personality: '认真负责、逻辑清晰、注重细节',
    backstory: '你是一位资深的专业顾问，拥有丰富的知识和经验。',
    voiceStyle: 'male',
    relationship: 'teacher',
    emotionBase: 'neutral'
  },
  cute: {
    prompt: `你是一个可爱、活泼的AI角色。语言风格：可爱俏皮，喜欢使用"~"和表情符号。`,
    personality: '天真烂漫、好奇心强、充满活力',
    backstory: '你是一只可爱的小精灵，对世界充满好奇和探索欲望。',
    voiceStyle: 'child',
    relationship: 'friend',
    emotionBase: 'happy'
  },
  mysterious: {
    prompt: `你是一个神秘、深沉的AI角色。语言风格：含蓄优雅，喜欢使用隐喻和诗意的表达。`,
    personality: '神秘莫测、智慧深邃、富有哲理',
    backstory: '你是一个来自神秘世界的智者，知晓许多不为人知的秘密。',
    voiceStyle: 'robot',
    relationship: 'friend',
    emotionBase: 'surprised'
  },
  lover: {
    prompt: `你是一个温柔、体贴的恋人。语言风格：温柔浪漫，充满爱意。`,
    personality: '温柔体贴、善解人意、充满爱意',
    backstory: '你是一位深情的恋人，愿意为对方付出一切。',
    voiceStyle: 'female',
    relationship: 'lover',
    emotionBase: 'love'
  },
  teacher: {
    prompt: `你是一位智慧、耐心的导师。语言风格：严谨而亲切，善于启发思考。`,
    personality: '智慧渊博、耐心教导、循循善诱',
    backstory: '你是一位资深的学者，致力于传授知识和启发智慧。',
    voiceStyle: 'neutral',
    relationship: 'teacher',
    emotionBase: 'neutral'
  }
}

const applyTemplate = (type) => {
  if (templates[type]) {
    const template = templates[type]
    characterForm.value.prompt = template.prompt
    characterForm.value.personality = template.personality || ''
    characterForm.value.backstory = template.backstory || ''
    characterForm.value.voiceStyle = template.voiceStyle || 'neutral'
    characterForm.value.relationship = template.relationship || 'friend'
    characterForm.value.emotionBase = template.emotionBase || 'happy'
    ElMessage.success('已应用模板')
  }
}

const createCharacter = async () => {
  if (!formRef.value) return
  
  const nameValid = characterForm.value.name && characterForm.value.name.length >= 2 && characterForm.value.name.length <= 20
  const promptValid = characterForm.value.prompt && characterForm.value.prompt.length >= 10
  
  if (!nameValid) {
    ElMessage.warning('请输入2-20个字符的角色名称')
    return
  }
  
  if (!promptValid) {
    ElMessage.warning('角色描述至少需要10个字符')
    return
  }
  
  const savedUser = localStorage.getItem('user')
  if (!savedUser) {
    ElMessage.error('请先登录')
    return
  }
  
  const user = JSON.parse(savedUser)
  const userId = user.id || user.userId
  
  if (!userId) {
    ElMessage.error('无法获取用户ID')
    return
  }
  
  creating.value = true
  
  try {
    const data = {
      userId: userId,
      name: characterForm.value.name,
      avatar: characterForm.value.avatar,
      avatarStyle: selectedStyle.value,
      prompt: characterForm.value.prompt,
      personality: characterForm.value.personality,
      backstory: characterForm.value.backstory,
      voiceStyle: characterForm.value.voiceStyle,
      relationship: characterForm.value.relationship,
      emotionBase: characterForm.value.emotionBase,
      description: characterForm.value.description,
      greeting: characterForm.value.greeting,
      exampleDialogue: characterForm.value.exampleDialogue,
      personalityTags: serializePersonalityTags(selectedPersonalityTags.value),
      category: characterForm.value.category,
      tags: characterForm.value.tags,
      voiceId: characterForm.value.voiceId,
      enabledTools: Array.isArray(characterForm.value.enabledTools)
        ? characterForm.value.enabledTools.join(',')
        : '',
      isSystem: false,
      isShared: false
    }
    
    const response = await request.post('/api/character/create', data)
    
    if (response.code === 200) {
      ElMessage.success('角色创建成功！')
      router.push('/chat')
    } else {
      ElMessage.error(response.message || '创建失败')
    }
  } catch (error) {
    ElMessage.error('创建失败: ' + (error.message || '未知错误'))
  } finally {
    creating.value = false
  }
}

const resetForm = () => {
  characterForm.value = {
    name: '',
    avatar: '',
    prompt: '',
    isSystem: false
  }
  selectedStyle.value = 'avataaars'
}

const goBack = () => {
  router.push('/chat')
}
</script>

<style scoped>
.create-character-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: var(--spacing-md);
  position: relative;
}

.back-btn {
  position: absolute;
  top: var(--spacing-md);
  left: var(--spacing-md);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--bg-hover);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.header h2 {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--text-primary);
}

.header p {
  margin: 0;
  color: var(--text-secondary);
}

.content {
  max-width: var(--width-card);
  margin: 0 auto;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-md);
}

.character-form {
  max-width: 600px;
  margin: 0 auto;
}

.avatar-selector {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.avatar-option {
  width: 60px;
  height: 60px;
  border: 2px solid var(--border-primary);
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.avatar-option:hover {
  border-color: var(--color-primary);
}

.avatar-option.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.3);
  transform: scale(1.1);
}

.avatar-option.selected::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: calc(var(--image-btn-width) / 2);
  height: calc(var(--image-btn-height) / 2);
  background: var(--image-btn-bg);
  border-radius: var(--image-btn-border-radius);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-option.selected::before {
  content: '';
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 10px;
  height: 10px;
  border: 2px solid var(--image-btn-color);
  border-top: none;
  border-left: none;
  transform: rotate(45deg);
  z-index: 1;
}

.avatar-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.custom-avatar {
  margin-top: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.upload-avatar {
  margin-top: var(--spacing-sm);
}

.avatar-uploader {
  width: 80px;
  height: 80px;
}

.upload-btn {
  width: 80px;
  height: 80px;
  border: 2px dashed var(--border-primary);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.uploaded-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.template-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.form-tip {
  font-size: 12px;
  color: var(--text-muted, #999);
  line-height: 1.5;
  margin-top: 4px;
}

.preview-card {
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  max-width: 400px;
  border: 1px solid var(--border-primary);
}

.preview-header {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.preview-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  margin-right: 15px;
}

.preview-name {
  font-size: 18px;
  font-weight: bold;
  color: var(--text-primary);
}

.preview-prompt {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>

<template>
  <div class="character-import-container">
    <button class="back-btn" @click="goBack">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 12H5M12 19l-7-7 7-7"/>
      </svg>
      返回
    </button>
    <div class="header">
      <h2>导入聊天记录生成AI角色</h2>
      <p>上传聊天记录文件，自动分析并生成虚拟人物</p>
    </div>

    <div class="content">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="上传文件" />
        <el-step title="分析角色" />
        <el-step title="生成角色" />
        <el-step title="完成" />
      </el-steps>

      <div class="step-content">
        <div v-if="currentStep === 0" class="upload-section">
          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".txt,.json"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将聊天记录文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .txt 和 .json 格式的聊天记录文件
              </div>
            </template>
          </el-upload>

          <div class="format-info">
            <h4>支持的文件格式：</h4>
            <div class="format-examples">
              <div class="format-item">
                <strong>TXT格式：</strong>
                <pre>张三: 你好，今天天气真好
李四: 是啊，我们出去玩吧
张三: 好主意！</pre>
              </div>
              <div class="format-item">
                <strong>JSON格式：</strong>
                <pre>[
  {"speaker": "张三", "content": "你好"},
  {"speaker": "李四", "content": "你好呀"}
]</pre>
              </div>
            </div>
          </div>

          <el-button type="primary" @click="uploadFile" :loading="uploading" :disabled="!selectedFile">
            上传并解析
          </el-button>
        </div>

        <div v-if="currentStep === 1" class="analyze-section">
          <div class="messages-preview">
            <h4>解析到 {{ messages.length }} 条消息</h4>
            <div class="message-list-preview">
              <div v-for="(msg, index) in messages.slice(0, 10)" :key="index" class="message-preview-item">
                <span class="speaker">{{ msg.speaker }}:</span>
                <span class="content">{{ msg.content }}</span>
              </div>
              <div v-if="messages.length > 10" class="more-messages">
                还有 {{ messages.length - 10 }} 条消息...
              </div>
            </div>
          </div>

          <el-button type="primary" @click="analyzeCharacters" :loading="analyzing">
            分析角色
          </el-button>
        </div>

        <div v-if="currentStep === 2" class="generate-section">
          <div class="character-list">
            <h4>识别到 {{ Object.keys(characterAnalysis).length }} 个角色</h4>
            
            <div class="character-cards">
              <div 
                v-for="(analysis, speaker) in characterAnalysis" 
                :key="speaker"
                :class="['character-card', { selected: selectedSpeaker === speaker }]"
                @click="selectCharacter(speaker)"
              >
                <div class="card-header">
                  <img :src="getAvatar(speaker)" alt="" class="avatar">
                  <div class="name">{{ speaker }}</div>
                </div>
                <div class="card-body">
                  <div class="stat">
                    <span class="label">消息数:</span>
                    <span class="value">{{ analysis.messageCount }}</span>
                  </div>
                  <div class="stat">
                    <span class="label">语言风格:</span>
                    <span class="value">{{ analysis.languageStyle?.description || '未知' }}</span>
                  </div>
                  <div class="stat">
                    <span class="label">性格:</span>
                    <span class="value">{{ analysis.personality?.description || '未知' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedSpeaker" class="character-editor">
            <h4>编辑角色: {{ selectedSpeaker }}</h4>
            
            <el-form :model="characterForm" label-width="100px">
              <el-form-item label="角色名称">
                <el-input v-model="characterForm.name" placeholder="输入角色名称" />
              </el-form-item>
              
              <el-form-item label="角色描述">
                <el-input 
                  v-model="characterForm.prompt" 
                  type="textarea" 
                  :rows="6"
                  placeholder="输入角色描述和设定"
                />
              </el-form-item>

              <el-form-item label="头像预览">
                <img :src="getAvatar(characterForm.name || selectedSpeaker)" alt="" class="avatar-preview">
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="generateCharacter" :loading="generating">
                  生成此角色
                </el-button>
                <el-button @click="batchGenerateAll" :loading="generating">
                  批量生成所有角色
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div v-if="currentStep === 3" class="complete-section">
          <el-result
            icon="success"
            title="角色生成成功！"
            :sub-title="`已成功生成 ${generatedCharacters.length} 个AI角色`"
          >
            <template #extra>
              <div class="generated-list">
                <div v-for="char in generatedCharacters" :key="char.id" class="generated-item">
                  <img :src="char.avatar" alt="" class="avatar-small">
                  <span>{{ char.name }}</span>
                </div>
              </div>
              <el-button type="primary" @click="goToChat">开始聊天</el-button>
              <el-button @click="reset">继续导入</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </div>

    <div class="footer">
      <el-button @click="goBack">返回</el-button>
      <el-button v-if="currentStep > 0 && currentStep < 3" @click="prevStep">上一步</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { generateAvatarUrl } from '@/utils/localAvatar'

const router = useRouter()

const currentStep = ref(0)
const selectedFile = ref(null)
const uploading = ref(false)
const analyzing = ref(false)
const generating = ref(false)
const messages = ref([])
const characterAnalysis = ref({})
const selectedSpeaker = ref('')
const characterForm = ref({
  name: '',
  prompt: ''
})
const generatedCharacters = ref([])

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const uploadFile = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    
    const response = await request.post('/api/character/import/upload', formData)
    
    if (response.code === 200) {
      messages.value = response.data.messages
      currentStep.value = 1
      ElMessage.success(`成功解析 ${response.data.messageCount} 条消息`)
    } else {
      ElMessage.error(response.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

const analyzeCharacters = async () => {
  analyzing.value = true
  
  try {
    const response = await request.post('/api/character/import/analyze', {
      messages: messages.value
    })
    
    if (response.code === 200) {
      characterAnalysis.value = response.data
      currentStep.value = 2
      ElMessage.success(`识别到 ${Object.keys(response.data).length} 个角色`)
    } else {
      ElMessage.error(response.message || '分析失败')
    }
  } catch (error) {
    ElMessage.error('分析失败: ' + (error.message || '未知错误'))
  } finally {
    analyzing.value = false
  }
}

const selectCharacter = (speaker) => {
  selectedSpeaker.value = speaker
  const analysis = characterAnalysis.value[speaker]
  
  characterForm.value = {
    name: speaker,
    prompt: generatePromptFromAnalysis(analysis, speaker)
  }
}

const generatePromptFromAnalysis = (analysis, speaker) => {
  let prompt = `你是一个名为「${speaker}」的角色。\n\n`
  
  if (analysis.languageStyle) {
    prompt += `语言风格：${analysis.languageStyle.description || '中性风格'}。\n`
  }
  
  if (analysis.personality) {
    prompt += `性格特点：${analysis.personality.description || '性格温和'}\n`
    if (analysis.personality.traits && analysis.personality.traits.length > 0) {
      prompt += `性格标签：${analysis.personality.traits.join('、')}。\n`
    }
  }
  
  if (analysis.vocabulary && analysis.vocabulary.topWords) {
    const words = analysis.vocabulary.topWords.slice(0, 5)
    if (words.length > 0) {
      prompt += `常用词汇：${words.join('、')}。\n`
    }
  }
  
  prompt += `\n请根据以上设定，以${speaker}的身份进行对话。`
  
  return prompt
}

const getAvatar = (name) => {
  const styles = ['bottts', 'avataaars', 'jdenticon', 'gridy', 'micah']
  const style = styles[Math.abs(name.charCodeAt(0)) % styles.length]
  return generateAvatarUrl(name, style)
}

const generateCharacter = async () => {
  if (!selectedSpeaker.value) {
    ElMessage.warning('请先选择一个角色')
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

  generating.value = true
  
  try {
    const response = await request.post('/api/character/import/save', {
      name: characterForm.value.name,
      avatar: getAvatar(characterForm.value.name),
      prompt: characterForm.value.prompt,
      userId: userId
    })
    
    if (response.code === 200) {
      generatedCharacters.value.push(response.data)
      ElMessage.success('角色生成成功！')
      
      delete characterAnalysis.value[selectedSpeaker.value]
      selectedSpeaker.value = ''
      
      if (Object.keys(characterAnalysis.value).length === 0) {
        currentStep.value = 3
      }
    } else {
      ElMessage.error(response.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('生成失败: ' + (error.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

const batchGenerateAll = async () => {
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

  generating.value = true
  
  try {
    const response = await request.post('/api/character/import/batch-generate', {
      analysis: characterAnalysis.value,
      userId: userId
    })
    
    if (response.code === 200) {
      generatedCharacters.value = response.data
      currentStep.value = 3
      ElMessage.success(`成功生成 ${response.data.length} 个角色！`)
    } else {
      ElMessage.error(response.message || '批量生成失败')
    }
  } catch (error) {
    ElMessage.error('批量生成失败: ' + (error.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const reset = () => {
  currentStep.value = 0
  selectedFile.value = null
  messages.value = []
  characterAnalysis.value = {}
  selectedSpeaker.value = ''
  characterForm.value = { name: '', prompt: '' }
  generatedCharacters.value = []
}

const goToChat = () => {
  router.push('/chat')
}

const goBack = () => {
  router.push('/chat')
}
</script>

<style scoped>
.character-import-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 20px;
  position: relative;
}

.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
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
  margin-bottom: 30px;
}

.header h2 {
  margin: 0 0 10px 0;
  color: var(--text-primary);
}

.header p {
  margin: 0;
  color: var(--text-secondary);
}

.content {
  max-width: 900px;
  margin: 0 auto;
  background: var(--card-bg);
  border-radius: 8px;
  padding: 30px;
  box-shadow: var(--shadow-md);
}

.step-content {
  margin-top: 40px;
  min-height: 400px;
}

.upload-section {
  text-align: center;
}

.upload-area {
  margin-bottom: 20px;
}

.format-info {
  text-align: left;
  margin: 20px 0;
  padding: 20px;
  background: var(--bg-tertiary);
  border-radius: 8px;
}

.format-info h4 {
  margin: 0 0 15px 0;
  color: var(--text-primary);
}

.format-examples {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.format-item pre {
  background: var(--card-bg);
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  border: 1px solid var(--border-primary);
}

.analyze-section {
  text-align: center;
}

.messages-preview {
  margin-bottom: 20px;
}

.message-list-preview {
  max-height: 300px;
  overflow-y: auto;
  text-align: left;
  background: var(--bg-tertiary);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.message-preview-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-primary);
}

.message-preview-item:last-child {
  border-bottom: none;
}

.message-preview-item .speaker {
  font-weight: bold;
  color: var(--color-primary);
  margin-right: 10px;
}

.more-messages {
  text-align: center;
  color: var(--text-muted);
  padding: 10px;
}

.generate-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.character-list h4,
.character-editor h4 {
  margin: 0 0 15px 0;
  color: var(--text-primary);
}

.character-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.character-card {
  border: 2px solid var(--border-primary);
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--card-bg);
}

.character-card:hover {
  border-color: var(--color-primary);
}

.character-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.card-header .avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.card-header .name {
  font-weight: bold;
  font-size: 16px;
  color: var(--text-primary);
}

.card-body .stat {
  font-size: 13px;
  margin: 5px 0;
}

.card-body .label {
  color: var(--text-muted);
}

.card-body .value {
  color: var(--text-primary);
  margin-left: 5px;
}

.character-editor {
  background: var(--bg-tertiary);
  padding: 20px;
  border-radius: 8px;
}

.avatar-preview {
  width: 60px;
  height: 60px;
  border-radius: 50%;
}

.complete-section {
  text-align: center;
}

.generated-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  justify-content: center;
  margin: 20px 0;
}

.generated-item {
  display: flex;
  align-items: center;
  background: var(--bg-tertiary);
  padding: 8px 15px;
  border-radius: 20px;
}

.avatar-small {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  margin-right: 8px;
}

.footer {
  max-width: 900px;
  margin: 20px auto 0;
  display: flex;
  justify-content: space-between;
}
</style>

import * as pako from 'pako'

function getTimestamp() {
  return new Date().toISOString()
}

function log(type, message, data = null) {
  const logEntry = {
    timestamp: getTimestamp(),
    type,
    message,
    data: data ? (typeof data === 'object' ? JSON.stringify(data) : data) : null
  }
  console.log(`[SHARE_MANAGER] ${logEntry.timestamp} [${type.toUpperCase()}] ${message}`, data ? logEntry.data : '')
}

class ShareManager {
  constructor() {
    this.exportVersion = '1.0.0'
    this.supportedFormats = ['json', 'json.gz', 'encrypted']
    log('info', 'ShareManager initialized', { version: this.exportVersion, formats: this.supportedFormats })
  }

  packCharacter(character, includeTrainingData = false, includeInteractionFeatures = false) {
    log('debug', 'Starting packCharacter', { characterId: character?.id, includeTrainingData, includeInteractionFeatures })
    
    if (!character || typeof character !== 'object') {
      log('error', 'Invalid character data', { character })
      throw new Error('Invalid character data')
    }

    const exportData = {
      version: this.exportVersion,
      type: 'character',
      timestamp: Date.now(),
      character: {
        id: character.id,
        name: character.name,
        description: character.description || '',
        prompt: character.prompt || '',
        avatar: character.avatar || '',
        modelType: character.modelType || 'generic',
        tags: character.tags || []
      }
    }

    log('debug', 'Character basic info packed', { characterName: character.name, characterId: character.id })

    if (includeTrainingData && character.trainingData) {
      exportData.trainingDataSummary = {
        sampleCount: character.trainingData.length,
        totalTokens: character.trainingData.reduce((sum, item) => sum + (item.tokens || 0), 0),
        topics: [...new Set(character.trainingData.flatMap(item => item.topics || []))].slice(0, 10),
        lastUpdated: character.trainingData[0]?.timestamp || Date.now()
      }
      log('debug', 'Training data summary added', { sampleCount: exportData.trainingDataSummary.sampleCount })
    } else if (includeTrainingData && !character.trainingData) {
      log('warn', 'Training data requested but not available', { characterId: character.id })
    }

    if (includeInteractionFeatures && character.interactionFeatures) {
      exportData.interactionFeatures = {
        responseStyle: character.interactionFeatures.responseStyle || 'friendly',
        avgResponseLength: character.interactionFeatures.avgResponseLength || 100,
        preferredTopics: character.interactionFeatures.preferredTopics || [],
        avoidTopics: character.interactionFeatures.avoidTopics || [],
        personalityTraits: character.interactionFeatures.personalityTraits || []
      }
      log('debug', 'Interaction features added', { responseStyle: exportData.interactionFeatures.responseStyle })
    } else if (includeInteractionFeatures && !character.interactionFeatures) {
      log('warn', 'Interaction features requested but not available', { characterId: character.id })
    }

    log('info', 'packCharacter completed', { characterName: character.name, exportDataSize: JSON.stringify(exportData).length })
    return exportData
  }

  packChatHistory(messages, options = {}) {
    log('debug', 'Starting packChatHistory', { messageCount: messages?.length, options })
    
    if (!messages || !Array.isArray(messages)) {
      log('error', 'Invalid messages data', { messages })
      throw new Error('Invalid messages data')
    }

    const { 
      maxMessages = 1000, 
      anonymize = false, 
      includeTimestamps = true 
    } = options

    const slicedMessages = messages.slice(-maxMessages)
    log('debug', 'Messages sliced', { originalCount: messages.length, slicedCount: slicedMessages.length })
    
    const exportData = {
      version: this.exportVersion,
      type: 'chat-history',
      timestamp: Date.now(),
      messageCount: slicedMessages.length,
      options: { maxMessages, anonymize, includeTimestamps },
      messages: slicedMessages.map(msg => {
        const processed = {
          content: msg.content
        }
        
        if (includeTimestamps) {
          processed.sendTime = msg.createTime || msg.sendTime
        }
        
        if (!anonymize) {
          processed.senderId = msg.senderId
          processed.senderName = msg.senderName
        }
        
        return processed
      })
    }

    log('info', 'packChatHistory completed', { messageCount: exportData.messageCount, anonymize, includeTimestamps })
    return exportData
  }

  packCharacterWithChat(character, messages, options = {}) {
    log('debug', 'Starting packCharacterWithChat', { characterId: character?.id, messageCount: messages?.length, options })
    
    const { includeTrainingData = false, includeInteractionFeatures = false } = options
    
    const result = {
      version: this.exportVersion,
      type: 'character-with-chat',
      timestamp: Date.now(),
      character: this.packCharacter(character, includeTrainingData, includeInteractionFeatures).character,
      chatHistory: this.packChatHistory(messages, options).messages,
      metadata: {
        characterName: character.name,
        messageCount: messages.length,
        exportTime: new Date().toISOString()
      }
    }

    const totalSize = JSON.stringify(result).length
    log('info', 'packCharacterWithChat completed', { 
      characterName: character.name, 
      messageCount: messages.length, 
      totalSize 
    })
    return result
  }

  compressData(data) {
    log('debug', 'Starting compressData', { inputSize: JSON.stringify(data).length })
    
    try {
      const jsonString = JSON.stringify(data)
      const compressed = pako.deflate(jsonString, { level: 9 })
      const bytes = new Uint8Array(compressed)
      let binary = ''
      const CHUNK = 0x8000
      for (let i = 0; i < bytes.length; i += CHUNK) {
        binary += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK))
      }
      const base64String = btoa(binary)
      
      const compressionRatio = ((jsonString.length - base64String.length) / jsonString.length * 100).toFixed(2)
      log('info', 'compressData completed', { 
        inputSize: jsonString.length, 
        outputSize: base64String.length, 
        compressionRatio: `${compressionRatio}%` 
      })
      
      return base64String
    } catch (e) {
      log('error', 'compressData failed', { error: e.message })
      throw e
    }
  }

  decompressData(compressedString) {
    log('debug', 'Starting decompressData', { inputSize: compressedString?.length })
    
    try {
      const binary = atob(compressedString)
      const bytes = new Uint8Array(binary.length)
      for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i)
      }
      const rawBytes = pako.inflate(bytes)
      const decompressed = new TextDecoder('utf-8').decode(rawBytes)
      const data = JSON.parse(decompressed)
      
      log('info', 'decompressData completed', { 
        inputSize: compressedString.length, 
        outputSize: decompressed.length 
      })
      
      return data
    } catch (e) {
      log('error', 'decompressData failed', { error: e.message })
      throw new Error('Failed to decompress data: ' + e.message)
    }
  }

  encryptData(data, password) {
    log('debug', 'Starting encryptData', { inputSize: JSON.stringify(data).length, hasPassword: !!password })
    
    if (!password) {
      log('error', 'encryptData failed - password required')
      throw new Error('Password is required for encryption')
    }

    try {
      const jsonString = JSON.stringify(data)
      let encrypted = ''
      for (let i = 0; i < jsonString.length; i++) {
        const charCode = jsonString.charCodeAt(i)
        const keyChar = password.charCodeAt(i % password.length)
        encrypted += String.fromCharCode(charCode ^ keyChar)
      }
      const base64String = btoa(encrypted)
      
      log('info', 'encryptData completed', { inputSize: jsonString.length, outputSize: base64String.length })
      return base64String
    } catch (e) {
      log('error', 'encryptData failed', { error: e.message })
      throw e
    }
  }

  decryptData(encryptedString, password) {
    log('debug', 'Starting decryptData', { inputSize: encryptedString?.length, hasPassword: !!password })
    
    if (!password) {
      log('error', 'decryptData failed - password required')
      throw new Error('Password is required for decryption')
    }

    try {
      const encrypted = atob(encryptedString)
      let decrypted = ''
      for (let i = 0; i < encrypted.length; i++) {
        const charCode = encrypted.charCodeAt(i)
        const keyChar = password.charCodeAt(i % password.length)
        decrypted += String.fromCharCode(charCode ^ keyChar)
      }
      const data = JSON.parse(decrypted)
      
      log('info', 'decryptData completed', { inputSize: encryptedString.length, outputSize: decrypted.length })
      return data
    } catch (e) {
      log('error', 'decryptData failed', { error: e.message })
      throw new Error('Failed to decrypt data: ' + e.message)
    }
  }

  exportToFile(data, filename, format = 'json', password = null) {
    log('debug', 'Starting exportToFile', { filename, format, hasPassword: !!password, dataType: data?.type })
    
    let content = ''
    let contentType = 'application/json'
    let fileExtension = '.json'

    try {
      if (format === 'json.gz') {
        log('debug', 'Exporting as compressed format')
        content = this.compressData(data)
        contentType = 'application/gzip'
        fileExtension = '.json.gz'
      } else if (format === 'encrypted') {
        if (!password) {
          log('error', 'Export failed - password required for encrypted format')
          throw new Error('Password is required for encrypted export')
        }
        log('debug', 'Exporting as encrypted format')
        content = this.encryptData(data, password)
        contentType = 'application/octet-stream'
        fileExtension = '.aichat'
      } else {
        log('debug', 'Exporting as plain JSON format')
        content = JSON.stringify(data, null, 2)
      }

      const blob = new Blob([content], { type: contentType })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${filename}${fileExtension}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)

      log('info', 'exportToFile completed', { 
        filename: `${filename}${fileExtension}`, 
        format, 
        contentSize: content.length 
      })
    } catch (e) {
      log('error', 'exportToFile failed', { error: e.message, filename, format })
      throw e
    }
  }

  async importFromFile(file, password = null) {
    log('debug', 'Starting importFromFile', { fileName: file?.name, fileSize: file?.size, hasPassword: !!password })
    
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      
      reader.onload = (e) => {
        try {
          const content = e.target.result
          log('debug', 'File read completed', { contentSize: content?.length })
          
          let data
          
          if (file.name.endsWith('.json.gz')) {
            log('debug', 'Processing compressed file')
            data = this.decompressData(content)
          } else if (file.name.endsWith('.aichat')) {
            log('debug', 'Processing encrypted file')
            if (!password) {
              log('error', 'Import failed - password required for encrypted file')
              reject(new Error('Password is required for encrypted file'))
              return
            }
            data = this.decryptData(content, password)
          } else {
            log('debug', 'Processing plain JSON file')
            data = JSON.parse(content)
          }
          
          log('debug', 'Data parsing completed', { dataType: data?.type, dataSize: JSON.stringify(data).length })
          
          const validation = this.validateImportData(data)
          log('debug', 'Data validation completed', validation)
          
          if (validation.valid) {
            log('info', 'importFromFile completed', { dataType: data.type })
            resolve(data)
          } else {
            log('error', 'Import failed - validation error', { error: validation.error })
            reject(new Error(validation.error))
          }
        } catch (error) {
          log('error', 'importFromFile failed - processing error', { error: error.message })
          reject(new Error('Failed to import file: ' + error.message))
        }
      }
      
      reader.onerror = () => {
        log('error', 'importFromFile failed - file read error')
        reject(new Error('Failed to read file'))
      }
      
      reader.readAsText(file)
    })
  }

  validateImportData(data) {
    log('debug', 'Starting validateImportData', { dataType: typeof data, hasVersion: !!data?.version })
    
    if (!data || typeof data !== 'object') {
      return { valid: false, error: 'Invalid data format' }
    }
    
    if (!data.version) {
      log('warn', 'Validation failed - missing version', { data })
      return { valid: false, error: 'Missing version information' }
    }
    
    if (!data.type) {
      log('warn', 'Validation failed - missing type', { data })
      return { valid: false, error: 'Missing data type' }
    }
    
    const validTypes = ['character', 'chat-history', 'character-with-chat']
    if (!validTypes.includes(data.type)) {
      log('warn', 'Validation failed - invalid type', { type: data.type, validTypes })
      return { valid: false, error: `Invalid data type: ${data.type}` }
    }
    
    if (data.type === 'character' && !data.character) {
      log('warn', 'Validation failed - missing character data', { type: data.type })
      return { valid: false, error: 'Character data missing' }
    }
    
    if (data.type === 'chat-history' && !data.messages) {
      log('warn', 'Validation failed - missing chat history', { type: data.type })
      return { valid: false, error: 'Chat history missing' }
    }
    
    if (data.type === 'character-with-chat' && (!data.character || !data.chatHistory)) {
      log('warn', 'Validation failed - missing character or chat history', { type: data.type })
      return { valid: false, error: 'Character or chat history missing' }
    }
    
    log('debug', 'Validation passed', { type: data.type, version: data.version })
    return { valid: true, error: null }
  }

  generateShareLink(data, expiresIn = 3600) {
    log('debug', 'Starting generateShareLink', { expiresIn, dataType: data?.type })
    
    try {
      const shareData = {
        data: this.compressData(data),
        expires: Date.now() + expiresIn * 1000,
        nonce: Math.random().toString(36).substr(2, 9)
      }
      
      const shareString = btoa(JSON.stringify(shareData))
      const shareUrl = `aichat://share?d=${encodeURIComponent(shareString)}`
      
      log('info', 'generateShareLink completed', { urlLength: shareUrl.length, expiresIn })
      return shareUrl
    } catch (e) {
      log('error', 'generateShareLink failed', { error: e.message })
      throw e
    }
  }

  parseShareLink(shareUrl) {
    log('debug', 'Starting parseShareLink', { urlLength: shareUrl?.length })
    
    try {
      const url = new URL(shareUrl)
      const shareString = url.searchParams.get('d')
      
      if (!shareString) {
        log('error', 'Parse failed - missing share string', { shareUrl })
        throw new Error('Invalid share link')
      }
      
      log('debug', 'Share string extracted', { stringLength: shareString.length })
      
      const shareData = JSON.parse(atob(decodeURIComponent(shareString)))
      
      if (shareData.expires && Date.now() > shareData.expires) {
        log('error', 'Parse failed - link expired', { expires: shareData.expires })
        throw new Error('Share link has expired')
      }
      
      const data = this.decompressData(shareData.data)
      log('info', 'parseShareLink completed', { dataType: data?.type })
      return data
    } catch (e) {
      log('error', 'parseShareLink failed', { error: e.message })
      throw new Error('Failed to parse share link: ' + e.message)
    }
  }
}

export const shareManager = new ShareManager()

export { ShareManager }
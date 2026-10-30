export const mockAiCharacters = [
  {
    id: 'character-001',
    name: '智慧助手',
    description: '一个博学多才的AI助手，擅长回答各种问题',
    prompt: '你是一个友好、博学的AI助手，能够回答用户的各种问题，提供准确、有用的信息。你的回答应该简洁明了，同时保持友好的语气。',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=assistant',
    modelType: 'gpt-4',
    tags: ['助手', '问答', '知识'],
    trainingData: [
      {
        content: '用户询问关于人工智能的基本概念',
        tokens: 150,
        topics: ['人工智能', '机器学习', '深度学习'],
        timestamp: Date.now() - 86400000 * 30
      },
      {
        content: '用户请求解释量子计算原理',
        tokens: 200,
        topics: ['量子计算', '物理学', '技术'],
        timestamp: Date.now() - 86400000 * 25
      },
      {
        content: '用户需要帮助解决编程问题',
        tokens: 180,
        topics: ['编程', 'JavaScript', '调试'],
        timestamp: Date.now() - 86400000 * 20
      }
    ],
    interactionFeatures: {
      responseStyle: 'professional',
      avgResponseLength: 150,
      preferredTopics: ['技术', '科学', '编程'],
      avoidTopics: ['政治', '敏感内容'],
      personalityTraits: ['友好', '耐心', '专业']
    }
  },
  {
    id: 'character-002',
    name: '创意写手',
    description: '富有创造力的AI作家，擅长生成故事和创意内容',
    prompt: '你是一个富有创造力的AI作家，擅长生成精彩的故事、诗歌和创意内容。你的作品应该充满想象力，能够打动读者。',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=writer',
    modelType: 'creative',
    tags: ['创意', '写作', '故事'],
    trainingData: [
      {
        content: '用户请求创作一个科幻故事',
        tokens: 350,
        topics: ['科幻', '未来', '冒险'],
        timestamp: Date.now() - 86400000 * 15
      },
      {
        content: '用户需要一首关于自然的诗歌',
        tokens: 120,
        topics: ['诗歌', '自然', '抒情'],
        timestamp: Date.now() - 86400000 * 10
      },
      {
        content: '用户请求创作短篇恐怖故事',
        tokens: 280,
        topics: ['恐怖', '悬疑', '惊悚'],
        timestamp: Date.now() - 86400000 * 5
      }
    ],
    interactionFeatures: {
      responseStyle: 'creative',
      avgResponseLength: 250,
      preferredTopics: ['文学', '艺术', '创意'],
      avoidTopics: ['暴力', '血腥'],
      personalityTraits: ['富有想象力', '情感丰富', '浪漫']
    }
  },
  {
    id: 'character-003',
    name: '编程导师',
    description: '专业的编程指导AI，帮助开发者解决技术问题',
    prompt: '你是一个专业的编程导师，能够帮助开发者解决各种技术问题，包括代码调试、架构设计和最佳实践。你的回答应该清晰、准确，提供可执行的代码示例。',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=coder',
    modelType: 'code',
    tags: ['编程', '技术', '指导'],
    trainingData: [
      {
        content: '用户遇到JavaScript异步编程问题',
        tokens: 220,
        topics: ['JavaScript', '异步', 'Promise'],
        timestamp: Date.now() - 86400000 * 28
      },
      {
        content: '用户询问React最佳实践',
        tokens: 260,
        topics: ['React', '前端', '最佳实践'],
        timestamp: Date.now() - 86400000 * 22
      },
      {
        content: '用户需要数据库设计建议',
        tokens: 190,
        topics: ['数据库', '设计', 'SQL'],
        timestamp: Date.now() - 86400000 * 18
      }
    ],
    interactionFeatures: {
      responseStyle: 'technical',
      avgResponseLength: 200,
      preferredTopics: ['编程', '技术', '架构'],
      avoidTopics: ['非技术问题'],
      personalityTraits: ['严谨', '耐心', '专业']
    }
  }
]

export const mockChatRecords = [
  {
    normal: [
      {
        id: 'msg-001',
        senderId: 1,
        senderName: '用户',
        content: '你好，我想了解一下人工智能的基础知识',
        sendTime: new Date(Date.now() - 3600000).toISOString()
      },
      {
        id: 'msg-002',
        senderId: 2,
        senderName: '智慧助手',
        content: '您好！人工智能是计算机科学的一个分支，致力于研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统。人工智能领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。',
        sendTime: new Date(Date.now() - 3580000).toISOString()
      },
      {
        id: 'msg-003',
        senderId: 1,
        senderName: '用户',
        content: '机器学习和深度学习有什么区别？',
        sendTime: new Date(Date.now() - 3500000).toISOString()
      },
      {
        id: 'msg-004',
        senderId: 2,
        senderName: '智慧助手',
        content: '机器学习是人工智能的一个子集，它使用算法让计算机从数据中学习。而深度学习是机器学习的一个子集，它使用多层神经网络来模拟人脑的学习过程。深度学习在处理复杂模式识别任务（如图像、语音）时表现出色。',
        sendTime: new Date(Date.now() - 3480000).toISOString()
      },
      {
        id: 'msg-005',
        senderId: 1,
        senderName: '用户',
        content: '明白了，谢谢！',
        sendTime: new Date(Date.now() - 3400000).toISOString()
      }
    ],
    specialChars: [
      {
        id: 'msg-sc-001',
        senderId: 1,
        senderName: '用户',
        content: '测试特殊字符：@#$%^&*()_+-=[]{}|;:,.<>?!~`"\'\\',
        sendTime: new Date(Date.now() - 7200000).toISOString()
      },
      {
        id: 'msg-sc-002',
        senderId: 2,
        senderName: '智慧助手',
        content: '我收到了特殊字符测试！这些字符包括：@#$%^&*()_+-=[]{}|;:,.<>?!~`"\'\\',
        sendTime: new Date(Date.now() - 7180000).toISOString()
      },
      {
        id: 'msg-sc-003',
        senderId: 1,
        senderName: '用户',
        content: '中文特殊符号测试：【】《》「」『』（）｛｝〔〕',
        sendTime: new Date(Date.now() - 7100000).toISOString()
      },
      {
        id: 'msg-sc-004',
        senderId: 2,
        senderName: '智慧助手',
        content: '中文特殊符号已收到：【】《》「」『』（）｛｝〔〕',
        sendTime: new Date(Date.now() - 7080000).toISOString()
      },
      {
        id: 'msg-sc-005',
        senderId: 1,
        senderName: '用户',
        content: 'Emoji测试：😊🎉🎈🌟🔥💡💪',
        sendTime: new Date(Date.now() - 7000000).toISOString()
      },
      {
        id: 'msg-sc-006',
        senderId: 2,
        senderName: '智慧助手',
        content: 'Emoji表情收到！😊🎉🎈🌟🔥💡💪',
        sendTime: new Date(Date.now() - 6980000).toISOString()
      }
    ],
    longText: [
      {
        id: 'msg-lt-001',
        senderId: 1,
        senderName: '用户',
        content: '请详细解释一下人工智能的发展历程，包括从早期的符号主义到现代深度学习的演变过程，以及每个阶段的代表性技术和应用。',
        sendTime: new Date(Date.now() - 1800000).toISOString()
      },
      {
        id: 'msg-lt-002',
        senderId: 2,
        senderName: '智慧助手',
        content: '人工智能的发展历程可以分为以下几个阶段：\n\n1. **早期阶段（1956-1974）- 符号主义**\n   - 1956年达特茅斯会议标志着AI诞生\n   - 代表性技术：逻辑推理、专家系统\n   - 代表性应用：通用问题求解器（GPS）、ELIZA聊天机器人\n\n2. **AI寒冬（1974-1980）**\n   - 由于计算能力不足和期望过高，AI研究陷入低谷\n\n3. **知识工程时期（1980-1987）**\n   - 专家系统蓬勃发展\n   - 代表性技术：知识表示、推理引擎\n   - 代表性应用：MYCIN医疗诊断系统\n\n4. **第二次AI寒冬（1987-1993）**\n   - 专家系统维护成本过高，商业应用受限\n\n5. **机器学习兴起（1993-2010）**\n   - 数据量增长推动机器学习发展\n   - 代表性技术：支持向量机、决策树、随机森林\n   - 代表性应用：垃圾邮件过滤、推荐系统\n\n6. **深度学习时代（2010-至今）**\n   - GPU计算能力突破和大数据驱动深度学习发展\n   - 代表性技术：卷积神经网络（CNN）、循环神经网络（RNN）、Transformer\n   - 代表性应用：图像识别、语音助手、自动驾驶、大语言模型\n\n近年来，以GPT、BERT为代表的大语言模型进一步推动了AI的发展，使其在自然语言处理、多模态理解等领域取得了突破性进展。',
        sendTime: new Date(Date.now() - 1750000).toISOString()
      },
      {
        id: 'msg-lt-003',
        senderId: 1,
        senderName: '用户',
        content: '非常详细，谢谢你的解释！我还有一个问题：未来人工智能的发展趋势是什么？会对人类社会产生哪些影响？',
        sendTime: new Date(Date.now() - 1600000).toISOString()
      },
      {
        id: 'msg-lt-004',
        senderId: 2,
        senderName: '智慧助手',
        content: '未来人工智能的发展趋势和影响包括：\n\n**技术趋势：**\n1. **通用人工智能（AGI）**：追求具备人类级别的智能\n2. **多模态AI**：融合视觉、听觉、语言等多种感知能力\n3. **边缘AI**：在设备端运行，保护隐私并降低延迟\n4. **AI安全与对齐**：确保AI行为符合人类价值观\n5. **AI与科学发现**：加速药物研发、气候模拟等科学研究\n\n**社会影响：**\n1. **就业变革**：自动化替代部分工作，同时创造新职业\n2. **教育变革**：个性化学习、AI辅助教学\n3. **医疗改善**：精准诊断、药物研发加速\n4. **交通革命**：自动驾驶改变出行方式\n5. **隐私与安全**：数据隐私保护和AI安全成为重要议题\n\n总体而言，AI将深刻改变人类社会的方方面面，关键在于如何引导其发展，确保技术进步造福全人类。',
        sendTime: new Date(Date.now() - 1550000).toISOString()
      }
    ]
  }
]

export const mockSharePackages = [
  {
    version: '1.0.0',
    type: 'character-with-chat',
    timestamp: Date.now(),
    character: mockAiCharacters[0],
    chatHistory: mockChatRecords[0].normal,
    metadata: {
      characterName: '智慧助手',
      messageCount: 5,
      exportTime: new Date().toISOString()
    }
  },
  {
    version: '1.0.0',
    type: 'character',
    timestamp: Date.now(),
    character: mockAiCharacters[1],
    metadata: {
      characterName: '创意写手',
      exportTime: new Date().toISOString()
    }
  },
  {
    version: '1.0.0',
    type: 'chat-history',
    timestamp: Date.now(),
    messageCount: 6,
    options: { maxMessages: 100, anonymize: false, includeTimestamps: true },
    messages: mockChatRecords[0].specialChars,
    metadata: {
      exportTime: new Date().toISOString()
    }
  }
]

export default {
  mockAiCharacters,
  mockChatRecords,
  mockSharePackages
}
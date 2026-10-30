# -*- coding: utf-8 -*-
"""清理误加的 int extractMemoriesFromMessage 接口方法（保留组织代理 void 版本）"""
import io

p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\service\AiMemoryService.java'
src = io.open(p, encoding='utf-8').read()

mine = '''    /**
     * 从用户聊天消息中自动提炼长期记忆（规则式）并去重落库。
     * 匹配"我叫/我喜欢/我讨厌/我住在/我今年X岁/生日/职业"等模式，
     * 提炼内容写入 ai_memory（source=chat），相同或高度相似内容自动跳过。
     * 返回本次新增记忆条数。
     */
    int extractMemoriesFromMessage(Long userId, String message);

'''
assert mine in src, 'my int version not found'
src = src.replace(mine, '', 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('cleaned; remaining extract declarations:', src.count('extractMemoriesFromMessage'))

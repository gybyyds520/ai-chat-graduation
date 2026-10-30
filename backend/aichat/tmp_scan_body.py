# -*- coding: utf-8 -*-
"""提取控制器中 Map<String,Object> 请求体使用的字段 key，辅助构造注入 body。"""
import io, re, os, glob

base = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\controller'
targets = ['AiCharacterController', 'CommunityController', 'SocialController', 'GroupChatController',
           'FriendGroupController', 'FriendshipController', 'AiElfController', 'MemoryController',
           'PrivacyController', 'PayAccountController', 'ToolController', 'BlockReportController',
           'RecommendationController', 'VoiceCallController', 'VoiceChatController', 'QuotaController']
for t in targets:
    p = os.path.join(base, t + '.java')
    if not os.path.exists(p):
        continue
    txt = io.open(p, encoding='utf-8', errors='replace').read()
    keys = set()
    for m in re.finditer(r'request\.get\("([^"]+)"\)', txt):
        keys.add(m.group(1))
    for m in re.finditer(r'updates\.get\("([^"]+)"\)', txt):
        keys.add(m.group(1))
    for m in re.finditer(r'body\.get\("([^"]+)"\)', txt):
        keys.add(m.group(1))
    if keys:
        print("%-24s %s" % (t, sorted(keys)))

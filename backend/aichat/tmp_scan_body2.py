# -*- coding: utf-8 -*-
import io, re

base = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\controller'
for f in ['AiMemoryController.java', 'KnowledgeBaseController.java']:
    txt = io.open(base + '\\' + f, encoding='utf-8', errors='replace').read()
    print('=====', f)
    for m in re.finditer(r'(\w+)\.get\("(\w+)"\)', txt):
        print('  ', m.group(1) + '.get(' + m.group(2) + ')')
    for m in re.finditer(r'public\s+(\S+)\s+(\w+)\s*\(([^)]*)\)', txt):
        s = ' '.join(m.group(3).split())
        if 'Request' in s or 'MultipartFile' in s or 'File' in s:
            print('   SIG:', m.group(2), '|', s[:140])

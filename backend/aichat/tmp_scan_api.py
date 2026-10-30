# -*- coding: utf-8 -*-
"""扫描所有 Controller 的端点签名，用于规划测试数据注入。"""
import io, re, os, glob, json

base = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\controller'
out = []
for p in sorted(glob.glob(os.path.join(base, '*.java'))):
    name = os.path.basename(p)
    txt = io.open(p, encoding='utf-8', errors='replace').read()
    cls = re.search(r'@RequestMapping\(([^)]*)\)', txt)
    prefix = cls.group(1).strip().strip('"') if cls else ''
    # 找所有映射方法
    for m in re.finditer(
        r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(([^)]*)\))?\s*\n\s*(?:@\w+(?:\([^)]*\))?\s*\n\s*)*public\s+\S+\s+(\w+)\s*\(([^)]*)\)',
        txt):
        method, mapping, fn, params = m.group(1), m.group(2) or '', m.group(3), m.group(4)
        path = prefix
        if mapping.strip():
            mp = re.search(r'"([^"]*)"', mapping)
            if mp:
                path += mp.group(1)
        out.append({'file': name, 'method': method, 'path': path, 'fn': fn,
                    'params': ' '.join(params.split())[:150]})

for e in out:
    print("%-5s %-52s %-24s %s" % (e['method'], e['path'], e['fn'], e['params']))
print("TOTAL:", len(out))

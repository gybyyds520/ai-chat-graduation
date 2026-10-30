# -*- coding: utf-8 -*-
"""盘点后端所有控制器接口 + 前端视图 API 调用，生成基线清单"""
import io, os, re, json

ROOT = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation'
CTRL = os.path.join(ROOT, r'backend\aichat\src\main\java\com\example\aichat\controller')
FRONT = os.path.join(ROOT, r'frontend\src')

# ---------- 后端接口 ----------
result = {}
total = 0
for f in sorted(os.listdir(CTRL)):
    if not f.endswith('.java'):
        continue
    path = os.path.join(CTRL, f)
    src = io.open(path, encoding='utf-8').read()
    m = re.search(r'@RequestMapping\("([^"]+)"\)', src)
    base = m.group(1) if m else ''
    endpoints = []
    for mm in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\("([^"]*)"\)', src):
        method, sub = mm.group(1), mm.group(2)
        endpoints.append((method, base + sub))
        total += 1
    result[f.replace('.java', '')] = endpoints

out = []
out.append('# 后端接口总清单')
out.append(f'控制器数：{len(result)}，接口总数：{total}')
for ctrl, eps in result.items():
    out.append(f'\n## {ctrl}（{len(eps)} 个）')
    for method, url in eps:
        out.append(f'- [{method}] {url}')
with io.open(os.path.join(ROOT, 'backend\\aichat\\tmp_api_inventory.md'), 'w', encoding='utf-8') as fh:
    fh.write('\n'.join(out))
print(f'后端清单写入完成：{len(result)} 控制器 / {total} 接口')

# ---------- 前端视图 API 调用（粗粒度） ----------
views = {}
for f in sorted(os.listdir(os.path.join(FRONT, 'views'))):
    if not f.endswith('.vue'):
        continue
    path = os.path.join(FRONT, 'views', f)
    src = io.open(path, encoding='utf-8').read()
    apis = set(re.findall(r"request\.(get|post|put|delete)\([`'\"](/api/[^`'\"?$)]*)", src))
    # 模板与脚本中的 /api/ 字面量
    apis2 = set(re.findall(r"/api/[A-Za-z0-9_/{}$.\-]+", src))
    views[f.replace('.vue', '')] = sorted(apis2)

with io.open(os.path.join(ROOT, 'backend\\aichat\\tmp_frontend_api_map.md'), 'w', encoding='utf-8') as fh:
    fh.write('# 前端视图 → API 调用映射\n')
    for v, apis in views.items():
        fh.write(f'\n## {v}（{len(apis)} 处）\n')
        for a in apis:
            fh.write(f'- {a}\n')
print(f'前端映射写入完成：{len(views)} 个视图')

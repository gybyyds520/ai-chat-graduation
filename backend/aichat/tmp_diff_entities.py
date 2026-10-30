# -*- coding: utf-8 -*-
"""对比 MyBatis-Plus 实体字段与真实 MySQL 表列，输出差异（缺列清单）。"""
import io, re, os, glob, json, subprocess, sys

SRC = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\entity'

# 1. 扫描实体
entities = []
for p in glob.glob(os.path.join(SRC, '*.java')):
    txt = io.open(p, encoding='utf-8', errors='replace').read()
    m = re.search(r'@TableName\("(\w+)"\)', txt)
    if not m:
        continue
    table = m.group(1)
    fields = []
    for fm in re.finditer(r'private\s+([\w<>.,\s]+?)\s+(\w+)\s*;', txt):
        ftype, fname = fm.group(1).strip(), fm.group(2)
        if fname in ('serialVersionUID',):
            continue
        fields.append((fname, ftype))
    entities.append({'table': table, 'file': os.path.basename(p), 'fields': fields})

print("扫描到实体:", len(entities))
for e in entities:
    print("  %-28s %s (%d字段)" % (e['table'], e['file'], len(e['fields'])))

# 2. 查询真实表列
sql = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ai_chat';"
open(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\tmp_cols.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_cols.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat', text=True, encoding='utf-8', errors='replace')
out = r.stdout
db_cols = {}
for line in out.splitlines():
    line = line.strip()
    if not line or line.startswith('TABLE_NAME') or line.startswith('mysql:'):
        continue
    parts = line.split('\t')
    if len(parts) >= 2:
        db_cols.setdefault(parts[0], set()).add(parts[1].lower())
os.remove(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\tmp_cols.sql')

# 3. 差异
print("\n===== 实体有但表缺少的列 =====")
for e in entities:
    t = e['table']
    if t not in db_cols:
        print("  [表不存在] %s" % t)
        continue
    missing = []
    for fname, ftype in e['fields']:
        col = re.sub(r'(?<!^)(?=[A-Z])', '_', fname).lower()
        if col not in db_cols[t]:
            missing.append((fname, col, ftype))
    if missing:
        print("  %-26s %d列缺失" % (t, len(missing)))
        for fname, col, ftype in missing:
            print("      %-28s -> %-28s (%s)" % (fname, col, ftype))

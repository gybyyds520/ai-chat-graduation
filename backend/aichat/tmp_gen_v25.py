# -*- coding: utf-8 -*-
"""全量扫描：NOT NULL 且无默认值且非自增的列，对比实体是否覆盖；未覆盖则生成 V25 改 NULL。"""
import io, re, os, glob, subprocess

SRC = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\entity'

def c2s(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()

# 实体字段表
entity_fields = {}
for p in glob.glob(os.path.join(SRC, '*.java')):
    txt = io.open(p, encoding='utf-8', errors='replace').read()
    m = re.search(r'@TableName\("(\w+)"\)', txt)
    if not m:
        continue
    table = m.group(1)
    flds = set()
    for fm in re.finditer(r'((?:@\w+(?:\([^)]*\))?\s*\n\s*)*)private\s+([\w<>.,\s]+?)\s+(\w+)\s*;', txt):
        annot, ftype, fname = fm.group(1), fm.group(2).strip(), fm.group(3)
        if 'exist = false' in annot or 'exist=false' in annot or fname == 'serialVersionUID' or '<' in ftype:
            continue
        # 显式 @TableField("col") 优先，否则驼峰转蛇形
        colm = re.search(r'@TableField\("([^"]+)"\)', annot)
        if colm:
            flds.add(colm.group(1).replace('`', '').lower())
        else:
            flds.add(c2s(fname))
    entity_fields[table] = flds

sql = """SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='ai_chat' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT IS NULL
AND EXTRA NOT LIKE '%auto_increment%';"""
open('tmp_nn.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_nn.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat',
                   text=True, encoding='utf-8', errors='replace')
os.remove('tmp_nn.sql')

issues = []
for line in r.stdout.splitlines():
    line = line.strip()
    if not line or line.startswith('TABLE_NAME') or line.startswith('mysql:'):
        continue
    parts = line.split('\t')
    if len(parts) < 3:
        continue
    t, col, ctype = parts[0], parts[1].lower(), parts[2]
    flds = entity_fields.get(t)
    if flds is None:
        continue  # 无对应实体，不处理
    if col in flds:
        continue  # 实体覆盖
    issues.append((t, col, ctype))

print("实体未覆盖的 NOT NULL 无默认列: %d 处\n" % len(issues))
from collections import defaultdict
by_t = defaultdict(list)
for t, col, ctype in issues:
    by_t[t].append((col, ctype))
for t in sorted(by_t):
    print("  %-26s: %s" % (t, ", ".join("%s(%s)" % (c, ty) for c, ty in by_t[t])))

# 生成 V25
lines = []
lines.append("-- ============================================================")
lines.append("-- V25: 旧架构遗留 NOT NULL 列改为可空（实体不维护这些列）")
lines.append("-- 幂等：仅当列为 NOT NULL 时执行 MODIFY")
lines.append("-- ============================================================")
lines.append("")
for t in sorted(by_t):
    for col, ctype in by_t[t]:
        lines.append("SET @nn_%s_%s := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='%s' AND COLUMN_NAME='%s' AND IS_NULLABLE='NO');" % (t, col, t, col))
        lines.append("SET @ddl_%s_%s := IF(@nn_%s_%s>0, 'ALTER TABLE `%s` MODIFY `%s` %s NULL', 'SELECT 1');" % (t, col, t, col, t, col, ctype))
        lines.append("PREPARE s_%s_%s FROM @ddl_%s_%s;" % (t, col, t, col))
        lines.append("EXECUTE s_%s_%s;" % (t, col))
        lines.append("DEALLOCATE PREPARE s_%s_%s;" % (t, col))
        lines.append("")
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\resources\db\migration\V25__nullable_legacy_columns.sql'
io.open(out, 'w', encoding='utf-8', newline='\n').write("\n".join(lines))
print("\n已生成:", out)

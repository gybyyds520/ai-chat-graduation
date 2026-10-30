# -*- coding: utf-8 -*-
"""修正版：排除 @TableField(exist=false) 后输出精确缺列，并生成 V24 补列 SQL。"""
import io, re, os, glob, subprocess

SRC = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\java\com\example\aichat\entity'

TYPE_MAP = {
    'Long': 'BIGINT',
    'long': 'BIGINT',
    'String': 'VARCHAR(255)',
    'Integer': 'INT',
    'int': 'INT',
    'LocalDateTime': 'DATETIME',
    'LocalDate': 'DATE',
    'Boolean': 'TINYINT(1)',
    'boolean': 'TINYINT(1)',
    'Double': 'DOUBLE',
    'double': 'DOUBLE',
    'BigDecimal': 'DECIMAL(20,2)',
    'Float': 'FLOAT',
}

entities = []
for p in glob.glob(os.path.join(SRC, '*.java')):
    txt = io.open(p, encoding='utf-8', errors='replace').read()
    m = re.search(r'@TableName\("(\w+)"\)', txt)
    if not m:
        continue
    table = m.group(1)
    fields = []
    # 按字段块切分：匹配 注解块 + private 声明
    for fm in re.finditer(r'((?:@\w+(?:\([^)]*\))?\s*\n\s*)*)private\s+([\w<>.,\s]+?)\s+(\w+)\s*;', txt):
        annot, ftype, fname = fm.group(1), fm.group(2).strip(), fm.group(3)
        if 'exist = false' in annot or 'exist=false' in annot:
            continue
        if fname == 'serialVersionUID':
            continue
        # 泛型或未知类型跳过（如 List<X>、Map）
        if '<' in ftype or '[' in ftype:
            continue
        fields.append((fname, ftype, annot.strip().split('\n')[0][:30] if annot.strip() else ''))
    entities.append({'table': table, 'file': os.path.basename(p), 'fields': fields})

sql = "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ai_chat';"
open(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\tmp_cols.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_cols.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat',
                   text=True, encoding='utf-8', errors='replace')
db_cols = {}
for line in r.stdout.splitlines():
    line = line.strip()
    if not line or line.startswith('TABLE_NAME') or line.startswith('mysql:'):
        continue
    parts = line.split('\t')
    if len(parts) >= 2:
        db_cols.setdefault(parts[0], set()).add(parts[1].lower())
os.remove(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\tmp_cols.sql')

def camel2snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()

missing_all = []
for e in entities:
    t = e['table']
    if t not in db_cols:
        continue
    for fname, ftype, ann in e['fields']:
        col = camel2snake(fname)
        if col not in db_cols[t]:
            missing_all.append((t, fname, col, ftype))

print("精确缺列总数: %d (跨 %d 张表)" % (len(missing_all), len(set(x[0] for x in missing_all))))
by_table = {}
for t, fname, col, ftype in missing_all:
    by_table.setdefault(t, []).append((fname, col, ftype))
for t in sorted(by_table):
    print("  %-26s %d列: %s" % (t, len(by_table[t]), ", ".join(c for _, c, _ in by_table[t])))

# 生成 V24 SQL
lines = []
lines.append("-- ============================================================")
lines.append("-- V24: 实体与真实表结构对齐（补齐历史迁移遗漏列，幂等）")
lines.append("-- 由实体-表结构差异扫描自动生成，可重复执行")
lines.append("-- ============================================================")
lines.append("")
for t in sorted(by_table):
    for fname, col, ftype in by_table[t]:
        sqltype = TYPE_MAP.get(ftype)
        if not sqltype:
            sqltype = 'VARCHAR(255)'
        colref = '`%s`' % col
        lines.append("SET @has_%s_%s := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '%s' AND COLUMN_NAME = '%s');" % (t, col, t, col))
        lines.append("SET @ddl_%s_%s := IF(@has_%s_%s = 0, 'ALTER TABLE `%s` ADD COLUMN %s %s NULL', 'SELECT 1');" % (t, col, t, col, t, colref, sqltype))
        lines.append("PREPARE s_%s_%s FROM @ddl_%s_%s;" % (t, col, t, col))
        lines.append("EXECUTE s_%s_%s;" % (t, col))
        lines.append("DEALLOCATE PREPARE s_%s_%s;" % (t, col))
        lines.append("")
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat\src\main\resources\db\migration\V24__align_entity_schema.sql'
io.open(out, 'w', encoding='utf-8', newline='\n').write("\n".join(lines))
print("\n已生成:", out)

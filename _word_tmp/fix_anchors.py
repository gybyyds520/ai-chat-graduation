# -*- coding: utf-8 -*-
import zipfile, sys, os, shutil
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
BAK = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\3.审核表_修锚点前备份.docx'

if not os.path.exists(BAK):
    shutil.copy2(TARGET, BAK)
    print('已备份 ->', BAK)

z = zipfile.ZipFile(TARGET)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

def para_text(para):
    return ''.join(x.text or '' for x in para.iter(W + 't'))

# 1) 移除当前误放的 id=14/15 引用 run（在任意段落中查找并删除）
removed = 0
for para in list(body.iter(W + 'p')):
    for r in list(para.findall(W + 'r')):
        refel = r.find(W + 'commentReference')
        if refel is not None and refel.get(W + 'id') in ('14', '15'):
            para.remove(r)
            removed += 1
print('移除误放引用:', removed)

# 2) 在目标段落中按精确偏移插入
MOVE = [
    ('14', '（2）AI角色引擎'),
    ('15', '（3）AI记忆系统'),
]
for cid, prefix in MOVE:
    done = False
    for para in body.iter(W + 'p'):
        text = para_text(para)
        idx = text.find(prefix)
        if idx < 0:
            continue
        # 找到包含 idx 的 run
        runs = para.findall(W + 'r')
        acc = 0
        target_run = None
        for r in runs:
            t = ''.join(x.text or '' for x in r.iter(W + 't'))
            if acc <= idx < acc + len(t) or (acc == idx and len(t) > 0):
                target_run = r
                break
            acc += len(t)
        if target_run is None:
            continue
        # 在 target_run 前插入引用 run
        newr = etree.Element(W + 'r')
        ref = etree.SubElement(newr, W + 'commentReference')
        ref.set(W + 'id', cid)
        target_run.addprevious(newr)
        print('已重新插入 id=%s -> "%s" (段落: %s...)' % (cid, prefix, text[:24]))
        done = True
        break
    if not done:
        print('失败: %s 未找到' % prefix)

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, TARGET)
print('已保存')

# 3) 回读验证
z2 = zipfile.ZipFile(TARGET)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
from collections import Counter
c = Counter(etree.QName(el).localname for el in b2.iter())
print('reference:', c.get('commentReference', 0), '| rangeStart:', c.get('commentRangeStart', 0))
for para in b2.iter(W + 'p'):
    t = para_text(para)
    refs = []
    for r in para.findall(W + 'r'):
        refel = r.find(W + 'commentReference')
        if refel is not None:
            refs.append(refel.get(W + 'id'))
    if refs:
        print('  段落(%s...): 引用id=%s' % (t[:30], refs))

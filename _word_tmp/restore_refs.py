# -*- coding: utf-8 -*-
import zipfile, sys, os, shutil
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
BAK = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\3.审核表_补引用前备份.docx'

if not os.path.exists(BAK):
    shutil.copy2(TARGET, BAK)
    print('已备份 ->', BAK)

z = zipfile.ZipFile(TARGET)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

def para_text(para):
    return ''.join(x.text or '' for x in para.iter(W + 't'))

# 目标：研究内容前三项段落
ANCHORS = [
    ('13', '（1）系统总体架构设计'),
    ('14', '（2）AI角色引擎'),
    ('15', '（3）AI记忆系统'),
]
inserted = []
for cid, prefix in ANCHORS:
    target = None
    for para in body.iter(W + 'p'):
        if para_text(para).strip().startswith(prefix):
            target = para
            break
    if target is None:
        print('未找到锚点段:', prefix)
        continue
    # 在 pPr 之后、第一个 run 之前插入 <w:r><w:commentReference w:id="cid"/></w:r>
    r = etree.SubElement(target, W + 'r')
    ref = etree.SubElement(r, W + 'commentReference')
    ref.set(W + 'id', cid)
    # 移到段落开头（pPr 后）
    ppr = target.find(W + 'pPr')
    if ppr is not None:
        ppr.addnext(r)
    else:
        target.insert(0, r)
    inserted.append(cid)
    print('已插入批注引用 id=%s -> %s...' % (cid, prefix))

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, TARGET)
print('已保存')

# 回读验证
z2 = zipfile.ZipFile(TARGET)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
from collections import Counter
c = Counter(etree.QName(el).localname for el in b2.iter())
print('reference:', c.get('commentReference', 0), '| rangeStart:', c.get('commentRangeStart', 0), '| rangeEnd:', c.get('commentRangeEnd', 0))
ids = [el.get(W + 'id') for el in b2.iter() if etree.QName(el).localname == 'commentReference']
print('引用批注id:', sorted(ids, key=int))
cr = etree.fromstring(z2.read('word/comments.xml'))
print('批注本体:', len(list(cr)))

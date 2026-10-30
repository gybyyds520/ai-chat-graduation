# -*- coding: utf-8 -*-
import zipfile, sys, os, shutil
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
BAK = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\3.审核表_去标记前备份.docx'

# 1) 备份（仅工作目录，不影响交付目录）
if not os.path.exists(BAK):
    shutil.copy2(TARGET, BAK)
    print('已备份 ->', BAK)

z = zipfile.ZipFile(TARGET)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

rm_range = 0
for el in list(body.iter()):
    tag = etree.QName(el).localname
    if tag in ('commentRangeStart', 'commentRangeEnd'):
        parent = el.getparent()
        if parent is not None:
            parent.remove(el)
            rm_range += 1
print('删除范围标记:', rm_range)

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, TARGET)
print('已原地写回:', TARGET)

# 回读验证
z2 = zipfile.ZipFile(TARGET)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
from collections import Counter
c = Counter(etree.QName(el).localname for el in b2.iter())
print('残留 rangeStart/End:', c.get('commentRangeStart', 0), c.get('commentRangeEnd', 0))
print('保留 commentReference:', c.get('commentReference', 0))
cr = etree.fromstring(z2.read('word/comments.xml'))
print('批注本体:', len(list(cr)))

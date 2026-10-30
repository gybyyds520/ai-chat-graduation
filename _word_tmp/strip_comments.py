# -*- coding: utf-8 -*-
import zipfile, sys, os, shutil
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
SRC = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
DST = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表（无批注提交版）.docx'
BAK = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\3.审核表_原始备份.docx'

# 备份原文件
if not os.path.exists(BAK):
    shutil.copy2(SRC, BAK)
    print('已备份原文件 ->', BAK)

z = zipfile.ZipFile(SRC)
names = z.namelist()

# 1) document.xml: 移除批注标记元素
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
removed = 0
for el in list(body.iter()):
    tag = etree.QName(el).localname
    if tag in ('commentRangeStart', 'commentRangeEnd', 'commentReference'):
        parent = el.getparent()
        if parent is not None:
            parent.remove(el)
            removed += 1
print('document.xml 移除批注标记:', removed)

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)

# 2) comments.xml: 清空批注（保留合法空元素）
def empty_comments(xml_bytes):
    try:
        cr = etree.fromstring(xml_bytes)
    except Exception:
        return None
    for child in list(cr):
        cr.remove(child)
    return etree.tostring(cr, xml_declaration=True, encoding='UTF-8', standalone=True)

# 3) 写入新文件
with zipfile.ZipFile(DST, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in names:
        data = z.read(n)
        if n == 'word/document.xml':
            data = new_doc
        elif n == 'word/comments.xml':
            ec = empty_comments(data)
            if ec is not None:
                data = ec
        w.writestr(n, data)
z.close()
print('已生成无批注版:', DST)

# 4) 回读验证
z2 = zipfile.ZipFile(DST)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
cnt = 0
for el in b2.iter():
    if etree.QName(el).localname in ('commentRangeStart', 'commentRangeEnd', 'commentReference'):
        cnt += 1
print('新文件残留批注标记:', cnt)
c = etree.fromstring(z2.read('word/comments.xml'))
print('comments.xml 批注数:', len(list(c)))

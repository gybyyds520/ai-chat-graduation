# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
# 定位封面表格（第一个 tbl）
tbl = body.find(W + 'tbl')
print('表格行数:', len(tbl.findall(W + 'tr')))
rows = tbl.findall(W + 'tr')
# 打印每行文本，找到空行（题目下方空行）
for ri, tr in enumerate(rows[:6]):
    texts = []
    for tc in tr.findall(W + 'tc'):
        t = ''.join(x.text or '' for x in tc.iter(W + 't'))
        texts.append(t[:24])
    print('行', ri, texts)
# 找到"空行"：两个单元格都无文本的行（在题目行之后）
target = None
for tr in rows[1:4]:
    tcs = tr.findall(W + 'tc')
    all_empty = all(''.join(x.text or '' for x in tc.iter(W + 't')).strip() == '' for tc in tcs)
    if all_empty:
        target = tr
        break
if target is not None:
    tbl.remove(target)
    print('已删除空行（两个空单元格的行）')
else:
    print('未找到空行！')
# 验证行1 题目单元格
rows2 = tbl.findall(W + 'tr')
print('删除后表格行数:', len(rows2))
for ri, tr in enumerate(rows2[:5]):
    texts = []
    for tc in tr.findall(W + 'tc'):
        t = ''.join(x.text or '' for x in tc.iter(W + 't'))
        texts.append(t[:30])
    print('行', ri, texts)
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = p + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
import os
os.replace(tmp, p)
print('已保存:', p)

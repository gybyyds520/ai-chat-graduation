# -*- coding: utf-8 -*-
import zipfile, sys, re
sys.stdout = sys.__stdout__
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
z = zipfile.ZipFile(p)
rels = z.read('word/_rels/document.xml.rels').decode('utf-8')
for m in re.finditer(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels):
    print(m.group(1), '->', m.group(2))
print()
d = z.read('word/document.xml').decode('utf-8')
print('blip 引用:', re.findall(r'r:embed="([^"]+)"', d))
print('docPr id:', re.findall(r'<wp:docPr id="(\d+)"', d))
print('绘图数:', d.count('<w:drawing>'))
print('表格数:', d.count('<w:tbl>'))
# 检查是否有 proofErr 在段7
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
from lxml import etree
root = etree.fromstring(d.encode('utf-8'))
paras = list(root.iter(W + 'p'))
for i in range(6, 8):
    xml = etree.tostring(paras[i]).decode('utf-8')
    print('段', i, 'proofErr数:', xml.count('proofErr'), '| bookmark数:', xml.count('bookmark'))
    print('段', i, '尾部120字符:', xml[-120:])

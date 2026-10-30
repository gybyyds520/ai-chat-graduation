# -*- coding: utf-8 -*-
import zipfile, sys, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\fix_no_kernlang.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
# 删除所有 run rPr 和 pPr rPr 中的 kern 与 lang（含 bidi=ar-SA）
removed_kern = 0
removed_lang = 0
for rpr in root.iter(W + 'rPr'):
    for kern in rpr.findall(W + 'kern'):
        rpr.remove(kern)
        removed_kern += 1
    for lang in rpr.findall(W + 'lang'):
        rpr.remove(lang)
        removed_lang += 1
print('删除 kern:', removed_kern, '| lang:', removed_lang)
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
print('写出:', out)

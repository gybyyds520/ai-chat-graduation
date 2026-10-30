# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
for label, p in [
    ('当前', r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'),
    ('修复', r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\repaired_test.docx'),
]:
    z = zipfile.ZipFile(p)
    root = etree.fromstring(z.read('word/document.xml'))
    paras = list(root.iter(W + 'p'))
    print('=====', label, '=====')
    for i in range(0, 12):
        t = ''.join(x.text or '' for x in paras[i].iter(W + 't'))
        jc = paras[i].find('.//' + W + 'jc')
        ppr_children = [etree.QName(c).localname for c in paras[i].find(W + 'pPr')] if paras[i].find(W + 'pPr') is not None else []
        print(i, repr(t[:35]), '| jc:', jc.get(W + 'val') if jc is not None else '-', '| pPr:', ppr_children)
    print()

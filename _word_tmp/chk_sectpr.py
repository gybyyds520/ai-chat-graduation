# -*- coding: utf-8 -*-
import zipfile, sys, re
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
for label, p in [
    ('当前成品', r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'),
    ('修复版', r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\repaired_test.docx'),
    ('模板', r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx'),
]:
    z = zipfile.ZipFile(p)
    root = etree.fromstring(z.read('word/document.xml'))
    body = root.find(W+'body')
    # body 末尾 sectPr
    body_end = body.find(W+'sectPr') is not None
    # 段落 pPr 内 sectPr
    in_para = []
    for i, para in enumerate(list(root.iter(W+'p'))):
        ppr = para.find(W+'pPr')
        if ppr is not None and ppr.find(W+'sectPr') is not None:
            t = ''.join(x.text or '' for x in para.iter(W+'t'))
            in_para.append((i, t[:25]))
    print(label, '| body末尾sectPr:', body_end, '| 段内sectPr数:', len(in_para), '| 位置:', in_para)

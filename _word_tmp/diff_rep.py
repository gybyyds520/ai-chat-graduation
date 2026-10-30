# -*- coding: utf-8 -*-
import zipfile, sys, re, hashlib
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
cur = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
rep = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\repaired_test.docx'
zc = zipfile.ZipFile(cur); zr = zipfile.ZipFile(rep)
dc = etree.fromstring(zc.read('word/document.xml'))
dr = etree.fromstring(zr.read('word/document.xml'))
print('当前 成员数:', len(zc.namelist()), '| 修复版 成员数:', len(zr.namelist()))
print('当前 media:', [n for n in zc.namelist() if 'media' in n])
print('修复 media:', [n for n in zr.namelist() if 'media' in n])
# 批注数
print('当前批注数:', zc.read('word/comments.xml').decode('utf-8').count('<w:comment ') if 'word/comments.xml' in zc.namelist() else 0)
print('修复批注数:', zr.read('word/comments.xml').decode('utf-8').count('<w:comment ') if 'word/comments.xml' in zr.namelist() else 0)
# 段落数
print('当前段落:', len(list(dc.iter(W+'p'))), '| 修复段落:', len(list(dr.iter(W+'p'))))
# 文本 diff（段落级）
tc = [''.join(x.text or '' for x in p.iter(W+'t')) for p in dc.iter(W+'p')]
tr = [''.join(x.text or '' for x in p.iter(W+'t')) for p in dr.iter(W+'p')]
if len(tc) == len(tr):
    diffs = [(i, tc[i], tr[i]) for i in range(len(tc)) if tc[i] != tr[i]]
    print('段落文本差异数:', len(diffs))
    for i, a, b in diffs[:15]:
        print(' 段', i, '当前:', repr(a[:40]), '| 修复:', repr(b[:40]))
else:
    print('段落数不同，查找最早差异:')
    for i in range(min(len(tc), len(tr))):
        if tc[i] != tr[i]:
            print(' 首个差异 段', i, '当前:', repr(tc[i][:40]), '| 修复:', repr(tr[i][:40]))
            break
# docPr
print('当前 docPr:', re.findall(r'<wp:docPr id="(\d+)"', zc.read('word/document.xml').decode('utf-8')))
print('修复 docPr:', re.findall(r'<wp:docPr id="(\d+)"', zr.read('word/document.xml').decode('utf-8')))

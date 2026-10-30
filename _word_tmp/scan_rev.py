# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

def scan(path, label):
    z = zipfile.ZipFile(path)
    root = etree.fromstring(z.read('word/document.xml'))
    body = root.find(W + 'body')
    print('=== %s ===' % label)
    counts = {}
    for el in body.iter():
        tag = etree.QName(el).localname
        if tag in ('ins', 'del', 'rPrChange', 'pPrChange', 'tblPrChange', 'trPrChange', 'tcPrChange', 'moveFrom', 'moveTo'):
            counts[tag] = counts.get(tag, 0) + 1
    print('  修订元素:', counts if counts else '无')
    if counts:
        # 显示带修订的文本片段
        for el in body.iter():
            if etree.QName(el).localname in ('ins', 'del'):
                t = ''.join(x.text or '' for x in el.iter(W + 't'))
                if t.strip():
                    print('    %s: %s' % (etree.QName(el).localname, t[:60]))
    # 批注
    c = 0
    for el in body.iter():
        if etree.QName(el).localname in ('commentRangeStart', 'commentReference'):
            c += 1
    print('  批注标记数:', c)

scan(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx', '审核表成品')

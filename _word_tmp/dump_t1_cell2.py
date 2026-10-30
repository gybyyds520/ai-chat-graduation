# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
tbl = body.find(W + 'tbl')
rows = tbl.findall(W + 'tr')
for ri, tr in enumerate(rows):
    tcs = tr.findall(W + 'tc')
    for ti, tc in enumerate(tcs):
        full = ''.join(x.text or '' for x in tc.iter(W + 't'))
        if full.startswith('1.课题意义'):
            print('=== 内容单元格 行%d tc#%d ===' % (ri, ti))
            for pi, para in enumerate(tc.findall(W + 'p')):
                t = ''.join(x.text or '' for x in para.iter(W + 't'))
                jc = para.find('.//' + W + 'jc')
                ind = para.find('.//' + W + 'ind')
                runs = [( ''.join(x.text or '' for x in r.iter(W + 't'))[:25],
                          (r.find('.//' + W + 'sz').get(W + 'val') if r.find('.//' + W + 'sz') is not None else '-')) for r in para.findall(W + 'r')]
                print(' 段%d: %r | jc=%s firstLine=%s | runs=%s' % (pi, t[:35], jc.get(W + 'val') if jc is not None else '-', ind.get(W + 'firstLine') if ind is not None else '-', runs[:6]))
            break

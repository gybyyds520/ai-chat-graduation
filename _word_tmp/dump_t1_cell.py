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
for tr in tbl.findall(W + 'tr'):
    tcs = tr.findall(W + 'tc')
    for ti, tc in enumerate(tcs):
        full = ''.join(x.text or '' for x in tc.iter(W + 't'))
        if '课题意义' in full:
            print('=== 课题意义单元格（tc #%d）=== ' % ti)
            for pi, para in enumerate(tc.findall(W + 'p')):
                t = ''.join(x.text or '' for x in para.iter(W + 't'))
                jc = para.find('.//' + W + 'jc')
                ind = para.find('.//' + W + 'ind')
                runs = []
                for r in para.findall(W + 'r'):
                    tt = ''.join(x.text or '' for x in r.iter(W + 't'))
                    sz = r.find('.//' + W + 'sz')
                    runs.append((tt[:20], sz.get(W + 'val') if sz is not None else '-'))
                print(' 段%d: %r | jc=%s ind=%s | runs=%s' % (pi, t[:30], jc.get(W + 'val') if jc is not None else '-', ind.get(W + 'firstLine') if ind is not None else '-', runs))
            break
    else:
        continue
    break

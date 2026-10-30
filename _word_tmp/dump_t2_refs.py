# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

def find_paras_with(container, keyword):
    out = []
    for pi, para in enumerate(container.iter(W + 'p')):
        t = ''.join(x.text or '' for x in para.iter(W + 't'))
        if keyword in t:
            out.append((pi, para, t))
    return out

# 定位"7.主要参考文献"及文献段落
hits = find_paras_with(body, '主要参考文献')
print('主要参考文献段落数:', len(hits))
if hits:
    pi, para, t = hits[-1]
    print('标题段文本:', t[:50])
    # 打印该段 pPr 的 ind/jc
    pPr = para.find(W + 'pPr')
    if pPr is not None:
        ind = pPr.find(W + 'ind')
        jc = pPr.find(W + 'jc')
        print('标题 pPr: ind=%s jc=%s' % (etree.tostring(ind).decode() if ind is not None else '-', jc.get(W + 'val') if jc is not None else '-'))

# 找 [12] 文献段（最后一个 [n] 开头段落）
refs = []
for pi, para in enumerate(body.iter(W + 'p')):
    t = ''.join(x.text or '' for x in para.iter(W + 't')).strip()
    if t.startswith('[') and '] ' in t[:6] and len(t) > 15:
        refs.append((pi, para, t))
print('\n文献段数量:', len(refs))
for pi, para, t in refs[-3:]:
    print('  idx=%d 文本: %s' % (pi, t[:60]))
    pPr = para.find(W + 'pPr')
    if pPr is not None:
        ind = pPr.find(W + 'ind')
        jc = pPr.find(W + 'jc')
        spacing = pPr.find(W + 'spacing')
        print('    pPr: ind=%s jc=%s spacing=%s' % (
            etree.tostring(ind).decode() if ind is not None else '-',
            jc.get(W + 'val') if jc is not None else '-',
            etree.tostring(spacing).decode() if spacing is not None else '-'))
    # 第一个 run 的 rPr
    r = para.find(W + 'r')
    if r is not None:
        rPr = r.find(W + 'rPr')
        print('    run rPr:', etree.tostring(rPr).decode() if rPr is not None else '-')

# -*- coding: utf-8 -*-
"""按批注要求修正 T2 格式：一级标题段距、二级标题段距、参考文献悬挂缩进+五号+段前3磅"""
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

T2 = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-XXX-开题报告（填写版）.docx'
doc = Document(T2)
root = doc.element
W = qn('w:')
q = lambda t: qn('w:' + t)

import re

def ptext(p):
    return ''.join(t.text or '' for t in p.iter(q('t')))

def set_spacing(p, before, beforeLines, after, afterLines):
    pPr = p.find(q('pPr'))
    if pPr is None:
        pPr = OxmlElement('w:pPr'); p.insert(0, pPr)
    sp = pPr.find(q('spacing'))
    if sp is None:
        sp = OxmlElement('w:spacing'); pPr.append(sp)
    sp.set(q('before'), str(before))
    sp.set(q('beforeLines'), str(beforeLines))
    sp.set(q('after'), str(after))
    sp.set(q('afterLines'), str(afterLines))

def set_hanging(p):
    pPr = p.find(q('pPr'))
    if pPr is None:
        pPr = OxmlElement('w:pPr'); p.insert(0, pPr)
    ind = pPr.find(q('ind'))
    if ind is None:
        ind = OxmlElement('w:ind'); pPr.append(ind)
    ind.set(q('left'), '0')
    ind.set(q('hanging'), '100')
    ind.set(q('hangingChars'), '100')
    # 删除 firstLine/firstLineChars 避免冲突
    for a in ('firstLine', 'firstLineChars'):
        if ind.get(q(a)) is not None:
            del ind.attrib[q(a)]

def set_keep_lines(p):
    pPr = p.find(q('pPr'))
    if pPr is None:
        pPr = OxmlElement('w:pPr'); p.insert(0, pPr)
    if pPr.find(q('keepLines')) is None:
        kl = OxmlElement('w:keepLines'); pPr.append(kl)

def set_run_font(p, east_asia='宋体', sz=21):
    """每个 run 设置 eastAsia 字体与字号"""
    for r in p.findall(q('r')):
        rPr = r.find(q('rPr'))
        if rPr is None:
            rPr = OxmlElement('w:rPr'); r.insert(0, rPr)
        fonts = rPr.find(q('rFonts'))
        if fonts is None:
            fonts = OxmlElement('w:rFonts'); rPr.insert(0, fonts)
        fonts.set(q('eastAsia'), east_asia)
        # sz
        szEl = rPr.find(q('sz'))
        if szEl is None:
            szEl = OxmlElement('w:sz'); rPr.append(szEl)
        szEl.set(q('val'), str(sz))
        szCs = rPr.find(q('szCs'))
        if szCs is None:
            szCs = OxmlElement('w:szCs'); rPr.append(szCs)
        szCs.set(q('val'), str(sz))

tbls = root.findall('.//' + q('tbl'))
bigcell = tbls[1].findall(q('tr'))[1].findall(q('tc'))[0]
ps = bigcell.findall(q('p'))

h1 = re.compile(r'^[1-7]\. ')
h2 = re.compile(r'^[1-7]\.[1-9] ')

for p in ps:
    t = ptext(p).strip()
    if not t:
        continue
    if h1.match(t):
        set_spacing(p, 164, 50, 164, 50)
    elif h2.match(t):
        set_spacing(p, 164, 50, 164, 50)

# 参考文献段 [1]-[12]：悬挂缩进 + 段前3磅 + 五号宋体 + keepLines
ref_re = re.compile(r'^\[\d{1,2}\] ')
for p in ps:
    t = ptext(p).strip()
    if ref_re.match(t):
        set_hanging(p)
        set_spacing(p, 60, 0, 0, 0)  # 段前3磅(60twips)，段后0
        set_keep_lines(p)
        set_run_font(p)

doc.save(T2)
print('T2 format fixed')

# 验证
import zipfile
from lxml import etree
root2 = etree.fromstring(zipfile.ZipFile(T2).read('word/document.xml'))
bigcell2 = root2.findall('.//' + q('tbl'))[1].findall(q('tr'))[1].findall(q('tc'))[0]
ps2 = bigcell2.findall(q('p'))
for p in ps2:
    t = ptext(p).strip()
    if t.startswith('1. 选题背景') or t.startswith('2. 国内外') or t.startswith('1.1') or t.startswith('[1]') or t.startswith('[7]') or t.startswith('[12]'):
        pPr = p.find(q('pPr'))
        sp = pPr.find(q('spacing')) if pPr is not None else None
        ind = pPr.find(q('ind')) if pPr is not None else None
        rPr = p.find(q('r'))
        rPrEl = rPr.find(q('rPr')) if rPr is not None else None
        sz = rPrEl.find(q('sz')) if rPrEl is not None else None
        print(t[:14], '| spacing:', {k.split('}')[1]: v for k, v in sp.attrib.items()} if sp is not None else None,
              '| ind:', {k.split('}')[1]: v for k, v in ind.attrib.items()} if ind is not None else None,
              '| sz:', sz.get(q('val')) if sz is not None else None)

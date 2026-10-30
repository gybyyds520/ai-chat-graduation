# -*- coding: utf-8 -*-
"""对照原模板对应段格式"""
import zipfile
from lxml import etree

W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

def load(path):
    return etree.fromstring(zipfile.ZipFile(path).read('word/document.xml'))

def ptext(p):
    return ''.join(t.text or '' for t in p.iter(W + 't'))

def runinfo(p):
    out = {}
    pPr = p.find(W + 'pPr')
    if pPr is not None:
        st = pPr.find(W + 'pStyle')
        if st is not None:
            out['style'] = st.get(W + 'val')
        ind = pPr.find(W + 'ind')
        if ind is not None:
            out['ind'] = {k.split('}')[1]: v for k, v in ind.attrib.items()}
        sp = pPr.find(W + 'spacing')
        if sp is not None:
            out['spacing'] = {k.split('}')[1]: v for k, v in sp.attrib.items()}
    r = p.find(W + 'r')
    if r is not None:
        rPr = r.find(W + 'rPr')
        if rPr is not None:
            fonts = rPr.find(W + 'rFonts')
            if fonts is not None:
                out['fonts'] = {k.split('}')[1]: v for k, v in fonts.attrib.items()}
            sz = rPr.find(W + 'sz')
            if sz is not None:
                out['sz'] = sz.get(W + 'val')
            b = rPr.find(W + 'b')
            if b is not None:
                out['bold'] = True
    return out

print('############ 原模板 T2 关键段格式 ############')
root = load(r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx')
tbls = root.findall('.//' + W + 'tbl')
bigcell = tbls[1].findall(W + 'tr')[1].findall(W + 'tc')[0]
ps = bigcell.findall(W + 'p')
for idx in [0, 7, 24, 49, 50, 53, 54, 56]:
    print('=== 原模板段', idx, ':', ptext(ps[idx]).strip()[:20])
    print(runinfo(ps[idx]))

print()
print('############ 原模板 styles.xml 中相关样式 ############')
z = zipfile.ZipFile(r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx')
styles = etree.fromstring(z.read('word/styles.xml'))
for s in styles.findall(W + 'style'):
    sid = s.get(W + 'styleId')
    name_el = s.find(W + 'name')
    nm = name_el.get(W + 'val') if name_el is not None else ''
    if nm in ('a', 'a3', 'a4', 'Normal', '正文', '标题 1', 'Title'):
        pass
    # 打印所有样式名便于识别
    if nm and (sid in ('1', '2', '3', '4', 'a', 'a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7', 'a8', 'a9', 'a10', 'a11', 'a12', 'a13') or 'Char' in sid or 'ref' in nm.lower()):
        print('--- style:', sid, nm)
        pPr = s.find(W + 'pPr')
        if pPr is not None:
            sp = pPr.find(W + 'spacing')
            if sp is not None:
                print('   spacing:', {k.split('}')[1]: v for k, v in sp.attrib.items()})
            ind = pPr.find(W + 'ind')
            if ind is not None:
                print('   ind:', {k.split('}')[1]: v for k, v in ind.attrib.items()})
        rPr = s.find(W + 'rPr')
        if rPr is not None:
            fonts = rPr.find(W + 'rFonts')
            if fonts is not None:
                print('   fonts:', {k.split('}')[1]: v for k, v in fonts.attrib.items()})
            sz = rPr.find(W + 'sz')
            if sz is not None:
                print('   sz:', sz.get(W + 'val'))

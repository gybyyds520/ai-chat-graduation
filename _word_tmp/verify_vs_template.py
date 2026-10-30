# -*- coding: utf-8 -*-
"""对照模板批注要求，核验两份填写版的格式与内容"""
import zipfile, re
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

print('############ T2 开题报告 格式核验 ############')
root = load(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-XXX-开题报告（填写版）.docx')
tbls = root.findall('.//' + W + 'tbl')
bigcell = tbls[1].findall(W + 'tr')[1].findall(W + 'tc')[0]
ps = bigcell.findall(W + 'p')

targets = ['1. 选题背景及研究的目的和意义', '1.1 选题背景', '大语言模型自2017年',
           '2. 国内外在该方向的研究现状', '7. 主要参考文献', '[1] VASWANI', '[12] 李航']
for t in targets:
    for p in ps:
        if ptext(p).strip().startswith(t):
            print('=== ', t[:24])
            print(runinfo(p))
            break

for p in ps:
    txt = ptext(p).strip()
    if txt.startswith('[5] 王昊奋'):
        print('=== [5] 王昊奋（克隆段格式）')
        print(runinfo(p))
        break

print()
print('############ T1 审核表 字数核验 ############')
root1 = load(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-XXX-题目审核表（填写版）.docx')
tbl1 = root1.findall('.//' + W + 'tbl')[0]
trs = tbl1.findall(W + 'tr')

def cell_text(tr, ci):
    tcs = tr.findall(W + 'tc')
    if ci >= len(tcs):
        return ''
    return ''.join(t.text or '' for t in tcs[ci].iter(W + 't'))

# 课题条件行 = tr8（含1.学生知识储备/2.实验设备/3.工作要求）
cond = cell_text(trs[8], 1)
cond_len = len(re.sub(r'\s', '', cond))
print('课题条件行字数(不含空白):', cond_len, ' (要求<=500)')
# 课题意义/研究内容/设计目标 = tr9 tc1
sig = cell_text(trs[9], 1)
sig_len = len(re.sub(r'\s', '', sig))
print('课题意义+研究内容+设计目标字数:', sig_len, ' (要求<=800)')
# 勾选核验
t4 = ''.join(cell_text(trs[4], i) for i in range(9))
print('课题类型勾选:', re.findall(r'[☑□](?:工程设计|实（试）验研究|观察研究|计算机软件|经济管理及文科|艺术设计|其他)', t4))
print('成果类别勾选:', re.findall(r'[☑□](?:设计|论文)', t4))
t6 = ''.join(cell_text(trs[6], i) for i in range(6))
print('结合实际情况勾选:', re.findall(r'[☑□](?:社会实践（实（试）验、实习、工程实践和社会调查等各类社会实践）|科研|其他)', t6))

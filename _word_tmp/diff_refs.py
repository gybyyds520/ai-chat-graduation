# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

def analyze(path, label):
    z = zipfile.ZipFile(path)
    root = etree.fromstring(z.read('word/document.xml'))
    body = root.find(W + 'body')
    # 收集所有 commentReference：批注id + 所在段落文本（锚点上下文）
    refs = []
    for para in body.iter(W + 'p'):
        ptext_parts = []
        ref_in_para = []
        for el in para.iter():
            tag = etree.QName(el).localname
            if tag == 'commentReference':
                ref_in_para.append(el.get(W + 'id'))
            elif tag == 't':
                ptext_parts.append(el.text or '')
        for rid in ref_in_para:
            refs.append((rid, ''.join(ptext_parts)))
    # 收集 rangeStart
    ranges = []
    for el in body.iter():
        if etree.QName(el).localname == 'commentRangeStart':
            ranges.append(el.get(W + 'id'))
    print('=== %s ===' % label)
    print('  reference 总数:', len(refs), '| rangeStart 总数:', len(ranges))
    for rid, ctx in refs:
        print('  ref id=%s 段落: %s...' % (rid, ctx[:38]))
    return refs, ranges

t_refs, t_ranges = analyze(r'C:\Users\郭耀彬\Desktop\3.学号-姓名-题目审核表（由指导老师填写，一题一表）.docx', '模板')
print()
f_refs, f_ranges = analyze(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx', '正式文件')

t_ids = [r[0] for r in t_refs]
f_ids = [r[0] for r in f_refs]
missing = [rid for rid in t_ids if rid not in f_ids]
extra = [rid for rid in f_ids if rid not in t_ids]
print('\n模板有而正式文件缺的引用:', missing)
print('正式文件有而模板没有的引用:', extra)

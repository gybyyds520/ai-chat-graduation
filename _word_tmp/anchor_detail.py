# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

def detail(path, label):
    z = zipfile.ZipFile(path)
    root = etree.fromstring(z.read('word/document.xml'))
    body = root.find(W + 'body')
    print('===== %s =====' % label)
    # 遍历段落（含单元格内），记录每段完整文本和引用位置
    results = []
    para_idx = 0
    for para in body.iter(W + 'p'):
        runs = list(para.findall(W + 'r'))
        # 计算每个 run 的文本，和 run 前是否插有引用
        acc = ''
        refs = []
        for r in runs:
            rid = None
            refel = r.find(W + 'commentReference')
            if refel is not None:
                rid = refel.get(W + 'id')
            t = ''.join(x.text or '' for x in r.iter(W + 't'))
            if rid is not None:
                refs.append((rid, acc, t[:20]))
            acc += t
        # 段落开头（pPr 后、第一个 run 前）的独立引用 run
        for r in runs[:3]:
            refel = r.find(W + 'commentReference')
            if refel is not None:
                rid = refel.get(W + 'id')
                if not any(x[0] == rid for x in refs):
                    refs.append((rid, '[段首]', acc[:20]))
        if refs:
            for rid, pos, ctx in refs:
                results.append((int(rid), para_idx, pos, acc.strip()[:60]))
        para_idx += 1
    results.sort()
    for rid, pidx, pos, txt in results:
        print('  id=%2d 段落#%d 锚点位置=%s  段落文本: %s...' % (rid, pidx, pos, txt))
    return results

t = detail(r'C:\Users\郭耀彬\Desktop\3.学号-姓名-题目审核表（由指导老师填写，一题一表）.docx', '老师模板')
print()
f = detail(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx', '正式文件')

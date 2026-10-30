# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

def scan(path, label):
    z = zipfile.ZipFile(path)
    root = etree.fromstring(z.read('word/document.xml'))
    # 收集着色属性
    colors = {}   # (类型, 值) -> [文本片段]
    def walk(elem, stack):
        tag = etree.QName(elem).localname
        if tag == 'color':
            val = elem.get(W + 'val')
            key = ('fontcolor', val)
            colors.setdefault(key, [])
            colors[key].append(stack_text(stack))
        if tag == 'highlight':
            val = elem.get(W + 'val')
            key = ('highlight', val)
            colors.setdefault(key, [])
            colors[key].append(stack_text(stack))
        if tag == 'shd':
            fill = elem.get(W + 'fill')
            val = elem.get(W + 'val')
            if fill and fill.lower() not in ('auto', 'ffffff', 'white'):
                key = ('shd_fill', fill)
                colors.setdefault(key, [])
                colors[key].append(stack_text(stack))
        if tag == 't':
            txt = elem.text or ''
            stack.append(txt)
        for c in elem:
            walk(c, stack)
        if tag == 't':
            stack.pop()
    def stack_text(s):
        return ''.join(s)[-30:]
    body = root.find(W + 'body')
    walk(body, [])
    print('=== %s ===' % label)
    for (typ, val), frags in colors.items():
        print('  %s = %s (%d处) 示例: %s' % (typ, val, len(frags), frags[:2]))
    return colors

scan(r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx', '审核表成品')
print()
scan(r'C:\Users\郭耀彬\Desktop\3.2301132013-g-题目审核表（由指导老师填写，一题一表）.docx', '老师模板')

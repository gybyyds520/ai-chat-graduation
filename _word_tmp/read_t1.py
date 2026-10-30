# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
def walk(elem, depth):
    for child in elem:
        tag = etree.QName(child).localname
        if tag == 'p':
            t = ''.join(x.text or '' for x in child.iter(W + 't'))
            if t.strip():
                print('  ' * depth + t)
        elif tag == 'tbl':
            print('  ' * depth + '【表格】')
            for tr in child.findall(W + 'tr'):
                cells = []
                for tc in tr.findall(W + 'tc'):
                    t = ''.join(x.text or '' for x in tc.iter(W + 't'))
                    cells.append(t.strip())
                print('  ' * depth + '  | ' + ' | '.join(cells))
            print('  ' * depth + '【表格结束】')
        else:
            walk(child, depth + 1)
walk(body, 0)

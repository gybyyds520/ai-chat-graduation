# -*- coding: utf-8 -*-
import zipfile, sys, re
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
# 遍历 body 结构，映射段落索引
para_index = 0
def walk(elem, depth):
    global para_index
    for child in elem:
        tag = etree.QName(child).localname
        if tag == 'p':
            t = ''.join(x.text or '' for x in child.iter(W + 't'))
            print('  ' * depth + f'段{para_index}: {tag!r} jc={child.find(".//"+W+"jc").get(W+"val") if child.find(".//"+W+"jc") is not None else "-"} text={t[:28]!r}')
            para_index += 1
        elif tag == 'tbl':
            print('  ' * depth + '[表格开始]')
            for tr in child.iter(W + 'tr'):
                print('  ' * depth + '  [行]')
                for tc in tr.findall(W + 'tc'):
                    print('  ' * depth + '    [单元格]')
                    for p in tc.findall(W + 'p'):
                        t = ''.join(x.text or '' for x in p.iter(W + 't'))
                        print('  ' * depth + f'      段{para_index}: {t[:28]!r}')
                        para_index += 1
            print('  ' * depth + '[表格结束]')
        elif tag == 'sectPr':
            print('  ' * depth + '[sectPr]')
        else:
            walk(child, depth + 1)
walk(body, 0)
print('总段数:', para_index)

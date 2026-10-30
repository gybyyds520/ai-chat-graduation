# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
paras = list(root.iter(W + 'p'))
for i in range(5, 10):
    xml = etree.tostring(paras[i]).decode('utf-8')
    # 去掉命名空间声明，只看结构
    import re
    xml2 = re.sub(r'xmlns:\w+="[^"]*"', '', xml)
    xml2 = re.sub(r'xmlns="[^"]*"', '', xml2)
    print('===== 段', i, '=====')
    print(xml2[:1500])
    print()

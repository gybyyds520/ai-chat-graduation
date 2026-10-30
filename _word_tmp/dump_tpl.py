# -*- coding: utf-8 -*-
import zipfile, sys, re
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
def dump_paras(p, lo, hi, label):
    z = zipfile.ZipFile(p)
    root = etree.fromstring(z.read('word/document.xml'))
    paras = list(root.iter(W + 'p'))
    print('=====', label, '=====')
    for i in range(lo, min(hi, len(paras))):
        xml = etree.tostring(paras[i]).decode('utf-8')
        xml = re.sub(r'xmlns:\w+="[^"]*"', '', xml)
        xml = re.sub(r'xmlns="[^"]*"', '', xml)
        # 压缩连续属性空格
        xml = re.sub(r'w14:paraId="[^"]*"', '', xml)
        t = ''.join(x.text or '' for x in paras[i].iter(W + 't'))
        print('--- 段', i, repr(t[:30]))
        print(xml[:900])
        print()
dump_paras(r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx', 6, 13, '模板 段6-12')

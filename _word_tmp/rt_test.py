# -*- coding: utf-8 -*-
import zipfile, os, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
src = r'C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx'
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\roundtrip_test.docx'
z = zipfile.ZipFile(src)
root = etree.fromstring(z.read('word/document.xml'))
# 仅 round-trip，不改任何内容
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
print('roundtrip 写出 OK:', os.path.getsize(out))

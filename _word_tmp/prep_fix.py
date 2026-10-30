# -*- coding: utf-8 -*-
import zipfile, sys, re, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
cur = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
rep = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\repaired_test.docx'
zc = zipfile.ZipFile(cur); zr = zipfile.ZipFile(rep)
rc = etree.fromstring(zc.read('word/document.xml'))
rr = etree.fromstring(zr.read('word/document.xml'))
pc = list(rc.iter(W+'p')); pr = list(rr.iter(W+'p'))
def slim(e):
    x = etree.tostring(e).decode('utf-8')
    x = re.sub(r'xmlns:\w+="[^"]*"', '', x)
    x = re.sub(r'xmlns="[^"]*"', '', x)
    x = re.sub(r'w14:paraId="[^"]*"', '', x)
    return x
print('--- 当前段7 ---')
print(slim(pc[7])[:1200])
print()
print('--- 修复段7 ---')
print(slim(pr[7])[:1200])
print()
# 删除修复版的段8/9（两个空段）保存测试副本
rr2 = copy.deepcopy(rr)
pr2 = list(rr2.iter(W+'p'))
# 修复版段8、段9 是空段，删除
removed = []
for para in (pr2[8], pr2[9]):
    if not ''.join(x.text or '' for x in para.iter(W+'t')).strip():
        para.getparent().remove(para)
        removed.append(True)
new_doc = etree.tostring(rr2, xml_declaration=True, encoding='UTF-8', standalone=True)
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\rep_minus_empty.docx'
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in zr.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else zr.read(n))
print('已生成删空段版:', out, '| 删除:', removed)

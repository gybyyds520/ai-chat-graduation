# -*- coding: utf-8 -*-
import zipfile, sys, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
out = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\restore_test.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
paras = list(root.iter(W + 'p'))
p7 = paras[7]  # 完整题目段（3 runs）
# 分离 run2/run3（的设计与实现 部分）
run1 = p7.findall(W + 'r')[0]
run2 = p7.findall(W + 'r')[1]
run3 = p7.findall(W + 'r')[2]
# 从 p7 移除 run2/run3
p7.remove(run2); p7.remove(run3)
# 构造段8（空段，pStyle5 jc=both）与段9（jc=center 两 run）
def make_rpr_text():
    rpr = etree.SubElement(etree.Element(W + 'x'), W + 'rPr')
    rf = etree.SubElement(rpr, W + 'rFonts')
    rf.set(W + 'hint', 'eastAsia'); rf.set(W + 'ascii', 'Times New Roman'); rf.set(W + 'hAnsi', 'Times New Roman'); rf.set(W + 'eastAsia', '宋体'); rf.set(W + 'cs', 'Times New Roman')
    etree.SubElement(rpr, W + 'bCs')
    kern = etree.SubElement(rpr, W + 'kern'); kern.set(W + 'val', '2')
    sz = etree.SubElement(rpr, W + 'sz'); sz.set(W + 'val', '32')
    szcs = etree.SubElement(rpr, W + 'szCs'); szcs.set(W + 'val', '32')
    lang = etree.SubElement(rpr, W + 'lang'); lang.set(W + 'val', 'en-US'); lang.set(W + 'eastAsia', 'zh-CN'); lang.set(W + 'bidi', 'ar-SA')
    return rpr
def make_para(jc, runs_text):
    p = etree.Element(W + 'p')
    ppr = etree.SubElement(p, W + 'pPr')
    ps = etree.SubElement(ppr, W + 'pStyle'); ps.set(W + 'val', '5')
    sp = etree.SubElement(ppr, W + 'spacing'); sp.set(W + 'line', '0'); sp.set(W + 'lineRule', 'atLeast')
    j = etree.SubElement(ppr, W + 'jc'); j.set(W + 'val', jc)
    for t in runs_text:
        r = etree.SubElement(p, W + 'r')
        rpr = make_rpr_text()
        r.insert(0, rpr)
        tt = etree.SubElement(r, W + 't'); tt.text = t
    return p
p8 = make_para('both', [])
p9 = make_para('center', ['的设计与实', '现'])
# 插入到 p7 之后
idx = list(body).index(p7)
body.insert(idx + 1, p8)
body.insert(idx + 2, p9)
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
# 验证
z2 = zipfile.ZipFile(out)
r2 = etree.fromstring(z2.read('word/document.xml'))
for i, para in enumerate(list(r2.iter(W+'p'))[6:12]):
    t = ''.join(x.text or '' for x in para.iter(W+'t'))
    print(i+6, repr(t[:40]))
print('恢复版写出:', out)

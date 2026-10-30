# -*- coding: utf-8 -*-
import zipfile, sys, os
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'

z = zipfile.ZipFile(TARGET)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

ANCHORS = [
    ('14', '（2）AI角色引擎'),
    ('15', '（3）AI记忆系统'),
]

for cid, prefix in ANCHORS:
    done = False
    for para in body.iter(W + 'p'):
        runs = para.findall(W + 'r')
        acc = ''
        target_run = None
        for r in runs:
            t = ''.join(x.text or '' for x in r.iter(W + 't'))
            if len(acc) < len(prefix) and prefix.startswith(acc) and t.startswith(prefix[len(acc):len(acc) + 1]):
                target_run = r
                break
            acc += t
        if target_run is not None:
            newr = etree.Element(W + 'r')
            ref = etree.SubElement(newr, W + 'commentReference')
            ref.set(W + 'id', cid)
            target_run.addprevious(newr)
            print('已插入 id=%s 于段落内 "%s..."' % (cid, prefix))
            done = True
            break
    if not done:
        print('未找到: %s' % prefix)

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, TARGET)
print('已保存')

z2 = zipfile.ZipFile(TARGET)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
from collections import Counter
c = Counter(etree.QName(el).localname for el in b2.iter())
print('reference:', c.get('commentReference', 0), '| rangeStart:', c.get('commentRangeStart', 0))
ids = sorted([el.get(W + 'id') for el in b2.iter() if etree.QName(el).localname == 'commentReference'], key=int)
print('引用批注id:', ids)
cr = etree.fromstring(z2.read('word/comments.xml'))
print('批注本体:', len(list(cr)))

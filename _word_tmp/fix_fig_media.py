# -*- coding: utf-8 -*-
"""用新版无标题图覆盖 docx media，并同步 drawing extent"""
import os, zipfile, shutil
from lxml import etree

TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
FIGS = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\figs'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
R = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'
A = '{http://schemas.openxmlformats.org/drawingml/2006/main}'
WP = '{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}'
PR = '{http://schemas.openxmlformats.org/package/2006/relationships}'

mapping = {  # media -> 新图文件与目标尺寸
    'media/image3.png': ('fig1_sanduan.png', 1500, 850),
    'media/image4.png': ('fig2_zongti.png', 1500, 940),
    'media/image5.png': ('fig3_linghun.png', 1500, 900),
}

z = zipfile.ZipFile(TARGET)
names = z.namelist()
doc = z.read('word/document.xml')
rels_raw = z.read('word/_rels/document.xml.rels')

root = etree.fromstring(doc)
rels = etree.fromstring(rels_raw)

# rId -> target
rid2target = {}
for rel in rels:
    rid2target[rel.get('Id')] = rel.get('Target')

# 找 image3/4/5 对应 rId
target2rid = {v: k for k, v in rid2target.items()}

# 遍历 document.xml 中 drawing 的 blip
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
# 先序列化，若后续要改 extent 则用对象方式。这里直接用 lxml 修改：
changed = []
for blip in root.iter(A + 'blip'):
    embed = blip.get(R + 'embed')
    if not embed:
        continue
    target = rid2target.get(embed, '')
    if target in mapping:
        # 找 ancestor drawing 中的 extent
        anc = blip.getparent()
        while anc is not None and anc.tag != WP + 'inline':
            anc = anc.getparent()
        if anc is None:
            print('no inline for', target); continue
        ext = anc.find(WP + 'extent')
        if ext is None:
            print('no extent for', target); continue
        cx = int(ext.get('cx'))
        fname, w, h = mapping[target]
        cy = int(cx * h / w)
        ext.set('cy', str(cy))
        changed.append(target)
        print('update', target, fname, 'cy=', cy)

assert changed, '未找到任何目标图片'
new_doc_bytes = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)

tmp = TARGET + '.tmp2'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in names:
        if n == 'word/document.xml':
            w.writestr(n, new_doc_bytes)
        elif n.split('/')[-1] in [k.split('/')[-1] for k in mapping]:
            fname = mapping['media/' + n.split('/')[-1]][0]
            with open(os.path.join(FIGS, fname), 'rb') as f:
                w.writestr(n, f.read())
        else:
            w.writestr(n, z.read(n))
z.close()
os.replace(tmp, TARGET)
print('media replaced OK')

# 验证
z2 = zipfile.ZipFile(TARGET)
from PIL import Image
import io
for n in ['word/media/image3.png', 'word/media/image4.png', 'word/media/image5.png']:
    im = Image.open(io.BytesIO(z2.read(n)))
    print(n, im.size)
d = z2.read('word/document.xml').decode('utf-8')
print('commentReference:', d.count('commentReference'), '| FF0000:', d.count('FF0000'))

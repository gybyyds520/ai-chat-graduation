# -*- coding: utf-8 -*-
"""郭耀彬版开题报告修复：
1. 正文 6 处"微信小程序/微信开发者工具"表述改为"移动端网页版"口径
2. 参考文献[10] 替换为响应式设计文献
3. 补插缺失的图3（AI灵魂算法与分层记忆流程图）
"""
import zipfile, os, io
from lxml import etree

TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
FIGS = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\figs'
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
R = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'
A = '{http://schemas.openxmlformats.org/drawingml/2006/main}'
WP = '{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}'
PR = '{http://schemas.openxmlformats.org/package/2006/relationships}'
REL_IMAGE = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/image'

z = zipfile.ZipFile(TARGET)
names = z.namelist()
doc = z.read('word/document.xml')
rels_raw = z.read('word/_rels/document.xml.rels')
root = etree.fromstring(doc)
rels = etree.fromstring(rels_raw)

# ---------- 1. 正文文本替换 ----------
REPL = [
    ('并支持Web端、Electron桌面端与移动端（微信小程序）三端复用',
     '并支持Web端、Electron桌面端与移动端（响应式网页版）三端复用'),
    ('微信小程序端满足移动场景下的轻量访问需求',
     '移动端网页版满足移动场景下的轻量访问需求'),
    ('前端采用Vue 3与Vite构建Web端与桌面端，微信开发者工具构建移动端',
     '前端采用Vue 3与Vite构建Web端与桌面端，并通过响应式适配形成移动端网页版'),
    ('表现层由Web端、Electron桌面端与微信小程序端构成',
     '表现层由Web端、Electron桌面端与移动端网页版构成'),
    ('已安装JDK17、Node.js、MySQL8、Redis、Ollama、微信开发者工具等软硬件环境',
     '已安装JDK17、Node.js、MySQL8、Redis、Ollama等软硬件环境'),
    ('三端适配复杂度高，微信小程序存在能力限制（如WebSocket、本地存储策略），Electron桌面端存在跨平台兼容问题',
     '三端适配复杂度高，移动端网页版存在屏幕适配与浏览器兼容差异，Electron桌面端存在跨平台兼容问题'),
    ('[10] 腾讯. 微信小程序开发文档[EB/OL]. [2026-10-06]. https://developers.weixin.qq.com/miniprogram/dev/framework.',
     '[10] Mozilla开发者网络. 响应式设计[EB/OL]. [2026-10-08]. https://developer.mozilla.org/zh-CN/docs/Learn/CSS/CSS_layout/Responsive_Design.'),
]
count = 0
for para in root.iter(W+'p'):
    for t in para.iter(W+'t'):
        if t.text:
            for old, new in REPL:
                if old in t.text:
                    t.text = t.text.replace(old, new)
                    count += 1
                    print('REPL:', old[:30], '...')
print('文本替换完成，共', count, '处')

# ---------- 2. 补插图3 ----------
# 找题注段"图3"与图2图片段
cap3_para = None
pic2_para = None
for para in root.iter(W+'p'):
    t = ''.join(x.text or '' for x in para.iter(W+'t')).strip()
    if t.startswith('图3') and 'AI灵魂算法' in t:
        cap3_para = para
    blips = [b.get(R+'embed') for b in para.iter(A+'blip')]
    if blips:
        # 判断该 blip 是否指向 image4（图2）
        for rid in blips:
            for rel in rels:
                if rel.get('Id') == rid and rel.get('Target') == 'media/image4.png':
                    pic2_para = para
assert cap3_para is not None, '未找到图3题注段'
assert pic2_para is not None, '未找到图2图片段'
print('定位完成：图3题注段、图2图片段均找到')

# 深拷贝图2图片段作为模板
import copy
pic3 = copy.deepcopy(pic2_para)
# 在题注段前插入
cap3_para.addprevious(pic3)

# 修改 extent cy（图3: 1500x900，保持 cx，cy = cx*900/1500）
for ext in pic3.iter(WP+'extent'):
    cx = int(ext.get('cx'))
    ext.set('cy', str(int(cx * 900 / 1500)))
    print('extent cx=', cx, 'cy->', ext.get('cy'))

# 修改 blip rId 为新关系
# 找最大 rId 号
max_rid = 0
for rel in rels:
    rid = rel.get('Id')
    if rid and rid.startswith('rId'):
        try:
            max_rid = max(max_rid, int(rid[3:]))
        except ValueError:
            pass
new_rid = 'rId' + str(max_rid + 1)
for blip in pic3.iter(A+'blip'):
    blip.set(R+'embed', new_rid)
print('新关系id:', new_rid)

# rels 增加关系
rel_el = etree.SubElement(rels, PR+'Relationship')
rel_el.set('Id', new_rid)
rel_el.set('Type', REL_IMAGE)
rel_el.set('Target', 'media/image5.png')

# ---------- 3. 打包写回 ----------
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
new_rels = etree.tostring(rels, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in names:
        if n == 'word/document.xml':
            w.writestr(n, new_doc)
        elif n == 'word/_rels/document.xml.rels':
            w.writestr(n, new_rels)
        else:
            w.writestr(n, z.read(n))
    # 新增图3 media（原包中没有该文件）
    with open(os.path.join(FIGS, 'fig3_linghun.png'), 'rb') as f:
        w.writestr('word/media/image5.png', f.read())
z.close()
os.replace(tmp, TARGET)
print('打包完成')

# ---------- 4. 验证 ----------
z2 = zipfile.ZipFile(TARGET)
d2 = z2.read('word/document.xml').decode('utf-8')
print('微信小程序残留:', d2.count('微信小程序'))
print('微信开发者工具残留:', d2.count('微信开发者工具'))
print('commentReference:', d2.count('commentReference'))
print('media:', [n for n in z2.namelist() if n.startswith('word/media/image')])
from PIL import Image
for n in ['word/media/image3.png', 'word/media/image4.png', 'word/media/image5.png']:
    im = Image.open(io.BytesIO(z2.read(n)))
    print(n, im.size)

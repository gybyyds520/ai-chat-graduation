# -*- coding: utf-8 -*-
import zipfile, sys, os, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
XML_SPACE = '{http://www.w3.org/XML/1998/namespace}space'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-郭耀彬-开题报告.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

def para_text(para):
    return ''.join(x.text or '' for x in para.iter(W + 't'))

# 1) 替换文献条目文本（按前缀定位）
REPLACE = {
    '[5] ': '[5] PAN S, LUO L, WANG Y, et al. Unifying large language models and knowledge graphs: A roadmap[J]. IEEE Transactions on Knowledge and Data Engineering, 2024, 36(7): 2851-2866.',
    '[13] ': '[13] GAO Y, XIONG Y, GAO X, et al. Retrieval-augmented generation for large language models: A survey[J]. arXiv preprint arXiv:2312.10997, 2023.',
    '[18] ': '[18] Spring Security. Spring Security reference documentation[EB/OL]. [2026-10-10]. https://docs.spring.io/spring-security/reference.',
}
# 2) 追加的新文献
APPEND = [
    '[21] DUBEY A, JAUHRI A, PANDEY A, et al. The Llama 3 herd of models[J]. arXiv preprint arXiv:2407.21783, 2024.',
    '[22] YANG A, YANG B, ZHANG B, et al. Qwen2.5 technical report[J]. arXiv preprint arXiv:2412.15115, 2024.',
]

def replace_para_text(para, new_text):
    rpr_tpl = None
    for r in para.findall(W + 'r'):
        rpr = r.find(W + 'rPr')
        if rpr is not None:
            rpr_tpl = copy.deepcopy(rpr)
            break
    for r in list(para.findall(W + 'r')):
        para.remove(r)
    r = etree.SubElement(para, W + 'r')
    if rpr_tpl is not None:
        r.insert(0, rpr_tpl)
    t = etree.SubElement(r, W + 't')
    t.set(XML_SPACE, 'preserve')
    t.text = new_text

# 定位所有文献段
ref_paras = {}
for para in body.iter(W + 'p'):
    t = para_text(para).strip()
    if len(t) > 15 and t.startswith('[') and '] ' in t[:6]:
        num = t[1:t.index(']')]
        if num.isdigit():
            ref_paras[int(num)] = para

for num, new_text in REPLACE.items():
    n = int(num[1:-2])
    assert n in ref_paras, '未找到文献 [%d]' % n
    replace_para_text(ref_paras[n], new_text)
    print('已替换 [%d]' % n)

# 在 [20] 后追加 [21][22]
ref20 = ref_paras[20]
parent = ref20.getparent()
idx = parent.index(ref20)
pPr_tpl = ref20.find(W + 'pPr')
rpr_tpl = None
for r in ref20.findall(W + 'r'):
    rpr = r.find(W + 'rPr')
    if rpr is not None:
        rpr_tpl = copy.deepcopy(rpr)
        break
for i, text in enumerate(APPEND):
    np_ = etree.Element(W + 'p')
    if pPr_tpl is not None:
        np_.append(copy.deepcopy(pPr_tpl))
    r = etree.SubElement(np_, W + 'r')
    if rpr_tpl is not None:
        r.append(copy.deepcopy(rpr_tpl))
    t = etree.SubElement(r, W + 't')
    t.set(XML_SPACE, 'preserve')
    t.text = text
    parent.insert(idx + 1 + i, np_)
print('已追加', len(APPEND), '条文献')

# 3) 正文第2章国内段补开源大模型论述
OLD_DOM = '在本地化部署方面，Ollama、llama.cpp、LM Studio等开源工具使大模型能够在消费级硬件上离线运行，极大降低了私有化部署门槛，相关研究已应用于智能客服、知识库问答等场景。'
NEW_DOM = '以Llama 3、Qwen2.5为代表的开源大模型相继发布并开放权重，使消费级硬件上的本地化部署成为可能；Ollama、llama.cpp、LM Studio等开源工具进一步降低了私有化部署门槛，相关研究已应用于智能客服、知识库问答等场景。'
for para in body.iter(W + 'p'):
    if OLD_DOM in para_text(para):
        replace_para_text(para, NEW_DOM)
        print('已更新本地化部署段')
        break
else:
    print('警告：未找到本地化部署段原文')

# 4) 保存
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = p + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, p)
print('已保存')

# 5) 回读验证：条数、近五年占比
z2 = zipfile.ZipFile(p)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
refs = []
for x in b2.iter(W + 'p'):
    t = para_text(x).strip()
    if len(t) > 15 and t.startswith('[') and '] ' in t[:6]:
        refs.append(t)
print('\n文献总数:', len(refs))
import re
recent = 0
for t in refs:
    m = re.search(r'(20\d\d)[^\d]', t)
    if m:
        yr = int(m.group(1))
        if 2022 <= yr <= 2026:
            recent += 1
        print('  %s (年份%d)' % (t[:40], yr))
print('近五年(2022-2026):', recent, '/', len(refs), '=', round(recent * 100 / len(refs)), '%')
has_wx = any('微信小程序' in para_text(x) for x in b2.iter(W + 'p'))
print('微信残留:', has_wx)

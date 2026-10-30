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

NEW_REFS = [
    '[13] LEWIS P, PEREZ E, PIKTUS A, et al. Retrieval-augmented generation for knowledge-intensive NLP tasks[C]//Advances in Neural Information Processing Systems 33. 2020: 9459-9474.',
    '[14] PARK J S, O\'BRIEN J C, CAI C J, et al. Generative agents: Interactive simulacra of human behavior[C]//Proceedings of the 36th Annual ACM Symposium on User Interface Software and Technology. New York: ACM, 2023: 1-22.',
    '[15] HINTON G, DENG L, YU D, et al. Deep neural networks for acoustic modeling in speech recognition[J]. IEEE Signal Processing Magazine, 2012, 29(6): 82-97.',
    '[16] WANG Y, SKERRY-RYAN R J, STANTON D, et al. Tacotron: Towards end-to-end speech synthesis[C]//Proceedings of Interspeech 2017. Stockholm: ISCA, 2017: 4006-4010.',
    '[17] WEI J, WANG X, SCHUURMANS D, et al. Chain-of-thought prompting elicits reasoning in large language models[C]//Advances in Neural Information Processing Systems 35. 2022.',
    '[18] FERRAIOLO D F, SANDHU R, GAVRILA S, et al. Proposed NIST standard for role-based access control[J]. ACM Transactions on Information and System Security, 2001, 4(3): 224-274.',
    '[19] JONES M, BRADLEY J, SAKIMURA N. JSON Web Token (JWT)[S/OL]. RFC 7519, 2015[2026-10-10]. https://www.rfc-editor.org/rfc/rfc7519.',
    '[20] FETTE I, MELNIKOV A. The WebSocket protocol[S/OL]. RFC 6455, 2011[2026-10-10]. https://www.rfc-editor.org/rfc/rfc6455.',
]

# 1) 定位 [12] 段
ref12 = None
for para in body.iter(W + 'p'):
    t = para_text(para).strip()
    if t.startswith('[12] '):
        ref12 = para
        break
assert ref12 is not None, '未找到 [12] 文献段'
parent = ref12.getparent()
idx = parent.index(ref12)

# 构造新文献段：复制 [12] 的 pPr 与第一个 run rPr
pPr_tpl = ref12.find(W + 'pPr')
rpr_tpl = None
for r in ref12.findall(W + 'r'):
    rpr = r.find(W + 'rPr')
    if rpr is not None:
        rpr_tpl = copy.deepcopy(rpr)
        break

for i, text in enumerate(NEW_REFS):
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
print('已插入', len(NEW_REFS), '条新文献，位置在 [12] 后')

# 2) 更新第2章"在基础研究方面"段
OLD_FOUNDATION = '在基础研究方面，Vaswani等人提出的Transformer架构奠定了大语言模型的基础，Zhao等人对大规模语言模型的研究进展进行了系统综述。'
NEW_FOUNDATION = '在基础研究方面，Vaswani等人提出的Transformer架构奠定了大语言模型的基础，Zhao等人对大规模语言模型的研究进展进行了系统综述；Wei等人提出的思维链提示方法有效增强了大模型的推理能力，Lewis等人提出的检索增强生成（RAG）范式将外部知识与大模型推理相结合，Park等人提出的生成式智能体通过记忆流与反思机制模拟人类行为，为AI长期记忆与人格化设计提供了理论支撑。'

found_para = None
for para in body.iter(W + 'p'):
    if OLD_FOUNDATION in para_text(para):
        found_para = para
        break
assert found_para is not None, '未找到基础研究段'
# 保留 pPr，重建 run
rpr_tpl2 = None
for r in found_para.findall(W + 'r'):
    rpr = r.find(W + 'rPr')
    if rpr is not None:
        rpr_tpl2 = copy.deepcopy(rpr)
        break
for r in list(found_para.findall(W + 'r')):
    found_para.remove(r)
r = etree.SubElement(found_para, W + 'r')
if rpr_tpl2 is not None:
    r.insert(0, rpr_tpl2)
t = etree.SubElement(r, W + 't')
t.set(XML_SPACE, 'preserve')
t.text = NEW_FOUNDATION
print('已更新基础研究段')

# 3) 保存
new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = p + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, p)
print('已保存')

# 4) 回读验证
z2 = zipfile.ZipFile(p)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
refs = [para_text(x).strip() for x in b2.iter(W + 'p') if para_text(x).strip().startswith('[') and '] ' in para_text(x).strip()[:5]]
print('回读文献条数:', len(refs))
print('末条:', refs[-1][:60] if refs else '-')
has_wx = any('微信小程序' in para_text(x) or '微信开发者工具' in para_text(x) for x in b2.iter(W + 'p'))
print('微信残留:', has_wx)

# -*- coding: utf-8 -*-
import zipfile, sys, os, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
XML_SPACE = '{http://www.w3.org/XML/1998/namespace}space'
TARGET = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'

z = zipfile.ZipFile(TARGET)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

def para_text(para):
    return ''.join(x.text or '' for x in para.iter(W + 't'))

# 1) 找到研究内容段落
target_para = None
for para in body.iter(W + 'p'):
    if '系统总体架构设计' in para_text(para):
        target_para = para
        break
assert target_para is not None

# 2) 移除段落中 id=14/15 的引用 run（保留 id=13）
removed = 0
for r in list(target_para.findall(W + 'r')):
    refel = r.find(W + 'commentReference')
    if refel is not None and refel.get(W + 'id') in ('14', '15'):
        target_para.remove(r)
        removed += 1
print('移除段首误堆引用:', removed)

# 3) 精确插入：拆分包含 idx 的 run
def insert_ref_at(para, cid, prefix):
    runs = para.findall(W + 'r')
    text = para_text(para)
    idx = text.find(prefix)
    if idx < 0:
        print('失败: %s 未找到' % prefix)
        return False
    acc = 0
    for r in runs:
        t = ''.join(x.text or '' for x in r.iter(W + 't'))
        rstart = acc
        rend = acc + len(t)
        if rstart <= idx < rend:
            # 拆分 r：rPr 复制
            offset = idx - rstart
            if offset == 0:
                # idx 正好在 run 起点：直接 addprevious
                newr = etree.Element(W + 'r')
                ref = etree.SubElement(newr, W + 'commentReference')
                ref.set(W + 'id', cid)
                r.addprevious(newr)
                print('插入 id=%s 于 run 起点 (offset=0)' % cid)
            else:
                # 拆分为 [前缀文本][引用][剩余文本]
                rpr = r.find(W + 'rPr')
                rpr_copy = copy.deepcopy(rpr) if rpr is not None else None
                # 找到 w:t 元素
                t_el = r.find(W + 't')
                if t_el is None:
                    print('失败: run无文本元素')
                    return False
                full = t_el.text or ''
                head_txt, tail_txt = full[:offset], full[offset:]
                # 原 run 变为前缀
                t_el.text = head_txt
                # 引用 run
                ref_r = etree.Element(W + 'r')
                if rpr_copy is not None:
                    ref_r.append(copy.deepcopy(rpr_copy))
                ref = etree.SubElement(ref_r, W + 'commentReference')
                ref.set(W + 'id', cid)
                # 剩余 run
                tail_r = etree.Element(W + 'r')
                if rpr_copy is not None:
                    tail_r.append(copy.deepcopy(rpr_copy))
                tail_t = etree.SubElement(tail_r, W + 't')
                tail_t.set(XML_SPACE, 'preserve')
                tail_t.text = tail_txt
                # 顺序: r(前缀), ref_r, tail_r
                r.addnext(ref_r)
                ref_r.addnext(tail_r)
                print('插入 id=%s 于偏移 %d ("%s")' % (cid, offset, full[max(0,offset-6):offset+8]))
            return True
        acc = rend
    print('失败: %s 未命中run' % prefix)
    return False

insert_ref_at(target_para, '14', '（2）AI角色引擎')
insert_ref_at(target_para, '15', '（3）AI记忆系统')

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = TARGET + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, TARGET)
print('已保存')

# 4) 回读验证：段落内 run 序列
z2 = zipfile.ZipFile(TARGET)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
for para in b2.iter(W + 'p'):
    t = para_text(para)
    if '系统总体架构设计' in t:
        print()
        print('修正后 run 序列:')
        for r in para.findall(W + 'r'):
            refel = r.find(W + 'commentReference')
            rt = ''.join(x.text or '' for x in r.iter(W + 't'))
            if refel is not None:
                print('  >>> 引用 id=%s' % refel.get(W + 'id'))
            elif rt.strip():
                print('  文本: %s...' % rt[:26])
        break

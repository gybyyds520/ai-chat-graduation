# -*- coding: utf-8 -*-
import zipfile, sys, os, copy
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')
tbl = body.find(W + 'tbl')
rows = tbl.findall(W + 'tr')
# 定位内容单元格（行9 tc#1）
target_tc = None
for ri, tr in enumerate(rows):
    tcs = tr.findall(W + 'tc')
    for ti, tc in enumerate(tcs):
        full = ''.join(x.text or '' for x in tc.iter(W + 't'))
        if full.startswith('1.课题意义'):
            target_tc = tc
            break
    if target_tc is not None:
        break
assert target_tc is not None, '未找到内容单元格'
paras = target_tc.findall(W + 'p')

TXT_MEANING = '本课题针对通用聊天机器人缺乏人格化设定、记忆连续性不足、依赖云端服务存在隐私与成本问题等现状，设计并实现一个基于本地大模型的智能AI陪伴聊天系统。系统以“AI角色+AI精灵”双体系提供个性化陪伴，采用“记忆—经历—感受—自我”灵魂算法构建分层长期记忆，实现“越聊越懂你”的持续陪伴体验；Ollama本地化部署保障数据隐私、降低使用成本；支持Web端、移动端（响应式网页版）与Electron桌面端多端使用，并融合好友、群聊、兴趣社群等社交功能。创新点包括：本地大模型私有化部署、灵魂算法驱动的记忆机制、3D立体AI桌面宠物、三端统一架构。'
TXT_CONTENT = '（1）系统总体架构设计：前后端分离架构，Spring Boot 3.2.5、Vue 3、MySQL 8与Redis，支持Web端、移动端（响应式网页版）与Electron桌面端三端复用；（2）AI角色引擎：角色创建、人设提示词、上下文管理与角色一致性对话；（3）AI记忆系统：基于向量检索、重要度评分与时间衰减的长期记忆，融合“记忆—经历—感受—自我”灵魂算法；（4）AI精灵与桌面宠物：3D立体形象、自由移动互动、形象更换与语音对话；（5）社交模块：好友、私聊、群聊、兴趣社群与动态发布；（6）语音交互：语音识别与语音合成；（7）管理员后台：用户管理、数据池管理、公告管理与AI系统管理；（8）安全合规：JWT认证与鉴权、RBAC分级权限控制、内容审核（敏感词过滤与管理员审核队列）、未成年保护模式；（9）测试体系：单元测试、集成测试与系统测试三层全覆盖，自动化测试用例1000个以上并全部通过。'
TXT_GOAL = '实现功能完整、安全健壮、流畅运行的智能AI陪伴聊天系统，覆盖Web端、移动端（响应式网页版）与Electron桌面端三端；完成自动化测试用例1000个以上并全部通过，系统全链路稳定运行。'

def replace_para_text(para, new_text):
    # 收集第一个 run 的 rPr 作为基准
    rpr_tpl = None
    for r in para.findall(W + 'r'):
        rpr = r.find(W + 'rPr')
        if rpr is not None:
            rpr_tpl = copy.deepcopy(rpr)
            break
    # 删除所有 run
    for r in list(para.findall(W + 'r')):
        para.remove(r)
    # 新建单 run
    r = etree.SubElement(para, W + 'r')
    if rpr_tpl is not None:
        r.insert(0, rpr_tpl)
    t = etree.SubElement(r, W + 't')
    t.set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
    t.text = new_text

# 段2=课题意义、段5=研究内容、段8=设计目标
replace_para_text(paras[2], TXT_MEANING)
replace_para_text(paras[5], TXT_CONTENT)
replace_para_text(paras[8], TXT_GOAL)

new_doc = etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True)
tmp = p + '.tmp'
with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as w:
    for n in z.namelist():
        w.writestr(n, new_doc if n == 'word/document.xml' else z.read(n))
z.close()
os.replace(tmp, p)
# 验证字数
z2 = zipfile.ZipFile(p)
r2 = etree.fromstring(z2.read('word/document.xml'))
b2 = r2.find(W + 'body')
for tr in b2.find(W + 'tbl').findall(W + 'tr'):
    for tc in tr.findall(W + 'tc'):
        full = ''.join(x.text or '' for x in tc.iter(W + 't'))
        if full.startswith('1.课题意义'):
            # 统计该单元格文本（不含标题）字数
            import re
            body_text = full.replace('1.课题意义', '').replace('2.主要研究内容', '').replace('3.主要设计目标', '')
            han = len(re.findall(r'[\u4e00-\u9fff]', body_text))
            total = len(re.sub(r'\s', '', body_text))
            print('课题意义栏汉字数:', han, '| 总字符(含标点数字):', total)
            for pi, para in enumerate(tc.findall(W + 'p')):
                t = ''.join(x.text or '' for x in para.iter(W + 't'))
                if t.strip():
                    print('段%d: %s...' % (pi, t[:40]))
print('已保存')

# -*- coding: utf-8 -*-
"""T1 完整重建 v2：保留全部批注与 run 结构；只改文本；删红色说明；设缩进2字符"""
import shutil
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

SRC = r"C:\Users\郭耀彬\Desktop\3.学号-姓名-题目审核表（由指导老师填写，一题一表）.docx"
T1 = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-XXX-题目审核表（填写版）.docx"
PLACEHOLDER = "×××"

shutil.copyfile(SRC, T1)
doc = Document(T1)
tbl_el = doc.tables[0]._tbl
trs = tbl_el.findall(qn('w:tr'))

def tc(tr_idx, tc_idx0):
    return trs[tr_idx].findall(qn('w:tc'))[tc_idx0]

def paras(c):
    return c.findall(qn('w:p'))

def runs(p):
    return p.findall(qn('w:r'))

def set_run_text(r, text):
    ts = r.findall(qn('w:t'))
    if not ts:
        t = OxmlElement('w:t'); t.text = text; t.set(qn('xml:space'), 'preserve'); r.append(t)
    else:
        ts[0].text = text
        for t in ts[1:]:
            t.text = ""

def del_red_runs(p):
    for r in list(p.findall(qn('w:r'))):
        rPr = r.find(qn('w:rPr'))
        if rPr is not None:
            col = rPr.find(qn('w:color'))
            if col is not None and col.get(qn('w:val'), '').upper() in ('FF0000', 'RED'):
                p.remove(r)

def set_indent2(p):
    pPr = p.find(qn('w:pPr'))
    if pPr is None:
        pPr = OxmlElement('w:pPr'); p.insert(0, pPr)
    ind = pPr.find(qn('w:ind'))
    if ind is None:
        ind = OxmlElement('w:ind'); pPr.append(ind)
    ind.set(qn('w:firstLineChars'), '200')
    ind.set(qn('w:firstLine'), '420')

# ---- tr[1] 专业：Xxxx → 计算机科学与技术 ----
set_run_text(runs(paras(tc(1, 3))[0])[0], "计算机科学与技术")

# ---- tr[2] 题目名称值 ----
p = paras(tc(2, 2))[0]
rs = runs(p)
if rs:
    set_run_text(rs[0], "基于本地大模型的智能AI陪伴聊天系统的设计与实现")
    for r in rs[1:]:
        set_run_text(r, "")
else:
    r = OxmlElement('w:r'); p.append(r)
    set_run_text(runs(p)[0], "基于本地大模型的智能AI陪伴聊天系统的设计与实现")

# ---- tr[3] 教师行 ----
set_run_text(runs(paras(tc(3, 2))[0])[0], PLACEHOLDER)   # 教师姓名值
set_run_text(runs(paras(tc(3, 4))[0])[0], PLACEHOLDER)   # 职称值
ps = paras(tc(3, 6))                                      # 学位学历值（硕士/研究生两段）
set_run_text(runs(ps[0])[0], PLACEHOLDER)
set_run_text(runs(ps[1])[0], "")
set_run_text(runs(paras(tc(3, 8))[0])[0], PLACEHOLDER)   # 研究方向值

# ---- tr[4] 课题类型 ----
c = tc(4, 2)
p1 = paras(c)[0]; p2 = paras(c)[1]
set_run_text(runs(p1)[0], "□")                            # ☑工程设计 → □工程设计
set_run_text(runs(p2)[0], "☑计算机软件 ")                  # □计算机软件 → ☑计算机软件

# ---- tr[6] 结合实际情况：☑其他 ----
c = tc(6, 2)
rs = runs(paras(c)[1])
set_run_text(rs[2], "☑其他")

# ---- tr[8] 课题应具备条件 ----
c = tc(8, 1)
ps = paras(c)
del_red_runs(ps[0])                                        # 标题1：删红色说明
conds = [
    "（1）掌握Java面向对象程序设计、数据结构与算法、数据库原理等专业基础知识；",
    "（2）掌握Spring Boot、MyBatis-Plus等后端框架，以及MySQL、Redis数据库应用技术；",
    "（3）掌握Vue.js等前端开发技术，了解大语言模型基本原理与本地化部署调用方法。",
]
for i, txt in zip((1, 2, 3), conds):
    rs = runs(ps[i])
    set_run_text(rs[0], txt)
    for r in rs[1:]:
        set_run_text(r, "")
    set_indent2(ps[i])
del_red_runs(ps[4])                                        # 标题2：删红色说明
hw = [
    "（1）个人电脑一台（建议16GB以上内存），安装JDK17、Node.js、MySQL8、Redis及微信开发者工具等开发环境；",
    "（2）Ollama本地大模型运行环境（llama3.2等开源模型），具备可联网下载模型与依赖库的网络条件。",
]
for i, txt in zip((5, 6), hw):
    rs = runs(ps[i])
    set_run_text(rs[0], txt)
    for r in rs[1:]:
        set_run_text(r, "")
    set_indent2(ps[i])
del_red_runs(ps[8])                                        # 时间要求：删红色
rs = runs(ps[9])                                           # 工作难度
set_run_text(rs[1], "2）工作难度：适中")
del_red_runs(ps[9])
rs = runs(ps[10])                                          # 工作任务量
set_run_text(rs[0], "（3）工作任务量：饱满")
del_red_runs(ps[10])
for i in (8, 9, 10):
    set_indent2(ps[i])

# ---- tr[9] 课题意义/研究内容/设计目标 ----
c = tc(9, 1)
ps = paras(c)
contents = [
    "1.课题意义：本课题针对通用聊天机器人缺乏人格化设定、记忆连续性不足、依赖云端服务存在隐私与成本问题等现状，设计并实现一个基于本地大模型的智能AI陪伴聊天系统。系统通过AI角色与AI精灵双体系提供个性化陪伴，基于“记忆—经历—感受—自我”的灵魂算法构建长期记忆机制，实现“越聊越懂你”的持续陪伴体验；采用Ollama本地化部署保障用户数据隐私、降低使用成本；支持Web端、桌面端与移动端多端使用，并融合好友、群聊、兴趣社群等社交功能。创新点包括：基于Ollama的开源大模型私有化部署方案、融合向量检索与时间衰减的AI记忆机制、AI桌面宠物互动以及三端统一架构。",
    "2.主要研究内容：（1）系统总体架构设计：前后端分离架构，Spring Boot 3.2.5、Vue 3、MySQL 8与Redis，支持Web端、Electron桌面端与移动端三端复用；（2）AI角色引擎：角色创建、人设提示词、上下文管理与角色一致性对话；（3）AI记忆系统：基于向量检索、重要度评分与时间衰减的长期记忆机制；（4）AI精灵与桌面宠物：形象更换、互动反馈与语音对话；（5）社交模块：好友、私聊、群聊、兴趣社群与动态发布；（6）语音交互：语音识别与语音合成；（7）管理员后台：用户管理、数据池管理、公告管理与AI系统管理；（8）安全合规：JWT认证、RBAC权限控制、内容审核与未成年保护；（9）测试体系：单元测试、集成测试与系统测试。",
    "3.主要设计目标：实现功能完整、安全健壮、流畅运行的智能AI陪伴聊天系统，覆盖Web端、桌面端与移动端三端；完成自动化测试用例1000个以上并全部通过，系统全链路稳定运行；交付符合福州理工学院毕业设计规范的设计文档、源代码与毕业论文。",
]
for idx in (0, 4, 7):
    del_red_runs(ps[idx])
for i, txt in zip((2, 5, 8), contents):
    rs = runs(ps[i])
    set_run_text(rs[0], txt)
    set_indent2(ps[i])

doc.save(T1)

total = sum(len(t) for t in contents)
print("课题意义三段合计字数:", total)
import zipfile
z = zipfile.ZipFile(T1)
d = z.read('word/document.xml').decode('utf-8')
print("commentReference:", d.count('commentReference'))
print("commentRangeStart:", d.count('commentRangeStart'))
print("红色run数:", d.count('FF0000'))
print("T1 REBUILT OK")

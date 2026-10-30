# -*- coding: utf-8 -*-
"""T2 开题报告完整重建：保留全部 18 条批注锚点；删红色/黄色示例；填真实内容"""
import shutil, copy
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

SRC = r"C:\Users\郭耀彬\Desktop\4.学号-姓名-开题报告.docx"
T2 = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-XXX-开题报告（填写版）.docx"
PLACEHOLDER = "×××"
TITLE = "基于本地大模型的智能AI陪伴聊天系统的设计与实现"

shutil.copyfile(SRC, T2)
doc = Document(T2)
root = doc.element
W = qn('w:')  # 仅用于辅助

def q(tag):
    return qn('w:' + tag)

tbls = root.findall('.//' + q('tbl'))
cov = tbls[0]
cov_trs = cov.findall(q('tr'))

def tc_of(tr, idx):
    return tr.findall(q('tc'))[idx]

def paras_of(tc):
    return tc.findall(q('p'))

def runs_of(p):
    return p.findall(q('r'))

def ptext(p):
    return "".join(t.text or "" for t in p.iter(q('t')))

def set_run_text(r, text):
    ts = r.findall(q('t'))
    if not ts:
        t = OxmlElement('w:t'); t.text = text; t.set(qn('xml:space'), 'preserve'); r.append(t)
    else:
        ts[0].text = text
        for t in ts[1:]:
            t.text = ""

def set_para_text(p, text):
    """段落级：首 run 存文本，其余文本 run 清空（保留所有 run 结构与批注）"""
    rs = runs_of(p)
    if not rs:
        r = OxmlElement('w:r'); p.append(r)
        set_run_text(r, text); return
    set_run_text(rs[0], text)
    for r in rs[1:]:
        set_run_text(r, "")

def del_para(p):
    p.getparent().remove(p)

def del_red_runs(p):
    for r in list(runs_of(p)):
        rPr = r.find(q('rPr'))
        if rPr is not None:
            col = rPr.find(q('color'))
            if col is not None and col.get(qn('w:val'), '').upper() in ('FF0000', 'RED'):
                p.remove(r)

def unyellow(p):
    for hl in list(p.iter(q('highlight'))):
        hl.getparent().remove(hl)

# ================= 封面表 =================
# row0 题目
set_para_text(paras_of(tc_of(cov_trs[0], 1))[0], TITLE)
# row1 提示行：整行删除（含红色 run）
cov_trs[1].getparent().remove(cov_trs[1])
# row2 姓名（红色示例 → ×××）
c = tc_of(cov_trs[2], 1)
ps = paras_of(c)
set_para_text(ps[0], PLACEHOLDER)
del_red_runs(ps[0])
# row3 学号
set_para_text(paras_of(tc_of(cov_trs[3], 1))[0], "2301132013")
# row5 专业
set_para_text(paras_of(tc_of(cov_trs[5], 1))[0], "计算机科学与技术")
# row6 年级
c = tc_of(cov_trs[6], 1)
ps = paras_of(c)
if runs_of(ps[0]):
    set_para_text(ps[0], "2023级")
else:
    r = OxmlElement('w:r'); ps[0].append(r); set_run_text(r, "2023级")
# row7 指导教师签名
c = tc_of(cov_trs[7], 1)
ps = paras_of(c)
if runs_of(ps[0]):
    set_para_text(ps[0], PLACEHOLDER)
else:
    r = OxmlElement('w:r'); ps[0].append(r); set_run_text(r, PLACEHOLDER)

# ================= 论文题目行（大表 row0） =================
big = tbls[1]
big_trs = big.findall(q('tr'))
set_para_text(paras_of(tc_of(big_trs[0], 0))[0], "论文题目：" + TITLE)

# ================= 正文大格（row1 tc0，105 段） =================
bigcell = tc_of(big_trs[1], 0)
ps = bigcell.findall(q('p'))

# 内容定义（段落索引 → 文本）——基于上一轮已验证内容
contents = {
    0: "1. 选题背景及研究的目的和意义",
    1: "1.1 选题背景",
    2: "大语言模型自2017年Transformer架构提出以来发展迅速，以ChatGPT为代表的生成式大模型在自然语言理解与生成方面表现出强大能力，已广泛应用于智能问答、内容创作、代码生成等领域。与此同时，人们对人工智能的需求正从“能回答问题”的工具型交互，逐步转向“能提供情感支持与长期陪伴”的关系型交互，Character.AI、星野等AI角色扮演与陪伴类产品快速兴起，受到大量年轻用户欢迎。然而，现有方案仍存在明显不足：一是通用大模型助手缺乏稳定的人格设定与情感连续性，难以形成长期陪伴关系；二是云端API服务存在数据隐私泄露风险与持续调用成本，用户对话数据无法完全自主掌控；三是单一终端产品难以满足用户在电脑、手机等多场景下的使用需求。在此背景下，设计并实现一个基于本地大模型的智能AI陪伴聊天系统，具有现实必要性与可行性。",
    3: "1.2 研究目的",
    4: "本课题旨在设计并实现一个基于本地大模型的智能AI陪伴聊天系统，具体目的包括：（1）利用Ollama本地化部署方案，实现大模型的私有化运行，保障用户数据隐私并降低使用成本；（2）通过AI角色与AI精灵双体系设计，为用户提供人格化、个性化的陪伴交互体验；（3）构建长期记忆机制，使AI能够持续记住用户并保持角色一致性；（4）实现Web端、桌面端与移动端多端覆盖，满足不同场景下的使用需求；（5）围绕系统建立完整的测试体系，确保功能与交付质量。",
    5: "1.3 研究意义",
    6: "本课题的研究意义体现在三个方面：在应用层面，系统可为用户提供情感陪伴、娱乐互动与知识问答等多样化服务，缓解现代社会中的孤独感与精神陪伴需求；在技术层面，课题涉及本地大模型部署、提示词工程、记忆机制、多端架构等关键技术，是对大模型应用工程化的有益探索；在人才培养层面，课题涵盖需求分析、系统设计、编码实现、测试与论文撰写全流程，能够全面锻炼综合工程能力。",
    7: "2. 国内外在该方向的研究现状及分析",
    8: "国外方面，OpenAI推出的ChatGPT引领了生成式大模型的发展，其多轮对话与指令遵循能力达到较高水平；Anthropic推出的Claude在长上下文理解与安全性方面表现突出。在陪伴类产品方面，Character.AI通过角色扮演对话为用户提供情感陪伴，Replika则聚焦AI伴侣场景。在基础研究方面，Vaswani等人提出的Transformer架构奠定了大语言模型的基础，Zhao等人对大规模语言模型的研究进展进行了系统综述。",
    12: "国内方面，字节跳动推出豆包、月之暗面推出Kimi、百度推出文心一言等大模型产品，在中文理解与多模态交互方面各具特色；星野、猫箱等角色扮演类产品在年轻用户中具有较高热度。在本地化部署方面，Ollama、llama.cpp、LM Studio等开源工具使大模型能够在消费级硬件上离线运行，极大降低了私有化部署门槛，相关研究已应用于智能客服、知识库问答等场景。",
    14: "综合分析，现有研究存在以下不足：通用大模型助手缺乏稳定人格与长期记忆；角色扮演类产品多依赖云端服务，存在隐私与成本问题；本地化部署研究多面向工具型问答，缺乏与人格化陪伴、社交互动的融合。本课题将本地大模型、AI角色人格化、记忆机制与多端社交功能相结合，具有一定的创新性与研究价值。",
    15: "3. 主要研究内容",
    16: "（1）系统总体架构设计：设计前后端分离、分层清晰的系统架构，采用Spring Boot 3.2.5作为后端框架、Vue 3作为前端框架，结合MySQL 8与Redis实现数据存储与缓存，通过RESTful API与WebSocket/SSE实现前后端通信，并支持Web端、Electron桌面端与移动端（微信小程序）三端复用。",
    17: "（2）AI角色与记忆引擎：实现AI角色与AI精灵双体系的创建、设定与对话管理，包括角色人设提示词构建、上下文管理、角色一致性维持与流式对话输出；设计基于向量检索、重要度评分与时间衰减的长期记忆机制，让AI能够持续积累对用户的了解；支持语音识别与语音合成等交互方式，并实现可移动、可交互的AI桌面精灵。",
    18: "（3）社交与管理后台：实现好友、私聊、群聊、兴趣社群、动态发布等社交功能，构建用户与AI、用户与用户之间的互动生态；实现管理员后台的用户管理、数据池管理、公告管理与AI系统管理；建立基于JWT的认证授权与RBAC权限控制、内容审核与敏感词过滤、未成年保护模式等安全合规机制，并建立单元测试、集成测试与系统测试三层测试体系。",
    21: "4. 研究方案及进度安排，预期达到的目标",
    22: "4.1 研究方案",
    23: "本课题采用“需求分析—系统设计—编码实现—测试验证”的工程化流程推进。技术选型上，后端采用Spring Boot 3.2.5与MyBatis-Plus，结合Spring Security实现安全控制；前端采用Vue 3与Vite构建Web端与桌面端，微信开发者工具构建移动端；大模型采用Ollama部署llama3.2等开源模型，通过HTTP接口与SSE流式响应接入对话能力；数据层使用MySQL 8存储业务数据、Redis缓存热点数据；WebSocket用于消息推送与桌面精灵互动。系统总体架构分为表现层（三端界面）、应用层（聊天、记忆、社交、管理等业务模块）、服务层（大模型服务、语音服务、消息服务）与数据层（MySQL、Redis、本地模型库）；大模型接入采用统一模型适配层，支持多模型路由与降级切换；记忆机制采用“对话提取—向量化存储—相关检索—衰减更新”的闭环流程，模拟“这一刻的自己，加上发生的经历、对经历的感受与过去留下的记忆，共同形成下一刻的自己”的灵魂算法思想。",
    24: "4.2 进度安排",
    25: "4.3 预期达到的目标",
    26: "（1）完成基于本地大模型的智能AI陪伴聊天系统，实现Web端、桌面端与移动端三端完整可用；",
    27: "（2）实现AI角色与AI精灵双体系，支持人格化对话、语音交互与桌面宠物互动；",
    28: "（3）实现AI长期记忆机制，提供个性化陪伴体验；",
    29: "（4）实现好友、群聊、兴趣社群等社交功能，构建互动生态；",
    30: "（5）实现管理员后台与安全合规机制（JWT、RBAC、内容审核、未成年保护）；",
    31: "（6）建立三层测试体系，自动化测试用例达到1000个以上且全部通过，系统稳定运行；",
    33: "5. 为完成课题已具备和所需的条件",
    34: "（1）已系统掌握Java面向对象编程、数据结构与算法、数据库原理等专业知识，具备Spring Boot、MyBatis-Plus、MySQL、Redis、Vue.js等主流技术的开发能力；",
    35: "（2）已具备大语言模型基础理论知识，掌握Ollama本地部署与模型调用方法，完成了llama3.2等模型的本地运行验证；",
    36: "（3）已完成系统主体功能开发与初步测试，积累了Web端、桌面端、移动端三端联调经验；",
    37: "（4）具备一台高性能个人电脑（16GB以上内存），已安装JDK17、Node.js、MySQL8、Redis、Ollama、微信开发者工具等软硬件环境。",
    39: "6. 预计研究过程中可能遇到的困难和问题，以及解决的措施",
    40: "6.1 困难及问题",
    41: "（1）本地大模型受硬件算力限制，模型参数规模与推理速度受限，可能影响对话质量与响应体验；",
    42: "（2）AI角色在多轮对话中可能出现人格漂移与记忆混淆，长期记忆的准确提取与更新难度较大；",
    43: "（3）三端适配复杂度高，微信小程序存在能力限制（如WebSocket、本地存储策略），Electron桌面端存在跨平台兼容问题；",
    45: "6.2 解决措施",
    46: "（1）选用量化后的中小规模模型（如llama3.2系列），通过提示词优化、多模型路由与异步流式响应提升体验，必要时引入云端模型作为降级方案；",
    47: "（2）设计分层记忆结构，短期记忆采用会话上下文管理，长期记忆采用向量检索、重要度评分与时间衰减机制，并定期对角色档案进行一致性维护；",
    49: "7. 主要参考文献",
}

# 删除的纯示例段（无批注）
delete_idx = [9, 10, 11, 13, 19, 20, 32, 38, 44, 48] + list(range(55, 73)) + list(range(74, 105))

# 应用修改
for i, p in enumerate(ps):
    if i in contents:
        set_para_text(p, contents[i])
    elif i in delete_idx:
        del_para(p)
    elif i == 73:
        # 批注15 锚定段：清空文本、去黄色、去红色
        set_para_text(p, "")
        unyellow(p)
        del_red_runs(p)
    elif i in (50, 51, 52, 54):
        pass  # 参考文献 [1][2][3][12] 段后续单独填

# 参考文献 [1]-[12]
refs = [
    "VASWANI A, SHAZEER N, PARMAR N, et al. Attention is all you need[C]//Proceedings of the 31st International Conference on Neural Information Processing Systems. Long Beach: Curran Associates Inc., 2017: 5998-6008.",
    "ZHAO W X, ZHOU K, LI J, et al. A survey of large language models[J/OL]. arXiv preprint arXiv:2303.18223, 2023[2026-10-06]. https://arxiv.org/abs/2303.18223.",
    "赵鑫, 窦志成, 文继荣. 大语言模型时代下的信息检索研究发展趋势[J]. 中国科学基金, 2023, 37(5): 786-792.",
    "周志华. 机器学习[M]. 北京: 清华大学出版社, 2016.",
    "王昊奋, 漆桂林, 陈华钧. 知识图谱: 方法、实践与应用[M]. 北京: 电子工业出版社, 2019.",
    "Ollama. Ollama documentation[EB/OL]. [2026-10-06]. https://ollama.com.",
    "Pivotal Software. Spring Boot reference documentation[EB/OL]. [2026-10-06]. https://docs.spring.io/spring-boot.",
    "Vue.js. Vue.js documentation[EB/OL]. [2026-10-06]. https://cn.vuejs.org.",
    "Redis. Redis documentation[EB/OL]. [2026-10-06]. https://redis.io/docs.",
    "腾讯. 微信小程序开发文档[EB/OL]. [2026-10-06]. https://developers.weixin.qq.com/miniprogram/dev/framework.",
    "Electron. Electron documentation[EB/OL]. [2026-10-06]. https://www.electronjs.org/docs.",
    "李航. 统计学习方法[M]. 2版. 北京: 清华大学出版社, 2019.",
]
# 重新获取段（删除后索引变化：保留段顺序）
ps = bigcell.findall(q('p'))
# 找到 [1][2][3]、……（批注13/14）、[12] 段
seg1 = seg2 = seg3 = segDots = seg12 = None
for p in ps:
    t = ptext(p).strip()
    if t == "[1]" and seg1 is None: seg1 = p
    elif t == "[2]" and seg2 is None: seg2 = p
    elif t == "[3]" and seg3 is None: seg3 = p
    elif t.startswith("……") and segDots is None: segDots = p
    elif t == "[12]" and seg12 is None: seg12 = p

def fill_ref(p, text):
    # 段落文本 = 原"[1]" → "1. 文献内容"；保留 run 结构
    set_para_text(p, "[" + text.split(" ", 1)[0].replace("[", "").replace("]", "") + "] " + text)

# [1][2][3]
for p, txt in zip((seg1, seg2, seg3), refs[:3]):
    fill_ref(p, txt)
# ……段 → [4]（保留批注13/14）
set_para_text(segDots, "[4] " + refs[3])
# [12] 段
set_para_text(seg12, "[12] " + refs[11])

# 克隆……段结构生成 [5]-[11]（去掉批注引用），插入到 [12] 前
def clone_without_refs(src_p, text):
    newp = copy.deepcopy(src_p)
    for el in list(newp.iter(q('commentReference'))) + list(newp.iter(q('commentRangeStart'))) + list(newp.iter(q('commentRangeEnd'))):
        el.getparent().remove(el)
    # 清文本后填
    for r in newp.findall(q('r')):
        for t in r.findall(q('t')):
            t.text = ""
    set_para_text(newp, text)
    return newp

insert_before = seg12
for i in range(4, 11):
    np_ = clone_without_refs(segDots, "[" + str(i + 1) + "] " + refs[i])
    insert_before.addprevious(np_)

# ================= 进度表 =================
nt = bigcell.findall(q('tbl'))[0]
nt_trs = nt.findall(q('tr'))
schedule = {
    1: ("2026年9月~2026年10月", "需求分析、文献调研、系统总体设计"),
    2: ("2026年10月", None),
    3: ("2026年10月~2026年12月", "系统开发与核心功能实现"),
    4: ("2026年12月", None),
    5: ("2026年12月~2027年3月", "功能完善、多端联调与系统测试"),
    6: ("2027年3月", None),
    7: ("2027年3月~2027年4月", "论文修改与完善"),
    8: ("2027年4月", None),
}
for ri, (tm, work) in schedule.items():
    tr = nt_trs[ri]
    tcs = tr.findall(q('tc'))
    set_para_text(paras_of(tcs[0])[0], tm)
    if work:
        c = tcs[1]
        ps2 = paras_of(c)
        if runs_of(ps2[0]):
            set_para_text(ps2[0], work)
        else:
            r = OxmlElement('w:r'); ps2[0].append(r); set_run_text(r, work)

doc.save(T2)
print("T2 REBUILT OK")
import zipfile
z = zipfile.ZipFile(T2)
d = z.read('word/document.xml').decode('utf-8')
print("commentReference:", d.count('commentReference'), "| commentRangeStart:", d.count('commentRangeStart'), "| FF0000:", d.count('FF0000'))

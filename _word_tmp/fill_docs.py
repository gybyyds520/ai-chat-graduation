# -*- coding: utf-8 -*-
"""填写两份毕业设计 Word：审核表 T1 + 开题报告 T2（保留模板格式）"""
import copy, sys
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.table import Table

TITLE = "基于本地大模型的智能AI陪伴聊天系统的设计与实现"
PLACEHOLDER = "×××"

def para_text(p):
    return "".join(r.text or "" for r in p.runs)

def set_cell_text(cell, text):
    """替换单元格文本，保留第一个段落的第一个 run 格式"""
    ps = list(cell.paragraphs)
    p0 = ps[0]
    for p in ps[1:]:
        p._element.getparent().remove(p._element)
    runs = list(p0.runs)
    if runs:
        first = runs[0]
        for r in runs[1:]:
            r._element.getparent().remove(r._element)
        first.text = text
    else:
        p0.add_run(text)
    return p0

def replace_in_cell(cell, old, new, occurrence=0):
    """在单元格段落内精确替换字符串（保留格式），occurrence 指定第几个匹配段落"""
    n = 0
    for p in cell.paragraphs:
        if old in para_text(p):
            if n < occurrence:
                n += 1
                continue
            for r in list(p.runs):
                if old in (r.text or ""):
                    r.text = r.text.replace(old, new)
                    return True
            # run 拆分时逐 run 处理
            full = para_text(p)
            new_full = full.replace(old, new, 1)
            # 重建单 run（保留第一个 run 格式）
            runs = list(p.runs)
            if runs:
                tmpl = runs[0]
                for r in runs[1:]:
                    r._element.getparent().remove(r._element)
                tmpl.text = new_full
            return True
    return False

# ---------- 格式化段落生成 ----------
def get_para_template(cell):
    """取单元格第一个段落的 pPr 与首个 run 的 rPr 作为模板"""
    pPr = None; rPr = None
    for p in cell.paragraphs:
        if p._p.pPr is not None:
            pPr = copy.deepcopy(p._p.pPr)
            for r in p.runs:
                if r._element.rPr is not None:
                    rPr = copy.deepcopy(r._element.rPr)
                break
            break
    return pPr, rPr

def make_para(pPr, rPr, text):
    p = OxmlElement('w:p')
    if pPr is not None:
        p.append(copy.deepcopy(pPr))
    r = OxmlElement('w:r')
    if rPr is not None:
        r.append(copy.deepcopy(rPr))
    t = OxmlElement('w:t')
    t.text = text
    t.set(qn('xml:space'), 'preserve')
    r.append(t)
    p.append(r)
    return p

def remove_paras_between(tc, start_el=None, end_el=None):
    """删除 tc 中 start_el 之后（不含）到 end_el 之前（不含）的所有 w:p；None=边界"""
    kids = list(tc)
    s = 0 if start_el is None else kids.index(start_el) + 1
    e = len(kids) if end_el is None else kids.index(end_el)
    removed = 0
    for el in list(kids[s:e]):
        if el.tag == qn('w:p'):
            tc.remove(el); removed += 1
    return removed

# ================= 任务1：审核表 T1 =================
def fill_t1(path):
    doc = Document(path)
    t = doc.tables[0]  # 唯一表格
    def cell(r, c):
        return t.rows[r].cells[c]

    # R2C4 专业
    set_cell_text(cell(1, 3), "计算机科学与技术")
    # R3C3 题目名称（行3，C3 是 colspan=12 的空单元格）
    set_cell_text(cell(2, 2), TITLE)
    # R4 教师行
    set_cell_text(cell(3, 2), PLACEHOLDER)   # 教师姓名
    set_cell_text(cell(3, 4), PLACEHOLDER)   # 职称
    set_cell_text(cell(3, 6), PLACEHOLDER)   # 学位学历
    set_cell_text(cell(3, 8), PLACEHOLDER)   # 研究方向
    # R5C3 课题类型
    replace_in_cell(cell(4, 2), "☑工程设计", "□工程设计")
    replace_in_cell(cell(4, 2), "□计算机软件", "☑计算机软件")
    # R7C3 结合实际情况
    replace_in_cell(cell(6, 2), "□其他", "☑其他")
    # R9C2 课题应具备的条件（11 段）
    c = cell(8, 1)
    repl = {
        "（1）": "（1）掌握Java面向对象程序设计、数据结构与算法、数据库原理等专业基础知识；",
        "（2）": "（2）掌握Spring Boot、MyBatis-Plus等后端框架，以及MySQL、Redis数据库应用技术；",
        "（3）": "（3）掌握Vue.js等前端开发技术，了解大语言模型基本原理与本地化部署调用方法。",
    }
    idx = 0
    for p in c.paragraphs:
        txt = para_text(p)
        if txt.strip() in ("（1）", "（2）", "（3）"):
            key = txt.strip()
            # 第 4-5 个填空（软硬件）在第 2 次出现的 （1）（2）
            if key == "（1）" and idx >= 3:
                repl[key] = "（1）个人电脑一台（建议16GB以上内存），安装JDK17、Node.js、MySQL8、Redis及微信开发者工具等开发环境；"
            if key == "（2）" and idx >= 3:
                repl[key] = "（2）Ollama本地大模型运行环境（llama3.2等开源模型），具备可联网下载模型与依赖库的网络条件。"
            for r in p.runs:
                r.text = r.text.replace(key, repl[key])
            idx += 1
        elif "（2）工作难度" in txt:
            for r in p.runs:
                if "工作难度" in (r.text or ""):
                    r.text = r.text.replace(r.text, "（2）工作难度：适中")
        elif "（3）工作任务量" in txt:
            for r in p.runs:
                if "工作任务量" in (r.text or ""):
                    r.text = r.text.replace(r.text, "（3）工作任务量：饱满")
    # R10C2 课题意义（替换 3 个 XXXX）
    c10 = cell(9, 1)
    texts = [
        "1.课题意义：本课题针对通用聊天机器人缺乏人格化设定、记忆连续性不足、依赖云端服务存在隐私与成本问题等现状，设计并实现一个基于本地大模型的智能AI陪伴聊天系统。系统通过AI角色与AI精灵双体系提供个性化陪伴，基于“记忆—经历—感受—自我”的灵魂算法构建长期记忆机制，实现“越聊越懂你”的持续陪伴体验；采用Ollama本地化部署保障用户数据隐私、降低使用成本；支持Web端、桌面端与移动端多端使用，并融合好友、群聊、兴趣社群等社交功能。创新点包括：基于Ollama的开源大模型私有化部署方案、融合向量检索与时间衰减的AI记忆机制、AI桌面宠物互动以及三端统一架构。",
        "2.主要研究内容：（1）系统总体架构设计：前后端分离架构，采用Spring Boot 3.2.5、Vue 3、MySQL 8与Redis，支持Web端、Electron桌面端与移动端三端复用；（2）AI角色引擎：角色创建、人设提示词构建、上下文管理、角色一致性对话与流式输出；（3）AI记忆系统：基于向量检索、重要度评分与时间衰减的长期记忆机制；（4）AI精灵与桌面宠物：形象更换、互动反馈与语音对话；（5）社交模块：好友、私聊、群聊、兴趣社群与动态发布；（6）语音交互：语音识别与语音合成；（7）管理员后台：用户管理、数据池管理、公告管理与AI系统管理；（8）安全合规：JWT认证、RBAC权限控制、内容审核与未成年保护；（9）测试体系：单元测试、集成测试与系统测试。",
        "3.主要设计目标：实现功能完整、安全健壮、流畅运行的智能AI陪伴聊天系统，覆盖Web端、桌面端与移动端三端；完成自动化测试用例1000个以上并全部通过，系统全链路稳定运行；交付符合福州理工学院毕业设计规范的设计文档、源代码与毕业论文。",
    ]
    n = 0
    for p in c10.paragraphs:
        if para_text(p).strip() == "XXXX":
            for r in p.runs:
                r.text = texts[n] if n < len(texts) else ""
            n += 1
    doc.save(path)
    print("T1 filled, XXXX count =", n)

# ================= 任务2：开题报告 T2 =================
def fill_t2(path):
    doc = Document(path)
    cover = doc.tables[0]   # 封面表
    body = doc.tables[1]    # 正文表

    # ---- 封面 ----
    set_cell_text(cover.rows[0].cells[1], TITLE)
    set_cell_text(cover.rows[2].cells[1], PLACEHOLDER)   # 姓名
    set_cell_text(cover.rows[3].cells[1], "2301132013")  # 学号
    set_cell_text(cover.rows[5].cells[1], "计算机科学与技术")  # 专业
    set_cell_text(cover.rows[6].cells[1], "2023级")      # 年级
    set_cell_text(cover.rows[7].cells[1], PLACEHOLDER)   # 指导教师签名
    # 删除提示行 R2（“若题目一行够写…”）
    hint_row = cover.rows[1]
    hint_row._tr.getparent().remove(hint_row._tr)

    # ---- 正文表 R1 论文题目 ----
    set_cell_text(body.rows[0].cells[0], "论文题目：" + TITLE)

    # ---- 正文大单元格 ----
    big = body.rows[1].cells[0]
    tc = big._tc
    # 找到嵌套进度表（T3）
    tbl_el = None
    for el in list(tc):
        if el.tag == qn('w:tbl'):
            tbl_el = el; break
    if tbl_el is None:
        raise RuntimeError("嵌套进度表未找到")
    # 定位表头“4.2 进度安排”段落（在 tbl 之前）
    kids = list(tc)
    before = [el for el in kids[:kids.index(tbl_el)] if el.tag == qn('w:p')]
    # 模板格式：取第一个正文段落
    pPr = None; rPr = None
    for el in before:
        p = el
        if p.pPr is not None:
            pPr = copy.deepcopy(p.pPr)
            for r in p.findall(qn('w:r')):
                if r.find(qn('w:rPr')) is not None:
                    rPr = copy.deepcopy(r.find(qn('w:rPr')))
                break
            break
    # ---- 删除 tbl 前后所有直接段落 ----
    removed_before = remove_paras_between(tc, None, tbl_el)
    removed_after = remove_paras_between(tc, tbl_el, None)

    # ---- 4.2 进度表更新（嵌套表） ----
    nt = Table(tbl_el, big._parent)
    schedule = [
        ("2026年9月~2026年10月", "需求分析、文献调研、系统总体设计"),
        ("2026年10月", "开题答辩"),
        ("2026年10月~2026年12月", "系统开发与核心功能实现"),
        ("2026年12月", "中期检查"),
        ("2026年12月~2027年3月", "功能完善、多端联调与系统测试"),
        ("2027年3月", "毕业设计（论文）评审"),
        ("2027年3月~2027年4月", "论文修改与完善"),
        ("2027年4月", "档案提交"),
    ]
    for i, (t1, t2) in enumerate(schedule):
        row = nt.rows[i + 1]
        set_cell_text(row.cells[0], t1)
        set_cell_text(row.cells[1], t2)

    # ---- 重建正文段落（tbl 前：1~4.1 节 + “4.2 进度安排”） ----
    before_lines = [
        "1. 选题背景及研究的目的和意义",
        "1.1 选题背景",
        "大语言模型自2017年Transformer架构提出以来发展迅速，以ChatGPT为代表的生成式大模型在自然语言理解与生成方面表现出强大能力，已广泛应用于智能问答、内容创作、代码生成等领域。与此同时，人们对人工智能的需求正从“能回答问题”的工具型交互，逐步转向“能提供情感支持与长期陪伴”的关系型交互，Character.AI、星野等AI角色扮演与陪伴类产品快速兴起，受到大量年轻用户欢迎。",
        "然而，现有方案仍存在明显不足：一是通用大模型助手缺乏稳定的人格设定与情感连续性，难以形成长期陪伴关系；二是云端API服务存在数据隐私泄露风险与持续调用成本，用户对话数据无法完全自主掌控；三是单一终端产品难以满足用户在电脑、手机等多场景下的使用需求。在此背景下，设计并实现一个基于本地大模型的智能AI陪伴聊天系统，具有现实必要性与可行性。",
        "1.2 研究目的",
        "本课题旨在设计并实现一个基于本地大模型的智能AI陪伴聊天系统，具体目的包括：（1）利用Ollama本地化部署方案，实现大模型的私有化运行，保障用户数据隐私并降低使用成本；（2）通过AI角色与AI精灵双体系设计，为用户提供人格化、个性化的陪伴交互体验；（3）构建长期记忆机制，使AI能够持续记住用户并保持角色一致性；（4）实现Web端、桌面端与移动端多端覆盖，满足不同场景下的使用需求；（5）围绕系统建立完整的测试体系，确保功能与交付质量。",
        "1.3 研究意义",
        "本课题的研究意义体现在三个方面：在应用层面，系统可为用户提供情感陪伴、娱乐互动与知识问答等多样化服务，缓解现代社会中的孤独感与精神陪伴需求；在技术层面，课题涉及本地大模型部署、提示词工程、记忆机制、多端架构等关键技术，是对大模型应用工程化的有益探索；在人才培养层面，课题涵盖需求分析、系统设计、编码实现、测试与论文撰写全流程，能够全面锻炼综合工程能力。",
        "2. 国内外在该方向的研究现状及分析",
        "国外方面，OpenAI推出的ChatGPT引领了生成式大模型的发展，其多轮对话与指令遵循能力达到较高水平；Anthropic推出的Claude在长上下文理解与安全性方面表现突出。在陪伴类产品方面，Character.AI通过角色扮演对话为用户提供情感陪伴，Replika则聚焦AI伴侣场景。在基础研究方面，Vaswani等人提出的Transformer架构奠定了大语言模型的基础，Zhao等人对大规模语言模型的研究进展进行了系统综述。",
        "国内方面，字节跳动推出豆包、月之暗面推出Kimi、百度推出文心一言等大模型产品，在中文理解与多模态交互方面各具特色；星野、猫箱等角色扮演类产品在年轻用户中具有较高热度。在本地化部署方面，Ollama、llama.cpp、LM Studio等开源工具使大模型能够在消费级硬件上离线运行，极大降低了私有化部署门槛，相关研究已应用于智能客服、知识库问答等场景。",
        "综合分析，现有研究存在以下不足：通用大模型助手缺乏稳定人格与长期记忆；角色扮演类产品多依赖云端服务，存在隐私与成本问题；本地化部署研究多面向工具型问答，缺乏与人格化陪伴、社交互动的融合。本课题将本地大模型、AI角色人格化、记忆机制与多端社交功能相结合，具有一定的创新性与研究价值。",
        "3. 主要研究内容",
        "本课题的主要研究内容包括：（1）系统总体架构设计：设计前后端分离、分层清晰的系统架构，采用Spring Boot 3.2.5作为后端框架、Vue 3作为前端框架，结合MySQL 8与Redis实现数据存储与缓存，通过RESTful API与WebSocket/SSE实现前后端通信，并支持Web端、Electron桌面端与移动端（微信小程序）三端复用；（2）AI角色引擎：实现AI角色的创建、设定与对话管理，包括角色人设提示词构建、上下文管理、角色一致性维持与流式对话输出，支持文本、语音（语音识别与语音合成）等多种交互方式；（3）AI记忆系统：设计基于向量检索、重要度评分与时间衰减的长期记忆机制，让AI能够持续积累对用户的了解，实现个性化陪伴体验；（4）AI精灵与桌面宠物：实现可移动、可交互的AI桌面精灵，支持形象更换、动画反馈与语音对话，增强陪伴感；（5）社交与互动模块：实现好友、私聊、群聊、兴趣社群、动态发布等社交功能，构建用户与AI、用户与用户之间的互动生态；（6）管理员后台：实现用户管理、数据池管理、公告管理、AI系统管理等功能，保障系统的可运营性；（7）安全与合规：实现基于JWT的认证授权与RBAC权限控制、登录失败锁定、内容审核与敏感词过滤、未成年保护模式等，保障系统安全合规；（8）测试体系：建立单元测试、集成测试与系统测试三层测试体系，对系统功能、接口、安全与性能进行全面验证。",
        "4. 研究方案及进度安排，预期达到的目标",
        "4.1 研究方案",
        "本课题采用“需求分析—系统设计—编码实现—测试验证”的工程化流程推进。技术选型上，后端采用Spring Boot 3.2.5与MyBatis-Plus，结合Spring Security实现安全控制；前端采用Vue 3与Vite构建Web端与桌面端，微信开发者工具构建移动端；大模型采用Ollama部署llama3.2等开源模型，通过HTTP接口与SSE流式响应接入对话能力；数据层使用MySQL 8存储业务数据、Redis缓存热点数据；WebSocket用于消息推送与桌面精灵互动。",
        "系统总体架构分为表现层（三端界面）、应用层（聊天、记忆、社交、管理等业务模块）、服务层（大模型服务、语音服务、消息服务）与数据层（MySQL、Redis、本地模型库）。大模型接入采用统一模型适配层，支持多模型路由与降级切换；记忆机制采用“对话提取—向量化存储—相关检索—衰减更新”的闭环流程，模拟“这一刻的自己，加上发生的经历、对经历的感受与过去留下的记忆，共同形成下一刻的自己”的灵魂算法思想。",
        "4.2 进度安排",
    ]
    insert_after = None
    for line in before_lines:
        p = make_para(pPr, rPr, line)
        tc.insert(tc.index(tbl_el), p)
        insert_after = p

    # ---- 重建 tbl 后段落（4.3~7 节 + 参考文献） ----
    after_lines = [
        "4.3 预期达到的目标",
        "（1）完成基于本地大模型的智能AI陪伴聊天系统，实现Web端、桌面端与移动端三端完整可用；",
        "（2）实现AI角色与AI精灵双体系，支持人格化对话、语音交互与桌面宠物互动；",
        "（3）实现AI长期记忆机制，提供个性化陪伴体验；",
        "（4）实现好友、群聊、兴趣社群等社交功能，构建互动生态；",
        "（5）实现管理员后台与安全合规机制（JWT、RBAC、内容审核、未成年保护）；",
        "（6）建立三层测试体系，自动化测试用例达到1000个以上且全部通过，系统稳定运行；",
        "（7）撰写符合规范的毕业设计论文并完成答辩。",
        "5. 为完成课题已具备和所需的条件",
        "（1）已系统掌握Java面向对象编程、数据结构与算法、数据库原理等专业知识，具备Spring Boot、MyBatis-Plus、MySQL、Redis、Vue.js等主流技术的开发能力；",
        "（2）已具备大语言模型基础理论知识，掌握Ollama本地部署与模型调用方法，完成了llama3.2等模型的本地运行验证；",
        "（3）已完成系统主体功能开发与初步测试，积累了Web端、桌面端、移动端三端联调经验；",
        "（4）具备一台高性能个人电脑（16GB以上内存），已安装JDK17、Node.js、MySQL8、Redis、Ollama、微信开发者工具等软硬件环境；",
        "（5）学校图书馆与知网等文献资源丰富，已检索并核验课题相关文献，为论文撰写提供支撑。",
        "6. 预计研究过程中可能遇到的困难和问题，以及解决的措施",
        "6.1 困难及问题",
        "（1）本地大模型受硬件算力限制，模型参数规模与推理速度受限，可能影响对话质量与响应体验；",
        "（2）AI角色在多轮对话中可能出现人格漂移与记忆混淆，长期记忆的准确提取与更新难度较大；",
        "（3）三端适配复杂度高，微信小程序存在能力限制（如WebSocket、本地存储策略），Electron桌面端存在跨平台兼容问题；",
        "（4）大模型输出内容不可完全控制，存在内容安全与合规风险，需要建立审核机制；",
        "（5）系统模块较多，测试覆盖与质量保障工作量较大。",
        "6.2 解决措施",
        "（1）选用量化后的中小规模模型（如llama3.2系列），通过提示词优化、多模型路由与异步流式响应提升体验，必要时引入云端模型作为降级方案；",
        "（2）设计分层记忆结构，短期记忆采用会话上下文管理，长期记忆采用向量检索、重要度评分与时间衰减机制，并定期对角色档案进行一致性维护；",
        "（3）采用响应式设计统一三端交互规范，针对微信小程序能力限制进行专项适配，在Electron端开展跨平台兼容性测试；",
        "（4）建立敏感词过滤、内容审核、用户举报与未成年保护机制，规范UGC内容发布；",
        "（5）采用三层测试体系与自动化测试工具，制定测试计划并持续回归，确保交付质量。",
        "7. 主要参考文献",
        "[1] VASWANI A, SHAZEER N, PARMAR N, et al. Attention is all you need[C]//Proceedings of the 31st International Conference on Neural Information Processing Systems. Long Beach: Curran Associates Inc., 2017: 5998-6008.",
        "[2] ZHAO W X, ZHOU K, LI J, et al. A survey of large language models[J/OL]. arXiv preprint arXiv:2303.18223, 2023[2026-10-06]. https://arxiv.org/abs/2303.18223.",
        "[3] 赵鑫, 窦志成, 文继荣. 大语言模型时代下的信息检索研究发展趋势[J]. 中国科学基金, 2023, 37(5): 786-792.",
        "[4] 周志华. 机器学习[M]. 北京: 清华大学出版社, 2016.",
        "[5] 王昊奋, 漆桂林, 陈华钧. 知识图谱: 方法、实践与应用[M]. 北京: 电子工业出版社, 2019.",
        "[6] Ollama. Ollama documentation[EB/OL]. [2026-10-06]. https://ollama.com.",
        "[7] Pivotal Software. Spring Boot reference documentation[EB/OL]. [2026-10-06]. https://docs.spring.io/spring-boot.",
        "[8] Vue.js. Vue.js documentation[EB/OL]. [2026-10-06]. https://cn.vuejs.org.",
        "[9] Redis. Redis documentation[EB/OL]. [2026-10-06]. https://redis.io/docs.",
        "[10] 腾讯. 微信小程序开发文档[EB/OL]. [2026-10-06]. https://developers.weixin.qq.com/miniprogram/dev/framework.",
        "[11] Electron. Electron documentation[EB/OL]. [2026-10-06]. https://www.electronjs.org/docs.",
        "[12] 李航. 统计学习方法[M]. 2版. 北京: 清华大学出版社, 2019.",
    ]
    anchor = tbl_el
    for line in after_lines:
        p = make_para(pPr, rPr, line)
        # 插到 anchor 之后（保持顺序）
        anchor.addnext(p)
        anchor = p

    doc.save(path)
    print("T2 filled, removed before/after =", removed_before, removed_after)

if __name__ == "__main__":
    t1 = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-XXX-题目审核表（填写版）.docx"
    t2 = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\4.2301132013-XXX-开题报告（填写版）.docx"
    if sys.argv[1] == "t1":
        fill_t1(t1)
    elif sys.argv[1] == "t2":
        fill_t2(t2)
    else:
        fill_t1(t1); fill_t2(t2)
    print("DONE")

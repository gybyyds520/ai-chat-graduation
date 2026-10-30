# -*- coding: utf-8 -*-
"""生成开题报告三张技术插图（Pillow，高清300dpi）"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\figs'
os.makedirs(OUT, exist_ok=True)

HEI = r'C:\Windows\Fonts\simhei.ttf'      # 黑体
YA = r'C:\Windows\Fonts\msyh.ttc'         # 微软雅黑

def F(path, size):
    return ImageFont.truetype(path, size)

def new_img(w, h, bg='white'):
    img = Image.new('RGB', (w, h), bg)
    return img, ImageDraw.Draw(img)

def box(d, xy, r=14, fill='#EAF2FB', outline='#2F6FB3', width=3):
    d.rounded_rectangle(xy, radius=r, fill=fill, outline=outline, width=width)

def ctext(d, xy, text, font, fill='#1F3864'):
    x0, y0, x1, y1 = xy
    tw, th = d.textbbox((0, 0), text, font=font)[2:]
    d.text(((x0+x1-tw)/2, (y0+y1-th)/2), text, font=font, fill=fill)

def arrow(d, p1, p2, color='#2F6FB3', w=3):
    d.line([p1, p2], fill=color, width=w)
    # 箭头头
    import math
    ang = math.atan2(p2[1]-p1[1], p2[0]-p1[0])
    L = 16
    for a in (ang+math.radians(150), ang-math.radians(150)):
        d.line([p2, (p2[0]+L*math.cos(a), p2[1]+L*math.sin(a))], fill=color, width=w)

def title(d, w, text, font, fill='#1F3864'):
    tw = d.textbbox((0, 0), text, font=font)[2]
    d.text(((w-tw)/2, 18), text, font=font, fill=fill)

# ============ 图1 三端功能架构图 ============
W, H = 1500, 850
img, d = new_img(W, H)

# 三端
ends = [
    ('Web 端（Vue 3）', ['角色对话', '记忆管理', '社交互动', '系统管理']),
    ('Electron 桌面端', ['沉浸式对话', '桌面精灵', '语音交互', '消息提醒']),
    ('移动端网页版', ['轻量对话', '角色浏览', '好友动态', '快捷入口']),
]
yw = 70; eh = 240; ew = 440; gap = 40; x0 = (W - ew*3 - gap*2)//2
for i, (name, items) in enumerate(ends):
    x = x0 + i*(ew+gap)
    box(d, (x, yw, x+ew, yw+eh), fill='#EAF2FB')
    ctext(d, (x, yw, x+ew, yw+64), name, F(HEI, 30), fill='#1F3864')
    for j, it in enumerate(items):
        d.text((x+30, yw+80+j*40), '· ' + it, font=F(YA, 24), fill='#333333')

# 后端
by = 400; bh = 150; bx = x0; bw = W - 2*x0
box(d, (bx, by, bx+bw, by+bh), fill='#FDF3E7', outline='#C98A2D')
ctext(d, (bx, by, bx+bw, by+58), '统一后端服务（Spring Boot 3.2.5 + MyBatis-Plus + Spring Security）', F(HEI, 30), fill='#8A5A00')
ctext(d, (bx, by+62, bx+bw, by+bh), '聊天对话 ｜ 记忆引擎 ｜ 社交互动 ｜ 系统管理 API   ·   WebSocket / SSE 实时通信', F(YA, 25), fill='#333333')

# 数据层
dy = 655; dh = 130
data_items = [('MySQL 8', '业务数据'), ('Redis', '缓存/在线状态'), ('Ollama 本地模型库', 'llama3.2 系列')]
dw2 = 430; dgap = 40; dx0 = (W - dw2*3 - dgap*2)//2
for i, (n, s) in enumerate(data_items):
    x = dx0 + i*(dw2+dgap)
    box(d, (x, dy, x+dw2, dy+dh), fill='#EDF7EE', outline='#3E8E4A')
    ctext(d, (x, dy, x+dw2, dy+60), n, F(HEI, 28), fill='#2E6B36')
    ctext(d, (x, dy+62, x+dw2, dy+dh), s, F(YA, 23), fill='#333333')

# 箭头
for i in range(3):
    cx = x0 + i*(ew+gap) + ew//2
    arrow(d, (cx, yw+eh), (cx, by), w=3)
for i in range(3):
    cx = x0 + i*(ew+gap) + ew//2
    arrow(d, (cx, by+bh), (cx, dy), w=3)
for i in range(3):
    cx = dx0 + i*(dw2+dgap) + dw2//2
    arrow(d, (cx, dy), (cx, by+bh), w=3)

# 侧标
d.text((x0, H-50), '三端共享同一后端与数据体系，会话、记忆与社交数据实时一致', font=F(YA, 24), fill='#666666')
img.save(os.path.join(OUT, 'fig1_sanduan.png'), dpi=(300, 300))

# ============ 图2 系统总体架构图 ============
W, H = 1500, 940
img, d = new_img(W, H)

layers = [
    ('表现层', ['Web 端（Vue 3）', 'Electron 桌面端', '移动端网页版'], '#EAF2FB', '#2F6FB3', '#1F3864'),
    ('应用层', ['聊天对话', 'AI角色与AI精灵', '记忆引擎', '社交互动', '系统管理'], '#FDF3E7', '#C98A2D', '#8A5A00'),
    ('服务层', ['大模型统一适配层（多模型路由·降级切换）', '语音服务（ASR / TTS）', '消息推送（WebSocket / SSE）'], '#EDF7EE', '#3E8E4A', '#2E6B36'),
    ('数据层', ['MySQL 8（业务数据）', 'Redis（缓存·在线状态）', '本地模型库（llama3.2）'], '#F2EFF9', '#6B4FA3', '#4A3373'),
]
ly = 60; lh = 180; lgap = 22; lx0 = 60; lw = W - 120
for i, (name, items, fill, oc, tc) in enumerate(layers):
    y = ly + i*(lh+lgap)
    box(d, (lx0, y, lx0+lw, y+lh), fill=fill, outline=oc)
    # 层名条
    box(d, (lx0, y, lx0+180, y+lh), fill=oc, outline=oc)
    ctext(d, (lx0, y, lx0+180, y+lh), name, F(HEI, 30), fill='white')
    # 层内容（动态列宽，避免溢出）
    n = len(items)
    item_gap = 22
    item_w = (lw - 210 - (n-1)*item_gap)//n
    item_h = 92
    tot = n*item_w + (n-1)*item_gap
    ix0 = lx0 + 210 + (lw - 210 - tot)//2
    for j, it in enumerate(items):
        ix = ix0 + j*(item_w+item_gap)
        iy = y + (lh - item_h)//2
        d.rounded_rectangle((ix, iy, ix+item_w, iy+item_h), radius=10, fill='white', outline=oc, width=2)
        ctext(d, (ix, iy, ix+item_w, iy+item_h), it, F(YA, 24), fill='#222222')
    if i < 3:
        ay = y + lh + lgap//2
        d.line([(W//2, ay-6), (W//2, ay+6)], fill='#888888', width=3)

d.text((lx0, H-56), '层间通过 RESTful API 与 WebSocket/SSE 通信，大模型经统一适配层接入，支持多模型路由与降级切换', font=F(YA, 24), fill='#666666')
img.save(os.path.join(OUT, 'fig2_zongti.png'), dpi=(300, 300))

# ============ 图3 灵魂算法与分层记忆流程图 ============
W, H = 1500, 900
img, d = new_img(W, H)

# 上栏：灵魂算法
d.text((60, 48), '① 灵魂算法：下一刻的自己 = 这一刻的自己 + 发生的经历 + 对经历的感受与理解 + 过去留下的记忆', font=F(HEI, 26), fill='#1F3864')
sx, sy, sw, sh = 70, 96, 260, 90
s1 = ('这一刻的自己', '#EAF2FB', '#2F6FB3')
s2 = ('发生的经历', '#EAF2FB', '#2F6FB3')
s3 = ('对经历的感受与理解', '#FDF3E7', '#C98A2D')
s4 = ('过去留下的记忆', '#EDF7EE', '#3E8E4A')
s5 = ('下一刻的自己', '#F2EFF9', '#6B4FA3')
seg_w = 250; seg_h = 90; seg_gap = 18
total = seg_w*4 + seg_gap*3
x = (W - total)//2
for i, (t, fill, oc) in enumerate([s1, s2, s3, s4]):
    box(d, (x, sy, x+seg_w, sy+seg_h), fill=fill, outline=oc)
    ctext(d, (x, sy, x+seg_w, sy+seg_h), t, F(YA, 24), fill='#222222')
    if i < 3:
        d.text((x+seg_w+2, sy+30), '+', font=F(HEI, 34), fill='#666666')
    x += seg_w + seg_gap
# 结果框
box(d, (x, sy, x+seg_w, sy+seg_h), fill=s5[1], outline=s5[2])
ctext(d, (x, sy, x+seg_w, sy+seg_h), s5[0], F(YA, 24), fill='#222222')
# 等号
d.text((x-22, sy+30), '=', font=F(HEI, 34), fill='#666666')

# 下栏：记忆闭环
d.text((60, 252), '② 分层记忆闭环：短期会话记忆 + 长期向量记忆', font=F(HEI, 26), fill='#1F3864')
steps = ['对话发生', '短期会话记忆', '重要度评分', '向量化存储', '相关检索召回', '衰减更新与遗忘', '参与下一轮对话']
sw2 = 180; sh2 = 84; sgap = 26
total2 = sw2*len(steps) + sgap*(len(steps)-1)
x2 = (W - total2)//2
y2 = 312
for i, st in enumerate(steps):
    box(d, (x2, y2, x2+sw2, y2+sh2), fill='#F5F7FA', outline='#5A6B7B')
    ctext(d, (x2, y2, x2+sw2, y2+sh2), st, F(YA, 23), fill='#222222')
    if i < len(steps)-1:
        arrow(d, (x2+sw2, y2+sh2//2), (x2+sw2+sgap, y2+sh2//2), w=3)
    x2 += sw2 + sgap

# 底部说明
d.text((60, 456), '记忆写入：对话提取 → 短期上下文管理；记忆沉淀：重要度评分 + 向量化 → 长期记忆库', font=F(YA, 24), fill='#333333')
d.text((60, 492), '记忆召回：相关检索 + 时间衰减 → 注入角色人格状态，保持人设一致性与成长性', font=F(YA, 24), fill='#333333')

# 一个角色成长示意小框
box(d, (60, 552, W-60, 872), fill='#FBFBFD', outline='#B8C4D0')
d.text((90, 574), '灵魂算法在系统中的落地实现', font=F(HEI, 28), fill='#1F3864')
impl = [
    '· 角色档案（人格设定）为“这一刻的自己”提供稳定基座；',
    '· 每次对话经历经“感受与理解”模块解析，形成情感与认知标签；',
    '· 长期记忆库按重要度与衰减系数沉淀“过去留下的记忆”；',
    '· 推理时三要素与历史记忆共同拼装提示词，生成“下一刻的自己”。',
]
for j, line in enumerate(impl):
    d.text((90, 618+j*44), line, font=F(YA, 25), fill='#222222')
d.text((90, 618+4*44+8), '→ 实现人格连续、记忆可溯、持续成长的 AI 陪伴体验', font=F(YA, 25), fill='#2F6FB3')
img.save(os.path.join(OUT, 'fig3_linghun.png'), dpi=(300, 300))

print('figs saved:', os.listdir(OUT))

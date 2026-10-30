# -*- coding: utf-8 -*-
import zipfile, sys
from lxml import etree
sys.stdout = sys.__stdout__
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
p = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\3.2301132013-郭耀彬-题目审核表.docx'
z = zipfile.ZipFile(p)
root = etree.fromstring(z.read('word/document.xml'))
body = root.find(W + 'body')

# 遍历段落，记录每段的文本与其中批注锚点
paras = []
for para in body.iter(W + 'p'):
    runs = []
    anchor = False
    for el in para.iter():
        tag = etree.QName(el).localname
        if tag == 'commentRangeStart':
            anchor = True
        if tag == 't':
            runs.append(el.text or '')
    text = ''.join(runs)
    if anchor:
        print('[批注锚点] %s...' % text[:50])
    if text.strip():
        paras.append(text)
print()
print('--- 与截图三处橙色对应的文字行（关键词） ---')
for i, t in enumerate(paras):
    if any(k in t for k in ('学生知识储备', '实验设备', '个人电脑', 'Vue', 'Ollama', '工作要求', '工作难度', '工作任务量', '课题意义')):
        print('%02d: %s...' % (i, t[:45]))

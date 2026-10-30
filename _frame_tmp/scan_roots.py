# -*- coding: utf-8 -*-
"""提取各视图模板根容器类名（供全局背景透明化）"""
import re, os, json

views = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\frontend\src\views'
res = {}
for f in sorted(os.listdir(views)):
    if not f.endswith('.vue'):
        continue
    p = os.path.join(views, f)
    txt = open(p, encoding='utf-8').read()
    m = re.search(r'<template>\s*<div([^>]*)>', txt)
    if not m:
        res[f] = 'NO_DIV_ROOT'
        continue
    attrs = m.group(1)
    cls = re.search(r'class="([^"]+)"', attrs)
    res[f] = cls.group(1).split()[0] if cls else '(no class)'
print(json.dumps(res, ensure_ascii=False, indent=1))

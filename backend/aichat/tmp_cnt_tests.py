# -*- coding: utf-8 -*-
import glob, re
t = f = e = s = 0
cls = 0
for fn in glob.glob('target/surefire-reports/TEST-*.xml'):
    txt = open(fn, encoding='utf-8', errors='replace').read()
    mt = re.search(r'\btests="(\d+)"', txt)
    mf = re.search(r'\bfailures="(\d+)"', txt)
    me = re.search(r'\berrors="(\d+)"', txt)
    ms = re.search(r'\bskipped="(\d+)"', txt)
    if mt:
        cls += 1
        t += int(mt.group(1)); f += int(mf.group(1)); e += int(me.group(1)); s += int(ms.group(1))
print('classes=%d tests=%d failures=%d errors=%d skipped=%d' % (cls, t, f, e, s))

# -*- coding: utf-8 -*-
import fitz, sys, os
sys.stdout = sys.__stdout__
pdf = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\_word_tmp\3.审核表.pdf'
outdir = r'C:\Users\郭耀彬\Desktop\gybct\.preview\t1_check'
os.makedirs(outdir, exist_ok=True)
d = fitz.open(pdf)
print('页数:', d.page_count)
for i in range(d.page_count):
    pix = d[i].get_pixmap(dpi=110)
    out = os.path.join(outdir, 'p%03d.png' % (i + 1))
    pix.save(out)
    print('已生成', out, pix.width, 'x', pix.height)

# -*- coding: utf-8 -*-
import fitz, sys, os
sys.stdout = sys.__stdout__
pdf = sys.argv[1]
outdir = sys.argv[2]
os.makedirs(outdir, exist_ok=True)
d = fitz.open(pdf)
print('页数:', d.page_count)
for i in range(d.page_count):
    pix = d[i].get_pixmap(dpi=100)
    out = os.path.join(outdir, 'p%03d.png' % (i + 1))
    pix.save(out)
    print('已生成', out)

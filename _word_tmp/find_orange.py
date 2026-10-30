# -*- coding: utf-8 -*-
import sys
import fitz
sys.stdout = sys.__stdout__
doc = fitz.open(r'C:\Users\郭耀彬\AppData\Local\Doubao\User Data\ClipboardTemp\2859c8f3-ee29-42f6-9e92-cc8b4dc18651.png')
page = doc[0]
pix = page.get_pixmap(dpi=72)
w, h = pix.width, pix.height
print('尺寸', w, h)
def px_at(x, y):
    i = (y * pix.stride) + x * pix.n
    return pix.samples[i], pix.samples[i + 1], pix.samples[i + 2]
def is_orange(r, g, b):
    return r > 190 and 90 < g < 190 and b < 90
rows = {}
for y in range(0, h, 2):
    for x in range(0, w, 2):
        r, g, b = px_at(x, y)
        if is_orange(r, g, b):
            rows.setdefault(y, []).append(x)
if not rows:
    print('未发现橙色像素')
else:
    ys = sorted(rows.keys())
    bands = []
    start = prev = ys[0]
    for y in ys[1:]:
        if y - prev > 15:
            bands.append((start, prev))
            start = y
        prev = y
    bands.append((start, prev))
    print('橙色区域（百分比）:')
    for a, b in bands[:30]:
        xall = []
        for yy in range(a, b + 1, 2):
            xall.extend(rows.get(yy, []))
        if xall:
            print('  y %.0f%%-%.0f%%  x %.0f%%-%.0f%%' % (a * 100 / h, b * 100 / h, min(xall) * 100 / w, max(xall) * 100 / w))

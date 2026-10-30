import re

# Read the file
with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)

lines = template.split('\n')

# Check lines 8-14
for i in range(7, 15):
    line = lines[i]
    line_num = i + 1
    
    if '</div>' not in line:
        opens = re.findall(r'<div[\s\S]*?>', line)
        for open_tag in opens:
            if '/>' not in open_tag:
                print(f'Line {line_num}: Found OPEN: {open_tag[:50].replace(chr(10), " ")}')

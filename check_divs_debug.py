import re

with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)

# Track divs properly - handle multi-line tags
stack = []
lines = template.split('\n')

for i, line in enumerate(lines):
    line_num = i + 1
    
    # Skip script and style
    if '<script' in line or '</script' in line or '<style' in line or '</style' in line:
        continue
    
    # Handle multi-line opening divs
    if '</div>' not in line:
        opens = re.findall(r'<div[\s\S]*?>', line)
        for open_tag in opens:
            if '/>' not in open_tag:
                clean_tag = open_tag[:60].replace('\n', ' ')
                stack.append((line_num, clean_tag))
                print(f'OPEN  [{line_num:3d}]: {clean_tag}')
    
    # Handle closing divs
    closes = re.findall(r'</div>', line)
    for close in closes:
        if stack:
            opened = stack.pop()
            print(f'CLOSE [{line_num:3d}]: </div> (was at line {opened[0]})')
        else:
            print(f'ERROR [{line_num:3d}]: Extra closing </div>')

if stack:
    print(f'\nUnclosed divs ({len(stack)}):')
    for item in stack:
        print(f'  Line {item[0]}: {item[1]}')

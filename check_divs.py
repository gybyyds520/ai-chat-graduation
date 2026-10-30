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
    
    # Handle multi-line opening divs - find any <div followed by content until >
    if '</div>' not in line:
        # Match opening divs but not self-closing or closing
        opens = re.findall(r'<div[\s\S]*?>', line)
        for open_tag in opens:
            if '/>' not in open_tag:  # Not self-closing
                stack.append((line_num, open_tag[:60].replace('\n', ' ')))
    
    # Handle closing divs
    closes = re.findall(r'</div>', line)
    for close in closes:
        if stack:
            opened = stack.pop()
        else:
            print(f'Line {line_num}: Extra closing </div>')

if stack:
    print(f'\nUnclosed divs ({len(stack)}):')
    for item in stack:
        print(f'  Line {item[0]}: {item[1]}')

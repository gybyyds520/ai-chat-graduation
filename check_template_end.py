import re

with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Find template end
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)
    print(f'Template length: {len(template)}')
    print(f'Last 500 chars: {repr(template[-500:])}')

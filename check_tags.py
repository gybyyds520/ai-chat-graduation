import re

# Read the file
with open('c:/Users/郭耀彬/Desktop/gybct/ai-chat-graduation/frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template content
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template_content = template_match.group(1)
    
    # Count opening and closing div tags
    opening_divs = len(re.findall(r'<div(?:\s[^>]*)?>(?!\s*</)', template_content))
    closing_divs = len(re.findall(r'</div>', template_content))
    
    print(f"Opening div tags: {opening_divs}")
    print(f"Closing div tags: {closing_divs}")
    
    if opening_divs != closing_divs:
        print(f"Difference: {opening_divs - closing_divs}")

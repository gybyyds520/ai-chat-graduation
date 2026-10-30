import re

# Read the file
with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)

# Find all opening divs (including multi-line) and closing divs
# Opening div pattern: <div ... > but not </div> or self-closing
opening_pattern = r'<div(?!\s*/)(?:\s[^>]*)?[^>]*>'
closing_pattern = r'</div>'

opens = [(m.start(), 'OPEN', m.group()) for m in re.finditer(opening_pattern, template)]
closes = [(m.start(), 'CLOSE', m.group()) for m in re.finditer(closing_pattern, template)]

# Interleave and sort by position
all_tags = sorted(opens + closes, key=lambda x: x[0])

# Track divs with line numbers
template_lines = template.split('\n')

def get_line_num(pos):
    return template[:pos].count('\n') + 1

# Process tags in order
stack = []
errors = []

for pos, tag_type, tag in all_tags:
    line_num = get_line_num(pos)
    
    # Skip script and style
    if tag_type == 'OPEN':
        if re.search(r'<script|</script|<style|</style', tag):
            continue
        stack.append((line_num, tag[:60].replace('\n', ' ')))
    else:  # CLOSE
        if stack:
            stack.pop()
        else:
            errors.append(f'Line {line_num}: Extra closing </div>')

if errors or stack:
    print("Errors:")
    for e in errors[:15]:
        print(f'  {e}')
    print(f"\nUnclosed divs ({len(stack)}):")
    for item in stack[:10]:
        print(f'  Line {item[0]}: {item[1]}')
else:
    print("All divs are properly closed!")

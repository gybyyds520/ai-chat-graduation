import re

# Read the file
with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)

# Find all opening divs using the correct pattern that handles multi-line
opening_pattern = r'<div(?!\s*/)(?:\s[^>]*)?[^>]*>'
closing_pattern = r'</div>'

opens = [(m.start(), m.group()) for m in re.finditer(opening_pattern, template)]
closes = [(m.start(), m.group()) for m in re.finditer(closing_pattern, template)]

# Interleave and sort by position
all_tags = sorted(opens + closes, key=lambda x: x[0])

# Track divs with line numbers
def get_line_num(pos):
    return template[:pos].count('\n') + 1

# Process tags in order
stack = []

for pos, tag in all_tags:
    line_num = get_line_num(pos)
    tag_short = tag[:60].replace('\n', ' ')
    
    # Skip script and style
    if re.search(r'<script|</script|<style|</style', tag):
        continue
    
    if tag.startswith('</'):
        # Closing tag
        if stack:
            opened_line, opened_tag = stack.pop()
            print(f'CLOSE [{line_num:3d}]: </div> (was at line {opened_line})')
        else:
            print(f'ERROR [{line_num:3d}]: Extra closing </div>')
    else:
        # Opening tag
        stack.append((line_num, tag_short))
        print(f'OPEN  [{line_num:3d}]: {tag_short[:50]}')

if stack:
    print(f'\nUnclosed divs ({len(stack)}):')
    for item in stack:
        print(f'  Line {item[0]}: {item[1][:50]}')

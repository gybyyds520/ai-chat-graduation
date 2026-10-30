import re

# Read the file
with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract template
template_match = re.search(r'<template>(.*?)</template>', content, re.DOTALL)
if template_match:
    template = template_match.group(1)

# Find all opening divs using the correct pattern that handles multi-line
# Match <div not followed by /, with any content until >
opening_pattern = r'<div(?!\s*/)(?:\s[^>]*)?[^>]*>'

matches = list(re.finditer(opening_pattern, template))

print(f"Found {len(matches)} opening divs")
print("\nFirst 20 opening divs:")
for i, m in enumerate(matches[:20]):
    line_num = template[:m.start()].count('\n') + 1
    tag = m.group()[:60].replace('\n', ' ')
    print(f"{i+1}. Line {line_num}: {tag}")

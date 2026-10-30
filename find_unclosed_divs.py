import re

# Read the file
with open('c:/Users/郭耀彬/Desktop/gybct/ai-chat-graduation/frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find template boundaries
template_start = None
template_end = None

for i, line in enumerate(lines):
    if '<template>' in line and template_start is None:
        template_start = i
    if '</template>' in line and template_start is not None:
        template_end = i
        break

if template_start is None or template_end is None:
    print("Could not find template")
    exit()

# Simple stack-based div tracker
stack = []
depth = 0

for i in range(template_start, template_end):
    line_num = i + 1
    line = lines[i]
    
    # Skip template and script tags
    if '<template>' in line or '</template>' in line or '<script' in line or '</script' in line:
        continue
    
    # Remove comments
    if '<!--' in line:
        continue
    
    # Find all div tags
    # Opening divs
    opening_pattern = r'<div(?:\s[^>]*)?>'
    opening_matches = list(re.finditer(opening_pattern, line))
    
    # Self-closing check
    for match in opening_matches:
        if '/' in line[match.end():match.end()+3]:
            # Self-closing, skip
            continue
        # Get the div with some context
        div_text = line[match.start():match.end()]
        stack.append((line_num, depth + 1, div_text[:60]))
        depth += 1
    
    # Closing divs
    closing_pattern = r'</div>'
    closing_matches = list(re.finditer(closing_pattern, line))
    
    for match in closing_matches:
        if stack:
            item = stack.pop()
            depth -= 1
        else:
            print(f"Line {line_num}: Extra closing </div>")

if stack:
    print(f"\nUnclosed div tags ({len(stack)} items):")
    for item in stack:
        print(f"  Line {item[0]}: depth {item[1]} - {item[2]}")

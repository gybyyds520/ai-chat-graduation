import re

# Read the file
with open('c:/Users/郭耀彬/Desktop/gybct/ai-chat-graduation/frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find template boundaries
template_start = None
template_end = None

for i, line in enumerate(lines):
    if '<template>' in line:
        template_start = i
    if '</template>' in line and template_start is not None:
        template_end = i
        break

if template_start is None or template_end is None:
    print("Could not find template")
    exit()

print(f"Template from line {template_start + 1} to {template_end + 1}")

# Check div tag balance line by line
stack = []
errors = []

for i in range(template_start, template_end):
    line_num = i + 1
    line = lines[i]
    
    # Find opening divs
    opening_matches = re.finditer(r'<div(?:\s[^>]*)?>(?!\s*</)', line)
    for match in opening_matches:
        stack.append((line_num, line.strip()[:50]))
    
    # Find closing divs
    closing_matches = re.finditer(r'</div>', line)
    for match in closing_matches:
        if stack:
            opened = stack.pop()
            print(f"Line {line_num}: Closed - {opened[1]}")
        else:
            errors.append(f"Line {line_num}: Extra closing </div> - {line.strip()[:50]}")

if stack:
    print(f"\nUnclosed div tags ({len(stack)}):")
    for item in stack:
        print(f"  Line {item[0]}: {item[1]}")
        
if errors:
    print(f"\nErrors:")
    for err in errors:
        print(f"  {err}")

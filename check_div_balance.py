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

print(f"Template from line {template_start + 1} to {template_end + 1}\n")

# Simple div counter
depth = 0
max_depth = 0
issues = []

for i in range(template_start, template_end):
    line_num = i + 1
    line = lines[i]
    
    # Count opening divs (not self-closing)
    opening = len(re.findall(r'<div(?:\s[^>]*)?>', line))
    # Exclude self-closing divs
    opening -= len(re.findall(r'<div[^>]*/\s*>', line))
    
    # Count closing divs
    closing = len(re.findall(r'</div>', line))
    
    if opening > 0:
        for _ in range(opening):
            depth += 1
            max_depth = max(max_depth, depth)
            # Print opening
            text = line.strip()[:80]
            print(f"Line {line_num:3d} [OPEN +{depth}]: {text}")
    
    if closing > 0:
        for _ in range(closing):
            if depth > 0:
                depth -= 1
                text = line.strip()[:80]
                print(f"Line {line_num:3d} [CLOSE -{depth + 1}]: {text}")
            else:
                issues.append(f"Line {line_num}: Extra closing </div>")
                print(f"Line {line_num:3d} [ERROR - EXTRA CLOSE]")

print(f"\nFinal depth: {depth}")
if depth > 0:
    print(f"Missing {depth} closing </div> tags")
if issues:
    print(f"\nIssues found: {len(issues)}")
    for issue in issues:
        print(f"  {issue}")

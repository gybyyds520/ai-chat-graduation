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

print("=" * 80)
print("DIV TAG TRACKING")
print("=" * 80)

# Stack-based tracker
stack = []
in_template = False
in_script = False

for i in range(template_start, template_end):
    line_num = i + 1
    line = lines[i]
    
    # Skip template/script tags
    if '<template>' in line or '</template>' in line or '<script' in line or '</script' in line:
        continue
    
    # Remove Vue comments
    if '<!--' in line:
        continue
    
    # Find opening divs
    opening_matches = list(re.finditer(r'<div(?:\s[^>]*)?>', line))
    for match in opening_matches:
        # Check if self-closing
        start = match.start()
        end = match.end()
        rest = line[end:]
        # Simple self-closing check
        if '/div' in rest[:5]:
            continue
        
        div_text = line[match.start():match.end()][:70]
        stack.append((line_num, div_text))
        indent = "  " * (len(stack) - 1)
        print(f"{indent}OPEN  [{line_num:3d}]: {div_text}")
    
    # Find closing divs
    closing_matches = list(re.finditer(r'</div>', line))
    for match in closing_matches:
        if stack:
            item = stack.pop()
            indent = "  " * len(stack)
            print(f"{indent}CLOSE [{line_num:3d}]: </div> (was opened at line {item[0]})")
        else:
            indent = "  "
            print(f"{indent}ERROR [{line_num:3d}]: Extra closing </div>")

print("\n" + "=" * 80)
if stack:
    print(f"UNCLOSED DIVS ({len(stack)} items):")
    for item in stack:
        print(f"  Line {item[0]}: {item[1][:70]}")
else:
    print("All divs are properly closed!")

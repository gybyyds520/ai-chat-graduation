import re

with open('frontend/src/views/AdminView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Find the REAL template end (last </template>)
last_template_end = content.rfind('</template>')
if last_template_end == -1:
    print("No </template> found!")
    exit()

# Find the first <template>
first_template_start = content.find('<template>')
if first_template_start == -1:
    print("No <template> found!")
    exit()

# Extract template content
template = content[first_template_start:last_template_end]

# Now analyze this template
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
errors = []

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
            # print(f'CLOSE [{line_num:3d}]: </div> (was at line {opened_line})')
        else:
            errors.append(f'ERROR [{line_num:3d}]: Extra closing </div>')
    else:
        # Opening tag
        stack.append((line_num, tag_short))
        # print(f'OPEN  [{line_num:3d}]: {tag_short[:50]}')

if errors or stack:
    print("Errors found:")
    for e in errors[:10]:
        print(f'  {e}')
    if stack:
        print(f'\nUnclosed divs ({len(stack)}):')
        for item in stack[:10]:
            print(f'  Line {item[0]}: {item[1][:50]}')
else:
    print("All divs are properly closed!")

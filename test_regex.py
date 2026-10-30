import re

# Test the regex on a multi-line opening tag
text = """        <div
          :class="['menu-item', { active: currentTab === 'users' }]"
          @click="handleTabChange('users')"
        >
"""

# Try to find the opening tag
opens = re.findall(r'<div(?:\s[^>]*)?>', text)
print(f'Found opens with simple regex: {opens}')

# Another approach - find any <div followed by content until >
opens2 = re.findall(r'<div[\s\S]*?>', text)
print(f'Found opens with [\s\S]*? regex: {opens2}')

# Another approach - match <div not followed by /
opens3 = re.findall(r'<div(?!\s*/)[^>]*>', text)
print(f'Found opens with negative lookahead: {opens3}')

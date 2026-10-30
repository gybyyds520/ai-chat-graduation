# -*- coding: utf-8 -*-
"""给 AI 记忆(MemoryView)与发现角色(PlazaView)左上角添加返回按钮"""
import io

V = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\frontend\src\views'

# ==================== MemoryView.vue ====================
p = V + r'\MemoryView.vue'
src = io.open(p, encoding='utf-8').read()

# 1. 模板：memory-header 内最前插入返回按钮
old_tpl = '''    <div class="memory-header">
      <div>
        <h2>AI 记忆</h2>'''
new_tpl = '''    <div class="memory-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div>
        <h2>AI 记忆</h2>'''
assert old_tpl in src, 'MemoryView template header not found'
src = src.replace(old_tpl, new_tpl, 1)

# 2. script：imports
old_imp = "import { ref, computed, onMounted } from 'vue'\nimport { ElMessage } from 'element-plus'"
new_imp = "import { ref, computed, onMounted } from 'vue'\nimport { useRouter } from 'vue-router'\nimport { ArrowLeft } from '@element-plus/icons-vue'\nimport { ElMessage } from 'element-plus'"
assert old_imp in src, 'MemoryView imports not found'
src = src.replace(old_imp, new_imp, 1)

# 3. script：添加 router + goBack（挂在第一个 const 之前）
old_script = "<script setup>\n"
# 在 <script setup> 之后找第一个 ref 定义行作为插入锚点
anchor = "const records = ref([])"
assert anchor in src, 'MemoryView script anchor not found'
go_back = """const router = useRouter()
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    router.push('/chat')
  }
}
"""
src = src.replace(anchor, go_back + anchor, 1)

# 4. style：memory-header 布局调整 + back-btn
old_css = '''.memory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}'''
new_css = '''.memory-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 8px;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}'''
assert old_css in src, 'MemoryView css not found'
src = src.replace(old_css, new_css, 1)

# header-tools 推到最右
old_tools = '''.header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}'''
new_tools = '''.header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}'''
assert old_tools in src, 'MemoryView header-tools css not found'
src = src.replace(old_tools, new_tools, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('MemoryView.vue OK')

# ==================== PlazaView.vue ====================
p = V + r'\PlazaView.vue'
src = io.open(p, encoding='utf-8').read()

# 1. 模板：plaza-header 内插入返回按钮
old_tpl = '''    <div class="plaza-header">
      <h2>角色广场</h2>
      <p>发现并与有趣的 AI 角色开始对话</p>
    </div>'''
new_tpl = '''    <div class="plaza-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div>
        <h2>角色广场</h2>
        <p>发现并与有趣的 AI 角色开始对话</p>
      </div>
    </div>'''
assert old_tpl in src, 'PlazaView template header not found'
src = src.replace(old_tpl, new_tpl, 1)

# 2. script：ArrowLeft import
old_imp = "import { useRouter } from 'vue-router'\nimport { ElMessage } from 'element-plus'"
new_imp = "import { useRouter } from 'vue-router'\nimport { ArrowLeft } from '@element-plus/icons-vue'\nimport { ElMessage } from 'element-plus'"
assert old_imp in src, 'PlazaView imports not found'
src = src.replace(old_imp, new_imp, 1)

# 3. script：goBack（const router = useRouter() 之后）
anchor = "const router = useRouter()"
go_back = """const router = useRouter()
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    router.push('/chat')
  }
}
"""
src = src.replace(anchor, go_back, 1)

# 4. style：plaza-header flex + back-btn
old_css = '''.plaza-header h2 { margin: 0 0 4px; }
.plaza-header p { color: var(--text-secondary); margin: 0 0 16px; }'''
new_css = '''.plaza-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 8px;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}
.plaza-header h2 { margin: 0 0 4px; }
.plaza-header p { color: var(--text-secondary); margin: 0 0 16px; }'''
assert old_css in src, 'PlazaView css not found'
src = src.replace(old_css, new_css, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(src)
print('PlazaView.vue OK')

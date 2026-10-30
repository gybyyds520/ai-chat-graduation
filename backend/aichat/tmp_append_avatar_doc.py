# -*- coding: utf-8 -*-
import io

section = """

## 27. 补充修复：创建角色上传头像后，列表仍显示随机头像（前端渲染 bug）

### 现象
用户反馈：创建角色时明明上传了头像，但 AI 角色列表里显示的不是上传的头像而是随机头像。

### 根因
- 后端链路正常：`POST /api/character/upload-avatar` 返回相对路径 `/uploads/character/xxx.png`（真实环境验证 200，GET 该 URL 200 image/png），`createCharacter` 正确保存 `avatar` 字段；
- **前端 ChatView.vue 渲染 bug**：角色列表项、聊天头部、消息头像全部使用 `<LocalAvatar :seed="character.id" />`——LocalAvatar 是本地生成头像组件（按 seed 哈希颜色 + 取末字符），**完全没有使用 `character.avatar` 字段**，因此无论上传什么头像，界面始终显示按角色 id 生成的"随机头像"；
- 对比：角色广场页（PlazaView）逻辑正确（`v-if="c.avatar"` 显示真实头像），故广场显示正常、聊天列表异常。

### 修复
- **LocalAvatar.vue**：新增可选 `src` prop——传入时渲染 `<img :src>`（圆形裁剪 + object-fit: cover），不传时保持原有本地生成头像逻辑，向后兼容；
- **ChatView.vue 5 处**传 `:src="character.avatar"`：角色列表项、聊天头部（40px）、对话框头像（64px）、消息 AI 头像（36px ×2）；
- **AdminView.vue** 角色管理表格头像列传 `:src="scope.row.avatar"`；
- **vitest.config.js** 补 `@vitejs/plugin-vue`（此前缺失导致 .vue 组件无法在测试中转换）。

### 顺带修复
- **alarm.test.js 跨午夜边界缺陷**：`once模式下应该在时间已过时返回null` 用例用 `now-1h` 构造过去时间，凌晨 0 点运行时跨到昨天 23 点，而"今天 23:00"尚未到来，业务正确返回时间导致断言误失败。改用 `vi.setSystemTime` 固定时间（2026-01-15 12:00，闹钟 11:00 已过）后断言确定成立。属测试侧修复，业务逻辑正确。

### 验证
- 新增 `tests/unit/localAvatar.test.js` **4 用例**（无 src 渲染本地头像 / 有 src 渲染真实图片 / 空 src 回退 / size 生效）；
- 前端全量：**31 文件 398 用例全部通过**（基线 394 + 新增 4，且修复了 1 个跨日偶发失败）；
- `npm run build` ✅；
- 真实环境：登录 demo_alice 上传 PNG → 200 返回 `/uploads/character/xxx.png` → GET 200 image/png（vite 已代理 /uploads → 8080，浏览器可直接显示）。

### 回归
纯前端改动，后端基线（721 用例）不受影响。
"""

path = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\测试执行报告.md"
with io.open(path, "r", encoding="utf-8") as f:
    text = f.read()
with io.open(path, "a", encoding="utf-8") as f:
    f.write(section)
print("appended, new len =", len(text) + len(section))

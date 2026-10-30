# -*- coding: utf-8 -*-
import io

section = """

## 24. 补充修复：学生端通知/隐私设置/动态页"点进去弹登录"（userId 缺失 bug）

### 现象
用户反馈"学生端通知点进去直接弹出登录"。

### 根因
- 前端登录成功（LoginView.handleLogin）只持久化 `token` 和 `user`（JSON 字符串），**从不单独存 `userId`**；
- 而 NotificationView / PrivacyView 直接 `localStorage.getItem('userId') || '1'`，取到默认值 **1**，与真实登录用户（如 demo_alice=82）不符；
- 后端 `AuthContext.requireSelf` 校验失败返回 **401** → request.js 拦截器清 token 并跳 `/login` → 用户看到"弹出登录"。
- 真实环境验证：正确 userId=82+token → 200；错误 userId=1+token → **401**（旧 bug 链路复现成功）。

### 修复
- 新建 `frontend/src/utils/auth.js`：`getCurrentUser()` / `getCurrentUserId()` / `getCurrentUserNickname()`，统一从 localStorage 'user' JSON 解析，解析失败返回 null；
- **3 文件 5 处引用替换**：
  1. `NotificationView.vue`：loadNotifications、markAllAsRead（2 处）改用 `getCurrentUserId()`，取不到则直接 return 不发请求；
  2. `PrivacyView.vue`：loadSettings、saveSetting（2 处）同改；
  3. `SocialFeedView.vue`：currentUserId 兜底改用 `getCurrentUserId() || 0`（替换内联 JSON.parse 逻辑）。
- 防御效果：未登录时不再发 userId=1 的错误请求，避免 401 误跳登录。

### 验证
- 新增 `tests/unit/auth.test.js` **9 用例**（解析/损坏 JSON/兼容 userId 字段/无 id 返回 null 等）；
- 前端全量：**29 文件 374 用例全部通过**（基线 365 + 新增 9）；
- `npm run build` ✅；
- 真实环境：`GET /api/notification/list?userId=82`（alice+token）→ 200；`POST /api/notification/read/all {userId:82}` → 200；错误 userId=1 → 401（修复前形态，已确认根因链路）。

### 回归
后端未改动（纯前端修复），后端基线不受影响。
"""

path = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\测试执行报告.md"
with io.open(path, "r", encoding="utf-8") as f:
    text = f.read()
with io.open(path, "a", encoding="utf-8") as f:
    f.write(section)
print("appended, new len =", len(text) + len(section))

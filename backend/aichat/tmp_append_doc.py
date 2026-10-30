# -*- coding: utf-8 -*-
import io

path = r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\测试执行报告.md'
append = '''

---

## 23. 补充修复：好友列表昵称全部显示为"好友"（真实环境验证驱动）

### 现象
用户反馈学生端"我的好友"昵称全部变成"好友"。实测 `GET /api/friend/list` 返回的每条记录 `username/nickname/avatar` 均为 **null**，前端只能渲染占位文案。

### 根因
- `friendship` 表存在冗余列 `username / nickname / avatar`（历史遗留，全部为 NULL）；
- `FriendshipMapper.getFriendList / getPendingRequests` 使用 `SELECT f.*, u.username, u.nickname, u.avatar` —— `f.*` 展开后与 JOIN 列产生**同名列**；
- MyBatis 结果映射按列名取**第一个**出现的同名列 → 取到 `f.*` 的 NULL，JOIN 来的真实昵称被丢弃。单测（mock Mapper）不暴露，真实 MySQL 必现。

### 修复
`FriendshipMapper` 两个查询改为**显式列出 f 表列**（`f.id, f.user_id, f.friend_id, f.status, f.create_time, f.accept_time`）+ JOIN 列，消除同名列冲突。

### 验证结果（真实环境）
- 修复前：alice 好友列表 3 条，`username/nickname/avatar` 全 null；
- 修复后：3 条好友正确返回 `demo_bob/鲍勃`、`demo_carol/卡罗尔`、`demo_dave/戴夫`；
- 待处理请求查询（`getPendingRequests`）同步修复，接口正常。

### 回归
- 后端：`mvnw test` 全量通过（721 用例基线，0 失败）；
- 前端：未改动（前端读取 `nickname` 字段，后端恢复后自动显示昵称）。
'''

with io.open(path, 'r', encoding='utf-8') as f:
    t = f.read()
with io.open(path, 'w', encoding='utf-8') as f:
    f.write(t.rstrip() + '\n' + append + '\n')
print('OK')

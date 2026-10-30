# AI Chat 系统 API 文档

## 概述

本文档描述了AI聊天系统的RESTful API接口。

**基础URL**: `http://localhost:8080/api`

---

## 1. 用户认证模块

### 1.1 用户注册

**接口**: `POST /user/register`

**描述**: 注册新用户

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户"
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "password": null
  }
}
```

### 1.2 用户登录

**接口**: `POST /user/login`

**描述**: 用户登录

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "password": null
  }
}
```

---

## 2. AI对话模块

### 2.1 健康检查

**接口**: `GET /chat/health`

**描述**: 检查AI服务状态

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": "AI服务正常运行..."
}
```

### 2.2 获取AI模型列表

**接口**: `GET /model/list`

**描述**: 获取可用的AI模型列表

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "modelName": "Llama 3.2 3B",
      "modelKey": "llama3.2:3b",
      "provider": "Ollama"
    }
  ]
}
```

### 2.3 获取模型详情

**接口**: `GET /model/{modelKey}`

**描述**: 根据模型key获取模型详情

---

## 3. AI角色模块

### 3.1 获取角色列表

**接口**: `GET /character/list`

**描述**: 获取所有AI角色列表

### 3.2 获取用户角色列表

**接口**: `GET /character/list/user/{userId}`

**描述**: 获取指定用户的角色列表

### 3.3 获取角色详情

**接口**: `GET /character/{id}?userId={userId}`

**描述**: 获取角色详情（需验证访问权限）

### 3.4 创建角色

**接口**: `POST /character/create`

**描述**: 创建新角色

**请求体**:
```json
{
  "userId": 1,
  "name": "我的助手",
  "prompt": "你是一个乐于助人的助手...",
  "model": "llama3.2:3b"
}
```

### 3.5 更新角色

**接口**: `PUT /character/{id}`

**描述**: 更新角色信息

**请求体**:
```json
{
  "userId": 1,
  "name": "新名称",
  "prompt": "新提示词"
}
```

### 3.6 删除角色

**接口**: `DELETE /character/{id}?userId={userId}`

**描述**: 删除角色

### 3.7 生成角色

**接口**: `POST /character/generate`

**描述**: 使用AI生成角色

**请求体**:
```json
{
  "theme": "科幻角色"
}
```

---

## 4. 搜索模块

### 4.1 用户搜索

**接口**: `GET /search/users?keyword={keyword}&limit={limit}`

**描述**: 根据关键词搜索用户

### 4.2 帖子搜索

**接口**: `GET /search/posts?keyword={keyword}&limit={limit}`

**描述**: 根据关键词搜索帖子

### 4.3 标签搜索

**接口**: `GET /search/tags?keyword={keyword}&limit={limit}`

**描述**: 搜索标签

### 4.4 按标签获取帖子

**接口**: `GET /search/posts/tag?tagName={tagName}&limit={limit}`

**描述**: 获取指定标签的帖子

### 4.5 网络搜索

**接口**: `POST /search/execute`

**描述**: 执行网络搜索

**请求体**:
```json
{
  "query": "AI技术",
  "maxResults": 10
}
```

---

## 5. 通知模块

### 5.1 获取通知列表

**接口**: `GET /notification/list?userId={userId}&limit={limit}`

**描述**: 获取用户的通知列表

### 5.2 获取未读数量

**接口**: `GET /notification/unread/count?userId={userId}`

**描述**: 获取未读通知数量

### 5.3 标记为已读

**接口**: `POST /notification/read/{notificationId}`

**描述**: 标记单个通知为已读

### 5.4 全部标记为已读

**接口**: `POST /notification/read/all`

**描述**: 全部通知标记为已读

**请求体**:
```json
{
  "userId": 1
}
```

### 5.5 删除通知

**接口**: `DELETE /notification/{notificationId}`

**描述**: 删除通知

---

## 6. 社交模块（好友）

### 6.1 好友搜索

**接口**: `GET /friend/search?username={username}`

**描述**: 根据用户名搜索用户

### 6.2 获取好友列表

**接口**: `GET /friend/list?userId={userId}`

**描述**: 获取用户的好友列表

### 6.3 获取待处理请求

**接口**: `GET /friend/requests?userId={userId}`

**描述**: 获取待处理的好友请求

### 6.4 发送好友请求

**接口**: `POST /friend/request`

**描述**: 发送好友请求

**请求体**:
```json
{
  "userId": 1,
  "friendId": 2
}
```

### 6.5 接受好友请求

**接口**: `POST /friend/accept`

**描述**: 接受好友请求

**请求体**:
```json
{
  "userId": 2,
  "requesterId": 1
}
```

### 6.6 拒绝好友请求

**接口**: `POST /friend/reject`

**描述**: 拒绝好友请求

**请求体**:
```json
{
  "userId": 2,
  "requesterId": 1
}
```

### 6.7 删除好友

**接口**: `POST /friend/remove`

**描述**: 删除好友

**请求体**:
```json
{
  "userId": 1,
  "friendId": 2
}
```

### 6.8 检查好友状态

**接口**: `GET /friend/status?userId={userId}&targetId={targetId}`

**描述**: 检查用户间的好友状态

---

## 7. 社交模块（帖子）

### 7.1 获取帖子列表

**接口**: `GET /social/posts`

**描述**: 获取帖子列表

### 7.2 获取帖子详情

**接口**: `GET /social/posts/{id}`

**描述**: 获取帖子详情

### 7.3 创建帖子

**接口**: `POST /social/posts`

**描述**: 创建帖子

### 7.4 更新帖子

**接口**: `PUT /social/posts/{id}`

**描述**: 更新帖子

### 7.5 删除帖子

**接口**: `DELETE /social/posts/{id}`

**描述**: 删除帖子

### 7.6 点赞帖子

**接口**: `POST /social/posts/{id}/like`

**描述**: 点赞帖子

### 7.7 添加评论

**接口**: `POST /social/posts/{id}/comments`

**描述**: 添加评论

---

## 8. 管理后台模块

### 8.1 获取公开公告

**接口**: `GET /admin/announcements/public/active`

**描述**: 获取公开的活动公告

### 8.2 获取公告详情

**接口**: `GET /admin/announcements/public/{id}`

**描述**: 获取公告详情

### 8.3 创建公告

**接口**: `POST /admin/announcements`

**描述**: 创建公告

**请求体**:
```json
{
  "title": "公告标题",
  "content": "公告内容"
}
```

### 8.4 更新公告

**接口**: `PUT /admin/announcements/{id}`

**描述**: 更新公告

### 8.5 发布公告

**接口**: `POST /admin/announcements/{id}/publish`

**描述**: 发布公告

### 8.6 获取公告列表

**接口**: `GET /admin/announcements/list`

**描述**: 获取所有公告列表

---

## 9. AI系统管理模块

### 9.1 注册AI系统

**接口**: `POST /aisystem/register`

**描述**: 注册新的AI系统

**请求体**:
```json
{
  "systemName": "MyChatbot",
  "systemType": "chatbot",
  "description": "智能客服机器人"
}
```

### 9.2 获取AI系统列表

**接口**: `GET /aisystem/list`

**描述**: 获取所有AI系统列表

### 9.3 AI对话

**接口**: `POST /external/ai/chat`

**描述**: 调用外部AI服务进行对话

**请求体**:
```json
{
  "provider": "ollama",
  "model": "llama3.2:3b",
  "prompt": "你好"
}
```

### 9.4 获取模型列表（按服务商）

**接口**: `GET /external/ai/models/{provider}`

**描述**: 获取指定服务商的模型列表

---

## 10. 错误处理

### 10.1 错误响应格式

所有错误响应遵循统一格式：

```json
{
  "code": 500,
  "msg": "错误描述信息",
  "data": {
    "message": "错误描述信息",
    "detail": "详细错误信息",
    "timestamp": 1234567890000
  }
}
```

### 10.2 常见错误码

| 错误码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 认证失败 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务暂不可用 |
| 504 | 请求超时 |

---

## 11. 使用示例

### 11.1 用户注册
```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","nickname":"测试用户"}'
```

### 11.2 用户登录
```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

### 11.3 获取模型列表
```bash
curl http://localhost:8080/api/model/list
```

### 11.4 创建角色
```bash
curl -X POST http://localhost:8080/api/character/create \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"name":"我的助手","prompt":"你是一个乐于助人的助手"}'
```

### 11.5 搜索用户
```bash
curl "http://localhost:8080/api/search/users?keyword=test"
```

---

## 12. 联系与支持

如有问题或建议，请联系系统管理员。

**文档版本**: v2.0
**最后更新**: 2026-07-16
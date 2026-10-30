# -*- coding: utf-8 -*-
import urllib.request, json

def post(url, body, token=None):
    req = urllib.request.Request('http://127.0.0.1:8080' + url, data=json.dumps(body).encode(),
        headers={'Content-Type': 'application/json'})
    if token: req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]

def get(url, token=None):
    req = urllib.request.Request('http://127.0.0.1:8080' + url)
    if token: req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]

# 1. 登录
s, body = post('/api/user/login', {'username': 'demo_alice', 'password': 'Test@123456'})
if s != 200:
    print('login failed:', s, body); raise SystemExit
token = body['data']['token']
uid = body['data']['user']['id']
print('login ok, uid=', uid)

# 2. 创建角色（无 avatar，avatarStyle=bottts）
c = {
    'userId': uid, 'name': '头像风格测试', 'avatar': '', 'avatarStyle': 'bottts',
    'prompt': '这是一个头像风格持久化测试角色，用于验证 avatarStyle 字段完整存取。',
    'personality': '测试', 'isSystem': False, 'isShared': False
}
s2, b2 = post('/api/character/create', c, token)
print('create:', s2, 'code=', b2.get('code') if isinstance(b2, dict) else b2)
created = b2.get('data', {}) if isinstance(b2, dict) else {}
print('created id=', created.get('id'), 'avatarStyle=', created.get('avatarStyle'), 'avatar=', repr(created.get('avatar')))

# 3. 列表返回 avatarStyle
s3, b3 = get(f'/api/character/list/user/{uid}', token)
items = b3.get('data', []) if isinstance(b3, dict) else []
mine = [x for x in items if x.get('name') == '头像风格测试']
print('list count=', len(items), 'mine=', len(mine))
if mine:
    m = mine[0]
    print('list avatarStyle=', m.get('avatarStyle'), 'avatar=', repr(m.get('avatar')))

# 4. 更新 avatarStyle
if created.get('id'):
    cid = created['id']
    s4, b4 = post(f'/api/character/{cid}', {'userId': uid, 'avatarStyle': 'jdenticon'}, token)
    print('update:', s4, 'code=', b4.get('code') if isinstance(b4, dict) else b4,
          'avatarStyle=', (b4.get('data') or {}).get('avatarStyle') if isinstance(b4, dict) else None)

# 5. 清理测试角色
if created.get('id'):
    import urllib.parse
    req = urllib.request.Request(f'http://127.0.0.1:8080/api/character/{created["id"]}?userId={uid}', method='DELETE')
    req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            print('cleanup delete:', r.status)
    except urllib.error.HTTPError as e:
        print('cleanup delete error:', e.code)

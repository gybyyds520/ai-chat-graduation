# -*- coding: utf-8 -*-
"""AI 记忆完善 - 真实环境端到端验证脚本
验证：登录 -> 记忆列表(搜索/来源过滤) -> 手动创建 -> 聊天自动提炼 -> 自动提炼记忆入库 -> 注入使用(markMemoryUsed)
"""
import urllib.request
import json
import uuid
import sys

BASE = 'http://127.0.0.1:8080'
PASS = 0
FAIL = 0


def log(ok, name, detail=''):
    global PASS, FAIL
    if ok:
        PASS += 1
        print(f'[PASS] {name} {detail}')
    else:
        FAIL += 1
        print(f'[FAIL] {name} {detail}')


def req(method, path, data=None, token=None, timeout=60):
    headers = {}
    if data is not None:
        headers['Content-Type'] = 'application/json'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    body = json.dumps(data).encode('utf-8') if data is not None else None
    r = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    resp = urllib.request.urlopen(r, timeout=timeout)
    raw = resp.read().decode('utf-8')
    try:
        return resp.status, json.loads(raw)
    except Exception:
        return resp.status, raw


def main():
    # 1. 登录
    try:
        s, b = req('POST', '/api/user/login', {'username': 'demo_alice', 'password': 'Test@123456'}, timeout=15)
        token = b['data']['token']
        uid = b['data']['user']['id']
        log(s == 200 and b.get('code') == 200 and token, '登录 demo_alice', f'uid={uid}')
    except Exception as e:
        log(False, '登录 demo_alice', f'ERR {type(e).__name__} {e}')
        return

    # 2. 记忆列表基线
    try:
        s, b = req('GET', '/api/memory/list?page=1&size=50', token=token)
        base_total = b['data']['total']
        records = b['data']['records']
        log(s == 200 and b.get('code') == 200, '记忆列表基线', f'total={base_total}, 首条={records[0]["content"][:20] if records else "无"}')
    except Exception as e:
        log(False, '记忆列表基线', f'ERR {e}')
        return

    # 3. 手动创建记忆（验证 create + 归属）
    try:
        content = '我是端到端验证手动记忆_' + uuid.uuid4().hex[:6]
        s, b = req('POST', '/api/memory', {'content': content, 'memoryType': 'preference', 'importance': 3}, token=token)
        created = b.get('data', {}).get('id') or (b.get('data') if isinstance(b.get('data'), (int, str)) else None)
        log(s == 200 and b.get('code') == 200, '手动创建记忆', f'id={created}')
    except Exception as e:
        log(False, '手动创建记忆', f'ERR {e}')
        created = None

    # 4. 关键词搜索过滤
    try:
        keyword = '验证手动记忆'
        s, b = req('GET', f'/api/memory/list?page=1&size=10&keyword={urllib.parse.quote(keyword)}', token=token)
        hit = any(keyword in r.get('content', '') for r in b['data']['records'])
        log(s == 200 and b.get('code') == 200 and hit, '关键词搜索过滤', f'keyword={keyword}, total={b["data"]["total"]}')
    except Exception as e:
        log(False, '关键词搜索过滤', f'ERR {e}')

    # 5. 角色列表拿 characterId
    try:
        s, b = req('GET', '/api/character/list', token=token)
        chars = b.get('data', [])
        cid = None
        if isinstance(chars, dict):
            chars = chars.get('records') or chars.get('list') or []
        if chars:
            cid = chars[0].get('id') or chars[0].get('characterId')
        log(s == 200 and cid is not None, '获取角色列表', f'characterId={cid}')
    except Exception as e:
        log(False, '获取角色列表', f'ERR {e}')
        cid = None

    # 6. 聊天触发自动提炼（注入 + 提炼）
    sid = 'e2e-' + uuid.uuid4().hex[:12]
    if cid:
        try:
            msg = '我叫小明，我喜欢打篮球，我今年20岁'
            url = f'/api/chat/stream?sessionId={sid}&userId={uid}&characterId={cid}&message={urllib.parse.quote(msg)}'
            s, body = req('GET', url, token=token, timeout=90)
            got_error = (isinstance(body, str) and 'error' in body) or (isinstance(body, dict) and body.get('code') != 200)
            log(not got_error, '聊天流式调用(触发自动提炼)', f'http={s}, 响应片段={str(body)[:120]}')
        except Exception as e:
            log(False, '聊天流式调用(触发自动提炼)', f'ERR {type(e).__name__} {e}')

    # 7. 验证自动提炼入库（source=chat）
    try:
        s, b = req('GET', '/api/memory/list?page=1&size=50&source=chat', token=token)
        chat_recs = [r for r in b['data']['records'] if r.get('source') == 'chat']
        new_auto = [r for r in chat_recs if r.get('category') == '自动提炼' or '小明' in r.get('content', '') or '打篮球' in r.get('content', '')]
        log(s == 200 and b.get('code') == 200 and new_auto, '聊天自动提炼入库', f'source=chat 总数={len(chat_recs)}, 新增自动提炼={len(new_auto)}, 示例={new_auto[0]["content"] if new_auto else "无"}')
    except Exception as e:
        log(False, '聊天自动提炼入库', f'ERR {e}')

    # 8. 验证来源过滤（source=manual）
    try:
        s, b = req('GET', '/api/memory/list?page=1&size=50&source=manual', token=token)
        manual_recs = [r for r in b['data']['records'] if r.get('source') == 'manual']
        log(s == 200 and b.get('code') == 200 and manual_recs, '来源筛选 source=manual', f'total={len(manual_recs)}')
    except Exception as e:
        log(False, '来源筛选 source=manual', f'ERR {e}')

    # 9. 统计接口
    try:
        s, b = req('GET', '/api/memory/stats', token=token)
        log(s == 200 and b.get('code') == 200, '记忆统计接口', str(b.get('data'))[:100])
    except Exception as e:
        log(False, '记忆统计接口', f'ERR {e}')

    # 10. 注入使用验证（记忆 lastUsedAt 非空 = markMemoryUsed 执行过）
    try:
        s, b = req('GET', '/api/memory/list?page=1&size=50', token=token)
        used = [r for r in b['data']['records'] if r.get('lastUsedAt')]
        log(s == 200 and b.get('code') == 200 and used, '注入使用(markMemoryUsed)', f'已被使用记忆数={len(used)}')
    except Exception as e:
        log(False, '注入使用(markMemoryUsed)', f'ERR {e}')

    print(f'\n===== 结果: PASS={PASS} FAIL={FAIL} =====')
    sys.exit(0 if FAIL == 0 else 1)


if __name__ == '__main__':
    import urllib.parse
    main()

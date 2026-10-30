# -*- coding: utf-8 -*-
import urllib.request, json, struct, zlib

def png():
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 2, 0, 0, 0))
    idat = chunk(b'IDAT', zlib.compress(b'\x00\xff\x00\x00'))
    iend = chunk(b'IEND', b'')
    return sig + ihdr + idat + iend

boundary = '----testboundary123'
body = b''
body += ('--' + boundary + '\r\n').encode()
body += b'Content-Disposition: form-data; name="file"; filename="test-avatar.png"\r\n'
body += b'Content-Type: image/png\r\n\r\n'
body += png()
body += ('\r\n--' + boundary + '--\r\n').encode()

req = urllib.request.Request('http://127.0.0.1:8080/api/character/upload-avatar', data=body,
    headers={'Content-Type': 'multipart/form-data; boundary=' + boundary})

# 先登录拿 token
login_req = urllib.request.Request('http://127.0.0.1:8080/api/user/login',
    data=json.dumps({'username': 'demo_alice', 'password': 'Test@123456'}).encode(),
    headers={'Content-Type': 'application/json'})
with urllib.request.urlopen(login_req, timeout=10) as r:
    login = json.loads(r.read().decode())
token = login['data']['token']
req.add_header('Authorization', 'Bearer ' + token)

try:
    with urllib.request.urlopen(req, timeout=10) as r:
        res = json.loads(r.read().decode())
    print('upload status:', r.status, 'code=', res.get('code'))
    fp = res.get('data', {}).get('filePath')
    print('filePath=', fp)
    if fp:
        with urllib.request.urlopen('http://127.0.0.1:8080' + fp, timeout=10) as r2:
            print('GET', fp, '->', r2.status, 'content-type=', r2.headers.get('Content-Type'))
except urllib.error.HTTPError as e:
    print('upload HTTP error:', e.code, e.read().decode()[:300])

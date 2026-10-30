# -*- coding: utf-8 -*-
import subprocess, json, time, random, string, io

BASE = "http://localhost:8080"
OUT = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\api_results.txt"
suf = "".join(random.choices(string.ascii_lowercase + string.digits, k=6))
reg_user = "qa013_" + suf

def curl(method, path, body=None, maxtime=7, token=None):
    args = ["curl.exe", "-s", "-o", "-", "--max-time", str(maxtime), "--connect-timeout", "4",
            "-X", method, BASE + path, "-H", "Content-Type: application/json",
            "-w", "\n@@HTTP:%{http_code} T:%{time_total}"]
    if token:
        args += ["-H", "Authorization: Bearer " + token]
    if body is not None:
        args += ["-d", json.dumps(body)]
    t0 = time.time()
    try:
        r = subprocess.run(args, capture_output=True, text=True, timeout=maxtime + 4, encoding="utf-8", errors="replace")
        out = r.stdout.strip()
    except Exception as e:
        out = "@@TIMEOUT/EXC: %s" % e
    return out

lines = []
def rec(name, raw):
    lines.append("==== %s ====" % name)
    lines.append(raw[:900])
    lines.append("")

# TC-001 health
rec("TC-001 health", curl("GET", "/api/health", maxtime=6))
# TC-012 bad email
rec("TC-012 bad email", curl("POST", "/api/user/register",
    {"username": reg_user, "password": "Test@123456", "email": "notanemail"}, 7))
# TC-004 illegal username (chinese)
rec("TC-004 chinese username", curl("POST", "/api/user/register",
    {"username": "测试用户013", "password": "Test@123456", "email": reg_user + "@e.com"}, 7))
# TC-005 short password
rec("TC-005 short password", curl("POST", "/api/user/register",
    {"username": reg_user, "password": "123", "email": reg_user + "@e.com"}, 7))
# TC-006 duplicate username admin
rec("TC-006 dup admin", curl("POST", "/api/user/register",
    {"username": "admin", "password": "Test@123456", "email": reg_user + "@e.com"}, 7))
# TC-002 valid register
rec("TC-002 valid register user=" + reg_user, curl("POST", "/api/user/register",
    {"username": reg_user, "password": "Test@123456", "nickname": "QA013", "email": reg_user + "@example.com", "age": 25}, 8))

# store created username
with io.open(r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\created_user.txt", "w", encoding="utf-8") as f:
    f.write(reg_user)

with io.open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("done; reg_user=" + reg_user)

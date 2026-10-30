# -*- coding: utf-8 -*-
import subprocess, json, time, io

BASE = "http://localhost:8080"
OUT = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\api_results2.txt"
reg_user = io.open(r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\created_user.txt", encoding="utf-8").read().strip()

def curl(method, path, body=None, maxtime=8, token=None):
    args = ["curl.exe", "-s", "-o", "-", "--max-time", str(maxtime), "--connect-timeout", "4",
            "-X", method, BASE + path, "-H", "Content-Type: application/json",
            "-w", "\n@@HTTP:%{http_code} T:%{time_total}"]
    if token:
        args += ["-H", "Authorization: Bearer " + token]
    if body is not None:
        args += ["-d", json.dumps(body)]
    try:
        r = subprocess.run(args, capture_output=True, text=True, timeout=maxtime + 4, encoding="utf-8", errors="replace")
        return r.stdout.strip()
    except Exception as e:
        return "@@TIMEOUT/EXC: %s" % e

lines = []
def rec(name, raw):
    lines.append("==== %s ====" % name); lines.append(raw[:1200]); lines.append("")

# TC-003 login admin
rec("TC-003 login admin", curl("POST", "/api/user/login", {"username":"admin","password":"123456"}, 10))
# TC-007 wrong password
rec("TC-007 wrong password admin", curl("POST", "/api/user/login", {"username":"admin","password":"wrongpass"}, 8))
# TC-008 nonexistent user
rec("TC-008 nonexistent", curl("POST", "/api/user/login", {"username":"nosuchuser013zz","password":"x123456"}, 8))
# login the freshly registered user
rec("TC-003b login reg_user " + reg_user, curl("POST", "/api/user/login", {"username":reg_user,"password":"Test@123456"}, 10))
# TC-009 no token protected
rec("TC-009 no token membership", curl("GET", "/api/user/membership", None, 6))

with io.open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("batch2 done")

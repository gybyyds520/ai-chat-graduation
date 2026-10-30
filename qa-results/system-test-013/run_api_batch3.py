# -*- coding: utf-8 -*-
import subprocess, json, io

BASE = "http://localhost:8080"
OUT = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\api_results3.txt"
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
        r = subprocess.run(args, capture_output=True, text=True, timeout=maxtime+4, encoding="utf-8", errors="replace")
        return r.stdout.strip()
    except Exception as e:
        return "@@TIMEOUT/EXC: %s" % e

lines=[]
def rec(n,r): lines.append("==== %s ===="%n); lines.append(r[:1200]); lines.append("")

# demo_alice login
r = curl("POST","/api/user/login",{"username":"demo_alice","password":"Test@123456"},10)
rec("TC-003c demo_alice login", r)
# extract alice token
alice_token=None
try:
    body=r.split("@@HTTP")[0]
    j=json.loads(body)
    alice_token=(j.get("data") or {}).get("token")
except Exception as e:
    rec("parse alice token", str(e))

# fresh user token from batch2
r2 = curl("POST","/api/user/login",{"username":reg_user,"password":"Test@123456"},10)
try:
    j2=json.loads(r2.split("@@HTTP")[0]); fresh_token=(j2.get("data") or {}).get("token")
except Exception as e:
    fresh_token=None; rec("parse fresh token", str(e))

# TC-009b valid token -> membership should be 200
rec("TC-membership with fresh token", curl("GET","/api/user/membership",None,8,fresh_token))
# TC-010 fresh (normal user) hits admin endpoint /api/auth/badge/award -> expect 403
rec("TC-010 normal-user badge award", curl("POST","/api/auth/badge/award",{"userId":1,"badgeId":"x","badgeName":"x"},8,fresh_token))
# TC-010b normal user hits admin list (guess /api/admin/users) -> expect 401/403
rec("TC-010b normal-user admin users", curl("GET","/api/admin/users",None,8,fresh_token))
# TC-009c alice hits self profile endpoint /api/user/profile?userId=1 (admin) -> object-level authz probe
rec("TC-objauthz alice GET /api/user/1", curl("GET","/api/user/1",None,8,alice_token))

with io.open(OUT,"w",encoding="utf-8") as f: f.write("\n".join(lines))
print("batch3 done alice_token=%s fresh=%s"%(bool(alice_token),bool(fresh_token)))

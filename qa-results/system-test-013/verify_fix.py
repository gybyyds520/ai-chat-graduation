# -*- coding: utf-8 -*-
import subprocess, json, io
BASE="http://localhost:8080"
def curl(method,path,body=None,maxtime=10,token=None):
    a=["curl.exe","-s","-o","-","--max-time",str(maxtime),"--connect-timeout","4","-X",method,BASE+path,"-H","Content-Type: application/json","-w","\n@@HTTP:%{http_code} t:%{time_total}"]
    if token: a+=["-H","Authorization: Bearer "+token]
    if body is not None: a+=["-d",json.dumps(body)]
    try:
        r=subprocess.run(a,capture_output=True,text=True,timeout=maxtime+4,encoding="utf-8",errors="replace")
        return r.stdout.strip()
    except Exception as e: return "@@EXC %s"%e
out=[]
def rec(n,r): out.append("==== %s ===="%n); out.append(r[:600]); out.append("")
rec("health", curl("GET","/api/health",None,6))
rec("admin user-login", curl("POST","/api/user/login",{"username":"admin","password":"123456"},10))
rec("admin admin-login", curl("POST","/api/admin/login",{"username":"admin","password":"123456"},10))
rec("admin wrong", curl("POST","/api/user/login",{"username":"admin","password":"wrongpass"},10))
rec("demo_alice", curl("POST","/api/user/login",{"username":"demo_alice","password":"Test@123456"},10))
io.open(r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\verify_fix.txt","w",encoding="utf-8").write("\n".join(out))
print("verify done")

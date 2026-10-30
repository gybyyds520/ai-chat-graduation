# -*- coding: utf-8 -*-
import subprocess, os
sql = """SELECT id, username, nickname, role, age, minor_mode_enabled FROM user ORDER BY id LIMIT 100;"""
open('tmp_ua.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_ua.sql'],
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
os.remove('tmp_ua.sql')
print(r.stdout if r.stdout else ('STDERR: ' + r.stderr[:500]))

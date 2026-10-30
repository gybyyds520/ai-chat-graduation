# -*- coding: utf-8 -*-
import subprocess
sql = """SHOW CREATE TABLE group_chat;
SHOW CREATE TABLE user_report;
SHOW CREATE TABLE user_block;
SHOW CREATE TABLE privacy_setting;
SHOW CREATE TABLE ai_elf_schedule;"""
open('tmp_show.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_show.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat',
                   text=True, encoding='utf-8', errors='replace')
import os; os.remove('tmp_show.sql')
print(r.stdout[:4000])

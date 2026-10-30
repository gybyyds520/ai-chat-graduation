# -*- coding: utf-8 -*-
import subprocess, os
sql = """SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='ai_chat' AND (
 (TABLE_NAME='ai_elf_alarm' AND COLUMN_NAME='time')
 OR (TABLE_NAME='group_chat' AND COLUMN_NAME='owner_id')
 OR (TABLE_NAME='privacy_setting' AND COLUMN_NAME='`key`')
 OR (TABLE_NAME='pay_account' AND COLUMN_NAME='account_number')
 OR (TABLE_NAME='user_statistics' AND COLUMN_NAME='post_count')
);"""
open('tmp_chk.sql', 'w', encoding='utf-8').write(sql)
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_chk.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat',
                   text=True, encoding='utf-8', errors='replace')
print("STDOUT:", r.stdout)
print("STDERR:", r.stderr[:500])
os.remove('tmp_chk.sql')

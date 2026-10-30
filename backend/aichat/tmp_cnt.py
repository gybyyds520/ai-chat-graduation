# -*- coding: utf-8 -*-
import subprocess
tables = ['user', 'friendship', 'friend_group', 'friend_group_member', 'friend_chat_message',
          'group_chat', 'group_member', 'group_message', 'community', 'community_member', 'community_post',
          'social_post', 'social_comment', 'social_like', 'social_share', 'ai_character', 'character_share',
          'ai_elf', 'ai_elf_interaction_log', 'ai_elf_sleep', 'ai_elf_alarm', 'ai_elf_schedule', 'ai_elf_health_reminder',
          'ai_memory', 'knowledge_document', 'knowledge_chunk', 'knowledge_query',
          'tool_call_log', 'user_report', 'user_block', 'privacy_setting', 'user_quota', 'pay_account',
          'content_audit_log', 'notification', 'user_statistics']
sql = "SELECT '%s' t, COUNT(*) c FROM %s UNION ALL " * 0
sel = " UNION ALL ".join("SELECT '%s' t, COUNT(*) c FROM %s" % (t, t) for t in tables)
open('tmp_cnt.sql', 'w', encoding='utf-8').write(sel + ";")
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat < tmp_cnt.sql'],
                   capture_output=True, cwd=r'C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\backend\aichat',
                   text=True, encoding='utf-8', errors='replace')
import os; os.remove('tmp_cnt.sql')
print(r.stdout)

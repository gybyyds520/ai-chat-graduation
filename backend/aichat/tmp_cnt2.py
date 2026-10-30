# -*- coding: utf-8 -*-
import subprocess, os
tables = ['user','friendship','friend_group_member','friend_chat_message','group_chat','group_member',
          'group_message','community_post','social_post','social_comment','social_like','social_share',
          'ai_character','character_share','ai_elf','ai_elf_interaction_log','ai_elf_sleep','ai_elf_alarm',
          'ai_elf_schedule','ai_memory','knowledge_document','knowledge_chunk','knowledge_query',
          'tool_call_log','user_report','user_block','privacy_setting','user_quota','pay_account',
          'content_audit_log','notification']
sel = ' UNION ALL '.join("SELECT '%s' t, COUNT(*) c FROM %s" % (t, t) for t in tables)
open('tmp_c.sql', 'w', encoding='utf-8').write(sel + ';')
r = subprocess.run(['cmd', '/c', 'mysql -uroot -p123456 -h127.0.0.1 ai_chat -e "source tmp_c.sql"'],
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
os.remove('tmp_c.sql')
print(r.stdout if r.stdout else ('STDERR: ' + r.stderr[:1200]))

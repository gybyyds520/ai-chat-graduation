INSERT IGNORE INTO ai_character (name, avatar, prompt, model, is_system) VALUES
('AI学习助手', '', '你是一个专业的学习助手，擅长解答各种学科问题，帮助用户学习新知识。', 'phi3:mini', TRUE),
('AI编程助手', '', '你是一个专业的编程助手，擅长多种编程语言，可以帮助用户解决代码问题。', 'llama3.2:3b', TRUE);

INSERT IGNORE INTO social_post (user_id, user_name, content, visibility, like_count, comment_count) VALUES
(1, 'test', '今天学习了Spring Boot框架，感觉非常强大！推荐大家也来学习一下。', 'public', 15, 5),
(1, 'test', 'AI技术发展真快，最近体验了几个新模型，效果都很不错。期待未来更多创新！', 'public', 23, 8),
(4, 'testuser', '周末去公园散步，天气真好！', 'public', 8, 3),
(4, 'testuser', '分享一个有趣的编程技巧：使用Lambda表达式简化代码，让代码更加简洁优雅。', 'public', 12, 6);

INSERT IGNORE INTO social_comment (post_id, user_id, user_name, content, like_count) VALUES
(1, 4, 'testuser', '同意！Spring Boot确实很好用。', 3),
(1, 3, 'testmock', '正在学习中，一起加油！', 2),
(2, 1, 'test', '确实，AI发展太快了！', 5);

INSERT IGNORE INTO notification (user_id, type, target_type, target_id, actor_id, actor_name, message, is_read) VALUES
(4, 'like', 'post', 1, 1, 'test', 'test 点赞了你的帖子', FALSE),
(4, 'comment', 'post', 1, 3, 'testmock', 'testmock 评论了你的帖子', FALSE),
(1, 'like', 'post', 2, 4, 'testuser', 'testuser 点赞了你的帖子', TRUE);

INSERT IGNORE INTO friendship (user_id, friend_id, status) VALUES
(1, 4, 'ACCEPTED'),
(4, 1, 'ACCEPTED'),
(1, 3, 'ACCEPTED'),
(3, 1, 'ACCEPTED');

INSERT IGNORE INTO friend_chat_message (sender_id, receiver_id, content, type) VALUES
(1, 4, '你好！最近在忙什么？', 'text'),
(4, 1, '你好！最近在学习AI相关的知识。', 'text'),
(1, 4, '太棒了！AI确实很有趣。', 'text');

INSERT IGNORE INTO announcement (title, content, status, priority, publish_time, start_time, end_time, display_scope, author) VALUES
('欢迎使用AI聊天系统', '欢迎来到AI聊天系统！这是一个功能强大的智能对话平台，支持多种AI模型和个性化角色。', 'PUBLISHED', 1, NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ALL', 'admin'),
('系统维护通知', '系统将于本周日凌晨2:00-4:00进行例行维护，期间可能会影响部分功能，请提前做好准备。', 'PUBLISHED', 2, NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'ALL', 'admin');
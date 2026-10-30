# 前端视图 → API 调用映射

## AdminProfileView（3 处）
- /api/admin/change-password
- /api/admin/profile
- /api/admin/upload-avatar

## AdminView（49 处）
- /api/admin/ai-chat-messages
- /api/admin/ai-chat-messages/${id}
- /api/admin/ai-chat-messages/session
- /api/admin/announcements
- /api/admin/announcements/${announcementForm.value.id}
- /api/admin/announcements/${id}
- /api/admin/announcements/${id}/publish
- /api/admin/announcements/${id}/unpublish
- /api/admin/backup/full
- /api/admin/backup/incremental
- /api/admin/backup/list
- /api/admin/backup/restore/${filename}
- /api/admin/backup/stats
- /api/admin/change-password
- /api/admin/characters
- /api/admin/data-pool/analysis
- /api/admin/data-pool/auto-save/status
- /api/admin/data-pool/auto-save/toggle
- /api/admin/data-pool/config
- /api/admin/data-pool/duplicates/check
- /api/admin/data-pool/duplicates/remove
- /api/admin/data-pool/export
- /api/admin/data-pool/import
- /api/admin/data-pool/records
- /api/admin/data-pool/records/${editDataForm.value.id}
- /api/admin/data-pool/records/${row.id}
- /api/admin/data-pool/records/batch
- /api/admin/data-pool/stats
- /api/admin/friend-chat-messages
- /api/admin/friend-chat-messages/${id}
- /api/admin/sessions
- /api/admin/stats
- /api/admin/update-profile
- /api/admin/users
- /api/admin/users/${userForm.value.id}
- /api/admin/users/${userId}
- /api/admin/users/count
- /api/admin/users/version/${versionTab.value}
- /api/aiSystem
- /api/aisystem/${row.id}
- /api/aisystem/${row.id}/regenerate-key
- /api/aisystem/list
- /api/aisystem/register
- /api/character/${characterForm.value.id}
- /api/character/${characterId}
- /api/character/create
- /api/payment/${row.id}/review
- /api/payment/create
- /api/payment/pending

## AiElfView（18 处）
- /api/elf/${elf.value.id}/daily-reward
- /api/elf/${elf.value.id}/experience
- /api/elf/${elf.value.id}/interact
- /api/elf/${elf.value.id}/interactions
- /api/elf/${elf.value.id}/relationship
- /api/elf/alarm
- /api/elf/alarm/${alarm.id}
- /api/elf/alarm/${id}
- /api/elf/alarms
- /api/elf/create
- /api/elf/get
- /api/elf/schedule
- /api/elf/schedule/${id}
- /api/elf/schedule/${schedule.id}
- /api/elf/schedules
- /api/elf/sleep
- /api/elf/sleep/records
- /api/elf/sleep/summary

## AnnouncementView（1 处）
- /api/admin/announcements/public/active

## CharacterImportView（4 处）
- /api/character/import/analyze
- /api/character/import/batch-generate
- /api/character/import/save
- /api/character/import/upload

## ChatView（5 处）
- /api/aisystem/ollama/restart
- /api/aisystem/ollama/status
- /api/character/list
- /api/character/list/user/${currentUserId}
- /api/chat/stream

## CommunityView（7 处）
- /api/community/create
- /api/community/join
- /api/community/list
- /api/community/post
- /api/community/post/${post.id}/like
- /api/community/posts/${communityId}
- /api/community/search

## CreateCharacterView（2 处）
- /api/character/create
- /api/character/upload-avatar

## DataPoolView（10 处）
- /api/aisystem/${row.id}
- /api/aisystem/${row.id}/regenerate-key
- /api/aisystem/list
- /api/aisystem/permission/${row.id}
- /api/aisystem/permission/grant
- /api/aisystem/register
- /api/datapool/${row.id}
- /api/datapool/manual
- /api/datapool/query
- /api/userData

## DesktopElf（0 处）

## FeedView（5 处）
- /api/feed/history
- /api/feed/stats
- /api/feed/submit
- /api/feed/task/${taskId}
- /api/feed/upload

## ForgotPasswordView（3 处）
- /api/user/reset-password-by-code
- /api/user/send-verification-code
- /api/user/verify-code

## FriendChatView（9 处）
- /api/character/list
- /api/character/list/user/${userId}
- /api/friend/chat/history
- /api/friend/chat/import
- /api/friend/chat/send
- /api/friend/chat/send/file
- /api/friend/chat/share
- /api/friend/list
- /api/user/status

## FriendView（15 处）
- /api/friend/accept
- /api/friend/group/${group.id}
- /api/friend/group/${groupId}
- /api/friend/group/create
- /api/friend/group/list
- /api/friend/group/member/add
- /api/friend/group/member/remove
- /api/friend/group/members/${groupId}
- /api/friend/list
- /api/friend/reject
- /api/friend/remove
- /api/friend/request
- /api/friend/requests
- /api/friend/search
- /api/friend/status

## LandingPage（0 处）

## LoginView（2 处）
- /api/admin/login
- /api/user/login

## MembershipView（0 处）

## MemoryView（1 处）
- /api/memory

## NotificationView（1 处）
- /api/notification

## PlazaView（5 处）
- /api/plaza/${c.id}/use
- /api/plaza/categories
- /api/plaza/hot
- /api/plaza/list
- /api/plaza/search

## PrivacyView（2 处）
- /api/privacy/setting
- /api/privacy/settings

## ProfileView（9 处）
- /api/pay-account/${accountId}
- /api/pay-account/bind
- /api/pay-account/user/${userProfile.id}
- /api/pay-account/verify
- /api/user/change-password
- /api/user/profile
- /api/user/stats
- /api/user/upload-avatar
- /api/user/voice-preference

## RegisterView（1 处）
- /api/user/register

## ResearchView（4 处）
- /api/research/${researchId}/result
- /api/research/${researchId}/status
- /api/research/list
- /api/research/start

## SocialFeedView（13 处）
- /api/recommendation/posts
- /api/recommendation/track/view
- /api/search/posts
- /api/social/comment
- /api/social/comments/${postId}
- /api/social/like
- /api/social/post
- /api/social/post/${postId}
- /api/social/posts/friends
- /api/social/posts/public
- /api/social/share
- /api/social/upload/image
- /api/social/upload/video

## UpgradeView（0 处）

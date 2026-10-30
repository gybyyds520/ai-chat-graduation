# 后端接口总清单
控制器数：47，接口总数：381

## AdminController（18 个）
- [Post] /api/admin/login
- [Post] /api/admin/change-password
- [Post] /api/admin/update-profile
- [Get] /api/admin/users
- [Delete] /api/admin/users/{id}
- [Get] /api/admin/stats
- [Get] /api/admin/characters
- [Post] /api/admin/users
- [Put] /api/admin/users/{id}
- [Get] /api/admin/users/version/{versionType}
- [Put] /api/admin/users/{id}/version
- [Get] /api/admin/users/count
- [Get] /api/admin/ai-chat-messages
- [Get] /api/admin/ai-chat-messages/session
- [Get] /api/admin/sessions
- [Get] /api/admin/friend-chat-messages
- [Delete] /api/admin/ai-chat-messages/{id}
- [Delete] /api/admin/friend-chat-messages/{id}

## AdvertisementController（8 个）
- [Put] /api/advertisement/{id}
- [Delete] /api/advertisement/{id}
- [Get] /api/advertisement/{id}
- [Get] /api/advertisement/position/{position}
- [Get] /api/advertisement/active/{position}
- [Post] /api/advertisement/{id}/enable
- [Post] /api/advertisement/{id}/disable
- [Post] /api/advertisement/{id}/click

## AiCharacterController（14 个）
- [Get] /api/character/list
- [Get] /api/character/list/user/{userId}
- [Get] /api/character/{id}
- [Post] /api/character/create
- [Put] /api/character/{id}
- [Delete] /api/character/{id}
- [Put] /api/character/{id}/share
- [Post] /api/character/{id}/share-with-user
- [Delete] /api/character/{id}/share-with-user/{toUserId}
- [Delete] /api/character/{id}/revoke-all-shares
- [Get] /api/character/{id}/shared-users
- [Get] /api/character/shared-with-me
- [Get] /api/character/check-access
- [Post] /api/character/upload-avatar

## AiElfController（26 个）
- [Get] /api/elf/get
- [Post] /api/elf/create
- [Put] /api/elf/{elfId}
- [Post] /api/elf/{elfId}/experience
- [Post] /api/elf/alarm
- [Get] /api/elf/alarms
- [Put] /api/elf/alarm/{alarmId}
- [Delete] /api/elf/alarm/{alarmId}
- [Post] /api/elf/schedule
- [Get] /api/elf/schedules
- [Put] /api/elf/schedule/{scheduleId}
- [Delete] /api/elf/schedule/{scheduleId}
- [Post] /api/elf/sleep
- [Get] /api/elf/sleep/records
- [Get] /api/elf/sleep/summary
- [Post] /api/elf/smart-alarm
- [Post] /api/elf/smart-alarm/adjust
- [Post] /api/elf/voice/command
- [Get] /api/elf/voice/commands
- [Post] /api/elf/health/report
- [Get] /api/elf/health/reports
- [Get] /api/elf/health/summary
- [Post] /api/elf/{elfId}/interact
- [Get] /api/elf/{elfId}/relationship
- [Get] /api/elf/{elfId}/interactions
- [Post] /api/elf/{elfId}/daily-reward

## AiMemoryController（6 个）
- [Put] /api/memory/{memoryId}
- [Delete] /api/memory/{memoryId}
- [Get] /api/memory/list
- [Get] /api/memory/stats
- [Get] /api/memory/{memoryId}
- [Post] /api/memory/{memoryId}/toggle

## AiSystemController（20 个）
- [Post] /api/aisystem/register
- [Post] /api/aisystem/authenticate
- [Get] /api/aisystem/list
- [Get] /api/aisystem/{id}
- [Put] /api/aisystem/{id}/status
- [Put] /api/aisystem/{id}/ratelimit
- [Post] /api/aisystem/{id}/regenerate-key
- [Delete] /api/aisystem/{id}
- [Post] /api/aisystem/permission/grant
- [Post] /api/aisystem/permission/grant-category
- [Get] /api/aisystem/{id}/permissions
- [Delete] /api/aisystem/permission/{permissionId}
- [Get] /api/aisystem/ollama/status
- [Get] /api/aisystem/ollama/models
- [Post] /api/aisystem/ollama/pull
- [Delete] /api/aisystem/ollama/models/{name}
- [Get] /api/aisystem/config
- [Put] /api/aisystem/config
- [Get] /api/aisystem/stats
- [Post] /api/aisystem/ollama/restart

## AnnouncementController（9 个）
- [Get] /api/admin/announcements/public/active
- [Get] /api/admin/announcements/public/{id}
- [Put] /api/admin/announcements/{id}
- [Delete] /api/admin/announcements/{id}
- [Get] /api/admin/announcements/{id}
- [Get] /api/admin/announcements/list
- [Get] /api/admin/announcements/active
- [Post] /api/admin/announcements/{id}/publish
- [Post] /api/admin/announcements/{id}/unpublish

## AuthController（15 个）
- [Post] /api/auth/sms/send
- [Post] /api/auth/sms/login
- [Post] /api/auth/mfa/enable
- [Post] /api/auth/mfa/verify
- [Get] /api/auth/devices
- [Post] /api/auth/device/trust
- [Post] /api/auth/device/logout
- [Get] /api/auth/alerts
- [Post] /api/auth/alerts/read
- [Get] /api/auth/badges
- [Post] /api/auth/badge/award
- [Get] /api/auth/tags
- [Post] /api/auth/tag/add
- [Post] /api/auth/tag/remove
- [Post] /api/auth/export

## BlockReportController（8 个）
- [Post] /api/block-report/block
- [Post] /api/block-report/unblock
- [Get] /api/block-report/blocked
- [Get] /api/block-report/is-blocked
- [Post] /api/block-report/report
- [Get] /api/block-report/reports/pending
- [Get] /api/block-report/reports/user
- [Post] /api/block-report/reports/review

## CharacterGeneratorController（2 个）
- [Post] /api/character/generate
- [Post] /api/character/generate/batch

## CharacterImportController（6 个）
- [Post] /api/character/import/upload
- [Post] /api/character/import/analyze
- [Post] /api/character/import/generate
- [Post] /api/character/import/preview
- [Post] /api/character/import/save
- [Post] /api/character/import/batch-generate

## CharacterPlazaController（6 个）
- [Get] /api/plaza/list
- [Get] /api/plaza/search
- [Get] /api/plaza/hot
- [Get] /api/plaza/categories
- [Get] /api/plaza/{characterId}
- [Post] /api/plaza/{characterId}/use

## ChatController（6 个）
- [Get] /api/chat/health
- [Get] /api/chat/models
- [Get] /api/chat/history
- [Get] /api/chat/context/compress
- [Get] /api/chat/multimodal/images/{sessionId}
- [Get] /api/chat/multimodal/files/{sessionId}

## CommunityController（12 个）
- [Post] /api/community/create
- [Get] /api/community/{communityId}
- [Get] /api/community/list
- [Get] /api/community/search
- [Post] /api/community/join
- [Post] /api/community/leave
- [Get] /api/community/members/{communityId}
- [Post] /api/community/post
- [Get] /api/community/posts/{communityId}
- [Post] /api/community/post/{postId}/like
- [Delete] /api/community/post/{postId}
- [Delete] /api/community/{communityId}

## ContentAuditController（2 个）
- [Get] /api/audit/logs
- [Get] /api/audit/stats

## DataBackupController（5 个）
- [Post] /api/admin/backup/full
- [Post] /api/admin/backup/incremental
- [Get] /api/admin/backup/list
- [Post] /api/admin/backup/restore/{filename}
- [Get] /api/admin/backup/stats

## DataPoolController（18 个）
- [Post] /api/admin/data-pool/upload
- [Get] /api/admin/data-pool/records
- [Post] /api/admin/data-pool/records
- [Get] /api/admin/data-pool/records/{id}
- [Put] /api/admin/data-pool/records/{id}
- [Get] /api/admin/data-pool/config
- [Post] /api/admin/data-pool/config
- [Post] /api/admin/data-pool/import
- [Get] /api/admin/data-pool/export
- [Get] /api/admin/data-pool/stats
- [Get] /api/admin/data-pool/types
- [Delete] /api/admin/data-pool/records/{id}
- [Delete] /api/admin/data-pool/records/batch
- [Get] /api/admin/data-pool/analysis
- [Get] /api/admin/data-pool/auto-save/status
- [Post] /api/admin/data-pool/auto-save/toggle
- [Get] /api/admin/data-pool/duplicates/check
- [Post] /api/admin/data-pool/duplicates/remove

## DeepResearchController（4 个）
- [Post] /api/research/start
- [Get] /api/research/{researchId}/status
- [Get] /api/research/{researchId}/result
- [Get] /api/research/list

## ExternalApiController（11 个）
- [Get] /api/external/health
- [Get] /api/external/providers
- [Post] /api/external/proxy
- [Post] /api/external/ai/chat
- [Post] /api/external/ai/completions
- [Get] /api/external/ai/models/{provider}
- [Post] /api/external/ai/embeddings
- [Get] /api/external/fetch
- [Get] /api/external/download
- [Post] /api/external/search
- [Get] /api/external/status

## FeedController（6 个）
- [Post] /api/feed/upload
- [Post] /api/feed/submit
- [Get] /api/feed/history
- [Get] /api/feed/stats
- [Get] /api/feed/task/{taskId}
- [Delete] /api/feed/task/{taskId}

## FriendChatController（8 个）
- [Post] /api/friend/chat/send
- [Get] /api/friend/chat/history
- [Get] /api/friend/chat/unread
- [Post] /api/friend/chat/read
- [Get] /api/friend/chat/list
- [Post] /api/friend/chat/send/file
- [Post] /api/friend/chat/share
- [Post] /api/friend/chat/import

## FriendGroupController（9 个）
- [Post] /api/friend/group/create
- [Get] /api/friend/group/{groupId}
- [Get] /api/friend/group/list
- [Put] /api/friend/group/{groupId}
- [Delete] /api/friend/group/{groupId}
- [Get] /api/friend/group/members/{groupId}
- [Post] /api/friend/group/member/add
- [Post] /api/friend/group/member/remove
- [Post] /api/friend/group/member/remark

## FriendshipController（8 个）
- [Post] /api/friend/request
- [Post] /api/friend/accept
- [Post] /api/friend/reject
- [Post] /api/friend/remove
- [Get] /api/friend/list
- [Get] /api/friend/requests
- [Get] /api/friend/search
- [Get] /api/friend/status

## GroupChatController（26 个）
- [Post] /api/group/create
- [Get] /api/group/{groupId}
- [Get] /api/group/my-groups
- [Get] /api/group/public
- [Post] /api/group/join
- [Post] /api/group/leave
- [Get] /api/group/members/{groupId}
- [Post] /api/group/message
- [Get] /api/group/messages/{groupId}
- [Delete] /api/group/{groupId}
- [Get] /api/group/recommend-friends
- [Put] /api/group/{groupId}/announcement
- [Put] /api/group/{groupId}/settings
- [Get] /api/group/{groupId}/details
- [Get] /api/group/activity-ranking
- [Post] /api/group/{groupId}/activity-score
- [Post] /api/group/bot
- [Get] /api/group/bots/{groupId}
- [Put] /api/group/bot/{botId}
- [Delete] /api/group/bot/{botId}
- [Post] /api/group/bot/response
- [Get] /api/group/recommend-friends/advanced
- [Post] /api/group/{groupId}/ai-members
- [Delete] /api/group/{groupId}/ai-members/{characterId}
- [Get] /api/group/{groupId}/ai-members
- [Post] /api/group/{groupId}/ai-round

## HealthController（0 个）

## ImageGenerationController（3 个）
- [Post] /api/image/generate
- [Post] /api/image/detect
- [Get] /api/image/health

## KnowledgeBaseController（7 个）
- [Post] /api/knowledge/upload
- [Get] /api/knowledge/list
- [Get] /api/knowledge/history
- [Get] /api/knowledge/{documentId}
- [Get] /api/knowledge/{documentId}/chunks
- [Delete] /api/knowledge/{documentId}
- [Post] /api/knowledge/{documentId}/ask

## ModelController（7 个）
- [Get] /api/model/list
- [Get] /api/model/category/{category}
- [Get] /api/model/{modelKey}
- [Post] /api/model/add
- [Put] /api/model/{id}
- [Delete] /api/model/{id}
- [Post] /api/model/{id}/toggle

## MultimodalController（5 个）
- [Post] /api/multimodal/image/upload
- [Post] /api/multimodal/image/process
- [Get] /api/multimodal/images/{sessionId}
- [Post] /api/multimodal/file/upload
- [Get] /api/multimodal/files/{sessionId}

## NotificationController（8 个）
- [Get] /api/notification/list
- [Get] /api/notification/unread/count
- [Post] /api/notification/read/{notificationId}
- [Post] /api/notification/read/all
- [Delete] /api/notification/{notificationId}
- [Post] /api/notification/clear-all
- [Get] /api/notification/settings
- [Put] /api/notification/settings

## OAuthController（5 个）
- [Post] /api/oauth/wechat/login
- [Get] /api/oauth/wechat/callback
- [Post] /api/oauth/wechat/bind
- [Post] /api/oauth/wechat/unbind
- [Get] /api/oauth/wechat/status

## PayAccountController（5 个）
- [Post] /api/pay-account/bind
- [Post] /api/pay-account/verify
- [Delete] /api/pay-account/{id}
- [Get] /api/pay-account/user/{userId}
- [Get] /api/pay-account/user/{userId}/has-any

## PaymentController（6 个）
- [Post] /api/payment/create
- [Post] /api/payment/{id}/review
- [Get] /api/payment/{id}
- [Get] /api/payment/user/{userId}
- [Get] /api/payment/pending
- [Post] /api/payment/membership/upgrade

## PrivacyController（4 个）
- [Get] /api/privacy/settings
- [Get] /api/privacy/setting
- [Post] /api/privacy/setting
- [Delete] /api/privacy/setting

## ProfileController（5 个）
- [Get] /api/profile/{userId}
- [Put] /api/profile/{userId}
- [Post] /api/profile/upload-avatar
- [Get] /api/profile/statistics/{userId}
- [Post] /api/profile/statistics/increment

## QuotaController（2 个）
- [Get] /api/quota/current
- [Post] /api/quota/reset

## RecommendationController（5 个）
- [Get] /api/recommendation/posts
- [Get] /api/recommendation/users
- [Post] /api/recommendation/track/view
- [Post] /api/recommendation/track/like
- [Post] /api/recommendation/track/comment

## RiskAssessmentController（3 个）
- [Post] /api/risk/assess
- [Get] /api/risk/history
- [Get] /api/risk/check

## SearchController（4 个）
- [Get] /api/search/users
- [Get] /api/search/posts
- [Get] /api/search/tags
- [Get] /api/search/posts/tag

## SocialController（15 个）
- [Post] /api/social/post
- [Get] /api/social/post/{postId}
- [Get] /api/social/posts/public
- [Get] /api/social/posts/user/{userId}
- [Post] /api/social/posts/friends
- [Delete] /api/social/post/{postId}
- [Post] /api/social/comment
- [Get] /api/social/comments/{postId}
- [Get] /api/social/replies/{parentId}
- [Delete] /api/social/comment/{commentId}
- [Post] /api/social/like
- [Get] /api/social/like/check
- [Post] /api/social/share
- [Post] /api/social/upload/image
- [Post] /api/social/upload/video

## ToolController（3 个）
- [Get] /api/tool/list
- [Post] /api/tool/call
- [Get] /api/tool/history

## UserController（22 个）
- [Post] /api/user/register
- [Post] /api/user/login
- [Get] /api/user/membership
- [Get] /api/user/{id}
- [Post] /api/user/forgot-password
- [Post] /api/user/reset-password
- [Get] /api/user/profile
- [Get] /api/user/me
- [Get] /api/user/me
- [Put] /api/user/profile
- [Get] /api/user/stats
- [Put] /api/user/change-password
- [Post] /api/user/upload-avatar
- [Post] /api/user/send-verification-code
- [Post] /api/user/verify-code
- [Post] /api/user/reset-password-by-code
- [Get] /api/user/status
- [Post] /api/user/status/update
- [Get] /api/user/minor-mode/status
- [Put] /api/user/minor-mode
- [Get] /api/user/voice-preference
- [Put] /api/user/voice-preference

## VoiceCallController（4 个）
- [Post] /api/voice-call/start
- [Post] /api/voice-call/{sessionId}/audio
- [Post] /api/voice-call/{sessionId}/end
- [Get] /api/voice-call/{sessionId}/status

## VoiceChatController（3 个）
- [Post] /api/voice/stt
- [Post] /api/voice/tts
- [Get] /api/voice/health

## WebCrawlerController（5 个）
- [Post] /api/crawler/collect
- [Get] /api/crawler/stats
- [Get] /api/crawler/search
- [Post] /api/crawler/clear-cache
- [Get] /api/crawler/health

## WebSearchAnswerController（1 个）
- [Post] /api/web-search/answer

## WebSearchController（6 个）
- [Post] /api/search/execute
- [Get] /api/search/quick
- [Post] /api/search/keywords
- [Get] /api/search/history
- [Get] /api/search/stats
- [Post] /api/search/verify
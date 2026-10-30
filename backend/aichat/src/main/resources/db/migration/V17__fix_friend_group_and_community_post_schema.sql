-- 修复 friend_group 与 community_post 表缺失字段，对齐实体类定义。
-- friend_group 实体期望 sort_order 字段，community_post 实体期望 media_type 字段。

-- friend_group 增加 sort_order（分组排序）
ALTER TABLE `friend_group` ADD COLUMN `sort_order` INT DEFAULT 0 COMMENT '排序序号' AFTER `name`;

-- community_post 增加 media_type（媒体类型：text/image/video）
ALTER TABLE `community_post` ADD COLUMN `media_type` VARCHAR(20) DEFAULT 'text' COMMENT '媒体类型' AFTER `content`;

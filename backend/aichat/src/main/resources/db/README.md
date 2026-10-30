# 数据库迁移脚本说明

## 迁移脚本管理规则（Phase 3 重构后）

本项目的数据库迁移脚本统一通过 **Flyway** 管理，所有新迁移脚本必须放在 `db/migration/` 目录下，命名格式为 `V{version}__{description}.sql`（双下划线分隔版本号与描述）。

## 目录结构

### ✅ `db/migration/` - 当前正式迁移目录（Flyway 管理）

| 脚本 | 说明 |
|---|---|
| `V1__create_base_tables.sql` | 基础表创建 |
| `V2__insert_initial_data.sql` | 初始数据 |
| `V3__insert_test_data.sql` | 测试数据 |
| `V4__insert_oauth_risk_data.sql` | OAuth 风险数据 |
| `V5__add_audit_fields.sql` | Phase 3 - 统一审计字段（created_by/updated_by/is_deleted） |
| `V6__add_performance_indexes.sql` | Phase 3 - 性能索引补齐 |

### ⚠️ `db/*.sql`（散落脚本）- 已废弃

下列脚本为 Phase 1 / Phase 2 早期开发阶段手动执行的迁移脚本，**不再维护**。Flyway 已通过 V1-V6 完整接管所有 schema 变更。

- `migration.sql` - 早期零散字段添加（ai_character.model、user.email/bio 等），V1 已包含
- `social_features_migration.sql` - 社交功能表创建，V1 已包含
- `social_optimization_migration.sql` - 社交表索引优化，V6 已包含
- `chat_features_migration.sql` - 聊天功能表创建，V1 已包含
- `ai_features_migration.sql` - AI 功能表创建，V1 已包含
- `advanced_features_migration.sql` - 高级功能表创建，V1 已包含
- `improvement_migration.sql` - 改进字段，V5 已包含
- `elf_optimization_migration.sql` - AI 精灵优化，已废弃
- `auth_optimization_migration.sql` - 认证优化，已废弃
- `character_isolation_migration.sql` - 角色隔离，V1 已包含
- `friend_chat_migration.sql` - 好友聊天，V1 已包含

## 执行规则

1. **新环境部署**：仅依赖 `db/migration/` 目录，Flyway 自动按版本号执行。
2. **现有环境升级**：Flyway `baseline-on-migrate=true` 会自动接管现有数据库，仅执行新增 V5+ 脚本。
3. **禁止**手动执行散落脚本，避免与 Flyway 元数据冲突。
4. **新增变更**：必须创建 `V{下一个版本号}__{描述}.sql` 文件，禁止修改已发布的历史脚本。

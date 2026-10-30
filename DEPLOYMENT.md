# AI Chat Graduation Project - Deployment Guide

## 1. System Requirements

### 1.1 Hardware Requirements
- CPU: Intel Core i5 or equivalent (minimum)
- RAM: 8GB minimum, 16GB recommended (for AI model inference)
- Storage: 5GB available space for application and AI models

### 1.2 Software Requirements
- Java Development Kit (JDK): 25 or higher
- MySQL Database: 8.0 or higher
- Ollama: Required for AI model inference (see Section 3)
- Maven: 3.8+ (for building)

---

## 2. Environment Setup

### 2.1 Install Java 25

**Windows:**
```powershell
# Check Java version
java -version

# If not installed, download from: https://adoptium.net/temurin/releases/?version=25
```

**Linux:**
```bash
sudo apt update
sudo apt install openjdk-25-jdk
java -version
```

### 2.2 Install MySQL

**Windows:**
1. Download MySQL Installer from: https://dev.mysql.com/downloads/installer/
2. Follow the installation wizard
3. 设置强密码（**不要**使用默认弱密码如 `123456`，建议至少 12 位含大小写数字符号）

**Linux:**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 2.3 Create Database

```sql
CREATE DATABASE ai_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ai_chat_user'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ai_chat.* TO 'ai_chat_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2.4 数据库密码外置（Phase 1 重构修复）

**重要说明**：Phase 1 架构重构后，数据库密码不再硬编码在 `application.yml`，
必须通过环境变量注入，避免源码泄漏导致数据库被入侵。

**Windows (PowerShell, 当前会话)**：
```powershell
$env:DB_USERNAME = "ai_chat_user"
$env:DB_PASSWORD = "YOUR_STRONG_PASSWORD"
$env:DB_URL = "jdbc:mysql://localhost:3306/ai_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true"
```

**Windows (持久化, 用户级环境变量)**：
```powershell
[Environment]::SetEnvironmentVariable("DB_USERNAME", "ai_chat_user", "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "YOUR_STRONG_PASSWORD", "User")
```

**Linux / macOS**：
```bash
export DB_USERNAME="ai_chat_user"
export DB_PASSWORD="YOUR_STRONG_PASSWORD"
# 可加入 ~/.bashrc 或 ~/.zshrc 持久化
```

**Docker / docker-compose**：
```yaml
environment:
  - DB_USERNAME=ai_chat_user
  - DB_PASSWORD=${DB_PASSWORD}  # 从 .env 文件读取
  - REDIS_HOST=redis
  - REDIS_PASSWORD=${REDIS_PASSWORD}
  - OPENAI_API_KEY=${OPENAI_API_KEY}
  - STT_PROVIDER=openai-whisper
  - TTS_PROVIDER=google-translate
  - IMAGE_PROVIDER=stable-diffusion
```

---

## 3. Ollama Installation (REQUIRED)

AI对话生成功能**依赖本地Ollama服务**，必须安装并运行Ollama才能使用AI对话功能。

### 3.1 Install Ollama

**Windows:**
1. Download from: https://ollama.com/download
2. Run the installer
3. Ollama will automatically start as a background service

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**macOS:**
```bash
brew install ollama
```

### 3.2 Verify Ollama Installation

```bash
ollama --version
```

Expected output: `ollama version x.x.x`

### 3.3 Pull AI Models

Before using the application, pull the required AI models:

```bash
# Pull Llama 3.2 3B (recommended for most use cases)
ollama pull llama3.2:3b

# Pull Llama 3.2 7B (better performance, requires more RAM)
ollama pull llama3.2:7b

# Pull Mistral
ollama pull mistral

# Pull Phi 3 (lightweight, fast)
ollama pull phi3
```

### 3.4 Start Ollama Service

```bash
# Linux/macOS
ollama serve

# Windows - Ollama runs automatically as a service
```

Verify the service is running:
```bash
curl http://localhost:11434/api/tags
```

---

## 4. Application Configuration

### 4.1 Configure Database Connection

Edit `backend/aichat/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4.2 Flyway Database Migration

Flyway is configured to automatically run database migrations on application startup.

**Migration Files Location:** `backend/aichat/src/main/resources/db/migration/`

**Flyway Configuration:**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true
```

- **V1__create_base_tables.sql**: Creates all database tables
- **V2__insert_initial_data.sql**: Inserts initial data (AI models, system characters)

---

## 5. Build and Run

### 5.1 Build the Application

```bash
cd backend/aichat
mvn clean package -DskipTests
```

### 5.2 Run the Application

**Development Mode:**
```bash
cd backend/aichat
mvn spring-boot:run
```

**Production Mode:**
```bash
java -jar target/aichat-0.0.1-SNAPSHOT.jar
```

### 5.3 Verify Application Startup

After startup, the application will be available at:
- API Base URL: `http://localhost:8080/api/`
- Health Check: `http://localhost:8080/api/chat/health`

Expected response from health check:
```json
{"code":200,"msg":"success","data":"AI服务已正常运行，可以使用对话功能"}
```

---

## 6. Frontend Setup

### 6.1 Install Dependencies

```bash
cd frontend
npm install
```

### 6.2 Run Frontend

```bash
npm run dev
```

Frontend will be available at: `http://localhost:5173/`

---

## 7. Troubleshooting

### 7.1 Ollama Connection Issues

**Symptom:** AI chat returns 500 error or "Ollama服务未连接"

**Solution:**
1. Verify Ollama service is running: `curl http://localhost:11434/api/tags`
2. Ensure the model is pulled: `ollama pull phi3`
3. Check firewall settings - port 11434 must be accessible

### 7.2 Database Connection Issues

**Symptom:** Application fails to start with database connection error

**Solution:**
1. Verify MySQL is running: `mysql -u root -p`
2. Check database credentials in `application.yml`
3. Ensure the `ai_chat` database exists

### 7.3 Flyway Migration Errors

**Symptom:** Application fails to start with Flyway validation error

**Solution:**
1. Check migration script syntax
2. Ensure all SQL scripts are valid MySQL syntax
3. Run `mvn flyway:repair` to fix schema history issues

### 7.4 Port Conflicts

**Symptom:** "Address already in use" error

**Solution:**
- Change server port in `application.yml`:
```yaml
server:
  port: 8081
```

---

## 8. API Endpoints

### 8.1 Authentication
- `POST /api/user/register` - Register user
- `POST /api/user/login` - Login user

### 8.2 AI Chat
- `POST /api/chat/completion` - AI chat completion
- `GET /api/model/list` - Get available models
- `GET /api/chat/health` - Health check

### 8.3 AI Characters
- `GET /api/character/list` - Get all characters
- `POST /api/character/create` - Create character

### 8.4 Social
- `GET /api/friend/list` - Get friend list
- `POST /api/friend/chat/list` - Get chat history
- `GET /api/community/list` - Get community list

### 8.5 Notifications
- `GET /api/notification/list` - Get notifications
- `GET /api/notification/unread/count` - Get unread count

### 8.6 Admin
- `GET /api/admin/announcements/list` - Get announcements

---

## 9. Security Considerations

1. **Password Security:** All passwords are stored using BCrypt encryption
2. **JWT Authentication:** Implement JWT for production use
3. **HTTPS:** Use HTTPS in production environment
4. **Input Validation:** All API endpoints validate input parameters
5. **Rate Limiting:** Consider adding rate limiting for public endpoints

---

## 10. Maintenance

### 10.1 Database Backup

```bash
mysqldump -u root -p ai_chat > ai_chat_backup.sql
```

### 10.2 Logs

Application logs are located at: `backend/aichat/logs/search-service.log`

### 10.3 Upgrading

1. Pull the latest code
2. Run `mvn clean package`
3. Stop the old instance
4. Start the new instance (Flyway will automatically apply migrations)

---

## Appendix

### A. Supported AI Models

| Model | Provider | Memory Requirement | Description |
|-------|----------|--------------------|-------------|
| Llama 3.2 3B | Ollama | ~4GB | Lightweight, fast inference |
| Llama 3.2 7B | Ollama | ~8GB | Balanced performance |
| Mistral | Ollama | ~8GB | High performance |
| Phi 3 | Ollama | ~4GB | Very fast, lightweight |

### B. Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | Database URL (Phase 1 重构后) | jdbc:mysql://localhost:3306/ai_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true |
| `DB_USERNAME` | Database username | root |
| `DB_PASSWORD` | Database password (**必填**) | (空，部署时必须设置) |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `REDIS_PASSWORD` | Redis password | (空) |
| `REDIS_DB` | Redis database | 0 |
| `OPENAI_API_KEY` | OpenAI API Key (用于 GPT/DALL-E/TTS/Whisper) | (空) |
| `IMAGE_PROVIDER` | 图像生成提供者 (stable-diffusion/openai-dall-e/disabled) | stable-diffusion |
| `SD_API_URL` | Stable Diffusion WebUI 地址 | http://localhost:7860 |
| `STT_PROVIDER` | 语音识别提供者 (openai-whisper/disabled) | openai-whisper |
| `TTS_PROVIDER` | 语音合成提供者 (google-translate/openai-tts/disabled) | google-translate |
| `SERVER_PORT` | Application port | 8080 |

### C. 迁移脚本现状（Phase 1 重构后说明）

项目使用 Flyway 管理数据库迁移脚本，位于 `src/main/resources/db/migration/`，
目前仅包含 V1-V4 基础迁移。

**遗留脚本**：`src/main/resources/` 根目录散落 16 个 `*_migration.sql` / `*_schema.sql`，
对应不同模块（社交、聊天、AI、Elf、支付等）的渐进式扩展，**未纳入 Flyway 管理**。
这些脚本由 `DatabaseInitializer.java` 在启动时按需执行（容错模式，已存在则跳过）。

**Phase 1 重构未做实际归并**（避免破坏现有部署），仅在文档标注现状。
建议后续 P3 阶段按依赖顺序整理为 V5+ 脚本纳入 Flyway 统一管理。

### D. 重构架构变更摘要

| 重构项 | 变更内容 |
|---|---|
| 数据库密码 | 由 `application.yml` 硬编码改为环境变量 `${DB_PASSWORD:}` |
| 循环依赖 | 关闭 `spring.main.allow-circular-references`（原为 `true`） |
| MyBatis 日志 | 由 `StdOutImpl`（控制台 stdout）改为 `Slf4jImpl`（logback 统一管理） |
| 参数校验 | 引入 `spring-boot-starter-validation`，UserController 用 `@Valid` + DTO |
| 异常体系 | 新增 `common/exception/` 包：`ErrorCode` 接口、`BusinessException`、`UserErrorCode` |
| 全局异常处理 | `GlobalExceptionHandler` 新增 `BusinessException` 处理路径，自动生成标准 R 响应 |
| 审计 AOP | 新增 `common/audit/` 包：`@Audited` 注解 + `AuditAspect` 切面，替代手写 logger |
| 测试环境 | 新增 `src/test/resources/application.yml`，用 H2 内存库 + 集成测试默认 @Disabled |
| AI 模型抽象 | `OllamaService` 标记 `@Deprecated`，新代码应注入 `ModelRouter` |

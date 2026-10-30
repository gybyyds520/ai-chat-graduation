-- 知识库 / RAG 问答功能建表脚本
-- 对应后端 KnowledgeBaseService：文档上传 -> 解析 -> 分块 -> 检索 -> 抽取式问答

-- 文档元数据表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '归属用户 ID',
    file_name     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type     VARCHAR(16)  NOT NULL COMMENT '文件类型 txt/pdf/docx',
    file_size     BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    file_path     VARCHAR(512) NOT NULL COMMENT '服务器存储绝对路径',
    title         VARCHAR(255) COMMENT '文档标题',
    chunk_count   INT          DEFAULT 0 COMMENT '分块数量',
    status        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/ready/failed',
    error_message VARCHAR(1024) COMMENT '失败原因',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_doc_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='知识库文档元数据';

-- 文档分块表
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    document_id  BIGINT       NOT NULL COMMENT '所属文档 ID',
    user_id      BIGINT       NOT NULL COMMENT '归属用户 ID(冗余隔离)',
    chunk_index  INT          NOT NULL COMMENT '分块序号(从0开始)',
    content      MEDIUMTEXT   NOT NULL COMMENT '分块文本内容',
    token_count  INT          DEFAULT 0 COMMENT '估算 token 数',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_kb_chunk_doc (document_id),
    KEY idx_kb_chunk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='知识库文档分块';

-- 问答记录表（可选审计/历史）
CREATE TABLE IF NOT EXISTS knowledge_query (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '归属用户 ID',
    document_id   BIGINT       NOT NULL COMMENT '提问文档 ID',
    query         TEXT         NOT NULL COMMENT '用户问题',
    answer        MEDIUMTEXT   COMMENT '抽取式回答',
    source_chunks VARCHAR(512) COMMENT '引用分块 ID JSON 数组',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_kb_query_user (user_id),
    KEY idx_kb_query_doc (document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='知识库问答记录';

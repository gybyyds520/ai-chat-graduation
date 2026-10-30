INSERT IGNORE INTO ai_model (model_name, model_key, provider, description, cost_per_token, max_tokens, available, category, priority) VALUES
('Llama 3.2 3B', 'llama3.2:3b', 'Ollama', '轻量级开源模型，适合日常对话', 0.0, 4096, TRUE, 'chat', 1),
('Llama 3.2 7B', 'llama3.2:7b', 'Ollama', '平衡性能和速度的开源模型', 0.0, 8192, TRUE, 'chat', 2),
('Mistral', 'mistral', 'Ollama', '高性能开源模型，推理迅速', 0.0, 8192, TRUE, 'chat', 3),
('Phi 3', 'phi3', 'Ollama', '微软发布的小型高效模型', 0.0, 4096, TRUE, 'chat', 4);

INSERT IGNORE INTO ai_character (name, avatar, prompt, model, is_system) VALUES
('AI翻译助手', '', '你是一个专业的AI翻译助手，请使用标准的翻译语言进行翻译。', 'phi3:mini', TRUE);

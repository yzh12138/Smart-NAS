-- Migration v5: Add ai_prompt to sys_user table, image_path to ai_message table
ALTER TABLE sys_user ADD COLUMN ai_prompt TEXT DEFAULT NULL AFTER avatar;
ALTER TABLE ai_message ADD COLUMN image_path VARCHAR(500) DEFAULT NULL AFTER content;

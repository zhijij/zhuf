SET NAMES utf8mb4;

-- Keep the development accounts and their roles, but remove all rental business
-- records and seeded demo/sample content.
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM biz_chat_message;
DELETE FROM biz_chat_session_user;
DELETE FROM biz_chat_session;

DELETE FROM rental_contract_confirm;
DELETE FROM rental_contract;
DELETE FROM rental_intention;
DELETE FROM rental_appointment;
DELETE FROM rental_house_favorite;
DELETE FROM rental_house_entrust;
DELETE FROM rental_house_image;
DELETE FROM rental_house;

DELETE FROM rental_tenant_preference;
DELETE FROM rental_tenant_profile;
DELETE FROM rental_owner_profile;

DELETE FROM ai_chat_message;
DELETE FROM ai_chat_session;
DELETE FROM ai_user_memory;
DELETE FROM ai_knowledge_chunk;
DELETE FROM ai_knowledge_doc;
DELETE FROM ai_vector_index_task;
DELETE FROM ai_tool_audit_log;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'kept_accounts' AS item, COUNT(*) AS count_value
FROM sys_user
WHERE user_name IN ('tenant_test', 'owner_test', 'agent_test', 'auditor_test')
UNION ALL
SELECT 'rental_houses', COUNT(*) FROM rental_house
UNION ALL
SELECT 'business_chat_sessions', COUNT(*) FROM biz_chat_session
UNION ALL
SELECT 'ai_knowledge_docs', COUNT(*) FROM ai_knowledge_doc;

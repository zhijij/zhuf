DELETE FROM ai_house_chunks;
DELETE FROM ai_knowledge_chunks;

SELECT 'vector_house_chunks' AS item, COUNT(*) AS count_value FROM ai_house_chunks
UNION ALL
SELECT 'vector_knowledge_chunks', COUNT(*) FROM ai_knowledge_chunks;

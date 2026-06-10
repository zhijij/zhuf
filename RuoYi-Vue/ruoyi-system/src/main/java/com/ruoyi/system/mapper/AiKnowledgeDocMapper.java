package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeDoc;

/**
 * AI知识库文档Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface AiKnowledgeDocMapper 
{
    /**
     * 查询AI知识库文档
     * 
     * @param docId AI知识库文档主键
     * @return AI知识库文档
     */
    public AiKnowledgeDoc selectAiKnowledgeDocByDocId(Long docId);

    /**
     * 查询AI知识库文档列表
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return AI知识库文档集合
     */
    public List<AiKnowledgeDoc> selectAiKnowledgeDocList(AiKnowledgeDoc aiKnowledgeDoc);

    /**
     * 新增AI知识库文档
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return 结果
     */
    public int insertAiKnowledgeDoc(AiKnowledgeDoc aiKnowledgeDoc);

    /**
     * 修改AI知识库文档
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return 结果
     */
    public int updateAiKnowledgeDoc(AiKnowledgeDoc aiKnowledgeDoc);

    /**
     * 删除AI知识库文档
     * 
     * @param docId AI知识库文档主键
     * @return 结果
     */
    public int deleteAiKnowledgeDocByDocId(Long docId);

    /**
     * 批量删除AI知识库文档
     * 
     * @param docIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiKnowledgeDocByDocIds(Long[] docIds);
}

package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeDoc;

/**
 * AI知识库文档Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IAiKnowledgeDocService 
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
     * 批量删除AI知识库文档
     * 
     * @param docIds 需要删除的AI知识库文档主键集合
     * @return 结果
     */
    public int deleteAiKnowledgeDocByDocIds(Long[] docIds);

    /**
     * 删除AI知识库文档信息
     * 
     * @param docId AI知识库文档主键
     * @return 结果
     */
    public int deleteAiKnowledgeDocByDocId(Long docId);
}

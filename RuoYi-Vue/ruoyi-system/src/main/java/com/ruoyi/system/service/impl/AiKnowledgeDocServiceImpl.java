package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiKnowledgeDocMapper;
import com.ruoyi.system.domain.AiKnowledgeDoc;
import com.ruoyi.system.service.IAiKnowledgeDocService;

/**
 * AI知识库文档Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiKnowledgeDocServiceImpl implements IAiKnowledgeDocService 
{
    @Autowired
    private AiKnowledgeDocMapper aiKnowledgeDocMapper;

    /**
     * 查询AI知识库文档
     * 
     * @param docId AI知识库文档主键
     * @return AI知识库文档
     */
    @Override
    public AiKnowledgeDoc selectAiKnowledgeDocByDocId(Long docId)
    {
        return aiKnowledgeDocMapper.selectAiKnowledgeDocByDocId(docId);
    }

    /**
     * 查询AI知识库文档列表
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return AI知识库文档
     */
    @Override
    public List<AiKnowledgeDoc> selectAiKnowledgeDocList(AiKnowledgeDoc aiKnowledgeDoc)
    {
        return aiKnowledgeDocMapper.selectAiKnowledgeDocList(aiKnowledgeDoc);
    }

    /**
     * 新增AI知识库文档
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return 结果
     */
    @Override
    public int insertAiKnowledgeDoc(AiKnowledgeDoc aiKnowledgeDoc)
    {
        aiKnowledgeDoc.setCreateTime(DateUtils.getNowDate());
        return aiKnowledgeDocMapper.insertAiKnowledgeDoc(aiKnowledgeDoc);
    }

    /**
     * 修改AI知识库文档
     * 
     * @param aiKnowledgeDoc AI知识库文档
     * @return 结果
     */
    @Override
    public int updateAiKnowledgeDoc(AiKnowledgeDoc aiKnowledgeDoc)
    {
        aiKnowledgeDoc.setUpdateTime(DateUtils.getNowDate());
        return aiKnowledgeDocMapper.updateAiKnowledgeDoc(aiKnowledgeDoc);
    }

    /**
     * 批量删除AI知识库文档
     * 
     * @param docIds 需要删除的AI知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteAiKnowledgeDocByDocIds(Long[] docIds)
    {
        return aiKnowledgeDocMapper.deleteAiKnowledgeDocByDocIds(docIds);
    }

    /**
     * 删除AI知识库文档信息
     * 
     * @param docId AI知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteAiKnowledgeDocByDocId(Long docId)
    {
        return aiKnowledgeDocMapper.deleteAiKnowledgeDocByDocId(docId);
    }
}

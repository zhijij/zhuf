package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiKnowledgeChunkMapper;
import com.ruoyi.system.domain.AiKnowledgeChunk;
import com.ruoyi.system.service.IAiKnowledgeChunkService;

/**
 * AI知识库分片Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiKnowledgeChunkServiceImpl implements IAiKnowledgeChunkService 
{
    @Autowired
    private AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    /**
     * 查询AI知识库分片
     * 
     * @param chunkId AI知识库分片主键
     * @return AI知识库分片
     */
    @Override
    public AiKnowledgeChunk selectAiKnowledgeChunkByChunkId(Long chunkId)
    {
        return aiKnowledgeChunkMapper.selectAiKnowledgeChunkByChunkId(chunkId);
    }

    /**
     * 查询AI知识库分片列表
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return AI知识库分片
     */
    @Override
    public List<AiKnowledgeChunk> selectAiKnowledgeChunkList(AiKnowledgeChunk aiKnowledgeChunk)
    {
        return aiKnowledgeChunkMapper.selectAiKnowledgeChunkList(aiKnowledgeChunk);
    }

    /**
     * 新增AI知识库分片
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return 结果
     */
    @Override
    public int insertAiKnowledgeChunk(AiKnowledgeChunk aiKnowledgeChunk)
    {
        aiKnowledgeChunk.setCreateTime(DateUtils.getNowDate());
        return aiKnowledgeChunkMapper.insertAiKnowledgeChunk(aiKnowledgeChunk);
    }

    /**
     * 修改AI知识库分片
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return 结果
     */
    @Override
    public int updateAiKnowledgeChunk(AiKnowledgeChunk aiKnowledgeChunk)
    {
        return aiKnowledgeChunkMapper.updateAiKnowledgeChunk(aiKnowledgeChunk);
    }

    /**
     * 批量删除AI知识库分片
     * 
     * @param chunkIds 需要删除的AI知识库分片主键
     * @return 结果
     */
    @Override
    public int deleteAiKnowledgeChunkByChunkIds(Long[] chunkIds)
    {
        return aiKnowledgeChunkMapper.deleteAiKnowledgeChunkByChunkIds(chunkIds);
    }

    /**
     * 删除AI知识库分片信息
     * 
     * @param chunkId AI知识库分片主键
     * @return 结果
     */
    @Override
    public int deleteAiKnowledgeChunkByChunkId(Long chunkId)
    {
        return aiKnowledgeChunkMapper.deleteAiKnowledgeChunkByChunkId(chunkId);
    }
}

package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeChunk;

/**
 * AI知识库分片Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IAiKnowledgeChunkService 
{
    /**
     * 查询AI知识库分片
     * 
     * @param chunkId AI知识库分片主键
     * @return AI知识库分片
     */
    public AiKnowledgeChunk selectAiKnowledgeChunkByChunkId(Long chunkId);

    /**
     * 查询AI知识库分片列表
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return AI知识库分片集合
     */
    public List<AiKnowledgeChunk> selectAiKnowledgeChunkList(AiKnowledgeChunk aiKnowledgeChunk);

    /**
     * 新增AI知识库分片
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return 结果
     */
    public int insertAiKnowledgeChunk(AiKnowledgeChunk aiKnowledgeChunk);

    /**
     * 修改AI知识库分片
     * 
     * @param aiKnowledgeChunk AI知识库分片
     * @return 结果
     */
    public int updateAiKnowledgeChunk(AiKnowledgeChunk aiKnowledgeChunk);

    /**
     * 批量删除AI知识库分片
     * 
     * @param chunkIds 需要删除的AI知识库分片主键集合
     * @return 结果
     */
    public int deleteAiKnowledgeChunkByChunkIds(Long[] chunkIds);

    /**
     * 删除AI知识库分片信息
     * 
     * @param chunkId AI知识库分片主键
     * @return 结果
     */
    public int deleteAiKnowledgeChunkByChunkId(Long chunkId);
}

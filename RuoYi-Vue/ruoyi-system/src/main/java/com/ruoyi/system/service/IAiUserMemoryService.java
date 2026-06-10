package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiUserMemory;

/**
 * AI用户记忆Service接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface IAiUserMemoryService 
{
    /**
     * 查询AI用户记忆
     * 
     * @param memoryId AI用户记忆主键
     * @return AI用户记忆
     */
    public AiUserMemory selectAiUserMemoryByMemoryId(Long memoryId);

    /**
     * 查询AI用户记忆列表
     * 
     * @param aiUserMemory AI用户记忆
     * @return AI用户记忆集合
     */
    public List<AiUserMemory> selectAiUserMemoryList(AiUserMemory aiUserMemory);

    /**
     * 新增AI用户记忆
     * 
     * @param aiUserMemory AI用户记忆
     * @return 结果
     */
    public int insertAiUserMemory(AiUserMemory aiUserMemory);

    /**
     * 修改AI用户记忆
     * 
     * @param aiUserMemory AI用户记忆
     * @return 结果
     */
    public int updateAiUserMemory(AiUserMemory aiUserMemory);

    /**
     * 批量删除AI用户记忆
     * 
     * @param memoryIds 需要删除的AI用户记忆主键集合
     * @return 结果
     */
    public int deleteAiUserMemoryByMemoryIds(Long[] memoryIds);

    /**
     * 删除AI用户记忆信息
     * 
     * @param memoryId AI用户记忆主键
     * @return 结果
     */
    public int deleteAiUserMemoryByMemoryId(Long memoryId);
}

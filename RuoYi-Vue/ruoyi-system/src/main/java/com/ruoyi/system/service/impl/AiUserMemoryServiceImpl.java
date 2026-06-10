package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiUserMemoryMapper;
import com.ruoyi.system.domain.AiUserMemory;
import com.ruoyi.system.service.IAiUserMemoryService;

/**
 * AI用户记忆Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiUserMemoryServiceImpl implements IAiUserMemoryService 
{
    @Autowired
    private AiUserMemoryMapper aiUserMemoryMapper;

    /**
     * 查询AI用户记忆
     * 
     * @param memoryId AI用户记忆主键
     * @return AI用户记忆
     */
    @Override
    public AiUserMemory selectAiUserMemoryByMemoryId(Long memoryId)
    {
        return aiUserMemoryMapper.selectAiUserMemoryByMemoryId(memoryId);
    }

    /**
     * 查询AI用户记忆列表
     * 
     * @param aiUserMemory AI用户记忆
     * @return AI用户记忆
     */
    @Override
    public List<AiUserMemory> selectAiUserMemoryList(AiUserMemory aiUserMemory)
    {
        return aiUserMemoryMapper.selectAiUserMemoryList(aiUserMemory);
    }

    /**
     * 新增AI用户记忆
     * 
     * @param aiUserMemory AI用户记忆
     * @return 结果
     */
    @Override
    public int insertAiUserMemory(AiUserMemory aiUserMemory)
    {
        aiUserMemory.setCreateTime(DateUtils.getNowDate());
        return aiUserMemoryMapper.insertAiUserMemory(aiUserMemory);
    }

    /**
     * 修改AI用户记忆
     * 
     * @param aiUserMemory AI用户记忆
     * @return 结果
     */
    @Override
    public int updateAiUserMemory(AiUserMemory aiUserMemory)
    {
        aiUserMemory.setUpdateTime(DateUtils.getNowDate());
        return aiUserMemoryMapper.updateAiUserMemory(aiUserMemory);
    }

    /**
     * 批量删除AI用户记忆
     * 
     * @param memoryIds 需要删除的AI用户记忆主键
     * @return 结果
     */
    @Override
    public int deleteAiUserMemoryByMemoryIds(Long[] memoryIds)
    {
        return aiUserMemoryMapper.deleteAiUserMemoryByMemoryIds(memoryIds);
    }

    /**
     * 删除AI用户记忆信息
     * 
     * @param memoryId AI用户记忆主键
     * @return 结果
     */
    @Override
    public int deleteAiUserMemoryByMemoryId(Long memoryId)
    {
        return aiUserMemoryMapper.deleteAiUserMemoryByMemoryId(memoryId);
    }
}

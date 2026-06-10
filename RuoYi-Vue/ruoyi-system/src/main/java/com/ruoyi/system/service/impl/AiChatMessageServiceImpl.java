package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiChatMessageMapper;
import com.ruoyi.system.domain.AiChatMessage;
import com.ruoyi.system.service.IAiChatMessageService;

/**
 * AI消息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiChatMessageServiceImpl implements IAiChatMessageService 
{
    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    /**
     * 查询AI消息
     * 
     * @param messageId AI消息主键
     * @return AI消息
     */
    @Override
    public AiChatMessage selectAiChatMessageByMessageId(Long messageId)
    {
        return aiChatMessageMapper.selectAiChatMessageByMessageId(messageId);
    }

    /**
     * 查询AI消息列表
     * 
     * @param aiChatMessage AI消息
     * @return AI消息
     */
    @Override
    public List<AiChatMessage> selectAiChatMessageList(AiChatMessage aiChatMessage)
    {
        return aiChatMessageMapper.selectAiChatMessageList(aiChatMessage);
    }

    /**
     * 新增AI消息
     * 
     * @param aiChatMessage AI消息
     * @return 结果
     */
    @Override
    public int insertAiChatMessage(AiChatMessage aiChatMessage)
    {
        aiChatMessage.setCreateTime(DateUtils.getNowDate());
        return aiChatMessageMapper.insertAiChatMessage(aiChatMessage);
    }

    /**
     * 修改AI消息
     * 
     * @param aiChatMessage AI消息
     * @return 结果
     */
    @Override
    public int updateAiChatMessage(AiChatMessage aiChatMessage)
    {
        return aiChatMessageMapper.updateAiChatMessage(aiChatMessage);
    }

    /**
     * 批量删除AI消息
     * 
     * @param messageIds 需要删除的AI消息主键
     * @return 结果
     */
    @Override
    public int deleteAiChatMessageByMessageIds(Long[] messageIds)
    {
        return aiChatMessageMapper.deleteAiChatMessageByMessageIds(messageIds);
    }

    /**
     * 删除AI消息信息
     * 
     * @param messageId AI消息主键
     * @return 结果
     */
    @Override
    public int deleteAiChatMessageByMessageId(Long messageId)
    {
        return aiChatMessageMapper.deleteAiChatMessageByMessageId(messageId);
    }
}

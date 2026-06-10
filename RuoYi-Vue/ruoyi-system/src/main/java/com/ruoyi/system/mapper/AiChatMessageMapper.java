package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiChatMessage;

/**
 * AI消息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface AiChatMessageMapper 
{
    /**
     * 查询AI消息
     * 
     * @param messageId AI消息主键
     * @return AI消息
     */
    public AiChatMessage selectAiChatMessageByMessageId(Long messageId);

    /**
     * 查询AI消息列表
     * 
     * @param aiChatMessage AI消息
     * @return AI消息集合
     */
    public List<AiChatMessage> selectAiChatMessageList(AiChatMessage aiChatMessage);

    /**
     * 新增AI消息
     * 
     * @param aiChatMessage AI消息
     * @return 结果
     */
    public int insertAiChatMessage(AiChatMessage aiChatMessage);

    /**
     * 修改AI消息
     * 
     * @param aiChatMessage AI消息
     * @return 结果
     */
    public int updateAiChatMessage(AiChatMessage aiChatMessage);

    /**
     * 删除AI消息
     * 
     * @param messageId AI消息主键
     * @return 结果
     */
    public int deleteAiChatMessageByMessageId(Long messageId);

    /**
     * 批量删除AI消息
     * 
     * @param messageIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiChatMessageByMessageIds(Long[] messageIds);
}

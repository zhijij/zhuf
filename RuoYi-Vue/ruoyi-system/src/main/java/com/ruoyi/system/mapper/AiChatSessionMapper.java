package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiChatSession;

/**
 * AI会话Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public interface AiChatSessionMapper 
{
    /**
     * 查询AI会话
     * 
     * @param sessionId AI会话主键
     * @return AI会话
     */
    public AiChatSession selectAiChatSessionBySessionId(Long sessionId);

    /**
     * 查询AI会话列表
     * 
     * @param aiChatSession AI会话
     * @return AI会话集合
     */
    public List<AiChatSession> selectAiChatSessionList(AiChatSession aiChatSession);

    /**
     * 新增AI会话
     * 
     * @param aiChatSession AI会话
     * @return 结果
     */
    public int insertAiChatSession(AiChatSession aiChatSession);

    /**
     * 修改AI会话
     * 
     * @param aiChatSession AI会话
     * @return 结果
     */
    public int updateAiChatSession(AiChatSession aiChatSession);

    /**
     * 删除AI会话
     * 
     * @param sessionId AI会话主键
     * @return 结果
     */
    public int deleteAiChatSessionBySessionId(Long sessionId);

    /**
     * 批量删除AI会话
     * 
     * @param sessionIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiChatSessionBySessionIds(Long[] sessionIds);
}

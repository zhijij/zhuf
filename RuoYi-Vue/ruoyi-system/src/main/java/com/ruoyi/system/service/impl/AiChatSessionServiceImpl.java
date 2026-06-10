package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AiChatSessionMapper;
import com.ruoyi.system.domain.AiChatSession;
import com.ruoyi.system.service.IAiChatSessionService;

/**
 * AI会话Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
@Service
public class AiChatSessionServiceImpl implements IAiChatSessionService 
{
    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;

    /**
     * 查询AI会话
     * 
     * @param sessionId AI会话主键
     * @return AI会话
     */
    @Override
    public AiChatSession selectAiChatSessionBySessionId(Long sessionId)
    {
        return aiChatSessionMapper.selectAiChatSessionBySessionId(sessionId);
    }

    /**
     * 查询AI会话列表
     * 
     * @param aiChatSession AI会话
     * @return AI会话
     */
    @Override
    public List<AiChatSession> selectAiChatSessionList(AiChatSession aiChatSession)
    {
        return aiChatSessionMapper.selectAiChatSessionList(aiChatSession);
    }

    /**
     * 新增AI会话
     * 
     * @param aiChatSession AI会话
     * @return 结果
     */
    @Override
    public int insertAiChatSession(AiChatSession aiChatSession)
    {
        aiChatSession.setCreateTime(DateUtils.getNowDate());
        return aiChatSessionMapper.insertAiChatSession(aiChatSession);
    }

    /**
     * 修改AI会话
     * 
     * @param aiChatSession AI会话
     * @return 结果
     */
    @Override
    public int updateAiChatSession(AiChatSession aiChatSession)
    {
        aiChatSession.setUpdateTime(DateUtils.getNowDate());
        return aiChatSessionMapper.updateAiChatSession(aiChatSession);
    }

    /**
     * 批量删除AI会话
     * 
     * @param sessionIds 需要删除的AI会话主键
     * @return 结果
     */
    @Override
    public int deleteAiChatSessionBySessionIds(Long[] sessionIds)
    {
        return aiChatSessionMapper.deleteAiChatSessionBySessionIds(sessionIds);
    }

    /**
     * 删除AI会话信息
     * 
     * @param sessionId AI会话主键
     * @return 结果
     */
    @Override
    public int deleteAiChatSessionBySessionId(Long sessionId)
    {
        return aiChatSessionMapper.deleteAiChatSessionBySessionId(sessionId);
    }
}

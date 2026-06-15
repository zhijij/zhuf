package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AiConversationSession;

public interface AiConversationSessionMapper
{
    public AiConversationSession selectAiConversationSessionByConversationId(Long conversationId);

    public int insertAiConversationSession(AiConversationSession aiConversationSession);

    public int updateAiConversationSession(AiConversationSession aiConversationSession);
}

package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AiConversationContext;

public interface AiConversationContextMapper
{
    public AiConversationContext selectAiConversationContextByConversationId(Long conversationId);

    public int insertAiConversationContext(AiConversationContext aiConversationContext);

    public int updateAiConversationContext(AiConversationContext aiConversationContext);

    public int deleteAiConversationContextByConversationId(Long conversationId);
}

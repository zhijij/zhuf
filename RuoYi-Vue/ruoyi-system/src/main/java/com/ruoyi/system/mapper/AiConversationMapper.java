package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiConversation;

public interface AiConversationMapper
{
    public AiConversation selectAiConversationByConversationId(Long conversationId);

    public List<AiConversation> selectAiConversationList(AiConversation aiConversation);

    public int insertAiConversation(AiConversation aiConversation);

    public int updateAiConversation(AiConversation aiConversation);
}

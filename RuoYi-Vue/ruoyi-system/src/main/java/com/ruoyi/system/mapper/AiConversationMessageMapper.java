package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiConversationMessage;

public interface AiConversationMessageMapper
{
    public AiConversationMessage selectAiConversationMessageByMessageId(Long messageId);

    public List<AiConversationMessage> selectAiConversationMessageList(AiConversationMessage aiConversationMessage);

    public int insertAiConversationMessage(AiConversationMessage aiConversationMessage);

    public int deleteAiConversationMessageByConversationId(Long conversationId);
}

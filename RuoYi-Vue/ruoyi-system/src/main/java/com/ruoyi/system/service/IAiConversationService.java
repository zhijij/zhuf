package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.AiConversation;
import com.ruoyi.system.domain.AiConversationContext;
import com.ruoyi.system.domain.AiConversationMessage;

public interface IAiConversationService
{
    public List<AiConversation> listMyConversations(String workMode);

    public AiConversation createConversation(Map<String, Object> payload);

    public AiConversation getMyConversation(Long conversationId);

    public void deleteMyConversation(Long conversationId);

    public AiConversationContext saveContext(Long conversationId, Map<String, Object> payload);

    public AiConversationMessage addMessage(Long conversationId, String role, String content, Map<String, Object> metadata);

    public List<Map<String, String>> buildHistory(Long conversationId, int limit);

    public String summarizeConversation(Long conversationId);
}

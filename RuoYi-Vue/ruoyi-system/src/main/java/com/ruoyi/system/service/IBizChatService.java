package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.BizChatMessage;
import com.ruoyi.system.domain.BizChatSession;

public interface IBizChatService
{
    BizChatSession openSession(String bizType, Long bizId);

    List<BizChatSession> listMySessions();

    List<BizChatMessage> listMessages(Long sessionId);

    BizChatMessage sendMessage(Long sessionId, String content, String messageType);

    BizChatMessage sendSystemMessage(String bizType, Long bizId, String content);
}

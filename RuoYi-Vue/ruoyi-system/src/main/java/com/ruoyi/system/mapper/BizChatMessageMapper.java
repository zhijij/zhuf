package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.BizChatMessage;

public interface BizChatMessageMapper
{
    BizChatMessage selectBizChatMessageByMessageId(Long messageId);

    List<BizChatMessage> selectBizChatMessageList(BizChatMessage bizChatMessage);

    int insertBizChatMessage(BizChatMessage bizChatMessage);
}

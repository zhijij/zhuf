package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BizChatSession;

public interface BizChatSessionMapper
{
    BizChatSession selectBizChatSessionBySessionId(Long sessionId);

    BizChatSession selectBizChatSessionByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    List<BizChatSession> selectBizChatSessionList(BizChatSession bizChatSession);

    List<BizChatSession> selectBizChatSessionListByUserId(Long userId);

    int insertBizChatSession(BizChatSession bizChatSession);

    int updateBizChatSession(BizChatSession bizChatSession);
}

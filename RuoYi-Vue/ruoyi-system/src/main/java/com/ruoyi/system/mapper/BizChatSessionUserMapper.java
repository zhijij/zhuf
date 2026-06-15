package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BizChatSessionUser;

public interface BizChatSessionUserMapper
{
    BizChatSessionUser selectBizChatSessionUser(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    List<BizChatSessionUser> selectBizChatSessionUserList(BizChatSessionUser bizChatSessionUser);

    int insertBizChatSessionUser(BizChatSessionUser bizChatSessionUser);

    int markSessionRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId, @Param("messageId") Long messageId);

    int increaseUnreadForReceivers(@Param("sessionId") Long sessionId, @Param("senderId") Long senderId);
}

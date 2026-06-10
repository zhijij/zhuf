package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Business chat session member.
 */
public class BizChatSessionUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "会话ID")
    private Long sessionId;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "业务角色")
    private String userRole;

    @Excel(name = "未读数量")
    private Integer unreadCount;

    @Excel(name = "最后已读消息ID")
    private Long lastReadMessageId;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserRole()
    {
        return userRole;
    }

    public void setUserRole(String userRole)
    {
        this.userRole = userRole;
    }

    public Integer getUnreadCount()
    {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount)
    {
        this.unreadCount = unreadCount;
    }

    public Long getLastReadMessageId()
    {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId)
    {
        this.lastReadMessageId = lastReadMessageId;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("sessionId", getSessionId())
                .append("userId", getUserId())
                .append("userRole", getUserRole())
                .append("unreadCount", getUnreadCount())
                .append("lastReadMessageId", getLastReadMessageId())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}

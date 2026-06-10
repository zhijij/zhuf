package com.ruoyi.system.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Business chat session.
 */
public class BizChatSession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long sessionId;

    @Excel(name = "业务类型")
    private String bizType;

    @Excel(name = "业务ID")
    private Long bizId;

    @Excel(name = "会话标题")
    private String title;

    @Excel(name = "状态")
    private String status;

    @Excel(name = "最后消息")
    private String lastMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后消息时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageTime;

    private List<BizChatSessionUser> members;

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage)
    {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTime()
    {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime)
    {
        this.lastMessageTime = lastMessageTime;
    }

    public List<BizChatSessionUser> getMembers()
    {
        return members;
    }

    public void setMembers(List<BizChatSessionUser> members)
    {
        this.members = members;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("sessionId", getSessionId())
                .append("bizType", getBizType())
                .append("bizId", getBizId())
                .append("title", getTitle())
                .append("status", getStatus())
                .append("lastMessage", getLastMessage())
                .append("lastMessageTime", getLastMessageTime())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}

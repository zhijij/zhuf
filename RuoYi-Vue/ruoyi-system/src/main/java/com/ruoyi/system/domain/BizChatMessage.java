package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Business chat message.
 */
public class BizChatMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long messageId;

    @Excel(name = "会话ID")
    private Long sessionId;

    @Excel(name = "发送人ID")
    private Long senderId;

    @Excel(name = "消息类型")
    private String messageType;

    @Excel(name = "消息内容")
    private String content;

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getSenderId()
    {
        return senderId;
    }

    public void setSenderId(Long senderId)
    {
        this.senderId = senderId;
    }

    public String getMessageType()
    {
        return messageType;
    }

    public void setMessageType(String messageType)
    {
        this.messageType = messageType;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("messageId", getMessageId())
                .append("sessionId", getSessionId())
                .append("senderId", getSenderId())
                .append("messageType", getMessageType())
                .append("content", getContent())
                .append("createTime", getCreateTime())
                .toString();
    }
}

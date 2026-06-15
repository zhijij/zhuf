package com.ruoyi.system.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class AiConversation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long conversationId;
    private Long userId;
    private String title;
    private String role;
    private String workMode;
    private String summary;
    private String lastMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageTime;

    private String status;
    private List<AiConversationMessage> messages;
    private AiConversationContext context;
    private AiConversationSession session;

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getWorkMode()
    {
        return workMode;
    }

    public void setWorkMode(String workMode)
    {
        this.workMode = workMode;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<AiConversationMessage> getMessages()
    {
        return messages;
    }

    public void setMessages(List<AiConversationMessage> messages)
    {
        this.messages = messages;
    }

    public AiConversationContext getContext()
    {
        return context;
    }

    public void setContext(AiConversationContext context)
    {
        this.context = context;
    }

    public AiConversationSession getSession()
    {
        return session;
    }

    public void setSession(AiConversationSession session)
    {
        this.session = session;
    }
}

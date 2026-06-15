package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class AiConversationMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private Long conversationId;
    private Long userId;
    private String role;
    private String content;
    private String intent;
    private String intentLabel;
    private String toolCalls;
    private String collaboration;
    private String contextSnapshot;

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

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

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getIntent()
    {
        return intent;
    }

    public void setIntent(String intent)
    {
        this.intent = intent;
    }

    public String getIntentLabel()
    {
        return intentLabel;
    }

    public void setIntentLabel(String intentLabel)
    {
        this.intentLabel = intentLabel;
    }

    public String getToolCalls()
    {
        return toolCalls;
    }

    public void setToolCalls(String toolCalls)
    {
        this.toolCalls = toolCalls;
    }

    public String getCollaboration()
    {
        return collaboration;
    }

    public void setCollaboration(String collaboration)
    {
        this.collaboration = collaboration;
    }

    public String getContextSnapshot()
    {
        return contextSnapshot;
    }

    public void setContextSnapshot(String contextSnapshot)
    {
        this.contextSnapshot = contextSnapshot;
    }
}

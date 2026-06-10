package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI消息对象 ai_chat_message
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiChatMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long messageId;

    /** 会话ID */
    @Excel(name = "会话ID")
    private Long sessionId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 发送方:user/assistant/tool/system */
    @Excel(name = "发送方:user/assistant/tool/system")
    private String senderType;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 工具名称 */
    @Excel(name = "工具名称")
    private String toolName;

    /** 工具参数 */
    @Excel(name = "工具参数")
    private String toolArgs;

    /** 工具结果 */
    @Excel(name = "工具结果")
    private String toolResult;

    /** Token数量 */
    @Excel(name = "Token数量")
    private Long tokenCount;

    public void setMessageId(Long messageId) 
    {
        this.messageId = messageId;
    }

    public Long getMessageId() 
    {
        return messageId;
    }

    public void setSessionId(Long sessionId) 
    {
        this.sessionId = sessionId;
    }

    public Long getSessionId() 
    {
        return sessionId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setSenderType(String senderType) 
    {
        this.senderType = senderType;
    }

    public String getSenderType() 
    {
        return senderType;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setToolName(String toolName) 
    {
        this.toolName = toolName;
    }

    public String getToolName() 
    {
        return toolName;
    }

    public void setToolArgs(String toolArgs) 
    {
        this.toolArgs = toolArgs;
    }

    public String getToolArgs() 
    {
        return toolArgs;
    }

    public void setToolResult(String toolResult) 
    {
        this.toolResult = toolResult;
    }

    public String getToolResult() 
    {
        return toolResult;
    }

    public void setTokenCount(Long tokenCount) 
    {
        this.tokenCount = tokenCount;
    }

    public Long getTokenCount() 
    {
        return tokenCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("messageId", getMessageId())
            .append("sessionId", getSessionId())
            .append("userId", getUserId())
            .append("senderType", getSenderType())
            .append("content", getContent())
            .append("toolName", getToolName())
            .append("toolArgs", getToolArgs())
            .append("toolResult", getToolResult())
            .append("tokenCount", getTokenCount())
            .append("createTime", getCreateTime())
            .toString();
    }
}

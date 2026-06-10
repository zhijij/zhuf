package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI工具调用审计对象 ai_tool_audit_log
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiToolAuditLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 请求ID */
    @Excel(name = "请求ID")
    private String requestId;

    /** 会话ID */
    @Excel(name = "会话ID")
    private Long sessionId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 角色 */
    @Excel(name = "角色")
    private String role;

    /** 工具名称 */
    @Excel(name = "工具名称")
    private String toolName;

    /** 工具参数 */
    @Excel(name = "工具参数")
    private String toolArgs;

    /** 工具结果 */
    @Excel(name = "工具结果")
    private String toolResult;

    /** 是否成功 */
    @Excel(name = "是否成功")
    private String success;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMsg;

    /** 耗时毫秒 */
    @Excel(name = "耗时毫秒")
    private Long costMs;

    public void setLogId(Long logId) 
    {
        this.logId = logId;
    }

    public Long getLogId() 
    {
        return logId;
    }

    public void setRequestId(String requestId) 
    {
        this.requestId = requestId;
    }

    public String getRequestId() 
    {
        return requestId;
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

    public void setRole(String role) 
    {
        this.role = role;
    }

    public String getRole() 
    {
        return role;
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

    public void setSuccess(String success) 
    {
        this.success = success;
    }

    public String getSuccess() 
    {
        return success;
    }

    public void setErrorMsg(String errorMsg) 
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() 
    {
        return errorMsg;
    }

    public void setCostMs(Long costMs) 
    {
        this.costMs = costMs;
    }

    public Long getCostMs() 
    {
        return costMs;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("requestId", getRequestId())
            .append("sessionId", getSessionId())
            .append("userId", getUserId())
            .append("role", getRole())
            .append("toolName", getToolName())
            .append("toolArgs", getToolArgs())
            .append("toolResult", getToolResult())
            .append("success", getSuccess())
            .append("errorMsg", getErrorMsg())
            .append("costMs", getCostMs())
            .append("createTime", getCreateTime())
            .toString();
    }
}

package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI用户记忆对象 ai_user_memory
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiUserMemory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记忆ID */
    private Long memoryId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 角色 */
    @Excel(name = "角色")
    private String role;

    /** 类型:preference/fact/summary */
    @Excel(name = "类型:preference/fact/summary")
    private String memoryType;

    /** 记忆内容 */
    @Excel(name = "记忆内容")
    private String content;

    /** 重要度 */
    @Excel(name = "重要度")
    private BigDecimal importance;

    /** 向量状态:0未索引,1已索引,2失败 */
    @Excel(name = "向量状态:0未索引,1已索引,2失败")
    private String vectorStatus;

    /** 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expiresAt;

    public void setMemoryId(Long memoryId) 
    {
        this.memoryId = memoryId;
    }

    public Long getMemoryId() 
    {
        return memoryId;
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

    public void setMemoryType(String memoryType) 
    {
        this.memoryType = memoryType;
    }

    public String getMemoryType() 
    {
        return memoryType;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setImportance(BigDecimal importance) 
    {
        this.importance = importance;
    }

    public BigDecimal getImportance() 
    {
        return importance;
    }

    public void setVectorStatus(String vectorStatus) 
    {
        this.vectorStatus = vectorStatus;
    }

    public String getVectorStatus() 
    {
        return vectorStatus;
    }

    public void setExpiresAt(Date expiresAt) 
    {
        this.expiresAt = expiresAt;
    }

    public Date getExpiresAt() 
    {
        return expiresAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("memoryId", getMemoryId())
            .append("userId", getUserId())
            .append("role", getRole())
            .append("memoryType", getMemoryType())
            .append("content", getContent())
            .append("importance", getImportance())
            .append("vectorStatus", getVectorStatus())
            .append("expiresAt", getExpiresAt())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

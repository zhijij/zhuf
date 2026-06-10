package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 租赁意向对象 rental_intention
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalIntention extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 意向ID */
    private Long intentionId;

    /** 房源ID */
    @Excel(name = "房源ID")
    private Long houseId;

    /** 租户用户ID */
    @Excel(name = "租户用户ID")
    private Long tenantId;

    /** 户主用户ID */
    @Excel(name = "户主用户ID")
    private Long ownerId;

    /** 中介用户ID，自主出租时为空 */
    @Excel(name = "中介用户ID，自主出租时为空")
    private Long agentId;

    /** 意向等级:1低,2中,3高 */
    @Excel(name = "意向等级:1低,2中,3高")
    private String intentionLevel;

    /** 状态:0跟进中,1已成交,2无效,3放弃 */
    @Excel(name = "状态:0跟进中,1已成交,2无效,3放弃")
    private String status;

    /** 预算 */
    @Excel(name = "预算")
    private BigDecimal budgetAmount;

    /** 期望入住时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "期望入住时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expectedMoveIn;

    /** 备注 */
    @Excel(name = "备注")
    private String note;

    /** AI意向摘要 */
    @Excel(name = "AI意向摘要")
    private String aiSummary;

    public void setIntentionId(Long intentionId) 
    {
        this.intentionId = intentionId;
    }

    public Long getIntentionId() 
    {
        return intentionId;
    }

    public void setHouseId(Long houseId) 
    {
        this.houseId = houseId;
    }

    public Long getHouseId() 
    {
        return houseId;
    }

    public void setTenantId(Long tenantId) 
    {
        this.tenantId = tenantId;
    }

    public Long getTenantId() 
    {
        return tenantId;
    }

    public void setOwnerId(Long ownerId) 
    {
        this.ownerId = ownerId;
    }

    public Long getOwnerId() 
    {
        return ownerId;
    }

    public void setAgentId(Long agentId) 
    {
        this.agentId = agentId;
    }

    public Long getAgentId() 
    {
        return agentId;
    }

    public void setIntentionLevel(String intentionLevel) 
    {
        this.intentionLevel = intentionLevel;
    }

    public String getIntentionLevel() 
    {
        return intentionLevel;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) 
    {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getBudgetAmount() 
    {
        return budgetAmount;
    }

    public void setExpectedMoveIn(Date expectedMoveIn) 
    {
        this.expectedMoveIn = expectedMoveIn;
    }

    public Date getExpectedMoveIn() 
    {
        return expectedMoveIn;
    }

    public void setNote(String note) 
    {
        this.note = note;
    }

    public String getNote() 
    {
        return note;
    }

    public void setAiSummary(String aiSummary) 
    {
        this.aiSummary = aiSummary;
    }

    public String getAiSummary() 
    {
        return aiSummary;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("intentionId", getIntentionId())
            .append("houseId", getHouseId())
            .append("tenantId", getTenantId())
            .append("ownerId", getOwnerId())
            .append("agentId", getAgentId())
            .append("intentionLevel", getIntentionLevel())
            .append("status", getStatus())
            .append("budgetAmount", getBudgetAmount())
            .append("expectedMoveIn", getExpectedMoveIn())
            .append("note", getNote())
            .append("aiSummary", getAiSummary())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

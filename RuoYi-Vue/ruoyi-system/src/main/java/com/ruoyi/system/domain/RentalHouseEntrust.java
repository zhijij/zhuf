package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 房源委托关系对象 rental_house_entrust
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalHouseEntrust extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 委托ID */
    private Long entrustId;

    /** 房源ID */
    @Excel(name = "房源ID")
    private Long houseId;

    /** 户主用户ID */
    @Excel(name = "户主用户ID")
    private Long ownerId;

    /** 中介用户ID */
    @Excel(name = "中介用户ID")
    private Long agentId;

    /** 委托范围:发布,预约,带看,签约等 */
    @Excel(name = "委托范围:发布,预约,带看,签约等")
    private String entrustScope;

    /** 佣金比例 */
    @Excel(name = "佣金比例")
    private BigDecimal commissionRate;

    /** 委托开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "委托开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 委托结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "委托结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 状态:0待确认,1生效中,2已拒绝,3已终止,4已过期 */
    @Excel(name = "状态:0待确认,1生效中,2已拒绝,3已终止,4已过期")
    private String status;

    public void setEntrustId(Long entrustId) 
    {
        this.entrustId = entrustId;
    }

    public Long getEntrustId() 
    {
        return entrustId;
    }

    public void setHouseId(Long houseId) 
    {
        this.houseId = houseId;
    }

    public Long getHouseId() 
    {
        return houseId;
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

    public void setEntrustScope(String entrustScope) 
    {
        this.entrustScope = entrustScope;
    }

    public String getEntrustScope() 
    {
        return entrustScope;
    }

    public void setCommissionRate(BigDecimal commissionRate) 
    {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getCommissionRate() 
    {
        return commissionRate;
    }

    public void setStartDate(Date startDate) 
    {
        this.startDate = startDate;
    }

    public Date getStartDate() 
    {
        return startDate;
    }

    public void setEndDate(Date endDate) 
    {
        this.endDate = endDate;
    }

    public Date getEndDate() 
    {
        return endDate;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("entrustId", getEntrustId())
            .append("houseId", getHouseId())
            .append("ownerId", getOwnerId())
            .append("agentId", getAgentId())
            .append("entrustScope", getEntrustScope())
            .append("commissionRate", getCommissionRate())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

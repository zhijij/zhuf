package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 租赁合同对象 rental_contract
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class RentalContract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 合同ID */
    private Long contractId;

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

    /** 合同编号 */
    @Excel(name = "合同编号")
    private String contractNo;

    /** 租期开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期开始", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 租期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期结束", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 月租金 */
    @Excel(name = "月租金")
    private BigDecimal rentAmount;

    /** 押金 */
    @Excel(name = "押金")
    private BigDecimal depositAmount;

    /** 付款周期 */
    @Excel(name = "付款周期")
    private String paymentCycle;

    /** 合同内容 */
    @Excel(name = "合同内容")
    private String contractContent;

    /** AI风险摘要 */
    @Excel(name = "AI风险摘要")
    private String aiRiskSummary;

    /** 状态:0草稿,1待签,2生效,3终止,4作废 */
    @Excel(name = "状态:0草稿,1待签,2生效,3终止,4作废")
    private String status;

    public void setContractId(Long contractId) 
    {
        this.contractId = contractId;
    }

    public Long getContractId() 
    {
        return contractId;
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

    public void setContractNo(String contractNo) 
    {
        this.contractNo = contractNo;
    }

    public String getContractNo() 
    {
        return contractNo;
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

    public void setRentAmount(BigDecimal rentAmount) 
    {
        this.rentAmount = rentAmount;
    }

    public BigDecimal getRentAmount() 
    {
        return rentAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) 
    {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getDepositAmount() 
    {
        return depositAmount;
    }

    public void setPaymentCycle(String paymentCycle) 
    {
        this.paymentCycle = paymentCycle;
    }

    public String getPaymentCycle() 
    {
        return paymentCycle;
    }

    public void setContractContent(String contractContent) 
    {
        this.contractContent = contractContent;
    }

    public String getContractContent() 
    {
        return contractContent;
    }

    public void setAiRiskSummary(String aiRiskSummary) 
    {
        this.aiRiskSummary = aiRiskSummary;
    }

    public String getAiRiskSummary() 
    {
        return aiRiskSummary;
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
            .append("contractId", getContractId())
            .append("houseId", getHouseId())
            .append("tenantId", getTenantId())
            .append("ownerId", getOwnerId())
            .append("agentId", getAgentId())
            .append("contractNo", getContractNo())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("rentAmount", getRentAmount())
            .append("depositAmount", getDepositAmount())
            .append("paymentCycle", getPaymentCycle())
            .append("contractContent", getContractContent())
            .append("aiRiskSummary", getAiRiskSummary())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

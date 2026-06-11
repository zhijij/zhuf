package com.ruoyi.system.domain.dto;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 房源成交请求。
 */
public class RentalHouseDealRequest
{
    /** 租户用户ID */
    private Long tenantId;

    /** 合同编号，可由后续合同层统一生成 */
    private String contractNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    private String paymentCycle;

    private String contractContent;

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getContractNo()
    {
        return contractNo;
    }

    public void setContractNo(String contractNo)
    {
        this.contractNo = contractNo;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public BigDecimal getRentAmount()
    {
        return rentAmount;
    }

    public void setRentAmount(BigDecimal rentAmount)
    {
        this.rentAmount = rentAmount;
    }

    public BigDecimal getDepositAmount()
    {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount)
    {
        this.depositAmount = depositAmount;
    }

    public String getPaymentCycle()
    {
        return paymentCycle;
    }

    public void setPaymentCycle(String paymentCycle)
    {
        this.paymentCycle = paymentCycle;
    }

    public String getContractContent()
    {
        return contractContent;
    }

    public void setContractContent(String contractContent)
    {
        this.contractContent = contractContent;
    }
}

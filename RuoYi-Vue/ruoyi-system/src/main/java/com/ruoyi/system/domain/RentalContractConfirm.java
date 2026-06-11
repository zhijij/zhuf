package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 合同参与方确认对象 rental_contract_confirm
 */
public class RentalContractConfirm extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 确认ID */
    private Long confirmId;

    /** 合同ID */
    @Excel(name = "合同ID")
    private Long contractId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户角色：tenant/owner/agent */
    @Excel(name = "用户角色")
    private String userRole;

    /** 确认状态：0未确认，1已确认，2已拒绝 */
    @Excel(name = "确认状态")
    private String confirmStatus;

    /** 确认意见 */
    @Excel(name = "确认意见")
    private String confirmOpinion;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "确认时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    public Long getConfirmId()
    {
        return confirmId;
    }

    public void setConfirmId(Long confirmId)
    {
        this.confirmId = confirmId;
    }

    public Long getContractId()
    {
        return contractId;
    }

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserRole()
    {
        return userRole;
    }

    public void setUserRole(String userRole)
    {
        this.userRole = userRole;
    }

    public String getConfirmStatus()
    {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus)
    {
        this.confirmStatus = confirmStatus;
    }

    public String getConfirmOpinion()
    {
        return confirmOpinion;
    }

    public void setConfirmOpinion(String confirmOpinion)
    {
        this.confirmOpinion = confirmOpinion;
    }

    public Date getConfirmTime()
    {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime)
    {
        this.confirmTime = confirmTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("confirmId", getConfirmId())
                .append("contractId", getContractId())
                .append("userId", getUserId())
                .append("userRole", getUserRole())
                .append("confirmStatus", getConfirmStatus())
                .append("confirmOpinion", getConfirmOpinion())
                .append("confirmTime", getConfirmTime())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
